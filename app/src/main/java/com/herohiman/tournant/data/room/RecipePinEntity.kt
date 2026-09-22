package com.herohiman.tournant.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "RecipePin")
data class RecipePinEntity (
	@PrimaryKey
	val recipeId: Long
)