package com.herohiman.tournant

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.herohiman.tournant.cost.CostConfigBackupPayload
import com.herohiman.tournant.data.room.BaseUnitType
import com.herohiman.tournant.data.room.IngredientAliasDao
import com.herohiman.tournant.data.room.IngredientAliasEntity
import com.herohiman.tournant.data.room.MasterIngredientDao
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.RecipeDao
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.UnitAliasDao
import com.herohiman.tournant.data.room.UnitAliasEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CostConfigBackupPayloadTest {

	private val moshi: Moshi = Moshi.Builder()
		.add(KotlinJsonAdapterFactory())
		.build()

	@Test
	fun `payload default schema version and properties`() {
		val payload = CostConfigBackupPayload()
		assertEquals(CostConfigBackupPayload.CURRENT_SCHEMA_VERSION, payload.schemaVersion)
		assertTrue(payload.exportedAt > 0L)
		assertTrue(payload.masterIngredients.isEmpty())
		assertTrue(payload.unitAliases.isEmpty())
		assertTrue(payload.ingredientAliases.isEmpty())
	}

	@Test
	fun `moshi serialization and deserialization roundtrip`() {
		val master = MasterIngredientEntity(
			id = 1,
			name = "Milk",
			unitCost = 2.50,
			baseUnit = "l",
			category = "Dairy",
			linkedRecipeId = 101L,
			yieldRatio = 0.20
		)
		val unit = UnitAliasEntity(
			id = 2,
			aliasName = "किलो",
			baseUnit = BaseUnitType.KG,
			conversionFactor = 1.0
		)
		val alias = IngredientAliasEntity(
			id = 3,
			rawName = "दूध (1 कप)",
			masterIngredientId = 1
		)

		val originalPayload = CostConfigBackupPayload(
			schemaVersion = 1,
			exportedAt = 123456789L,
			masterIngredients = listOf(master),
			unitAliases = listOf(unit),
			ingredientAliases = listOf(alias)
		)

		val adapter = moshi.adapter(CostConfigBackupPayload::class.java).indent("  ")
		val json = adapter.toJson(originalPayload)

		assertNotNull(json)
		assertTrue(json.contains("Milk"))
		assertTrue(json.contains("किलो"))
		assertTrue(json.contains("दूध (1 कप)"))

		val parsed = adapter.fromJson(json)
		assertNotNull(parsed)
		assertEquals(1, parsed!!.schemaVersion)
		assertEquals(123456789L, parsed.exportedAt)
		assertEquals(1, parsed.masterIngredients.size)
		assertEquals("Milk", parsed.masterIngredients[0].name)
		assertEquals(101L, parsed.masterIngredients[0].linkedRecipeId)
		assertEquals(0.20, parsed.masterIngredients[0].yieldRatio!!, 0.001)

		assertEquals(1, parsed.unitAliases.size)
		assertEquals("किलो", parsed.unitAliases[0].aliasName)
		assertEquals(BaseUnitType.KG, parsed.unitAliases[0].baseUnit)

		assertEquals(1, parsed.ingredientAliases.size)
		assertEquals("दूध (1 कप)", parsed.ingredientAliases[0].rawName)
	}

	@Test
	fun `exportCostConfiguration retrieves all entities from repository`() = runBlocking {
		val masterDao = FakeMasterDao()
		val unitDao = FakeUnitDao()
		val aliasDao = FakeAliasDao()

		masterDao.insertMasterIngredient(MasterIngredientEntity(name = "Sugar", unitCost = 1.20, baseUnit = "kg"))
		unitDao.insertAlias(UnitAliasEntity(aliasName = "gm", baseUnit = BaseUnitType.KG, conversionFactor = 0.001))
		aliasDao.insertAlias(IngredientAliasEntity(rawName = "Sugar (fine)", masterIngredientId = 1))

		val repository = RecipeRepository(
			dao = DummyRecipeDao(),
			masterIngredientDao = masterDao,
			unitAliasDao = unitDao,
			ingredientAliasDao = aliasDao
		)

		val payload = repository.exportCostConfiguration()

		assertEquals(1, payload.masterIngredients.size)
		assertEquals("Sugar", payload.masterIngredients[0].name)
		assertEquals(1, payload.unitAliases.size)
		assertEquals("gm", payload.unitAliases[0].aliasName)
		assertEquals(1, payload.ingredientAliases.size)
		assertEquals("Sugar (fine)", payload.ingredientAliases[0].rawName)
	}

	@Test
	fun `importCostConfiguration merges existing ingredients by name and inserts new ones`() = runBlocking {
		val masterDao = FakeMasterDao()
		val unitDao = FakeUnitDao()
		val aliasDao = FakeAliasDao()

		// Pre-existing item with old price
		masterDao.insertMasterIngredient(MasterIngredientEntity(name = "Milk", unitCost = 1.00, baseUnit = "l"))

		val repository = RecipeRepository(
			dao = DummyRecipeDao(),
			masterIngredientDao = masterDao,
			unitAliasDao = unitDao,
			ingredientAliasDao = aliasDao
		)

		// Payload has updated price for Milk, and a brand new item Khoya
		val payload = CostConfigBackupPayload(
			masterIngredients = listOf(
				MasterIngredientEntity(id = 99, name = "Milk", unitCost = 2.50, baseUnit = "l"),
				MasterIngredientEntity(id = 100, name = "Khoya", unitCost = 10.0, baseUnit = "kg", linkedRecipeId = 42L, yieldRatio = 0.20)
			),
			unitAliases = listOf(
				UnitAliasEntity(aliasName = "gram", baseUnit = BaseUnitType.KG, conversionFactor = 0.001)
			)
		)

		val result = repository.importCostConfiguration(payload)

		assertTrue(result.success)
		assertEquals(2, result.importedMastersCount)
		assertEquals(1, result.importedUnitAliasesCount)

		// Check that Milk was updated, not duplicated
		val allMasters = masterDao.getAllMasterIngredientsList()
		assertEquals(2, allMasters.size)

		val milk = masterDao.getMasterIngredientByName("Milk")
		assertNotNull(milk)
		assertEquals(2.50, milk!!.unitCost, 0.001)

		val khoya = masterDao.getMasterIngredientByName("Khoya")
		assertNotNull(khoya)
		assertEquals(10.0, khoya!!.unitCost, 0.001)
		assertEquals(42L, khoya.linkedRecipeId)
	}

	@Test
	fun `importCostConfiguration maps foreign key masterIngredientId for ingredient aliases`() = runBlocking {
		val masterDao = FakeMasterDao()
		val aliasDao = FakeAliasDao()

		val repository = RecipeRepository(
			dao = DummyRecipeDao(),
			masterIngredientDao = masterDao,
			unitAliasDao = FakeUnitDao(),
			ingredientAliasDao = aliasDao
		)

		// In the backup payload, Heeng has ID 500, and alias references 500
		val payload = CostConfigBackupPayload(
			masterIngredients = listOf(
				MasterIngredientEntity(id = 500, name = "Heeng", unitCost = 5.0, baseUnit = "g")
			),
			ingredientAliases = listOf(
				IngredientAliasEntity(rawName = "Heeng (2 Chamach)", masterIngredientId = 500)
			)
		)

		val result = repository.importCostConfiguration(payload)

		assertTrue(result.success)
		assertEquals(1, result.importedMastersCount)
		assertEquals(1, result.importedIngredientAliasesCount)

		val insertedMaster = masterDao.getMasterIngredientByName("Heeng")
		assertNotNull(insertedMaster)

		val insertedAlias = aliasDao.getAliasByRawName("Heeng (2 Chamach)")
		assertNotNull(insertedAlias)
		// Foreign key must map to the locally inserted master ID, not the old 500
		assertEquals(insertedMaster!!.id, insertedAlias!!.masterIngredientId)
	}

	@Test
	fun `importCostConfiguration rejects unsupported schema version`() = runBlocking {
		val repository = RecipeRepository(
			dao = DummyRecipeDao(),
			masterIngredientDao = FakeMasterDao()
		)

		val invalidPayload = CostConfigBackupPayload(schemaVersion = 99)
		val result = repository.importCostConfiguration(invalidPayload)

		assertFalse(result.success)
		assertTrue(result.message.contains("Unsupported"))
	}

	// Fake DAOs for pure JUnit testing
	private class FakeMasterDao : MasterIngredientDao {
		val map = mutableMapOf<Long, MasterIngredientEntity>()
		private var nextId = 1L

		override fun getAllActiveMasterIngredients(): Flow<List<MasterIngredientEntity>> = flowOf(getAllActiveMasterIngredientsList())
		override fun getAllActiveMasterIngredientsList() = map.values.filter { it.isActive }.sortedBy { it.name }
		override fun getAllMasterIngredients(): Flow<List<MasterIngredientEntity>> = flowOf(getAllMasterIngredientsList())
		override fun getAllMasterIngredientsList() = map.values.sortedBy { it.name }
		override fun getMasterIngredientById(id: Long) = map[id]
		override fun getMasterIngredientByName(name: String) = map.values.firstOrNull { it.name.equals(name, ignoreCase = true) && it.isActive }
		override suspend fun getMasterIngredientByLinkedRecipeId(recipeId: Long) = map.values.firstOrNull { it.linkedRecipeId == recipeId && it.isActive }
		override suspend fun getMasterIngredientsWithLinkedRecipes() = map.values.filter { it.linkedRecipeId != null && it.isActive }
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

	private class FakeUnitDao : UnitAliasDao {
		val map = mutableMapOf<Long, UnitAliasEntity>()
		private var nextId = 1L

		override fun getAllUnitAliases(): Flow<List<UnitAliasEntity>> = flowOf(getAllUnitAliasesList())
		override fun getAllUnitAliasesList() = map.values.sortedBy { it.aliasName }
		override suspend fun getAliasByName(name: String) = map.values.firstOrNull { it.aliasName.equals(name, ignoreCase = true) }
		override suspend fun getAliasById(id: Long) = map[id]
		override suspend fun getAliasesByBaseUnit(baseUnit: BaseUnitType) = map.values.filter { it.baseUnit == baseUnit }
		override suspend fun insertAlias(alias: UnitAliasEntity): Long {
			val existing = map.values.firstOrNull { it.aliasName.equals(alias.aliasName, ignoreCase = true) }
			val id = if (existing != null) existing.id else (if (alias.id == 0L) nextId++ else alias.id)
			map[id] = alias.copy(id = id)
			return id
		}
		override suspend fun insertAliases(aliases: List<UnitAliasEntity>) = aliases.map { insertAlias(it) }
		override suspend fun updateAlias(alias: UnitAliasEntity) { map[alias.id] = alias }
		override suspend fun deleteAlias(alias: UnitAliasEntity) { map.remove(alias.id) }
		override suspend fun deleteAliasById(id: Long) { map.remove(id) }
		override suspend fun getUnitAliasCount() = map.size
	}

	private class FakeAliasDao : IngredientAliasDao {
		val map = mutableMapOf<Long, IngredientAliasEntity>()
		private var nextId = 1L

		override suspend fun getAliasByRawName(rawName: String) = map.values.firstOrNull { it.rawName.equals(rawName.trim(), ignoreCase = true) }
		override suspend fun getMasterIngredientForRawName(rawName: String): MasterIngredientEntity? = null
		override suspend fun getAliasesForMaster(masterIngredientId: Long) = map.values.filter { it.masterIngredientId == masterIngredientId }
		override fun getAllAliases(): Flow<List<IngredientAliasEntity>> = flowOf(getAllAliasesList())
		override fun getAllAliasesList() = map.values.sortedBy { it.rawName }
		override suspend fun insertAlias(alias: IngredientAliasEntity): Long {
			val existing = map.values.firstOrNull { it.rawName.equals(alias.rawName.trim(), ignoreCase = true) }
			val id = if (existing != null) existing.id else (if (alias.id == 0L) nextId++ else alias.id)
			map[id] = alias.copy(id = id)
			return id
		}
		override suspend fun insertAliases(aliases: List<IngredientAliasEntity>) = aliases.map { insertAlias(it) }
		override suspend fun updateAlias(alias: IngredientAliasEntity) { map[alias.id] = alias }
		override suspend fun deleteAlias(alias: IngredientAliasEntity) { map.remove(alias.id) }
		override suspend fun deleteAliasById(id: Long) { map.remove(id) }
		override suspend fun deleteAliasByRawName(rawName: String) { map.values.removeAll { it.rawName.equals(rawName.trim(), ignoreCase = true) } }
		override suspend fun deleteAliasesForMaster(masterIngredientId: Long) { map.values.removeAll { it.masterIngredientId == masterIngredientId } }
		override suspend fun getIngredientAliasCount() = map.size
	}

	private class DummyRecipeDao : RecipeDao() {
		override fun getRecipeById(id: Long) = kotlinx.coroutines.flow.emptyFlow<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations>()
		override fun getRecipesById(ids: Set<Long>) = emptyList<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations>()
		override fun getReferencedRecipes(recipeIds: Set<Long>) = emptyList<com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations>()
		override fun getRecipeTitlesWithIds() = kotlinx.coroutines.flow.flowOf(emptyList<com.herohiman.tournant.data.RecipeTitleId>())
		override fun getRecipeTitleById(id: Long) = ""
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
}
