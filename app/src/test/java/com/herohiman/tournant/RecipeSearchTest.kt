package com.herohiman.tournant

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecipeSearchTest {

	private lateinit var dao: InMemorySearchRecipeDao
	private lateinit var repository: RecipeRepository

	private class InMemorySearchRecipeDao : RecipeDao() {
		private var idCounter = 1L
		val recipes = mutableMapOf<Long, RecipeEntity>()
		val keywords = mutableMapOf<Long, MutableList<KeywordEntity>>()

		override fun getRecipeById(id: Long): Flow<RecipeWithIngredientsAndPreparations> {
			val entity = recipes[id] ?: return emptyFlow()
			return flowOf(RecipeWithIngredientsAndPreparations(entity, emptyList(), keywords[id] ?: emptyList(), emptyList()))
		}

		override fun getRecipesById(ids: Set<Long>): List<RecipeWithIngredientsAndPreparations> {
			return ids.mapNotNull { id ->
				val entity = recipes[id] ?: return@mapNotNull null
				RecipeWithIngredientsAndPreparations(entity, emptyList(), keywords[id] ?: emptyList(), emptyList())
			}
		}

		override fun getReferencedRecipes(recipeIds: Set<Long>): List<RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getRecipeTitlesWithIds(): Flow<List<RecipeTitleId>> =
			flowOf(recipes.values.map { RecipeTitleId(it.id, it.title) })

		override fun getRecipeTitleById(id: Long): String = recipes[id]?.title ?: ""
		override fun getRecipeByGourmandId(gourmandId: Int): RecipeWithIngredientsAndPreparations? = null
		override fun getRecipeIdByGourmandId(gourmandId: Long): Long? = null
		override fun getDeprecatedRecipes(gourmandIds: List<Int>): List<RecipeWithIngredientsAndPreparations> = emptyList()

		override fun getRecipeDescriptions(query: String, orderedBy: Int, offset: Int, limit: Int, month: Int): List<RecipeDescription> {
			val lower = query.lowercase()
			val matched = recipes.values.filter { entity ->
				val kwMatch = keywords[entity.id]?.any { it.keyword.lowercase().contains(lower) } ?: false
				val titleMatch = entity.title.lowercase().contains(lower)
				val descMatch = entity.description?.lowercase()?.contains(lower) ?: false
				val catMatch = entity.category?.lowercase()?.contains(lower) ?: false
				val cuisineMatch = entity.cuisine?.lowercase()?.contains(lower) ?: false
				val instMatch = entity.instructions?.lowercase()?.contains(lower) ?: false
				titleMatch || descMatch || catMatch || cuisineMatch || kwMatch || instMatch
			}
			return matched.drop(offset).take(limit).map { entity ->
				RecipeDescription(
					id = entity.id,
					title = entity.title,
					description = entity.description,
					category = entity.category,
					cuisine = entity.cuisine,
					rating = entity.rating,
					seasonFrom = entity.seasonFrom,
					seasonUntil = entity.seasonUntil,
					image = entity.image,
					preptime = entity.preptime,
					cooktime = entity.cooktime,
					created = entity.created?.time,
					modified = entity.modified?.time,
					instructionsLength = entity.instructions?.length ?: 0,
					ingredientsCount = 0,
					preparationsCount = 0,
					prepared = null,
					pinned = false
				)
			}
		}

		override fun getKeywords(id: Long): List<String> = keywords[id]?.map { it.keyword } ?: emptyList()
		override fun getRecipeCount(): Flow<Int> = flowOf(recipes.size)

		override fun getRecipeIds(query: String): List<Long> {
			val lower = query.lowercase()
			return recipes.values.filter { entity ->
				val kwMatch = keywords[entity.id]?.any { it.keyword.lowercase().contains(lower) } ?: false
				val titleMatch = entity.title.lowercase().contains(lower)
				val descMatch = entity.description?.lowercase()?.contains(lower) ?: false
				val catMatch = entity.category?.lowercase()?.contains(lower) ?: false
				val cuisineMatch = entity.cuisine?.lowercase()?.contains(lower) ?: false
				val instMatch = entity.instructions?.lowercase()?.contains(lower) ?: false
				titleMatch || descMatch || catMatch || cuisineMatch || kwMatch || instMatch
			}.map { it.id }
		}

		override fun getDependentRecipeIds(recipeIds: Set<Long>): List<Long> = emptyList()

		override fun getAllCategories(): Flow<List<String>> =
			flowOf(recipes.values.mapNotNull { it.category }.distinct())

		override fun getAllCuisines(): Flow<List<String>> =
			flowOf(recipes.values.mapNotNull { it.cuisine }.distinct())

		override fun getAllKeywords(): Flow<List<String>> =
			flowOf(keywords.values.flatten().map { it.keyword }.distinct())

		override fun getCategories(query: String): Flow<List<StringAndCount>> {
			val lower = query.lowercase()
			val cats = recipes.values.mapNotNull { it.category }.filter { it.lowercase().contains(lower) }
			return flowOf(cats.groupingBy { it }.eachCount().map { StringAndCount(it.key, it.value) })
		}

		override fun getCuisines(query: String): Flow<List<StringAndCount>> {
			val lower = query.lowercase()
			val cuis = recipes.values.mapNotNull { it.cuisine }.filter { it.lowercase().contains(lower) }
			return flowOf(cuis.groupingBy { it }.eachCount().map { StringAndCount(it.key, it.value) })
		}

		override fun getKeywords(query: String): Flow<List<StringAndCount>> {
			val lower = query.lowercase()
			val kws = keywords.values.flatten().map { it.keyword }.filter { it.lowercase().contains(lower) }
			return flowOf(kws.groupingBy { it }.eachCount().map { StringAndCount(it.key, it.value) })
		}

		override fun getSources(): Flow<List<String>> = flowOf(emptyList())
		override fun getYieldUnits(): Flow<List<String>> = flowOf(emptyList())
		override fun getIngredientItems(): Flow<List<String>> = flowOf(emptyList())
		override fun getIngredientUnits(): Flow<List<String>> = flowOf(emptyList())

		override suspend fun insertRecipe(recipe: RecipeEntity): Long {
			val id = if (recipe.id == 0L) idCounter++ else recipe.id
			recipes[id] = recipe.copy(id = id)
			return id
		}

		override suspend fun updateRecipe(recipe: RecipeEntity) {
			recipes[recipe.id] = recipe
		}

		override suspend fun deleteRecipe(recipe: RecipeEntity) {
			recipes.remove(recipe.id)
			keywords.remove(recipe.id)
		}

		override suspend fun deleteRecipesByIds(recipeIds: Set<Long>) {
			recipeIds.forEach {
				recipes.remove(it)
				keywords.remove(it)
			}
		}

		override suspend fun deleteAllRecipes() {
			recipes.clear()
			keywords.clear()
		}

		override suspend fun insertIngredient(ingredient: IngredientEntity) {}
		override suspend fun updateIngredient(ingredient: IngredientEntity) {}
		override suspend fun deleteIngredient(ingredient: IngredientEntity) {}
		override suspend fun deleteIngredientsNotInList(recipeId: Long, positions: List<Int>) {}

		override suspend fun insertKeyword(keyword: KeywordEntity): Long {
			val list = keywords.getOrPut(keyword.recipeId) { mutableListOf() }
			list.add(keyword)
			return 1L
		}

		override suspend fun deleteKeywordsNotInList(recipeId: Long, positions: List<Int>) {
			keywords[recipeId]?.removeIf { it.position !in positions }
		}

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
		dao = InMemorySearchRecipeDao()
		repository = RecipeRepository(dao)
	}

	@Test
	fun `search finds recipes by title matching query`() = runBlocking {
		val recipe1 = Recipe(id = 1L, title = "Spaghetti Bolognese", category = "Main")
		val recipe2 = Recipe(id = 2L, title = "Pancake Stack", category = "Breakfast")
		repository.upsertSingleRecipe(recipe1.toRecipeWithIngredientsAndPreparations())
		repository.upsertSingleRecipe(recipe2.toRecipeWithIngredientsAndPreparations())

		val results = repository.getRecipeIds("Spaghetti")
		assertEquals(1, results.size)
		assertEquals(1L, results.first())
	}

	@Test
	fun `search finds recipes by instruction content matching query`() = runBlocking {
		val recipe = Recipe(
			id = 3L,
			title = "Roasted Cauliflower",
			instructions = "Preheat oven to 200C and roast until tender"
		)
		repository.upsertSingleRecipe(recipe.toRecipeWithIngredientsAndPreparations())

		val results = repository.getRecipeIds("roast until tender")
		assertEquals(1, results.size)
		assertEquals(3L, results.first())
	}

	@Test
	fun `search finds recipes by keyword or category matching query`() = runBlocking {
		val recipe = Recipe(
			id = 4L,
			title = "Caesar Salad",
			category = "Appetizer",
			cuisine = "Italian",
			keywords = linkedSetOf("crispy", "parmesan")
		)
		repository.upsertSingleRecipe(recipe.toRecipeWithIngredientsAndPreparations())

		// Search by category
		val catResults = repository.getRecipeIds("Appetizer")
		assertEquals(1, catResults.size)
		assertEquals(4L, catResults.first())

		// Search by cuisine
		val cuiResults = repository.getRecipeIds("Italian")
		assertEquals(1, cuiResults.size)
		assertEquals(4L, cuiResults.first())

		// Search by keyword
		val kwResults = repository.getRecipeIds("parmesan")
		assertEquals(1, kwResults.size)
		assertEquals(4L, kwResults.first())
	}

	@Test
	fun `search returns empty when no recipes match query`() = runBlocking {
		val recipe = Recipe(id = 5L, title = "Apple Pie")
		repository.upsertSingleRecipe(recipe.toRecipeWithIngredientsAndPreparations())

		val results = repository.getRecipeIds("NonExistentTerm")
		assertTrue(results.isEmpty())
	}

	@Test
	fun `search query persistence state management simulation`() {
		// Test simulation of query save, load, and clear state
		var savedPreference: String? = null

		fun saveQuery(query: String?) {
			savedPreference = if (query.isNullOrEmpty()) null else query
		}

		fun loadQuery(): String? = savedPreference

		// Initial state is null
		assertNull(loadQuery())

		// User types a search query
		saveQuery("Chocolate")
		assertEquals("Chocolate", loadQuery())

		// User clears search
		saveQuery("")
		assertNull(loadQuery())

		// User sets new query
		saveQuery("Vanilla")
		assertEquals("Vanilla", loadQuery())

		// User closes search
		saveQuery(null)
		assertNull(loadQuery())
	}

	@Test
	fun `autocomplete suggestions count and chip filtering`() = runBlocking {
		val r1 = Recipe(id = 1L, title = "Tacos", category = "Mexican Dinner", cuisine = "Mexican")
		val r2 = Recipe(id = 2L, title = "Burritos", category = "Mexican Lunch", cuisine = "Mexican")
		repository.upsertSingleRecipe(r1.toRecipeWithIngredientsAndPreparations())
		repository.upsertSingleRecipe(r2.toRecipeWithIngredientsAndPreparations())

		val catSuggestions = repository.getCategories("Mexican").first()
		assertEquals(2, catSuggestions.size)

		val cuiSuggestions = repository.getCuisines("Mex").first()
		assertEquals(1, cuiSuggestions.size)
		assertEquals("Mexican", cuiSuggestions.first().string)
		assertEquals(2, cuiSuggestions.first().count)
	}

}
