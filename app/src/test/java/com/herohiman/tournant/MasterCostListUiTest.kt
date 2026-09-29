package com.herohiman.tournant

import com.herohiman.tournant.data.room.MasterIngredientEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MasterCostListUiTest {

	@Test
	fun `master cost item validation requires non blank name unit and cost`() {
		fun validate(name: String, unitCostStr: String, baseUnit: String): Boolean {
			return name.isNotBlank() && unitCostStr.isNotBlank() && baseUnit.isNotBlank() && (unitCostStr.toDoubleOrNull() != null)
		}

		assertTrue(validate("Flour", "0.002", "g"))
		assertTrue(validate("Eggs", "0.25", "unit"))
		assertFalse(validate("", "0.002", "g"))
		assertFalse(validate("Flour", "", "g"))
		assertFalse(validate("Flour", "abc", "g"))
		assertFalse(validate("Flour", "0.002", ""))
	}

	@Test
	fun `master cost item display formats unit cost correctly when visible`() {
		val item = MasterIngredientEntity(
			id = 1,
			name = "Olive Oil",
			unitCost = 0.025,
			baseUnit = "ml"
		)

		val formatted = com.herohiman.tournant.cost.CostCurrencyFormatter.formatUnitCost(
			unitCost = item.unitCost,
			baseUnit = item.baseUnit
		)
		assertEquals("₹0.0250 / ml", formatted)
	}

	@Test
	fun `master cost item display masks unit cost in privacy mode`() {
		val item = MasterIngredientEntity(
			id = 2,
			name = "Truffle Oil",
			unitCost = 1.50,
			baseUnit = "ml"
		)

		val formatted = com.herohiman.tournant.cost.CostCurrencyFormatter.formatUnitCost(
			unitCost = item.unitCost,
			baseUnit = item.baseUnit,
			isPrivacyMode = true
		)

		assertEquals("•••• / ml", formatted)
	}

	@Test
	fun `master ingredient soft delete toggle flips active state`() {
		val activeItem = MasterIngredientEntity(
			id = 1,
			name = "Butter",
			unitCost = 4.0,
			baseUnit = "lb",
			isActive = true
		)

		val softDeleted = activeItem.copy(isActive = false)
		assertFalse(softDeleted.isActive)

		val restored = softDeleted.copy(isActive = true)
		assertTrue(restored.isActive)
	}

	@Test
	fun `master cost item parsing formats decimal precision cleanly`() {
		fun formatInputCost(cost: Double): String {
			return String.format(Locale.US, "%.4f", cost).trimEnd('0').trimEnd('.')
		}

		assertEquals("4", formatInputCost(4.0))
		assertEquals("0.5", formatInputCost(0.50))
		assertEquals("0.025", formatInputCost(0.0250))
		assertEquals("0.0002", formatInputCost(0.0002))
	}

	@Test
	fun `empty state visibility toggles based on item count`() {
		fun shouldShowEmptyState(items: List<MasterIngredientEntity>): Boolean {
			return items.isEmpty()
		}

		assertTrue(shouldShowEmptyState(emptyList()))
		assertFalse(shouldShowEmptyState(listOf(MasterIngredientEntity(1, "Sugar", 1.0, "kg"))))
	}

	@Test
	fun `auto fetch feedback message formats summary correctly`() {
		fun formatFeedback(count: Int, summaryFormat: String, noNewMessage: String): String {
			return if (count > 0) {
				String.format(summaryFormat, count)
			} else {
				noNewMessage
			}
		}

		val summaryFormat = "Imported %d new ingredients from recipes"
		val noNewMessage = "All recipe ingredients are already in the master list"

		assertEquals("Imported 5 new ingredients from recipes", formatFeedback(5, summaryFormat, noNewMessage))
		assertEquals("Imported 1 new ingredients from recipes", formatFeedback(1, summaryFormat, noNewMessage))
		assertEquals("All recipe ingredients are already in the master list", formatFeedback(0, summaryFormat, noNewMessage))
	}

	@Test
	fun `merge candidates filter excludes current source ingredient and inactive items`() {
		val source = MasterIngredientEntity(id = 1, name = "तेल (5 कटोरी)", unitCost = 0.0, baseUnit = "cup", isActive = true)
		val candidateActive = MasterIngredientEntity(id = 2, name = "तेल", unitCost = 150.0, baseUnit = "liter", isActive = true)
		val candidateInactive = MasterIngredientEntity(id = 3, name = "घी", unitCost = 500.0, baseUnit = "kg", isActive = false)

		val allList = listOf(source, candidateActive, candidateInactive)
		val candidates = allList.filter { it.id != source.id && it.isActive }

		assertEquals(1, candidates.size)
		assertEquals(2L, candidates[0].id)
		assertEquals("तेल", candidates[0].name)
	}

	@Test
	fun `merge candidate map resolves target ingredient case insensitively`() {
		val candidate = MasterIngredientEntity(id = 2, name = "All-Purpose Flour", unitCost = 1.0, baseUnit = "kg", isActive = true)
		val candidates = listOf(candidate)
		val candidateMap = candidates.associateBy { it.name.trim().lowercase() }

		val matchedExact = candidateMap["All-Purpose Flour".lowercase()]
		assertNotNull(matchedExact)
		assertEquals(2L, matchedExact?.id)

		val matchedLower = candidateMap["all-purpose flour"]
		assertNotNull(matchedLower)
		assertEquals(2L, matchedLower?.id)

		val matchedMissing = candidateMap["rice flour"]
		assertNull(matchedMissing)
	}
}
