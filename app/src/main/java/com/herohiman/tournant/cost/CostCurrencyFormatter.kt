package com.herohiman.tournant.cost

import java.util.Locale

/**
 * Centralized currency and cost formatting utility.
 * Defaults globally to INR (₹).
 */
object CostCurrencyFormatter {
	const val DEFAULT_CURRENCY_SYMBOL = "₹"
	const val DEFAULT_CURRENCY_CODE = "INR"

	fun formatAmount(
		amount: Double,
		symbol: String = DEFAULT_CURRENCY_SYMBOL,
		decimals: Int = 2,
		isPrivacyMode: Boolean = false,
		mask: String = "••••"
	): String {
		if (isPrivacyMode) return mask
		return String.format(Locale.US, "%s%." + decimals + "f", symbol, amount)
	}

	fun formatUnitCost(
		unitCost: Double,
		baseUnit: String,
		isDerived: Boolean = false,
		symbol: String = DEFAULT_CURRENCY_SYMBOL,
		decimals: Int = 4,
		isPrivacyMode: Boolean = false,
		mask: String = "••••"
	): String {
		if (isPrivacyMode) return "$mask / $baseUnit"
		val costPart = String.format(Locale.US, "%s%." + decimals + "f", symbol, unitCost)
		return if (isDerived) {
			if (unitCost > 0.0) "$costPart / $baseUnit (Derived)" else "$baseUnit (Derived)"
		} else {
			"$costPart / $baseUnit"
		}
	}
}
