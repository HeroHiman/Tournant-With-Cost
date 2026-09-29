package com.herohiman.tournant

import com.herohiman.tournant.cost.CostCurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class CostCurrencyFormatterTest {

	@Test
	fun `default currency symbol is rupee and currency code is INR`() {
		assertEquals("₹", CostCurrencyFormatter.DEFAULT_CURRENCY_SYMBOL)
		assertEquals("INR", CostCurrencyFormatter.DEFAULT_CURRENCY_CODE)
	}

	@Test
	fun `formatAmount formats with default rupee symbol and two decimals`() {
		val result = CostCurrencyFormatter.formatAmount(7450.0)
		assertEquals("₹7450.00", result)
	}

	@Test
	fun `formatAmount respects custom symbol and decimals`() {
		val result = CostCurrencyFormatter.formatAmount(12.3456, symbol = "$", decimals = 3)
		assertEquals("$12.346", result)
	}

	@Test
	fun `formatAmount masks value when privacy mode is active`() {
		val result = CostCurrencyFormatter.formatAmount(7450.0, isPrivacyMode = true)
		assertEquals("••••", result)
	}

	@Test
	fun `formatUnitCost formats regular and derived ingredients in rupee`() {
		val direct = CostCurrencyFormatter.formatUnitCost(unitCost = 700.0, baseUnit = "kg", decimals = 2)
		assertEquals("₹700.00 / kg", direct)

		val derived = CostCurrencyFormatter.formatUnitCost(unitCost = 110.0, baseUnit = "kg", isDerived = true, decimals = 2)
		assertEquals("₹110.00 / kg (Derived)", derived)

		val derivedZero = CostCurrencyFormatter.formatUnitCost(unitCost = 0.0, baseUnit = "kg", isDerived = true)
		assertEquals("kg (Derived)", derivedZero)
	}

	@Test
	fun `formatUnitCost masks unit cost in privacy mode`() {
		val masked = CostCurrencyFormatter.formatUnitCost(unitCost = 700.0, baseUnit = "kg", isPrivacyMode = true)
		assertEquals("•••• / kg", masked)
	}
}
