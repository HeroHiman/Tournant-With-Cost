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
}
