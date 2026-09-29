package com.herohiman.tournant

import androidx.sqlite.db.SupportSQLiteDatabase
import com.herohiman.tournant.data.room.MasterIngredientDao
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.RecipeDao
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.RecipeRoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class SubRecipeLinkMigrationAndDaoTest {

	private val executedSqlStatements = mutableListOf<String>()

	@Before
	fun setUp() {
		executedSqlStatements.clear()
	}

	@Test
	fun `migration from version 11 to 12 adds linkedRecipeId yieldRatio and index`() {
		val fakeDb = Proxy.newProxyInstance(
			SupportSQLiteDatabase::class.java.classLoader,
			arrayOf(SupportSQLiteDatabase::class.java)
		) { _, method, args ->
			if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
				executedSqlStatements.add(args[0] as String)
			}
			null
		} as SupportSQLiteDatabase

		RecipeRoomDatabase.MIGRATION_11_12.migrate(fakeDb)

		assertEquals(3, executedSqlStatements.size)

		val addLinkedRecipeIdSql = executedSqlStatements[0]
		assertTrue(addLinkedRecipeIdSql.contains("ALTER TABLE `MasterIngredient` ADD COLUMN `linkedRecipeId` INTEGER DEFAULT NULL"))

		val addYieldRatioSql = executedSqlStatements[1]
		assertTrue(addYieldRatioSql.contains("ALTER TABLE `MasterIngredient` ADD COLUMN `yieldRatio` REAL DEFAULT NULL"))

		val createIndexSql = executedSqlStatements[2]
		assertTrue(createIndexSql.contains("CREATE INDEX IF NOT EXISTS `index_MasterIngredient_linkedRecipeId` ON `MasterIngredient` (`linkedRecipeId`)"))
	}

	@Test
	fun `master ingredient entity stores sub recipe linking and yield ratio attributes`() {
		val khoya = MasterIngredientEntity(
			id = 1,
			name = "Khoya (Mawa)",
			unitCost = 0.0, // derived from recipe
			baseUnit = "kg",
			currency = "INR",
			isActive = true,
			linkedRecipeId = 42L,
			yieldRatio = 0.20 // 200g khoya per 1 kg/L milk
		)

		assertEquals(42L, khoya.linkedRecipeId)
		assertEquals(0.20, khoya.yieldRatio ?: 0.0, 0.0001)
		assertEquals("Khoya (Mawa)", khoya.name)
	}

	@Test
	fun `dao and repository query master ingredients by linked recipe id accurately`() = runBlocking {
		val fakeDao = FakeMasterDao()
		val dummyRecipeDao = MinimalRecipeDao()
		val repository = RecipeRepository(dao = dummyRecipeDao, masterIngredientDao = fakeDao)

		repository.insertMasterIngredient(
			MasterIngredientEntity(
				id = 10,
				name = "Khoya",
				unitCost = 0.0,
				baseUnit = "kg",
				linkedRecipeId = 101L,
				yieldRatio = 0.20
			)
		)

		repository.insertMasterIngredient(
			MasterIngredientEntity(
				id = 11,
				name = "Paneer",
				unitCost = 0.0,
				baseUnit = "kg",
				linkedRecipeId = 102L,
				yieldRatio = 0.18
			)
		)

		repository.insertMasterIngredient(
			MasterIngredientEntity(
				id = 12,
				name = "Sugar",
				unitCost = 40.0,
				baseUnit = "kg",
				linkedRecipeId = null,
				yieldRatio = null
			)
		)

		val khoyaItem = repository.getMasterIngredientByLinkedRecipeId(101L)
		assertNotNull(khoyaItem)
		assertEquals("Khoya", khoyaItem?.name)
		assertEquals(0.20, khoyaItem?.yieldRatio ?: 0.0, 0.001)

		val paneerItem = repository.getMasterIngredientByLinkedRecipeId(102L)
		assertNotNull(paneerItem)
		assertEquals("Paneer", paneerItem?.name)

		val missingItem = repository.getMasterIngredientByLinkedRecipeId(999L)
		assertNull(missingItem)

		val linkedList = repository.getMasterIngredientsWithLinkedRecipes()
		assertEquals(2, linkedList.size)
		assertTrue(linkedList.any { it.name == "Khoya" })
		assertTrue(linkedList.any { it.name == "Paneer" })
	}

	private class MinimalRecipeDao : RecipeDao() {
		override fun getRecipeById(id: Long) = kotlinx.coroutines.flow.emptyFlow<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations>()
		override fun getRecipesById(ids: Set<Long>) = emptyList<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations>()
		override fun getReferencedRecipes(recipeIds: Set<Long>) = emptyList<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations>()
		override fun getRecipeTitlesWithIds() = kotlinx.coroutines.flow.flowOf(emptyList<com.herohiman.tournant.data.RecipeTitleId>())
		override fun getRecipeTitleById(id: Long) = ""
		override suspend fun getRecipesUsingMasterIngredient(masterIngredientId: Long, ingredientName: String): List<com.herohiman.tournant.data.RecipeTitleId> = emptyList()
		override fun getRecipeByGourmandId(gourmandId: Int) = null
		override fun getRecipeIdByGourmandId(gourmandId: Long) = null
		override fun getDeprecatedRecipes(gourmandIds: List<Int>) = emptyList<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations>()
		override fun getRecipeDescriptions(query: String, orderedBy: Int, offset: Int, limit: Int, month: Int) = emptyList<com.herohiman.tournant.data.RecipeDescription>()
		override fun getKeywords(id: Long) = emptyList<String>()
		override fun getRecipeCount() = kotlinx.coroutines.flow.flowOf(0)
		override fun getRecipeIds(query: String) = emptyList<Long>()
		override fun getDependentRecipeIds(recipeIds: Set<Long>) = emptyList<Long>()
		override fun getAllCategories() = kotlinx.coroutines.flow.flowOf(emptyList<String>())
		override fun getAllCuisines() = kotlinx.coroutines.flow.flowOf(emptyList<String>())
		override fun getAllKeywords() = kotlinx.coroutines.flow.flowOf(emptyList<String>())
		override fun getCategories(query: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.herohiman.tournant.data.room.StringAndCount>())
		override fun getCuisines(query: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.herohiman.tournant.data.room.StringAndCount>())
		override fun getKeywords(query: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.herohiman.tournant.data.room.StringAndCount>())
		override fun getSources() = kotlinx.coroutines.flow.flowOf(emptyList<String>())
		override fun getYieldUnits() = kotlinx.coroutines.flow.flowOf(emptyList<String>())
		override fun getIngredientItems() = kotlinx.coroutines.flow.flowOf(emptyList<String>())
		override fun getIngredientUnits() = kotlinx.coroutines.flow.flowOf(emptyList<String>())
		override suspend fun getUniqueIngredientNames() = emptyList<String>()
		override suspend fun getPreferredUnitForIngredient(item: String) = null
		override suspend fun insertRecipe(recipe: com.herohiman.tournant.data.room.RecipeEntity) = 1L
		override suspend fun updateRecipe(recipe: com.herohiman.tournant.data.room.RecipeEntity) {}
		override suspend fun deleteRecipe(recipe: com.herohiman.tournant.data.room.RecipeEntity) {}
		override suspend fun deleteRecipesByIds(recipeIds: Set<Long>) {}
		override suspend fun deleteAllRecipes() {}
		override suspend fun insertIngredient(ingredient: com.herohiman.tournant.data.room.IngredientEntity) {}
		override suspend fun updateIngredient(ingredient: com.herohiman.tournant.data.room.IngredientEntity) {}
		override suspend fun deleteIngredient(ingredient: com.herohiman.tournant.data.room.IngredientEntity) {}
		override suspend fun deleteIngredientsNotInList(recipeId: Long, positions: List<Int>) {}
		override suspend fun insertKeyword(preparation: com.herohiman.tournant.data.room.KeywordEntity) = 1L
		override suspend fun deleteKeywordsNotInList(recipeId: Long, positions: List<Int>) {}
		override suspend fun insertPreparationDate(preparation: com.herohiman.tournant.data.room.PreparationEntity) = 1L
		override suspend fun updatePreparationDate(preparation: com.herohiman.tournant.data.room.PreparationEntity) {}
		override suspend fun deletePreparationDate(preparation: com.herohiman.tournant.data.room.PreparationEntity) {}
		override suspend fun deletePreparationDatesNotInList(recipeId: Long, dates: List<Long>) {}
		override suspend fun getPreparation(recipeId: Long, date: Long) = null
		override suspend fun getPreparations(recipeId: Long) = emptyList<com.herohiman.tournant.data.room.PreparationEntity>()
		override suspend fun getMostRecentPreparation(recipeId: Long) = null
		override suspend fun getPreparationsByDateRange(recipeId: Long, startDate: Long, endDate: Long) = emptyList<com.herohiman.tournant.data.room.PreparationEntity>()
		override suspend fun pinRecipe(recipePin: com.herohiman.tournant.data.room.RecipePinEntity) = 1L
		override suspend fun unpinRecipe(recipeId: Long) {}
	}

	private class FakeMasterDao : MasterIngredientDao {
		val map = mutableMapOf<Long, MasterIngredientEntity>()
		private var nextId = 1L

		override fun getAllActiveMasterIngredients(): Flow<List<MasterIngredientEntity>> = flowOf(getAllActiveMasterIngredientsList())
		override fun getAllActiveMasterIngredientsList() = map.values.filter { it.isActive }.sortedBy { it.name }
		override fun getAllMasterIngredients(): Flow<List<MasterIngredientEntity>> = flowOf(getAllMasterIngredientsList())
		override fun getAllMasterIngredientsList() = map.values.sortedBy { it.name }
		override fun getMasterIngredientById(id: Long) = map[id]
		override fun getMasterIngredientByName(name: String) = map.values.firstOrNull { it.name.equals(name, ignoreCase = true) && it.isActive }
		override suspend fun getMasterIngredientByLinkedRecipeId(recipeId: Long): MasterIngredientEntity? {
			return map.values.firstOrNull { it.linkedRecipeId == recipeId && it.isActive }
		}
		override suspend fun getMasterIngredientsWithLinkedRecipes(): List<MasterIngredientEntity> {
			return map.values.filter { it.linkedRecipeId != null && it.isActive }
		}
		override suspend fun insertMasterIngredient(item: MasterIngredientEntity): Long {
			val id = if (item.id == 0L) nextId++ else item.id
			map[id] = item.copy(id = id)
			return id
		}
		override suspend fun insertMasterIngredients(items: List<MasterIngredientEntity>) = items.map { insertMasterIngredient(it) }
		override suspend fun updateMasterIngredient(item: MasterIngredientEntity) { map[item.id] = item }
		override suspend fun softDeleteMasterIngredient(id: Long) { map[id]?.let { map[id] = it.copy(isActive = false) } }
		override suspend fun restoreMasterIngredient(id: Long) { map[id]?.let { map[id] = it.copy(isActive = true) } }
		override suspend fun hardDeleteMasterIngredient(id: Long) { map.remove(id) }
	}
}
