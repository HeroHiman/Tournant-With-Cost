package com.herohiman.tournant

import com.herohiman.tournant.cost.CostStatus
import com.herohiman.tournant.cost.LiveCostCalculator
import com.herohiman.tournant.cost.SubRecipeData
import com.herohiman.tournant.cost.SubRecipeResolver
import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.room.MasterIngredientEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecursiveSubRecipeCostCalculatorTest {

	@Test
	fun `calculate sub recipe cost derived from milk to khoya with yield ratio`() {
		// Raw Milk master ingredient: $2.00 per liter
		val masterMilk = MasterIngredientEntity(
			id = 1,
			name = "Milk",
			unitCost = 2.0,
			baseUnit = "l"
		)

		// Khoya master ingredient: derived from recipe 101 ("Khoya Boiling"), yield ratio 0.20 (20% yield)
		val masterKhoya = MasterIngredientEntity(
			id = 2,
			name = "Khoya",
			unitCost = 0.0, // derived from recipe
			baseUnit = "kg",
			linkedRecipeId = 101L,
			yieldRatio = 0.20
		)

		val masterIngredients = listOf(masterMilk, masterKhoya)

		// Sub-recipe 101: 5 Liters of Milk
		val subRecipes = mapOf(
			101L to SubRecipeData(
				recipeId = 101L,
				title = "Khoya Boiling",
				ingredients = listOf(
					Ingredient(amount = 5.0, unit = "l", item = "Milk")
				),
				yieldValue = 1.0,
				yieldUnit = "batch"
			)
		)

		val resolver = SubRecipeResolver { id -> subRecipes[id] }

		// Parent Recipe: Gulab Jamun uses 200 grams of Khoya
		val gulabJamunIngredients = listOf(
			Ingredient(amount = 200.0, unit = "g", item = "Khoya")
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = gulabJamunIngredients,
			masterIngredients = masterIngredients,
			subRecipeResolver = resolver,
			currentRecipeId = 201L
		)

		// Math verification:
		// Sub-recipe 101 cost: 5L * $2.00/L = $10.00
		// Derived Khoya cost per kg: $10.00 / 0.20 = $50.00 / kg
		// 200g of Khoya = 0.200 kg * $50.00 = $10.00
		assertEquals(10.00, breakdown.totalCost, 0.001)
		assertEquals(1, breakdown.items.size)

		val khoyaItem = breakdown.items[0]
		assertEquals(CostStatus.MATCHED, khoyaItem.status)
		assertTrue(khoyaItem.isDerivedFromSubRecipe)
		assertEquals(101L, khoyaItem.derivedRecipeId)
		assertEquals(50.00, khoyaItem.derivedUnitCost!!, 0.001)
		assertEquals(10.00, khoyaItem.lineCost, 0.001)
	}

	@Test
	fun `sub recipe with overhead included in derived cost`() {
		val masterMilk = MasterIngredientEntity(
			id = 1,
			name = "Milk",
			unitCost = 2.0,
			baseUnit = "l"
		)

		// Khoya with 0.50 yield ratio and gas overhead
		val masterKhoya = MasterIngredientEntity(
			id = 2,
			name = "Khoya",
			unitCost = 0.0,
			baseUnit = "kg",
			linkedRecipeId = 101L,
			yieldRatio = 0.50
		)

		// 5L milk ($10.00) + $5.00 gas overhead = $15.00 total
		val subRecipes = mapOf(
			101L to SubRecipeData(
				recipeId = 101L,
				title = "Khoya with Gas",
				ingredients = listOf(
					Ingredient(amount = 5.0, unit = "l", item = "Milk")
				),
				yieldValue = 1.0,
				overheadCost = 5.0
			)
		)

		val resolver = SubRecipeResolver { id -> subRecipes[id] }

		val parentIngredients = listOf(
			Ingredient(amount = 1.0, unit = "kg", item = "Khoya")
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = parentIngredients,
			masterIngredients = listOf(masterMilk, masterKhoya),
			subRecipeResolver = resolver,
			currentRecipeId = 301L
		)

		// ($10.00 + $5.00) / 0.50 = $30.00 per kg
		assertEquals(30.00, breakdown.totalCost, 0.001)
		val item = breakdown.items[0]
		assertEquals(30.00, item.derivedUnitCost!!, 0.001)
		assertEquals(30.00, item.lineCost, 0.001)
	}

	@Test
	fun `multi level sub recipe calculation`() {
		// Level 1 raw ingredient: Milk ($1.00 / l)
		val masterMilk = MasterIngredientEntity(id = 1, name = "Milk", unitCost = 1.0, baseUnit = "l")

		// Level 2 intermediate: Condensed Milk (from recipe 10, yieldRatio 0.5 -> $2.00 / l)
		val masterCondensedMilk = MasterIngredientEntity(
			id = 2,
			name = "Condensed Milk",
			unitCost = 0.0,
			baseUnit = "l",
			linkedRecipeId = 10L,
			yieldRatio = 0.5
		)

		// Level 3 intermediate: Khoya (from recipe 20, uses Condensed Milk, yieldRatio 0.5 -> $4.00 / kg)
		val masterKhoya = MasterIngredientEntity(
			id = 3,
			name = "Khoya",
			unitCost = 0.0,
			baseUnit = "kg",
			linkedRecipeId = 20L,
			yieldRatio = 0.5
		)

		val subRecipes = mapOf(
			// Recipe 10: 1L Milk = $1.00. YieldRatio 0.5 -> $2.00/L
			10L to SubRecipeData(
				recipeId = 10L,
				title = "Condensed Milk",
				ingredients = listOf(Ingredient(amount = 1.0, unit = "l", item = "Milk")),
				yieldValue = 1.0
			),
			// Recipe 20: 1L Condensed Milk = $2.00. YieldRatio 0.5 -> $4.00/kg
			20L to SubRecipeData(
				recipeId = 20L,
				title = "Khoya from Condensed Milk",
				ingredients = listOf(Ingredient(amount = 1.0, unit = "l", item = "Condensed Milk")),
				yieldValue = 1.0
			)
		)

		val resolver = SubRecipeResolver { id -> subRecipes[id] }

		// Parent Recipe 30 uses 500g Khoya -> 0.5kg * $4.00 = $2.00
		val finalIngredients = listOf(
			Ingredient(amount = 500.0, unit = "g", item = "Khoya")
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = finalIngredients,
			masterIngredients = listOf(masterMilk, masterCondensedMilk, masterKhoya),
			subRecipeResolver = resolver,
			currentRecipeId = 30L
		)

		assertEquals(2.00, breakdown.totalCost, 0.001)
		val item = breakdown.items[0]
		assertEquals(CostStatus.MATCHED, item.status)
		assertEquals(4.00, item.derivedUnitCost!!, 0.001)
	}

	@Test
	fun `direct self reference cycle detection halts recursion without stack overflow`() {
		// Recipe 100 links to master ingredient "Recursive Item" which links back to recipe 100
		val masterSelf = MasterIngredientEntity(
			id = 1,
			name = "Self Item",
			unitCost = 5.0,
			baseUnit = "kg",
			linkedRecipeId = 100L
		)

		val subRecipes = mapOf(
			100L to SubRecipeData(
				recipeId = 100L,
				title = "Self Recipe",
				ingredients = listOf(Ingredient(amount = 1.0, unit = "kg", item = "Self Item"))
			)
		)

		val resolver = SubRecipeResolver { id -> subRecipes[id] }

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(Ingredient(amount = 1.0, unit = "kg", item = "Self Item")),
			masterIngredients = listOf(masterSelf),
			subRecipeResolver = resolver,
			currentRecipeId = 100L // parent is recipe 100
		)

		assertTrue(breakdown.hasRecursionCycle)
		assertEquals(1, breakdown.unpricedItemCount)
		assertEquals(CostStatus.RECURSION_CYCLE_DETECTED, breakdown.items[0].status)
		assertEquals(0.0, breakdown.items[0].lineCost, 0.001)
	}

	@Test
	fun `mutual circular reference cycle detection halts recursion without stack overflow`() {
		// Recipe A (id 1) links to Item B (linked to Recipe B, id 2)
		// Recipe B (id 2) links to Item A (linked to Recipe A, id 1)
		val masterA = MasterIngredientEntity(id = 1, name = "Item A", unitCost = 0.0, baseUnit = "kg", linkedRecipeId = 1L)
		val masterB = MasterIngredientEntity(id = 2, name = "Item B", unitCost = 0.0, baseUnit = "kg", linkedRecipeId = 2L)

		val subRecipes = mapOf(
			1L to SubRecipeData(
				recipeId = 1L,
				title = "Recipe A",
				ingredients = listOf(Ingredient(amount = 1.0, unit = "kg", item = "Item B"))
			),
			2L to SubRecipeData(
				recipeId = 2L,
				title = "Recipe B",
				ingredients = listOf(Ingredient(amount = 1.0, unit = "kg", item = "Item A"))
			)
		)

		val resolver = SubRecipeResolver { id -> subRecipes[id] }

		// Calculate for Recipe A
		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(Ingredient(amount = 1.0, unit = "kg", item = "Item B")),
			masterIngredients = listOf(masterA, masterB),
			subRecipeResolver = resolver,
			currentRecipeId = 1L
		)

		// Recursion must halt safely without error
		assertNotNull(breakdown)
		// Since Recipe B recursed and encountered Recipe A in visited set, Item A in Recipe B hit recursion cycle
		assertEquals(0.0, breakdown.totalCost, 0.001)
	}

	@Test
	fun `missing or null sub recipe falls back to manual unit cost if positive`() {
		// Khoya has linkedRecipeId 999 which does NOT exist in resolver, but has unitCost = 25.0
		val masterKhoya = MasterIngredientEntity(
			id = 2,
			name = "Khoya",
			unitCost = 25.0,
			baseUnit = "kg",
			linkedRecipeId = 999L,
			yieldRatio = 0.20
		)

		val resolver = SubRecipeResolver { null }

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(Ingredient(amount = 2.0, unit = "kg", item = "Khoya")),
			masterIngredients = listOf(masterKhoya),
			subRecipeResolver = resolver,
			currentRecipeId = 401L
		)

		assertEquals(50.00, breakdown.totalCost, 0.001)
		val item = breakdown.items[0]
		assertEquals(CostStatus.MATCHED, item.status)
		assertFalse(item.isDerivedFromSubRecipe)
		assertEquals(50.00, item.lineCost, 0.001)
	}
}
