package com.herohiman.tournant.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitAliasDao {

	@Query("SELECT * FROM UnitAlias ORDER BY aliasName COLLATE LOCALIZED ASC")
	fun getAllUnitAliases(): Flow<List<UnitAliasEntity>>

	@Query("SELECT * FROM UnitAlias ORDER BY aliasName COLLATE LOCALIZED ASC")
	fun getAllUnitAliasesList(): List<UnitAliasEntity>

	@Query("SELECT * FROM UnitAlias WHERE aliasName = :name COLLATE NOCASE LIMIT 1")
	suspend fun getAliasByName(name: String): UnitAliasEntity?

	@Query("SELECT * FROM UnitAlias WHERE id = :id LIMIT 1")
	suspend fun getAliasById(id: Long): UnitAliasEntity?

	@Query("SELECT * FROM UnitAlias WHERE baseUnit = :baseUnit ORDER BY conversionFactor ASC")
	suspend fun getAliasesByBaseUnit(baseUnit: BaseUnitType): List<UnitAliasEntity>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAlias(alias: UnitAliasEntity): Long

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertAliases(aliases: List<UnitAliasEntity>): List<Long>

	@Update
	suspend fun updateAlias(alias: UnitAliasEntity)

	@Delete
	suspend fun deleteAlias(alias: UnitAliasEntity)

	@Query("DELETE FROM UnitAlias WHERE id = :id")
	suspend fun deleteAliasById(id: Long)

	@Query("SELECT COUNT(*) FROM UnitAlias")
	suspend fun getUnitAliasCount(): Int

	companion object {
		/**
		 * Default pre-population unit dictionary covering English and Hindi units.
		 */
		fun getDefaultAliases(): List<UnitAliasEntity> {
			return listOf(
				// KG (Mass)
				UnitAliasEntity(aliasName = "kg", baseUnit = BaseUnitType.KG, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "kilogram", baseUnit = BaseUnitType.KG, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "kilograms", baseUnit = BaseUnitType.KG, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "kilo", baseUnit = BaseUnitType.KG, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "किलो", baseUnit = BaseUnitType.KG, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "किग्रा", baseUnit = BaseUnitType.KG, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "कि.ग्रा.", baseUnit = BaseUnitType.KG, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "g", baseUnit = BaseUnitType.KG, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "gram", baseUnit = BaseUnitType.KG, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "grams", baseUnit = BaseUnitType.KG, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "gm", baseUnit = BaseUnitType.KG, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "gms", baseUnit = BaseUnitType.KG, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "ग्राम", baseUnit = BaseUnitType.KG, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "ग्रा", baseUnit = BaseUnitType.KG, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "mg", baseUnit = BaseUnitType.KG, conversionFactor = 0.000001),
				UnitAliasEntity(aliasName = "milligram", baseUnit = BaseUnitType.KG, conversionFactor = 0.000001),
				UnitAliasEntity(aliasName = "milligrams", baseUnit = BaseUnitType.KG, conversionFactor = 0.000001),
				UnitAliasEntity(aliasName = "मिग्रा", baseUnit = BaseUnitType.KG, conversionFactor = 0.000001),
				UnitAliasEntity(aliasName = "lb", baseUnit = BaseUnitType.KG, conversionFactor = 0.45359237),
				UnitAliasEntity(aliasName = "lbs", baseUnit = BaseUnitType.KG, conversionFactor = 0.45359237),
				UnitAliasEntity(aliasName = "pound", baseUnit = BaseUnitType.KG, conversionFactor = 0.45359237),
				UnitAliasEntity(aliasName = "pounds", baseUnit = BaseUnitType.KG, conversionFactor = 0.45359237),
				UnitAliasEntity(aliasName = "पाउंड", baseUnit = BaseUnitType.KG, conversionFactor = 0.45359237),
				UnitAliasEntity(aliasName = "oz", baseUnit = BaseUnitType.KG, conversionFactor = 0.0283495),
				UnitAliasEntity(aliasName = "ounce", baseUnit = BaseUnitType.KG, conversionFactor = 0.0283495),
				UnitAliasEntity(aliasName = "ounces", baseUnit = BaseUnitType.KG, conversionFactor = 0.0283495),
				UnitAliasEntity(aliasName = "औंस", baseUnit = BaseUnitType.KG, conversionFactor = 0.0283495),

				// LITER (Volume)
				UnitAliasEntity(aliasName = "l", baseUnit = BaseUnitType.LITER, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "liter", baseUnit = BaseUnitType.LITER, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "liters", baseUnit = BaseUnitType.LITER, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "litre", baseUnit = BaseUnitType.LITER, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "litres", baseUnit = BaseUnitType.LITER, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "लीटर", baseUnit = BaseUnitType.LITER, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "ली", baseUnit = BaseUnitType.LITER, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "ml", baseUnit = BaseUnitType.LITER, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "milliliter", baseUnit = BaseUnitType.LITER, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "milliliters", baseUnit = BaseUnitType.LITER, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "millilitre", baseUnit = BaseUnitType.LITER, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "millilitres", baseUnit = BaseUnitType.LITER, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "मिली", baseUnit = BaseUnitType.LITER, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "एमएल", baseUnit = BaseUnitType.LITER, conversionFactor = 0.001),
				UnitAliasEntity(aliasName = "cup", baseUnit = BaseUnitType.LITER, conversionFactor = 0.24),
				UnitAliasEntity(aliasName = "cups", baseUnit = BaseUnitType.LITER, conversionFactor = 0.24),
				UnitAliasEntity(aliasName = "कप", baseUnit = BaseUnitType.LITER, conversionFactor = 0.24),
				UnitAliasEntity(aliasName = "tbsp", baseUnit = BaseUnitType.LITER, conversionFactor = 0.015),
				UnitAliasEntity(aliasName = "tablespoon", baseUnit = BaseUnitType.LITER, conversionFactor = 0.015),
				UnitAliasEntity(aliasName = "tablespoons", baseUnit = BaseUnitType.LITER, conversionFactor = 0.015),
				UnitAliasEntity(aliasName = "बड़ा चम्मच", baseUnit = BaseUnitType.LITER, conversionFactor = 0.015),
				UnitAliasEntity(aliasName = "चम्मच", baseUnit = BaseUnitType.LITER, conversionFactor = 0.015),
				UnitAliasEntity(aliasName = "tsp", baseUnit = BaseUnitType.LITER, conversionFactor = 0.005),
				UnitAliasEntity(aliasName = "teaspoon", baseUnit = BaseUnitType.LITER, conversionFactor = 0.005),
				UnitAliasEntity(aliasName = "teaspoons", baseUnit = BaseUnitType.LITER, conversionFactor = 0.005),
				UnitAliasEntity(aliasName = "छोटा चम्मच", baseUnit = BaseUnitType.LITER, conversionFactor = 0.005),

				// COUNT (Discrete units)
				UnitAliasEntity(aliasName = "unit", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "units", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "piece", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "pieces", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "pc", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "pcs", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "इकाई", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "नग", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "दाने", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "dozen", baseUnit = BaseUnitType.COUNT, conversionFactor = 12.0),
				UnitAliasEntity(aliasName = "dozens", baseUnit = BaseUnitType.COUNT, conversionFactor = 12.0),
				UnitAliasEntity(aliasName = "दर्जन", baseUnit = BaseUnitType.COUNT, conversionFactor = 12.0),
				UnitAliasEntity(aliasName = "pinch", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "pinches", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0),
				UnitAliasEntity(aliasName = "चुटकी", baseUnit = BaseUnitType.COUNT, conversionFactor = 1.0)
			)
		}
	}
}
