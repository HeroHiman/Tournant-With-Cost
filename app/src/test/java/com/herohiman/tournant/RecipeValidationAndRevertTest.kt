package com.herohiman.tournant

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.IngredientLine
import com.herohiman.tournant.data.Recipe
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.RecipeRoomDatabase
import com.herohiman.tournant.ui.RecipeEditingViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RecipeValidationAndRevertTest {

	private lateinit var database: RecipeRoomDatabase
	private lateinit var repository: RecipeRepository

	@Before
	fun setup() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		database = Room.inMemoryDatabaseBuilder(context, RecipeRoomDatabase::class.java)
			.allowMainThreadQueries()
			.build()
		repository = RecipeRepository(database.recipeDao())
	}

	@After
	fun tearDown() {
		database.close()
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
	fun existingRecipe_revertAndChangeTracking() = runTest {
		val originalRecipe = Recipe(
			id = 0L,
			title = "Saved Recipe",
			description = "Original Description",
			yieldValue = 2.0
		)
		val insertedId = repository.upsertSingleRecipe(originalRecipe.toRecipeWithIngredientsAndPreparations())

		val viewModel = RecipeEditingViewModel(repository, insertedId)

		// Wait for room emission in viewModel init
		kotlinx.coroutines.delay(100)

		assertFalse(viewModel.hasUnsavedChanges())

		viewModel.recipe.value = viewModel.recipe.value.copy(description = "Edited Description")
		assertTrue(viewModel.hasUnsavedChanges())

		viewModel.revert()
		assertEquals("Original Description", viewModel.recipe.value.description)
		assertFalse(viewModel.hasUnsavedChanges())
	}

}
