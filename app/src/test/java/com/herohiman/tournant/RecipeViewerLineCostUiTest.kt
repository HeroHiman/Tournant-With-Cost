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

	@Test
	fun `alias mapping resolves unpriced ingredient alias and recalculates live on binding`() {
		val kajuTukda = Ingredient(amount = 10.0, unit = "kg", item = "Kaju Tukda")
		val sugar = Ingredient(amount = 9.0, unit = "kg", item = "Sugar")

		val masterKaju = MasterIngredientEntity(id = 501L, name = "काजू", unitCost = 700.0, baseUnit = "kg")
		val masterSugar = MasterIngredientEntity(id = 502L, name = "Sugar", unitCost = 48.0, baseUnit = "kg")
		val masterIngredients = listOf(masterKaju, masterSugar)

		// 1. Without alias: "Kaju Tukda" is unpriced
		val unmappedBreakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kajuTukda, sugar),
			masterIngredients = masterIngredients,
			aliases = emptyMap()
		)

		assertEquals(1, unmappedBreakdown.unpricedItemCount)
		assertNull(unmappedBreakdown.formattedCostForIngredient(kajuTukda))
		assertEquals(432.0, unmappedBreakdown.totalCost, 0.001)

		// 2. Map "Kaju Tukda" alias to "काजू" master ingredient
		val aliases = mapOf("kaju tukda" to masterKaju)
		val mappedBreakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kajuTukda, sugar),
			masterIngredients = masterIngredients,
			aliases = aliases
		)

		assertEquals(0, mappedBreakdown.unpricedItemCount)
		assertNotNull(mappedBreakdown.findCostItem(kajuTukda))
		assertEquals(501L, mappedBreakdown.findCostItem(kajuTukda)?.masterIngredient?.id)
		assertEquals("₹7000.00", mappedBreakdown.formattedCostForIngredient(kajuTukda))
		assertEquals(7432.00, mappedBreakdown.totalCost, 0.001)
		assertEquals(391.16, mappedBreakdown.costPerKg ?: 0.0, 0.001)
	}

	@Test
	fun `batch scaling proportionally multiplies amounts and costs across all presets`() {
		val kaju = Ingredient(amount = 10.0, unit = "kg", item = "काजू")
		val sugar = Ingredient(amount = 9.0, unit = "kg", item = "Sugar")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1L, name = "काजू", unitCost = 700.0, baseUnit = "kg"),
			MasterIngredientEntity(id = 2L, name = "Sugar", unitCost = 48.0, baseUnit = "kg")
		)

		// Base (1.0x): ₹7432.00, 19 kg, ₹391.16/kg
		val baseBreakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju, sugar),
			masterIngredients = masterIngredients
		)
		assertEquals(7432.00, baseBreakdown.totalCost, 0.001)
		assertEquals(19.0, baseBreakdown.totalMassInKg ?: 0.0, 0.001)
		assertEquals(391.16, baseBreakdown.costPerKg ?: 0.0, 0.001)

		// 2.0x Batch Scaler
		val scaled2xIngredients = listOf(
			kaju.copy(amount = 10.0 * 2.0),
			sugar.copy(amount = 9.0 * 2.0)
		)
		val scaled2xBreakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = scaled2xIngredients,
			masterIngredients = masterIngredients
		)
		assertEquals(14864.00, scaled2xBreakdown.totalCost, 0.001)
		assertEquals(38.0, scaled2xBreakdown.totalMassInKg ?: 0.0, 0.001)
		assertEquals(391.16, scaled2xBreakdown.costPerKg ?: 0.0, 0.001)

		// 0.5x Batch Scaler
		val scaledHalfIngredients = listOf(
			kaju.copy(amount = 10.0 * 0.5),
			sugar.copy(amount = 9.0 * 0.5)
		)
		val scaledHalfBreakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = scaledHalfIngredients,
			masterIngredients = masterIngredients
		)
		assertEquals(3716.00, scaledHalfBreakdown.totalCost, 0.001)
		assertEquals(9.5, scaledHalfBreakdown.totalMassInKg ?: 0.0, 0.001)
		assertEquals(391.16, scaledHalfBreakdown.costPerKg ?: 0.0, 0.001)

		// 10.0x Commercial Scale
		val scaled10xIngredients = listOf(
			kaju.copy(amount = 10.0 * 10.0),
			sugar.copy(amount = 9.0 * 10.0)
		)
		val scaled10xBreakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = scaled10xIngredients,
			masterIngredients = masterIngredients
		)
		assertEquals(74320.00, scaled10xBreakdown.totalCost, 0.001)
		assertEquals(190.0, scaled10xBreakdown.totalMassInKg ?: 0.0, 0.001)
		assertEquals(391.16, scaled10xBreakdown.costPerKg ?: 0.0, 0.001)
	}

	@Test
	fun `cook mode masks costs across line items, total batch cost, and cost per kg`() {
		val kaju = Ingredient(amount = 10.0, unit = "kg", item = "काजू")
		val sugar = Ingredient(amount = 9.0, unit = "kg", item = "Sugar")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1L, name = "काजू", unitCost = 700.0, baseUnit = "kg"),
			MasterIngredientEntity(id = 2L, name = "Sugar", unitCost = 48.0, baseUnit = "kg")
		)

		val cookModeBreakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju, sugar),
			masterIngredients = masterIngredients,
			isPrivacyMode = true
		)

		// Line costs are masked
		assertEquals("••••", cookModeBreakdown.formattedCostForIngredient(kaju))
		assertEquals("••••", cookModeBreakdown.formattedCostForIngredient(sugar))

		// Header dashboard metrics are masked
		assertEquals("••••", cookModeBreakdown.formattedTotalCost())
		assertEquals("••••", cookModeBreakdown.formattedCostPerKg())

		// Internal calculation still preserved for unmasking
		assertEquals(7432.00, cookModeBreakdown.totalCost, 0.001)
		assertEquals(19.0, cookModeBreakdown.totalMassInKg ?: 0.0, 0.001)
	}

	@Test
	fun `group-level informational exclusion zone excludes total mal, dabba, and tukda notes from mass sum and costing`() {
		// Raw materials
		val kaju = Ingredient(amount = 10.0, unit = "kg", item = "काजू", group = "सामग्री")
		val sugar = Ingredient(amount = 9.0, unit = "kg", item = "Sugar", group = "सामग्री")

		// Operational notes under informational groups "टोटल माल" and "जानकारी"
		val totalMal = Ingredient(amount = 19.0, unit = "kg", item = "टोटल माल", group = "टोटल माल")
		val dabbaPack = Ingredient(amount = 17.0, unit = "kg", item = "डब्बा में पैक", group = "जानकारी")
		val tukda = Ingredient(amount = 2.0, unit = "kg", item = "टुकड़ा", group = "जानकारी")

		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1L, name = "काजू", unitCost = 700.0, baseUnit = "kg"), // ₹7000.00
			MasterIngredientEntity(id = 2L, name = "Sugar", unitCost = 48.0, baseUnit = "kg")   // ₹432.00
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju, sugar, totalMal, dabbaPack, tukda),
			masterIngredients = masterIngredients
		)

		// 1. Total Output must NOT be 57 kg; it must strictly sum only the raw materials (10 + 9 = 19 kg)
		assertEquals(19.0, breakdown.totalMassInKg ?: 0.0, 0.001)

		// 2. Batch Cost is exactly 7000 + 432 = ₹7432.00
		assertEquals(7432.00, breakdown.totalCost, 0.001)

		// 3. Cost Per Kg is 7432 / 19 = ₹391.16/kg
		assertEquals(391.16, breakdown.costPerKg ?: 0.0, 0.001)
		assertEquals("₹391.16", breakdown.formattedCostPerKg())

		// 4. Operational lines under informational groups MUST NOT trigger unpriced warnings
		assertEquals(0, breakdown.unpricedItemCount)

		// 5. Line costs for operational items are ₹0.00 and have INFORMATIONAL_EXCLUDED status
		val totalMalCost = breakdown.findCostItem(totalMal)
		assertNotNull(totalMalCost)
		assertEquals(com.herohiman.tournant.cost.CostStatus.INFORMATIONAL_EXCLUDED, totalMalCost?.status)
		assertNull(breakdown.formattedCostForIngredient(totalMal))

		val dabbaCost = breakdown.findCostItem(dabbaPack)
		assertNotNull(dabbaCost)
		assertEquals(com.herohiman.tournant.cost.CostStatus.INFORMATIONAL_EXCLUDED, dabbaCost?.status)

		val tukdaCost = breakdown.findCostItem(tukda)
		assertNotNull(tukdaCost)
		assertEquals(com.herohiman.tournant.cost.CostStatus.INFORMATIONAL_EXCLUDED, tukdaCost?.status)
	}

	@Test
	fun `group-level informational toggle in extensions cascades isInformationalOnly across group items`() {
		val items = mutableListOf(
			Ingredient(amount = 10.0, unit = "kg", item = "काजू", group = "सामग्री"),
			Ingredient(amount = 19.0, unit = "kg", item = "टोटल माल", group = "टोटल माल"),
			Ingredient(amount = 17.0, unit = "kg", item = "डब्बा में पैक", group = "टोटल माल")
		)

		// Add group titles
		val lines = items.addGroupTitles()
		val groupTitle = lines.filterIsInstance<com.herohiman.tournant.data.IngredientLine.IngredientGroupTitle>()
			.firstOrNull { it.title == "टोटल माल" }
		assertNotNull(groupTitle)
		assertTrue(groupTitle!!.isInformationalGroup)

		// Converting back via hideGroupTitles cascades isInformationalOnly = true
		val persisted = lines.hideGroupTitles()
		val persistedTotalMal = persisted.first { it.item == "टोटल माल" }
		val persistedDabba = persisted.first { it.item == "डब्बा में पैक" }
		val persistedKaju = persisted.first { it.item == "काजू" }

		assertTrue(persistedTotalMal.isInformationalOnly)
		assertTrue(persistedDabba.isInformationalOnly)
		assertTrue(!persistedKaju.isInformationalOnly)
	}
}
