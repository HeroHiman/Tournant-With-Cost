package com.herohiman.tournant

import com.herohiman.tournant.Constants.Companion.PREF_COLOR_THEME
import com.herohiman.tournant.Constants.Companion.PREF_DECIMAL_SEPARATOR_COMMA
import com.herohiman.tournant.Constants.Companion.PREF_MARKDOWN
import com.herohiman.tournant.Constants.Companion.PREF_SCREEN_ON
import com.herohiman.tournant.Constants.Companion.PREF_UNIT_SYSTEM
import com.herohiman.tournant.Constants.Companion.UNIT_SYSTEM_DEFAULT
import com.herohiman.tournant.Constants.Companion.UNIT_SYSTEM_IMPERIAL
import com.herohiman.tournant.Constants.Companion.UNIT_SYSTEM_METRIC
import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.utils.RecipeUnitConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeSettingsAndUnitConversionTest {

	@Test
	fun `convert imperial weight to metric grams and kilograms`() {
		val (gAmount, gUnit) = RecipeUnitConverter.convertToMetric(4.0, "oz")
		assertEquals("g", gUnit)
		assertEquals(113.4, gAmount, 0.1)

		val (kgAmount, kgUnit) = RecipeUnitConverter.convertToMetric(5.0, "lb")
		assertEquals("kg", kgUnit)
		assertEquals(2.27, kgAmount, 0.05)
	}

	@Test
	fun `convert metric weight to imperial ounces and pounds`() {
		val (ozAmount, ozUnit) = RecipeUnitConverter.convertToImperial(100.0, "g")
		assertEquals("oz", ozUnit)
		assertEquals(3.53, ozAmount, 0.05)

		val (lbAmount, lbUnit) = RecipeUnitConverter.convertToImperial(1000.0, "g")
		assertEquals("lb", lbUnit)
		assertEquals(2.2, lbAmount, 0.05)

		val (kgToLbAmount, kgToLbUnit) = RecipeUnitConverter.convertToImperial(2.5, "kg")
		assertEquals("lb", kgToLbUnit)
		assertEquals(5.51, kgToLbAmount, 0.05)
	}

	@Test
	fun `convert imperial volume to metric milliliters and liters`() {
		val (tspAmount, tspUnit) = RecipeUnitConverter.convertToMetric(2.0, "tsp")
		assertEquals("ml", tspUnit)
		assertEquals(9.86, tspAmount, 0.1)

		val (tbspAmount, tbspUnit) = RecipeUnitConverter.convertToMetric(1.0, "tbsp")
		assertEquals("ml", tbspUnit)
		assertEquals(14.79, tbspAmount, 0.1)

		val (cupAmount, cupUnit) = RecipeUnitConverter.convertToMetric(2.0, "cups")
		assertEquals("ml", cupUnit)
		assertEquals(473.18, cupAmount, 0.1)

		val (largeCupAmount, largeCupUnit) = RecipeUnitConverter.convertToMetric(5.0, "cups")
		assertEquals("l", largeCupUnit)
		assertEquals(1.18, largeCupAmount, 0.05)
	}

	@Test
	fun `convert metric volume to imperial teaspoons tablespoons and cups`() {
		val (tspAmount, tspUnit) = RecipeUnitConverter.convertToImperial(10.0, "ml")
		assertEquals("tsp", tspUnit)
		assertEquals(2.03, tspAmount, 0.1)

		val (cupAmount, cupUnit) = RecipeUnitConverter.convertToImperial(500.0, "ml")
		assertEquals("cup", cupUnit)
		assertEquals(2.11, cupAmount, 0.05)

		val (lAmount, lUnit) = RecipeUnitConverter.convertToImperial(1.5, "l")
		assertEquals("cup", lUnit)
		assertEquals(6.34, lAmount, 0.1)
	}

	@Test
	fun `convert full ingredient object between unit systems preserving range and item`() {
		val ingredient = Ingredient(
			amount = 2.0,
			amountRange = 3.0,
			unit = "cups",
			item = "All-Purpose Flour",
			group = "Dry"
		)

		val converted = RecipeUnitConverter.convertIngredient(ingredient, UNIT_SYSTEM_METRIC)
		assertEquals("All-Purpose Flour", converted.item)
		assertEquals("Dry", converted.group)
		assertEquals("ml", converted.unit)
		assertEquals(473.18, converted.amount!!, 0.1)
		assertEquals(709.76, converted.amountRange!!, 0.1)
	}

	@Test
	fun `unrecognized or non-standard units remain unchanged during conversion`() {
		val pinch = Ingredient(amount = 2.0, unit = "pinch", item = "Nutmeg")
		val convertedPinch = RecipeUnitConverter.convertIngredient(pinch, UNIT_SYSTEM_METRIC)
		assertEquals("pinch", convertedPinch.unit)
		assertEquals(2.0, convertedPinch.amount!!, 0.001)

		val piece = Ingredient(amount = 3.0, unit = "cloves", item = "Garlic")
		val convertedPiece = RecipeUnitConverter.convertIngredient(piece, UNIT_SYSTEM_IMPERIAL)
		assertEquals("cloves", convertedPiece.unit)
		assertEquals(3.0, convertedPiece.amount!!, 0.001)
	}

	@Test
	fun `settings preference keys and unit system constants validate correctly`() {
		assertEquals("MARKDOWN", PREF_MARKDOWN)
		assertEquals("SCREEN_ON", PREF_SCREEN_ON)
		assertEquals("COLOR_THEME", PREF_COLOR_THEME)
		assertEquals("DECIMAL_SEPARATOR_COMMA", PREF_DECIMAL_SEPARATOR_COMMA)
		assertEquals("UNIT_SYSTEM", PREF_UNIT_SYSTEM)

		assertEquals(0, UNIT_SYSTEM_DEFAULT)
		assertEquals(1, UNIT_SYSTEM_METRIC)
		assertEquals(2, UNIT_SYSTEM_IMPERIAL)

		assertTrue(RecipeUnitConverter.isMetric("g"))
		assertTrue(RecipeUnitConverter.isMetric("ml"))
		assertTrue(RecipeUnitConverter.isImperial("oz"))
		assertTrue(RecipeUnitConverter.isImperial("cups"))
		assertFalse(RecipeUnitConverter.isMetric("pinch"))
		assertFalse(RecipeUnitConverter.isImperial("pinch"))
	}

}
