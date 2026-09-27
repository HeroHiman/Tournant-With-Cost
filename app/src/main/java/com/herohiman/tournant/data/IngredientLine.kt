package com.herohiman.tournant.data

sealed class IngredientLine {
	data class IngredientGroupTitle(var title: String?) : IngredientLine()
	data class IngredientItem(val ingredient: Ingredient, val isChecked: Boolean = false, val isSelected: Boolean = false) : IngredientLine()

	fun toStringForCooks(optionalString: String) =
		when (this) {
			is IngredientGroupTitle -> title ?: ""
			is IngredientItem -> buildString {
				append(ingredient.toStringForCooks(optionalString))
				if (isChecked) {
					append(" ✓")
				}
			}
		}

	fun deepCopy(): IngredientLine = when (this) {
		is IngredientGroupTitle -> copy()
		is IngredientItem -> copy(ingredient = ingredient.copy())
	}
}