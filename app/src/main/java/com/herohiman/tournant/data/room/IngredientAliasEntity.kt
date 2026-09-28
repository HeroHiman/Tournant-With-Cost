package com.herohiman.tournant.data.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "IngredientAlias",
	foreignKeys = [
		ForeignKey(
			entity = MasterIngredientEntity::class,
			parentColumns = ["id"],
			childColumns = ["masterIngredientId"],
			onDelete = ForeignKey.CASCADE,
			onUpdate = ForeignKey.CASCADE
		)
	],
	indices = [
		Index(value = ["rawName"], unique = true),
		Index(value = ["masterIngredientId"])
	]
)
data class IngredientAliasEntity(
	@PrimaryKey(autoGenerate = true)
	var id: Long = 0,
	var rawName: String,
	var masterIngredientId: Long
)
