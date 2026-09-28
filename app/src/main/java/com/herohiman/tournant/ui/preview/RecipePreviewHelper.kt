package com.herohiman.tournant.ui.preview

import android.content.Context
import android.text.Spanned
import androidx.core.text.parseAsHtml
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.SoftBreakAddsNewLinePlugin

import com.herohiman.tournant.roundToNDigits
import com.herohiman.tournant.toStringForCooks

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

    companion object {
        /**
         * Formats yield value and unit for preview display with optional scaling.
         * Safely handles null, zero, negative, or infinite values.
         */
        @JvmStatic
        fun formatYieldForPreview(
            yieldValue: Double?,
            yieldUnit: String?,
            scaleFactor: Double = 1.0
        ): String {
            if (yieldValue == null || yieldValue <= 0.0 || yieldValue.isNaN() || yieldValue.isInfinite()) {
                return ""
            }
            val safeScale = if (scaleFactor > 0.0 && !scaleFactor.isNaN() && !scaleFactor.isInfinite()) scaleFactor else 1.0
            val targetYield = if (safeScale != 1.0) {
                val raw = yieldValue * safeScale
                val roundedInt = kotlin.math.round(raw)
                if (kotlin.math.abs(raw - roundedInt) < 0.001) roundedInt else raw.roundToNDigits(2)
            } else {
                yieldValue
            }
            val formattedYield = targetYield.toStringForCooks(thousands = false)
            return if (!yieldUnit.isNullOrBlank()) {
                "$formattedYield $yieldUnit"
            } else {
                formattedYield
            }
        }

        /**
         * Formats ingredient list for preview display.
         * Groups ingredients by their group headers and applies scaling.
         */
        @JvmStatic
        fun formatIngredientsForPreview(
            ingredients: List<com.herohiman.tournant.data.IngredientLine>,
            scaleFactor: Double = 1.0,
            optionalWord: String = ""
        ): String {
            val builder = StringBuilder()
            val safeScale = if (scaleFactor > 0.0 && !scaleFactor.isNaN() && !scaleFactor.isInfinite()) scaleFactor else 1.0

            ingredients.forEach { ingredientLine ->
                when (ingredientLine) {
                    is com.herohiman.tournant.data.IngredientLine.IngredientGroupTitle -> {
                        ingredientLine.title?.let {
                            if (builder.isNotEmpty()) builder.append("\n")
                            builder.append("**$it**\n\n")
                        }
                    }
                    is com.herohiman.tournant.data.IngredientLine.IngredientItem -> {
                        val baseIngredient = ingredientLine.ingredient
                        val ingredient = if (safeScale != 1.0) {
                            baseIngredient.withScaledAmount(safeScale)
                        } else {
                            baseIngredient
                        }
                        val formatted = ingredient.toStringForCooks(optionalWord).trim()
                        if (formatted.isNotEmpty()) {
                            builder.append("- $formatted\n")
                        }
                    }
                }
            }

            return builder.toString()
        }

        /**
         * Formats live recipe cost summary for preview display.
         * Returns empty string if ingredients or prices are unavailable.
         * Respects Privacy Mode masking.
         */
        @JvmStatic
        fun formatCostForPreview(
            ingredients: List<com.herohiman.tournant.data.Ingredient>,
            masterIngredients: List<com.herohiman.tournant.data.room.MasterIngredientEntity>,
            yieldValue: Double? = 1.0,
            scaleFactor: Double = 1.0,
            isPrivacyMode: Boolean = false,
            symbol: String = "$",
            subRecipeResolver: com.herohiman.tournant.cost.SubRecipeResolver? = null,
            currentRecipeId: Long? = null
        ): String {
            if (ingredients.isEmpty() || masterIngredients.isEmpty()) return ""
            val breakdown = com.herohiman.tournant.cost.LiveCostCalculator.calculateRecipeCost(
                ingredients = ingredients,
                masterIngredients = masterIngredients,
                yield = yieldValue ?: 1.0,
                scaleFactor = scaleFactor,
                isPrivacyMode = isPrivacyMode,
                subRecipeResolver = subRecipeResolver,
                currentRecipeId = currentRecipeId
            )
            return if (breakdown.totalCost <= 0.0 && !isPrivacyMode) {
                ""
            } else {
                val totalStr = breakdown.formattedTotalCost(symbol = symbol)
                val portionStr = breakdown.formattedCostPerPortion(symbol = symbol)
                "Cost: $totalStr ($portionStr / serving)"
            }
        }
    }
}