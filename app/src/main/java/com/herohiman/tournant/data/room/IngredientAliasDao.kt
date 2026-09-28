package com.herohiman.tournant.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientAliasDao {

	@Query("SELECT * FROM IngredientAlias WHERE rawName = :rawName COLLATE NOCASE LIMIT 1")
	suspend fun getAliasByRawName(rawName: String): IngredientAliasEntity?

	@Query("""
		SELECT m.* FROM MasterIngredient m
		INNER JOIN IngredientAlias a ON m.id = a.masterIngredientId
		WHERE a.rawName = :rawName COLLATE NOCASE AND m.isActive = 1
		LIMIT 1
	""")
	suspend fun getMasterIngredientForRawName(rawName: String): MasterIngredientEntity?

	@Query("SELECT * FROM IngredientAlias WHERE masterIngredientId = :masterIngredientId ORDER BY rawName COLLATE LOCALIZED ASC")
	suspend fun getAliasesForMaster(masterIngredientId: Long): List<IngredientAliasEntity>

	@Query("SELECT * FROM IngredientAlias ORDER BY rawName COLLATE LOCALIZED ASC")
	fun getAllAliases(): Flow<List<IngredientAliasEntity>>

	@Query("SELECT * FROM IngredientAlias ORDER BY rawName COLLATE LOCALIZED ASC")
	fun getAllAliasesList(): List<IngredientAliasEntity>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAlias(alias: IngredientAliasEntity): Long

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAliases(aliases: List<IngredientAliasEntity>): List<Long>

	@Update
	suspend fun updateAlias(alias: IngredientAliasEntity)

	@Delete
	suspend fun deleteAlias(alias: IngredientAliasEntity)

	@Query("DELETE FROM IngredientAlias WHERE id = :id")
	suspend fun deleteAliasById(id: Long)

	@Query("DELETE FROM IngredientAlias WHERE rawName = :rawName COLLATE NOCASE")
	suspend fun deleteAliasByRawName(rawName: String)

	@Query("DELETE FROM IngredientAlias WHERE masterIngredientId = :masterIngredientId")
	suspend fun deleteAliasesForMaster(masterIngredientId: Long)

	@Query("SELECT COUNT(*) FROM IngredientAlias")
	suspend fun getIngredientAliasCount(): Int
}
