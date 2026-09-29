package com.herohiman.tournant.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.net.Uri
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.herohiman.tournant.R
import com.herohiman.tournant.TournantApplication
import com.herohiman.tournant.cost.CostConfigBackupManager
import com.herohiman.tournant.cost.CostPrivacyManager
import com.herohiman.tournant.data.room.MasterIngredientEntity
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.safeInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MasterCostListActivity : AppCompatActivity() {

	private lateinit var repository: RecipeRepository
	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyStateContainer: View
	private lateinit var adapter: MasterCostAdapter
	private var ingredientsList = listOf<MasterIngredientEntity>()

	private val exportConfigLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
		if (uri != null) {
			exportConfigurationToUri(uri)
		}
	}

	private val importConfigLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
		if (uri != null) {
			importConfigurationFromUri(uri)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_master_cost_list)
		ViewGroupCompat.installCompatInsetsDispatch(window.decorView.rootView)

		repository = (application as TournantApplication).recipeRepository

		val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.setDisplayShowHomeEnabled(true)
		toolbar.setNavigationOnClickListener { finish() }

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinator_layout)) { view, windowInsets ->
			view.updatePadding(
				top = windowInsets.safeInsets().top,
				bottom = windowInsets.safeInsets().bottom,
				left = windowInsets.safeInsets().left,
				right = windowInsets.safeInsets().right
			)
			windowInsets
		}

		recyclerView = findViewById(R.id.recycler_master_costs)
		emptyStateContainer = findViewById(R.id.empty_state_container)
		val btnAutoFetch = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_auto_fetch)
		val fab = findViewById<FloatingActionButton>(R.id.fab_add_master_ingredient)

		btnAutoFetch.setOnClickListener {
			autoFetchIngredientsFromRecipes()
		}

		adapter = MasterCostAdapter(
			items = emptyList(),
			isPrivacyMode = CostPrivacyManager.isPrivacyModeEnabled(this),
			onEdit = { showAddEditDialog(it) },
			onToggleActive = { toggleIngredientActive(it) },
			onMerge = { showMergeDialog(it) },
			onViewSubRecipe = { openSubRecipe(it) },
			onViewUsage = { showUsageBottomSheet(it) }
		)
		recyclerView.layoutManager = LinearLayoutManager(this)
		recyclerView.adapter = adapter

		fab.setOnClickListener {
			showAddEditDialog(null)
		}

		loadMasterIngredients()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.menu_master_cost, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			android.R.id.home -> {
				finish()
				true
			}
			R.id.action_sync_ingredients -> {
				autoFetchIngredientsFromRecipes()
				true
			}
			R.id.action_toggle_privacy -> {
				val newPrivacyMode = CostPrivacyManager.togglePrivacyMode(this)
				adapter.setPrivacyMode(newPrivacyMode)
				val message = if (newPrivacyMode) {
					getString(R.string.privacy_mode_enabled)
				} else {
					getString(R.string.privacy_mode_disabled)
				}
				Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
				true
			}
			R.id.action_add_ingredient -> {
				showAddEditDialog(null)
				true
			}
			R.id.action_export_config -> {
				exportConfigLauncher.launch(CostConfigBackupManager.generateBackupFilename())
				true
			}
			R.id.action_import_config -> {
				importConfigLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun exportConfigurationToUri(uri: Uri) {
		lifecycleScope.launch {
			val success = withContext(Dispatchers.IO) {
				val payload = repository.exportCostConfiguration()
				CostConfigBackupManager.exportToUri(contentResolver, uri, payload)
			}
			if (success) {
				Toast.makeText(this@MasterCostListActivity, R.string.config_exported_success, Toast.LENGTH_SHORT).show()
			} else {
				Toast.makeText(this@MasterCostListActivity, R.string.config_export_failed, Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun importConfigurationFromUri(uri: Uri) {
		lifecycleScope.launch {
			val result = withContext(Dispatchers.IO) {
				val payload = CostConfigBackupManager.importFromUri(contentResolver, uri)
				if (payload == null) {
					null
				} else {
					repository.importCostConfiguration(payload)
				}
			}
			if (result == null) {
				Toast.makeText(this@MasterCostListActivity, R.string.config_import_invalid_file, Toast.LENGTH_SHORT).show()
			} else if (result.success) {
				val message = getString(
					R.string.config_imported_summary,
					result.importedMastersCount,
					result.importedUnitAliasesCount,
					result.importedIngredientAliasesCount
				)
				Toast.makeText(this@MasterCostListActivity, message, Toast.LENGTH_LONG).show()
				loadMasterIngredients()
			} else {
				val message = "${getString(R.string.config_import_failed)}: ${result.message}"
				Toast.makeText(this@MasterCostListActivity, message, Toast.LENGTH_LONG).show()
			}
		}
	}

	private fun autoFetchIngredientsFromRecipes() {
		lifecycleScope.launch {
			val count = withContext(Dispatchers.IO) {
				repository.syncIngredientsFromRecipes()
			}
			val message = if (count > 0) {
				getString(R.string.auto_fetch_summary, count)
			} else {
				getString(R.string.auto_fetch_no_new)
			}
			Toast.makeText(this@MasterCostListActivity, message, Toast.LENGTH_SHORT).show()
			if (count > 0) {
				loadMasterIngredients()
			}
		}
	}

	private fun loadMasterIngredients() {
		lifecycleScope.launch {
			val items = withContext(Dispatchers.IO) {
				repository.getAllMasterIngredientsList()
			}
			ingredientsList = items
			adapter.updateItems(items)
			emptyStateContainer.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
		}
	}

	private fun toggleIngredientActive(entity: MasterIngredientEntity) {
		lifecycleScope.launch {
			withContext(Dispatchers.IO) {
				if (entity.isActive) {
					repository.softDeleteMasterIngredient(entity.id)
				} else {
					repository.restoreMasterIngredient(entity.id)
				}
			}
			val actionName = if (entity.isActive) getString(R.string.soft_delete) else getString(R.string.restore)
			Toast.makeText(this@MasterCostListActivity, "$actionName: ${entity.name}", Toast.LENGTH_SHORT).show()
			loadMasterIngredients()
		}
	}

	private fun openSubRecipe(entity: MasterIngredientEntity) {
		val recipeId = entity.linkedRecipeId ?: return
		openRecipeById(recipeId)
	}

	private fun openRecipeById(recipeId: Long) {
		val intent = android.content.Intent(this, RecipeActivity::class.java).apply {
			putExtra("RECIPE_ID", recipeId)
		}
		startActivity(intent)
	}

	private fun showUsageBottomSheet(entity: MasterIngredientEntity) {
		lifecycleScope.launch {
			val recipes = withContext(Dispatchers.IO) {
				repository.getRecipesUsingMasterIngredient(entity.id, entity.name)
			}
			val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this@MasterCostListActivity)
			val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_ingredient_usage, null)
			bottomSheetDialog.setContentView(sheetView)

			val txtName = sheetView.findViewById<TextView>(R.id.txt_usage_ingredient_name)
			val txtCount = sheetView.findViewById<TextView>(R.id.txt_usage_count)
			val recycler = sheetView.findViewById<RecyclerView>(R.id.recycler_ingredient_usage)
			val emptyContainer = sheetView.findViewById<View>(R.id.empty_usage_container)

			txtName.text = entity.name
			if (recipes.isEmpty()) {
				txtCount.text = getString(R.string.recipe_usage_count_zero)
				txtCount.setTextColor(ContextCompat.getColor(this@MasterCostListActivity, R.color.subtitle_color))
				emptyContainer.visibility = View.VISIBLE
				recycler.visibility = View.GONE
			} else {
				val countText = if (recipes.size == 1) {
					getString(R.string.recipe_usage_count_one)
				} else {
					getString(R.string.recipe_usage_count, recipes.size)
				}
				txtCount.text = countText
				txtCount.setTextColor(ContextCompat.getColor(this@MasterCostListActivity, R.color.blue_text))
				emptyContainer.visibility = View.GONE
				recycler.visibility = View.VISIBLE

				recycler.layoutManager = LinearLayoutManager(this@MasterCostListActivity)
				recycler.adapter = IngredientUsageRecipeAdapter(recipes) { recipeId ->
					bottomSheetDialog.dismiss()
					openRecipeById(recipeId)
				}
			}

			bottomSheetDialog.show()
		}
	}

	private fun showAddEditDialog(existing: MasterIngredientEntity?) {
		lifecycleScope.launch {
			val titlesWithIds = withContext(Dispatchers.IO) {
				try {
					repository.getRecipeTitlesWithIds().first()
				} catch (e: Exception) {
					emptyList()
				}
			}
			try {
				showAddEditDialogInternal(existing, titlesWithIds)
			} catch (e: Exception) {
				android.util.Log.e("MasterCostList", "Error opening edit dialog", e)
				Toast.makeText(this@MasterCostListActivity, "Unable to open dialog: ${e.message}", Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun showAddEditDialogInternal(existing: MasterIngredientEntity?, titlesWithIds: List<com.herohiman.tournant.data.RecipeTitleId>) {
		val dialogBuilder = MaterialAlertDialogBuilder(this)
		val dialogView = LayoutInflater.from(dialogBuilder.context).inflate(R.layout.dialog_edit_master_ingredient, null)
		val editName = dialogView.findViewById<EditText>(R.id.edit_name)
		val editBaseUnit = dialogView.findViewById<EditText>(R.id.edit_base_unit)
		val editCategory = dialogView.findViewById<EditText>(R.id.edit_category)
		val layoutUnitCost = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.layout_unit_cost)
		val editUnitCost = dialogView.findViewById<EditText>(R.id.edit_unit_cost)
		val switchSubRecipe = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_sub_recipe)
		val layoutSubRecipeContainer = dialogView.findViewById<View>(R.id.layout_sub_recipe_container)
		val autoLinkedRecipe = dialogView.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.auto_linked_recipe)
		val editYieldRatio = dialogView.findViewById<EditText>(R.id.edit_yield_ratio)

		val recipeTitles = titlesWithIds.map { it.title }
		val recipeMap = titlesWithIds.associateBy { it.title.trim().lowercase(Locale.ROOT) }
		val recipeAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, recipeTitles)
		autoLinkedRecipe.setAdapter(recipeAdapter)
		autoLinkedRecipe.setOnClickListener { autoLinkedRecipe.showDropDown() }
		autoLinkedRecipe.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) autoLinkedRecipe.showDropDown() }

		var selectedRecipeId: Long? = existing?.linkedRecipeId

		autoLinkedRecipe.setOnItemClickListener { _, _, position, _ ->
			val selectedTitle = recipeAdapter.getItem(position)
			selectedRecipeId = titlesWithIds.firstOrNull { it.title == selectedTitle }?.id
		}

		if (existing != null) {
			editName.setText(existing.name)
			if (existing.unitCost > 0.0) {
				editUnitCost.setText(String.format(Locale.US, "%.4f", existing.unitCost).trimEnd('0').trimEnd('.'))
			}
			editBaseUnit.setText(existing.baseUnit)
			editCategory.setText(existing.category ?: "")

			if (existing.linkedRecipeId != null) {
				switchSubRecipe.isChecked = true
				layoutSubRecipeContainer.visibility = View.VISIBLE
				val matchedTitle = titlesWithIds.firstOrNull { it.id == existing.linkedRecipeId }?.title
				if (!matchedTitle.isNullOrBlank()) {
					autoLinkedRecipe.setText(matchedTitle, false)
				}
				if (existing.yieldRatio != null) {
					editYieldRatio.setText(String.format(Locale.US, "%.4f", existing.yieldRatio!!).trimEnd('0').trimEnd('.'))
				}
				layoutUnitCost.hint = getString(R.string.unit_cost_optional_fallback)
			} else {
				switchSubRecipe.isChecked = false
				layoutSubRecipeContainer.visibility = View.GONE
				layoutUnitCost.hint = getString(R.string.unit_cost)
			}
		}

		switchSubRecipe.setOnCheckedChangeListener { _, isChecked ->
			layoutSubRecipeContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
			layoutUnitCost.hint = if (isChecked) getString(R.string.unit_cost_optional_fallback) else getString(R.string.unit_cost)
		}

		val titleRes = if (existing == null) R.string.add_ingredient_cost else R.string.edit_ingredient_cost

		dialogBuilder
			.setTitle(titleRes)
			.setView(dialogView)
			.setPositiveButton(R.string.save) { _, _ ->
				val name = editName.text.toString().trim()
				val baseUnit = editBaseUnit.text.toString().trim()
				val category = editCategory.text.toString().trim().ifBlank { null }
				val isSubRecipe = switchSubRecipe.isChecked
				val unitCostStr = editUnitCost.text.toString().trim()
				val yieldRatioStr = editYieldRatio.text.toString().trim()

				if (name.isBlank() || baseUnit.isBlank()) {
					Toast.makeText(this, "Name and base unit are required", Toast.LENGTH_SHORT).show()
					return@setPositiveButton
				}

				if (!isSubRecipe && unitCostStr.isBlank()) {
					Toast.makeText(this, "Unit cost is required for manual ingredients", Toast.LENGTH_SHORT).show()
					return@setPositiveButton
				}

				val linkedRecipe = if (isSubRecipe) {
					selectedRecipeId ?: recipeMap[autoLinkedRecipe.text.toString().trim().lowercase(Locale.ROOT)]?.id
				} else null

				if (isSubRecipe && linkedRecipe == null) {
					Toast.makeText(this, R.string.select_sub_recipe_error, Toast.LENGTH_SHORT).show()
					return@setPositiveButton
				}

				val yieldRatio = if (isSubRecipe) yieldRatioStr.toDoubleOrNull() else null
				val cost = unitCostStr.toDoubleOrNull() ?: 0.0

				lifecycleScope.launch {
					withContext(Dispatchers.IO) {
						if (existing != null) {
							val updated = existing.copy(
								name = name,
								unitCost = cost,
								baseUnit = baseUnit,
								category = category,
								linkedRecipeId = linkedRecipe,
								yieldRatio = yieldRatio,
								lastUpdated = System.currentTimeMillis()
							)
							repository.updateMasterIngredient(updated)
						} else {
							val newEntity = MasterIngredientEntity(
								name = name,
								unitCost = cost,
								baseUnit = baseUnit,
								category = category,
								linkedRecipeId = linkedRecipe,
								yieldRatio = yieldRatio,
								isActive = true,
								lastUpdated = System.currentTimeMillis()
							)
							repository.insertMasterIngredient(newEntity)
						}
					}
					Toast.makeText(this@MasterCostListActivity, R.string.cost_saved, Toast.LENGTH_SHORT).show()
					loadMasterIngredients()
				}
			}
			.setNegativeButton(R.string.cancel, null)
			.show()
	}

	private fun showMergeDialog(source: MasterIngredientEntity) {
		val candidates = ingredientsList.filter { it.id != source.id && it.isActive }
		if (candidates.isEmpty()) {
			Toast.makeText(this, R.string.no_other_ingredients_to_merge, Toast.LENGTH_SHORT).show()
			return
		}

		val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_merge_ingredient, null)
		val textDesc = dialogView.findViewById<TextView>(R.id.text_merge_description)
		val autoTarget = dialogView.findViewById<androidx.appcompat.widget.AppCompatAutoCompleteTextView>(R.id.auto_target_ingredient)

		textDesc.text = getString(R.string.merge_description, source.name)

		val candidateNames = candidates.map { it.name }
		val candidateMap = candidates.associateBy { it.name.trim().lowercase() }
		val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, candidateNames)
		autoTarget.setAdapter(adapter)

		autoTarget.setOnClickListener { autoTarget.showDropDown() }
		autoTarget.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) autoTarget.showDropDown() }

		MaterialAlertDialogBuilder(this)
			.setTitle(R.string.merge_ingredient)
			.setView(dialogView)
			.setPositiveButton(R.string.merge) { _, _ ->
				val selectedText = autoTarget.text.toString().trim()
				val target = candidateMap[selectedText.lowercase()]
				if (target == null) {
					Toast.makeText(this, R.string.invalid_target_ingredient, Toast.LENGTH_SHORT).show()
					return@setPositiveButton
				}

				lifecycleScope.launch {
					val result = withContext(Dispatchers.IO) {
						com.herohiman.tournant.cost.MergeIngredientUseCase(repository).merge(source.id, target.id)
					}
					Toast.makeText(this@MasterCostListActivity, result.message, Toast.LENGTH_SHORT).show()
					if (result.success) {
						loadMasterIngredients()
					}
				}
			}
			.setNegativeButton(R.string.cancel, null)
			.show()
	}

	class MasterCostAdapter(
		private var items: List<MasterIngredientEntity>,
		private var isPrivacyMode: Boolean,
		private val onEdit: (MasterIngredientEntity) -> Unit,
		private val onToggleActive: (MasterIngredientEntity) -> Unit,
		private val onMerge: (MasterIngredientEntity) -> Unit,
		private val onViewSubRecipe: (MasterIngredientEntity) -> Unit,
		private val onViewUsage: (MasterIngredientEntity) -> Unit
	) : RecyclerView.Adapter<MasterCostAdapter.ViewHolder>() {

		class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val nameText: TextView = view.findViewById(R.id.ingredient_name)
			val costText: TextView = view.findViewById(R.id.ingredient_cost)
			val statusText: TextView = view.findViewById(R.id.ingredient_status)
			val categoryText: TextView = view.findViewById(R.id.ingredient_category)
			val subRecipeBadge: TextView = view.findViewById(R.id.ingredient_sub_recipe_badge)
			val subRecipeBtn: ImageButton = view.findViewById(R.id.btn_sub_recipe)
			val usageBtn: ImageButton = view.findViewById(R.id.btn_usage)
			val mergeBtn: ImageButton = view.findViewById(R.id.btn_merge)
			val editBtn: ImageButton = view.findViewById(R.id.btn_edit)
			val deleteRestoreBtn: ImageButton = view.findViewById(R.id.btn_delete_restore)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = LayoutInflater.from(parent.context).inflate(R.layout.item_master_ingredient, parent, false)
			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val item = items[position]
			val context = holder.itemView.context

			holder.nameText.text = item.name

			val isDerived = item.linkedRecipeId != null
			if (isDerived) {
				holder.subRecipeBadge.visibility = View.VISIBLE
				holder.subRecipeBadge.text = context.getString(R.string.sub_recipe_badge)
				holder.subRecipeBtn.visibility = View.VISIBLE
				holder.subRecipeBtn.setOnClickListener { onViewSubRecipe(item) }
			} else {
				holder.subRecipeBadge.visibility = View.GONE
				holder.subRecipeBtn.visibility = View.GONE
			}

			val costFormatted = if (isPrivacyMode) {
				"•••• / ${item.baseUnit}"
			} else if (isDerived) {
				if (item.unitCost > 0.0) {
					String.format(Locale.US, "$%.4f / %s (Derived)", item.unitCost, item.baseUnit)
				} else {
					String.format(Locale.US, "%s (Derived)", item.baseUnit)
				}
			} else {
				String.format(Locale.US, "$%.4f / %s", item.unitCost, item.baseUnit)
			}
			holder.costText.text = costFormatted

			if (item.isActive) {
				holder.statusText.text = context.getString(R.string.active)
				holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.blue_text))
				holder.deleteRestoreBtn.setImageResource(R.drawable.ic_delete)
				holder.deleteRestoreBtn.contentDescription = context.getString(R.string.soft_delete)
				holder.itemView.alpha = 1.0f
			} else {
				holder.statusText.text = context.getString(R.string.inactive)
				holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.subtitle_color))
				holder.deleteRestoreBtn.setImageResource(R.drawable.ic_checked)
				holder.deleteRestoreBtn.contentDescription = context.getString(R.string.restore)
				holder.itemView.alpha = 0.6f
			}

			if (!item.category.isNullOrBlank()) {
				holder.categoryText.text = item.category
				holder.categoryText.visibility = View.VISIBLE
			} else {
				holder.categoryText.visibility = View.GONE
			}

			holder.usageBtn.setOnClickListener { onViewUsage(item) }
			holder.mergeBtn.setOnClickListener { onMerge(item) }
			holder.editBtn.setOnClickListener { onEdit(item) }
			holder.deleteRestoreBtn.setOnClickListener { onToggleActive(item) }
		}

		override fun getItemCount(): Int = items.size

		fun updateItems(newItems: List<MasterIngredientEntity>) {
			items = newItems
			notifyDataSetChanged()
		}

		fun setPrivacyMode(privacyMode: Boolean) {
			isPrivacyMode = privacyMode
			notifyDataSetChanged()
		}
	}

	class IngredientUsageRecipeAdapter(
		private val items: List<com.herohiman.tournant.data.RecipeTitleId>,
		private val onClick: (Long) -> Unit
	) : RecyclerView.Adapter<IngredientUsageRecipeAdapter.ViewHolder>() {

		class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val titleText: TextView = view.findViewById(R.id.txt_recipe_title)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ingredient_usage_recipe, parent, false)
			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val item = items[position]
			holder.titleText.text = item.title
			holder.itemView.setOnClickListener { onClick(item.id) }
		}

		override fun getItemCount(): Int = items.size
	}
}
