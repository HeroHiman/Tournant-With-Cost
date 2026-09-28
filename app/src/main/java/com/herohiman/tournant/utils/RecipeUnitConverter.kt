package com.herohiman.tournant.utils

import com.herohiman.tournant.Constants.Companion.UNIT_SYSTEM_DEFAULT
import com.herohiman.tournant.Constants.Companion.UNIT_SYSTEM_IMPERIAL
import com.herohiman.tournant.Constants.Companion.UNIT_SYSTEM_METRIC
import com.herohiman.tournant.data.Ingredient
import kotlin.math.round

object RecipeUnitConverter {

	private const val GRAMS_PER_OUNCE = 28.3495
	private const val GRAMS_PER_POUND = 453.592
	private const val ML_PER_TSP = 4.92892
	private const val ML_PER_TBSP = 14.7868
	private const val ML_PER_FL_OZ = 29.5735
	private const val ML_PER_CUP = 236.588

	private val METRIC_WEIGHT = setOf("g", "gram", "grams", "kg", "kilogram", "kilograms")
	private val METRIC_VOLUME = setOf("ml", "milliliter", "milliliters", "l", "liter", "liters")

	private val IMPERIAL_WEIGHT = setOf("oz", "ounce", "ounces", "lb", "lbs", "pound", "pounds")
	private val IMPERIAL_VOLUME = setOf("tsp", "teaspoon", "teaspoons", "tbsp", "tablespoon", "tablespoons", "fl oz", "fluid ounce", "cup", "cups")

	fun isMetric(unit: String): Boolean {
		val lower = unit.trim().lowercase()
		return lower in METRIC_WEIGHT || lower in METRIC_VOLUME
	}

	fun isImperial(unit: String): Boolean {
		val lower = unit.trim().lowercase()
		return lower in IMPERIAL_WEIGHT || lower in IMPERIAL_VOLUME
	}

	fun convertToMetric(amount: Double, unit: String): Pair<Double, String> {
		val lower = unit.trim().lowercase()
		return when (lower) {
			"oz", "ounce", "ounces" -> {
				val grams = amount * GRAMS_PER_OUNCE
				Pair(roundToTwoDecimals(grams), "g")
			}
			"lb", "lbs", "pound", "pounds" -> {
				val grams = amount * GRAMS_PER_POUND
				if (grams >= 1000.0) {
					Pair(roundToTwoDecimals(grams / 1000.0), "kg")
				} else {
					Pair(roundToTwoDecimals(grams), "g")
				}
			}
			"tsp", "teaspoon", "teaspoons" -> {
				Pair(roundToTwoDecimals(amount * ML_PER_TSP), "ml")
			}
			"tbsp", "tablespoon", "tablespoons" -> {
				Pair(roundToTwoDecimals(amount * ML_PER_TBSP), "ml")
			}
			"fl oz", "fluid ounce" -> {
				Pair(roundToTwoDecimals(amount * ML_PER_FL_OZ), "ml")
			}
			"cup", "cups" -> {
				val ml = amount * ML_PER_CUP
				if (ml >= 1000.0) {
					Pair(roundToTwoDecimals(ml / 1000.0), "l")
				} else {
					Pair(roundToTwoDecimals(ml), "ml")
				}
			}
			else -> Pair(amount, unit)
		}
	}

	fun convertToImperial(amount: Double, unit: String): Pair<Double, String> {
		val lower = unit.trim().lowercase()
		return when (lower) {
			"g", "gram", "grams" -> {
				if (amount >= 453.592) {
					Pair(roundToTwoDecimals(amount / GRAMS_PER_POUND), "lb")
				} else {
					Pair(roundToTwoDecimals(amount / GRAMS_PER_OUNCE), "oz")
				}
			}
			"kg", "kilogram", "kilograms" -> {
				Pair(roundToTwoDecimals((amount * 1000.0) / GRAMS_PER_POUND), "lb")
			}
			"ml", "milliliter", "milliliters" -> {
				when {
					amount >= 236.588 -> Pair(roundToTwoDecimals(amount / ML_PER_CUP), "cup")
					amount >= 29.5735 -> Pair(roundToTwoDecimals(amount / ML_PER_FL_OZ), "fl oz")
					amount >= 14.7868 -> Pair(roundToTwoDecimals(amount / ML_PER_TBSP), "tbsp")
					else -> Pair(roundToTwoDecimals(amount / ML_PER_TSP), "tsp")
				}
			}
			"l", "liter", "liters" -> {
				val ml = amount * 1000.0
				Pair(roundToTwoDecimals(ml / ML_PER_CUP), "cup")
			}
			else -> Pair(amount, unit)
		}
	}

	fun convertIngredient(ingredient: Ingredient, targetSystem: Int): Ingredient {
		val unit = ingredient.unit ?: return ingredient
		val amount = ingredient.amount ?: return ingredient

		return when (targetSystem) {
			UNIT_SYSTEM_METRIC -> {
				if (isImperial(unit)) {
					val (newAmount, newUnit) = convertToMetric(amount, unit)
					val newRange = ingredient.amountRange?.let { convertToMetric(it, unit).first }
					ingredient.copy(amount = newAmount, amountRange = newRange, unit = newUnit)
				} else ingredient
			}
			UNIT_SYSTEM_IMPERIAL -> {
				if (isMetric(unit)) {
					val (newAmount, newUnit) = convertToImperial(amount, unit)
					val newRange = ingredient.amountRange?.let { convertToImperial(it, unit).first }
					ingredient.copy(amount = newAmount, amountRange = newRange, unit = newUnit)
				} else ingredient
			}
			else -> ingredient
		}
	}

	private fun roundToTwoDecimals(value: Double): Double {
		return round(value * 100.0) / 100.0
	}

}
