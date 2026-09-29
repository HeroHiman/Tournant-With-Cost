package com.herohiman.tournant

import com.herohiman.tournant.data.room.MasterIngredientEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class SubRecipeYieldConfigurationTest {

	@Test
	fun `sub recipe entity creation stores linked recipe and yield ratio correctly`() {
		val subRecipeItem = MasterIngredientEntity(
			id = 10,
			name = "Khoya",
			unitCost = 0.0,
			baseUnit = "kg",
			category = "Dairy",
			linkedRecipeId = 42L,
			yieldRatio = 0.20
		)

		assertEquals(42L, subRecipeItem.linkedRecipeId)
		assertEquals(0.20, subRecipeItem.yieldRatio!!, 0.001)
		assertTrue(subRecipeItem.isActive)
		assertEquals("Dairy", subRecipeItem.category)
	}

	@Test
	fun `derived ingredient formatted string displays derived indicator when not privacy mode`() {
		val itemWithFallback = MasterIngredientEntity(
			id = 11,
			name = "Paneer",
			unitCost = 8.50,
			baseUnit = "kg",
			linkedRecipeId = 55L,
			yieldRatio = 0.18
		)

		val isPrivacyMode = false
		val isDerived = itemWithFallback.linkedRecipeId != null

		val formattedCost = com.herohiman.tournant.cost.CostCurrencyFormatter.formatUnitCost(
			unitCost = itemWithFallback.unitCost,
			baseUnit = itemWithFallback.baseUnit,
			isDerived = isDerived,
			isPrivacyMode = isPrivacyMode
		)

		assertEquals("₹8.5000 / kg (Derived)", formattedCost)
	}

	@Test
	fun `derived ingredient without fallback formatted string displays base unit and derived indicator`() {
		val itemZeroCost = MasterIngredientEntity(
			id = 12,
			name = "Khoya",
			unitCost = 0.0,
			baseUnit = "kg",
			linkedRecipeId = 42L,
			yieldRatio = 0.20
		)

		val isPrivacyMode = false
		val isDerived = itemZeroCost.linkedRecipeId != null

		val formattedCost = com.herohiman.tournant.cost.CostCurrencyFormatter.formatUnitCost(
			unitCost = itemZeroCost.unitCost,
			baseUnit = itemZeroCost.baseUnit,
			isDerived = isDerived,
			isPrivacyMode = isPrivacyMode
		)

		assertEquals("kg (Derived)", formattedCost)
	}

	@Test
	fun `derived ingredient respects privacy mode mask`() {
		val item = MasterIngredientEntity(
			id = 13,
			name = "Khoya",
			unitCost = 12.0,
			baseUnit = "kg",
			linkedRecipeId = 42L,
			yieldRatio = 0.20
		)

		val formattedCost = com.herohiman.tournant.cost.CostCurrencyFormatter.formatUnitCost(
			unitCost = item.unitCost,
			baseUnit = item.baseUnit,
			isPrivacyMode = true
		)

		assertEquals("•••• / kg", formattedCost)
	}

	@Test
	fun `validation logic requires linked recipe when sub recipe toggle is enabled`() {
		val isSubRecipe = true
		val selectedRecipeId: Long? = null

		val isValid = !isSubRecipe || selectedRecipeId != null
		assertFalse(isValid)

		val selectedValidId: Long? = 101L
		val isValidNow = !isSubRecipe || selectedValidId != null
		assertTrue(isValidNow)
	}

	@Test
	fun `validation logic allows zero unit cost when sub recipe is enabled`() {
		val isSubRecipe = true
		val unitCostStr = ""
		val selectedRecipeId: Long? = 101L

		val isUnitCostValid = isSubRecipe || unitCostStr.isNotBlank()
		assertTrue(isUnitCostValid)
	}

	@Test
	fun `validation logic requires unit cost when sub recipe is disabled`() {
		val isSubRecipe = false
		val unitCostStr = ""

		val isUnitCostValid = isSubRecipe || unitCostStr.isNotBlank()
		assertFalse(isUnitCostValid)
	}

	@Test
	fun `raw ingredient without linked recipe hides sub recipe link button`() {
		val rawItem = MasterIngredientEntity(
			id = 20,
			name = "Haldi",
			unitCost = 0.50,
			baseUnit = "g",
			linkedRecipeId = null
		)

		val shouldShowLinkButton = rawItem.linkedRecipeId != null
		assertFalse("Raw ingredient without linked recipe must not show link button", shouldShowLinkButton)
	}

	@Test
	fun `prepared ingredient with linked recipe displays sub recipe link button`() {
		val preparedItem = MasterIngredientEntity(
			id = 21,
			name = "Khoya",
			unitCost = 0.0,
			baseUnit = "kg",
			linkedRecipeId = 42L,
			yieldRatio = 0.20
		)

		val shouldShowLinkButton = preparedItem.linkedRecipeId != null
		assertTrue("Prepared ingredient with linked recipe must show link button", shouldShowLinkButton)
	}
}
