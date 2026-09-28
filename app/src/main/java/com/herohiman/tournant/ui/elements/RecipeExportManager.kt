package com.herohiman.tournant.ui.elements

import android.content.Context
import com.herohiman.tournant.data.Recipe
import com.herohiman.tournant.gourmand.GourmandIssues
import java.util.Locale

class RecipeExportManager(
	private val context: Context? = null
) {

	fun checkGourmandCompatibility(
		recipes: List<Recipe>,
		defaultLocale: Locale = Locale.getDefault()
	): Set<String> {
		val issues = mutableSetOf<String>()
		for (recipe in recipes) {
			if (!recipe.description.isNullOrBlank()) {
				issues.add(GourmandIssues.NO_DESCRIPTIONS)
			}
			if (recipe.keywords.isNotEmpty()) {
				issues.add(GourmandIssues.NO_KEYWORDS)
			}
			if (recipe.language != defaultLocale) {
				issues.add(GourmandIssues.NO_LANGUAGE)
			}
			if (recipe.season != null) {
				issues.add(GourmandIssues.NO_SEASON)
			}
			val yv = recipe.yieldValue
			if (yv != null && yv > yv.toInt()) {
				issues.add(GourmandIssues.NO_FRACTIONS_IN_YIELD)
			}
		}
		return issues
	}

	fun formatRecipeForTextShare(recipe: Recipe): String {
		val sb = StringBuilder()
		sb.appendLine(recipe.title)
		if (!recipe.description.isNullOrBlank()) {
			sb.appendLine(recipe.description)
		}
		if (recipe.yieldValue != null) {
			sb.appendLine()
			sb.append("Yield: ")
			val yVal = if (recipe.yieldValue!! % 1.0 == 0.0) {
				recipe.yieldValue!!.toInt().toString()
			} else {
				recipe.yieldValue.toString()
			}
			sb.append(yVal)
			if (!recipe.yieldUnit.isNullOrBlank()) {
				sb.append(" ").append(recipe.yieldUnit)
			}
			sb.appendLine()
		}
		if (recipe.ingredients.isNotEmpty()) {
			sb.appendLine()
			sb.appendLine("Ingredients:")
			var currentGroup: String? = null
			for (ingredient in recipe.ingredients) {
				if (!ingredient.group.isNullOrBlank() && ingredient.group != currentGroup) {
					currentGroup = ingredient.group
					sb.appendLine("[$currentGroup]")
				}
				sb.append("- ").appendLine(ingredient.toStringForCooks(""))
			}
		}
		if (!recipe.instructions.isNullOrBlank()) {
			sb.appendLine()
			sb.appendLine("Instructions:")
			sb.appendLine(recipe.instructions)
		}
		if (!recipe.notes.isNullOrBlank()) {
			sb.appendLine()
			sb.appendLine("Notes:")
			sb.appendLine(recipe.notes)
		}
		return sb.toString().trimEnd()
	}

	fun getMimeType(format: String): String {
		return when (format.lowercase()) {
			"json" -> "application/json"
			"xml" -> "application/xml"
			"zip" -> "application/zip"
			"txt", "text" -> "text/plain"
			else -> "application/octet-stream"
		}
	}

	fun getExportFileName(recipeTitles: List<String>, format: String): String {
		val baseName = if (recipeTitles.size == 1) {
			recipeTitles.first().replace(Regex("[^a-zA-Z0-9._-]"), "_")
		} else {
			"recipes_export"
		}
		val ext = format.lowercase().removePrefix(".")
		return "$baseName.$ext"
	}

}
