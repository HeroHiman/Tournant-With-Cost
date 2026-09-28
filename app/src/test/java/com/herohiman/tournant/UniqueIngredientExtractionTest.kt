package com.herohiman.tournant

import com.herohiman.tournant.data.RecipeDescription
import com.herohiman.tournant.data.RecipeTitleId
import com.herohiman.tournant.data.room.IngredientEntity
import com.herohiman.tournant.data.room.KeywordEntity
import com.herohiman.tournant.data.room.PreparationEntity
import com.herohiman.tournant.data.room.RecipeDao
import com.herohiman.tournant.data.room.RecipeEntity
import com.herohiman.tournant.data.room.RecipePinEntity
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations
import com.herohiman.tournant.data.room.StringAndCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UniqueIngredientExtractionTest {

	@Test
	fun `unique ingredient extraction returns distinct trimmed names from recipes`() = runBlocking {
		val dao = ExtractionFakeRecipeDao(
			listOf(
				IngredientEntity(recipeId = 1, position = 0, amount = 200.0, amountRange = null, unit = "g", item = "Flour", refId = null, group = null, optional = false),
				IngredientEntity(recipeId = 1, position = 1, amount = 100.0, amountRange = null, unit = "g", item = "Sugar", refId = null, group = null, optional = false),
				IngredientEntity(recipeId = 2, position = 0, amount = 300.0, amountRange = null, unit = "g", item = "Flour", refId = null, group = null, optional = false),
				IngredientEntity(recipeId = 2, position = 1, amount = 50.0, amountRange = null, unit = "ml", item = "Milk", refId = null, group = null, optional = false)
			)
		)
		val repository = RecipeRepository(dao)

		val uniqueNames = repository.getUniqueIngredientNames()

		assertEquals(3, uniqueNames.size)
		assertEquals(listOf("Flour", "Milk", "Sugar"), uniqueNames.sorted())
	}

	@Test
	fun `unique ingredient extraction filters out null and blank names and linked recipe references`() = runBlocking {
		val dao = ExtractionFakeRecipeDao(
			listOf(
				IngredientEntity(recipeId = 1, position = 0, amount = 100.0, amountRange = null, unit = "g", item = "Butter", refId = null, group = null, optional = false),
				IngredientEntity(recipeId = 1, position = 1, amount = null, amountRange = null, unit = null, item = null, refId = 42L, group = null, optional = false), // Linked recipe
				IngredientEntity(recipeId = 1, position = 2, amount = null, amountRange = null, unit = null, item = "   ", refId = null, group = null, optional = false), // Blank item
				IngredientEntity(recipeId = 1, position = 3, amount = 10.0, amountRange = null, unit = "g", item = null, refId = null, group = null, optional = false) // Null item
			)
		)
		val repository = RecipeRepository(dao)

		val uniqueNames = repository.getUniqueIngredientNames()

		assertEquals(1, uniqueNames.size)
		assertEquals("Butter", uniqueNames[0])
	}

	@Test
	fun `unique ingredient extraction preserves localized and multilingual names`() = runBlocking {
		// Testing non-ASCII unicode ingredients (e.g. Hindi, German, Japanese)
		val dao = ExtractionFakeRecipeDao(
			listOf(
				IngredientEntity(recipeId = 1, position = 0, amount = 250.0, amountRange = null, unit = "ग्राम", item = "मैदा", refId = null, group = null, optional = false),
				IngredientEntity(recipeId = 1, position = 1, amount = 150.0, amountRange = null, unit = "ग्राम", item = "शक्कर", refId = null, group = null, optional = false),
				IngredientEntity(recipeId = 1, position = 2, amount = 50.0, amountRange = null, unit = "मिलीलीटर", item = "तेल", refId = null, group = null, optional = false),
				IngredientEntity(recipeId = 2, position = 0, amount = 100.0, amountRange = null, unit = "ग्राम", item = "मैदा", refId = null, group = null, optional = false)
			)
		)
		val repository = RecipeRepository(dao)

		val uniqueNames = repository.getUniqueIngredientNames()

		assertEquals(3, uniqueNames.size)
		assertTrue(uniqueNames.contains("मैदा"))
		assertTrue(uniqueNames.contains("शक्कर"))
		assertTrue(uniqueNames.contains("तेल"))
	}

	@Test
	fun `preferred unit extraction finds associated unit for ingredient`() = runBlocking {
		val dao = ExtractionFakeRecipeDao(
			listOf(
				IngredientEntity(recipeId = 1, position = 0, amount = 500.0, amountRange = null, unit = "g", item = "Flour", refId = null, group = null, optional = false),
				IngredientEntity(recipeId = 1, position = 1, amount = 250.0, amountRange = null, unit = "ml", item = "Oil", refId = null, group = null, optional = false)
			)
		)
		val repository = RecipeRepository(dao)

		assertEquals("g", repository.getPreferredUnitForIngredient("Flour"))
		assertEquals("ml", repository.getPreferredUnitForIngredient("Oil"))
		assertNull(repository.getPreferredUnitForIngredient("NonExistent"))
	}

	private class ExtractionFakeRecipeDao(
		private val ingredientsList: List<IngredientEntity> = emptyList()
	) : RecipeDao() {

		override suspend fun getUniqueIngredientNames(): List<String> {
			return ingredientsList
				.mapNotNull { it.item?.trim() }
				.filter { it.isNotEmpty() }
				.distinct()
				.sorted()
		}

		override suspend fun getPreferredUnitForIngredient(item: String): String? {
			return ingredientsList
				.firstOrNull { it.item?.trim().equals(item.trim(), ignoreCase = true) && !it.unit.isNullOrBlank() }
				?.unit
				?.trim()
		}

		override fun getRecipeById(id: Long): Flow<RecipeWithIngredientsAndPreparations> = emptyFlow()
		override fun getRecipesById(ids: Set<Long>): List<RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getReferencedRecipes(recipeIds: Set<Long>): List<RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getRecipeTitlesWithIds(): Flow<List<RecipeTitleId>> = flowOf(emptyList())
		override fun getRecipeTitleById(id: Long): String = ""
		override fun getRecipeByGourmandId(gourmandId: Int): RecipeWithIngredientsAndPreparations? = null
		override fun getRecipeIdByGourmandId(gourmandId: Long): Long? = null
		override fun getDeprecatedRecipes(gourmandIds: List<Int>): List<RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getRecipeDescriptions(query: String, orderedBy: Int, offset: Int, limit: Int, month: Int): List<RecipeDescription> = emptyList()
		override fun getKeywords(id: Long): List<String> = emptyList()
		override fun getRecipeCount(): Flow<Int> = flowOf(0)
		override fun getRecipeIds(query: String): List<Long> = emptyList()
		override fun getDependentRecipeIds(recipeIds: Set<Long>): List<Long> = emptyList()
		override fun getAllCategories(): Flow<List<String>> = flowOf(emptyList())
		override fun getAllCuisines(): Flow<List<String>> = flowOf(emptyList())
		override fun getAllKeywords(): Flow<List<String>> = flowOf(emptyList())
		override fun getCategories(query: String): Flow<List<StringAndCount>> = flowOf(emptyList())
		override fun getCuisines(query: String): Flow<List<StringAndCount>> = flowOf(emptyList())
		override fun getKeywords(query: String): Flow<List<StringAndCount>> = flowOf(emptyList())
		override fun getSources(): Flow<List<String>> = flowOf(emptyList())
		override fun getYieldUnits(): Flow<List<String>> = flowOf(emptyList())
		override fun getIngredientItems(): Flow<List<String>> = flowOf(emptyList())
		override fun getIngredientUnits(): Flow<List<String>> = flowOf(emptyList())

		override suspend fun insertRecipe(recipe: RecipeEntity): Long = 1L
		override suspend fun updateRecipe(recipe: RecipeEntity) {}
		override suspend fun deleteRecipe(recipe: RecipeEntity) {}
		override suspend fun deleteRecipesByIds(recipeIds: Set<Long>) {}
		override suspend fun deleteAllRecipes() {}
		override suspend fun insertIngredient(ingredient: IngredientEntity) {}
		override suspend fun updateIngredient(ingredient: IngredientEntity) {}
		override suspend fun deleteIngredient(ingredient: IngredientEntity) {}
		override suspend fun deleteIngredientsNotInList(recipeId: Long, positions: List<Int>) {}
		override suspend fun insertKeyword(preparation: KeywordEntity): Long = 1L
		override suspend fun deleteKeywordsNotInList(recipeId: Long, positions: List<Int>) {}
		override suspend fun insertPreparationDate(preparation: PreparationEntity): Long = 1L
		override suspend fun updatePreparationDate(preparation: PreparationEntity) {}
		override suspend fun deletePreparationDate(preparation: PreparationEntity) {}
		override suspend fun deletePreparationDatesNotInList(recipeId: Long, dates: List<Long>) {}
		override suspend fun getPreparation(recipeId: Long, date: Long): PreparationEntity? = null
		override suspend fun getPreparations(recipeId: Long): List<PreparationEntity> = emptyList()
		override suspend fun getMostRecentPreparation(recipeId: Long): PreparationEntity? = null
		override suspend fun getPreparationsByDateRange(recipeId: Long, startDate: Long, endDate: Long): List<PreparationEntity> = emptyList()
		override suspend fun pinRecipe(recipePin: RecipePinEntity): Long = 1L
		override suspend fun unpinRecipe(recipeId: Long) {}
	}
}
