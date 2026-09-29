package com.herohiman.tournant

import com.herohiman.tournant.cost.CostStatus
import com.herohiman.tournant.cost.IngredientCostItem
import com.herohiman.tournant.cost.LiveCostCalculator
import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.room.MasterIngredientEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LineItemCostResolutionTest {

	@Test
	fun `line item cost resolution resolves individual ingredient costs in rupee`() {
		val kaju = Ingredient(amount = 10.0, unit = "kg", item = "काजू")
		val sugar = Ingredient(amount = 5.0, unit = "kg", item = "Sugar")

		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "काजू", unitCost = 700.0, baseUnit = "kg", currency = "INR"),
			MasterIngredientEntity(id = 2, name = "Sugar", unitCost = 40.0, baseUnit = "kg", currency = "INR")
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju, sugar),
			masterIngredients = masterIngredients
		)

		assertEquals(7200.0, breakdown.totalCost, 0.001)
		assertEquals("₹7200.00", breakdown.formattedTotalCost())

		val kajuCostItem = breakdown.findCostItem(kaju)
		assertNotNull(kajuCostItem)
		assertEquals(7000.0, kajuCostItem!!.lineCost, 0.001)
		assertEquals(CostStatus.MATCHED, kajuCostItem.status)
		assertTrue(kajuCostItem.hasResolvedCost)
		assertEquals("₹7000.00", kajuCostItem.formattedCost())

		val sugarCostItem = breakdown.findCostItem(sugar)
		assertNotNull(sugarCostItem)
		assertEquals(200.0, sugarCostItem!!.lineCost, 0.001)
		assertEquals("₹200.00", sugarCostItem.formattedCost())

		assertEquals("₹7000.00", breakdown.formattedCostForIngredient(kaju))
		assertEquals("₹200.00", breakdown.formattedCostForIngredient(sugar))
	}

	@Test
	fun `substitute ingredients resolve active lineCost and inactive potentialCost without double charging`() {
		val gud = Ingredient(amount = 500.0, unit = "g", item = "Gud", substituteGroupId = "sweet_1", isActiveSubstitute = true)
		val sugarSyrup = Ingredient(amount = 750.0, unit = "g", item = "Sugar Syrup", substituteGroupId = "sweet_1", isActiveSubstitute = false)
		val honey = Ingredient(amount = 400.0, unit = "g", item = "Honey", substituteGroupId = "sweet_1", isActiveSubstitute = false)

		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "Gud", unitCost = 100.0, baseUnit = "kg", currency = "INR"), // 500g = ₹50
			MasterIngredientEntity(id = 2, name = "Sugar Syrup", unitCost = 40.0, baseUnit = "kg", currency = "INR"), // 750g = ₹30
			MasterIngredientEntity(id = 3, name = "Honey", unitCost = 300.0, baseUnit = "kg", currency = "INR") // 400g = ₹120
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(gud, sugarSyrup, honey),
			masterIngredients = masterIngredients
		)

		// Only Gud (₹50) is active, so total cost is ₹50
		assertEquals(50.0, breakdown.totalCost, 0.001)
		assertEquals("₹50.00", breakdown.formattedTotalCost())

		val gudItem = breakdown.findCostItem(gud)
		assertNotNull(gudItem)
		assertEquals(50.0, gudItem!!.lineCost, 0.001)
		assertEquals(CostStatus.MATCHED, gudItem.status)
		assertEquals("₹50.00", gudItem.formattedCost())

		val sugarItem = breakdown.findCostItem(sugarSyrup)
		assertNotNull(sugarItem)
		assertEquals(0.0, sugarItem!!.lineCost, 0.001) // Does not charge total
		assertEquals(30.0, sugarItem.potentialCost ?: 0.0, 0.001)
		assertEquals(CostStatus.SUBSTITUTE_INACTIVE, sugarItem.status)
		assertTrue(sugarItem.hasResolvedCost)
		assertEquals("₹30.00", sugarItem.formattedCost())

		val honeyItem = breakdown.findCostItem(honey)
		assertNotNull(honeyItem)
		assertEquals(0.0, honeyItem!!.lineCost, 0.001)
		assertEquals(120.0, honeyItem.potentialCost ?: 0.0, 0.001)
		assertEquals("₹120.00", honeyItem.formattedCost())

		// Verify lookup via breakdown helper
		assertEquals("₹50.00", breakdown.formattedCostForIngredient(gud))
		assertEquals("₹30.00", breakdown.formattedCostForIngredient(sugarSyrup))
		assertEquals("₹120.00", breakdown.formattedCostForIngredient(honey))
	}

	@Test
	fun `privacy mode masks all line item costs`() {
		val kaju = Ingredient(amount = 10.0, unit = "kg", item = "काजू")
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "काजू", unitCost = 700.0, baseUnit = "kg")
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju),
			masterIngredients = masterIngredients,
			isPrivacyMode = true
		)

		assertEquals("••••", breakdown.formattedTotalCost())
		assertEquals("••••", breakdown.formattedCostForIngredient(kaju))
	}

	@Test
	fun `ingredientCostMap returns formatted prices for all priced ingredients`() {
		val kaju = Ingredient(amount = 10.0, unit = "kg", item = "काजू")
		val water = Ingredient(amount = 2.0, unit = "l", item = "Water") // unpriced
		val masterIngredients = listOf(
			MasterIngredientEntity(id = 1, name = "काजू", unitCost = 700.0, baseUnit = "kg")
		)

		val breakdown = LiveCostCalculator.calculateRecipeCost(
			ingredients = listOf(kaju, water),
			masterIngredients = masterIngredients
		)

		val costMap = breakdown.ingredientCostMap()
		assertEquals(1, costMap.size)
		assertEquals("₹7000.00", costMap[kaju])
		assertNull(costMap[water])
	}
}
