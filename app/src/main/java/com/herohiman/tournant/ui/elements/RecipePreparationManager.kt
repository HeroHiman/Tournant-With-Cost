// RecipePreparationManager - Core implementation
package com.herohiman.tournant.ui.elements

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.herohiman.tournant.R
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations
import com.herohiman.tournant.data.room.PreparationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.Result
import kotlinx.coroutines.flow.MutableStateFlow

class RecipePreparationManager(
    private val context: Context,
    private val recipeRepository: RecipeRepository
) {

    companion object {
        private const val TAG = "RecipePreparationManager"
    }

    suspend fun addPreparation(recipeId: Long, preparationDate: Date = Date()): Result<Unit> {
        return try {
            val recipe = withContext(Dispatchers.IO) { recipeRepository.getRecipeById(recipeId).firstOrNull() }
            if (recipe == null) {
                Result.failure(Exception("Recipe not found"))
            } else {
                withContext(Dispatchers.IO) { recipeRepository.addPreparation(recipeId, preparationDate) }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding preparation", e)
            Result.failure(e)
        }
    }

    suspend fun removePreparation(recipeId: Long, date: Date): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) { recipeRepository.removePreparation(recipeId, date) }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing preparation", e)
            Result.failure(e)
        }
    }

    suspend fun getPreparations(recipeId: Long): Result<List<com.herohiman.tournant.data.room.PreparationEntity>> {
        return try {
            val preparations = withContext(Dispatchers.IO) { recipeRepository.getPreparations(recipeId) }
            Result.success(preparations)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting preparations", e)
            Result.failure(e)
        }
    }

    suspend fun getMostRecentPreparation(recipeId: Long): Result<com.herohiman.tournant.data.room.PreparationEntity?> {
        return try {
            val preparation = withContext(Dispatchers.IO) { recipeRepository.getMostRecentPreparation(recipeId) }
            Result.success(preparation)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting most recent preparation", e)
            Result.failure(e)
        }
    }

    suspend fun getPreparationsByDateRange(recipeId: Long, startDate: Date, endDate: Date): Result<List<com.herohiman.tournant.data.room.PreparationEntity>> {
        return try {
            val preparations = withContext(Dispatchers.IO) { recipeRepository.getPreparationsByDateRange(recipeId, startDate, endDate) }
            Result.success(preparations)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting preparations by date range", e)
            Result.failure(e)
        }
    }

    suspend fun getPreparationStats(recipeId: Long): Result<Map<String, Any>> {
        return try {
            val preparations = withContext(Dispatchers.IO) { recipeRepository.getPreparations(recipeId) }
            val totalPreparations = preparations.size
            
            val stats = mutableMapOf<String, Any>(
                "totalPreparations" to totalPreparations,
                "lastPreparationDate" to (preparations.firstOrNull()?.date as Any?),
                "firstPreparationDate" to (preparations.lastOrNull()?.date as Any?),
                "averageDaysBetween" to if (totalPreparations > 1) {
                    val dates = preparations.map { it.date.time }.sortedDescending()
                    val intervals = dates.windowed(2).map { (it[0] - it[1]) / (1000 * 60 * 60 * 24) }
                    intervals.average() as Any
                } else null,
                "preparationsThisMonth" to preparations.count { 
                    it.date.after(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))
                }
            )
            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting preparation stats", e)
            Result.failure(e)
        }
    }

    // UI state management
    data class PreparationUiState(
        val isAddingPreparation: Boolean = false,
        val isRemovingPreparation: Boolean = false,
        val addError: String? = null,
        val removeError: String? = null,
        val showPreparationDialog: Boolean = false,
        val selectedRecipeId: Long? = null,
        val selectedPreparationDate: Date? = null,
        val preparations: List<com.herohiman.tournant.data.room.PreparationEntity> = emptyList(),
        val preparationStats: Map<String, Any> = emptyMap()
    )

    fun createPreparationUiState() = MutableStateFlow(
        PreparationUiState()
    )
}