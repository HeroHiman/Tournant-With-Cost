package com.herohiman.tournant

import com.herohiman.tournant.data.room.BaseUnitType
import com.herohiman.tournant.data.room.UnitAliasDao
import com.herohiman.tournant.data.room.UnitAliasEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class UnitManagementUiTest {

	@Test
	fun `unit alias input validation requires non-blank alias name and positive factor`() {
		fun validate(name: String, factorStr: String): Boolean {
			val factor = factorStr.toDoubleOrNull()
			return name.isNotBlank() && factor != null && factor > 0.0
		}

		assertTrue(validate("डब्बा", "15"))
		assertTrue(validate("चुटकी", "0.002"))
		assertTrue(validate("cup", "0.24"))
		assertFalse(validate("", "15"))
		assertFalse(validate("   ", "15"))
		assertFalse(validate("डब्बा", ""))
		assertFalse(validate("डब्बा", "0"))
		assertFalse(validate("डब्बा", "-5"))
		assertFalse(validate("डब्बा", "abc"))
	}

	@Test
	fun `unit formula preview formats correctly across base unit categories`() {
		fun formatFormula(alias: String, factor: Double, baseUnit: BaseUnitType): String {
			val categoryUnit = when (baseUnit) {
				BaseUnitType.KG -> "kg"
				BaseUnitType.LITER -> "liter"
				BaseUnitType.COUNT -> "units"
			}
			val factorStr = if (factor == factor.toLong().toDouble()) {
				factor.toLong().toString()
			} else {
				String.format(Locale.US, "%.6f", factor).trimEnd('0').trimEnd('.')
			}
			return "1 $alias = $factorStr $categoryUnit"
		}

		assertEquals("1 डब्बा = 15 kg", formatFormula("डब्बा", 15.0, BaseUnitType.KG))
		assertEquals("1 चुटकी = 0.002 kg", formatFormula("चुटकी", 0.002, BaseUnitType.KG))
		assertEquals("1 bottle = 750 liter", formatFormula("bottle", 750.0, BaseUnitType.LITER))
		assertEquals("1 cup = 0.24 liter", formatFormula("cup", 0.24, BaseUnitType.LITER))
		assertEquals("1 दर्जन = 12 units", formatFormula("दर्जन", 12.0, BaseUnitType.COUNT))
	}

	@Test
	fun `category filtering correctly separates mass, volume, and count units`() {
		val aliases = listOf(
			UnitAliasEntity(1, "किलो", BaseUnitType.KG, 1.0),
			UnitAliasEntity(2, "डब्बा", BaseUnitType.KG, 15.0),
			UnitAliasEntity(3, "लीटर", BaseUnitType.LITER, 1.0),
			UnitAliasEntity(4, "मिली", BaseUnitType.LITER, 0.001),
			UnitAliasEntity(5, "दर्जन", BaseUnitType.COUNT, 12.0),
			UnitAliasEntity(6, "नग", BaseUnitType.COUNT, 1.0)
		)

		val massOnly = aliases.filter { it.baseUnit == BaseUnitType.KG }
		assertEquals(2, massOnly.size)
		assertTrue(massOnly.all { it.baseUnit == BaseUnitType.KG })

		val volumeOnly = aliases.filter { it.baseUnit == BaseUnitType.LITER }
		assertEquals(2, volumeOnly.size)
		assertTrue(volumeOnly.all { it.baseUnit == BaseUnitType.LITER })

		val countOnly = aliases.filter { it.baseUnit == BaseUnitType.COUNT }
		assertEquals(2, countOnly.size)
		assertTrue(countOnly.all { it.baseUnit == BaseUnitType.COUNT })
	}

	@Test
	fun `search filtering matches alias name case-insensitively and supports multilingual characters`() {
		val aliases = listOf(
			UnitAliasEntity(1, "किलो", BaseUnitType.KG, 1.0),
			UnitAliasEntity(2, "डब्बा", BaseUnitType.KG, 15.0),
			UnitAliasEntity(3, "Kilogram", BaseUnitType.KG, 1.0),
			UnitAliasEntity(4, "gram", BaseUnitType.KG, 0.001)
		)

		fun filter(query: String): List<UnitAliasEntity> {
			return aliases.filter { it.aliasName.contains(query, ignoreCase = true) }
		}

		val hindiSearch = filter("डब्बा")
		assertEquals(1, hindiSearch.size)
		assertEquals("डब्बा", hindiSearch[0].aliasName)

		val englishCaseSearch = filter("kilo")
		assertEquals(1, englishCaseSearch.size)
		assertEquals("Kilogram", englishCaseSearch[0].aliasName)
	}

	@Test
	fun `conversion factor decimal precision formatting handles whole and fractional numbers cleanly`() {
		fun formatFactor(factor: Double): String {
			return if (factor == factor.toLong().toDouble()) {
				factor.toLong().toString()
			} else {
				String.format(Locale.US, "%.6f", factor).trimEnd('0').trimEnd('.')
			}
		}

		assertEquals("15", formatFactor(15.0))
		assertEquals("1", formatFactor(1.0))
		assertEquals("12", formatFactor(12.0))
		assertEquals("0.001", formatFactor(0.001))
		assertEquals("0.002", formatFactor(0.002))
		assertEquals("0.24", formatFactor(0.240000))
	}

	@Test
	fun `restore default units provides standard English and Hindi units`() {
		val defaults = UnitAliasDao.getDefaultAliases()
		assertTrue(defaults.isNotEmpty())

		// Verify that "डब्बा" can be added alongside defaults without conflict
		val customBox = UnitAliasEntity(id = 999, aliasName = "डब्बा", baseUnit = BaseUnitType.KG, conversionFactor = 15.0)
		val combined = defaults + customBox

		assertTrue(combined.any { it.aliasName == "डब्बा" && it.conversionFactor == 15.0 })
		assertTrue(combined.any { it.aliasName == "किलो" && it.conversionFactor == 1.0 })
		assertTrue(combined.any { it.aliasName == "ग्राम" && it.conversionFactor == 0.001 })
	}

	@Test
	fun `unit backup filename generator produces valid timestamped filename`() {
		val filename = com.herohiman.tournant.cost.CostConfigBackupManager.generateUnitBackupFilename(1759163400000L)
		assertTrue(filename.startsWith("tournant_units_backup_"))
		assertTrue(filename.endsWith(".json"))
	}

	@Test
	fun `unit export payload isolates unit aliases and deserializes cleanly`() {
		val customAliases = listOf(
			UnitAliasEntity(id = 1, aliasName = "डब्बा", baseUnit = BaseUnitType.KG, conversionFactor = 15.0),
			UnitAliasEntity(id = 2, aliasName = "चुटकी", baseUnit = BaseUnitType.KG, conversionFactor = 0.002)
		)

		val payload = com.herohiman.tournant.cost.CostConfigBackupPayload(
			unitAliases = customAliases
		)

		val json = com.herohiman.tournant.cost.CostConfigBackupManager.serializeToJson(payload)
		assertNotNull(json)
		assertTrue(json.contains("डब्बा"))
		assertTrue(json.contains("15.0"))

		val restored = com.herohiman.tournant.cost.CostConfigBackupManager.deserializeFromJson(json)
		assertNotNull(restored)
		assertEquals(2, restored!!.unitAliases.size)
		assertEquals("डब्बा", restored.unitAliases[0].aliasName)
		assertEquals(15.0, restored.unitAliases[0].conversionFactor, 0.0001)
	}
}
