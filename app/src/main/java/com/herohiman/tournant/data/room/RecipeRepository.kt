package com.herohiman.tournant.data.room

import android.util.Log
import androidx.room.Transaction
import androidx.room.withTransaction
import com.herohiman.tournant.cost.CostConfigBackupPayload
import com.herohiman.tournant.cost.ImportConfigResult
import java.util.Date

class RecipeRepository(
	private val dao: RecipeDao,
	private val masterIngredientDao: MasterIngredientDao? = null,
	private val unitAliasDao: UnitAliasDao? = null,
	private val ingredientAliasDao: IngredientAliasDao? = null,
	private val database: RecipeRoomDatabase? = null
) {

	companion object { private const val TAG = "RecipeRepository" }

	// Master Ingredient operations
	fun getAllActiveMasterIngredients() = masterIngredientDao?.getAllActiveMasterIngredients()
	fun getAllActiveMasterIngredientsList() = masterIngredientDao?.getAllActiveMasterIngredientsList() ?: emptyList()
	fun getAllMasterIngredients() = masterIngredientDao?.getAllMasterIngredients()
	fun getAllMasterIngredientsList() = masterIngredientDao?.getAllMasterIngredientsList() ?: emptyList()
	fun searchMasterIngredients(query: String) = masterIngredientDao?.searchMasterIngredients(query)
	fun searchActiveMasterIngredients(query: String) = masterIngredientDao?.searchActiveMasterIngredients(query)
	fun searchMasterIngredientsList(query: String) = masterIngredientDao?.searchMasterIngredientsList(query) ?: emptyList()
	fun getMasterIngredientById(id: Long) = masterIngredientDao?.getMasterIngredientById(id)
	fun getMasterIngredientByName(name: String) = masterIngredientDao?.getMasterIngredientByName(name)
	suspend fun getMasterIngredientByLinkedRecipeId(recipeId: Long) = masterIngredientDao?.getMasterIngredientByLinkedRecipeId(recipeId)
	suspend fun getMasterIngredientsWithLinkedRecipes() = masterIngredientDao?.getMasterIngredientsWithLinkedRecipes() ?: emptyList()
	suspend fun insertMasterIngredient(item: MasterIngredientEntity) = masterIngredientDao?.insertMasterIngredient(item) ?: -1L
	suspend fun insertMasterIngredients(items: List<MasterIngredientEntity>) = masterIngredientDao?.insertMasterIngredients(items) ?: emptyList()
	suspend fun updateMasterIngredient(item: MasterIngredientEntity) { masterIngredientDao?.updateMasterIngredient(item) }
	suspend fun softDeleteMasterIngredient(id: Long) { masterIngredientDao?.softDeleteMasterIngredient(id) }
	suspend fun restoreMasterIngredient(id: Long) { masterIngredientDao?.restoreMasterIngredient(id) }
	suspend fun hardDeleteMasterIngredient(id: Long) { masterIngredientDao?.hardDeleteMasterIngredient(id) }
	suspend fun syncIngredientsFromRecipes(): Int = com.herohiman.tournant.cost.IngredientSyncManager.syncIngredientsFromRecipes(this)
	suspend fun getRecipesUsingMasterIngredient(
		masterIngredientId: Long,
		ingredientName: String = ""
	): List<com.herohiman.tournant.data.RecipeTitleId> =
		dao.getRecipesUsingMasterIngredient(masterIngredientId, ingredientName)

	fun getSubRecipeData(recipeId: Long): com.herohiman.tournant.cost.SubRecipeData? {
		val recipeWithData = dao.getRecipesById(setOf(recipeId)).firstOrNull() ?: return null
		val recipe = recipeWithData.toRecipe()
		return com.herohiman.tournant.cost.SubRecipeData(
			recipeId = recipe.id,
			title = recipe.title ?: "",
			ingredients = recipe.ingredients,
			yieldValue = recipe.yieldValue ?: 1.0,
			yieldUnit = recipe.yieldUnit
		)
	}

	fun asSubRecipeResolver(): com.herohiman.tournant.cost.SubRecipeResolver =
		com.herohiman.tournant.cost.SubRecipeResolver { recipeId ->
			getSubRecipeData(recipeId)
		}

	// Unit Alias operations
	fun getAllUnitAliases() = unitAliasDao?.getAllUnitAliases()
	fun getAllUnitAliasesList() = unitAliasDao?.getAllUnitAliasesList() ?: emptyList()
	fun searchUnitAliases(query: String) = unitAliasDao?.searchUnitAliases(query)
	fun searchUnitAliasesList(query: String) = unitAliasDao?.searchUnitAliasesList(query) ?: emptyList()
	fun searchUnitAliasesByCategory(query: String, baseUnit: BaseUnitType) = unitAliasDao?.searchUnitAliasesByCategory(query, baseUnit)
	fun searchUnitAliasesByCategoryList(query: String, baseUnit: BaseUnitType) = unitAliasDao?.searchUnitAliasesByCategoryList(query, baseUnit) ?: emptyList()
	suspend fun getUnitAliasByName(name: String) = unitAliasDao?.getAliasByName(name)
	suspend fun getUnitAliasById(id: Long) = unitAliasDao?.getAliasById(id)
	suspend fun getUnitAliasesByBaseUnit(baseUnit: BaseUnitType) = unitAliasDao?.getAliasesByBaseUnit(baseUnit) ?: emptyList()
	suspend fun insertUnitAlias(alias: UnitAliasEntity) = unitAliasDao?.insertAlias(alias) ?: -1L
	suspend fun insertUnitAliases(aliases: List<UnitAliasEntity>) = unitAliasDao?.insertAliases(aliases) ?: emptyList()
	suspend fun updateUnitAlias(alias: UnitAliasEntity) { unitAliasDao?.updateAlias(alias) }
	suspend fun deleteUnitAlias(alias: UnitAliasEntity) { unitAliasDao?.deleteAlias(alias) }
	suspend fun deleteUnitAliasById(id: Long) { unitAliasDao?.deleteAliasById(id) }
	suspend fun getUnitAliasCount(): Int = unitAliasDao?.getUnitAliasCount() ?: 0

	suspend fun seedDefaultUnitAliasesIfEmpty(): Int {
		val count = getUnitAliasCount()
		if (count == 0 && unitAliasDao != null) {
			val defaults = UnitAliasDao.getDefaultAliases()
			val inserted = unitAliasDao.insertAliases(defaults)
			return inserted.size
		}
		return 0
	}

	suspend fun buildUnitAliasLookupMap(): Map<String, UnitAliasEntity> {
		val aliases = getAllUnitAliasesList()
		if (aliases.isEmpty()) return emptyMap()
		return aliases.associateBy { it.aliasName.trim().lowercase(java.util.Locale.ROOT) }
	}

	// Ingredient Alias operations
	suspend fun getIngredientAliasByRawName(rawName: String) = ingredientAliasDao?.getAliasByRawName(rawName)
	suspend fun getMasterIngredientForRawName(rawName: String) = ingredientAliasDao?.getMasterIngredientForRawName(rawName)
	suspend fun getAliasesForMaster(masterIngredientId: Long) = ingredientAliasDao?.getAliasesForMaster(masterIngredientId) ?: emptyList()
	fun getAllIngredientAliases() = ingredientAliasDao?.getAllAliases()
	fun getAllIngredientAliasesList() = ingredientAliasDao?.getAllAliasesList() ?: emptyList()
	suspend fun insertIngredientAlias(alias: IngredientAliasEntity) = ingredientAliasDao?.insertAlias(alias) ?: -1L
	suspend fun insertIngredientAliases(aliases: List<IngredientAliasEntity>) = ingredientAliasDao?.insertAliases(aliases) ?: emptyList()
	suspend fun deleteIngredientAlias(alias: IngredientAliasEntity) { ingredientAliasDao?.deleteAlias(alias) }
	suspend fun deleteIngredientAliasById(id: Long) { ingredientAliasDao?.deleteAliasById(id) }
	suspend fun deleteIngredientAliasByRawName(rawName: String) { ingredientAliasDao?.deleteAliasByRawName(rawName) }
	suspend fun deleteIngredientAliasesForMaster(masterIngredientId: Long) { ingredientAliasDao?.deleteAliasesForMaster(masterIngredientId) }
	suspend fun getIngredientAliasCount(): Int = ingredientAliasDao?.getIngredientAliasCount() ?: 0

	suspend fun buildIngredientAliasLookupMap(): Map<String, MasterIngredientEntity> {
		val aliases = getAllIngredientAliasesList()
		if (aliases.isEmpty()) return emptyMap()

		val masterMap = getAllActiveMasterIngredientsList().associateBy { it.id }
		val lookup = mutableMapOf<String, MasterIngredientEntity>()
		for (alias in aliases) {
			masterMap[alias.masterIngredientId]?.let { master ->
				lookup[alias.rawName.trim().lowercase()] = master
			}
		}
		return lookup
	}

	fun exportCostConfiguration(): CostConfigBackupPayload {
		return CostConfigBackupPayload(
			schemaVersion = CostConfigBackupPayload.CURRENT_SCHEMA_VERSION,
			exportedAt = System.currentTimeMillis(),
			masterIngredients = getAllMasterIngredientsList(),
			unitAliases = getAllUnitAliasesList(),
			ingredientAliases = getAllIngredientAliasesList()
		)
	}

	fun exportUnitConfiguration(): CostConfigBackupPayload {
		return CostConfigBackupPayload(
			schemaVersion = CostConfigBackupPayload.CURRENT_SCHEMA_VERSION,
			exportedAt = System.currentTimeMillis(),
			masterIngredients = emptyList(),
			unitAliases = getAllUnitAliasesList(),
			ingredientAliases = emptyList()
		)
	}

	suspend fun importCostConfiguration(payload: CostConfigBackupPayload): ImportConfigResult {
		if (payload.schemaVersion <= 0 || payload.schemaVersion > CostConfigBackupPayload.CURRENT_SCHEMA_VERSION) {
			return ImportConfigResult(
				success = false,
				message = "Unsupported configuration schema version: ${payload.schemaVersion}"
			)
		}

		if (masterIngredientDao == null && unitAliasDao == null && ingredientAliasDao == null) {
			return ImportConfigResult(
				success = false,
				message = "Database DAOs not initialized"
			)
		}

		suspend fun executeImport(): ImportConfigResult {
			var importedMasters = 0
			var importedUnits = 0
			var importedIngredientAliases = 0

			val masterIdMap = mutableMapOf<Long, Long>()

			// 1. Process Master Ingredients
			if (masterIngredientDao != null) {
				for (master in payload.masterIngredients) {
					val existing = masterIngredientDao.getMasterIngredientByName(master.name)
					val resolvedId = if (existing != null) {
						val updated = master.copy(
							id = existing.id,
							lastUpdated = System.currentTimeMillis()
						)
						masterIngredientDao.updateMasterIngredient(updated)
						existing.id
					} else {
						val toInsert = master.copy(
							id = 0L,
							lastUpdated = System.currentTimeMillis()
						)
						masterIngredientDao.insertMasterIngredient(toInsert)
					}
					if (master.id != 0L) {
						masterIdMap[master.id] = resolvedId
					}
					importedMasters++
				}
			}

			// 2. Process Unit Aliases
			if (unitAliasDao != null) {
				for (unitAlias in payload.unitAliases) {
					val existing = unitAliasDao.getAliasByName(unitAlias.aliasName)
					if (existing != null) {
						val updated = unitAlias.copy(id = existing.id)
						unitAliasDao.updateAlias(updated)
					} else {
						unitAliasDao.insertAlias(unitAlias.copy(id = 0L))
					}
					importedUnits++
				}
			}

			// 3. Process Ingredient Aliases
			if (ingredientAliasDao != null) {
				for (alias in payload.ingredientAliases) {
					val targetMasterId = masterIdMap[alias.masterIngredientId] ?: alias.masterIngredientId
					val targetMaster = masterIngredientDao?.getMasterIngredientById(targetMasterId)
					if (targetMaster != null) {
						val existing = ingredientAliasDao.getAliasByRawName(alias.rawName)
						if (existing != null) {
							val updated = alias.copy(id = existing.id, masterIngredientId = targetMasterId)
							ingredientAliasDao.updateAlias(updated)
						} else {
							ingredientAliasDao.insertAlias(alias.copy(id = 0L, masterIngredientId = targetMasterId))
						}
						importedIngredientAliases++
					}
				}
			}

			return ImportConfigResult(
				success = true,
				message = "Configuration imported successfully",
				importedMastersCount = importedMasters,
				importedUnitAliasesCount = importedUnits,
				importedIngredientAliasesCount = importedIngredientAliases
			)
		}

		return try {
			if (database != null) {
				database.withTransaction {
					executeImport()
				}
			} else {
				executeImport()
			}
		} catch (e: Exception) {
			ImportConfigResult(
				success = false,
				message = "Failed to import configuration: ${e.message}"
			)
		}
	}


	fun getRecipeById(id: Long) = dao.getRecipeById(id)
	fun getRecipesById(ids: Set<Long>) = dao.getRecipesById(ids)
	fun getReferencedRecipes(ids: Set<Long>) = dao.getReferencedRecipes(ids)
	fun getRecipeTitlesWithIds() = dao.getRecipeTitlesWithIds()
	fun getRecipeTitleById(id: Long) = dao.getRecipeTitleById(id)
	fun getRecipeDescriptions(query: String, orderedBy: Int, offset: Int, limit: Int, month: Int) = dao.getRecipeDescriptions(query, orderedBy, offset, limit, month)
	fun getKeywords(id: Long) = dao.getKeywords(id)
	fun getRecipeCount() = dao.getRecipeCount()
	fun getRecipeIds(query: String) = dao.getRecipeIds(query)
	fun getDependentRecipeIds(ids: Set<Long>) = dao.getDependentRecipeIds(ids)
	fun getAllCategories() = dao.getAllCategories()
	fun getAllCuisines() = dao.getAllCuisines()
	fun getAllKeywords() = dao.getAllKeywords()
	fun getCategories(query: String) = dao.getCategories(query)
	fun getCuisines(query: String) = dao.getCuisines(query)
	fun getKeywords(query: String) = dao.getKeywords(query)
	fun getSources() = dao.getSources()
	fun getYieldUnits() = dao.getYieldUnits()
	fun getIngredientItems() = dao.getIngredientItems()
	fun getIngredientUnits() = dao.getIngredientUnits()
	suspend fun getUniqueIngredientNames() = dao.getUniqueIngredientNames()
	suspend fun getPreferredUnitForIngredient(item: String) = dao.getPreferredUnitForIngredient(item)
	suspend fun deleteRecipesByIds(ids: Set<Long>) = dao.deleteRecipesByIds(ids)
	suspend fun deleteAllRecipes() = dao.deleteAllRecipes()
	suspend fun pinRecipe(recipePinEntity: RecipePinEntity) = dao.pinRecipe(recipePinEntity)
	suspend fun unpinRecipe(id: Long) = dao.unpinRecipe(id)

	// Synced mode: compares a list of recipes with the database
	suspend fun compareAndUpdateGourmandRecipes(recipes: List<RecipeWithIngredientsAndPreparations>) {
		Log.d(TAG, "Updating recipes...")
		// Remove recipes and ingredients not found in file
		dao.getDeprecatedRecipes(recipes.mapNotNull { it.recipe.gourmandId }).forEach {
			Log.d(TAG, "${it.recipe.title} was removed")
			dao.deleteRecipe(it.recipe)
		}

		// Update recipe properties
		recipes.forEach {
			if (it.recipe.gourmandId == null) {
				Log.e(TAG, "Recipe ${it.recipe.title} does not have a Gourmand id")
				return
			}

			val storedRecipe = dao.getRecipeByGourmandId(it.recipe.gourmandId)
			if (storedRecipe == null) {
				Log.d(TAG, "${it.recipe.title} is new")
				it.recipe.id = dao.insertRecipe(it.recipe)
			} else {
				it.recipe.id = storedRecipe.recipe.id
				if (storedRecipe.recipe != it.recipe) {
					// Recipe properties have changed
					Log.d(TAG, "${it.recipe.title} has changed")
					dao.updateRecipe(it.recipe)
				} else {
					Log.v(TAG, "${it.recipe.title} has not changed")
				}
			}
		}

		// Update ingredients
		recipes.forEach {
			if (it.recipe.gourmandId == null) return@forEach
			Log.d(TAG, "Storing ingredients of ${it.recipe.title}")

			// Update reference IDs
			it.ingredients.forEach { ingredient ->
				ingredient.recipeId = it.recipe.id
				ingredient.refId?.let { refId ->
					val newRef = dao.getRecipeIdByGourmandId(refId)
					ingredient.refId = newRef ?: throw Error("Error while saving ${it.recipe.title} to database: Referenced recipe not found")
				}
			}

			// Compare ingredients
			val storedIngredients = dao.getRecipeByGourmandId(it.recipe.gourmandId)?.ingredients
			if (storedIngredients != null) {
				storedIngredients.forEach { ing ->
					val new = it.ingredients.find { newIng ->
						ing.refId?.equals(newIng.refId) ?: ing.item.equals(newIng.item)
					}
					if (new == null) {
						Log.d(TAG, "${ing.refId ?: ing.item} was removed")
						dao.deleteIngredient(ing)
					} else {
						if (new != ing) {
							Log.d(TAG, "${ing.refId ?: ing.item} has changed")
							dao.updateIngredient(new)
						} else {
							Log.v(TAG, "${ing.refId ?: ing.item} has not changed")
						}
					}
				}
				Log.d(TAG, "New ingredients: ${it.ingredients.map { ing -> ing.refId ?: ing.item }.joinToString(", ")}")
				it.ingredients.forEach { ingredient -> dao.insertIngredient(ingredient) }
			}
		}
	}

	suspend fun upsertSingleRecipe(recipe: RecipeWithIngredientsAndPreparations): Long {
		return if (recipe.recipe.id == 0L) {
			dao.insertRecipe(recipe.recipe).also { id ->
				recipe.ingredients.forEach {
					it.recipeId = id
					dao.insertIngredient(it)
				}
				recipe.keywords.forEach {
					it.recipeId = id
					dao.insertKeyword(it)
				}
				recipe.preparations.forEach {
					it.recipeId = id
					dao.insertPreparationDate(it)
				}
			}
		}
		else {
			dao.updateRecipe(recipe.recipe)
			recipe.ingredients.forEach {
				dao.insertIngredient(it)
			}
			dao.deleteIngredientsNotInList(recipe.recipe.id, recipe.ingredients.map { it.position })
			recipe.keywords.forEach {
				dao.insertKeyword(it)
			}
			dao.deleteKeywordsNotInList(recipe.recipe.id, recipe.keywords.map { it.position })
			recipe.preparations.forEach {
				dao.insertPreparationDate(it)
			}
			dao.deletePreparationDatesNotInList(recipe.recipe.id, recipe.preparations.map { it.date.time })
			recipe.recipe.id
		}
	}

	// Standalone mode: saves recipes in the database
	@Transaction
	suspend fun insertRecipesWithIngredientsAndPreparations(recipes: List<RecipeWithIngredientsAndPreparations>): List<RecipeWithIngredientsAndPreparations> {

		// Stores recipe information except for the ingredients, retrieves the generated ID
		recipes.forEach {
			// Save previous id for json parsed recipes
			it.recipe.prevId = it.recipe.id.takeUnless { id -> id == 0L } ?: it.recipe.gourmandId?.toLong()
			it.recipe.id = 0L
			// Insert recipe in database and save id
			it.recipe.id = dao.insertRecipe(it.recipe)
		}

		// Stores the ingredients, replaces gourmand refIds with the correct new ones
		recipes.forEach {
			it.ingredients.forEach { ingredient ->
				ingredient.recipeId = it.recipe.id
				// For referenced recipes
				if (ingredient.refId != null) {
					ingredient.refId = recipes.find { rwi -> rwi.recipe.prevId == ingredient.refId }?.recipe?.id
						?: throw Error("Error while saving ${it.recipe.title} to database: Referenced recipe not found")
				}
			}
			it.ingredients.forEach { ingredient -> dao.insertIngredient(ingredient) }
			it.keywords.forEach { kw ->
				kw.recipeId = it.recipe.id
				dao.insertKeyword(kw)
			}
		}

		return recipes
	}

	suspend fun addPreparation(recipeId: Long, date: Date) {
		dao.getPreparation(recipeId, date.time)?.let {
			dao.updatePreparationDate(it.copy(count = it.count + 1))
		} ?: dao.insertPreparationDate(PreparationEntity(recipeId, date, 1))
	}

	suspend fun removePreparation(recipeId: Long, date: Date) {
		dao.getPreparation(recipeId, date.time)?.let {
			if (it.count > 1)
				dao.updatePreparationDate(it.copy(count = it.count - 1))
			else
				dao.deletePreparationDate(PreparationEntity(recipeId, date, 1))
		}
	}

	suspend fun getPreparations(recipeId: Long): List<PreparationEntity> {
		return dao.getPreparations(recipeId)
	}

	suspend fun getMostRecentPreparation(recipeId: Long): PreparationEntity? {
		return dao.getMostRecentPreparation(recipeId)
	}

	suspend fun getPreparationsByDateRange(recipeId: Long, startDate: Date, endDate: Date): List<PreparationEntity> {
		return dao.getPreparationsByDateRange(recipeId, startDate.time, endDate.time)
	}
}