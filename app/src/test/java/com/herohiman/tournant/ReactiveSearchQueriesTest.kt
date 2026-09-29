package com.herohiman.tournant

import com.herohiman.tournant.data.room.BaseUnitType
import com.herohiman.tournant.data.room.MasterIngredientDao
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.UnitAliasDao
import com.herohiman.tournant.data.room.UnitAliasEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactiveSearchQueriesTest {

	private class FakeMasterIngredientDao : MasterIngredientDao {
		private val items = mutableListOf<MasterIngredientEntity>()
		private var nextId = 1L

		fun populate(entities: List<MasterIngredientEntity>) {
			items.clear()
			items.addAll(entities)
		}

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

		override fun searchActiveMasterIngredients(query: String): Flow<List<MasterIngredientEntity>> =
			flowOf(items.filter { it.isActive && (it.name.contains(query, ignoreCase = true) || it.category?.contains(query, ignoreCase = true) == true) }.sortedBy { it.name })

		override fun searchMasterIngredientsList(query: String): List<MasterIngredientEntity> =
			items.filter { it.name.contains(query, ignoreCase = true) || it.category?.contains(query, ignoreCase = true) == true }.sortedBy { it.name }

		override fun getMasterIngredientById(id: Long): MasterIngredientEntity? = items.firstOrNull { it.id == id }

		override fun getMasterIngredientByName(name: String): MasterIngredientEntity? =
			items.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) && it.isActive }

		override suspend fun getMasterIngredientByLinkedRecipeId(recipeId: Long): MasterIngredientEntity? =
			items.firstOrNull { it.linkedRecipeId == recipeId && it.isActive }

		override suspend fun getMasterIngredientsWithLinkedRecipes(): List<MasterIngredientEntity> =
			items.filter { it.linkedRecipeId != null && it.isActive }

		override suspend fun insertMasterIngredient(item: MasterIngredientEntity): Long {
			val id = if (item.id == 0L) nextId++ else item.id
			items.add(item.copy(id = id))
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
	}

	private class FakeUnitAliasDao : UnitAliasDao {
		private val items = mutableListOf<UnitAliasEntity>()
		private var nextId = 1L

		fun populate(aliases: List<UnitAliasEntity>) {
			items.clear()
			items.addAll(aliases)
		}

		override fun getAllUnitAliases(): Flow<List<UnitAliasEntity>> = flowOf(getAllUnitAliasesList())
		override fun getAllUnitAliasesList(): List<UnitAliasEntity> = items.sortedBy { it.aliasName }
		override suspend fun getAliasesByBaseUnit(baseUnit: BaseUnitType): List<UnitAliasEntity> =
			items.filter { it.baseUnit == baseUnit }.sortedBy { it.conversionFactor }

		override suspend fun getAliasByName(name: String): UnitAliasEntity? =
			items.firstOrNull { it.aliasName.equals(name.trim(), ignoreCase = true) }

		override suspend fun getAliasById(id: Long): UnitAliasEntity? = items.firstOrNull { it.id == id }

		override suspend fun insertAlias(alias: UnitAliasEntity): Long {
			val id = if (alias.id == 0L) nextId++ else alias.id
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
	fun `searchMasterIngredients filters by partial name match including Hindi characters`() = runBlocking {
		val dao = FakeMasterIngredientDao()
		dao.populate(
			listOf(
				MasterIngredientEntity(id = 1, name = "काजू", unitCost = 700.0, baseUnit = "kg"),
				MasterIngredientEntity(id = 2, name = "काला नमक", unitCost = 50.0, baseUnit = "kg"),
				MasterIngredientEntity(id = 3, name = "हल्दी", unitCost = 120.0, baseUnit = "kg")
			)
		)

		val results = dao.searchMasterIngredients("का").first()
		assertEquals(2, results.size)
		assertTrue(results.any { it.name == "काजू" })
		assertTrue(results.any { it.name == "काला नमक" })
	}

	@Test
	fun `searchMasterIngredients filters by category`() = runBlocking {
		val dao = FakeMasterIngredientDao()
		dao.populate(
			listOf(
				MasterIngredientEntity(id = 1, name = "Milk", unitCost = 60.0, baseUnit = "l", category = "Dairy"),
				MasterIngredientEntity(id = 2, name = "Ghee", unitCost = 500.0, baseUnit = "kg", category = "Dairy"),
				MasterIngredientEntity(id = 3, name = "Flour", unitCost = 30.0, baseUnit = "kg", category = "Grains")
			)
		)

		val results = dao.searchMasterIngredients("Dairy").first()
		assertEquals(2, results.size)
		assertTrue(results.any { it.name == "Milk" })
		assertTrue(results.any { it.name == "Ghee" })
	}

	@Test
	fun `searchActiveMasterIngredients excludes inactive ingredients`() = runBlocking {
		val dao = FakeMasterIngredientDao()
		dao.populate(
			listOf(
				MasterIngredientEntity(id = 1, name = "Sugar", unitCost = 40.0, baseUnit = "kg", isActive = true),
				MasterIngredientEntity(id = 2, name = "Sugar Syrup", unitCost = 30.0, baseUnit = "kg", isActive = false)
			)
		)

		val allResults = dao.searchMasterIngredients("Sugar").first()
		assertEquals(2, allResults.size)

		val activeResults = dao.searchActiveMasterIngredients("Sugar").first()
		assertEquals(1, activeResults.size)
		assertEquals("Sugar", activeResults[0].name)
	}

	@Test
	fun `searchUnitAliases filters aliases by partial name across Hindi and English`() = runBlocking {
		val dao = FakeUnitAliasDao()
		dao.populate(
			listOf(
				UnitAliasEntity(id = 1, aliasName = "चम्मच", baseUnit = BaseUnitType.LITER, conversionFactor = 0.015),
				UnitAliasEntity(id = 2, aliasName = "बड़ा चम्मच", baseUnit = BaseUnitType.LITER, conversionFactor = 0.015),
				UnitAliasEntity(id = 3, aliasName = "छोटा चम्मच", baseUnit = BaseUnitType.LITER, conversionFactor = 0.005),
				UnitAliasEntity(id = 4, aliasName = "किलो", baseUnit = BaseUnitType.KG, conversionFactor = 1.0)
			)
		)

		val results = dao.searchUnitAliases("चम्मच").first()
		assertEquals(3, results.size)
		assertTrue(results.any { it.aliasName == "चम्मच" })
		assertTrue(results.any { it.aliasName == "बड़ा चम्मच" })
		assertTrue(results.any { it.aliasName == "छोटा चम्मच" })
	}

	@Test
	fun `searchUnitAliasesByCategory filters by both query and baseUnit category`() = runBlocking {
		val dao = FakeUnitAliasDao()
		dao.populate(
			listOf(
				UnitAliasEntity(id = 1, aliasName = "cup", baseUnit = BaseUnitType.LITER, conversionFactor = 0.24),
				UnitAliasEntity(id = 2, aliasName = "clove", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0),
				UnitAliasEntity(id = 3, aliasName = "can", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0)
			)
		)

		val countResults = dao.searchUnitAliasesByCategory("c", BaseUnitType.COUNT).first()
		assertEquals(2, countResults.size)
		assertTrue(countResults.all { it.baseUnit == BaseUnitType.COUNT })

		val volumeResults = dao.searchUnitAliasesByCategory("c", BaseUnitType.LITER).first()
		assertEquals(1, volumeResults.size)
		assertEquals("cup", volumeResults[0].aliasName)
	}
}
