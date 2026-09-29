package com.herohiman.tournant

import com.herohiman.tournant.data.RecipeDescription
import com.herohiman.tournant.data.RecipeTitleId
import com.herohiman.tournant.data.room.StringAndCount
import com.herohiman.tournant.data.room.BaseUnitType
import com.herohiman.tournant.data.room.IngredientEntity
import com.herohiman.tournant.data.room.KeywordEntity
import com.herohiman.tournant.data.room.MasterIngredientDao
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.PreparationEntity
import com.herohiman.tournant.data.room.RecipeDao
import com.herohiman.tournant.data.room.RecipeEntity
import com.herohiman.tournant.data.room.RecipePinEntity
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations
import com.herohiman.tournant.data.room.UnitAliasDao
import com.herohiman.tournant.data.room.UnitAliasEntity
import com.herohiman.tournant.ui.MasterCostListViewModel
import com.herohiman.tournant.ui.UnitManagementViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StateManagementSearchViewModelTest {

	private class StubRecipeDao : RecipeDao() {
		override fun getRecipeById(id: Long): Flow<RecipeWithIngredientsAndPreparations> = emptyFlow()
		override fun getRecipesById(ids: Set<Long>): List<RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getReferencedRecipes(recipeIds: Set<Long>): List<RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getRecipeTitlesWithIds(): Flow<List<RecipeTitleId>> = flowOf(emptyList())
		override fun getRecipeTitleById(id: Long): String = ""
		override suspend fun getRecipesUsingMasterIngredient(masterIngredientId: Long, ingredientName: String): List<RecipeTitleId> = emptyList()
		override fun getRecipeByGourmandId(gourmandId: Int): RecipeWithIngredientsAndPreparations? = null
		override fun getRecipeIdByGourmandId(gourmandId: Long): Long? = null
		override fun getDeprecatedRecipes(gourmandIds: List<Int>): List<RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getRecipeDescriptions(query: String, orderedBy: Int, offset: Int, limit: Int, month: Int): List<RecipeDescription> = emptyList()
		override fun getKeywords(id: Long): List<String> = emptyList()
		override fun getRecipeCount(): Flow<Int> = flowOf(0)
		override fun getRecipeIds(query: String): List<Long> = emptyList()
		override fun getDependentRecipeIds(recipeIds: Set<Long>): List<Long> = emptyList()
		override fun getAllCategories(): Flow<List<String>> = flowOf(emptyList())
		override fun getAllCuisines(): Flow<List<String>> = flowOf(emptyList())
		override fun getAllKeywords(): Flow<List<String>> = flowOf(emptyList())
		override fun getCategories(query: String): Flow<List<StringAndCount>> = flowOf(emptyList())
		override fun getCuisines(query: String): Flow<List<StringAndCount>> = flowOf(emptyList())
		override fun getKeywords(query: String): Flow<List<StringAndCount>> = flowOf(emptyList())
		override fun getSources(): Flow<List<String>> = flowOf(emptyList())
		override fun getYieldUnits(): Flow<List<String>> = flowOf(emptyList())
		override fun getIngredientItems(): Flow<List<String>> = flowOf(emptyList())
		override fun getIngredientUnits(): Flow<List<String>> = flowOf(emptyList())
		override suspend fun getUniqueIngredientNames(): List<String> = emptyList()
		override suspend fun getPreferredUnitForIngredient(item: String): String? = null

		override suspend fun insertRecipe(recipe: RecipeEntity): Long = 1L
		override suspend fun updateRecipe(recipe: RecipeEntity) {}
		override suspend fun deleteRecipe(recipe: RecipeEntity) {}
		override suspend fun deleteRecipesByIds(recipeIds: Set<Long>) {}
		override suspend fun deleteAllRecipes() {}
		override suspend fun insertIngredient(ingredient: IngredientEntity) {}
		override suspend fun updateIngredient(ingredient: IngredientEntity) {}
		override suspend fun deleteIngredient(ingredient: IngredientEntity) {}
		override suspend fun deleteIngredientsNotInList(recipeId: Long, positions: List<Int>) {}
		override suspend fun insertKeyword(preparation: KeywordEntity): Long = 1L
		override suspend fun deleteKeywordsNotInList(recipeId: Long, positions: List<Int>) {}
		override suspend fun insertPreparationDate(preparation: PreparationEntity): Long = 1L
		override suspend fun updatePreparationDate(preparation: PreparationEntity) {}
		override suspend fun deletePreparationDate(preparation: PreparationEntity) {}
		override suspend fun deletePreparationDatesNotInList(recipeId: Long, dates: List<Long>) {}
		override suspend fun getPreparation(recipeId: Long, date: Long): PreparationEntity? = null
		override suspend fun getPreparations(recipeId: Long): List<PreparationEntity> = emptyList()
		override suspend fun getMostRecentPreparation(recipeId: Long): PreparationEntity? = null
		override suspend fun getPreparationsByDateRange(recipeId: Long, startDate: Long, endDate: Long): List<PreparationEntity> = emptyList()
		override suspend fun pinRecipe(recipePin: RecipePinEntity): Long = 1L
		override suspend fun unpinRecipe(recipeId: Long) {}
	}

	private class TestMasterIngredientDao : MasterIngredientDao {
		val items = mutableListOf<MasterIngredientEntity>()
		private var nextId = 1L

		override fun getAllActiveMasterIngredients(): Flow<List<MasterIngredientEntity>> =
			flowOf(items.filter { it.isActive }.sortedBy { it.name })

		override fun getAllActiveMasterIngredientsList(): List<MasterIngredientEntity> =
			items.filter { it.isActive }.sortedBy { it.name }

		override fun getAllMasterIngredients(): Flow<List<MasterIngredientEntity>> =
			flowOf(items.sortedBy { it.name })

		override fun getAllMasterIngredientsList(): List<MasterIngredientEntity> =
			items.sortedBy { it.name }

		override fun searchMasterIngredients(query: String): Flow<List<MasterIngredientEntity>> =
			flowOf(searchMasterIngredientsList(query))

		override fun searchMasterIngredientsList(query: String): List<MasterIngredientEntity> =
			items.filter { it.name.contains(query, ignoreCase = true) || it.category?.contains(query, ignoreCase = true) == true }
				.sortedBy { it.name }

		override fun searchActiveMasterIngredients(query: String): Flow<List<MasterIngredientEntity>> =
			flowOf(items.filter { it.isActive && (it.name.contains(query, ignoreCase = true) || it.category?.contains(query, ignoreCase = true) == true) }.sortedBy { it.name })

		override fun getMasterIngredientById(id: Long): MasterIngredientEntity? =
			items.firstOrNull { it.id == id }

		override fun getMasterIngredientByName(name: String): MasterIngredientEntity? =
			items.firstOrNull { it.name.equals(name, ignoreCase = true) }

		override suspend fun insertMasterIngredient(item: MasterIngredientEntity): Long {
			val id = if (item.id == 0L) (items.maxOfOrNull { it.id } ?: 0L) + 1 else item.id
			val entity = item.copy(id = id)
			items.add(entity)
			return id
		}

		override suspend fun insertMasterIngredients(items: List<MasterIngredientEntity>): List<Long> =
			items.map { insertMasterIngredient(it) }

		override suspend fun updateMasterIngredient(item: MasterIngredientEntity) {
			val idx = items.indexOfFirst { it.id == item.id }
			if (idx != -1) items[idx] = item
		}

		override suspend fun softDeleteMasterIngredient(id: Long) {
			val idx = items.indexOfFirst { it.id == id }
			if (idx != -1) items[idx] = items[idx].copy(isActive = false)
		}

		override suspend fun restoreMasterIngredient(id: Long) {
			val idx = items.indexOfFirst { it.id == id }
			if (idx != -1) items[idx] = items[idx].copy(isActive = true)
		}

		override suspend fun hardDeleteMasterIngredient(id: Long) {
			items.removeAll { it.id == id }
		}

		override suspend fun getMasterIngredientByLinkedRecipeId(recipeId: Long): MasterIngredientEntity? =
			items.firstOrNull { it.linkedRecipeId == recipeId }

		override suspend fun getMasterIngredientsWithLinkedRecipes(): List<MasterIngredientEntity> =
			items.filter { it.linkedRecipeId != null }
	}

	private class TestUnitAliasDao : UnitAliasDao {
		val items = mutableListOf<UnitAliasEntity>()

		override fun getAllUnitAliases(): Flow<List<UnitAliasEntity>> =
			flowOf(items.sortedBy { it.aliasName })

		override fun getAllUnitAliasesList(): List<UnitAliasEntity> =
			items.sortedBy { it.aliasName }

		override suspend fun getAliasByName(name: String): UnitAliasEntity? =
			items.firstOrNull { it.aliasName.equals(name, ignoreCase = true) }

		override suspend fun getAliasById(id: Long): UnitAliasEntity? =
			items.firstOrNull { it.id == id }

		override suspend fun getAliasesByBaseUnit(baseUnit: BaseUnitType): List<UnitAliasEntity> =
			items.filter { it.baseUnit == baseUnit }.sortedBy { it.aliasName }

		override suspend fun insertAlias(alias: UnitAliasEntity): Long {
			val id = if (alias.id == 0L) (items.maxOfOrNull { it.id } ?: 0L) + 1 else alias.id
			items.add(alias.copy(id = id))
			return id
		}

		override suspend fun insertAliases(aliases: List<UnitAliasEntity>): List<Long> =
			aliases.map { insertAlias(it) }

		override suspend fun updateAlias(alias: UnitAliasEntity) {
			val idx = items.indexOfFirst { it.id == alias.id }
			if (idx != -1) items[idx] = alias
		}

		override suspend fun deleteAlias(alias: UnitAliasEntity) {
			items.removeAll { it.id == alias.id }
		}

		override suspend fun deleteAliasById(id: Long) {
			items.removeAll { it.id == id }
		}

		override suspend fun getUnitAliasCount(): Int = items.size

		override fun searchUnitAliases(query: String): Flow<List<UnitAliasEntity>> =
			flowOf(searchUnitAliasesList(query))

		override fun searchUnitAliasesList(query: String): List<UnitAliasEntity> =
			items.filter { it.aliasName.contains(query, ignoreCase = true) }.sortedBy { it.aliasName }

		override fun searchUnitAliasesByCategory(query: String, baseUnit: BaseUnitType): Flow<List<UnitAliasEntity>> =
			flowOf(searchUnitAliasesByCategoryList(query, baseUnit))

		override fun searchUnitAliasesByCategoryList(query: String, baseUnit: BaseUnitType): List<UnitAliasEntity> =
			items.filter { it.baseUnit == baseUnit && it.aliasName.contains(query, ignoreCase = true) }.sortedBy { it.aliasName }
	}

	@Test
	fun `MasterCostListViewModel updates search query and produces filtered ingredient list`() = runBlocking<Unit> {
		val masterDao = TestMasterIngredientDao()
		masterDao.items.addAll(
			listOf(
				MasterIngredientEntity(id = 1, name = "काजू", category = "Nuts", unitCost = 700.0, baseUnit = "kg"),
				MasterIngredientEntity(id = 2, name = "काला नमक", category = "Spices", unitCost = 50.0, baseUnit = "kg"),
				MasterIngredientEntity(id = 3, name = "हल्दी", category = "Spices", unitCost = 120.0, baseUnit = "kg")
			)
		)

		val repository = RecipeRepository(
			dao = StubRecipeDao(),
			masterIngredientDao = masterDao
		)

		val testScope = CoroutineScope(Dispatchers.IO)
		val viewModel = MasterCostListViewModel(repository, testScope)

		// 1. Initial empty query state
		assertEquals("", viewModel.searchQuery.value)
		val initialList = viewModel.ingredients.filter { it.size == 3 }.first()
		assertEquals(3, initialList.size)

		// 2. Query for Hindi "का"
		viewModel.onSearchQueryChanged("का")
		assertEquals("का", viewModel.searchQuery.value)
		val filteredList = viewModel.ingredients.filter { it.size == 2 && it.all { item -> item.name.contains("का") } }.first()
		assertEquals(2, filteredList.size)
		assertTrue(filteredList.any { it.name == "काजू" })
		assertTrue(filteredList.any { it.name == "काला नमक" })

		// 3. Query for Category "Spices"
		viewModel.onSearchQueryChanged("Spices")
		val categoryFilteredList = viewModel.ingredients.filter { it.size == 2 && it.all { item -> item.category == "Spices" } }.first()
		assertEquals(2, categoryFilteredList.size)
		assertTrue(categoryFilteredList.any { it.name == "काला नमक" })
		assertTrue(categoryFilteredList.any { it.name == "हल्दी" })

		// 4. Toggle active state
		val entity = masterDao.items.first { it.name == "काजू" }
		assertTrue(entity.isActive)
		viewModel.toggleIngredientActive(entity)
		delay(100)
		val updatedEntity = masterDao.getMasterIngredientById(1)
		assertFalse(updatedEntity!!.isActive)
		testScope.cancel()
	}

	@Test
	fun `UnitManagementViewModel combines search query and category filter reactively`() = runBlocking<Unit> {
		val unitDao = TestUnitAliasDao()
		unitDao.items.addAll(
			listOf(
				UnitAliasEntity(id = 1, aliasName = "किलो", baseUnit = BaseUnitType.KG, conversionFactor = 1.0),
				UnitAliasEntity(id = 2, aliasName = "डब्बा 15kg", baseUnit = BaseUnitType.KG, conversionFactor = 15.0),
				UnitAliasEntity(id = 3, aliasName = "बड़ा चम्मच", baseUnit = BaseUnitType.LITER, conversionFactor = 0.015),
				UnitAliasEntity(id = 4, aliasName = "छोटा चम्मच", baseUnit = BaseUnitType.LITER, conversionFactor = 0.005),
				UnitAliasEntity(id = 5, aliasName = "दुकान पैकेट", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0)
			)
		)

		val repository = RecipeRepository(
			dao = StubRecipeDao(),
			unitAliasDao = unitDao
		)

		val testScope = CoroutineScope(Dispatchers.IO)
		val viewModel = UnitManagementViewModel(repository, testScope)

		// 1. Initial state (no query, no category filter)
		assertEquals("", viewModel.searchQuery.value)
		assertEquals(null, viewModel.categoryFilter.value)
		val initialList = viewModel.unitAliases.filter { it.size == 5 }.first()
		assertEquals(5, initialList.size)

		// 2. Filter by Category only (BaseUnitType.KG)
		viewModel.onCategoryFilterChanged(BaseUnitType.KG)
		assertEquals(BaseUnitType.KG, viewModel.categoryFilter.value)
		val kgList = viewModel.unitAliases.filter { it.size == 2 && it.all { item -> item.baseUnit == BaseUnitType.KG } }.first()
		assertEquals(2, kgList.size)
		assertTrue(kgList.all { it.baseUnit == BaseUnitType.KG })

		// 3. Combined query + category filter (Category: KG, Query: "डब्बा")
		viewModel.onSearchQueryChanged("डब्बा")
		assertEquals("डब्बा", viewModel.searchQuery.value)
		val filteredKgList = viewModel.unitAliases.filter { it.size == 1 && it[0].aliasName == "डब्बा 15kg" }.first()
		assertEquals(1, filteredKgList.size)
		assertEquals("डब्बा 15kg", filteredKgList[0].aliasName)

		// 4. Clear category filter and search across all categories for "चम्मच"
		viewModel.onCategoryFilterChanged(null)
		viewModel.onSearchQueryChanged("चम्मच")
		val spoonList = viewModel.unitAliases.filter { it.size == 2 && it.all { item -> item.aliasName.contains("चम्मच") } }.first()
		assertEquals(2, spoonList.size)
		assertTrue(spoonList.all { it.aliasName.contains("चम्मच") })

		// 5. Add / Update / Delete
		val newAlias = UnitAliasEntity(id = 0, aliasName = "pinch", baseUnit = BaseUnitType.KG, conversionFactor = 0.0005)
		val generatedId = viewModel.insertOrUpdateUnitAlias(newAlias)
		assertTrue(generatedId > 0)
		assertEquals(6, unitDao.getUnitAliasCount())

		viewModel.deleteUnitAlias(newAlias.copy(id = generatedId))
		assertEquals(5, unitDao.getUnitAliasCount())
		testScope.cancel()
	}
}
