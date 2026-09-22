package com.herohiman.tournant

import com.herohiman.tournant.data.Cookbook
import com.herohiman.tournant.utils.RecipeJsonAdapter
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.io.File

class JsonAdapterTest {

	@Test
	fun jsonTest() {
		val json = RecipeJsonAdapter.adapter.toJson(Cookbook(sampleRecipeWithIngredients(1)))
		val expected = File("src/test/java/com.herohiman.tournant/JsonAdapterTestOutput").readText()
		assertEquals(expected, json)
	}

}