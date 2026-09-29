package com.herohiman.tournant

import androidx.sqlite.db.SupportSQLiteDatabase
import com.herohiman.tournant.data.room.IngredientAliasDao
import com.herohiman.tournant.data.room.IngredientAliasEntity
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

class IngredientAliasMigrationAndDaoTest {

	private val executedSqlStatements = mutableListOf<String>()

	@Before
	fun setUp() {
		executedSqlStatements.clear()
	}

	@Test
	fun `migration from version 10 to 11 creates IngredientAlias table foreign keys and indices`() {
		val fakeDb = Proxy.newProxyInstance(
			SupportSQLiteDatabase::class.java.classLoader,
			arrayOf(SupportSQLiteDatabase::class.java)
		) { _, method, args ->
			if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
				executedSqlStatements.add(args[0] as String)
			}
			null
		} as SupportSQLiteDatabase

		RecipeRoomDatabase.MIGRATION_10_11.migrate(fakeDb)

		assertEquals(3, executedSqlStatements.size)

		val createTableSql = executedSqlStatements[0]
		assertTrue(createTableSql.contains("CREATE TABLE IF NOT EXISTS `IngredientAlias`"))
		assertTrue(createTableSql.contains("`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
		assertTrue(createTableSql.contains("`rawName` TEXT NOT NULL"))
		assertTrue(createTableSql.contains("`masterIngredientId` INTEGER NOT NULL"))
		assertTrue(createTableSql.contains("FOREIGN KEY(`masterIngredientId`) REFERENCES `MasterIngredient`(`id`) ON UPDATE CASCADE ON DELETE CASCADE"))

		val createRawNameIndexSql = executedSqlStatements[1]
		assertTrue(createRawNameIndexSql.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_IngredientAlias_rawName` ON `IngredientAlias` (`rawName`)"))

		val createMasterIdIndexSql = executedSqlStatements[2]
		assertTrue(createMasterIdIndexSql.contains("CREATE INDEX IF NOT EXISTS `index_IngredientAlias_masterIngredientId` ON `IngredientAlias` (`masterIngredientId`)"))
	}

	@Test
	fun `ingredient alias dao performs case insensitive lookup and master resolution`() = runBlocking {
		val masterDao = FakeMasterDao()
		val aliasDao = FakeIngredientAliasDao(masterDao)

		val oilId = masterDao.insertMasterIngredient(
			MasterIngredientEntity(id = 1, name = "तेल", unitCost = 150.0, baseUnit = "LITER")
		)
		val hingId = masterDao.insertMasterIngredient(
			MasterIngredientEntity(id = 2, name = "हींग", unitCost = 500.0, baseUnit = "KG")
		)

		aliasDao.insertAlias(IngredientAliasEntity(rawName = "तेल (1 कटोरी)", masterIngredientId = oilId))
		aliasDao.insertAlias(IngredientAliasEntity(rawName = "तेल कढ़ाई में लेना है", masterIngredientId = oilId))
		aliasDao.insertAlias(IngredientAliasEntity(rawName = "हींग (2 चमच)", masterIngredientId = hingId))

		// Case-insensitive rawName lookup
		val lookedUp = aliasDao.getAliasByRawName("तेल (1 कटोरी)")
		assertNotNull(lookedUp)
		assertEquals(oilId, lookedUp?.masterIngredientId)

		val lookedUpMaster = aliasDao.getMasterIngredientForRawName("तेल कढ़ाई में लेना है")
		assertNotNull(lookedUpMaster)
		assertEquals("तेल", lookedUpMaster?.name)

		val lookedUpHingMaster = aliasDao.getMasterIngredientForRawName("हींग (2 चमच)")
		assertNotNull(lookedUpHingMaster)
		assertEquals("हींग", lookedUpHingMaster?.name)

		// Non-existent alias
		assertNull(aliasDao.getAliasByRawName("धनिया"))
		assertNull(aliasDao.getMasterIngredientForRawName("धनिया"))
	}

	@Test
	fun `cascade deletion cleans up all aliases when master ingredient is removed`() = runBlocking {
		val masterDao = FakeMasterDao()
		val aliasDao = FakeIngredientAliasDao(masterDao)

		val oilId = masterDao.insertMasterIngredient(
			MasterIngredientEntity(id = 1, name = "तेल", unitCost = 150.0, baseUnit = "LITER")
		)

		aliasDao.insertAlias(IngredientAliasEntity(rawName = "तेल (1 कटोरी)", masterIngredientId = oilId))
		aliasDao.insertAlias(IngredientAliasEntity(rawName = "तेल (5 कटोरी)", masterIngredientId = oilId))

		assertEquals(2, aliasDao.getAliasesForMaster(oilId).size)

		// Hard delete master ingredient triggers cascade in our fake DAO
		masterDao.hardDeleteMasterIngredient(oilId)
		aliasDao.onMasterDeleted(oilId)

		assertEquals(0, aliasDao.getAliasesForMaster(oilId).size)
		assertNull(aliasDao.getAliasByRawName("तेल (1 कटोरी)"))
	}

	@Test
	fun `repository delegates ingredient alias operations accurately`() = runBlocking {
		val masterDao = FakeMasterDao()
		val aliasDao = FakeIngredientAliasDao(masterDao)
		val minimalRecipeDao = MinimalRecipeDao()

		val repository = RecipeRepository(
			dao = minimalRecipeDao,
			masterIngredientDao = masterDao,
			ingredientAliasDao = aliasDao
		)

		val masterId = repository.insertMasterIngredient(
			MasterIngredientEntity(id = 5, name = "Sugar", unitCost = 2.0, baseUnit = "kg")
		)

		repository.insertIngredientAlias(
			IngredientAliasEntity(id = 1, rawName = "White Sugar Granulated", masterIngredientId = masterId)
		)

		val resolvedMaster = repository.getMasterIngredientForRawName("white sugar granulated")
		assertNotNull(resolvedMaster)
		assertEquals("Sugar", resolvedMaster?.name)

		assertEquals(1, repository.getIngredientAliasCount())
		repository.deleteIngredientAliasByRawName("White Sugar Granulated")
		assertEquals(0, repository.getIngredientAliasCount())
	}

	private class MinimalRecipeDao : RecipeDao() {
		override fun getRecipeById(id: Long) = kotlinx.coroutines.flow.emptyFlow<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations>()
		override fun getRecipesById(ids: Set<Long>) = emptyList<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations>()
		override fun getReferencedRecipes(recipeIds: Set<Long>) = emptyList<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations>()
		override fun getRecipeTitlesWithIds() = kotlinx.coroutines.flow.flowOf(emptyList<com.herohiman.tournant.data.RecipeTitleId>())
		override fun getRecipeTitleById(id: Long) = ""
		override suspend fun getRecipesUsingMasterIngredient(masterIngredientId: Long, ingredientName: String) = emptyList<com.herohiman.tournant.data.RecipeTitleId>()
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
		override suspend fun getMasterIngredientByLinkedRecipeId(recipeId: Long): MasterIngredientEntity? = map.values.firstOrNull { it.linkedRecipeId == recipeId && it.isActive }
		override suspend fun getMasterIngredientsWithLinkedRecipes(): List<MasterIngredientEntity> = map.values.filter { it.linkedRecipeId != null && it.isActive }
	}

	private class FakeIngredientAliasDao(private val masterDao: FakeMasterDao) : IngredientAliasDao {
		private val map = mutableMapOf<Long, IngredientAliasEntity>()
		private var nextId = 1L

		fun onMasterDeleted(masterIngredientId: Long) {
			map.values.filter { it.masterIngredientId == masterIngredientId }.forEach {
				map.remove(it.id)
			}
		}

		override suspend fun getAliasByRawName(rawName: String): IngredientAliasEntity? {
			return map.values.firstOrNull { it.rawName.equals(rawName.trim(), ignoreCase = true) }
		}

		override suspend fun getMasterIngredientForRawName(rawName: String): MasterIngredientEntity? {
			val alias = getAliasByRawName(rawName) ?: return null
			val master = masterDao.getMasterIngredientById(alias.masterIngredientId)
			return if (master != null && master.isActive) master else null
		}

		override suspend fun getAliasesForMaster(masterIngredientId: Long): List<IngredientAliasEntity> {
			return map.values.filter { it.masterIngredientId == masterIngredientId }.sortedBy { it.rawName }
		}

		override fun getAllAliases(): Flow<List<IngredientAliasEntity>> = flowOf(getAllAliasesList())

		override fun getAllAliasesList(): List<IngredientAliasEntity> = map.values.sortedBy { it.rawName }

		override suspend fun insertAlias(alias: IngredientAliasEntity): Long {
			val existing = map.values.firstOrNull { it.rawName.equals(alias.rawName.trim(), ignoreCase = true) }
			val id = if (existing != null) existing.id else (if (alias.id == 0L) nextId++ else alias.id)
			val entity = alias.copy(id = id)
			map[id] = entity
			return id
		}

		override suspend fun insertAliases(aliases: List<IngredientAliasEntity>): List<Long> {
			return aliases.map { insertAlias(it) }
		}

		override suspend fun updateAlias(alias: IngredientAliasEntity) {
			map[alias.id] = alias
		}

		override suspend fun deleteAlias(alias: IngredientAliasEntity) {
			map.remove(alias.id)
		}

		override suspend fun deleteAliasById(id: Long) {
			map.remove(id)
		}

		override suspend fun deleteAliasByRawName(rawName: String) {
			val existing = getAliasByRawName(rawName)
			if (existing != null) {
				map.remove(existing.id)
			}
		}

		override suspend fun deleteAliasesForMaster(masterIngredientId: Long) {
			map.values.filter { it.masterIngredientId == masterIngredientId }.forEach {
				map.remove(it.id)
			}
		}

		override suspend fun getIngredientAliasCount(): Int = map.size
	}
}
