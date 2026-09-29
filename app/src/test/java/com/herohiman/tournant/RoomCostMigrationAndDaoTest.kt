package com.herohiman.tournant

import androidx.sqlite.db.SupportSQLiteDatabase
import com.herohiman.tournant.cost.CostPrivacyManager
import com.herohiman.tournant.data.RecipeDescription
import com.herohiman.tournant.data.RecipeTitleId
import com.herohiman.tournant.data.room.IngredientEntity
import com.herohiman.tournant.data.room.KeywordEntity
import com.herohiman.tournant.data.room.MasterIngredientDao
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.PreparationEntity
import com.herohiman.tournant.data.room.RecipeDao
import com.herohiman.tournant.data.room.RecipeEntity
import com.herohiman.tournant.data.room.RecipePinEntity
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.RecipeRoomDatabase
import com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations
import com.herohiman.tournant.data.room.StringAndCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class RoomCostMigrationAndDaoTest {

	private val executedSqlStatements = mutableListOf<String>()

	@Before
	fun setUp() {
		executedSqlStatements.clear()
		CostPrivacyManager.clearInMemoryOverride()
	}

	@After
	fun tearDown() {
		CostPrivacyManager.clearInMemoryOverride()
	}

	@Test
	fun `migration from version 8 to 9 creates MasterIngredient table and index`() {
		val fakeDb = Proxy.newProxyInstance(
			SupportSQLiteDatabase::class.java.classLoader,
			arrayOf(SupportSQLiteDatabase::class.java)
		) { _, method, args ->
			if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
				executedSqlStatements.add(args[0] as String)
			}
			null
		} as SupportSQLiteDatabase

		RecipeRoomDatabase.MIGRATION_8_9.migrate(fakeDb)

		assertEquals(2, executedSqlStatements.size)

		val createTableSql = executedSqlStatements[0]
		assertTrue(createTableSql.contains("CREATE TABLE IF NOT EXISTS `MasterIngredient`"))
		assertTrue(createTableSql.contains("`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
		assertTrue(createTableSql.contains("`name` TEXT NOT NULL"))
		assertTrue(createTableSql.contains("`unitCost` REAL NOT NULL"))
		assertTrue(createTableSql.contains("`baseUnit` TEXT NOT NULL"))
		assertTrue(createTableSql.contains("`currency` TEXT NOT NULL DEFAULT 'USD'"))
		assertTrue(createTableSql.contains("`isActive` INTEGER NOT NULL DEFAULT 1"))
		assertTrue(createTableSql.contains("`lastUpdated` INTEGER NOT NULL"))

		val createIndexSql = executedSqlStatements[1]
		assertTrue(createIndexSql.contains("CREATE INDEX IF NOT EXISTS `index_MasterIngredient_name` ON `MasterIngredient` (`name`)"))
	}

	@Test
	fun `repository delegates active master ingredients correctly`() = runBlocking {
		val fakeDao = FakeMasterIngredientDao()
		val fakeRecipeDao = TestRecipeDao()

		val repository = RecipeRepository(fakeRecipeDao, fakeDao)

		repository.insertMasterIngredient(
			MasterIngredientEntity(id = 1, name = "Flour", unitCost = 0.002, baseUnit = "g", isActive = true)
		)
		repository.insertMasterIngredient(
			MasterIngredientEntity(id = 2, name = "Inactive Spice", unitCost = 0.50, baseUnit = "g", isActive = false)
		)

		val activeList = repository.getAllActiveMasterIngredientsList()
		assertEquals(1, activeList.size)
		assertEquals("Flour", activeList[0].name)

		val allList = repository.getAllMasterIngredientsList()
		assertEquals(2, allList.size)
	}

	@Test
	fun `soft deletion updates active status to false`() = runBlocking {
		val fakeDao = FakeMasterIngredientDao()
		val fakeRecipeDao = TestRecipeDao()

		val repository = RecipeRepository(fakeRecipeDao, fakeDao)

		repository.insertMasterIngredient(
			MasterIngredientEntity(id = 10, name = "Butter", unitCost = 4.0, baseUnit = "lb", isActive = true)
		)

		assertEquals(1, repository.getAllActiveMasterIngredientsList().size)

		// Soft delete
		repository.softDeleteMasterIngredient(10)

		val activeAfterDelete = repository.getAllActiveMasterIngredientsList()
		assertEquals(0, activeAfterDelete.size)

		val allAfterDelete = repository.getAllMasterIngredientsList()
		assertEquals(1, allAfterDelete.size)
		assertFalse(allAfterDelete[0].isActive)

		// Restore
		repository.restoreMasterIngredient(10)
		assertEquals(1, repository.getAllActiveMasterIngredientsList().size)
		assertTrue(repository.getAllActiveMasterIngredientsList()[0].isActive)
	}

	@Test
	fun `cost privacy manager manages in memory state`() {
		assertFalse(CostPrivacyManager.isPrivacyModeEnabled())

		CostPrivacyManager.setInMemoryOverride(true)
		assertTrue(CostPrivacyManager.isPrivacyModeEnabled())

		CostPrivacyManager.setInMemoryOverride(false)
		assertFalse(CostPrivacyManager.isPrivacyModeEnabled())
	}

	private class TestRecipeDao : RecipeDao() {
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

	private class FakeMasterIngredientDao : MasterIngredientDao {
		private val map = mutableMapOf<Long, MasterIngredientEntity>()
		private var nextId = 1L

		override fun getAllActiveMasterIngredients(): Flow<List<MasterIngredientEntity>> {
			return flowOf(getAllActiveMasterIngredientsList())
		}

		override fun getAllActiveMasterIngredientsList(): List<MasterIngredientEntity> {
			return map.values.filter { it.isActive }.sortedBy { it.name }
		}

		override fun getAllMasterIngredients(): Flow<List<MasterIngredientEntity>> {
			return flowOf(getAllMasterIngredientsList())
		}

		override fun getAllMasterIngredientsList(): List<MasterIngredientEntity> {
			return map.values.sortedBy { it.name }
		}

		override fun getMasterIngredientById(id: Long): MasterIngredientEntity? {
			return map[id]
		}

		override fun getMasterIngredientByName(name: String): MasterIngredientEntity? {
			return map.values.firstOrNull { it.name.equals(name, ignoreCase = true) && it.isActive }
		}

		override suspend fun insertMasterIngredient(item: MasterIngredientEntity): Long {
			val id = if (item.id == 0L) nextId++ else item.id
			val entity = item.copy(id = id)
			map[id] = entity
			return id
		}

		override suspend fun insertMasterIngredients(items: List<MasterIngredientEntity>): List<Long> {
			return items.map { insertMasterIngredient(it) }
		}

		override suspend fun updateMasterIngredient(item: MasterIngredientEntity) {
			map[item.id] = item
		}

		override suspend fun softDeleteMasterIngredient(id: Long) {
			map[id]?.let {
				map[id] = it.copy(isActive = false)
			}
		}

		override suspend fun restoreMasterIngredient(id: Long) {
			map[id]?.let {
				map[id] = it.copy(isActive = true)
			}
		}

		override suspend fun hardDeleteMasterIngredient(id: Long) {
			map.remove(id)
		}

		override suspend fun getMasterIngredientByLinkedRecipeId(recipeId: Long): MasterIngredientEntity? {
			return map.values.firstOrNull { it.linkedRecipeId == recipeId && it.isActive }
		}

		override suspend fun getMasterIngredientsWithLinkedRecipes(): List<MasterIngredientEntity> {
			return map.values.filter { it.linkedRecipeId != null && it.isActive }
		}

		override fun searchMasterIngredients(query: String): Flow<List<MasterIngredientEntity>> {
			return flowOf(searchMasterIngredientsList(query))
		}

		override fun searchActiveMasterIngredients(query: String): Flow<List<MasterIngredientEntity>> {
			return flowOf(map.values.filter { it.isActive && (it.name.contains(query, ignoreCase = true) || it.category?.contains(query, ignoreCase = true) == true) }.sortedBy { it.name })
		}

		override fun searchMasterIngredientsList(query: String): List<MasterIngredientEntity> {
			return map.values.filter { it.name.contains(query, ignoreCase = true) || it.category?.contains(query, ignoreCase = true) == true }.sortedBy { it.name }
		}
	}
}
