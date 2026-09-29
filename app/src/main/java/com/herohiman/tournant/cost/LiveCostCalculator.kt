package com.herohiman.tournant.cost

import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.UnitAliasEntity
import java.util.Locale
import kotlin.math.round

enum class CostStatus {
	MATCHED,
	MISSING_PRICE,
	INACTIVE_PRICE,
	INCOMPATIBLE_UNITS,
	OPTIONAL_EXCLUDED,
	RECURSION_CYCLE_DETECTED,
	SUBSTITUTE_INACTIVE
}

data class SubRecipeData(
	val recipeId: Long,
	val title: String = "",
	val ingredients: List<Ingredient> = emptyList(),
	val yieldValue: Double = 1.0,
	val yieldUnit: String? = null,
	val overheadCost: Double = 0.0
)

fun interface SubRecipeResolver {
	fun getSubRecipe(recipeId: Long): SubRecipeData?
}

data class IngredientCostItem(
	val ingredient: Ingredient,
	val masterIngredient: MasterIngredientEntity?,
	val matchedUnit: String?,
	val effectiveAmount: Double?,
	val lineCost: Double,
	val status: CostStatus,
	val isDerivedFromSubRecipe: Boolean = false,
	val derivedRecipeId: Long? = null,
	val derivedUnitCost: Double? = null
)

data class RecipeCostBreakdown(
	val totalCost: Double,
	val costPerPortion: Double,
	val yield: Double,
	val currency: String = "USD",
	val isPrivacyMode: Boolean = false,
	val items: List<IngredientCostItem> = emptyList(),
	val unpricedItemCount: Int = 0,
	val hasRecursionCycle: Boolean = items.any { it.status == CostStatus.RECURSION_CYCLE_DETECTED }
) {
	fun formattedTotalCost(symbol: String = "$", mask: String = "••••"): String {
		return if (isPrivacyMode) mask else String.format(Locale.US, "%s%.2f", symbol, totalCost)
	}

	fun formattedCostPerPortion(symbol: String = "$", mask: String = "••••"): String {
		return if (isPrivacyMode) mask else String.format(Locale.US, "%s%.2f", symbol, costPerPortion)
	}

	fun calculateTargetSellingPrice(targetFoodCostPercentage: Double): Double {
		if (targetFoodCostPercentage <= 0.0 || targetFoodCostPercentage > 1.0) return 0.0
		return roundToTwoDecimals(totalCost / targetFoodCostPercentage)
	}

	fun calculatePortionSellingPrice(targetFoodCostPercentage: Double): Double {
		if (targetFoodCostPercentage <= 0.0 || targetFoodCostPercentage > 1.0) return 0.0
		return roundToTwoDecimals(costPerPortion / targetFoodCostPercentage)
	}

	fun calculateMarginPercentage(sellingPrice: Double): Double {
		if (sellingPrice <= 0.0) return 0.0
		return roundToTwoDecimals(((sellingPrice - totalCost) / sellingPrice) * 100.0)
	}

	private fun roundToTwoDecimals(value: Double): Double {
		return round(value * 100.0) / 100.0
	}
}

object LiveCostCalculator {

	private const val GRAMS_PER_KG = 1000.0
	private const val GRAMS_PER_MG = 0.001
	private const val GRAMS_PER_OZ = 28.349523125
	private const val GRAMS_PER_LB = 453.59237

	private const val ML_PER_LITER = 1000.0
	private const val ML_PER_CL = 10.0
	private const val ML_PER_DL = 100.0
	private const val ML_PER_TSP = 4.92892
	private const val ML_PER_TBSP = 14.7868
	private const val ML_PER_FL_OZ = 29.5735
	private const val ML_PER_CUP = 236.588
	private const val ML_PER_PINT = 473.176
	private const val ML_PER_QUART = 946.353
	private const val ML_PER_GALLON = 3785.41

	private val WEIGHT_UNITS = mapOf(
		"g" to 1.0,
		"gram" to 1.0,
		"grams" to 1.0,
		"kg" to GRAMS_PER_KG,
		"kilogram" to GRAMS_PER_KG,
		"kilograms" to GRAMS_PER_KG,
		"mg" to GRAMS_PER_MG,
		"milligram" to GRAMS_PER_MG,
		"milligrams" to GRAMS_PER_MG,
		"oz" to GRAMS_PER_OZ,
		"ounce" to GRAMS_PER_OZ,
		"ounces" to GRAMS_PER_OZ,
		"lb" to GRAMS_PER_LB,
		"lbs" to GRAMS_PER_LB,
		"pound" to GRAMS_PER_LB,
		"pounds" to GRAMS_PER_LB
	)

	private val VOLUME_UNITS = mapOf(
		"ml" to 1.0,
		"milliliter" to 1.0,
		"milliliters" to 1.0,
		"l" to ML_PER_LITER,
		"liter" to ML_PER_LITER,
		"liters" to ML_PER_LITER,
		"cl" to ML_PER_CL,
		"centiliter" to ML_PER_CL,
		"centiliters" to ML_PER_CL,
		"dl" to ML_PER_DL,
		"deciliter" to ML_PER_DL,
		"deciliters" to ML_PER_DL,
		"tsp" to ML_PER_TSP,
		"teaspoon" to ML_PER_TSP,
		"teaspoons" to ML_PER_TSP,
		"tbsp" to ML_PER_TBSP,
		"tablespoon" to ML_PER_TBSP,
		"tablespoons" to ML_PER_TBSP,
		"fl oz" to ML_PER_FL_OZ,
		"fluid ounce" to ML_PER_FL_OZ,
		"fluid ounces" to ML_PER_FL_OZ,
		"cup" to ML_PER_CUP,
		"cups" to ML_PER_CUP,
		"pt" to ML_PER_PINT,
		"pint" to ML_PER_PINT,
		"pints" to ML_PER_PINT,
		"qt" to ML_PER_QUART,
		"quart" to ML_PER_QUART,
		"quarts" to ML_PER_QUART,
		"gal" to ML_PER_GALLON,
		"gallon" to ML_PER_GALLON,
		"gallons" to ML_PER_GALLON
	)

	private val COUNT_UNITS = setOf(
		"pc", "pcs", "piece", "pieces", "unit", "units", "item", "items", "count", "ct",
		"can", "cans", "bottle", "bottles", "bunch", "bunches", "clove", "cloves",
		"slice", "slices", "pinch", "pinches", "dash", "dashes", "package", "packages", "pkg", "pkgs"
	)

	fun calculateRecipeCost(
		ingredients: List<Ingredient>,
		masterIngredients: List<MasterIngredientEntity>,
		yield: Double = 1.0,
		scaleFactor: Double = 1.0,
		includeOptional: Boolean = false,
		isPrivacyMode: Boolean = false,
		currency: String = "USD",
		aliases: Map<String, MasterIngredientEntity> = emptyMap(),
		unitAliases: Map<String, UnitAliasEntity> = emptyMap(),
		subRecipeResolver: SubRecipeResolver? = null,
		currentRecipeId: Long? = null,
		visitedRecipeIds: Set<Long> = emptySet()
	): RecipeCostBreakdown {
		val scaledIngredients = if (scaleFactor > 0.0 && scaleFactor != 1.0) {
			ingredients.map { it.withScaledAmount(scaleFactor) }
		} else {
			ingredients
		}

		// Enforce "Rule of One" for substitute groups: at most one substitute per group remains active
		val normalizedIngredients = normalizeSubstituteStates(scaledIngredients)

		val items = normalizedIngredients.map { ingredient ->
			calculateLineCost(
				ingredient = ingredient,
				masterIngredients = masterIngredients,
				includeOptional = includeOptional,
				aliases = aliases,
				unitAliases = unitAliases,
				subRecipeResolver = subRecipeResolver,
				currentRecipeId = currentRecipeId,
				visitedRecipeIds = visitedRecipeIds
			)
		}

		val totalCost = roundToTwoDecimals(items.sumOf { it.lineCost })
		val effectiveYield = if (yield > 0.0) yield * (if (scaleFactor > 0.0) scaleFactor else 1.0) else 1.0
		val portionCost = if (effectiveYield > 0.0) roundToTwoDecimals(totalCost / effectiveYield) else totalCost
		val unpricedCount = items.count {
			it.status == CostStatus.MISSING_PRICE ||
			it.status == CostStatus.INCOMPATIBLE_UNITS ||
			it.status == CostStatus.RECURSION_CYCLE_DETECTED
		}

		return RecipeCostBreakdown(
			totalCost = totalCost,
			costPerPortion = portionCost,
			yield = effectiveYield,
			currency = currency,
			isPrivacyMode = isPrivacyMode,
			items = items,
			unpricedItemCount = unpricedCount
		)
	}

	/**
	 * Filters ingredients to include standard ingredients (without substitute group)
	 * and only the single active substitute per substitution group, enforcing the Rule of One.
	 */
	fun filterActiveIngredients(ingredients: List<Ingredient>): List<Ingredient> {
		val seenGroups = mutableSetOf<String>()
		return ingredients.filter { ing ->
			val groupId = ing.substituteGroupId
			if (groupId.isNullOrBlank()) {
				true
			} else if (ing.isActiveSubstitute) {
				seenGroups.add(groupId)
			} else {
				false
			}
		}
	}

	/**
	 * Normalizes a list of ingredients so that at most one substitute per group has isActiveSubstitute = true.
	 * If multiple items in the same group are marked active, only the first encountered remains active.
	 */
	fun normalizeSubstituteStates(ingredients: List<Ingredient>): List<Ingredient> {
		val seenActiveGroups = mutableSetOf<String>()
		return ingredients.map { ing ->
			val groupId = ing.substituteGroupId
			if (!groupId.isNullOrBlank()) {
				if (ing.isActiveSubstitute) {
					if (seenActiveGroups.add(groupId)) {
						ing
					} else {
						ing.copy(isActiveSubstitute = false)
					}
				} else {
					ing
				}
			} else {
				ing
			}
		}
	}

	/**
	 * Calculates the total batch mass in grams for all active ingredients with known mass.
	 */
	fun calculateActiveBatchMassGrams(ingredients: List<Ingredient>): Double {
		return filterActiveIngredients(ingredients).mapNotNull { it.getMass() }.sum()
	}

	fun calculateLineCost(
		ingredient: Ingredient,
		masterIngredients: List<MasterIngredientEntity>,
		includeOptional: Boolean = false,
		aliases: Map<String, MasterIngredientEntity> = emptyMap(),
		unitAliases: Map<String, UnitAliasEntity> = emptyMap(),
		subRecipeResolver: SubRecipeResolver? = null,
		currentRecipeId: Long? = null,
		visitedRecipeIds: Set<Long> = emptySet()
	): IngredientCostItem {
		if (ingredient.optional && !includeOptional) {
			return IngredientCostItem(
				ingredient = ingredient,
				masterIngredient = null,
				matchedUnit = ingredient.unit,
				effectiveAmount = ingredient.amount,
				lineCost = 0.0,
				status = CostStatus.OPTIONAL_EXCLUDED
			)
		}

		val itemName = ingredient.item?.trim()
		val refId = ingredient.refId

		// Inactive substitutes are excluded from the total cost ($0.00) without generating unpriced errors
		if (!ingredient.substituteGroupId.isNullOrBlank() && !ingredient.isActiveSubstitute) {
			val candidate = if (!itemName.isNullOrBlank()) {
				findMatchingMasterIngredient(itemName, masterIngredients, aliases)
			} else if (refId != null) {
				masterIngredients.firstOrNull { it.linkedRecipeId == refId && it.isActive }
			} else null

			return IngredientCostItem(
				ingredient = ingredient,
				masterIngredient = candidate,
				matchedUnit = ingredient.unit,
				effectiveAmount = ingredient.amount,
				lineCost = 0.0,
				status = CostStatus.SUBSTITUTE_INACTIVE
			)
		}
		if (itemName.isNullOrBlank() && refId == null) {
			return IngredientCostItem(
				ingredient = ingredient,
				masterIngredient = null,
				matchedUnit = ingredient.unit,
				effectiveAmount = ingredient.amount,
				lineCost = 0.0,
				status = CostStatus.MISSING_PRICE
			)
		}

		// Find matching master ingredient by alias, exact, or fuzzy name, or by refId
		var candidate = if (!itemName.isNullOrBlank()) {
			findMatchingMasterIngredient(itemName, masterIngredients, aliases)
		} else null

		if (candidate == null && refId != null) {
			candidate = masterIngredients.firstOrNull { it.linkedRecipeId == refId && it.isActive }
		}

		// Direct recipe reference with no master ingredient
		if (candidate == null && refId != null) {
			val cycleDetected = (refId in visitedRecipeIds) || (currentRecipeId != null && refId == currentRecipeId)
			if (cycleDetected) {
				return IngredientCostItem(
					ingredient = ingredient,
					masterIngredient = null,
					matchedUnit = ingredient.unit,
					effectiveAmount = ingredient.amount ?: 1.0,
					lineCost = 0.0,
					status = CostStatus.RECURSION_CYCLE_DETECTED
				)
			}

			if (subRecipeResolver != null) {
				val subRecipe = subRecipeResolver.getSubRecipe(refId)
				if (subRecipe != null) {
					val nextVisited = visitedRecipeIds + setOfNotNull(currentRecipeId, refId)
					val subBreakdown = calculateRecipeCost(
						ingredients = subRecipe.ingredients,
						masterIngredients = masterIngredients,
						yield = subRecipe.yieldValue,
						scaleFactor = 1.0,
						includeOptional = includeOptional,
						aliases = aliases,
						unitAliases = unitAliases,
						subRecipeResolver = subRecipeResolver,
						currentRecipeId = refId,
						visitedRecipeIds = nextVisited
					)
					val subTotal = subBreakdown.totalCost + subRecipe.overheadCost
					val subYield = if (subBreakdown.yield > 0.0) subBreakdown.yield else 1.0
					val costPerYield = subTotal / subYield
					val amount = ingredient.amount ?: 1.0
					val lineCost = roundToTwoDecimals(amount * costPerYield)
					return IngredientCostItem(
						ingredient = ingredient,
						masterIngredient = null,
						matchedUnit = ingredient.unit ?: subRecipe.yieldUnit ?: "unit",
						effectiveAmount = amount,
						lineCost = lineCost,
						status = CostStatus.MATCHED,
						isDerivedFromSubRecipe = true,
						derivedRecipeId = refId,
						derivedUnitCost = roundToTwoDecimals(costPerYield)
					)
				}
			}

			return IngredientCostItem(
				ingredient = ingredient,
				masterIngredient = null,
				matchedUnit = ingredient.unit,
				effectiveAmount = ingredient.amount,
				lineCost = 0.0,
				status = CostStatus.MISSING_PRICE
			)
		}

		if (candidate == null) {
			return IngredientCostItem(
				ingredient = ingredient,
				masterIngredient = null,
				matchedUnit = ingredient.unit,
				effectiveAmount = ingredient.amount,
				lineCost = 0.0,
				status = CostStatus.MISSING_PRICE
			)
		}

		// Soft deletion check: Inactive ingredients cannot be actively priced
		if (!candidate.isActive) {
			return IngredientCostItem(
				ingredient = ingredient,
				masterIngredient = candidate,
				matchedUnit = candidate.baseUnit,
				effectiveAmount = ingredient.amount,
				lineCost = 0.0,
				status = CostStatus.INACTIVE_PRICE
			)
		}

		var effectiveUnitCost = candidate.unitCost
		var isDerived = false
		var derivedRecipeId: Long? = null

		val targetRecipeId = candidate.linkedRecipeId ?: refId
		if (targetRecipeId != null) {
			val cycleDetected = (targetRecipeId in visitedRecipeIds) || (currentRecipeId != null && targetRecipeId == currentRecipeId)
			if (cycleDetected) {
				return IngredientCostItem(
					ingredient = ingredient,
					masterIngredient = candidate,
					matchedUnit = candidate.baseUnit,
					effectiveAmount = ingredient.amount ?: 1.0,
					lineCost = 0.0,
					status = CostStatus.RECURSION_CYCLE_DETECTED
				)
			}

			if (subRecipeResolver != null) {
				val subRecipe = subRecipeResolver.getSubRecipe(targetRecipeId)
				if (subRecipe != null) {
					val nextVisited = visitedRecipeIds + setOfNotNull(currentRecipeId, targetRecipeId)
					val subBreakdown = calculateRecipeCost(
						ingredients = subRecipe.ingredients,
						masterIngredients = masterIngredients,
						yield = subRecipe.yieldValue,
						scaleFactor = 1.0,
						includeOptional = includeOptional,
						aliases = aliases,
						unitAliases = unitAliases,
						subRecipeResolver = subRecipeResolver,
						currentRecipeId = targetRecipeId,
						visitedRecipeIds = nextVisited
					)
					val subTotal = subBreakdown.totalCost + subRecipe.overheadCost
					val subYield = if (subBreakdown.yield > 0.0) subBreakdown.yield else 1.0
					val costPerYield = subTotal / subYield
					val calculatedDerivedCost = if ((candidate.yieldRatio ?: 0.0) > 0.0) {
						costPerYield / candidate.yieldRatio!!
					} else {
						costPerYield
					}

					if (calculatedDerivedCost > 0.0) {
						effectiveUnitCost = calculatedDerivedCost
						isDerived = true
						derivedRecipeId = targetRecipeId
					}
				}
			}
		}

		val amount = ingredient.amount ?: 1.0
		val conversionFactor = resolveConversionFactor(ingredient.unit, candidate.baseUnit, unitAliases)
		if (conversionFactor == null) {
			return IngredientCostItem(
				ingredient = ingredient,
				masterIngredient = candidate,
				matchedUnit = candidate.baseUnit,
				effectiveAmount = amount,
				lineCost = 0.0,
				status = CostStatus.INCOMPATIBLE_UNITS,
				isDerivedFromSubRecipe = isDerived,
				derivedRecipeId = derivedRecipeId,
				derivedUnitCost = if (isDerived) roundToTwoDecimals(effectiveUnitCost) else null
			)
		}

		val convertedAmount = amount * conversionFactor
		val lineCost = roundToTwoDecimals(convertedAmount * effectiveUnitCost)

		return IngredientCostItem(
			ingredient = ingredient,
			masterIngredient = candidate,
			matchedUnit = candidate.baseUnit,
			effectiveAmount = convertedAmount,
			lineCost = lineCost,
			status = CostStatus.MATCHED,
			isDerivedFromSubRecipe = isDerived,
			derivedRecipeId = derivedRecipeId,
			derivedUnitCost = if (isDerived) roundToTwoDecimals(effectiveUnitCost) else null
		)
	}

	fun resolveConversionFactor(
		sourceUnit: String?,
		targetUnit: String?,
		unitAliases: Map<String, UnitAliasEntity> = emptyMap()
	): Double? {
		return UnitConverterEngine.resolveConversionFactor(sourceUnit, targetUnit, unitAliases)
	}

	fun findMatchingMasterIngredient(
		itemName: String,
		masterIngredients: List<MasterIngredientEntity>,
		aliases: Map<String, MasterIngredientEntity> = emptyMap()
	): MasterIngredientEntity? {
		val normalizedName = itemName.lowercase(Locale.ROOT).trim()

		// 0. Direct alias dictionary match (from merged variations)
		aliases[normalizedName]?.let { return it }

		// 1. Exact match (case insensitive)
		masterIngredients.firstOrNull { it.name.trim().equals(normalizedName, ignoreCase = true) }?.let {
			return it
		}

		// 2. Singular/plural match (e.g., "Bananas" vs "Banana", "Eggs" vs "Egg")
		val singular = if (normalizedName.endsWith("s")) normalizedName.dropLast(1) else normalizedName
		masterIngredients.firstOrNull {
			val masterName = it.name.trim().lowercase(Locale.ROOT)
			val masterSingular = if (masterName.endsWith("s")) masterName.dropLast(1) else masterName
			masterSingular == singular
		}?.let {
			return it
		}

		// 3. Substring match (e.g., "All-Purpose Flour" contains "Flour")
		return masterIngredients.firstOrNull {
			val masterName = it.name.trim().lowercase(Locale.ROOT)
			normalizedName.contains(masterName) || masterName.contains(normalizedName)
		}
	}

	private fun roundToTwoDecimals(value: Double): Double {
		return round(value * 100.0) / 100.0
	}
}
