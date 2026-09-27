package com.herohiman.tournant.ui.preview

import android.content.Context
import android.text.Spanned
import androidx.core.text.parseAsHtml
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.SoftBreakAddsNewLinePlugin

/**
 * Helper class for rendering recipe content with Markdown support.
 * Provides consistent formatting for recipe instructions and notes.
 */
class RecipePreviewHelper(private val context: Context) {

    private val markwon: Markwon by lazy {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .build()
    }

    /**
     * Converts Markdown text to formatted Spanned for display.
     * Falls back to HTML parsing if Markdown is not available.
     */
    fun formatRecipeText(text: String?): Spanned {
        if (text.isNullOrBlank()) {
            return "".parseAsHtml()
        }

        return try {
            markwon.toMarkdown(text)
        } catch (e: Exception) {
            // Fallback to HTML parsing
            text.parseAsHtml()
        }
    }

    /**
     * Formats ingredient list for preview display.
     * Groups ingredients by their group headers.
     */
    fun formatIngredientsForPreview(ingredients: List<com.herohiman.tournant.data.IngredientLine>): String {
        val builder = StringBuilder()

        ingredients.forEach { ingredientLine ->
            when (ingredientLine) {
                is com.herohiman.tournant.data.IngredientLine.IngredientGroupTitle -> {
                    ingredientLine.title?.let {
                        builder.append("**$it**\n\n")
                    }
                }
                is com.herohiman.tournant.data.IngredientLine.IngredientItem -> {
                    val ingredient = ingredientLine.ingredient
                    val amount = ingredient.amount?.toString() ?: ""
                    val unit = ingredient.unit ?: ""
                    val item = ingredient.item ?: ""

                    builder.append("- $amount $unit $item\n")
                }
            }
        }

        return builder.toString()
    }
}