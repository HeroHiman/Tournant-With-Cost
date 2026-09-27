package com.herohiman.tournant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.herohiman.tournant.addGroupTitles
import com.herohiman.tournant.data.IngredientLine
import com.herohiman.tournant.data.Recipe
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.hideGroupTitles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipeEditingViewModel(private val recipeRepository: RecipeRepository, private val recipeId: Long) : ViewModel() {

	val recipe = MutableStateFlow(Recipe(title = ""))
	val ingredients = MutableStateFlow(mutableListOf<IngredientLine>())
	val titlesWithIds = recipeRepository.getRecipeTitlesWithIds()
	val categoryStrings = recipeRepository.getAllCategories()
	val cuisineStrings = recipeRepository.getAllCuisines()
	val sourceStrings = recipeRepository.getSources()
	val yieldUnitStrings = recipeRepository.getYieldUnits()
	val ingredientItemSuggestions = recipeRepository.getIngredientItems()
	val ingredientUnitSuggestions = recipeRepository.getIngredientUnits()

	private var originalRecipe: Recipe? = null
	private var originalIngredients: List<IngredientLine>? = null

	init {
		if (recipeId != 0L) {
			viewModelScope.launch {
				withContext(Dispatchers.IO) {
					recipeRepository.getRecipeById(recipeId).map { it.toRecipe() }.collectLatest { loadedRecipe ->
						if (originalRecipe == null) {
							originalRecipe = loadedRecipe.deepCopy()
							originalIngredients = loadedRecipe.ingredients.addGroupTitles().map { it.deepCopy() }
						}
						recipe.emit(loadedRecipe)
						ingredients.emit(loadedRecipe.ingredients.addGroupTitles())
					}
				}
			}
		} else {
			val initialRecipe = Recipe(title = "")
			originalRecipe = initialRecipe.deepCopy()
			originalIngredients = emptyList()
			recipe.value = initialRecipe
			ingredients.value = mutableListOf()
		}
	}

	fun canRevert(): Boolean = originalRecipe != null

	fun revert(): Boolean {
		val orig = originalRecipe ?: return false
		val origIngr = originalIngredients ?: return false
		recipe.value = orig.deepCopy()
		ingredients.value = origIngr.map { it.deepCopy() }.toMutableList()
		return true
	}

	fun hasUnsavedChanges(): Boolean {
		val orig = originalRecipe ?: return false
		val current = recipe.value
		if (current.title != orig.title) return true
		if (current.description != orig.description) return true
		if (current.instructions != orig.instructions) return true
		if (current.notes != orig.notes) return true
		if (current.category != orig.category) return true
		if (current.cuisine != orig.cuisine) return true
		if (current.source != orig.source) return true
		if (current.link != orig.link) return true
		if (current.rating != orig.rating) return true
		if (current.preptime != orig.preptime) return true
		if (current.cooktime != orig.cooktime) return true
		if (current.yieldValue != orig.yieldValue) return true
		if (current.yieldUnit != orig.yieldUnit) return true
		if (current.season != orig.season) return true
		if (current.language != orig.language) return true
		if (current.keywords != orig.keywords) return true

		val origIngr = originalIngredients ?: emptyList()
		if (ingredients.value != origIngr) return true

		return false
	}

	sealed class ValidationResult {
		object Valid : ValidationResult()
		data class Invalid(val errorType: ValidationError) : ValidationResult()
	}

	enum class ValidationError {
		EMPTY_TITLE,
		INVALID_YIELD
	}

	fun validate(): ValidationResult {
		val current = recipe.value
		if (current.title.trim().isBlank()) {
			return ValidationResult.Invalid(ValidationError.EMPTY_TITLE)
		}
		current.yieldValue?.let {
			if (it <= 0.0 || it.isNaN() || it.isInfinite()) {
				return ValidationResult.Invalid(ValidationError.INVALID_YIELD)
			}
		}
		return ValidationResult.Valid
	}

	var savedWithId = MutableStateFlow(0L)

	fun saveRecipe() {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				val ingredientList = ingredients.value.hideGroupTitles().onEach { it.removeEmptyValues() }
				val id = recipeRepository.upsertSingleRecipe(
					recipe.value.apply {
						processModifications()
						ingredients.clear()
						ingredients.addAll(ingredientList)
					}.toRecipeWithIngredientsAndPreparations()
				)
				savedWithId.emit(id)
			}
		}
	}

}

class RecipeEditingViewModelFactory(private val recipeRepository: RecipeRepository, private val recipeId: Long) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(RecipeEditingViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return RecipeEditingViewModel(recipeRepository, recipeId) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}

}