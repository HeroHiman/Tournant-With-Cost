package com.herohiman.tournant.cost

import com.herohiman.tournant.data.room.IngredientAliasEntity
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.RecipeRepository

data class MergeResult(
	val success: Boolean,
	val mergedCount: Int = 0,
	val message: String = ""
)

class MergeIngredientUseCase(
	private val repository: RecipeRepository
) {

	/**
	 * Merges a single duplicate ingredient [sourceId] into the target core ingredient [targetId].
	 *
	 * 1. Checks that source and target are different and both exist.
	 * 2. Re-points any existing aliases pointing to sourceId to targetId.
	 * 3. Creates an alias mapping source.name -> targetId.
	 * 4. Removes the redundant source ingredient from MasterIngredient table so it disappears from the catalog.
	 */
	suspend fun merge(sourceId: Long, targetId: Long): MergeResult {
		if (sourceId == targetId) {
			return MergeResult(false, 0, "Source and target ingredient cannot be the same")
		}

		val source = repository.getMasterIngredientById(sourceId)
			?: return MergeResult(false, 0, "Source ingredient not found")
		val target = repository.getMasterIngredientById(targetId)
			?: return MergeResult(false, 0, "Target ingredient not found")

		return executeMerge(listOf(source), target)
	}

	/**
	 * Merges multiple duplicate ingredients [sourceIds] into the target core ingredient [targetId].
	 */
	suspend fun mergeMultiple(sourceIds: List<Long>, targetId: Long): MergeResult {
		val validSources = sourceIds.filter { it != targetId }
		if (validSources.isEmpty()) {
			return MergeResult(false, 0, "No valid source ingredients to merge")
		}

		val target = repository.getMasterIngredientById(targetId)
			?: return MergeResult(false, 0, "Target ingredient not found")

		val sources = validSources.mapNotNull { repository.getMasterIngredientById(it) }
		if (sources.isEmpty()) {
			return MergeResult(false, 0, "No existing source ingredients found")
		}

		return executeMerge(sources, target)
	}

	private suspend fun executeMerge(
		sources: List<MasterIngredientEntity>,
		target: MasterIngredientEntity
	): MergeResult {
		var mergedCount = 0

		for (source in sources) {
			val sourceAliases = repository.getAliasesForMaster(source.id)
			// Re-point existing aliases of source to target
			for (alias in sourceAliases) {
				repository.insertIngredientAlias(
					alias.copy(masterIngredientId = target.id)
				)
			}

			// Add source's own name as an alias for target
			repository.insertIngredientAlias(
				IngredientAliasEntity(
					rawName = source.name.trim(),
					masterIngredientId = target.id
				)
			)

			// Remove duplicate source item from MasterIngredient table
			repository.hardDeleteMasterIngredient(source.id)
			mergedCount++
		}

		return MergeResult(
			success = true,
			mergedCount = mergedCount,
			message = "Successfully merged $mergedCount ingredient(s) into ${target.name}"
		)
	}
}
