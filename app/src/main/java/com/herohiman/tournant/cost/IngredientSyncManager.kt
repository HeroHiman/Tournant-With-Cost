package com.herohiman.tournant.cost

import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.RecipeRepository

object IngredientSyncManager {

	/**
	 * Resolves missing ingredients from recipes and bulk-inserts them into the Master Ingredient catalog
	 * with unitCost = 0.0 and their most frequently used unit (defaulting to "unit").
	 *
	 * Existing ingredients (active or inactive) are preserved and never overwritten.
	 *
	 * @param repository The recipe repository containing both recipe and master ingredient data.
	 * @return Number of newly added master ingredients.
	 */
	suspend fun syncIngredientsFromRecipes(repository: RecipeRepository): Int {
		val uniqueNames = repository.getUniqueIngredientNames()
		if (uniqueNames.isEmpty()) return 0

		val existingItems = repository.getAllMasterIngredientsList()
		val existingNameSet = existingItems.map { it.name.trim().lowercase() }.toSet()

		val missingEntities = createMissingMasterIngredients(
			uniqueNames = uniqueNames,
			existingNameSet = existingNameSet,
			unitResolver = { item -> repository.getPreferredUnitForIngredient(item) }
		)

		if (missingEntities.isEmpty()) return 0

		repository.insertMasterIngredients(missingEntities)
		return missingEntities.size
	}

	/**
	 * Domain helper that calculates which MasterIngredientEntity objects need to be created.
	 */
	suspend fun createMissingMasterIngredients(
		uniqueNames: List<String>,
		existingNameSet: Set<String>,
		unitResolver: suspend (String) -> String?
	): List<MasterIngredientEntity> {
		val distinctMissing = LinkedHashMap<String, String>() // lowercase -> originalTrimmed
		for (name in uniqueNames) {
			val trimmed = name.trim()
			val lower = trimmed.lowercase()
			if (trimmed.isNotBlank() && !existingNameSet.contains(lower) && !distinctMissing.containsKey(lower)) {
				distinctMissing[lower] = trimmed
			}
		}

		if (distinctMissing.isEmpty()) return emptyList()

		val results = ArrayList<MasterIngredientEntity>(distinctMissing.size)
		for (trimmedName in distinctMissing.values) {
			val preferredUnit = unitResolver(trimmedName)?.trim()?.takeIf { it.isNotBlank() } ?: "unit"
			results.add(
				MasterIngredientEntity(
					name = trimmedName,
					unitCost = 0.0,
					baseUnit = preferredUnit,
					currency = "USD",
					isActive = true,
					lastUpdated = System.currentTimeMillis()
				)
			)
		}
		return results
	}
}
