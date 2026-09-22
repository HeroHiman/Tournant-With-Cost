package com.herohiman.tournant

import com.herohiman.tournant.data.Cookbook
import com.herohiman.tournant.utils.RecipeJsonAdapter
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.io.FileNotFoundException

class JsonAdapterTest {

	@Test
	fun jsonTest() {
		val json = RecipeJsonAdapter.adapter.toJson(Cookbook(sampleRecipeWithIngredients(1)))
		val inputStream = javaClass.classLoader.getResourceAsStream("JsonAdapterTestOutput")
			?: throw FileNotFoundException("Could not find JsonAdapterTestOutput in test resources")
		val expected = inputStream.bufferedReader().use { it.readText() }
		assertEquals(expected, json)
	}

}