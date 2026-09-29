package com.herohiman.tournant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.herohiman.tournant.cost.CostConfigBackupPayload
import com.herohiman.tournant.cost.ImportConfigResult
import com.herohiman.tournant.data.room.BaseUnitType
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.UnitAliasDao
import com.herohiman.tournant.data.room.UnitAliasEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class UnitManagementViewModel(
	private val repository: RecipeRepository,
	coroutineScope: CoroutineScope? = null
) : ViewModel() {

	private val scope = coroutineScope ?: viewModelScope

	private val _searchQuery = MutableStateFlow("")
	val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

	private val _categoryFilter = MutableStateFlow<BaseUnitType?>(null)
	val categoryFilter: StateFlow<BaseUnitType?> = _categoryFilter.asStateFlow()

	val unitAliases: StateFlow<List<UnitAliasEntity>> = combine(_searchQuery, _categoryFilter) { query, category ->
		Pair(query.trim(), category)
	}.flatMapLatest { (trimmedQuery, category) ->
		if (category != null) {
			repository.searchUnitAliasesByCategory(trimmedQuery, category)
		} else if (trimmedQuery.isNotEmpty()) {
			repository.searchUnitAliases(trimmedQuery)
		} else {
			repository.getAllUnitAliases()
		}
	}.flowOn(Dispatchers.IO)
	.stateIn(
		scope = scope,
		started = SharingStarted.Lazily,
		initialValue = emptyList()
	)

	fun onSearchQueryChanged(query: String) {
		_searchQuery.value = query
	}

	fun onCategoryFilterChanged(category: BaseUnitType?) {
		_categoryFilter.value = category
	}

	suspend fun insertOrUpdateUnitAlias(entity: UnitAliasEntity): Long {
		return withContext(Dispatchers.IO) {
			if (entity.id == 0L) {
				repository.insertUnitAlias(entity)
			} else {
				repository.updateUnitAlias(entity)
				entity.id
			}
		}
	}

	suspend fun deleteUnitAlias(entity: UnitAliasEntity) {
		withContext(Dispatchers.IO) {
			repository.deleteUnitAlias(entity)
		}
	}

	suspend fun restoreDefaultUnits(): List<Long> {
		return withContext(Dispatchers.IO) {
			val defaults = UnitAliasDao.getDefaultAliases()
			repository.insertUnitAliases(defaults)
		}
	}

	suspend fun exportUnitConfiguration(): CostConfigBackupPayload {
		return withContext(Dispatchers.IO) {
			repository.exportUnitConfiguration()
		}
	}

	suspend fun importCostConfiguration(payload: CostConfigBackupPayload): ImportConfigResult {
		return withContext(Dispatchers.IO) {
			repository.importCostConfiguration(payload)
		}
	}
}

class UnitManagementViewModelFactory(private val repository: RecipeRepository) : ViewModelProvider.Factory {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(UnitManagementViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return UnitManagementViewModel(repository) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
	}
}
