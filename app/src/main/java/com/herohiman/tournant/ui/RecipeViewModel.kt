package com.herohiman.tournant.ui

import android.text.format.DateUtils
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.herohiman.tournant.R
import com.herohiman.tournant.TournantApplication
import com.herohiman.tournant.addGroupTitles
import com.herohiman.tournant.data.IngredientLine
import com.herohiman.tournant.data.IngredientLine.IngredientGroupTitle
import com.herohiman.tournant.data.IngredientLine.IngredientItem
import com.herohiman.tournant.data.RecipeTitleId
import com.herohiman.tournant.lessYield
import com.herohiman.tournant.logit
import com.herohiman.tournant.moreYield
import com.herohiman.tournant.parseLocalFormattedDoubleOrNull
import com.herohiman.tournant.separator
import com.herohiman.tournant.toStringForCooks
import com.herohiman.tournant.cost.LiveCostCalculator
import com.herohiman.tournant.cost.RecipeCostBreakdown
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.UnitAliasEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class RecipeViewModel(application: TournantApplication, private val recipeId: Long) : AndroidViewModel(application) {

	private val recipeRepository = application.recipeRepository

	private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
	val uiEvents = _uiEvents.receiveAsFlow()

	private val _recipeYieldValue = MutableStateFlow<Double?>(null)
	private val _targetYieldValue = MutableStateFlow<Double?>(null)
	private val _yieldFromTextField = MutableStateFlow<String?>(null)

	private val _scaleRatio = combine(_recipeYieldValue, _targetYieldValue) { currentYield, targetYield ->
		targetYield?.div(currentYield ?: 1.0) ?: 1.0
	}
	val scaleRatio: Flow<Double> = _scaleRatio

	val yieldValueScaled = combine(_recipeYieldValue, _scaleRatio, _yieldFromTextField) { yield, ratio, textField ->
		textField ?: yield?.times(ratio).toStringForCooks(thousands = false)
	}

	private val _ingredients = MutableStateFlow<List<IngredientLine>>(emptyList())
	val ingredientsScaled = combine(_ingredients, _scaleRatio) { ingredients, scale ->
		ingredients.map { item ->
			when (item) {
				is IngredientGroupTitle -> item
				is IngredientItem -> {
					val scaled = item.ingredient.withScaledAmount(scale)
					item.copy(
						ingredient = scaled,
						originalIngredient = if (scale != 1.0) item.ingredient else null
					)
				}
			}
		}
	}

	private val _weighingModeOn = MutableStateFlow(false)
	val weighingModeOn = _weighingModeOn.asStateFlow()
	fun toggleWeighingMode() { _weighingModeOn.update { !it } }

	val ingredientWeight = ingredientsScaled.map { ingredientLines ->
		ingredientLines
			.filterIsInstance<IngredientItem>()
			.filter { it.isSelected && (it.ingredient.substituteGroupId == null || it.ingredient.isActiveSubstitute) }
			.sumOf { it.ingredient.getMass() ?: 0.0 }
	}

	private val _masterIngredients: Flow<List<MasterIngredientEntity>> =
		recipeRepository.getAllActiveMasterIngredients() ?: kotlinx.coroutines.flow.flowOf(emptyList())

	private val _unitAliases: Flow<List<UnitAliasEntity>> =
		recipeRepository.getAllUnitAliases() ?: kotlinx.coroutines.flow.flowOf(emptyList())

	val recipeCostBreakdown: Flow<RecipeCostBreakdown?> = combine(
		_ingredients,
		_recipeYieldValue,
		_scaleRatio,
		_masterIngredients,
		_unitAliases
	) { ingredients: List<IngredientLine>, yield: Double?, scale: Double, masters: List<MasterIngredientEntity>, aliases: List<UnitAliasEntity> ->
		val rawIngredients = ingredients.filterIsInstance<IngredientItem>().map { it.ingredient }
		if (rawIngredients.isEmpty() || masters.isEmpty()) {
			null
		} else {
			val aliasMap = aliases.associateBy { it.aliasName.trim().lowercase(java.util.Locale.ROOT) }
			LiveCostCalculator.calculateRecipeCost(
				ingredients = rawIngredients,
				masterIngredients = masters,
				yield = yield ?: 1.0,
				scaleFactor = scale,
				unitAliases = aliasMap,
				subRecipeResolver = recipeRepository.asSubRecipeResolver(),
				currentRecipeId = recipeId
			)
		}
	}.flowOn(Dispatchers.IO)
	.catch { e ->
		logit { "Error calculating recipe cost breakdown: ${e.message}" }
		emit(null)
	}

	fun selectActiveSubstitute(targetIngredient: com.herohiman.tournant.data.Ingredient) {
		val groupId = targetIngredient.substituteGroupId ?: return
		_ingredients.update { currentLines ->
			currentLines.map { line ->
				if (line is IngredientItem) {
					if (line.ingredient.substituteGroupId == groupId) {
						val isTarget = line.ingredient.item == targetIngredient.item &&
								line.ingredient.amount == targetIngredient.amount &&
								line.ingredient.unit == targetIngredient.unit
						line.copy(ingredient = line.ingredient.copy(isActiveSubstitute = isTarget))
					} else {
						line
					}
				} else {
					line
				}
			}
		}
	}

	val recipe = recipeRepository.getRecipeById(recipeId)
		.map {
			it.toRecipe()
		}
		.onEach { recipe ->
			recipe.ingredients.forEach {
				it.refId?.let { refId ->
					withContext(Dispatchers.IO) {
						it.item = recipeRepository.getRecipeTitleById(refId) ?: it.item
					}
				}
			}
			_ingredients.value = recipe.ingredients.addGroupTitles()
			recipe.yieldValue.let {
				_yieldFromTextField.value = null
				_recipeYieldValue.value = it
			}
		}

	fun toggleChecked(position: Int) {
		_ingredients.update { list ->
			list.mapIndexed { i, ingredient ->
				if (i == position && ingredient is IngredientItem) {
					if (weighingModeOn.value) {
						if (ingredient.ingredient.getMass() == null) {
							viewModelScope.launch { _uiEvents.send(UiEvent.Shrug) }
							ingredient
						} else {
							ingredient.copy(isSelected = !ingredient.isSelected)
						}
					} else {
						ingredient.copy(isChecked = !ingredient.isChecked)
					}
				}
				else
					ingredient
			}
		}
	}

	fun toggleChecked(groupName: String) {
		if (weighingModeOn.value) {
			_ingredients.update { list ->
				val groupSelectable = list.filterIsInstance<IngredientItem>()
					.filter { it.ingredient.group == groupName }
					.any { !it.isSelected && it.ingredient.getMass() != null }
				list.mapIndexed { i, ingredient ->
					if (ingredient is IngredientItem && ingredient.ingredient.group == groupName) {
						ingredient.copy(isSelected = groupSelectable && ingredient.ingredient.getMass() != null)
					} else
						ingredient
				}
			}
		}
	}

	fun scale(newYieldValue: Double) {
		_targetYieldValue.value = newYieldValue
	}

	fun scale(newYieldValue: String) {
		if (newYieldValue.all { it.isDigit() || it == separator } && newYieldValue.count { it == separator } <= 1) {
			newYieldValue.parseLocalFormattedDoubleOrNull()?.let {
				_targetYieldValue.value = it
				_yieldFromTextField.value = newYieldValue
			}
			if (newYieldValue.isEmpty()) {
				_targetYieldValue.value = null
				_yieldFromTextField.value = ""
			}
		}
	}

	fun scale(ingredientPosition: Int, scale: String) {
		val unscaledAmount = (_ingredients.value[ingredientPosition] as? IngredientItem)?.ingredient?.amount
		if (unscaledAmount == null ) {
			logit { "RecipeViewModel::scale($ingredientPosition, $scale): unscaledAmount is null" }
			return
		}
		_targetYieldValue.update {
			_yieldFromTextField.value = null
			scale.toDouble() / unscaledAmount * (_recipeYieldValue.value ?: 1.0)
		}
	}

	fun scaleUp() {
		_targetYieldValue.update {
			_yieldFromTextField.value = null
			(it ?: _recipeYieldValue.value).moreYield()
		}
	}

	fun scaleDown() {
		_targetYieldValue.update {
			_yieldFromTextField.value = null
			(it ?: _recipeYieldValue.value).lessYield()
		}
	}

	fun scaleReset() {
		_targetYieldValue.update {
			_yieldFromTextField.value = null
			_recipeYieldValue.value
		}
	}

	val recipeDates = flow {
		while (true) {
			emit(null)
			delay(MINUTE_IN_MILLIS)
		}
	}.combine(recipe) { _, recipe ->
		Pair(
			recipe.created?.let {
				DateUtils.getRelativeDateTimeString(application, it.time, MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0)
			},
			recipe.modified?.takeIf { it != recipe.created }?.let {
				DateUtils.getRelativeDateTimeString(application, it.time, MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0)
			}
		)
	}

	val dependentRecipes = MutableStateFlow(listOf<RecipeTitleId>())

	init {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				dependentRecipes.emit(recipeRepository.getDependentRecipeIds(setOf(recipeId)).map {
					RecipeTitleId(it, recipeRepository.getRecipeTitleById(it) ?: "")
				})
			}
		}
	}

	fun addPreparation(date: Date) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				recipeRepository.addPreparation(recipeId, date)
				withContext(Dispatchers.Main) {
					Toast.makeText(getApplication(), R.string.done, Toast.LENGTH_SHORT).show()
				}
			}
		}
	}

	fun removePreparation(date: Date) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				recipeRepository.removePreparation(recipeId, date)
			}
		}
	}

}

class RecipeViewModelFactory(private val application: TournantApplication, private val recipeId: Long) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(RecipeViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return RecipeViewModel(application, recipeId) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}

}