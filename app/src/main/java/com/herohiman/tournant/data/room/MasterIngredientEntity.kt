package com.herohiman.tournant.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MasterIngredient")
data class MasterIngredientEntity(
	@PrimaryKey(autoGenerate = true)
	var id: Long = 0,
	@ColumnInfo(index = true)
	var name: String,
	var unitCost: Double,
	var baseUnit: String,
	var currency: String = "USD",
	var isActive: Boolean = true,
	var lastUpdated: Long = System.currentTimeMillis(),
	var category: String? = null,
	var notes: String? = null
)
