package com.herohiman.tournant.data

import com.squareup.moshi.JsonClass
import com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations

@JsonClass(generateAdapter = true)
data class RecipeList(
	val recipes: List<RecipeWithIngredientsAndPreparations>
)
