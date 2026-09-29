package com.herohiman.tournant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.herohiman.tournant.cost.CostConfigBackupPayload
import com.herohiman.tournant.cost.ImportConfigResult
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.RecipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class MasterCostListViewModel(
	private val repository: RecipeRepository,
	coroutineScope: CoroutineScope? = null
) : ViewModel() {

	private val scope = coroutineScope ?: viewModelScope

	private val _searchQuery = MutableStateFlow("")
	val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

	val ingredients: StateFlow<List<MasterIngredientEntity>> = _searchQuery
		.flatMapLatest { query ->
			val trimmed = query.trim()
			if (trimmed.isEmpty()) {
				repository.getAllMasterIngredients()
			} else {
				repository.searchMasterIngredients(trimmed)
			}
		}
		.flowOn(Dispatchers.IO)
		.stateIn(
			scope = scope,
			started = SharingStarted.Lazily,
			initialValue = emptyList()
		)

	fun onSearchQueryChanged(query: String) {
		_searchQuery.value = query
	}

	fun toggleIngredientActive(entity: MasterIngredientEntity) {
		scope.launch(Dispatchers.IO) {
			if (entity.isActive) {
				repository.softDeleteMasterIngredient(entity.id)
			} else {
				repository.restoreMasterIngredient(entity.id)
			}
		}
	}

	suspend fun autoFetchIngredientsFromRecipes(): Int {
		return withContext(Dispatchers.IO) {
			repository.syncIngredientsFromRecipes()
		}
	}

	suspend fun exportCostConfiguration(): CostConfigBackupPayload {
		return withContext(Dispatchers.IO) {
			repository.exportCostConfiguration()
		}
	}

	suspend fun importCostConfiguration(payload: CostConfigBackupPayload): ImportConfigResult {
		return withContext(Dispatchers.IO) {
			repository.importCostConfiguration(payload)
		}
	}
}

class MasterCostListViewModelFactory(private val repository: RecipeRepository) : ViewModelProvider.Factory {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(MasterCostListViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return MasterCostListViewModel(repository) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
	}
}
