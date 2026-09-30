package com.herohiman.tournant

import com.herohiman.tournant.cost.CostStatus
import com.herohiman.tournant.cost.LiveCostCalculator
import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.room.MasterIngredientEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InformationalCostExclusionTest {

	@Test
	fun `calculate line cost with isInformationalOnly true returns INFORMATIONAL_EXCLUDED and zero cost`() {
		val trayNote = Ingredient(
			amount = 17.0,
			unit = "किलो",
			item = "डब्बा में पैक",
			isInformationalOnly = true,
			noteTitle = "ट्रे Size",
			noteValue = "12x18 inch"
		)
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "डब्बा", unitCost = 50.0, baseUnit = "किलो")
		)

		val result = LiveCostCalculator.calculateLineCost(trayNote, masterIngredients)

		assertEquals(CostStatus.INFORMATIONAL_EXCLUDED, result.status)
		assertEquals(0.0, result.lineCost, 0.001)
		assertFalse(result.hasResolvedCost)
		assertNull(result.formattedCost())
	}

	@Test
	fun `recipe cost calculation excludes informational items from total and unpriced count`() {
		val kaju = Ingredient(
			amount = 10.0,
			unit = "kg",
			item = "काजू",
			isInformationalOnly = false
		)
		val packagingNote = Ingredient(
			amount = 5.0,
			unit = "box",
			item = "17 किलो डब्बा में पैक",
			isInformationalOnly = true,
			noteTitle = "Packing Note",
			noteValue = "5 boxes ready"
		)

		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "काजू", unitCost = 700.0, baseUnit = "kg")
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju, packagingNote),
			masterIngredients = masterIngredients,
			yield = 10.0
		)

		// 10kg * ₹700 = ₹7000 total. Packaging note must NOT add cost and must NOT count as unpriced!
		assertEquals(7000.0, breakdown.totalCost, 0.001)
		assertEquals(0, breakdown.unpricedItemCount)
		assertEquals(2, breakdown.items.size)

		val informationalItem = breakdown.items.find { it.ingredient.isInformationalOnly }
		assertNotNull(informationalItem)
		assertEquals(CostStatus.INFORMATIONAL_EXCLUDED, informationalItem!!.status)
		assertEquals(0.0, informationalItem.lineCost, 0.001)
		assertNull(breakdown.formattedCostForIngredient(packagingNote))
	}
}
