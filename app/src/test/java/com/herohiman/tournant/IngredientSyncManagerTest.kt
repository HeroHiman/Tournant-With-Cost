package com.herohiman.tournant

import com.herohiman.tournant.cost.IngredientSyncManager
import com.herohiman.tournant.data.room.MasterIngredientEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientSyncManagerTest {

	@Test
	fun `createMissingMasterIngredients filters out existing ingredients case-insensitively`() = runBlocking {
		val existingNames = setOf("sugar", "flour")
		val recipeIngredients = listOf("Sugar", "FLOUR", "Milk", "Salt")

		val missing = IngredientSyncManager.createMissingMasterIngredients(
			uniqueNames = recipeIngredients,
			existingNameSet = existingNames,
			unitResolver = { "g" }
		)

		assertEquals(2, missing.size)
		assertEquals("Milk", missing[0].name)
		assertEquals("Salt", missing[1].name)
	}

	@Test
	fun `createMissingMasterIngredients preserves soft-deleted items by respecting existing catalog`() = runBlocking {
		// Even if an item is soft-deleted, it exists in the catalog so existingNameSet includes it
		val existingCatalog = listOf(
			MasterIngredientEntity(id = 1, name = "Vanilla", unitCost = 5.0, baseUnit = "ml", isActive = false)
		)
		val existingNameSet = existingCatalog.map { it.name.trim().lowercase() }.toSet()

		val recipeIngredients = listOf("Vanilla", "Cocoa Powder")

		val missing = IngredientSyncManager.createMissingMasterIngredients(
			uniqueNames = recipeIngredients,
			existingNameSet = existingNameSet,
			unitResolver = { "g" }
		)

		assertEquals(1, missing.size)
		assertEquals("Cocoa Powder", missing[0].name)
	}

	@Test
	fun `createMissingMasterIngredients assigns preferred unit and defaults to unit when unit is null or empty`() = runBlocking {
		val existingNames = emptySet<String>()
		val recipeIngredients = listOf("Ghee", "Cardamom", "Cinnamon")

		val unitsMap = mapOf(
			"Ghee" to "kg",
			"Cardamom" to "",
			"Cinnamon" to null
		)

		val missing = IngredientSyncManager.createMissingMasterIngredients(
			uniqueNames = recipeIngredients,
			existingNameSet = existingNames,
			unitResolver = { unitsMap[it] }
		)

		assertEquals(3, missing.size)

		assertEquals("Ghee", missing[0].name)
		assertEquals("kg", missing[0].baseUnit)
		assertEquals(0.0, missing[0].unitCost, 0.0001)
		assertTrue(missing[0].isActive)

		assertEquals("Cardamom", missing[1].name)
		assertEquals("unit", missing[1].baseUnit)

		assertEquals("Cinnamon", missing[2].name)
		assertEquals("unit", missing[2].baseUnit)
	}

	@Test
	fun `createMissingMasterIngredients handles unicode Hindi names and deduplicates casing`() = runBlocking {
		val existingNames = setOf("मैदा")
		val recipeIngredients = listOf("मैदा", "शक्कर", "Sugar", "sugar", "   ")

		val missing = IngredientSyncManager.createMissingMasterIngredients(
			uniqueNames = recipeIngredients,
			existingNameSet = existingNames,
			unitResolver = { if (it == "शक्कर") "ग्राम" else "kg" }
		)

		assertEquals(2, missing.size)
		assertEquals("शक्कर", missing[0].name)
		assertEquals("ग्राम", missing[0].baseUnit)

		assertEquals("Sugar", missing[1].name)
		assertEquals("kg", missing[1].baseUnit)
	}

	@Test
	fun `createMissingMasterIngredients returns empty list when all ingredients exist`() = runBlocking {
		val existingNames = setOf("salt", "pepper")
		val recipeIngredients = listOf("Salt", "Pepper")

		val missing = IngredientSyncManager.createMissingMasterIngredients(
			uniqueNames = recipeIngredients,
			existingNameSet = existingNames,
			unitResolver = { "tsp" }
		)

		assertTrue(missing.isEmpty())
	}
}
