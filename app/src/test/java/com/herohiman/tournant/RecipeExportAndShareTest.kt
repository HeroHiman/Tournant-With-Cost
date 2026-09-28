package com.herohiman.tournant

import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.Recipe
import com.herohiman.tournant.data.Season
import com.herohiman.tournant.gourmand.GourmandIssues
import com.herohiman.tournant.ui.elements.RecipeExportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class RecipeExportAndShareTest {

	private lateinit var exportManager: RecipeExportManager

	@Before
	fun setup() {
		exportManager = RecipeExportManager()
	}

	@Test
	fun `check gourmand compatibility detects issues with description and keywords`() {
		val recipe = Recipe(
			title = "Beef Stew",
			description = "Hearty traditional winter stew",
			keywords = linkedSetOf("winter", "hearty", "comfort"),
			language = Locale.ENGLISH
		)

		val issues = exportManager.checkGourmandCompatibility(listOf(recipe), defaultLocale = Locale.ENGLISH)
		assertEquals(2, issues.size)
		assertTrue(issues.contains(GourmandIssues.NO_DESCRIPTIONS))
		assertTrue(issues.contains(GourmandIssues.NO_KEYWORDS))
	}

	@Test
	fun `check gourmand compatibility detects fractional yield values`() {
		val recipe = Recipe(
			title = "Cookies",
			yieldValue = 2.5,
			yieldUnit = "batches",
			language = Locale.ENGLISH
		)

		val issues = exportManager.checkGourmandCompatibility(listOf(recipe), defaultLocale = Locale.ENGLISH)
		assertEquals(1, issues.size)
		assertTrue(issues.contains(GourmandIssues.NO_FRACTIONS_IN_YIELD))
	}

	@Test
	fun `check gourmand compatibility detects season and language mismatch`() {
		val recipe = Recipe(
			title = "Summer Drink",
			season = Season(6, 8),
			language = Locale.FRENCH
		)

		val issues = exportManager.checkGourmandCompatibility(listOf(recipe), defaultLocale = Locale.ENGLISH)
		assertEquals(2, issues.size)
		assertTrue(issues.contains(GourmandIssues.NO_SEASON))
		assertTrue(issues.contains(GourmandIssues.NO_LANGUAGE))
	}

	@Test
	fun `check gourmand compatibility returns empty issues for fully compatible recipes`() {
		val recipe = Recipe(
			title = "Simple Bread",
			yieldValue = 2.0,
			yieldUnit = "loaves",
			language = Locale.ENGLISH
		)

		val issues = exportManager.checkGourmandCompatibility(listOf(recipe), defaultLocale = Locale.ENGLISH)
		assertTrue(issues.isEmpty())
	}

	@Test
	fun `format recipe for text share formats title ingredients and instructions`() {
		val recipe = Recipe(
			title = "Pancakes",
			description = "Fluffy buttermilk pancakes",
			yieldValue = 4.0,
			yieldUnit = "servings",
			instructions = "Mix dry ingredients, add buttermilk, and cook on griddle.",
			notes = "Serve warm with maple syrup."
		).apply {
			ingredients.add(Ingredient(amount = 200.0, unit = "g", item = "Flour", group = "Dry"))
			ingredients.add(Ingredient(amount = 1.0, unit = "pinch", item = "Salt", group = "Dry"))
			ingredients.add(Ingredient(amount = 250.0, unit = "ml", item = "Buttermilk", group = "Wet"))
		}

		val text = exportManager.formatRecipeForTextShare(recipe)
		assertTrue(text.contains("Pancakes"))
		assertTrue(text.contains("Fluffy buttermilk pancakes"))
		assertTrue(text.contains("Yield: 4 servings"))
		assertTrue(text.contains("[Dry]"))
		assertTrue(text.contains("- 200 g Flour"))
		assertTrue(text.contains("[Wet]"))
		assertTrue(text.contains("- 250 ml Buttermilk"))
		assertTrue(text.contains("Mix dry ingredients, add buttermilk, and cook on griddle."))
		assertTrue(text.contains("Serve warm with maple syrup."))
	}

	@Test
	fun `export file name sanitization and mime types`() {
		val singleName = exportManager.getExportFileName(listOf("Mom's Famous Pie!"), "json")
		assertEquals("Mom_s_Famous_Pie_.json", singleName)

		val multiName = exportManager.getExportFileName(listOf("Dish 1", "Dish 2"), "xml")
		assertEquals("recipes_export.xml", multiName)

		val zipName = exportManager.getExportFileName(listOf("Soup"), "zip")
		assertEquals("Soup.zip", zipName)

		assertEquals("application/json", exportManager.getMimeType("json"))
		assertEquals("application/xml", exportManager.getMimeType("xml"))
		assertEquals("application/zip", exportManager.getMimeType("zip"))
		assertEquals("text/plain", exportManager.getMimeType("txt"))
	}

}
