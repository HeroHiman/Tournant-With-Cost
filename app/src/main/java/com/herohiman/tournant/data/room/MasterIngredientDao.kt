package com.herohiman.tournant.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterIngredientDao {

	@Query("SELECT * FROM MasterIngredient WHERE isActive = 1 ORDER BY name COLLATE LOCALIZED ASC")
	fun getAllActiveMasterIngredients(): Flow<List<MasterIngredientEntity>>

	@Query("SELECT * FROM MasterIngredient WHERE isActive = 1 ORDER BY name COLLATE LOCALIZED ASC")
	fun getAllActiveMasterIngredientsList(): List<MasterIngredientEntity>

	@Query("SELECT * FROM MasterIngredient ORDER BY name COLLATE LOCALIZED ASC")
	fun getAllMasterIngredients(): Flow<List<MasterIngredientEntity>>

	@Query("SELECT * FROM MasterIngredient ORDER BY name COLLATE LOCALIZED ASC")
	fun getAllMasterIngredientsList(): List<MasterIngredientEntity>

	@Query("SELECT * FROM MasterIngredient WHERE name LIKE '%' || :query || '%' OR (category IS NOT NULL AND category LIKE '%' || :query || '%') ORDER BY name COLLATE LOCALIZED ASC")
	fun searchMasterIngredients(query: String): Flow<List<MasterIngredientEntity>>

	@Query("SELECT * FROM MasterIngredient WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR (category IS NOT NULL AND category LIKE '%' || :query || '%')) ORDER BY name COLLATE LOCALIZED ASC")
	fun searchActiveMasterIngredients(query: String): Flow<List<MasterIngredientEntity>>

	@Query("SELECT * FROM MasterIngredient WHERE name LIKE '%' || :query || '%' OR (category IS NOT NULL AND category LIKE '%' || :query || '%') ORDER BY name COLLATE LOCALIZED ASC")
	fun searchMasterIngredientsList(query: String): List<MasterIngredientEntity>

	@Query("SELECT * FROM MasterIngredient WHERE id = :id")
	fun getMasterIngredientById(id: Long): MasterIngredientEntity?

	@Query("SELECT * FROM MasterIngredient WHERE name = :name COLLATE NOCASE AND isActive = 1 LIMIT 1")
	fun getMasterIngredientByName(name: String): MasterIngredientEntity?

	@Query("SELECT * FROM MasterIngredient WHERE linkedRecipeId = :recipeId AND isActive = 1 LIMIT 1")
	suspend fun getMasterIngredientByLinkedRecipeId(recipeId: Long): MasterIngredientEntity?

	@Query("SELECT * FROM MasterIngredient WHERE linkedRecipeId IS NOT NULL AND isActive = 1")
	suspend fun getMasterIngredientsWithLinkedRecipes(): List<MasterIngredientEntity>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertMasterIngredient(item: MasterIngredientEntity): Long

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertMasterIngredients(items: List<MasterIngredientEntity>): List<Long>

	@Update
	suspend fun updateMasterIngredient(item: MasterIngredientEntity)

	@Query("UPDATE MasterIngredient SET isActive = 0 WHERE id = :id")
	suspend fun softDeleteMasterIngredient(id: Long)

	@Query("UPDATE MasterIngredient SET isActive = 1 WHERE id = :id")
	suspend fun restoreMasterIngredient(id: Long)

	@Query("DELETE FROM MasterIngredient WHERE id = :id")
	suspend fun hardDeleteMasterIngredient(id: Long)
}
