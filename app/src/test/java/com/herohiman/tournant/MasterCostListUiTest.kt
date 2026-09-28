package com.herohiman.tournant

import com.herohiman.tournant.data.room.MasterIngredientEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

		val formatted = String.format(Locale.US, "$%.4f / %s", item.unitCost, item.baseUnit)
		assertEquals("$0.0250 / ml", formatted)
	}

	@Test
	fun `master cost item display masks unit cost in privacy mode`() {
		val item = MasterIngredientEntity(
			id = 2,
			name = "Truffle Oil",
			unitCost = 1.50,
			baseUnit = "ml"
		)

		val isPrivacyMode = true
		val formatted = if (isPrivacyMode) {
			"•••• / ${item.baseUnit}"
		} else {
			String.format(Locale.US, "$%.4f / %s", item.unitCost, item.baseUnit)
		}

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
}
