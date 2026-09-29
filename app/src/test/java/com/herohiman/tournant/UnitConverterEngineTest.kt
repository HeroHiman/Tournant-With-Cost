package com.herohiman.tournant

import com.herohiman.tournant.cost.CostStatus
import com.herohiman.tournant.cost.LiveCostCalculator
import com.herohiman.tournant.cost.UnitConverterEngine
import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.room.BaseUnitType
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.UnitAliasEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UnitConverterEngineTest {

	@Test
	fun `resolves conversion factor for Hindi kilo to English kg`() {
		// 10 किलो should convert to 10 kg with factor 1.0
		val factor = UnitConverterEngine.resolveConversionFactor("किलो", "kg")
		assertNotNull(factor)
		assertEquals(1.0, factor!!, 0.0001)

		val converted = UnitConverterEngine.convertQuantity(10.0, "किलो", "kg")
		assertEquals(10.0, converted!!, 0.0001)
	}

	@Test
	fun `resolves conversion factor for Hindi gram to English kg`() {
		// 500 ग्राम should convert to 0.5 kg with factor 0.001
		val factor = UnitConverterEngine.resolveConversionFactor("ग्राम", "kg")
		assertNotNull(factor)
		assertEquals(0.001, factor!!, 0.00001)

		val converted = UnitConverterEngine.convertQuantity(500.0, "ग्राम", "kg")
		assertEquals(0.5, converted!!, 0.0001)
	}

	@Test
	fun `resolves conversion factor for Hindi abbreviations with dots`() {
		// "कि.ग्रा." to "kg"
		val factor = UnitConverterEngine.resolveConversionFactor("कि.ग्रा.", "kg")
		assertNotNull(factor)
		assertEquals(1.0, factor!!, 0.0001)

		// "ग्रा" to "kg"
		val gramFactor = UnitConverterEngine.resolveConversionFactor("ग्रा", "kg")
		assertNotNull(gramFactor)
		assertEquals(0.001, gramFactor!!, 0.00001)
	}

	@Test
	fun `resolves volume conversion for Hindi milli to English liter`() {
		// 500 मिली to "l"
		val factor = UnitConverterEngine.resolveConversionFactor("मिली", "l")
		assertNotNull(factor)
		assertEquals(0.001, factor!!, 0.00001)

		val converted = UnitConverterEngine.convertQuantity(500.0, "मिली", "l")
		assertEquals(0.5, converted!!, 0.0001)

		// 1.5 लीटर to "ml"
		val reverseFactor = UnitConverterEngine.resolveConversionFactor("लीटर", "ml")
		assertNotNull(reverseFactor)
		assertEquals(1000.0, reverseFactor!!, 0.0001)

		val reverseConverted = UnitConverterEngine.convertQuantity(1.5, "लीटर", "ml")
		assertEquals(1500.0, reverseConverted!!, 0.0001)
	}

	@Test
	fun `resolves count conversion for Hindi dozen to unit and piece`() {
		// 2 दर्जन to "piece" (1 dozen = 12 pieces)
		val factor = UnitConverterEngine.resolveConversionFactor("दर्जन", "piece")
		assertNotNull(factor)
		assertEquals(12.0, factor!!, 0.0001)

		val converted = UnitConverterEngine.convertQuantity(2.0, "दर्जन", "piece")
		assertEquals(24.0, converted!!, 0.0001)

		// "नग" to "unit"
		val unitFactor = UnitConverterEngine.resolveConversionFactor("नग", "unit")
		assertNotNull(unitFactor)
		assertEquals(1.0, unitFactor!!, 0.0001)
	}

	@Test
	fun `resolves custom user-defined unit aliases from dictionary`() {
		// Custom bulk unit: "1 डब्बा" = 15 kg
		val customAliases = mapOf(
			"डब्बा" to UnitAliasEntity(id = 100, aliasName = "डब्बा", baseUnit = BaseUnitType.KG, conversionFactor = 15.0)
		)

		val factor = UnitConverterEngine.resolveConversionFactor("डब्बा", "kg", customAliases)
		assertNotNull(factor)
		assertEquals(15.0, factor!!, 0.0001)

		val converted = UnitConverterEngine.convertQuantity(2.0, "डब्बा", "kg", customAliases)
		assertEquals(30.0, converted!!, 0.0001)
	}

	@Test
	fun `incompatible categories return null`() {
		// Mass ("किलो") to Volume ("लीटर")
		val factor = UnitConverterEngine.resolveConversionFactor("किलो", "लीटर")
		assertNull(factor)

		val converted = UnitConverterEngine.convertQuantity(10.0, "किलो", "लीटर")
		assertNull(converted)
	}

	@Test
	fun `live cost calculator accurately prices Hindi recipe ingredients against English master catalog`() {
		// Recipe ingredient: "10 किलो काजू"
		// Master catalog: "काजू" priced at 700.0 per "kg"
		val ingredient = Ingredient(amount = 10.0, unit = "किलो", item = "काजू")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "काजू", unitCost = 700.0, baseUnit = "kg", currency = "INR")
		)

		val lineCost = LiveCostCalculator.calculateLineCost(ingredient, masterIngredients)

		assertEquals(CostStatus.MATCHED, lineCost.status)
		assertEquals(7000.00, lineCost.lineCost, 0.01)
		assertEquals(10.0, lineCost.effectiveAmount!!, 0.01)
	}

	@Test
	fun `live cost calculator accurately prices sub-unit Hindi ingredients`() {
		// Recipe ingredient: "500 ग्राम काजू"
		// Master catalog: "काजू" priced at 700.0 per "kg" -> 0.5 kg * 700 = 350.00
		val ingredient = Ingredient(amount = 500.0, unit = "ग्राम", item = "काजू")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "काजू", unitCost = 700.0, baseUnit = "kg", currency = "INR")
		)

		val lineCost = LiveCostCalculator.calculateLineCost(ingredient, masterIngredients)

		assertEquals(CostStatus.MATCHED, lineCost.status)
		assertEquals(350.00, lineCost.lineCost, 0.01)
		assertEquals(0.5, lineCost.effectiveAmount!!, 0.001)
	}

	@Test
	fun `live cost calculator supports custom database aliases during whole recipe cost calculation`() {
		// Recipe ingredients:
		// 1. "2 डब्बा" Ghee (custom alias: 1 डब्बा = 15 kg) -> 30 kg * 500 = 15,000
		// 2. "500 ग्राम" Sugar (0.5 kg * 40 = 20)
		val customAliases = mapOf(
			"डब्बा" to UnitAliasEntity(id = 101, aliasName = "डब्बा", baseUnit = BaseUnitType.KG, conversionFactor = 15.0)
		)

		val ingredients = listOf(
			Ingredient(amount = 2.0, unit = "डब्बा", item = "Ghee"),
			Ingredient(amount = 500.0, unit = "ग्राम", item = "Sugar")
		)

		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "Ghee", unitCost = 500.0, baseUnit = "kg", currency = "INR"),
			MasterIngredientEntity(id = 2, name = "Sugar", unitCost = 40.0, baseUnit = "kg", currency = "INR")
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = ingredients,
			masterIngredients = masterIngredients,
			yield = 10.0,
			currency = "INR",
			unitAliases = customAliases
		)

		assertEquals(15020.00, breakdown.totalCost, 0.01)
		assertEquals(1502.00, breakdown.costPerPortion, 0.01)
		assertEquals(0, breakdown.unpricedItemCount)
		assertEquals(CostStatus.MATCHED, breakdown.items[0].status)
		assertEquals(15000.00, breakdown.items[0].lineCost, 0.01)
		assertEquals(CostStatus.MATCHED, breakdown.items[1].status)
		assertEquals(20.00, breakdown.items[1].lineCost, 0.01)
	}
}
