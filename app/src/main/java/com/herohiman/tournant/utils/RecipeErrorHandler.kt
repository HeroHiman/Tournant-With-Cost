package com.herohiman.tournant.utils

import java.io.FileNotFoundException
import java.io.IOException

enum class RecipeErrorCategory {
	FILE_IO,
	FORMAT_CORRUPTED,
	OUT_OF_MEMORY,
	VALIDATION,
	DATABASE,
	UNKNOWN
}

data class ActionableError(
	val category: RecipeErrorCategory,
	val title: String,
	val userMessage: String,
	val actionableSuggestion: String,
	val canRetry: Boolean
)

object RecipeErrorHandler {

	fun classifyError(throwable: Throwable): ActionableError {
		return when (throwable) {
			is OutOfMemoryError -> ActionableError(
				category = RecipeErrorCategory.OUT_OF_MEMORY,
				title = "Insufficient Memory",
				userMessage = "The app encountered a low-memory condition while processing this operation.",
				actionableSuggestion = "Try closing background apps or importing a smaller, compressed image file.",
				canRetry = true
			)
			is FileNotFoundException -> ActionableError(
				category = RecipeErrorCategory.FILE_IO,
				title = "File Not Found",
				userMessage = "The requested file or recipe image could not be accessed.",
				actionableSuggestion = "Verify the file location or re-select the file from your device storage.",
				canRetry = true
			)
			is IOException -> ActionableError(
				category = RecipeErrorCategory.FILE_IO,
				title = "Storage Access Error",
				userMessage = "Unable to read from or write to device storage.",
				actionableSuggestion = "Check storage permissions and ensure sufficient disk space is available.",
				canRetry = true
			)
			is IllegalArgumentException -> ActionableError(
				category = RecipeErrorCategory.VALIDATION,
				title = "Invalid Input",
				userMessage = throwable.localizedMessage ?: "The provided recipe data is invalid.",
				actionableSuggestion = "Please check the required fields (e.g. recipe title, numeric yield) and try again.",
				canRetry = false
			)
			is IllegalStateException -> ActionableError(
				category = RecipeErrorCategory.DATABASE,
				title = "Database State Error",
				userMessage = "A database conflict or unexpected state occurred.",
				actionableSuggestion = "Restart the application or refresh the recipe list to reload local state.",
				canRetry = true
			)
			else -> {
				val msg = throwable.localizedMessage ?: ""
				if (msg.contains("JSON", ignoreCase = true) || msg.contains("XML", ignoreCase = true) || msg.contains("parse", ignoreCase = true)) {
					ActionableError(
						category = RecipeErrorCategory.FORMAT_CORRUPTED,
						title = "Unrecognized File Format",
						userMessage = "The file could not be parsed into a valid recipe format.",
						actionableSuggestion = "Ensure the file is a valid Tournant JSON or Gourmand XML recipe export.",
						canRetry = false
					)
				} else {
					ActionableError(
						category = RecipeErrorCategory.UNKNOWN,
						title = "Unexpected Error",
						userMessage = msg.ifBlank { "An unexpected error occurred." },
						actionableSuggestion = "Please retry the operation. If the issue persists, consider exporting a backup.",
						canRetry = true
					)
				}
			}
		}
	}

}
