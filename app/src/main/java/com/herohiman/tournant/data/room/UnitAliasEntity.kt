package com.herohiman.tournant.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(
	tableName = "UnitAlias",
	indices = [Index(value = ["aliasName"], unique = true)]
)
data class UnitAliasEntity(
	@PrimaryKey(autoGenerate = true)
	var id: Long = 0,
	var aliasName: String,
	var baseUnit: BaseUnitType,
	var conversionFactor: Double
)
