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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReverseLookupRecipeDaoTest {

	private class FakeReverseLookupRecipeDao : RecipeDao() {
		var recipesUsingIngredient = mutableListOf<RecipeTitleId>()
		var lastQueriedMasterId: Long? = null

		override suspend fun getRecipesUsingMasterIngredient(masterIngredientId: Long): List<RecipeTitleId> {
			lastQueriedMasterId = masterIngredientId
			return recipesUsingIngredient
		}

		override fun getRecipeById(id: Long) = emptyFlow<RecipeWithIngredientsAndPreparations>()
		override fun getRecipesById(ids: Set<Long>) = emptyList<RecipeWithIngredientsAndPreparations>()
		override fun getReferencedRecipes(recipeIds: Set<Long>) = emptyList<RecipeWithIngredientsAndPreparations>()
		override fun getRecipeTitlesWithIds() = flowOf(emptyList<RecipeTitleId>())
		override fun getRecipeTitleById(id: Long) = ""
		override fun getRecipeByGourmandId(gourmandId: Int) = null
		override fun getRecipeIdByGourmandId(gourmandId: Long) = null
		override fun getDeprecatedRecipes(gourmandIds: List<Int>) = emptyList<RecipeWithIngredientsAndPreparations>()
		override fun getRecipeDescriptions(query: String, orderedBy: Int, offset: Int, limit: Int, month: Int) = emptyList<RecipeDescription>()
		override fun getKeywords(id: Long) = emptyList<String>()
		override fun getRecipeCount() = flowOf(0)
		override fun getRecipeIds(query: String) = emptyList<Long>()
		override fun getDependentRecipeIds(recipeIds: Set<Long>) = emptyList<Long>()
		override fun getAllCategories() = flowOf(emptyList<String>())
		override fun getAllCuisines() = flowOf(emptyList<String>())
		override fun getAllKeywords() = flowOf(emptyList<String>())
		override fun getCategories(query: String) = flowOf(emptyList<StringAndCount>())
		override fun getCuisines(query: String) = flowOf(emptyList<StringAndCount>())
		override fun getKeywords(query: String) = flowOf(emptyList<StringAndCount>())
		override fun getSources() = flowOf(emptyList<String>())
		override fun getYieldUnits() = flowOf(emptyList<String>())
		override fun getIngredientItems() = flowOf(emptyList<String>())
		override fun getIngredientUnits() = flowOf(emptyList<String>())
		override suspend fun getUniqueIngredientNames() = emptyList<String>()
		override suspend fun getPreferredUnitForIngredient(item: String) = null
		override suspend fun insertRecipe(recipe: RecipeEntity) = 1L
		override suspend fun updateRecipe(recipe: RecipeEntity) {}
		override suspend fun deleteRecipe(recipe: RecipeEntity) {}
		override suspend fun deleteRecipesByIds(recipeIds: Set<Long>) {}
		override suspend fun deleteAllRecipes() {}
		override suspend fun insertIngredient(ingredient: IngredientEntity) {}
		override suspend fun updateIngredient(ingredient: IngredientEntity) {}
		override suspend fun deleteIngredient(ingredient: IngredientEntity) {}
		override suspend fun deleteIngredientsNotInList(recipeId: Long, positions: List<Int>) {}
		override suspend fun insertKeyword(preparation: KeywordEntity) = 1L
		override suspend fun deleteKeywordsNotInList(recipeId: Long, positions: List<Int>) {}
		override suspend fun insertPreparationDate(preparation: PreparationEntity) = 1L
		override suspend fun updatePreparationDate(preparation: PreparationEntity) {}
		override suspend fun deletePreparationDate(preparation: PreparationEntity) {}
		override suspend fun deletePreparationDatesNotInList(recipeId: Long, dates: List<Long>) {}
		override suspend fun getPreparation(recipeId: Long, date: Long) = null
		override suspend fun getPreparations(recipeId: Long) = emptyList<PreparationEntity>()
		override suspend fun getMostRecentPreparation(recipeId: Long) = null
		override suspend fun getPreparationsByDateRange(recipeId: Long, startDate: Long, endDate: Long) = emptyList<PreparationEntity>()
		override suspend fun pinRecipe(recipePin: RecipePinEntity) = 1L
		override suspend fun unpinRecipe(recipeId: Long) {}
	}

	@Test
	fun testRepositoryDelegatesToDaoForReverseLookup() = runBlocking {
		val fakeDao = FakeReverseLookupRecipeDao()
		val repository = RecipeRepository(dao = fakeDao)

		fakeDao.recipesUsingIngredient = mutableListOf(
			RecipeTitleId(id = 1L, title = "Gulab Jamun"),
			RecipeTitleId(id = 2L, title = "Rasgulla")
		)

		val results = repository.getRecipesUsingMasterIngredient(42L)

		assertEquals(42L, fakeDao.lastQueriedMasterId)
		assertEquals(2, results.size)
		assertEquals("Gulab Jamun", results[0].title)
		assertEquals(1L, results[0].id)
		assertEquals("Rasgulla", results[1].title)
		assertEquals(2L, results[1].id)
	}

	@Test
	fun testRepositoryReturnsEmptyWhenNoRecipesFound() = runBlocking {
		val fakeDao = FakeReverseLookupRecipeDao()
		val repository = RecipeRepository(dao = fakeDao)

		val results = repository.getRecipesUsingMasterIngredient(999L)

		assertEquals(999L, fakeDao.lastQueriedMasterId)
		assertTrue(results.isEmpty())
	}
}
