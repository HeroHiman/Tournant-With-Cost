package com.herohiman.tournant

import com.herohiman.tournant.data.Ingredient
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
import com.herohiman.tournant.ui.elements.RecipeLinkingManager
import com.herohiman.tournant.ui.elements.RecipePreparationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class RecipeLinkingAndPreparationTest {

	private lateinit var dao: InMemoryRecipeDao
	private lateinit var repository: RecipeRepository
	private lateinit var linkingManager: RecipeLinkingManager
	private lateinit var preparationManager: RecipePreparationManager

	private class InMemoryRecipeDao : RecipeDao() {
		private var idCounter = 1L
		val recipes = mutableMapOf<Long, RecipeEntity>()
		val ingredients = mutableMapOf<Long, MutableList<IngredientEntity>>()
		val preparations = mutableMapOf<Long, MutableList<PreparationEntity>>()

		override fun getRecipeById(id: Long): Flow<RecipeWithIngredientsAndPreparations> {
			val entity = recipes[id] ?: return emptyFlow()
			val ingList = ingredients[id] ?: emptyList()
			val prepList = preparations[id] ?: emptyList()
			return flowOf(RecipeWithIngredientsAndPreparations(entity, ingList, emptyList(), prepList))
		}

		override fun getRecipesById(ids: Set<Long>): List<RecipeWithIngredientsAndPreparations> {
			return ids.mapNotNull { id ->
				val entity = recipes[id] ?: return@mapNotNull null
				RecipeWithIngredientsAndPreparations(entity, ingredients[id] ?: emptyList(), emptyList(), preparations[id] ?: emptyList())
			}
		}

		override fun getReferencedRecipes(recipeIds: Set<Long>): List<RecipeWithIngredientsAndPreparations> = emptyList()

		override fun getRecipeTitlesWithIds(): Flow<List<RecipeTitleId>> {
			return flowOf(recipes.values.map { RecipeTitleId(it.id, it.title) })
		}

		override fun getRecipeTitleById(id: Long): String = recipes[id]?.title ?: ""
		override suspend fun getRecipesUsingMasterIngredient(masterIngredientId: Long, ingredientName: String): List<RecipeTitleId> = emptyList()

		override fun getRecipeByGourmandId(gourmandId: Int): RecipeWithIngredientsAndPreparations? = null
		override fun getRecipeIdByGourmandId(gourmandId: Long): Long? = null
		override fun getDeprecatedRecipes(gourmandIds: List<Int>): List<RecipeWithIngredientsAndPreparations> = emptyList()
		override fun getRecipeDescriptions(query: String, orderedBy: Int, offset: Int, limit: Int, month: Int): List<RecipeDescription> = emptyList()
		override fun getKeywords(id: Long): List<String> = emptyList()
		override fun getRecipeCount(): Flow<Int> = flowOf(recipes.size)
		override fun getRecipeIds(query: String): List<Long> = recipes.keys.toList()

		override fun getDependentRecipeIds(recipeIds: Set<Long>): List<Long> {
			val dependent = mutableListOf<Long>()
			for ((recId, ingList) in ingredients) {
				if (ingList.any { it.refId in recipeIds }) {
					dependent.add(recId)
				}
			}
			return dependent
		}

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
		override suspend fun getUniqueIngredientNames(): List<String> = emptyList()
		override suspend fun getPreferredUnitForIngredient(item: String): String? = null

		override suspend fun insertRecipe(recipe: RecipeEntity): Long {
			val id = if (recipe.id == 0L) idCounter++ else recipe.id
			val saved = recipe.copy(id = id)
			recipes[id] = saved
			return id
		}

		override suspend fun updateRecipe(recipe: RecipeEntity) {
			recipes[recipe.id] = recipe
		}

		override suspend fun deleteRecipe(recipe: RecipeEntity) {
			recipes.remove(recipe.id)
			ingredients.remove(recipe.id)
			preparations.remove(recipe.id)
		}

		override suspend fun deleteRecipesByIds(recipeIds: Set<Long>) {
			recipeIds.forEach {
				recipes.remove(it)
				ingredients.remove(it)
				preparations.remove(it)
			}
		}

		override suspend fun deleteAllRecipes() {
			recipes.clear()
			ingredients.clear()
			preparations.clear()
		}

		override suspend fun insertIngredient(ingredient: IngredientEntity) {
			val list = ingredients.getOrPut(ingredient.recipeId) { mutableListOf() }
			list.removeAll { it.position == ingredient.position }
			list.add(ingredient)
		}

		override suspend fun updateIngredient(ingredient: IngredientEntity) {
			insertIngredient(ingredient)
		}

		override suspend fun deleteIngredient(ingredient: IngredientEntity) {
			ingredients[ingredient.recipeId]?.removeIf { it.position == ingredient.position }
		}

		override suspend fun deleteIngredientsNotInList(recipeId: Long, positions: List<Int>) {
			ingredients[recipeId]?.removeIf { it.position !in positions }
		}

		override suspend fun insertKeyword(preparation: KeywordEntity): Long = 1L
		override suspend fun deleteKeywordsNotInList(recipeId: Long, positions: List<Int>) {}

		override suspend fun insertPreparationDate(preparation: PreparationEntity): Long {
			val list = preparations.getOrPut(preparation.recipeId) { mutableListOf() }
			list.add(preparation)
			return 1L
		}

		override suspend fun updatePreparationDate(preparation: PreparationEntity) {
			val list = preparations.getOrPut(preparation.recipeId) { mutableListOf() }
			val index = list.indexOfFirst { it.date.time == preparation.date.time }
			if (index >= 0) {
				list[index] = preparation
			} else {
				list.add(preparation)
			}
		}

		override suspend fun deletePreparationDate(preparation: PreparationEntity) {
			preparations[preparation.recipeId]?.removeIf { it.date.time == preparation.date.time }
		}

		override suspend fun deletePreparationDatesNotInList(recipeId: Long, dates: List<Long>) {
			preparations[recipeId]?.removeIf { it.date.time !in dates }
		}

		override suspend fun getPreparation(recipeId: Long, date: Long): PreparationEntity? {
			return preparations[recipeId]?.find { it.date.time == date }
		}

		override suspend fun getPreparations(recipeId: Long): List<PreparationEntity> {
			return preparations[recipeId]?.sortedByDescending { it.date.time } ?: emptyList()
		}

		override suspend fun getMostRecentPreparation(recipeId: Long): PreparationEntity? {
			return preparations[recipeId]?.maxByOrNull { it.date.time }
		}

		override suspend fun getPreparationsByDateRange(recipeId: Long, startDate: Long, endDate: Long): List<PreparationEntity> {
			return preparations[recipeId]?.filter { it.date.time in startDate..endDate } ?: emptyList()
		}

		override suspend fun pinRecipe(recipePin: RecipePinEntity): Long = 1L
		override suspend fun unpinRecipe(recipeId: Long) {}
	}

	@Before
	fun setup() {
		dao = InMemoryRecipeDao()
		repository = RecipeRepository(dao)
		linkingManager = RecipeLinkingManager(recipeRepository = repository)
		preparationManager = RecipePreparationManager(recipeRepository = repository)
	}

	@Test
	fun `linking recipes adds reference ingredient and detects dependency`() = runBlocking {
		val sauceRecipe = Recipe(id = 10L, title = "Tomato Sauce")
		val pastaRecipe = Recipe(id = 20L, title = "Pasta with Sauce")

		repository.upsertSingleRecipe(sauceRecipe.toRecipeWithIngredientsAndPreparations())
		repository.upsertSingleRecipe(pastaRecipe.toRecipeWithIngredientsAndPreparations())

		// Link sauce as ingredient to pasta
		val linkResult = linkingManager.linkRecipes(sourceRecipeId = 20L, targetRecipeId = 10L)
		assertTrue(linkResult.isSuccess)

		// Check referenced recipes for pasta
		val referenced = linkingManager.getReferencedRecipes(20L)
		assertTrue(referenced.isSuccess)
		assertEquals(1, referenced.getOrNull()?.size)
		assertEquals(10L, referenced.getOrNull()?.first()?.id)

		// Check dependent recipes for sauce
		val dependent = linkingManager.getDependentRecipes(10L)
		assertTrue(dependent.isSuccess)
		assertEquals(1, dependent.getOrNull()?.size)
		assertEquals(20L, dependent.getOrNull()?.first()?.id)
	}

	@Test
	fun `linking recipes prevents circular references`() = runBlocking {
		val recipeA = Recipe(id = 1L, title = "Recipe A")
		val recipeB = Recipe(id = 2L, title = "Recipe B")

		repository.upsertSingleRecipe(recipeA.toRecipeWithIngredientsAndPreparations())
		repository.upsertSingleRecipe(recipeB.toRecipeWithIngredientsAndPreparations())

		// A -> B
		val firstLink = linkingManager.linkRecipes(sourceRecipeId = 1L, targetRecipeId = 2L)
		assertTrue(firstLink.isSuccess)

		// B -> A should fail due to cycle
		val circularLink = linkingManager.linkRecipes(sourceRecipeId = 2L, targetRecipeId = 1L)
		assertTrue(circularLink.isFailure)
	}

	@Test
	fun `unlinking recipes removes referenced ingredient`() = runBlocking {
		val recipeA = Recipe(id = 1L, title = "Base Dough")
		val recipeB = Recipe(id = 2L, title = "Pizza")

		repository.upsertSingleRecipe(recipeA.toRecipeWithIngredientsAndPreparations())
		repository.upsertSingleRecipe(recipeB.toRecipeWithIngredientsAndPreparations())

		// Link
		linkingManager.linkRecipes(sourceRecipeId = 2L, targetRecipeId = 1L)
		assertEquals(1, linkingManager.getReferencedRecipes(2L).getOrNull()?.size)

		// Unlink
		val unlinkResult = linkingManager.unlinkRecipes(sourceRecipeId = 2L, targetRecipeId = 1L)
		assertTrue(unlinkResult.isSuccess)
		assertEquals(0, linkingManager.getReferencedRecipes(2L).getOrNull()?.size)
	}

	@Test
	fun `add and remove preparations tracking with statistics calculation`() = runBlocking {
		val recipe = Recipe(id = 50L, title = "Chocolate Cake")
		repository.upsertSingleRecipe(recipe.toRecipeWithIngredientsAndPreparations())

		val date1 = Date(1000000000L)
		val date2 = Date(1000000000L + 86400000L * 5) // 5 days later

		// Add first preparation
		val add1 = preparationManager.addPreparation(50L, date1)
		assertTrue(add1.isSuccess)

		// Add second preparation
		val add2 = preparationManager.addPreparation(50L, date2)
		assertTrue(add2.isSuccess)

		val list = preparationManager.getPreparations(50L).getOrNull()
		assertNotNull(list)
		assertEquals(2, list?.size)

		val mostRecent = preparationManager.getMostRecentPreparation(50L).getOrNull()
		assertEquals(date2.time, mostRecent?.date?.time)

		// Stats
		val statsResult = preparationManager.getPreparationStats(50L)
		assertTrue(statsResult.isSuccess)
		val stats = statsResult.getOrNull()
		assertEquals(2, stats?.get("totalPreparations"))

		// Remove preparation
		val removeResult = preparationManager.removePreparation(50L, date1)
		assertTrue(removeResult.isSuccess)

		val remaining = preparationManager.getPreparations(50L).getOrNull()
		assertEquals(1, remaining?.size)
		assertEquals(date2.time, remaining?.first()?.date?.time)
	}

}
