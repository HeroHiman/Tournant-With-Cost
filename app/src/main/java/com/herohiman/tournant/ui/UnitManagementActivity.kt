package com.herohiman.tournant.ui

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.herohiman.tournant.R
import com.herohiman.tournant.TournantApplication
import com.herohiman.tournant.cost.CostConfigBackupManager
import com.herohiman.tournant.cost.ImportConfigResult
import com.herohiman.tournant.data.room.BaseUnitType
import com.herohiman.tournant.data.room.RecipeRepository
import com.herohiman.tournant.data.room.UnitAliasDao
import com.herohiman.tournant.data.room.UnitAliasEntity
import com.herohiman.tournant.safeInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class UnitManagementActivity : AppCompatActivity() {

	private lateinit var repository: RecipeRepository
	private lateinit var viewModel: UnitManagementViewModel
	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyStateContainer: View
	private lateinit var chipGroupFilters: ChipGroup
	private lateinit var adapter: UnitAliasAdapter

	private var allAliases: List<UnitAliasEntity> = emptyList()
	private var currentSearchQuery: String = ""
	private var selectedCategoryFilter: BaseUnitType? = null

	private val exportUnitsLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
		if (uri != null) {
			exportUnitsToUri(uri)
		}
	}

	private val importUnitsLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
		if (uri != null) {
			importUnitsFromUri(uri)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_unit_management)

		repository = (application as TournantApplication).recipeRepository
		val factory = UnitManagementViewModelFactory(repository)
		viewModel = ViewModelProvider(this, factory)[UnitManagementViewModel::class.java]

		enableEdgeToEdge()
		ViewGroupCompat.installCompatInsetsDispatch(window.decorView.rootView)

		val toolbar = findViewById<Toolbar>(R.id.toolbar)
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

		@Suppress("DEPRECATION")
		if (Build.VERSION.SDK_INT < 35) {
			window.navigationBarColor = ContextCompat.getColor(this, R.color.bar_color)
		}

		recyclerView = findViewById(R.id.recycler_units)
		emptyStateContainer = findViewById(R.id.empty_state_container)
		chipGroupFilters = findViewById(R.id.chip_group_filters)
		val fab = findViewById<FloatingActionButton>(R.id.fab_add_unit_alias)
		val btnRestoreDefaults = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_restore_defaults)

		adapter = UnitAliasAdapter(
			items = emptyList(),
			onEdit = { showAddEditDialog(it) },
			onDelete = { showDeleteConfirmationDialog(it) }
		)
		recyclerView.layoutManager = LinearLayoutManager(this)
		recyclerView.adapter = adapter

		fab.setOnClickListener {
			showAddEditDialog(null)
		}

		btnRestoreDefaults.setOnClickListener {
			restoreDefaultUnits()
		}

		chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
			selectedCategoryFilter = when {
				checkedIds.contains(R.id.chip_mass) -> BaseUnitType.KG
				checkedIds.contains(R.id.chip_volume) -> BaseUnitType.LITER
				checkedIds.contains(R.id.chip_count) -> BaseUnitType.COUNT
				else -> null
			}
			viewModel.onCategoryFilterChanged(selectedCategoryFilter)
		}

		lifecycleScope.launch {
			viewModel.unitAliases.collect { filtered ->
				allAliases = filtered
				adapter.updateData(filtered)
				if (filtered.isEmpty()) {
					emptyStateContainer.visibility = View.VISIBLE
					recyclerView.visibility = View.GONE
				} else {
					emptyStateContainer.visibility = View.GONE
					recyclerView.visibility = View.VISIBLE
				}
			}
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.menu_unit_management, menu)

		val searchItem = menu.findItem(R.id.action_search_units)
		val searchView = searchItem?.actionView as? SearchView
		searchView?.queryHint = getString(R.string.search_units)
		searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
			override fun onQueryTextSubmit(query: String?): Boolean = false
			override fun onQueryTextChange(newText: String?): Boolean {
				currentSearchQuery = newText?.trim() ?: ""
				viewModel.onSearchQueryChanged(currentSearchQuery)
				return true
			}
		})

		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			android.R.id.home -> {
				finish()
				true
			}
			R.id.action_add_unit -> {
				showAddEditDialog(null)
				true
			}
			R.id.action_export_units -> {
				exportUnitsLauncher.launch(CostConfigBackupManager.generateUnitBackupFilename())
				true
			}
			R.id.action_import_units -> {
				importUnitsLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
				true
			}
			R.id.action_restore_defaults -> {
				MaterialAlertDialogBuilder(this)
					.setTitle(R.string.reset_default_units)
					.setMessage(R.string.reset_default_units_confirm)
					.setPositiveButton(R.string.ok) { _, _ ->
						restoreDefaultUnits()
					}
					.setNegativeButton(R.string.cancel, null)
					.show()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun exportUnitsToUri(uri: Uri) {
		lifecycleScope.launch {
			val success = withContext(Dispatchers.IO) {
				val payload = repository.exportUnitConfiguration()
				CostConfigBackupManager.exportToUri(contentResolver, uri, payload)
			}
			if (success) {
				Toast.makeText(this@UnitManagementActivity, R.string.units_exported_success, Toast.LENGTH_SHORT).show()
			} else {
				Toast.makeText(this@UnitManagementActivity, R.string.units_export_failed, Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun importUnitsFromUri(uri: Uri) {
		lifecycleScope.launch {
			val result = withContext(Dispatchers.IO) {
				val payload = CostConfigBackupManager.importFromUri(contentResolver, uri)
				if (payload == null) {
					null
				} else if (payload.unitAliases.isEmpty()) {
					ImportConfigResult(success = false, message = getString(R.string.no_units_in_backup))
				} else {
					repository.importCostConfiguration(payload)
				}
			}
			if (result == null) {
				Toast.makeText(this@UnitManagementActivity, R.string.units_import_invalid_file, Toast.LENGTH_SHORT).show()
			} else if (result.success) {
				val message = getString(R.string.units_imported_summary, result.importedUnitAliasesCount)
				Toast.makeText(this@UnitManagementActivity, message, Toast.LENGTH_SHORT).show()
				loadUnitAliases()
			} else {
				val message = "${getString(R.string.units_import_failed)}: ${result.message}"
				Toast.makeText(this@UnitManagementActivity, message, Toast.LENGTH_LONG).show()
			}
		}
	}

	private fun loadUnitAliases() {
		lifecycleScope.launch {
			val aliases = withContext(Dispatchers.IO) {
				repository.getAllUnitAliasesList()
			}
			allAliases = aliases
			applyFilters()
		}
	}

	private fun applyFilters() {
		val filtered = allAliases.filter { entity ->
			val matchesCategory = selectedCategoryFilter == null || entity.baseUnit == selectedCategoryFilter
			val matchesQuery = currentSearchQuery.isBlank() ||
					entity.aliasName.contains(currentSearchQuery, ignoreCase = true) ||
					entity.baseUnit.name.contains(currentSearchQuery, ignoreCase = true)
			matchesCategory && matchesQuery
		}

		adapter.updateData(filtered)
		if (filtered.isEmpty()) {
			emptyStateContainer.visibility = View.VISIBLE
			recyclerView.visibility = View.GONE
		} else {
			emptyStateContainer.visibility = View.GONE
			recyclerView.visibility = View.VISIBLE
		}
	}

	private fun restoreDefaultUnits() {
		lifecycleScope.launch {
			viewModel.restoreDefaultUnits()
			Toast.makeText(this@UnitManagementActivity, R.string.units_restored, Toast.LENGTH_SHORT).show()
		}
	}

	private fun showDeleteConfirmationDialog(entity: UnitAliasEntity) {
		MaterialAlertDialogBuilder(this)
			.setTitle(R.string.delete_unit_alias)
			.setMessage(getString(R.string.delete_unit_alias_confirm, entity.aliasName))
			.setPositiveButton(R.string.delete) { _, _ ->
				lifecycleScope.launch {
					viewModel.deleteUnitAlias(entity)
					Toast.makeText(this@UnitManagementActivity, R.string.unit_alias_deleted, Toast.LENGTH_SHORT).show()
				}
			}
			.setNegativeButton(R.string.cancel, null)
			.show()
	}

	private fun showAddEditDialog(existing: UnitAliasEntity?) {
		val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_unit_alias, null)
		val editAliasName = dialogView.findViewById<TextInputEditText>(R.id.edit_alias_name)
		val radioGroupCategory = dialogView.findViewById<RadioGroup>(R.id.radio_group_base_category)
		val editFactor = dialogView.findViewById<TextInputEditText>(R.id.edit_conversion_factor)
		val txtPreview = dialogView.findViewById<TextView>(R.id.txt_formula_preview)

		var selectedCategory = existing?.baseUnit ?: BaseUnitType.KG

		fun updateFormulaPreview() {
			val alias = editAliasName.text.toString().trim().ifBlank { "unit" }
			val factorText = editFactor.text.toString().trim().ifBlank { "1.0" }
			val categoryUnit = when (selectedCategory) {
				BaseUnitType.KG -> "kg"
				BaseUnitType.LITER -> "liter"
				BaseUnitType.COUNT -> "units"
			}
			txtPreview.text = getString(R.string.unit_formula_preview, alias, factorText, categoryUnit)
		}

		if (existing != null) {
			editAliasName.setText(existing.aliasName)
			editFactor.setText(formatFactor(existing.conversionFactor))
			when (existing.baseUnit) {
				BaseUnitType.KG -> radioGroupCategory.check(R.id.radio_category_mass)
				BaseUnitType.LITER -> radioGroupCategory.check(R.id.radio_category_volume)
				BaseUnitType.COUNT -> radioGroupCategory.check(R.id.radio_category_count)
			}
		} else {
			editFactor.setText("1.0")
			radioGroupCategory.check(R.id.radio_category_mass)
		}
		updateFormulaPreview()

		radioGroupCategory.setOnCheckedChangeListener { _, checkedId ->
			selectedCategory = when (checkedId) {
				R.id.radio_category_volume -> BaseUnitType.LITER
				R.id.radio_category_count -> BaseUnitType.COUNT
				else -> BaseUnitType.KG
			}
			updateFormulaPreview()
		}

		val watcher = object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				updateFormulaPreview()
			}
			override fun afterTextChanged(s: Editable?) {}
		}

		editAliasName.addTextChangedListener(watcher)
		editFactor.addTextChangedListener(watcher)

		val dialogTitle = if (existing == null) R.string.add_unit_alias else R.string.edit_unit_alias

		MaterialAlertDialogBuilder(this)
			.setTitle(dialogTitle)
			.setView(dialogView)
			.setPositiveButton(R.string.save) { _, _ ->
				val aliasName = editAliasName.text.toString().trim()
				val factor = editFactor.text.toString().toDoubleOrNull()

				if (aliasName.isBlank()) {
					Toast.makeText(this, "Unit alias name cannot be empty", Toast.LENGTH_SHORT).show()
					return@setPositiveButton
				}
				if (factor == null || factor <= 0.0) {
					Toast.makeText(this, "Multiplier factor must be greater than zero", Toast.LENGTH_SHORT).show()
					return@setPositiveButton
				}

				lifecycleScope.launch {
					if (existing == null) {
						viewModel.insertOrUpdateUnitAlias(
							UnitAliasEntity(
								aliasName = aliasName,
								baseUnit = selectedCategory,
								conversionFactor = factor
							)
						)
					} else {
						viewModel.insertOrUpdateUnitAlias(
							existing.copy(
								aliasName = aliasName,
								baseUnit = selectedCategory,
								conversionFactor = factor
							)
						)
					}
					Toast.makeText(this@UnitManagementActivity, R.string.unit_alias_saved, Toast.LENGTH_SHORT).show()
				}
			}
			.setNegativeButton(R.string.cancel, null)
			.show()
	}

	private fun formatFactor(factor: Double): String {
		return if (factor == factor.toLong().toDouble()) {
			factor.toLong().toString()
		} else {
			String.format(Locale.US, "%.6f", factor).trimEnd('0').trimEnd('.')
		}
	}

	class UnitAliasAdapter(
		private var items: List<UnitAliasEntity>,
		private val onEdit: (UnitAliasEntity) -> Unit,
		private val onDelete: (UnitAliasEntity) -> Unit
	) : RecyclerView.Adapter<UnitAliasAdapter.ViewHolder>() {

		class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val nameText: TextView = view.findViewById(R.id.unit_alias_name)
			val formulaText: TextView = view.findViewById(R.id.unit_formula)
			val categoryBadge: TextView = view.findViewById(R.id.unit_category_badge)
			val editBtn: ImageButton = view.findViewById(R.id.btn_edit_unit)
			val deleteBtn: ImageButton = view.findViewById(R.id.btn_delete_unit)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = LayoutInflater.from(parent.context).inflate(R.layout.item_unit_alias, parent, false)
			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val item = items[position]
			val context = holder.itemView.context

			holder.nameText.text = item.aliasName

			val categoryUnit = when (item.baseUnit) {
				BaseUnitType.KG -> "kg"
				BaseUnitType.LITER -> "liter"
				BaseUnitType.COUNT -> "units"
			}

			val factorStr = if (item.conversionFactor == item.conversionFactor.toLong().toDouble()) {
				item.conversionFactor.toLong().toString()
			} else {
				String.format(Locale.US, "%.6f", item.conversionFactor).trimEnd('0').trimEnd('.')
			}

			holder.formulaText.text = context.getString(R.string.unit_formula_preview, item.aliasName, factorStr, categoryUnit)

			holder.categoryBadge.text = when (item.baseUnit) {
				BaseUnitType.KG -> context.getString(R.string.category_mass)
				BaseUnitType.LITER -> context.getString(R.string.category_volume)
				BaseUnitType.COUNT -> context.getString(R.string.category_count)
			}

			holder.itemView.setOnClickListener { onEdit(item) }
			holder.editBtn.setOnClickListener { onEdit(item) }
			holder.deleteBtn.setOnClickListener { onDelete(item) }
		}

		override fun getItemCount(): Int = items.size

		fun updateData(newItems: List<UnitAliasEntity>) {
			items = newItems
			notifyDataSetChanged()
		}
	}
}
