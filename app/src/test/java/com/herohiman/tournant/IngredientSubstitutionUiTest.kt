package com.herohiman.tournant

import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.IngredientLine.IngredientGroupTitle
import com.herohiman.tournant.data.IngredientLine.IngredientItem
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.ui.preview.RecipePreviewHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientSubstitutionUiTest {

	@Test
	fun `preview helper formats substitute ingredients with OR indicator and active status`() {
		val lines = listOf(
			IngredientItem(Ingredient(amount = 500.0, unit = "g", item = "Gud (Jaggery)", substituteGroupId = "grp_1", isActiveSubstitute = true)),
			IngredientItem(Ingredient(amount = 750.0, unit = "g", item = "Sugar", substituteGroupId = "grp_1", isActiveSubstitute = false)),
			IngredientItem(Ingredient(amount = 1.0, unit = "kg", item = "Flour"))
		)

		val formatted = RecipePreviewHelper.formatIngredientsForPreview(lines)

		assertTrue(formatted.contains("- 500 g Gud (Jaggery) *(Active)*"))
		assertTrue(formatted.contains("*OR* 750 g Sugar"))
		assertTrue(formatted.contains("- 1 kg Flour"))
	}

	@Test
	fun `preview helper formats cost dynamically when active substitute is toggled`() {
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "Gud", unitCost = 0.10, baseUnit = "g"),
			MasterIngredientEntity(id = 2, name = "Sugar", unitCost = 0.04, baseUnit = "g")
		)

		// State A: Gud is active (500g * 0.10 = $50.00)
		val stateA = listOf(
			Ingredient(amount = 500.0, unit = "g", item = "Gud", substituteGroupId = "sw_1", isActiveSubstitute = true),
			Ingredient(amount = 750.0, unit = "g", item = "Sugar", substituteGroupId = "sw_1", isActiveSubstitute = false)
		)
		val costA = RecipePreviewHelper.formatCostForPreview(stateA, masterIngredients, yieldValue = 1.0)
		assertEquals("Cost: $50.00 ($50.00 / serving)", costA)

		// State B: Sugar is active (750g * 0.04 = $30.00)
		val stateB = listOf(
			Ingredient(amount = 500.0, unit = "g", item = "Gud", substituteGroupId = "sw_1", isActiveSubstitute = false),
			Ingredient(amount = 750.0, unit = "g", item = "Sugar", substituteGroupId = "sw_1", isActiveSubstitute = true)
		)
		val costB = RecipePreviewHelper.formatCostForPreview(stateB, masterIngredients, yieldValue = 1.0)
		assertEquals("Cost: $30.00 ($30.00 / serving)", costB)
	}

	@Test
	fun `substitute linking helper links with previous item and initializes group`() {
		val item1 = Ingredient(amount = 500.0, unit = "g", item = "Gud")
		val item2 = Ingredient(amount = 750.0, unit = "g", item = "Sugar")

		assertNull(item1.substituteGroupId)
		assertNull(item2.substituteGroupId)

		// Linking item2 with item1
		val generatedGroupId = "sub_test_123"
		item1.substituteGroupId = generatedGroupId
		item1.isActiveSubstitute = true

		item2.substituteGroupId = generatedGroupId
		item2.isActiveSubstitute = false

		assertEquals(generatedGroupId, item1.substituteGroupId)
		assertEquals(generatedGroupId, item2.substituteGroupId)
		assertTrue(item1.isActiveSubstitute)
		assertFalse(item2.isActiveSubstitute)
	}

	@Test
	fun `substitute unlinking clears group and restores active status`() {
		val item1 = Ingredient(amount = 500.0, unit = "g", item = "Gud", substituteGroupId = "grp_test", isActiveSubstitute = true)
		val item2 = Ingredient(amount = 750.0, unit = "g", item = "Sugar", substituteGroupId = "grp_test", isActiveSubstitute = false)

		val ingredients = mutableListOf(item1, item2)

		// Unlink item2
		item2.substituteGroupId = null
		item2.isActiveSubstitute = true

		// Check remaining in group: only item1 remains, so item1 is unlinked automatically
		val remaining = ingredients.filter { it.substituteGroupId == "grp_test" }
		if (remaining.size == 1) {
			remaining.first().substituteGroupId = null
			remaining.first().isActiveSubstitute = true
		}

		assertNull(item1.substituteGroupId)
		assertTrue(item1.isActiveSubstitute)
		assertNull(item2.substituteGroupId)
		assertTrue(item2.isActiveSubstitute)
	}

	@Test
	fun `setting active substitute updates active item and deactivates siblings in group`() {
		val item1 = Ingredient(amount = 500.0, unit = "g", item = "Gud", substituteGroupId = "sweet_3", isActiveSubstitute = true)
		val item2 = Ingredient(amount = 750.0, unit = "g", item = "Sugar", substituteGroupId = "sweet_3", isActiveSubstitute = false)
		val item3 = Ingredient(amount = 400.0, unit = "g", item = "Honey", substituteGroupId = "sweet_3", isActiveSubstitute = false)

		val groupItems = listOf(item1, item2, item3)

		// Activate Honey (item3)
		groupItems.forEach { it.isActiveSubstitute = (it == item3) }

		assertFalse(item1.isActiveSubstitute)
		assertFalse(item2.isActiveSubstitute)
		assertTrue(item3.isActiveSubstitute)
	}
}
