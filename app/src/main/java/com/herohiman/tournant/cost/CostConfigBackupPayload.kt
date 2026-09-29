package com.herohiman.tournant.cost

import com.squareup.moshi.JsonClass
import com.herohiman.tournant.data.room.IngredientAliasEntity
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.UnitAliasEntity

@JsonClass(generateAdapter = true)
data class CostConfigBackupPayload(
	val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
	val exportedAt: Long = System.currentTimeMillis(),
	val masterIngredients: List<MasterIngredientEntity> = emptyList(),
	val unitAliases: List<UnitAliasEntity> = emptyList(),
	val ingredientAliases: List<IngredientAliasEntity> = emptyList()
) {
	companion object {
		const val CURRENT_SCHEMA_VERSION = 1
	}
}

data class ImportConfigResult(
	val success: Boolean,
	val message: String,
	val importedMastersCount: Int = 0,
	val importedUnitAliasesCount: Int = 0,
	val importedIngredientAliasesCount: Int = 0
)
