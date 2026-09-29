package com.herohiman.tournant

import com.herohiman.tournant.cost.CostConfigBackupManager
import com.herohiman.tournant.cost.CostConfigBackupPayload
import com.herohiman.tournant.data.room.BaseUnitType
import com.herohiman.tournant.data.room.IngredientAliasEntity
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.UnitAliasEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class CostConfigBackupManagerTest {

	@Test
	fun `serializeToJson produces formatted JSON string`() {
		val payload = CostConfigBackupPayload(
			schemaVersion = 1,
			exportedAt = 1000L,
			masterIngredients = listOf(
				MasterIngredientEntity(id = 1, name = "Butter", unitCost = 5.0, baseUnit = "kg")
			),
			unitAliases = listOf(
				UnitAliasEntity(id = 2, aliasName = "lb", baseUnit = BaseUnitType.KG, conversionFactor = 0.45359)
			),
			ingredientAliases = listOf(
				IngredientAliasEntity(id = 3, rawName = "Butter (unsalted)", masterIngredientId = 1)
			)
		)

		val json = CostConfigBackupManager.serializeToJson(payload)

		assertNotNull(json)
		assertTrue(json.contains("\"schemaVersion\": 1"))
		assertTrue(json.contains("\"name\": \"Butter\""))
		assertTrue(json.contains("\"aliasName\": \"lb\""))
		assertTrue(json.contains("\"rawName\": \"Butter (unsalted)\""))
	}

	@Test
	fun `exportToStream and importFromStream stream roundtrip`() {
		val payload = CostConfigBackupPayload(
			schemaVersion = 1,
			exportedAt = 2000L,
			masterIngredients = listOf(
				MasterIngredientEntity(
					id = 10,
					name = "Khoya",
					unitCost = 0.0,
					baseUnit = "kg",
					linkedRecipeId = 42L,
					yieldRatio = 0.20
				)
			),
			unitAliases = listOf(
				UnitAliasEntity(id = 20, aliasName = "किलो", baseUnit = BaseUnitType.KG, conversionFactor = 1.0)
			),
			ingredientAliases = listOf(
				IngredientAliasEntity(id = 30, rawName = "खोया", masterIngredientId = 10)
			)
		)

		val outputStream = ByteArrayOutputStream()
		CostConfigBackupManager.exportToStream(payload, outputStream)

		val bytes = outputStream.toByteArray()
		assertTrue(bytes.isNotEmpty())

		val inputStream = ByteArrayInputStream(bytes)
		val importedPayload = CostConfigBackupManager.importFromStream(inputStream)

		assertNotNull(importedPayload)
		assertEquals(1, importedPayload!!.schemaVersion)
		assertEquals(2000L, importedPayload.exportedAt)

		assertEquals(1, importedPayload.masterIngredients.size)
		val master = importedPayload.masterIngredients[0]
		assertEquals("Khoya", master.name)
		assertEquals(42L, master.linkedRecipeId)
		assertEquals(0.20, master.yieldRatio!!, 0.001)

		assertEquals(1, importedPayload.unitAliases.size)
		assertEquals("किलो", importedPayload.unitAliases[0].aliasName)

		assertEquals(1, importedPayload.ingredientAliases.size)
		assertEquals("खोया", importedPayload.ingredientAliases[0].rawName)
	}

	@Test
	fun `importFromStream with corrupted JSON returns null safely`() {
		val corruptJson = "{ corrupted json content: invalid [ "
		val inputStream = ByteArrayInputStream(corruptJson.toByteArray(StandardCharsets.UTF_8))

		val result = CostConfigBackupManager.importFromStream(inputStream)
		assertNull(result)
	}

	@Test
	fun `generateBackupFilename creates timestamped json filename`() {
		val filename = CostConfigBackupManager.generateBackupFilename(1700000000000L)
		assertTrue(filename.startsWith("tournant_cost_config_"))
		assertTrue(filename.endsWith(".json"))
	}
}
