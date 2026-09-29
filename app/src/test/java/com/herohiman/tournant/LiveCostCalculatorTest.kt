package com.herohiman.tournant

import com.herohiman.tournant.cost.CostStatus
import com.herohiman.tournant.cost.LiveCostCalculator
import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.room.MasterIngredientEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveCostCalculatorTest {

	@Test
	fun `calculate line cost for exact matching unit`() {
		val ingredient = Ingredient(amount = 250.0, unit = "g", item = "Flour")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "Flour", unitCost = 0.002, baseUnit = "g")
		)

		val result = LiveCostCalculator.calculateLineCost(ingredient, masterIngredients)

		assertEquals(CostStatus.MATCHED, result.status)
		assertEquals(0.50, result.lineCost, 0.001)
		assertEquals(250.0, result.effectiveAmount!!, 0.001)
	}

	@Test
	fun `calculate line cost with weight conversion grams to kilograms`() {
		// 500 grams of Sugar when master ingredient is priced at 2 dollars per kg
		val ingredient = Ingredient(amount = 500.0, unit = "g", item = "Sugar")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 2, name = "Sugar", unitCost = 2.0, baseUnit = "kg")
		)

		val result = LiveCostCalculator.calculateLineCost(ingredient, masterIngredients)

		assertEquals(CostStatus.MATCHED, result.status)
		// 500g = 0.5kg -> 0.5 * 2.0 = 1.00
		assertEquals(1.00, result.lineCost, 0.001)
		assertEquals(0.5, result.effectiveAmount!!, 0.001)
	}

	@Test
	fun `calculate line cost with weight conversion ounces to pounds`() {
		// 8 oz Butter when master ingredient is priced at 4 dollars per lb (1 lb = 16 oz, 8 oz = 0.5 lb -> $2.00)
		val ingredient = Ingredient(amount = 8.0, unit = "oz", item = "Butter")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 3, name = "Butter", unitCost = 4.0, baseUnit = "lb")
		)

		val result = LiveCostCalculator.calculateLineCost(ingredient, masterIngredients)

		assertEquals(CostStatus.MATCHED, result.status)
		assertEquals(2.00, result.lineCost, 0.01)
	}

	@Test
	fun `calculate line cost with volume conversion tablespoons to milliliters`() {
		// 2 tbsp Olive Oil, base unit ml at 0.02 dollars per ml
		// 2 tbsp = 2 * 14.7868 ml = 29.5736 ml -> 29.5736 * 0.02 = 0.59
		val ingredient = Ingredient(amount = 2.0, unit = "tbsp", item = "Olive Oil")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 4, name = "Olive Oil", unitCost = 0.02, baseUnit = "ml")
		)

		val result = LiveCostCalculator.calculateLineCost(ingredient, masterIngredients)

		assertEquals(CostStatus.MATCHED, result.status)
		assertEquals(0.59, result.lineCost, 0.01)
	}

	@Test
	fun `calculate line cost with volume conversion cups to liters`() {
		// 2 cups Milk, base unit liter at 1.50 dollars per l
		// 2 cups = 2 * 236.588 ml = 473.176 ml = 0.473176 l -> 0.473176 * 1.50 = 0.71
		val ingredient = Ingredient(amount = 2.0, unit = "cup", item = "Milk")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 5, name = "Milk", unitCost = 1.50, baseUnit = "l")
		)

		val result = LiveCostCalculator.calculateLineCost(ingredient, masterIngredients)

		assertEquals(CostStatus.MATCHED, result.status)
		assertEquals(0.71, result.lineCost, 0.01)
	}

	@Test
	fun `calculate line cost with count units pieces and units`() {
		val ingredient = Ingredient(amount = 3.0, unit = "pcs", item = "Egg")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 6, name = "Egg", unitCost = 0.30, baseUnit = "unit")
		)

		val result = LiveCostCalculator.calculateLineCost(ingredient, masterIngredients)

		assertEquals(CostStatus.MATCHED, result.status)
		assertEquals(0.90, result.lineCost, 0.001)
		assertEquals(3.0, result.effectiveAmount!!, 0.001)
	}

	@Test
	fun `calculate line cost with singular and plural name matching`() {
		// Recipe says "Bananas" (plural), catalog has "Banana" (singular)
		val ingredient = Ingredient(amount = 4.0, unit = "item", item = "Bananas")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 7, name = "Banana", unitCost = 0.25, baseUnit = "piece")
		)

		val result = LiveCostCalculator.calculateLineCost(ingredient, masterIngredients)

		assertEquals(CostStatus.MATCHED, result.status)
		assertEquals(1.00, result.lineCost, 0.001)
	}

	@Test
	fun `soft deleted inactive ingredient is flagged and excluded from active cost`() {
		val ingredient = Ingredient(amount = 100.0, unit = "g", item = "Vanilla Extract")
		val masterIngredients = listOf(
			MasterIngredientEntity(
				id = 8,
				name = "Vanilla Extract",
				unitCost = 0.15,
				baseUnit = "g",
				isActive = false // Soft-deleted
			)
		)

		val result = LiveCostCalculator.calculateLineCost(ingredient, masterIngredients)

		assertEquals(CostStatus.INACTIVE_PRICE, result.status)
		assertEquals(0.0, result.lineCost, 0.001)
	}

	@Test
	fun `missing master ingredient returns missing price status`() {
		val ingredient = Ingredient(amount = 1.0, unit = "tsp", item = "Saffron")
		val masterIngredients = emptyList<MasterIngredientEntity>()

		val result = LiveCostCalculator.calculateLineCost(ingredient, masterIngredients)

		assertEquals(CostStatus.MISSING_PRICE, result.status)
		assertEquals(0.0, result.lineCost, 0.001)
	}

	@Test
	fun `incompatible units returns incompatible status with zero cost`() {
		// Master ingredient in kg (weight), but ingredient specified in cups (volume) without density
		val ingredient = Ingredient(amount = 2.0, unit = "cup", item = "Flour")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 9, name = "Flour", unitCost = 1.20, baseUnit = "kg")
		)

		val result = LiveCostCalculator.calculateLineCost(ingredient, masterIngredients)

		assertEquals(CostStatus.INCOMPATIBLE_UNITS, result.status)
		assertEquals(0.0, result.lineCost, 0.001)
	}

	@Test
	fun `optional ingredient is excluded by default and included when requested`() {
		val optionalIngredient = Ingredient(amount = 50.0, unit = "g", item = "Chocolate Chips", optional = true)
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 10, name = "Chocolate Chips", unitCost = 0.02, baseUnit = "g")
		)

		// Excluded by default
		val excludedResult = LiveCostCalculator.calculateLineCost(optionalIngredient, masterIngredients, includeOptional = false)
		assertEquals(CostStatus.OPTIONAL_EXCLUDED, excludedResult.status)
		assertEquals(0.0, excludedResult.lineCost, 0.001)

		// Included when explicitly enabled
		val includedResult = LiveCostCalculator.calculateLineCost(optionalIngredient, masterIngredients, includeOptional = true)
		assertEquals(CostStatus.MATCHED, includedResult.status)
		assertEquals(1.00, includedResult.lineCost, 0.001)
	}

	@Test
	fun `calculate recipe total cost and portion cost`() {
		val ingredients = listOf(
			Ingredient(amount = 500.0, unit = "g", item = "Flour"), // 500 * 0.002 = $1.00
			Ingredient(amount = 200.0, unit = "g", item = "Sugar"), // 200 * 0.005 = $1.00
			Ingredient(amount = 2.0, unit = "unit", item = "Eggs")  // 2 * 0.50 = $1.00
		)
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "Flour", unitCost = 0.002, baseUnit = "g"),
			MasterIngredientEntity(id = 2, name = "Sugar", unitCost = 0.005, baseUnit = "g"),
			MasterIngredientEntity(id = 3, name = "Egg", unitCost = 0.50, baseUnit = "unit")
		)

		// Yield 6 servings
		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = ingredients,
			masterIngredients = masterIngredients,
			yield = 6.0
		)

		assertEquals(3.00, breakdown.totalCost, 0.001)
		assertEquals(0.50, breakdown.costPerPortion, 0.001)
		assertEquals(6.0, breakdown.yield, 0.001)
		assertEquals("₹3.00", breakdown.formattedTotalCost())
		assertEquals("₹0.50", breakdown.formattedCostPerPortion())
		assertEquals("$3.00", breakdown.formattedTotalCost(symbol = "$"))
	}

	@Test
	fun `calculate scaled recipe cost adjusts amounts and total proportionally`() {
		val ingredients = listOf(
			Ingredient(amount = 100.0, unit = "g", item = "Butter") // 100 * 0.01 = $1.00
		)
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "Butter", unitCost = 0.01, baseUnit = "g")
		)

		// Scale x3
		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = ingredients,
			masterIngredients = masterIngredients,
			yield = 2.0,
			scaleFactor = 3.0
		)

		// Butter should scale to 300g -> cost $3.00
		assertEquals(3.00, breakdown.totalCost, 0.001)
		assertEquals(6.0, breakdown.yield, 0.001)
		assertEquals(0.50, breakdown.costPerPortion, 0.001)
	}

	@Test
	fun `target selling price and margin calculations`() {
		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(
				Ingredient(amount = 10.0, unit = "unit", item = "Apples") // 10 * 0.50 = $5.00
			),
			masterIngredients = listOf(
				MasterIngredientEntity(id = 1, name = "Apples", unitCost = 0.50, baseUnit = "unit")
			),
			yield = 5.0
		)

		assertEquals(5.00, breakdown.totalCost, 0.001)
		assertEquals(1.00, breakdown.costPerPortion, 0.001)

		// At 25% target food cost (0.25): selling price should be 5.00 / 0.25 = 20.00
		val targetPrice = breakdown.calculateTargetSellingPrice(0.25)
		assertEquals(20.00, targetPrice, 0.01)

		// Portion selling price at 25% food cost: 1.00 / 0.25 = 4.00
		val portionTargetPrice = breakdown.calculatePortionSellingPrice(0.25)
		assertEquals(4.00, portionTargetPrice, 0.01)

		// Margin percentage at selling price $20.00 on total cost $5.00: (20 - 5) / 20 = 75%
		val margin = breakdown.calculateMarginPercentage(20.0)
		assertEquals(75.0, margin, 0.01)
	}

	@Test
	fun `privacy mode masks monetary amounts in formatted output`() {
		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(
				Ingredient(amount = 100.0, unit = "g", item = "Truffle")
			),
			masterIngredients = listOf(
				MasterIngredientEntity(id = 1, name = "Truffle", unitCost = 2.50, baseUnit = "g")
			),
			yield = 2.0,
			isPrivacyMode = true
		)

		assertTrue(breakdown.isPrivacyMode)
		assertEquals("••••", breakdown.formattedTotalCost())
		assertEquals("••••", breakdown.formattedCostPerPortion())
		assertEquals("---", breakdown.formattedTotalCost(mask = "---"))
	}

	@Test
	fun `recipe preview helper formats live cost preview correctly`() {
		val ingredients = listOf(
			Ingredient(amount = 200.0, unit = "g", item = "Flour")
		)
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "Flour", unitCost = 0.01, baseUnit = "g")
		)

		val preview = com.herohiman.tournant.ui.preview.RecipePreviewHelper.formatCostForPreview(
			ingredients = ingredients,
			masterIngredients = masterIngredients,
			yieldValue = 2.0,
			isPrivacyMode = false
		)

		assertEquals("Cost: ₹2.00 (₹1.00 / serving)", preview)

		val customSymbolPreview = com.herohiman.tournant.ui.preview.RecipePreviewHelper.formatCostForPreview(
			ingredients = ingredients,
			masterIngredients = masterIngredients,
			yieldValue = 2.0,
			isPrivacyMode = false,
			symbol = "$"
		)
		assertEquals("Cost: $2.00 ($1.00 / serving)", customSymbolPreview)
	}

	@Test
	fun `recipe preview helper masks cost preview in privacy mode`() {
		val ingredients = listOf(
			Ingredient(amount = 200.0, unit = "g", item = "Flour")
		)
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "Flour", unitCost = 0.01, baseUnit = "g")
		)

		val preview = com.herohiman.tournant.ui.preview.RecipePreviewHelper.formatCostForPreview(
			ingredients = ingredients,
			masterIngredients = masterIngredients,
			yieldValue = 2.0,
			isPrivacyMode = true
		)

		assertEquals("Cost: •••• (•••• / serving)", preview)
	}

	@Test
	fun `calculate line cost for inactive substitute returns zero cost and SUBSTITUTE_INACTIVE status`() {
		val inactiveSugar = Ingredient(
			amount = 750.0,
			unit = "g",
			item = "Sugar",
			substituteGroupId = "sweetener_group",
			isActiveSubstitute = false
		)
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "Sugar", unitCost = 0.04, baseUnit = "g")
		)

		val result = LiveCostCalculator.calculateLineCost(inactiveSugar, masterIngredients)

		assertEquals(CostStatus.SUBSTITUTE_INACTIVE, result.status)
		assertEquals(0.0, result.lineCost, 0.001)
		assertNotNull(result.masterIngredient)
		assertEquals("Sugar", result.masterIngredient?.name)
	}

	@Test
	fun `recipe cost calculation includes active substitute and excludes inactive substitute without double counting`() {
		// Base: 500g Gud (at $0.10/g = $50) OR 750g Sugar (at $0.04/g = $30), plus 1000g Flour (at $0.002/g = $2)
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "Gud (Jaggery)", unitCost = 0.10, baseUnit = "g"),
			MasterIngredientEntity(id = 2, name = "Sugar", unitCost = 0.04, baseUnit = "g"),
			MasterIngredientEntity(id = 3, name = "Flour", unitCost = 0.002, baseUnit = "g")
		)

		// State A: Gud is active ($50), Sugar is inactive ($0), Flour ($2) -> Total = $52
		val ingredientsStateA = listOf(
			Ingredient(amount = 1000.0, unit = "g", item = "Flour"),
			Ingredient(amount = 500.0, unit = "g", item = "Gud (Jaggery)", substituteGroupId = "sweetener_1", isActiveSubstitute = true),
			Ingredient(amount = 750.0, unit = "g", item = "Sugar", substituteGroupId = "sweetener_1", isActiveSubstitute = false)
		)

		val breakdownA = LiveCostCalculator.calculateRecipeCost(ingredientsStateA, masterIngredients, yield = 1.0)
		assertEquals(52.00, breakdownA.totalCost, 0.01)
		assertEquals(52.00, breakdownA.costPerPortion, 0.01)
		assertEquals(0, breakdownA.unpricedItemCount)

		val gudItem = breakdownA.items.first { it.ingredient.item == "Gud (Jaggery)" }
		assertEquals(CostStatus.MATCHED, gudItem.status)
		assertEquals(50.00, gudItem.lineCost, 0.01)

		val sugarItem = breakdownA.items.first { it.ingredient.item == "Sugar" }
		assertEquals(CostStatus.SUBSTITUTE_INACTIVE, sugarItem.status)
		assertEquals(0.0, sugarItem.lineCost, 0.01)

		// State B: User toggles to Sugar: Sugar is active ($30), Gud is inactive ($0), Flour ($2) -> Total = $32
		val ingredientsStateB = listOf(
			Ingredient(amount = 1000.0, unit = "g", item = "Flour"),
			Ingredient(amount = 500.0, unit = "g", item = "Gud (Jaggery)", substituteGroupId = "sweetener_1", isActiveSubstitute = false),
			Ingredient(amount = 750.0, unit = "g", item = "Sugar", substituteGroupId = "sweetener_1", isActiveSubstitute = true)
		)

		val breakdownB = LiveCostCalculator.calculateRecipeCost(ingredientsStateB, masterIngredients, yield = 1.0)
		assertEquals(32.00, breakdownB.totalCost, 0.01)
		assertEquals(32.00, breakdownB.costPerPortion, 0.01)
		assertEquals(0, breakdownB.unpricedItemCount)
	}

	@Test
	fun `recipe cost calculation enforces Rule of One when multiple substitutes in a group are marked true`() {
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "Gud", unitCost = 0.10, baseUnit = "g"),
			MasterIngredientEntity(id = 2, name = "Sugar Syrup", unitCost = 0.04, baseUnit = "g"),
			MasterIngredientEntity(id = 3, name = "Honey", unitCost = 0.30, baseUnit = "g")
		)

		// Erroneously both Gud and Honey marked active in same group
		val ingredients = listOf(
			Ingredient(amount = 500.0, unit = "g", item = "Gud", substituteGroupId = "sweetener_n", isActiveSubstitute = true),
			Ingredient(amount = 750.0, unit = "g", item = "Sugar Syrup", substituteGroupId = "sweetener_n", isActiveSubstitute = false),
			Ingredient(amount = 400.0, unit = "g", item = "Honey", substituteGroupId = "sweetener_n", isActiveSubstitute = true)
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(ingredients, masterIngredients, yield = 1.0)

		// Rule of One: only first active (Gud, 500g * 0.10 = $50) should be included; Honey ($120) normalized to inactive
		assertEquals(50.00, breakdown.totalCost, 0.01)
		val honeyItem = breakdown.items.first { it.ingredient.item == "Honey" }
		assertEquals(CostStatus.SUBSTITUTE_INACTIVE, honeyItem.status)
		assertEquals(0.0, honeyItem.lineCost, 0.01)
	}

	@Test
	fun `recipe cost calculation handles multiple distinct substitute groups independently`() {
		val masterIngredients = listOf(
			// Sweeteners
			MasterIngredientEntity(id = 1, name = "Gud", unitCost = 0.10, baseUnit = "g"),
			MasterIngredientEntity(id = 2, name = "Sugar", unitCost = 0.04, baseUnit = "g"),
			// Fats
			MasterIngredientEntity(id = 3, name = "Butter", unitCost = 0.05, baseUnit = "g"),
			MasterIngredientEntity(id = 4, name = "Margarine", unitCost = 0.02, baseUnit = "g"),
			// Base
			MasterIngredientEntity(id = 5, name = "Flour", unitCost = 0.002, baseUnit = "g")
		)

		// Group 1: Sweetener (Sugar active: 750g * 0.04 = $30)
		// Group 2: Fat (Butter active: 200g * 0.05 = $10)
		// Base: Flour (1000g * 0.002 = $2)
		// Total expected = $42.00
		val ingredients = listOf(
			Ingredient(amount = 1000.0, unit = "g", item = "Flour"),
			Ingredient(amount = 500.0, unit = "g", item = "Gud", substituteGroupId = "group_sweet", isActiveSubstitute = false),
			Ingredient(amount = 750.0, unit = "g", item = "Sugar", substituteGroupId = "group_sweet", isActiveSubstitute = true),
			Ingredient(amount = 200.0, unit = "g", item = "Butter", substituteGroupId = "group_fat", isActiveSubstitute = true),
			Ingredient(amount = 200.0, unit = "g", item = "Margarine", substituteGroupId = "group_fat", isActiveSubstitute = false)
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(ingredients, masterIngredients, yield = 2.0)

		assertEquals(42.00, breakdown.totalCost, 0.01)
		assertEquals(21.00, breakdown.costPerPortion, 0.01)
	}

	@Test
	fun `filterActiveIngredients and calculateActiveBatchMassGrams only count active ingredients`() {
		val ingredients = listOf(
			Ingredient(amount = 1000.0, unit = "g", item = "Flour"),
			Ingredient(amount = 500.0, unit = "g", item = "Gud", substituteGroupId = "sweet_grp", isActiveSubstitute = true),
			Ingredient(amount = 750.0, unit = "g", item = "Sugar", substituteGroupId = "sweet_grp", isActiveSubstitute = false)
		)

		val activeList = LiveCostCalculator.filterActiveIngredients(ingredients)
		assertEquals(2, activeList.size)
		assertTrue(activeList.any { it.item == "Flour" })
		assertTrue(activeList.any { it.item == "Gud" })
		assertFalse(activeList.any { it.item == "Sugar" })

		val totalGrams = LiveCostCalculator.calculateActiveBatchMassGrams(ingredients)
		// 1000g Flour + 500g Gud = 1500g (Sugar's 750g excluded)
		assertEquals(1500.0, totalGrams, 0.001)
	}
}
