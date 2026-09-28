package com.herohiman.tournant

import com.herohiman.tournant.Constants.Companion.SORTED_BY_COOKTIME
import com.herohiman.tournant.Constants.Companion.SORTED_BY_PREPARED
import com.herohiman.tournant.Constants.Companion.SORTED_BY_RATING
import com.herohiman.tournant.Constants.Companion.SORTED_BY_TITLE
import com.herohiman.tournant.data.Recipe
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
import com.herohiman.tournant.ui.elements.RecipeOrganizationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecipeOrganizationAndSortingTest {

	private lateinit var organizationManager: RecipeOrganizationManager
	private lateinit var dao: OrganizationTestRecipeDao
	private lateinit var repository: RecipeRepository

	private class OrganizationTestRecipeDao : RecipeDao() {
		val recipes = mutableMapOf<Long, RecipeEntity>()

		override fun getRecipeById(id: Long): Flow<RecipeWithIngredientsAndPreparations> {
			val entity = recipes[id] ?: return emptyFlow()
			return flowOf(RecipeWithIngredientsAndPreparations(entity, emptyList(), emptyList(), emptyList()))
		}
		override fun getRecipesById(ids: Set<Long>): List<RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getReferencedRecipes(recipeIds: Set<Long>): List<RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getRecipeTitlesWithIds(): Flow<List<RecipeTitleId>> = flowOf(emptyList())
		override fun getRecipeTitleById(id: Long): String = ""
		override fun getRecipeByGourmandId(gourmandId: Int): RecipeWithIngredientsAndPreparations? = null
		override fun getRecipeIdByGourmandId(gourmandId: Long): Long? = null
		override fun getDeprecatedRecipes(gourmandIds: List<Int>): List<RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getRecipeDescriptions(query: String, orderedBy: Int, offset: Int, limit: Int, month: Int): List<RecipeDescription> = emptyList()
		override fun getKeywords(id: Long): List<String> = emptyList()
		override fun getRecipeCount(): Flow<Int> = flowOf(recipes.size)
		override fun getRecipeIds(query: String): List<Long> = emptyList()
		override fun getDependentRecipeIds(recipeIds: Set<Long>): List<Long> = emptyList()
		override fun getAllCategories(): Flow<List<String>> = flowOf(recipes.values.mapNotNull { it.category }.distinct())
		override fun getAllCuisines(): Flow<List<String>> = flowOf(recipes.values.mapNotNull { it.cuisine }.distinct())
		override fun getAllKeywords(): Flow<List<String>> = flowOf(emptyList())

		override fun getCategories(query: String): Flow<List<StringAndCount>> {
			val lower = query.lowercase()
			val matching = recipes.values.filter {
				val catMatch = it.category?.lowercase()?.contains(lower) ?: false
				val instMatch = it.instructions?.lowercase()?.contains(lower) ?: false
				(catMatch || instMatch) && it.category != null
			}
			val counts = matching.mapNotNull { it.category }.groupingBy { it }.eachCount()
			return flowOf(counts.map { StringAndCount(it.key, it.value) })
		}

		override fun getCuisines(query: String): Flow<List<StringAndCount>> {
			val lower = query.lowercase()
			val matching = recipes.values.filter {
				val cuiMatch = it.cuisine?.lowercase()?.contains(lower) ?: false
				val instMatch = it.instructions?.lowercase()?.contains(lower) ?: false
				(cuiMatch || instMatch) && it.cuisine != null
			}
			val counts = matching.mapNotNull { it.cuisine }.groupingBy { it }.eachCount()
			return flowOf(counts.map { StringAndCount(it.key, it.value) })
		}

		override fun getKeywords(query: String): Flow<List<StringAndCount>> = flowOf(emptyList())
		override fun getSources(): Flow<List<String>> = flowOf(emptyList())
		override fun getYieldUnits(): Flow<List<String>> = flowOf(emptyList())
		override fun getIngredientItems(): Flow<List<String>> = flowOf(emptyList())
		override fun getIngredientUnits(): Flow<List<String>> = flowOf(emptyList())

		override suspend fun insertRecipe(recipe: RecipeEntity): Long {
			recipes[recipe.id] = recipe
			return recipe.id
		}
		override suspend fun updateRecipe(recipe: RecipeEntity) { recipes[recipe.id] = recipe }
		override suspend fun deleteRecipe(recipe: RecipeEntity) { recipes.remove(recipe.id) }
		override suspend fun deleteRecipesByIds(recipeIds: Set<Long>) { recipeIds.forEach { recipes.remove(it) } }
		override suspend fun deleteAllRecipes() { recipes.clear() }
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
		dao = OrganizationTestRecipeDao()
		repository = RecipeRepository(dao)
		organizationManager = RecipeOrganizationManager(recipeRepository = repository)
	}

	private fun createSampleDescription(
		id: Long,
		title: String,
		category: String? = null,
		cuisine: String? = null,
		rating: Float? = null,
		cooktime: Int? = null,
		prepared: Long? = null,
		preparationsCount: Int = 0
	): RecipeDescription {
		return RecipeDescription(
			id = id,
			title = title,
			description = null,
			category = category,
			cuisine = cuisine,
			seasonFrom = null,
			seasonUntil = null,
			rating = rating,
			image = null,
			preptime = null,
			cooktime = cooktime,
			created = 1000L,
			modified = 2000L,
			instructionsLength = 100,
			ingredientsCount = 5,
			preparationsCount = preparationsCount,
			prepared = prepared,
			pinned = false
		)
	}

	@Test
	fun `sort recipes by title ascending and descending`() {
		val r1 = createSampleDescription(1L, "Banana Bread")
		val r2 = createSampleDescription(2L, "Apple Pie")
		val r3 = createSampleDescription(3L, "Cherry Tart")

		val list = listOf(r1, r2, r3)

		// Title Ascending (SORTED_BY_TITLE * 2 = 0)
		val asc = organizationManager.sortRecipes(list, SORTED_BY_TITLE * 2)
		assertEquals("Apple Pie", asc[0].title)
		assertEquals("Banana Bread", asc[1].title)
		assertEquals("Cherry Tart", asc[2].title)

		// Title Descending (SORTED_BY_TITLE * 2 + 1 = 1)
		val desc = organizationManager.sortRecipes(list, SORTED_BY_TITLE * 2 + 1)
		assertEquals("Cherry Tart", desc[0].title)
		assertEquals("Banana Bread", desc[1].title)
		assertEquals("Apple Pie", desc[2].title)
	}

	@Test
	fun `sort recipes by rating and cook time`() {
		val r1 = createSampleDescription(1L, "Quick Snack", rating = 3.5f, cooktime = 10)
		val r2 = createSampleDescription(2L, "Gourmet Dinner", rating = 5.0f, cooktime = 60)
		val r3 = createSampleDescription(3L, "Simple Soup", rating = 4.0f, cooktime = 25)

		val list = listOf(r1, r2, r3)

		// Rating Descending
		val byRatingDesc = organizationManager.sortRecipes(list, SORTED_BY_RATING * 2 + 1)
		assertEquals(5.0f, byRatingDesc[0].rating)
		assertEquals(4.0f, byRatingDesc[1].rating)
		assertEquals(3.5f, byRatingDesc[2].rating)

		// Cooktime Ascending
		val byCooktimeAsc = organizationManager.sortRecipes(list, SORTED_BY_COOKTIME * 2)
		assertEquals(10, byCooktimeAsc[0].cooktime)
		assertEquals(25, byCooktimeAsc[1].cooktime)
		assertEquals(60, byCooktimeAsc[2].cooktime)
	}

	@Test
	fun `sort recipes by preparation date and count`() {
		val r1 = createSampleDescription(1L, "Old Dish", prepared = 1000L, preparationsCount = 1)
		val r2 = createSampleDescription(2L, "Recent Dish", prepared = 5000L, preparationsCount = 4)
		val r3 = createSampleDescription(3L, "Unprepared Dish", prepared = null, preparationsCount = 0)

		val list = listOf(r1, r2, r3)

		// Prepared date descending
		val byPreparedDesc = organizationManager.sortRecipes(list, SORTED_BY_PREPARED * 2 + 1)
		assertEquals(2L, byPreparedDesc[0].id)
		assertEquals(1L, byPreparedDesc[1].id)
	}

	@Test
	fun `filter recipes by preparation status prepared vs unprepared`() {
		val r1 = createSampleDescription(1L, "Prepared Once", prepared = 2000L, preparationsCount = 1)
		val r2 = createSampleDescription(2L, "Never Cooked", prepared = null, preparationsCount = 0)

		val list = listOf(r1, r2)

		val prepared = organizationManager.filterByPreparationStatus(list, preparedOnly = true)
		assertEquals(1, prepared.size)
		assertEquals(1L, prepared.first().id)

		val unprepared = organizationManager.filterByPreparationStatus(list, preparedOnly = false)
		assertEquals(1, unprepared.size)
		assertEquals(2L, unprepared.first().id)
	}

	@Test
	fun `filter recipes by preparation date range`() {
		val r1 = createSampleDescription(1L, "Last Week", prepared = 1000L, preparationsCount = 1)
		val r2 = createSampleDescription(2L, "Yesterday", prepared = 5000L, preparationsCount = 2)
		val r3 = createSampleDescription(3L, "Long Ago", prepared = 200L, preparationsCount = 1)

		val list = listOf(r1, r2, r3)

		val inRange = organizationManager.filterByPreparationDateRange(list, 500L, 3000L)
		assertEquals(1, inRange.size)
		assertEquals(1L, inRange.first().id)
	}

	@Test
	fun `organize recipes into collections by category and cuisine`() {
		val r1 = Recipe(id = 1L, title = "Tacos", category = "Main Course", cuisine = "Mexican")
		val r2 = Recipe(id = 2L, title = "Enchiladas", category = "Main Course", cuisine = "Mexican")
		val r3 = Recipe(id = 3L, title = "Tiramisu", category = "Dessert", cuisine = "Italian")

		val recipes = listOf(r1, r2, r3)

		val byCategory = organizationManager.getCollectionsByCategory(recipes)
		assertEquals(2, byCategory.keys.size)
		assertEquals(2, byCategory["Main Course"]?.size)
		assertEquals(1, byCategory["Dessert"]?.size)

		val byCuisine = organizationManager.getCollectionsByCuisine(recipes)
		assertEquals(2, byCuisine.keys.size)
		assertEquals(2, byCuisine["Mexican"]?.size)
		assertEquals(1, byCuisine["Italian"]?.size)
	}

	@Test
	fun `category and cuisine count queries include instruction matches`() = runBlocking {
		val r1 = Recipe(
			id = 10L,
			title = "Slow Braised Meat",
			category = "Stew",
			cuisine = "French",
			instructions = "Simmer slowly in red wine broth"
		)
		repository.upsertSingleRecipe(r1.toRecipeWithIngredientsAndPreparations())

		val catSummary = repository.getCategories("wine broth").first()
		assertEquals(1, catSummary.size)
		assertEquals("Stew", catSummary.first().string)

		val cuiSummary = repository.getCuisines("wine broth").first()
		assertEquals(1, cuiSummary.size)
		assertEquals("French", cuiSummary.first().string)
	}

}
