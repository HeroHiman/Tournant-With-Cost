package com.herohiman.tournant

import com.herohiman.tournant.cost.CostCurrencyFormatter
import com.herohiman.tournant.cost.YieldParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class YieldParserTest {

	@Test
	fun `extracts 10 kg from Hindi parentheses yield string and computes cost per kg`() {
		val yieldUnit = "1 टीपा काजू (10 किलो)"
		val parsed = YieldParser.parseWeightInKg(yieldValue = 1.0, yieldUnit = yieldUnit)

		assertNotNull(parsed)
		assertEquals(10.0, parsed!!.amountInKg, 0.001)
		assertEquals("किलो", parsed.rawUnit)

		val totalCost = 7495.00
		val costPerKg = YieldParser.calculateCostPerKg(
			totalCost = totalCost,
			yieldValue = 1.0,
			yieldUnit = yieldUnit
		)

		assertNotNull(costPerKg)
		assertEquals(749.50, costPerKg!!, 0.001)
		assertEquals("₹749.50", CostCurrencyFormatter.formatAmount(costPerKg))
	}

	@Test
	fun `extracts weight with Devanagari numerals in parentheses`() {
		val yieldUnit = "1 टीपा (१० किलो)"
		val parsed = YieldParser.parseWeightInKg(yieldValue = 1.0, yieldUnit = yieldUnit)

		assertNotNull(parsed)
		assertEquals(10.0, parsed!!.amountInKg, 0.001)
	}

	@Test
	fun `extracts grams and converts to kilograms accurately`() {
		val yieldUnit = "500 ग्राम"
		val parsed = YieldParser.parseWeightInKg(yieldValue = null, yieldUnit = yieldUnit)

		assertNotNull(parsed)
		assertEquals(0.5, parsed!!.amountInKg, 0.001)

		val costPerKg = YieldParser.calculateCostPerKg(
			totalCost = 150.0,
			yieldValue = null,
			yieldUnit = yieldUnit
		)

		assertNotNull(costPerKg)
		assertEquals(300.0, costPerKg!!, 0.001)
	}

	@Test
	fun `extracts from yieldValue and yieldUnit pair`() {
		val parsed = YieldParser.parseWeightInKg(yieldValue = 2.5, yieldUnit = "kg")

		assertNotNull(parsed)
		assertEquals(2.5, parsed!!.amountInKg, 0.001)
	}

	@Test
	fun `scaling yields scales amount in kg proportionately`() {
		val yieldUnit = "1 टीपा काजू (10 किलो)"
		val parsedScaled = YieldParser.parseWeightInKg(
			yieldValue = 1.0,
			yieldUnit = yieldUnit,
			scaleFactor = 2.0
		)

		assertNotNull(parsedScaled)
		assertEquals(20.0, parsedScaled!!.amountInKg, 0.001)

		// Scaled total cost: 7495 * 2 = 14990
		val costPerKg = YieldParser.calculateCostPerKg(
			totalCost = 14990.0,
			yieldValue = 1.0,
			yieldUnit = yieldUnit,
			scaleFactor = 2.0
		)

		assertNotNull(costPerKg)
		assertEquals(749.50, costPerKg!!, 0.001)
	}

	@Test
	fun `non weight units return null without errors`() {
		val parsed = YieldParser.parseWeightInKg(yieldValue = 12.0, yieldUnit = "Muffins")
		assertNull(parsed)

		val costPerKg = YieldParser.calculateCostPerKg(
			totalCost = 240.0,
			yieldValue = 12.0,
			yieldUnit = "Muffins"
		)
		assertNull(costPerKg)
	}
}
