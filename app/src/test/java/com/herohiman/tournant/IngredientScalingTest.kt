package com.herohiman.tournant

import com.herohiman.tournant.data.Ingredient
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.DecimalFormatSymbols

class IngredientScalingTest {

    @Test
    fun `test scaling with decimal amounts preserves precision`() {
        val ingredient = Ingredient(amount = 1.5, unit = "cups")
        val scaled = ingredient.withScaledAmount(2.0)
        
        assertEquals(3.0, scaled.amount)
        assertEquals("cups", scaled.unit)
    }

    @Test
    fun `test scaling with fractional amounts handles fractions correctly`() {
        val ingredient = Ingredient(amount = 0.25, unit = "kg")
        val scaled = ingredient.withScaledAmount(4.0)
        
        assertEquals(1.0, scaled.amount)
        assertEquals("kg", scaled.unit)
    }

    @Test
    fun `test scaling preserves ingredient ranges`() {
        val ingredient = Ingredient(amount = 2.0, amountRange = 3.0, unit = "ml")
        val scaled = ingredient.withScaledAmount(2.0)
        
        assertEquals(4.0, scaled.amount)
        assertEquals(6.0, scaled.amountRange)
        assertEquals("ml", scaled.unit)
    }

    @Test
    fun `test scaling null amount preserves null`() {
        val ingredient = Ingredient(amount = null, unit = "g")
        val scaled = ingredient.withScaledAmount(2.0)
        
        assertEquals(null, scaled.amount)
        assertEquals("g", scaled.unit)
    }

    @Test
    fun `test scaling with zero factor returns original ingredient`() {
        val ingredient = Ingredient(amount = 5.0, unit = "oz")
        val scaled = ingredient.withScaledAmount(0.0)
        
        assertEquals(5.0, scaled.amount)
        assertEquals("oz", scaled.unit)
    }

    @Test
    fun `test scaling with one factor returns original ingredient`() {
        val ingredient = Ingredient(amount = 5.0, unit = "oz")
        val scaled = ingredient.withScaledAmount(1.0)
        
        assertEquals(5.0, scaled.amount)
        assertEquals("oz", scaled.unit)
    }

    @Test
    fun `test amountToStringForCooks with scaled range displays correctly`() {
        val ingredient = Ingredient(amount = 2.0, amountRange = 3.0, unit = "tsp")
        val scaled = ingredient.withScaledAmount(1.5)
        val formatted = scaled.amountToStringForCooks()
        
        assertEquals("3–4½ tsp ", formatted)
    }

    @Test
    fun `test scaling preserves optional flag`() {
        val ingredient = Ingredient(amount = 1.0, item = "Salt", optional = true)
        val scaled = ingredient.withScaledAmount(2.0)
        
        assertEquals(true, scaled.optional)
        assertEquals("Salt", scaled.item)
        assertEquals(2.0, scaled.amount)
    }

    @Test
    fun `test scaling preserves refId`() {
        val ingredient = Ingredient(amount = 1.0, refId = 5L)
        val scaled = ingredient.withScaledAmount(3.0)
        
        assertEquals(5L, scaled.refId)
        assertEquals(3.0, scaled.amount)
    }

    @Test
    fun `test scaling preserves group`() {
        val ingredient = Ingredient(amount = 1.0, group = "Dry Ingredients")
        val scaled = ingredient.withScaledAmount(2.0)
        
        assertEquals("Dry Ingredients", scaled.group)
        assertEquals(2.0, scaled.amount)
    }

    @Test
    fun `test scaling with many decimal places handles precision`() {
        val ingredient = Ingredient(amount = 0.33333, unit = "cups")
        val scaled = ingredient.withScaledAmount(3.0)
        
        assertEquals(1.0, scaled.amount)
        assertEquals("cups", scaled.unit)
    }

    @Test
    fun `test scaling respects units without modification`() {
        val ingredient = Ingredient(amount = 100.0, unit = "g")
        val scaled = ingredient.withScaledAmount(0.5)
        
        assertEquals(50.0, scaled.amount)
        assertEquals("g", scaled.unit)
    }

    @Test
    fun `test scaling with null unit preserves null unit`() {
        val ingredient = Ingredient(amount = 100.0, unit = null)
        val scaled = ingredient.withScaledAmount(2.0)
        
        assertEquals(200.0, scaled.amount)
        assertEquals(null, scaled.unit)
    }

    @Test
    fun `test scaling amountRange with null amount`() {
        val ingredient = Ingredient(amount = null, amountRange = 5.0, unit = "ml")
        val scaled = ingredient.withScaledAmount(2.0)
        
        assertEquals(null, scaled.amount)
        assertEquals(10.0, scaled.amountRange)
        assertEquals("ml", scaled.unit)
    }

    @Test
    fun `test scaling optional ingredient with refId`() {
        val ingredient = Ingredient(amount = 1.0, item = "Chicken", optional = true, refId = 42L)
        val scaled = ingredient.withScaledAmount(2.0)
        
        assertEquals(2.0, scaled.amount)
        assertEquals("Chicken", scaled.item)
        assertEquals(true, scaled.optional)
        assertEquals(42L, scaled.refId)
    }

    @Test
    fun `test scaling preserves item when amount is null`() {
        val ingredient = Ingredient(amount = null, item = "Water")
        val scaled = ingredient.withScaledAmount(5.0)
        
        assertEquals(null, scaled.amount)
        assertEquals("Water", scaled.item)
    }

    @Test
    fun `test scaling with range only no amount`() {
        val ingredient = Ingredient(amount = null, amountRange = 3.0, unit = "L")
        val scaled = ingredient.withScaledAmount(2.0)
        
        assertEquals(null, scaled.amount)
        assertEquals(6.0, scaled.amountRange)
        assertEquals("L", scaled.unit)
    }

    @Test
    fun `test scaling preserves all properties when scale is 1.0`() {
        val ingredient = Ingredient(
            amount = 2.5,
            amountRange = 3.5,
            unit = "g",
            item = "Butter",
            refId = 1L,
            group = "Fats",
            optional = false
        )
        val scaled = ingredient.withScaledAmount(1.0)
        
        assertEquals(2.5, scaled.amount)
        assertEquals(3.5, scaled.amountRange)
        assertEquals("g", scaled.unit)
        assertEquals("Butter", scaled.item)
        assertEquals(1L, scaled.refId)
        assertEquals("Fats", scaled.group)
        assertEquals(false, scaled.optional)
    }

    @Test
    fun `test scaling with fractions and mixed numbers`() {
        val ingredient = Ingredient(amount = 1.5, unit = "cups")
        val scaled = ingredient.withScaledAmount(1.33333)
        
        assertEquals(2.0, scaled.amount)
        assertEquals("cups", scaled.unit)
    }

    @Test
    fun `test scaling with small decimal values preserves precision`() {
        val ingredient = Ingredient(amount = 0.1, unit = "tsp")
        val scaled = ingredient.withScaledAmount(3.0)
        
        assertEquals(0.3, scaled.amount)
        assertEquals("tsp", scaled.unit)
    }

    @Test
    fun `test scaling amountRange without amount works correctly`() {
        val ingredient = Ingredient(amount = null, amountRange = 4.0, unit = "L")
        val scaled = ingredient.withScaledAmount(2.5)
        
        assertEquals(null, scaled.amount)
        assertEquals(10.0, scaled.amountRange)
        assertEquals("L", scaled.unit)
    }

    @Test
    fun `test scaling with zero amount handles gracefully`() {
        val ingredient = Ingredient(amount = 0.0, unit = "g")
        val scaled = ingredient.withScaledAmount(2.0)
        
        assertEquals(0.0, scaled.amount)
        assertEquals("g", scaled.unit)
    }
}