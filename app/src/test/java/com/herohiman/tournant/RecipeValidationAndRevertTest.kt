package com.herohiman.tournant

import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.IngredientLine
import com.herohiman.tournant.data.Recipe
import com.herohiman.tournant.data.RecipeDescription
import com.herohiman.tournant.data.RecipeTitleId
import com.herohiman.tournant.data.room.RecipeDao
import com.herohiman.tournant.data.room.IngredientEntity
import com.herohiman.tournant.data.room.KeywordEntity
import com.herohiman.tournant.data.room.PreparationEntity
import com.herohiman.tournant.data.room.RecipeEntity
import com.herohiman.tournant.data.room.RecipePinEntity
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations
import com.herohiman.tournant.data.room.StringAndCount
import com.herohiman.tournant.ui.RecipeEditingViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecipeValidationAndRevertTest {

	private lateinit var repository: RecipeRepository

	private class FakeRecipeDao : RecipeDao() {
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

	@Before
	fun setup() {
		repository = RecipeRepository(FakeRecipeDao())
	}

	@Test
	fun processModifications_cleansInstructionsAndNotesWithoutClearingCategory() {
		val recipe = Recipe(
			title = "Pasta",
			category = "Main Course",
			instructions = "   ",
			notes = "   "
		)
		recipe.processModifications()

		assertEquals("Main Course", recipe.category)
		assertEquals(null, recipe.instructions)
		assertEquals(null, recipe.notes)
	}

	@Test
	fun recipeDeepCopy_createsIndependentClone() {
		val original = Recipe(
			id = 1L,
			title = "Original Title",
			keywords = linkedSetOf("Italian", "Quick"),
			ingredients = mutableListOf(
				Ingredient(amount = 2.0, unit = "cups", item = "Flour")
			)
		)

		val copy = original.deepCopy()
		assertNotSame(original, copy)
		assertEquals(original, copy)

		copy.title = "Modified Title"
		copy.keywords.add("Healthy")
		copy.ingredients[0].amount = 5.0

		assertEquals("Original Title", original.title)
		assertEquals(2, original.keywords.size)
		assertEquals(2.0, original.ingredients[0].amount)
	}

	@Test
	fun ingredientLineDeepCopy_createsIndependentClone() {
		val item = IngredientLine.IngredientItem(Ingredient(amount = 1.0, item = "Egg"))
		val itemCopy = item.deepCopy()

		assertNotSame(item, itemCopy)
		assertNotSame(item.ingredient, (itemCopy as IngredientLine.IngredientItem).ingredient)
		assertEquals(item.ingredient, itemCopy.ingredient)

		val group = IngredientLine.IngredientGroupTitle("Sauce")
		val groupCopy = group.deepCopy()
		assertNotSame(group, groupCopy)
		assertEquals(group, groupCopy)
	}

	@Test
	fun validate_catchesBlankTitleAndInvalidYield() {
		val viewModel = RecipeEditingViewModel(repository, 0L)

		// Blank title
		viewModel.recipe.value = Recipe(title = "   ")
		val emptyTitleResult = viewModel.validate()
		assertTrue(emptyTitleResult is RecipeEditingViewModel.ValidationResult.Invalid)
		assertEquals(
			RecipeEditingViewModel.ValidationError.EMPTY_TITLE,
			(emptyTitleResult as RecipeEditingViewModel.ValidationResult.Invalid).errorType
		)

		// Invalid yield values
		viewModel.recipe.value = Recipe(title = "Good Title", yieldValue = 0.0)
		val zeroYieldResult = viewModel.validate()
		assertTrue(zeroYieldResult is RecipeEditingViewModel.ValidationResult.Invalid)
		assertEquals(
			RecipeEditingViewModel.ValidationError.INVALID_YIELD,
			(zeroYieldResult as RecipeEditingViewModel.ValidationResult.Invalid).errorType
		)

		viewModel.recipe.value = Recipe(title = "Good Title", yieldValue = -2.5)
		val negativeYieldResult = viewModel.validate()
		assertTrue(negativeYieldResult is RecipeEditingViewModel.ValidationResult.Invalid)
		assertEquals(
			RecipeEditingViewModel.ValidationError.INVALID_YIELD,
			(negativeYieldResult as RecipeEditingViewModel.ValidationResult.Invalid).errorType
		)

		// Valid recipe
		viewModel.recipe.value = Recipe(title = "Good Title", yieldValue = 4.0)
		val validResult = viewModel.validate()
		assertTrue(validResult is RecipeEditingViewModel.ValidationResult.Valid)
	}

	@Test
	fun newRecipe_revertAndChangeTracking() {
		val viewModel = RecipeEditingViewModel(repository, 0L)

		assertFalse(viewModel.hasUnsavedChanges())

		viewModel.recipe.value = viewModel.recipe.value.copy(title = "A changed title")
		assertTrue(viewModel.hasUnsavedChanges())

		assertTrue(viewModel.canRevert())
		assertTrue(viewModel.revert())

		assertEquals("", viewModel.recipe.value.title)
		assertFalse(viewModel.hasUnsavedChanges())
	}

	@Test
	fun existingRecipe_revertAndChangeTracking() {
		val originalRecipe = Recipe(
			id = 1L,
			title = "Saved Recipe",
			description = "Original Description",
			yieldValue = 2.0
		)
		val viewModel = RecipeEditingViewModel(repository, 0L)
		viewModel.setInitialRecipe(originalRecipe)

		assertFalse(viewModel.hasUnsavedChanges())

		viewModel.recipe.value = viewModel.recipe.value.copy(description = "Edited Description")
		assertTrue(viewModel.hasUnsavedChanges())

		assertTrue(viewModel.canRevert())
		assertTrue(viewModel.revert())
		assertEquals("Original Description", viewModel.recipe.value.description)
		assertFalse(viewModel.hasUnsavedChanges())
	}

	@Test
	fun `instructions and rating change tracking and revert works properly`() {
		val originalRecipe = Recipe(
			id = 2L,
			title = "Recipe with Instructions",
			instructions = "1. Chop onions\n2. Sauté in oil",
			rating = 4.5f
		)
		val viewModel = RecipeEditingViewModel(repository, 0L)
		viewModel.setInitialRecipe(originalRecipe)

		assertFalse(viewModel.hasUnsavedChanges())

		// Modify instructions
		viewModel.recipe.value = viewModel.recipe.value.copy(instructions = "1. Finely dice onions\n2. Fry until golden")
		assertTrue(viewModel.hasUnsavedChanges())
		assertTrue(viewModel.revert())
		assertEquals("1. Chop onions\n2. Sauté in oil", viewModel.recipe.value.instructions)
		assertFalse(viewModel.hasUnsavedChanges())

		// Modify rating
		viewModel.recipe.value = viewModel.recipe.value.copy(rating = 5.0f)
		assertTrue(viewModel.hasUnsavedChanges())
		assertTrue(viewModel.revert())
		assertEquals(4.5f, viewModel.recipe.value.rating)
		assertFalse(viewModel.hasUnsavedChanges())
	}

	@Test
	fun `formatYieldForPreview formats valid yield and handles edge cases safely`() {
		assertEquals("4 portions", com.herohiman.tournant.ui.preview.RecipePreviewHelper.formatYieldForPreview(4.0, "portions"))
		assertEquals("8 portions", com.herohiman.tournant.ui.preview.RecipePreviewHelper.formatYieldForPreview(4.0, "portions", scaleFactor = 2.0))
		assertEquals("2", com.herohiman.tournant.ui.preview.RecipePreviewHelper.formatYieldForPreview(2.0, null))
		assertEquals("", com.herohiman.tournant.ui.preview.RecipePreviewHelper.formatYieldForPreview(null, "portions"))
		assertEquals("", com.herohiman.tournant.ui.preview.RecipePreviewHelper.formatYieldForPreview(0.0, "portions"))
		assertEquals("", com.herohiman.tournant.ui.preview.RecipePreviewHelper.formatYieldForPreview(-1.0, "portions"))
	}

}


