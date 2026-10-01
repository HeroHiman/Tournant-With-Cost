package com.herohiman.tournant

import com.herohiman.tournant.cost.LiveCostCalculator
import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.IngredientLine.IngredientItem
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.ui.preview.RecipePreviewHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeViewerLineCostUiTest {

	@Test
	fun `recipe viewer preview renders line item costs alongside ingredients`() {
		val kaju = Ingredient(amount = 10.0, unit = "kg", item = "काजू")
		val sugar = Ingredient(amount = 5.0, unit = "kg", item = "Sugar")
		val water = Ingredient(amount = 1.0, unit = "l", item = "Water") // unpriced

		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "काजू", unitCost = 700.0, baseUnit = "kg"),
			MasterIngredientEntity(id = 2, name = "Sugar", unitCost = 40.0, baseUnit = "kg")
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju, sugar, water),
			masterIngredients = masterIngredients
		)

		val items = listOf(
			IngredientItem(kaju),
			IngredientItem(sugar),
			IngredientItem(water)
		)

		val previewText = RecipePreviewHelper.formatIngredientsForPreview(
			ingredients = items,
			costBreakdown = breakdown
		)

		assertTrue(previewText.contains("10 kg काजू (₹7000.00)"))
		assertTrue(previewText.contains("5 kg Sugar (₹200.00)"))
		assertTrue(previewText.contains("1 l Water"))
		// Unpriced item should not have cost parentheses
		assertTrue(!previewText.contains("Water ("))
	}

	@Test
	fun `recipe viewer preview displays active and inactive substitute options with their respective line costs`() {
		val gud = Ingredient(amount = 500.0, unit = "g", item = "Gud", substituteGroupId = "grp_1", isActiveSubstitute = true)
		val sugarSyrup = Ingredient(amount = 750.0, unit = "g", item = "Sugar Syrup", substituteGroupId = "grp_1", isActiveSubstitute = false)

		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "Gud", unitCost = 100.0, baseUnit = "kg"), // ₹50
			MasterIngredientEntity(id = 2, name = "Sugar Syrup", unitCost = 40.0, baseUnit = "kg") // ₹30
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(gud, sugarSyrup),
			masterIngredients = masterIngredients
		)

		val items = listOf(
			IngredientItem(gud),
			IngredientItem(sugarSyrup)
		)

		val previewText = RecipePreviewHelper.formatIngredientsForPreview(
			ingredients = items,
			costBreakdown = breakdown
		)

		assertTrue(previewText.contains("- 500 g Gud *(Active)* (₹50.00)"))
		assertTrue(previewText.contains("*OR* 750 g Sugar Syrup (₹30.00)"))
	}

	@Test
	fun `recipe viewer preview masks line item costs when privacy mode is active`() {
		val kaju = Ingredient(amount = 10.0, unit = "kg", item = "काजू")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "काजू", unitCost = 700.0, baseUnit = "kg")
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju),
			masterIngredients = masterIngredients,
			isPrivacyMode = true
		)

		val previewText = RecipePreviewHelper.formatIngredientsForPreview(
			ingredients = listOf(IngredientItem(kaju)),
			costBreakdown = breakdown
		)

		assertTrue(previewText.contains("10 kg काजू (••••)"))
	}

	@Test
	fun `costBreakdown formattedCostForIngredient matches line-item UI requirements`() {
		val kaju = Ingredient(amount = 10.0, unit = "kg", item = "काजू")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "काजू", unitCost = 700.0, baseUnit = "kg")
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju),
			masterIngredients = masterIngredients
		)

		val formattedCost = breakdown.formattedCostForIngredient(kaju)
		assertEquals("₹7000.00", formattedCost)
	}

	@Test
	fun `recipe viewer cost calculation handles null item names and missing sub recipes safely`() {
		val emptyItem = Ingredient(amount = null, unit = null, item = null, refId = 9999L)
		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(emptyItem),
			masterIngredients = emptyList(),
			subRecipeResolver = { null },
			currentRecipeId = 1L
		)
		assertNotNull(breakdown)
		assertEquals(0.0, breakdown.totalCost, 0.001)
		assertEquals(1, breakdown.unpricedItemCount)
		assertNull(breakdown.formattedCostForIngredient(emptyItem))
	}

	@Test
	fun `output summary dashboard correctly prepares output, total batch cost, and cost per kg metrics`() {
		val kaju = Ingredient(amount = 10.0, unit = "kg", item = "काजू")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "काजू", unitCost = 700.0, baseUnit = "kg")
		)
		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju),
			masterIngredients = masterIngredients
		)

		val yieldUnit = "1 टीपा काजू (10 किलो)"
		val parsedYield = com.herohiman.tournant.cost.YieldParser.parseWeightInKg(
			yieldValue = 1.0,
			yieldUnit = yieldUnit,
			scaleFactor = 1.0
		)
		assertNotNull(parsedYield)
		val displayOutput = "${parsedYield!!.rawAmount.toStringForCooks()} ${parsedYield.rawUnit}"
		assertEquals("10 किलो", displayOutput)

		val displayTotalCost = breakdown.formattedTotalCost()
		assertEquals("₹7000.00", displayTotalCost)

		val costPerKg = com.herohiman.tournant.cost.YieldParser.calculateCostPerKg(
			totalCost = breakdown.totalCost,
			yieldValue = 1.0,
			yieldUnit = yieldUnit,
			scaleFactor = 1.0
		)
		assertNotNull(costPerKg)
		val displayCostPerKg = "${com.herohiman.tournant.cost.CostCurrencyFormatter.formatAmount(costPerKg!!)} / kg"
		assertEquals("₹700.00 / kg", displayCostPerKg)
	}

	@Test
	fun `automatic sum of active ingredient weights calculates true cost per kg 10kg plus 9kg equals 19kg`() {
		val kaju = Ingredient(amount = 10.0, unit = "kg", item = "काजू")
		val sugar = Ingredient(amount = 9.0, unit = "kg", item = "Sugar")
		val packagingNote = Ingredient(amount = 17.0, unit = "kg", item = "डब्बा में पैक", isInformationalOnly = true)

		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "काजू", unitCost = 700.0, baseUnit = "kg"), // ₹7000.00
			MasterIngredientEntity(id = 2, name = "Sugar", unitCost = 48.0, baseUnit = "kg")   // ₹432.00
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju, sugar, packagingNote),
			masterIngredients = masterIngredients
		)

		// Total cost: 7000 + 432 = 7432.00
		assertEquals(7432.00, breakdown.totalCost, 0.001)
		// Active weight sum: 10 + 9 = 19 kg (packagingNote is excluded)
		assertNotNull(breakdown.totalMassInKg)
		assertEquals(19.0, breakdown.totalMassInKg!!, 0.001)

		// Cost per kg: 7432.00 / 19.0 = 391.15789 -> 391.16
		assertNotNull(breakdown.costPerKg)
		assertEquals(391.16, breakdown.costPerKg!!, 0.001)
		assertEquals("₹391.16", breakdown.formattedCostPerKg())
	}

	@Test
	fun `explicit yield override takes precedence over sum of raw ingredient weights due to evaporation`() {
		val kaju = Ingredient(amount = 10.0, unit = "kg", item = "काजू")
		val sugar = Ingredient(amount = 9.0, unit = "kg", item = "Sugar")

		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "काजू", unitCost = 700.0, baseUnit = "kg"),
			MasterIngredientEntity(id = 2, name = "Sugar", unitCost = 48.0, baseUnit = "kg")
		)

		// Total raw materials = 19 kg, but boiled down to 17 kg
		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju, sugar),
			masterIngredients = masterIngredients,
			explicitYieldWeightKg = 17.0
		)

		assertEquals(7432.00, breakdown.totalCost, 0.001)
		assertEquals(17.0, breakdown.totalMassInKg!!, 0.001)

		// Cost per kg: 7432.00 / 17.0 = 437.176 -> 437.18
		assertEquals(437.18, breakdown.costPerKg!!, 0.001)
		assertEquals("₹437.18", breakdown.formattedCostPerKg())
	}

	@Test
	fun `quick-edit pricing resolves master ingredient and recalculates live on price update`() {
		val kaju = Ingredient(amount = 10.0, unit = "kg", item = "काजू")
		val sugar = Ingredient(amount = 9.0, unit = "kg", item = "Sugar")

		val initialMasters = listOf(
			MasterIngredientEntity(id = 101L, name = "काजू", unitCost = 700.0, baseUnit = "kg"),
			MasterIngredientEntity(id = 102L, name = "Sugar", unitCost = 48.0, baseUnit = "kg")
		)

		val initialBreakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju, sugar),
			masterIngredients = initialMasters
		)

		// 1. Verify Story 14.1 & 14.2: findCostItem resolves master ingredient accurately
		val kajuCostItem = initialBreakdown.findCostItem(kaju)
		assertNotNull(kajuCostItem)
		assertNotNull(kajuCostItem?.masterIngredient)
		assertEquals(101L, kajuCostItem?.masterIngredient?.id)
		assertEquals(700.0, kajuCostItem?.masterIngredient?.unitCost ?: 0.0, 0.001)
		assertEquals("₹7000.00", kajuCostItem?.formattedCost())
		assertEquals(7432.00, initialBreakdown.totalCost, 0.001)
		assertEquals(391.16, initialBreakdown.costPerKg ?: 0.0, 0.001)

		// 2. Simulate Story 14.3: User edits "काजू" price to ₹750.00/kg
		val updatedMasters = initialMasters.map {
			if (it.id == 101L) it.copy(unitCost = 750.0) else it
		}

		val recalculatedBreakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju, sugar),
			masterIngredients = updatedMasters
		)

		// Total cost: 7500 + 432 = 7932.00
		val updatedKajuCostItem = recalculatedBreakdown.findCostItem(kaju)
		assertNotNull(updatedKajuCostItem)
		assertEquals("₹7500.00", updatedKajuCostItem?.formattedCost())
		assertEquals(7932.00, recalculatedBreakdown.totalCost, 0.001)
		// Cost per kg: 7932.00 / 19 = 417.47368 -> 417.47
		assertEquals(417.47, recalculatedBreakdown.costPerKg ?: 0.0, 0.001)
		assertEquals("₹417.47", recalculatedBreakdown.formattedCostPerKg())
	}

	@Test
	fun `quick-edit pricing for unpriced ingredient activates line cost and updates totals`() {
		val pista = Ingredient(amount = 2.0, unit = "kg", item = "पिस्ता")
		val sugar = Ingredient(amount = 5.0, unit = "kg", item = "Sugar")

		val initialMasters = listOf(
			MasterIngredientEntity(id = 201L, name = "Sugar", unitCost = 40.0, baseUnit = "kg")
		)

		val initialBreakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(pista, sugar),
			masterIngredients = initialMasters
		)

		assertEquals(1, initialBreakdown.unpricedItemCount)
		assertEquals(200.0, initialBreakdown.totalCost, 0.001)
		assertNull(initialBreakdown.formattedCostForIngredient(pista))

		// User adds price for "पिस्ता" at ₹1200/kg
		val updatedMasters = initialMasters + MasterIngredientEntity(id = 202L, name = "पिस्ता", unitCost = 1200.0, baseUnit = "kg")

		val recalculatedBreakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(pista, sugar),
			masterIngredients = updatedMasters
		)

		assertEquals(0, recalculatedBreakdown.unpricedItemCount)
		assertEquals("₹2400.00", recalculatedBreakdown.formattedCostForIngredient(pista))
		assertEquals(2600.00, recalculatedBreakdown.totalCost, 0.001)
		// Total mass: 2 kg + 5 kg = 7 kg -> 2600 / 7 = 371.43
		assertEquals(371.43, recalculatedBreakdown.costPerKg ?: 0.0, 0.001)
	}
}
