package com.herohiman.tournant.ui.elements

import android.content.Context
import com.herohiman.tournant.Constants.Companion.SORTED_BY_COOKTIME
import com.herohiman.tournant.Constants.Companion.SORTED_BY_CREATED
import com.herohiman.tournant.Constants.Companion.SORTED_BY_INGREDIENTS_COUNT
import com.herohiman.tournant.Constants.Companion.SORTED_BY_INSTRUCTIONS_LENGTH
import com.herohiman.tournant.Constants.Companion.SORTED_BY_MODIFIED
import com.herohiman.tournant.Constants.Companion.SORTED_BY_PREPARATIONS_COUNT
import com.herohiman.tournant.Constants.Companion.SORTED_BY_PREPARED
import com.herohiman.tournant.Constants.Companion.SORTED_BY_PREPTIME
import com.herohiman.tournant.Constants.Companion.SORTED_BY_RATING
import com.herohiman.tournant.Constants.Companion.SORTED_BY_TITLE
import com.herohiman.tournant.Constants.Companion.SORTED_BY_TOTALTIME
import com.herohiman.tournant.data.Recipe
import com.herohiman.tournant.data.RecipeDescription
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.StringAndCount
import kotlinx.coroutines.flow.Flow

class RecipeOrganizationManager(
	private val context: Context? = null,
	private val recipeRepository: RecipeRepository? = null
) {

	fun filterByPreparationStatus(
		recipes: List<RecipeDescription>,
		preparedOnly: Boolean
	): List<RecipeDescription> {
		return if (preparedOnly) {
			recipes.filter { it.prepared != null && it.preparationsCount > 0 }
		} else {
			recipes.filter { it.prepared == null || it.preparationsCount == 0 }
		}
	}

	fun filterByPreparationDateRange(
		recipes: List<RecipeDescription>,
		startDate: Long,
		endDate: Long
	): List<RecipeDescription> {
		return recipes.filter { it.prepared != null && it.prepared in startDate..endDate }
	}

	fun sortRecipes(
		recipes: List<RecipeDescription>,
		orderBy: Int
	): List<RecipeDescription> {
		val isAscending = orderBy % 2 == 0
		val sortCriterion = orderBy / 2

		return when (sortCriterion) {
			SORTED_BY_TITLE -> {
				val comparator = Comparator<RecipeDescription> { a, b -> a.title.compareTo(b.title, ignoreCase = true) }
				if (isAscending) recipes.sortedWith(comparator) else recipes.sortedWith(comparator.reversed())
			}
			SORTED_BY_RATING -> {
				if (isAscending) recipes.sortedWith(compareBy(nullsLast()) { it.rating })
				else recipes.sortedWith(compareBy(nullsLast(reverseOrder())) { it.rating })
			}
			SORTED_BY_PREPTIME -> {
				if (isAscending) recipes.sortedWith(compareBy(nullsLast()) { it.preptime })
				else recipes.sortedWith(compareBy(nullsLast(reverseOrder())) { it.preptime })
			}
			SORTED_BY_COOKTIME -> {
				if (isAscending) recipes.sortedWith(compareBy(nullsLast()) { it.cooktime })
				else recipes.sortedWith(compareBy(nullsLast(reverseOrder())) { it.cooktime })
			}
			SORTED_BY_TOTALTIME -> {
				val selector: (RecipeDescription) -> Int? = {
					val prep = it.preptime ?: 0
					val cook = it.cooktime ?: 0
					if (it.preptime == null && it.cooktime == null) null else prep + cook
				}
				if (isAscending) recipes.sortedWith(compareBy(nullsLast(), selector))
				else recipes.sortedWith(compareBy(nullsLast(reverseOrder()), selector))
			}
			SORTED_BY_CREATED -> {
				if (isAscending) recipes.sortedWith(compareBy(nullsLast()) { it.created })
				else recipes.sortedWith(compareBy(nullsLast(reverseOrder())) { it.created })
			}
			SORTED_BY_MODIFIED -> {
				if (isAscending) recipes.sortedWith(compareBy(nullsLast()) { it.modified })
				else recipes.sortedWith(compareBy(nullsLast(reverseOrder())) { it.modified })
			}
			SORTED_BY_INSTRUCTIONS_LENGTH -> {
				if (isAscending) recipes.sortedWith(compareBy(nullsLast()) { it.instructionsLength })
				else recipes.sortedWith(compareBy(nullsLast(reverseOrder())) { it.instructionsLength })
			}
			SORTED_BY_INGREDIENTS_COUNT -> {
				if (isAscending) recipes.sortedBy { it.ingredientsCount }
				else recipes.sortedByDescending { it.ingredientsCount }
			}
			SORTED_BY_PREPARATIONS_COUNT -> {
				if (isAscending) recipes.sortedBy { it.preparationsCount }
				else recipes.sortedByDescending { it.preparationsCount }
			}
			SORTED_BY_PREPARED -> {
				if (isAscending) recipes.sortedWith(compareBy(nullsLast()) { it.prepared })
				else recipes.sortedWith(compareBy(nullsLast(reverseOrder())) { it.prepared })
			}
			else -> if (isAscending) recipes.sortedBy { it.title } else recipes.sortedByDescending { it.title }
		}
	}

	fun getCollectionsByCategory(recipes: List<Recipe>): Map<String, List<Recipe>> {
		return recipes
			.filter { !it.category.isNullOrBlank() }
			.groupBy { it.category!!.trim() }
	}

	fun getCollectionsByCuisine(recipes: List<Recipe>): Map<String, List<Recipe>> {
		return recipes
			.filter { !it.cuisine.isNullOrBlank() }
			.groupBy { it.cuisine!!.trim() }
	}

	fun getCategorySummary(): Flow<List<StringAndCount>>? {
		return recipeRepository?.getCategories("")
	}

	fun getCuisineSummary(): Flow<List<StringAndCount>>? {
		return recipeRepository?.getCuisines("")
	}

}
