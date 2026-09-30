package com.herohiman.tournant.data

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import com.herohiman.tournant.getNumberOfDigits
import com.herohiman.tournant.roundToNDigits
import com.herohiman.tournant.toStringForCooks
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class Ingredient(
	var amount: Double? = null,
	var amountRange: Double? = null,
	var unit: String? = null,
	var item: String? = null,
	var refId: Long? = null,
	var group: String? = null,
	var optional: Boolean = false,
	var substituteGroupId: String? = null,
	var isActiveSubstitute: Boolean = true,
	var isInformationalOnly: Boolean = false,
	var noteTitle: String? = null,
	var noteValue: String? = null
) : Parcelable {

	companion object {
		fun createDummy(n: Int? = null) = when (n) {
			3 -> Ingredient(4.5, null, "dt", null, 3, null, false)
			2 -> Ingredient(2.0, 3.0, null, "Bananas", null, null, true)
			else -> Ingredient(100.0, null, "mg", "Amoxicillin", null, null, false)
		}
		fun createExamples(n: Int) = (1..n).map { createDummy(it) }
	}

	fun getMass() = when (unit) {
		"g" -> amount
		"kg" -> amount?.times(1000)
		"oz" -> amount?.times(28.349523125)
		else -> null
	}

	fun isMassKnown() = getMass() != null

	fun removeEmptyValues() {
		if (unit?.isBlank() == true) unit = null
		if (item?.isBlank() == true) item = null
		if (refId != null) item = null
	}

	fun withScaledAmount(factor: Double): Ingredient {
		if (factor <= 0.0 || factor == 1.0) {
			return this
		}

		fun scaleValue(valToScale: Double?): Double? {
			if (valToScale == null) return null
			if (valToScale == 0.0) return 0.0
			val raw = valToScale * factor
			val roundedInt = kotlin.math.round(raw)
			if (kotlin.math.abs(raw - roundedInt) < 0.001) {
				return roundedInt
			}
			val digits = kotlin.math.min(valToScale.getNumberOfDigits() + 1, 4)
			val rounded = raw.roundToNDigits(digits)
			val roundedInt2 = kotlin.math.round(rounded)
			return if (kotlin.math.abs(rounded - roundedInt2) < 0.001) roundedInt2 else rounded
		}

		val amountScaled = scaleValue(amount)
		val amountRangeScaled = scaleValue(amountRange)

		return copy(amount = amountScaled, amountRange = amountRangeScaled)
	}

	fun amountToStringForCooks(appendSpace: Boolean = true) = buildString {
		if (amountRange == null) {
			append(amount.toStringForCooks())
		}
		else {
			append("${amount.toStringForCooks()}–${amountRange.toStringForCooks()}")
		}
		if (unit != null) {
			append(" $unit")
		}
		if (appendSpace && isNotEmpty()) {
			append(" ")
		}
	}

	fun toStringForCooks(optionalWord: String) = buildString {
		append(amountToStringForCooks())
		append(item)
		if (optional) {
			append(" $optionalWord")
		}
	}

}