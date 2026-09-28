package com.herohiman.tournant

import androidx.sqlite.db.SupportSQLiteDatabase
import com.herohiman.tournant.data.room.BaseUnitType
import com.herohiman.tournant.data.room.RecipeDao
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.RecipeRoomDatabase
import com.herohiman.tournant.data.room.UnitAliasDao
import com.herohiman.tournant.data.room.UnitAliasEntity
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

class UnitAliasMigrationAndDaoTest {

	private val executedSqlStatements = mutableListOf<String>()

	@Before
	fun setUp() {
		executedSqlStatements.clear()
	}

	@Test
	fun `migration from version 9 to 10 creates UnitAlias table and unique index`() {
		val fakeDb = Proxy.newProxyInstance(
			SupportSQLiteDatabase::class.java.classLoader,
			arrayOf(SupportSQLiteDatabase::class.java)
		) { _, method, args ->
			if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
				executedSqlStatements.add(args[0] as String)
			}
			null
		} as SupportSQLiteDatabase

		RecipeRoomDatabase.MIGRATION_9_10.migrate(fakeDb)

		assertEquals(2, executedSqlStatements.size)

		val createTableSql = executedSqlStatements[0]
		assertTrue(createTableSql.contains("CREATE TABLE IF NOT EXISTS `UnitAlias`"))
		assertTrue(createTableSql.contains("`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
		assertTrue(createTableSql.contains("`aliasName` TEXT NOT NULL"))
		assertTrue(createTableSql.contains("`baseUnit` TEXT NOT NULL"))
		assertTrue(createTableSql.contains("`conversionFactor` REAL NOT NULL"))

		val createIndexSql = executedSqlStatements[1]
		assertTrue(createIndexSql.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_UnitAlias_aliasName` ON `UnitAlias` (`aliasName`)"))
	}

	@Test
	fun `default aliases contain essential English and Hindi units across all three base categories`() {
		val defaults = UnitAliasDao.getDefaultAliases()
		assertTrue(defaults.size >= 30)

		val kgAliases = defaults.filter { it.baseUnit == BaseUnitType.KG }
		val literAliases = defaults.filter { it.baseUnit == BaseUnitType.LITER }
		val countAliases = defaults.filter { it.baseUnit == BaseUnitType.COUNT }

		assertTrue(kgAliases.isNotEmpty())
		assertTrue(literAliases.isNotEmpty())
		assertTrue(countAliases.isNotEmpty())

		// Verify key Hindi and English aliases
		assertTrue(kgAliases.any { it.aliasName == "ग्राम" && it.conversionFactor == 0.001 })
		assertTrue(kgAliases.any { it.aliasName == "किलो" && it.conversionFactor == 1.0 })
		assertTrue(kgAliases.any { it.aliasName == "g" && it.conversionFactor == 0.001 })
		assertTrue(kgAliases.any { it.aliasName == "kg" && it.conversionFactor == 1.0 })

		assertTrue(literAliases.any { it.aliasName == "लीटर" && it.conversionFactor == 1.0 })
		assertTrue(literAliases.any { it.aliasName == "मिली" && it.conversionFactor == 0.001 })
		assertTrue(literAliases.any { it.aliasName == "ml" && it.conversionFactor == 0.001 })
		assertTrue(literAliases.any { it.aliasName == "l" && it.conversionFactor == 1.0 })

		assertTrue(countAliases.any { it.aliasName == "नग" && it.conversionFactor == 1.0 })
		assertTrue(countAliases.any { it.aliasName == "दर्जन" && it.conversionFactor == 12.0 })
		assertTrue(countAliases.any { it.aliasName == "unit" && it.conversionFactor == 1.0 })
	}

	@Test
	fun `unit alias dao performs case insensitive lookup and crud operations`() = runBlocking {
		val fakeDao = FakeUnitAliasDao()

		fakeDao.insertAlias(UnitAliasEntity(id = 1, aliasName = "Gram", baseUnit = BaseUnitType.KG, conversionFactor = 0.001))
		fakeDao.insertAlias(UnitAliasEntity(id = 2, aliasName = "मिली", baseUnit = BaseUnitType.LITER, conversionFactor = 0.001))

		// Case-insensitive lookup
		val lookedUpGram = fakeDao.getAliasByName("gram")
		assertNotNull(lookedUpGram)
		assertEquals(BaseUnitType.KG, lookedUpGram?.baseUnit)
		assertEquals(0.001, lookedUpGram?.conversionFactor ?: 0.0, 0.00001)

		// Hindi lookup
		val lookedUpMilli = fakeDao.getAliasByName("मिली")
		assertNotNull(lookedUpMilli)
		assertEquals(BaseUnitType.LITER, lookedUpMilli?.baseUnit)

		// Missing lookup
		assertNull(fakeDao.getAliasByName("unknown_unit"))

		// Update
		fakeDao.updateAlias(lookedUpGram!!.copy(conversionFactor = 0.002))
		assertEquals(0.002, fakeDao.getAliasByName("gram")?.conversionFactor ?: 0.0, 0.00001)

		// Delete
		fakeDao.deleteAliasById(1)
		assertNull(fakeDao.getAliasByName("gram"))
	}

	@Test
	fun `repository seeds default unit aliases when table is empty and preserves on repeat calls`() = runBlocking {
		val fakeDao = FakeUnitAliasDao()
		val fakeRecipeDao = MinimalRecipeDao()

		val repository = RecipeRepository(dao = fakeRecipeDao, unitAliasDao = fakeDao)

		assertEquals(0, repository.getUnitAliasCount())

		val seededCount = repository.seedDefaultUnitAliasesIfEmpty()
		assertTrue(seededCount > 0)
		assertEquals(seededCount, repository.getUnitAliasCount())

		// Second call should detect non-empty table and do nothing
		val secondCallCount = repository.seedDefaultUnitAliasesIfEmpty()
		assertEquals(0, secondCallCount)
		assertEquals(seededCount, repository.getUnitAliasCount())
	}

	private class MinimalRecipeDao : RecipeDao() {
		override fun getRecipeById(id: Long): Flow<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations> = kotlinx.coroutines.flow.emptyFlow()
		override fun getRecipesById(ids: Set<Long>): List<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getReferencedRecipes(recipeIds: Set<Long>): List<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getRecipeTitlesWithIds(): Flow<List<com.herohiman.tournant.data.RecipeTitleId>> = kotlinx.coroutines.flow.flowOf(emptyList())
		override fun getRecipeTitleById(id: Long): String = ""
		override fun getRecipeByGourmandId(gourmandId: Int): com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations? = null
		override fun getRecipeIdByGourmandId(gourmandId: Long): Long? = null
		override fun getDeprecatedRecipes(gourmandIds: List<Int>): List<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getRecipeDescriptions(query: String, orderedBy: Int, offset: Int, limit: Int, month: Int): List<com.herohiman.tournant.data.RecipeDescription> = emptyList()
		override fun getKeywords(id: Long): List<String> = emptyList()
		override fun getRecipeCount(): Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
		override fun getRecipeIds(query: String): List<Long> = emptyList()
		override fun getDependentRecipeIds(recipeIds: Set<Long>): List<Long> = emptyList()
		override fun getAllCategories(): Flow<List<String>> = kotlinx.coroutines.flow.flowOf(emptyList())
		override fun getAllCuisines(): Flow<List<String>> = kotlinx.coroutines.flow.flowOf(emptyList())
		override fun getAllKeywords(): Flow<List<String>> = kotlinx.coroutines.flow.flowOf(emptyList())
		override fun getCategories(query: String): Flow<List<com.herohiman.tournant.data.room.StringAndCount>> = kotlinx.coroutines.flow.flowOf(emptyList())
		override fun getCuisines(query: String): Flow<List<com.herohiman.tournant.data.room.StringAndCount>> = kotlinx.coroutines.flow.flowOf(emptyList())
		override fun getKeywords(query: String): Flow<List<com.herohiman.tournant.data.room.StringAndCount>> = kotlinx.coroutines.flow.flowOf(emptyList())
		override fun getSources(): Flow<List<String>> = kotlinx.coroutines.flow.flowOf(emptyList())
		override fun getYieldUnits(): Flow<List<String>> = kotlinx.coroutines.flow.flowOf(emptyList())
		override fun getIngredientItems(): Flow<List<String>> = kotlinx.coroutines.flow.flowOf(emptyList())
		override fun getIngredientUnits(): Flow<List<String>> = kotlinx.coroutines.flow.flowOf(emptyList())
		override suspend fun getUniqueIngredientNames(): List<String> = emptyList()
		override suspend fun getPreferredUnitForIngredient(item: String): String? = null
		override suspend fun insertRecipe(recipe: com.herohiman.tournant.data.room.RecipeEntity): Long = 1L
		override suspend fun updateRecipe(recipe: com.herohiman.tournant.data.room.RecipeEntity) {}
		override suspend fun deleteRecipe(recipe: com.herohiman.tournant.data.room.RecipeEntity) {}
		override suspend fun deleteRecipesByIds(recipeIds: Set<Long>) {}
		override suspend fun deleteAllRecipes() {}
		override suspend fun insertIngredient(ingredient: com.herohiman.tournant.data.room.IngredientEntity) {}
		override suspend fun updateIngredient(ingredient: com.herohiman.tournant.data.room.IngredientEntity) {}
		override suspend fun deleteIngredient(ingredient: com.herohiman.tournant.data.room.IngredientEntity) {}
		override suspend fun deleteIngredientsNotInList(recipeId: Long, positions: List<Int>) {}
		override suspend fun insertKeyword(preparation: com.herohiman.tournant.data.room.KeywordEntity): Long = 1L
		override suspend fun deleteKeywordsNotInList(recipeId: Long, positions: List<Int>) {}
		override suspend fun insertPreparationDate(preparation: com.herohiman.tournant.data.room.PreparationEntity): Long = 1L
		override suspend fun updatePreparationDate(preparation: com.herohiman.tournant.data.room.PreparationEntity) {}
		override suspend fun deletePreparationDate(preparation: com.herohiman.tournant.data.room.PreparationEntity) {}
		override suspend fun deletePreparationDatesNotInList(recipeId: Long, dates: List<Long>) {}
		override suspend fun getPreparation(recipeId: Long, date: Long): com.herohiman.tournant.data.room.PreparationEntity? = null
		override suspend fun getPreparations(recipeId: Long): List<com.herohiman.tournant.data.room.PreparationEntity> = emptyList()
		override suspend fun getMostRecentPreparation(recipeId: Long): com.herohiman.tournant.data.room.PreparationEntity? = null
		override suspend fun getPreparationsByDateRange(recipeId: Long, startDate: Long, endDate: Long): List<com.herohiman.tournant.data.room.PreparationEntity> = emptyList()
		override suspend fun pinRecipe(recipePin: com.herohiman.tournant.data.room.RecipePinEntity): Long = 1L
		override suspend fun unpinRecipe(recipeId: Long) {}
	}

	private class FakeUnitAliasDao : UnitAliasDao {
		private val map = mutableMapOf<Long, UnitAliasEntity>()
		private var nextId = 1L

		override fun getAllUnitAliases(): Flow<List<UnitAliasEntity>> {
			return flowOf(getAllUnitAliasesList())
		}

		override fun getAllUnitAliasesList(): List<UnitAliasEntity> {
			return map.values.sortedBy { it.aliasName }
		}

		override suspend fun getAliasesByBaseUnit(baseUnit: BaseUnitType): List<UnitAliasEntity> {
			return map.values.filter { it.baseUnit == baseUnit }.sortedBy { it.conversionFactor }
		}

		override suspend fun getAliasByName(name: String): UnitAliasEntity? {
			return map.values.firstOrNull { it.aliasName.equals(name.trim(), ignoreCase = true) }
		}

		override suspend fun getAliasById(id: Long): UnitAliasEntity? {
			return map[id]
		}

		override suspend fun insertAlias(alias: UnitAliasEntity): Long {
			val existing = map.values.firstOrNull { it.aliasName.equals(alias.aliasName.trim(), ignoreCase = true) }
			val id = if (existing != null) existing.id else (if (alias.id == 0L) nextId++ else alias.id)
			val entity = alias.copy(id = id)
			map[id] = entity
			return id
		}

		override suspend fun insertAliases(aliases: List<UnitAliasEntity>): List<Long> {
			return aliases.mapNotNull { alias ->
				val existing = map.values.firstOrNull { it.aliasName.equals(alias.aliasName.trim(), ignoreCase = true) }
				if (existing != null) {
					null
				} else {
					insertAlias(alias)
				}
			}
		}

		override suspend fun updateAlias(alias: UnitAliasEntity) {
			map[alias.id] = alias
		}

		override suspend fun deleteAlias(alias: UnitAliasEntity) {
			map.remove(alias.id)
		}

		override suspend fun deleteAliasById(id: Long) {
			map.remove(id)
		}

		override suspend fun getUnitAliasCount(): Int {
			return map.size
		}
	}
}
