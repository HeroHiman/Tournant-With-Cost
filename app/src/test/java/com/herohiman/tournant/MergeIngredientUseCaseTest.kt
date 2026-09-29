package com.herohiman.tournant

import com.herohiman.tournant.cost.LiveCostCalculator
import com.herohiman.tournant.cost.MergeIngredientUseCase
import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.room.IngredientAliasDao
import com.herohiman.tournant.data.room.IngredientAliasEntity
import com.herohiman.tournant.data.room.MasterIngredientDao
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.RecipeDao
import com.herohiman.tournant.data.room.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeIngredientUseCaseTest {

	@Test
	fun `merge single duplicate transfers existing aliases creates new alias and deletes duplicate`() = runBlocking {
		val masterDao = FakeMasterDao()
		val aliasDao = FakeAliasDao()
		val repository = createRepository(masterDao, aliasDao)

		// Create target: तेल
		val targetId = repository.insertMasterIngredient(
			MasterIngredientEntity(id = 1, name = "तेल", unitCost = 150.0, baseUnit = "liter")
		)

		// Create duplicate: तेल (5 कटोरी)
		val duplicateId = repository.insertMasterIngredient(
			MasterIngredientEntity(id = 2, name = "तेल (5 कटोरी)", unitCost = 0.0, baseUnit = "cup")
		)

		// Add an existing alias to the duplicate: "तेल पांच कटोरी"
		repository.insertIngredientAlias(
			IngredientAliasEntity(id = 10, rawName = "तेल पांच कटोरी", masterIngredientId = duplicateId)
		)

		val useCase = MergeIngredientUseCase(repository)
		val result = useCase.merge(sourceId = duplicateId, targetId = targetId)

		assertTrue(result.success)
		assertEquals(1, result.mergedCount)

		// Duplicate should be deleted from MasterIngredient catalog
		assertNull(repository.getMasterIngredientById(duplicateId))

		// Target should still exist
		assertNotNull(repository.getMasterIngredientById(targetId))

		// Both the duplicate name and its prior alias should now point to target
		val alias1 = repository.getIngredientAliasByRawName("तेल (5 कटोरी)")
		assertNotNull(alias1)
		assertEquals(targetId, alias1?.masterIngredientId)

		val alias2 = repository.getIngredientAliasByRawName("तेल पांच कटोरी")
		assertNotNull(alias2)
		assertEquals(targetId, alias2?.masterIngredientId)
	}

	@Test
	fun `merge prevents merging an ingredient into itself`() = runBlocking {
		val masterDao = FakeMasterDao()
		val aliasDao = FakeAliasDao()
		val repository = createRepository(masterDao, aliasDao)

		repository.insertMasterIngredient(
			MasterIngredientEntity(id = 1, name = "हींग", unitCost = 500.0, baseUnit = "kg")
		)

		val useCase = MergeIngredientUseCase(repository)
		val result = useCase.merge(sourceId = 1, targetId = 1)

		assertFalse(result.success)
		assertEquals(0, result.mergedCount)
	}

	@Test
	fun `merge fails gracefully when source or target is missing`() = runBlocking {
		val masterDao = FakeMasterDao()
		val aliasDao = FakeAliasDao()
		val repository = createRepository(masterDao, aliasDao)

		repository.insertMasterIngredient(
			MasterIngredientEntity(id = 1, name = "जीरा", unitCost = 300.0, baseUnit = "kg")
		)

		val useCase = MergeIngredientUseCase(repository)

		val resultSourceMissing = useCase.merge(sourceId = 999, targetId = 1)
		assertFalse(resultSourceMissing.success)

		val resultTargetMissing = useCase.merge(sourceId = 1, targetId = 999)
		assertFalse(resultTargetMissing.success)
	}

	@Test
	fun `mergeMultiple merges multiple duplicate variations into single core ingredient`() = runBlocking {
		val masterDao = FakeMasterDao()
		val aliasDao = FakeAliasDao()
		val repository = createRepository(masterDao, aliasDao)

		val hingId = repository.insertMasterIngredient(
			MasterIngredientEntity(id = 1, name = "हींग", unitCost = 500.0, baseUnit = "kg")
		)
		val hingVar1 = repository.insertMasterIngredient(
			MasterIngredientEntity(id = 2, name = "हींग (2 चमच)", unitCost = 0.0, baseUnit = "tsp")
		)
		val hingVar2 = repository.insertMasterIngredient(
			MasterIngredientEntity(id = 3, name = "हींग (3 चमच)", unitCost = 0.0, baseUnit = "tsp")
		)

		val useCase = MergeIngredientUseCase(repository)
		val result = useCase.mergeMultiple(listOf(hingVar1, hingVar2), hingId)

		assertTrue(result.success)
		assertEquals(2, result.mergedCount)

		assertNull(repository.getMasterIngredientById(hingVar1))
		assertNull(repository.getMasterIngredientById(hingVar2))

		assertEquals(hingId, repository.getIngredientAliasByRawName("हींग (2 चमच)")?.masterIngredientId)
		assertEquals(hingId, repository.getIngredientAliasByRawName("हींग (3 चमच)")?.masterIngredientId)
	}

	@Test
	fun `buildIngredientAliasLookupMap and LiveCostCalculator price conversational recipe ingredients using alias map`() = runBlocking {
		val masterDao = FakeMasterDao()
		val aliasDao = FakeAliasDao()
		val repository = createRepository(masterDao, aliasDao)

		val oilId = repository.insertMasterIngredient(
			MasterIngredientEntity(id = 1, name = "तेल", unitCost = 150.0, baseUnit = "liter")
		)

		// Create conversational aliases
		repository.insertIngredientAlias(
			IngredientAliasEntity(id = 10, rawName = "तेल (1 कटोरी)", masterIngredientId = oilId)
		)

		val lookupMap = repository.buildIngredientAliasLookupMap()
		assertEquals(1, lookupMap.size)
		assertTrue(lookupMap.containsKey("तेल (1 कटोरी)"))
		assertEquals("तेल", lookupMap["तेल (1 कटोरी)"]?.name)

		// Recipe ingredient has conversational name "तेल (1 कटोरी)"
		val recipeIngredient = Ingredient(
			amount = 250.0,
			amountRange = null,
			unit = "ml",
			item = "तेल (1 कटोरी)",
			refId = null,
			group = null,
			optional = false
		)

		val activeMasters = repository.getAllActiveMasterIngredientsList()
		val lineCostItem = LiveCostCalculator.calculateLineCost(
			ingredient = recipeIngredient,
			masterIngredients = activeMasters,
			aliases = lookupMap
		)

		assertEquals(com.herohiman.tournant.cost.CostStatus.MATCHED, lineCostItem.status)
		assertEquals("तेल", lineCostItem.masterIngredient?.name)
		// 250 ml = 0.25 liter * 150 = 37.5
		assertEquals(37.5, lineCostItem.lineCost, 0.01)
	}

	private fun createRepository(masterDao: MasterIngredientDao, aliasDao: IngredientAliasDao): RecipeRepository {
		val dummyRecipeDao = object : RecipeDao() {
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
		return RecipeRepository(
			dao = dummyRecipeDao,
			masterIngredientDao = masterDao,
			ingredientAliasDao = aliasDao
		)
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

	private class FakeAliasDao : IngredientAliasDao {
		val map = mutableMapOf<Long, IngredientAliasEntity>()
		private var nextId = 1L

		override suspend fun getAliasByRawName(rawName: String): IngredientAliasEntity? {
			return map.values.firstOrNull { it.rawName.equals(rawName.trim(), ignoreCase = true) }
		}

		override suspend fun getMasterIngredientForRawName(rawName: String): MasterIngredientEntity? = null

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

		override suspend fun insertAliases(aliases: List<IngredientAliasEntity>) = aliases.map { insertAlias(it) }
		override suspend fun updateAlias(alias: IngredientAliasEntity) { map[alias.id] = alias }
		override suspend fun deleteAlias(alias: IngredientAliasEntity) { map.remove(alias.id) }
		override suspend fun deleteAliasById(id: Long) { map.remove(id) }
		override suspend fun deleteAliasByRawName(rawName: String) {
			val existing = getAliasByRawName(rawName)
			if (existing != null) map.remove(existing.id)
		}
		override suspend fun deleteAliasesForMaster(masterIngredientId: Long) {
			map.values.filter { it.masterIngredientId == masterIngredientId }.forEach { map.remove(it.id) }
		}
		override suspend fun getIngredientAliasCount() = map.size
	}
}
