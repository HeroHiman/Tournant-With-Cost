// RecipeLinkingManager - Core implementation
package com.herohiman.tournant.ui.elements

import android.content.Context
import android.util.Log
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlin.Result
import kotlinx.coroutines.flow.MutableStateFlow

class RecipeLinkingManager(
    private val context: Context,
    private val recipeRepository: RecipeRepository
) {

    companion object {
        private const val TAG = "RecipeLinkingManager"
    }

    suspend fun linkRecipes(sourceRecipeId: Long, targetRecipeId: Long): Result<Unit> {
        return try {
            withContext<Result<Unit>>(Dispatchers.IO) {
                val sourceRecipe = recipeRepository.getRecipeById(sourceRecipeId).firstOrNull()
                    ?: return@withContext Result.failure(Exception("Source recipe not found"))
                val targetRecipe = recipeRepository.getRecipeById(targetRecipeId).firstOrNull()
                    ?: return@withContext Result.failure(Exception("Target recipe not found"))

                // Check for circular references
                if (wouldCreateCycle(sourceRecipeId, targetRecipeId)) {
                    return@withContext Result.failure(Exception("Linking these recipes would create a circular reference"))
                }

                // Add target as ingredient to source
                val newIngredient = com.herohiman.tournant.data.Ingredient(
                    amount = 1.0,
                    unit = "recipe",
                    item = targetRecipe.recipe.title,
                    refId = targetRecipe.recipe.id
                )
                
                // Create updated ingredients list
                val updatedIngredients = sourceRecipe.toRecipe().ingredients.toMutableList().apply {
                    add(newIngredient)
                }
                
                // Create updated recipe
                val updatedSourceRecipe = sourceRecipe.toRecipe().copy(
                    ingredients = updatedIngredients
                )

                // Save the updated recipe with the new ingredient
                recipeRepository.upsertSingleRecipe(updatedSourceRecipe.toRecipeWithIngredientsAndPreparations())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error linking recipes", e)
            Result.failure(e)
        }
    }

    suspend fun unlinkRecipes(sourceRecipeId: Long, targetRecipeId: Long): Result<Unit> {
        return try {
            withContext<Result<Unit>>(Dispatchers.IO) {
                val sourceRecipe = recipeRepository.getRecipeById(sourceRecipeId).firstOrNull()
                    ?: return@withContext Result.failure(Exception("Recipe not found"))

                // Remove target from source ingredients
                val updatedIngredients = sourceRecipe.toRecipe().ingredients.filter { ingredient ->
                    ingredient.refId != targetRecipeId
                }.toMutableList()

                val updatedSourceRecipe = sourceRecipe.toRecipe().copy(
                    ingredients = updatedIngredients
                )

                recipeRepository.upsertSingleRecipe(updatedSourceRecipe.toRecipeWithIngredientsAndPreparations())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unlinking recipe", e)
            Result.failure(e)
        }
    }

    suspend fun getDependentRecipes(recipeId: Long): Result<List<com.herohiman.tournant.data.RecipeTitleId>> {
        return try {
            withContext(Dispatchers.IO) {
                val dependentIds = recipeRepository.getDependentRecipeIds(setOf(recipeId))
                Result.success(dependentIds.map { id ->
                    com.herohiman.tournant.data.RecipeTitleId(id, recipeRepository.getRecipeTitleById(id))
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting dependent recipes", e)
            Result.failure(e)
        }
    }

    suspend fun getReferencedRecipes(recipeId: Long): Result<List<com.herohiman.tournant.data.RecipeTitleId>> {
        return try {
            withContext(Dispatchers.IO) {
                val sourceRecipe = recipeRepository.getRecipeById(recipeId).firstOrNull()
                if (sourceRecipe == null) {
                    return@withContext Result.failure(Exception("Recipe not found"))
                }

                val referencedIds = sourceRecipe.ingredients
                    .mapNotNull { it.refId }
                    .distinct()

                Result.success(referencedIds.map { id ->
                    com.herohiman.tournant.data.RecipeTitleId(id, recipeRepository.getRecipeTitleById(id))
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting referenced recipes", e)
            Result.failure(e)
        }
    }

    // Helper function to detect cycles
    private suspend fun wouldCreateCycle(sourceId: Long, targetId: Long): Boolean {
        val visited = mutableSetOf<Long>()
        val recursionStack = mutableSetOf<Long>()

        suspend fun hasCycle(currentId: Long): Boolean {
            if (currentId == sourceId) return true
            if (currentId in recursionStack) return true
            if (currentId in visited) return false

            visited.add(currentId)
            recursionStack.add(currentId)

            // Get all recipes that currentId references
            val currentRecipe = recipeRepository.getRecipeById(currentId).firstOrNull()
            if (currentRecipe != null) {
                val referencedIds = currentRecipe.ingredients.mapNotNull { it.refId }

                for (referencedId in referencedIds) {
                    if (hasCycle(referencedId)) return true
                }
            }

            recursionStack.remove(currentId)
            return false
        }

        return hasCycle(targetId)
    }

    // UI state management
    data class LinkingUiState(
        val isLinkingInProgress: Boolean = false,
        val isUnlinkingInProgress: Boolean = false,
        val linkError: String? = null,
        val unlinkError: String? = null,
        val showLinkDialog: Boolean = false,
        val selectedRecipeId: Long? = null,
        val dependentRecipes: List<com.herohiman.tournant.data.RecipeTitleId> = emptyList(),
        val referencedRecipes: List<com.herohiman.tournant.data.RecipeTitleId> = emptyList()
    )

    fun createLinkingUiState(): MutableStateFlow<LinkingUiState> = MutableStateFlow(
        LinkingUiState()
    )
}