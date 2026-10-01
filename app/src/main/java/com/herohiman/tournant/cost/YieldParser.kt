package com.herohiman.tournant.cost

import com.herohiman.tournant.data.room.BaseUnitType

data class ParsedWeightYield(
	val rawAmount: Double,
	val rawUnit: String,
	val amountInKg: Double
)

object YieldParser {

	private val PARENTHESES_PATTERN = Regex("""\(\s*([0-9०-९]+(?:[.,][0-9०-९]+)?)\s*([a-zA-Z\u0900-\u097F.]+)\s*\)""")
	private val DIRECT_PATTERN = Regex("""(?:^|\s)([0-9०-९]+(?:[.,][0-9०-९]+)?)\s*([a-zA-Z\u0900-\u097F.]+)(?:\s|$)""")

	/**
	 * Parses a weight yield in kilograms from the recipe's yield value and yield unit string.
	 *
	 * Supports formats such as:
	 * - Embedded in parentheses: "1 टीपा काजू (10 किलो)", "1 Tray (5 kg)", "1 Batch (१० किलो)"
	 * - Direct text with quantity and unit: "10 kg", "10 किलो", "500 ग्राम"
	 * - Value + Unit pair: yieldValue = 10.0, yieldUnit = "किलो" or "kg"
	 *
	 * Applies [scaleFactor] if provided. Returns null if no mass unit is found.
	 */
	fun parseWeightInKg(
		yieldValue: Double?,
		yieldUnit: String?,
		scaleFactor: Double = 1.0
	): ParsedWeightYield? {
		val unitStr = yieldUnit?.trim() ?: ""

		// 1. Try finding weight inside parentheses e.g. "1 टीपा काजू (10 किलो)"
		PARENTHESES_PATTERN.find(unitStr)?.let { match ->
			val rawNum = normalizeDigits(match.groupValues[1]).replace(',', '.')
			val unit = match.groupValues[2].trim()
			val amount = rawNum.toDoubleOrNull()
			if (amount != null && amount > 0.0) {
				resolveKg(amount, unit, scaleFactor)?.let { return it }
			}
		}

		// 2. Try matching yieldValue with a weight unit in yieldUnit (e.g. yieldValue=10.0, yieldUnit="किलो")
		if (yieldValue != null && yieldValue > 0.0 && unitStr.isNotEmpty()) {
			resolveKg(yieldValue, unitStr, scaleFactor)?.let { return it }
		}

		// 3. Try finding direct quantity and weight unit in yieldUnit string (e.g. "10 kg" or "10 किलो")
		DIRECT_PATTERN.find(unitStr)?.let { match ->
			val rawNum = normalizeDigits(match.groupValues[1]).replace(',', '.')
			val unit = match.groupValues[2].trim()
			val amount = rawNum.toDoubleOrNull()
			if (amount != null && amount > 0.0) {
				resolveKg(amount, unit, scaleFactor)?.let { return it }
			}
		}

		return null
	}

	/**
	 * Computes cost per kilogram given total cost and recipe context.
	 * Checks for explicit mass yield override first, falls back to the Sum of All Active
	 * Ingredient Weights (e.g. 10 kg + 9 kg = 19 kg), and finally falls back to string parsing.
	 */
	fun calculateCostPerKg(
		totalCost: Double,
		yieldValue: Double?,
		yieldUnit: String?,
		scaleFactor: Double = 1.0,
		ingredients: List<com.herohiman.tournant.data.Ingredient> = emptyList(),
		unitAliases: Map<String, com.herohiman.tournant.data.room.UnitAliasEntity> = emptyMap()
	): Double? {
		if (totalCost <= 0.0) return null

		// 1. Explicit yield override: strict numeric mass unit in yieldValue & yieldUnit
		if (yieldValue != null && yieldValue > 0.0 && !yieldUnit.isNullOrBlank()) {
			val alias = UnitConverterEngine.findAlias(yieldUnit.trim(), unitAliases)
			if (alias != null && alias.baseUnit == BaseUnitType.KG && alias.conversionFactor > 0.0) {
				val effectiveScale = if (scaleFactor > 0.0) scaleFactor else 1.0
				val massKg = yieldValue * alias.conversionFactor * effectiveScale
				if (massKg > 0.0) {
					return totalCost / massKg
				}
			}
		}

		// 2. The Automatic Sum Fix: calculate Sum of All Active Ingredient Weights
		if (ingredients.isNotEmpty()) {
			val activeIngredients = LiveCostCalculator.filterActiveIngredients(ingredients)
				.filter { !it.isInformationalOnly && !it.optional }
			val activeMassSum = activeIngredients.mapNotNull { ing ->
				val amount = ing.amount ?: return@mapNotNull null
				if (amount <= 0.0) return@mapNotNull null
				val alias = UnitConverterEngine.findAlias(ing.unit, unitAliases)
				if (alias != null && alias.baseUnit == BaseUnitType.KG && alias.conversionFactor > 0.0) {
					amount * alias.conversionFactor * (if (scaleFactor > 0.0) scaleFactor else 1.0)
				} else {
					null
				}
			}.sum()

			if (activeMassSum > 0.0) {
				return totalCost / activeMassSum
			}
		}

		// 3. Fallback to descriptive yield string parser if ingredients are empty (legacy / test support)
		val parsed = parseWeightInKg(yieldValue, yieldUnit, scaleFactor) ?: return null
		if (parsed.amountInKg <= 0.0) return null
		return totalCost / parsed.amountInKg
	}

	private fun resolveKg(amount: Double, unit: String, scaleFactor: Double): ParsedWeightYield? {
		val alias = UnitConverterEngine.findAlias(unit) ?: return null
		if (alias.baseUnit != BaseUnitType.KG || alias.conversionFactor <= 0.0) return null

		val effectiveScale = if (scaleFactor > 0.0) scaleFactor else 1.0
		val amountInKg = amount * alias.conversionFactor * effectiveScale
		return ParsedWeightYield(
			rawAmount = amount * effectiveScale,
			rawUnit = unit,
			amountInKg = amountInKg
		)
	}

	fun normalizeDigits(input: String): String {
		return input.map { char ->
			when (char) {
				'०' -> '0'; '१' -> '1'; '२' -> '2'; '३' -> '3'; '४' -> '4'
				'५' -> '5'; '६' -> '6'; '७' -> '7'; '८' -> '8'; '९' -> '9'
				else -> char
			}
		}.joinToString("")
	}
}
