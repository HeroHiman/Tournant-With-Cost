package com.herohiman.tournant.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.text.InputFilter
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.format.DateUtils.DAY_IN_MILLIS
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.herohiman.tournant.cost.IngredientCostItem
import com.herohiman.tournant.cost.RecipeCostBreakdown
import com.herohiman.tournant.cost.YieldParser
import com.herohiman.tournant.cost.CostCurrencyFormatter
import com.herohiman.tournant.data.room.MasterIngredientEntity
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Scale
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.text.parseAsHtml
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.load
import coil3.request.addLastModifiedToFileCacheKey
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.herohiman.tournant.BuildConfig
import com.herohiman.tournant.Constants.Companion.MODE_SYNCED
import com.herohiman.tournant.Constants.Companion.PREF_MARKDOWN
import com.herohiman.tournant.Constants.Companion.PREF_MODE
import com.herohiman.tournant.Constants.Companion.PREF_SCREEN_ON
import com.herohiman.tournant.R
import com.herohiman.tournant.TournantApplication
import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.IngredientLine
import com.herohiman.tournant.data.IngredientLine.IngredientGroupTitle
import com.herohiman.tournant.data.IngredientLine.IngredientItem
import com.herohiman.tournant.data.Recipe
import com.herohiman.tournant.databinding.ActivityRecipeBinding
import com.herohiman.tournant.databinding.InputFieldTimeBinding
import com.herohiman.tournant.databinding.RecyclerPreparationsBinding
import com.herohiman.tournant.getAppOrSystemLocale
import com.herohiman.tournant.getQuantityIntForPlurals
import com.herohiman.tournant.safeInsets
import com.herohiman.tournant.separator
import com.herohiman.tournant.shiftToLocalDayStart
import com.herohiman.tournant.splitLines
import com.herohiman.tournant.toStringForCooks
import com.herohiman.tournant.ui.adapter.InstructionsTextAdapter
import com.herohiman.tournant.ui.adapter.PreparationsAdapter
import com.herohiman.tournant.ui.elements.TournantRoundIconButton
import com.herohiman.tournant.ui.elements.TournantUnderlinedTextField
import com.herohiman.tournant.utils.RecipeMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.html.HtmlPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RecipeActivity : AppCompatActivity(), InstructionsTextAdapter.InstructionsTextInterface, PreparationsAdapter.PreparationsInterface {

	companion object {
		private const val TAG = "RecipeActivity"
	}

	private lateinit var binding: ActivityRecipeBinding
	private val viewModel: RecipeViewModel by viewModels {
		RecipeViewModelFactory(
			application as TournantApplication,
			intent.getLongExtra("RECIPE_ID", 0L)
		)
	}

	private val markwon: Markwon? by lazy {
		if (getSharedPreferences(packageName + "_preferences", MODE_PRIVATE).getBoolean(PREF_MARKDOWN, true)) {
			Markwon.builder(this)
				.usePlugin(HtmlPlugin.create())
				.usePlugin(RecipeMarkwonPlugin(this))
				.usePlugin(SoftBreakAddsNewLinePlugin.create())
				.build()
		}
		else null
	}

	@SuppressLint("SetTextI18n")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		if (!intent.hasExtra("RECIPE_ID")) {
			Log.e(TAG, "No recipe provided")
			finish()
			return
		}

		binding = ActivityRecipeBinding.inflate(layoutInflater)

		enableEdgeToEdge()
		ViewGroupCompat.installCompatInsetsDispatch(window.decorView.rootView)

		ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
			Log.d(TAG, "setOnApplyWindowInsetsListener(content)")
			view.updateLayoutParams<MarginLayoutParams> {
				topMargin = windowInsets.safeInsets().top
				bottomMargin = windowInsets.safeInsets().bottom
			}
			view.updatePadding(
				left = windowInsets.safeInsets().left,
				right = windowInsets.safeInsets().right,
			)
			WindowInsetsCompat.CONSUMED
		}

		@Suppress("DEPRECATION")
		if (Build.VERSION.SDK_INT < 35) {
			window.navigationBarColor = ContextCompat.getColor(this, R.color.bar_color)
		}

		setContentView(binding.root)

		supportActionBar?.apply {
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(true)
		}

		if (getSharedPreferences(packageName + "_preferences", MODE_PRIVATE).getBoolean(PREF_SCREEN_ON, true))
			window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		if (resources.displayMetrics.run { widthPixels / density } > 600) {
			binding.recipeDetailImageDrawable.apply {
				layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
				scaleType = ImageView.ScaleType.CENTER_CROP
			}
		}

		binding.recipeDetailPreptime.updateLayoutParams<LinearLayout.LayoutParams> {
			weight = getString(R.string.preptime).length.toFloat()
		}

		binding.recipeDetailCooktime.updateLayoutParams<LinearLayout.LayoutParams> {
			weight = getString(R.string.cooktime).length.toFloat()
		}

		val keywords = viewModel.recipe.map { it.keywords }
		@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
		binding.recipeDetailKeywords.setContent {
			CompositionLocalProvider(LocalRippleConfiguration provides null) {
				FlowRow(
					horizontalArrangement = Arrangement.spacedBy(8.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					keywords.collectAsState(emptyList()).value.forEach {
						Chip(
							modifier = Modifier.height(24.dp),
							onClick = {},
							colors = ChipDefaults.chipColors(backgroundColor = materialColors100.getRandom(it)),
							border = BorderStroke(2.dp, materialColors200.getRandom(it)),
							shape = RoundedCornerShape(4.dp)
						) {
							Text(text = it, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 24.dp))
						}
					}
				}
			}
		}

		val months = viewModel.recipe.map { it.season?.getIncludedMonths() }
		binding.recipeDetailSeason.setContent {
			months.collectAsState(null).value?.let { months ->
				TournantTheme {
					Surface {
						Column {
							Text(
								stringResource(R.string.season),
								color = colorResource(R.color.heading_color),
								fontFamily = FontFamily(Font(R.font.quicksand_bold)),
								fontSize = 18.sp
							)

							val currentMonth = Calendar.getInstance().get(Calendar.MONTH)

							val monthNamesThreeLetters = Calendar.getInstance().run {
								(0..11).map {
									set(Calendar.MONTH, it)
									getDisplayName(
										Calendar.MONTH,
										Calendar.SHORT,
										getAppOrSystemLocale()
									)
									?.take(3) ?: ""
								}
							}

							val maxSpaceThreeLetters = TextMeasurer(
								LocalFontFamilyResolver.current,
								LocalDensity.current,
								LocalLayoutDirection.current
							).run {
								monthNamesThreeLetters.maxOf {
									measure(it, LocalTextStyle.current.copy(fontSize = 12.sp)).size.width
								} + 2 * resources.displayMetrics.density
							}

							val availableSpace = remember { mutableIntStateOf(0) }

							val useThreeLetters = remember { derivedStateOf { maxSpaceThreeLetters * 12 <= availableSpace.intValue } }

							Row(
								Modifier
									.fillMaxWidth()
									.onGloballyPositioned {
										availableSpace.intValue = it.size.width
									}
							) {
								monthNamesThreeLetters.forEachIndexed { i, monthName ->
									Column(
										Modifier.weight(1f),
										horizontalAlignment = Alignment.CenterHorizontally
									) {
										Box(
											Modifier
												.padding(bottom = 2.dp)
												.alpha(if (i in months) 1f else .3f)
										) {
											Text(
												text = when {
													useThreeLetters.value -> monthName
													monthName.first().isDigit() -> monthName.takeWhile { it.isDigit() }
													else -> monthName.take(1)
												},
												fontSize = 12.sp
											)
											if (i == currentMonth)
												Box(
													Modifier
														.size(4.dp)
														.align(Alignment.BottomCenter)
														.clip(CircleShape)
														.background(if (i in months) MaterialTheme.colors.primary else Color.Gray.copy(.3f))
												)
										}
										if (i in months) {
											Box(
												Modifier
													.height(4.dp)
													.fillMaxWidth()
													.clip(
														RoundedCornerShape(
															topStartPercent = if (i - 1 !in months) 50 else 0,
															bottomStartPercent = if (i - 1 !in months) 50 else 0,
															topEndPercent = if (i + 1 !in months) 50 else 0,
															bottomEndPercent = if (i + 1 !in months) 50 else 0
														)
													)
													.background(materialColors700[(i + 5) % 14])
											)
										}
									}
								}
							}
						}
					}
				}
			}
		}

		lifecycleScope.launch {
			viewModel.recipe.collectLatest { recipe ->
				binding.recipe = recipe
				title = recipe.title
				val lang = if (Build.VERSION.SDK_INT >= 26)
					Locale.lookupTag(Locale.LanguageRange.parse(recipe.language.toLanguageTag() + ";q=1.0"), getString(R.string.availableLanguages).split(",")) ?: recipe.language.toLanguageTag()
				else recipe.language.toLanguageTag()
				val timeStrings = getString(R.string.localisedTimeStrings).split(";").find { it.substringBefore(":") == lang }?.split(":") ?: List(5) { "" }
				val dashWords = timeStrings[1].ifEmpty { getString(R.string.to) }
				val hString = timeStrings[2].ifEmpty { getString(R.string.hours_for_regex) }
				val minString = timeStrings[3].ifEmpty { getString(R.string.minutes_for_regex) }
				val sString = timeStrings[4].ifEmpty { getString(R.string.seconds_for_regex) }
				binding.recipeDetailImage.visibility = recipe.image.let { image ->
					val imageFile = File(File(application.filesDir, "images"), "${recipe.id}.jpg")
					if (imageFile.exists()) {
						binding.recipeDetailImageDrawable.load(File(File(application.filesDir, "images"), "${recipe.id}.jpg")) {
							addLastModifiedToFileCacheKey(true)
						}
						View.VISIBLE
					}
					else if (image != null) {
						binding.recipeDetailImageDrawable.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.size))
						View.VISIBLE
					}
					else {
						View.GONE
					}
				}
				recipe.category?.let {
					binding.recipeDetailCategory.chipBackgroundColor = ColorStateList.valueOf(materialColors700.getRandom(it).toArgb())
				}
				recipe.cuisine?.let {
					binding.recipeDetailCuisine.chipBackgroundColor = ColorStateList.valueOf(materialColors900.getRandom(it).toArgb())
				}
				recipe.instructions?.let {
					binding.recipeDetailInstructions.visibility = View.VISIBLE
					binding.recipeDetailInstructionsRecycler.adapter = InstructionsTextAdapter(
						this@RecipeActivity,
						parseRecipeText(it).splitLines(),
						dashWords = dashWords,
						hString = hString,
						minString = minString,
						sString = sString
					)
				}
				recipe.notes?.let {
					binding.recipeDetailNotes.visibility = View.VISIBLE
					binding.recipeDetailNotesText.movementMethod = LinkMovementMethod.getInstance()
					binding.recipeDetailNotesText.text = parseRecipeText(it)
				}
				if (intent.hasExtra("RECIPE_YIELD_AMOUNT")) {
					val requestedYieldAmount = intent.getDoubleExtra("RECIPE_YIELD_AMOUNT", 0.0)
					val requestedYieldUnit = intent.getStringExtra("RECIPE_YIELD_UNIT")
					if (requestedYieldUnit.isNullOrEmpty() && recipe.yieldUnit != null) {
						viewModel.scale(requestedYieldAmount * (recipe.yieldValue ?: 1.0))
					}
					else if (requestedYieldUnit == recipe.yieldUnit) {
						viewModel.scale(requestedYieldAmount)
					}
					intent.removeExtra("RECIPE_YIELD_AMOUNT")
				}
				binding.recipeDetailPreparations.apply {
					if (recipe.preparations.isEmpty())
						visibility = View.GONE
					else {
						visibility = View.VISIBLE
						binding.recipeDetailPreparationsCount.text = resources.getQuantityString(
							R.plurals.prepared_times,
							recipe.preparations.size,
							recipe.preparations.size
						)
						binding.recipeDetailPreparationsTime.text = getString(
							R.string.last_time,
							DateUtils.getRelativeTimeSpanString(
								recipe.preparations.last().shiftToLocalDayStart().time,
								Date().time,
								DAY_IN_MILLIS
							)
						)
						val preparationsDialog = MaterialAlertDialogBuilder(this@RecipeActivity)
							.setTitle(R.string.prepared_on)
							.setView(
								RecyclerPreparationsBinding.inflate(layoutInflater).apply {
									preparationsRecycler.adapter = PreparationsAdapter(this@RecipeActivity, recipe.preparations.asReversed())
									preparationsRecycler.layoutManager = FlexboxLayoutManager(this@RecipeActivity)
								}.root
							)
							.setPositiveButton(R.string.ok) { _, _ -> }
							.create()
						setOnClickListener {
							preparationsDialog.show()
						}
					}
				}
			}
		}

		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiEvents.collect { event ->
					when (event) {
						UiEvent.Shrug -> Toast.makeText(this@RecipeActivity, "\uD83E\uDD37", Toast.LENGTH_SHORT).show()
					}
				}
			}
		}

		lifecycleScope.launch {
			viewModel.recipeDates.collectLatest { (created, modified) ->
				created?.let {
					binding.recipeDetailCreated.visibility = View.VISIBLE
					binding.recipeDetailCreatedDate.text = it
				}
				modified?.let {
					binding.recipeDetailModified.visibility = View.VISIBLE
					binding.recipeDetailModifiedDate.text = it
				}
			}
		}

		lifecycleScope.launch {
			viewModel.dependentRecipes.collectLatest { recipeTitleIdList ->
				if (recipeTitleIdList.isNotEmpty()) {
					binding.recipeDetailDependentRecipes.visibility = View.VISIBLE
					binding.recipeDetailDependentRecipesText.movementMethod = LinkMovementMethod.getInstance()
					binding.recipeDetailDependentRecipesText.text =
						recipeTitleIdList.joinTo(SpannableStringBuilder(), "\n") {
							SpannableString(it.title).apply {
								setSpan(
									object : ClickableSpan() {
										override fun onClick(widget: View) {
											startActivity(Intent(this@RecipeActivity, RecipeActivity::class.java).apply {
												putExtra("RECIPE_ID", it.id)
											})
										}
									},
									0,
									it.title.length,
									Spanned.SPAN_INCLUSIVE_EXCLUSIVE
								)
							}
					}
				}
			}
		}

		lifecycleScope.launch {
			viewModel.cookMode.collectLatest { enabled ->
				invalidateOptionsMenu()
				if (enabled) {
					window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
				} else {
					val keepScreenOnPref = application.getSharedPreferences(packageName + "_preferences", MODE_PRIVATE).getBoolean(PREF_SCREEN_ON, false)
					if (!keepScreenOnPref) {
						window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
					}
				}
			}
		}

		binding.recipeDetailIngredients.setContent {
			val recipe by viewModel.recipe.collectAsState(Recipe.createEmpty())
			if (recipe.ingredients.isNotEmpty()) {
				TournantTheme {
					IngredientCard(recipe)
				}
			}
		}

	}

	@Composable
	fun IngredientCard(recipe: Recipe) {
		Card(
			modifier = Modifier.fillMaxWidth(),
			elevation = 4.dp,
			shape = RoundedCornerShape(8.dp)
		) {
			Surface(
				Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
			) {
				val textMeasurer = rememberTextMeasurer()
				val cookModeActive by viewModel.cookMode.collectAsState(false)
				val weighingModeOn by viewModel.weighingModeOn.collectAsState(false)
				val weight by viewModel.ingredientWeight.collectAsState(0.0)
				val costBreakdown by viewModel.recipeCostBreakdown.collectAsState(null)
				val scaleRatio by viewModel.scaleRatio.collectAsState(1.0)
				val yieldValue by viewModel.yieldValueScaled.collectAsState("")
				val placeholder = recipe.yieldValue.toStringForCooks(thousands = false)
				val items by viewModel.ingredientsScaled.collectAsState(listOf())
				val allMasters by viewModel.allActiveMasterIngredients.collectAsState(emptyList())

				var showBatchScaler by remember { mutableStateOf(false) }
				var quickMapIngredient by remember { mutableStateOf<Ingredient?>(null) }

				val displayOutput = if (costBreakdown?.totalMassInKg != null && costBreakdown!!.totalMassInKg!! > 0.0) {
					"${costBreakdown!!.totalMassInKg!!.toStringForCooks()} kg"
				} else {
					val effectiveYield = yieldValue.ifEmpty { placeholder }
					val unit = recipe.yieldUnit ?: ""
					if (effectiveYield.isNotEmpty() || unit.isNotEmpty()) {
						"$effectiveYield $unit".trim()
					} else {
						"—"
					}
				}
				val displayTotalCost = if (costBreakdown != null && costBreakdown!!.totalCost > 0.0) {
					costBreakdown!!.formattedTotalCost()
				} else {
					"—"
				}
				val displayCostPerKg = if (costBreakdown?.costPerKg != null && costBreakdown!!.costPerKg!! > 0.0) {
					"${costBreakdown!!.formattedCostPerKg() ?: CostCurrencyFormatter.formatAmount(costBreakdown!!.costPerKg!!)} / kg"
				} else {
					"—"
				}

				Column(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(14.dp)
				) {
					// 1. Ingredients Header & Actions
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(
							modifier = Modifier.weight(1f),
							text = stringResource(R.string.ingredients),
							style = MaterialTheme.typography.h2,
							maxLines = 1,
							softWrap = false,
							overflow = TextOverflow.Ellipsis
						)
						if (weighingModeOn) {
							Text(
								text = "${weight.toStringForCooks()} g",
								style = MaterialTheme.typography.subtitle1.copy(
									fontWeight = FontWeight.Bold,
									color = MaterialTheme.colors.primary
								),
								overflow = TextOverflow.Visible,
								softWrap = false
							)
						}
						// Quick Cook Mode Toggle Chip
						Surface(
							shape = RoundedCornerShape(16.dp),
							color = if (cookModeActive) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
							modifier = Modifier.clickable { viewModel.toggleCookMode() }
						) {
							Row(
								verticalAlignment = Alignment.CenterVertically,
								modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
							) {
								Text(
									text = if (cookModeActive) "👨‍🍳 Cooking" else "👨‍🍳 Cook",
									style = MaterialTheme.typography.caption.copy(
										fontWeight = FontWeight.Bold,
										color = if (cookModeActive) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
									)
								)
							}
						}
						TournantRoundIconButton(
							size = 32.dp,
							icon = Icons.Default.ContentCopy,
							onClick = {
								(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
									ClipData.newPlainText(
										getString(R.string.ingredients),
										items.map { it.toStringForCooks(getString(R.string.optional)) }.joinToString("\n")
									)
								)
								Toast.makeText(
									this@RecipeActivity,
									getString(R.string.copied_to_clipboard),
									Toast.LENGTH_SHORT
								).show()
							},
							contentDescription = stringResource(R.string.copy_to_clipboard)
						)
						TournantRoundIconButton(
							size = 32.dp,
							icon = Icons.Default.Scale,
							onClick = { viewModel.toggleWeighingMode() },
							contentDescription = stringResource(R.string.weigh),
							isDark = weighingModeOn
						)
					}

					// Cook Mode Active Banner & Pinned Informational Notes
					if (cookModeActive) {
						Surface(
							modifier = Modifier.fillMaxWidth(),
							shape = RoundedCornerShape(8.dp),
							color = MaterialTheme.colors.primary.copy(alpha = 0.10f),
							border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.5f))
						) {
							Row(
								modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
								verticalAlignment = Alignment.CenterVertically,
								horizontalArrangement = Arrangement.SpaceBetween
							) {
								Text(
									text = stringResource(R.string.cook_mode_active_banner),
									style = MaterialTheme.typography.subtitle2.copy(
										fontWeight = FontWeight.Bold,
										color = MaterialTheme.colors.primary
									)
								)
								TextButton(
									onClick = { viewModel.setCookMode(false) },
									contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
								) {
									Text(
										text = stringResource(R.string.exit_cook_mode),
										style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold)
									)
								}
							}
						}

						val infoItems = items.filterIsInstance<IngredientItem>().filter {
							it.ingredient.isInformationalOnly || com.herohiman.tournant.cost.LiveCostCalculator.isInformationalGroup(it.ingredient.group)
						}
						if (infoItems.isNotEmpty()) {
							Card(
								modifier = Modifier.fillMaxWidth(),
								shape = RoundedCornerShape(8.dp),
								backgroundColor = Color(0xFFFFF8E1),
								border = BorderStroke(1.dp, Color(0xFFFFB300)),
								elevation = 2.dp
							) {
								Column(modifier = Modifier.padding(12.dp)) {
									Text(
										text = stringResource(R.string.kitchen_notes_pinned),
										style = MaterialTheme.typography.subtitle2.copy(
											fontWeight = FontWeight.Bold,
											color = Color(0xFFE65100)
										)
									)
									Spacer(Modifier.height(6.dp))
									infoItems.forEach { infoLine ->
										val noteTitle = infoLine.ingredient.noteTitle?.trim()
										val noteVal = infoLine.ingredient.noteValue?.trim() ?: infoLine.ingredient.item?.trim() ?: ""
										val cleanVal = if (noteVal.startsWith("जानकारी:", ignoreCase = true)) {
											noteVal.removePrefix("जानकारी:").removePrefix("जानकारी :").trim()
										} else {
											noteVal
										}
										val displayTitle = if (!noteTitle.isNullOrBlank() && !noteTitle.equals("जानकारी", ignoreCase = true)) {
											noteTitle
										} else {
											infoLine.ingredient.group?.takeIf { !it.equals("जानकारी", ignoreCase = true) }
										}
										Row(modifier = Modifier.padding(vertical = 2.dp)) {
											if (!displayTitle.isNullOrBlank()) {
												Text(
													text = "• $displayTitle: ",
													style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold)
												)
											} else {
												Text(
													text = "• ",
													style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold)
												)
											}
											Text(
												text = cleanVal,
												style = MaterialTheme.typography.body2
											)
										}
									}
								}
							}
						}
					}

					// 2. Output & Cost Dashboard Card
					OutputSummaryDashboard(
						displayOutput = displayOutput,
						displayTotalCost = displayTotalCost,
						displayCostPerKg = displayCostPerKg,
						yieldValue = yieldValue,
						placeholder = placeholder,
						yieldUnit = recipe.yieldUnit,
						onYieldChange = { viewModel.scale(it) },
						onScaleReset = { viewModel.scaleReset() },
						onScaleDown = { viewModel.scaleDown() },
						onScaleUp = { viewModel.scaleUp() },
						textMeasurer = textMeasurer,
						cookMode = cookModeActive,
						scaleRatio = scaleRatio,
						onOpenBatchScaler = { showBatchScaler = true }
					)

					// 3. Full-width Ingredient List
					IngredientList(
						items = items,
						modifier = Modifier.fillMaxWidth(),
						textMeasurer = textMeasurer,
						weighMode = weighingModeOn,
						costBreakdown = costBreakdown,
						cookMode = cookModeActive,
						onOpenQuickMap = { quickMapIngredient = it }
					)
				}

				if (showBatchScaler) {
					BatchScalerDialog(
						currentOutput = displayOutput,
						currentCost = displayTotalCost,
						yieldValue = yieldValue,
						placeholder = placeholder,
						yieldUnit = recipe.yieldUnit,
						costBreakdown = costBreakdown,
						scaleRatio = scaleRatio,
						onApplyScale = { targetVal ->
							viewModel.scale(targetVal)
						},
						onApplyMultiplier = { mult ->
							viewModel.scaleByMultiplier(mult)
						},
						onResetScale = {
							viewModel.scaleReset()
						},
						onDismiss = { showBatchScaler = false }
					)
				}

				if (quickMapIngredient != null) {
					QuickMapOrPriceDialog(
						ingredient = quickMapIngredient!!,
						allMasters = allMasters,
						onMapAlias = { masterId ->
							val raw = quickMapIngredient?.item.orEmpty()
							viewModel.bindIngredientAlias(raw, masterId) {
								Toast.makeText(
									this@RecipeActivity,
									getString(R.string.cost_saved),
									Toast.LENGTH_SHORT
								).show()
							}
						},
						onSavePrice = { name, price, unit ->
							val raw = quickMapIngredient?.item.orEmpty()
							viewModel.saveMasterIngredient(
								existing = null,
								rawItemName = raw,
								name = name,
								unitCost = price,
								baseUnit = unit,
								category = null,
								linkedRecipeId = null,
								yieldRatio = null,
								onSaved = {
									Toast.makeText(
										this@RecipeActivity,
										getString(R.string.cost_saved),
										Toast.LENGTH_SHORT
									).show()
								}
							)
						},
						onDismiss = { quickMapIngredient = null }
					)
				}
			}
		}
	}

	@Composable
	fun OutputSummaryDashboard(
		displayOutput: String,
		displayTotalCost: String,
		displayCostPerKg: String,
		yieldValue: String,
		placeholder: String,
		yieldUnit: String?,
		onYieldChange: (String) -> Unit,
		onScaleReset: () -> Unit,
		onScaleDown: () -> Unit,
		onScaleUp: () -> Unit,
		textMeasurer: TextMeasurer,
		modifier: Modifier = Modifier,
		cookMode: Boolean = false,
		scaleRatio: Double = 1.0,
		onOpenBatchScaler: () -> Unit = {}
	) {
		Card(
			modifier = modifier.fillMaxWidth(),
			shape = RoundedCornerShape(10.dp),
			elevation = 0.dp,
			backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.04f)
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 14.dp, vertical = 12.dp)
			) {
				// Interactive Yield / Scale Row
				Row(
					modifier = Modifier.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(6.dp)
				) {
					Text(
						text = stringResource(R.string.yield) + ":",
						style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Bold)
					)
					TournantUnderlinedTextField(
						value = yieldValue,
						onValueChange = onYieldChange,
						textMeasurer = textMeasurer,
						placeholder = placeholder.takeIf { it.isNotEmpty() } ?: "1"
					)
					Text(
						modifier = Modifier.weight(1f, fill = false),
						text = yieldUnit ?: pluralStringResource(
							R.plurals.lots,
							(yieldValue.takeIf { it.isNotEmpty() } ?: placeholder).getQuantityIntForPlurals() ?: 3
						),
						style = MaterialTheme.typography.body2.copy(fontStyle = FontStyle.Italic),
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
					Spacer(Modifier.weight(1f))
					TournantRoundIconButton(
						size = 28.dp,
						icon = Icons.Default.Scale,
						onClick = onOpenBatchScaler,
						contentDescription = stringResource(R.string.batch_scaler),
						isDark = false
					)
					TournantRoundIconButton(
						size = 28.dp,
						icon = Icons.Default.RepeatOne,
						isDark = true,
						onClick = onScaleReset,
						contentDescription = stringResource(R.string.reset)
					)
					TournantRoundIconButton(
						size = 28.dp,
						icon = Icons.Default.Remove,
						onClick = onScaleDown,
						contentDescription = stringResource(R.string.less)
					)
					TournantRoundIconButton(
						size = 28.dp,
						icon = Icons.Default.Add,
						onClick = onScaleUp,
						contentDescription = stringResource(R.string.more)
					)
				}

				Spacer(Modifier.height(10.dp))
				Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f), thickness = 1.dp)
				Spacer(Modifier.height(10.dp))

				// 3 Dashboard Metrics: Total Output | Batch Cost | Cost / kg
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceEvenly,
					verticalAlignment = Alignment.CenterVertically
				) {
					DashboardMetricTile(
						label = stringResource(R.string.dashboard_total_output),
						value = displayOutput,
						modifier = Modifier.weight(1f),
						onClick = onOpenBatchScaler
					)
					Divider(
						modifier = Modifier
							.height(34.dp)
							.width(1.dp),
						color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
					)
					if (cookMode) {
						DashboardMetricTile(
							label = stringResource(R.string.multiplier),
							value = "${String.format(Locale.US, "%.1f", scaleRatio)}x",
							modifier = Modifier.weight(1f),
							isPrimary = true,
							onClick = onOpenBatchScaler
						)
						Divider(
							modifier = Modifier
								.height(34.dp)
								.width(1.dp),
							color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
						)
						DashboardMetricTile(
							label = "FINANCIALS",
							value = "•••• (MASKED)",
							modifier = Modifier.weight(1f)
						)
					} else {
						DashboardMetricTile(
							label = stringResource(R.string.dashboard_total_cost),
							value = displayTotalCost,
							modifier = Modifier.weight(1f),
							isPrimary = true
						)
						Divider(
							modifier = Modifier
								.height(34.dp)
								.width(1.dp),
							color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
						)
						DashboardMetricTile(
							label = stringResource(R.string.dashboard_cost_per_kg),
							value = displayCostPerKg,
							modifier = Modifier.weight(1f),
							isPrimary = true
						)
					}
				}
			}
		}
	}

	@Composable
	fun DashboardMetricTile(
		label: String,
		value: String,
		modifier: Modifier = Modifier,
		isPrimary: Boolean = false,
		onClick: (() -> Unit)? = null
	) {
		Column(
			modifier = modifier
				.clip(RoundedCornerShape(6.dp))
				.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
				.padding(horizontal = 4.dp, vertical = 2.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = label.uppercase(),
					style = MaterialTheme.typography.overline.copy(
						fontSize = 9.sp,
						fontWeight = FontWeight.Bold,
						letterSpacing = 0.5.sp
					),
					color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
					textAlign = TextAlign.Center,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
				if (onClick != null) {
					Spacer(Modifier.width(2.dp))
					Text(text = "⚡", fontSize = 9.sp)
				}
			}
			Spacer(Modifier.height(3.dp))
			Text(
				text = value,
				style = MaterialTheme.typography.subtitle2.copy(
					fontWeight = FontWeight.Bold,
					fontSize = if (value.length > 11) 12.sp else 14.sp
				),
				color = if (isPrimary && value != "—" && !value.contains("••••")) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
				textAlign = TextAlign.Center,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}

	@Composable
	fun IngredientList(
		items: List<IngredientLine>,
		modifier: Modifier = Modifier,
		textMeasurer: TextMeasurer = rememberTextMeasurer(),
		weighMode: Boolean,
		costBreakdown: RecipeCostBreakdown? = null,
		cookMode: Boolean = false,
		onOpenQuickMap: ((Ingredient) -> Unit)? = null
	) {
		val typography = MaterialTheme.typography.body1
		val amountMaxWidth = with(LocalDensity.current) {
			items.filterIsInstance<IngredientItem>().maxOfOrNull {
				val displayStr = if (it.originalIngredient != null && it.originalIngredient.amount != it.ingredient.amount) {
					"${it.ingredient.amountToStringForCooks(appendSpace = false)} (${it.originalIngredient.amountToStringForCooks(appendSpace = false)}) "
				} else {
					it.ingredient.amountToStringForCooks()
				}
				textMeasurer.measure(
					displayStr,
					typography
				).size.width.toDp()
			}
		}
		Column(modifier) {
			items.forEachIndexed { i, item ->
				when (item) {
					is IngredientGroupTitle -> {
						val isInfoGroup = item.isInformationalGroup || com.herohiman.tournant.cost.LiveCostCalculator.isInformationalGroup(item.title)
						Row(
							verticalAlignment = Alignment.CenterVertically,
							modifier = Modifier
								.padding(top = if (i == 0) 0.dp else 8.dp, bottom = 4.dp)
								.clickable {
									item.title?.let { viewModel.toggleChecked(it) }
								}
						) {
							Text(
								text = item.title ?: "",
								style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
								color = if (isInfoGroup) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
							)
							if (isInfoGroup) {
								Spacer(Modifier.width(6.dp))
								Surface(
									shape = RoundedCornerShape(4.dp),
									color = MaterialTheme.colors.primary.copy(alpha = 0.12f)
								) {
									Text(
										text = stringResource(R.string.informational_badge),
										style = MaterialTheme.typography.caption.copy(
											fontSize = 10.sp,
											fontWeight = FontWeight.Bold,
											color = MaterialTheme.colors.primary
										),
										modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
									)
								}
							}
						}
					}
					is IngredientItem -> {
						val prevItem = items.getOrNull(i - 1) as? IngredientItem
						val isConsecutiveSubstitute = !item.ingredient.substituteGroupId.isNullOrBlank() &&
								prevItem != null &&
								prevItem.ingredient.substituteGroupId == item.ingredient.substituteGroupId

						if (isConsecutiveSubstitute) {
							Row(
								verticalAlignment = Alignment.CenterVertically,
								modifier = Modifier.padding(start = (amountMaxWidth ?: 0.dp) + 8.dp, top = 2.dp, bottom = 2.dp)
							) {
								Text(
									text = "— OR —",
									style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
									color = MaterialTheme.colors.primary.copy(alpha = 0.85f)
								)
							}
						}

						IngredientDisplay(
							item = item,
							id = i,
							amountMaxWidth = amountMaxWidth ?: 0.dp,
							weighMode = weighMode,
							costBreakdown = costBreakdown,
							cookMode = cookMode,
							onOpenQuickMap = onOpenQuickMap
						)
					}
				}
			}
		}
	}

	@Preview(showBackground = true)
	@Composable
	fun IngredientListPreview() {
		IngredientList(Ingredient.createExamples(3).mapIndexed { i, ingredient -> IngredientItem(ingredient, i % 2 == 1) }, weighMode = false)
	}

	@OptIn(ExperimentalFoundationApi::class)
	@Composable
	fun IngredientDisplay(
		item: IngredientItem,
		id: Int,
		amountMaxWidth: Dp,
		weighMode: Boolean,
		costBreakdown: RecipeCostBreakdown? = null,
		cookMode: Boolean = false,
		onOpenQuickMap: ((Ingredient) -> Unit)? = null
	) {
		val interactionSource = remember { MutableInteractionSource() }
		var dialogVisible by remember { mutableStateOf(false) }
		var value by remember { mutableStateOf(TextFieldValue("")) }

		val isSubstitute = !item.ingredient.substituteGroupId.isNullOrBlank()
		val isInactiveSub = isSubstitute && !item.ingredient.isActiveSubstitute
		val isInformational = item.ingredient.isInformationalOnly ||
				com.herohiman.tournant.cost.LiveCostCalculator.isInformationalGroup(item.ingredient.group)

		val lineCostFormatted = costBreakdown?.formattedCostForIngredient(item.ingredient)

		val amountText = if (item.originalIngredient != null && item.originalIngredient.amount != item.ingredient.amount) {
			buildAnnotatedString {
				append(item.ingredient.amountToStringForCooks(appendSpace = false))
				withStyle(SpanStyle(fontSize = if (cookMode) 14.sp else 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f))) {
					append(" (${item.originalIngredient.amountToStringForCooks(appendSpace = false)})")
				}
				append(" ")
			}
		} else {
			buildAnnotatedString { append(item.ingredient.amountToStringForCooks()) }
		}

		Row(
			modifier = Modifier
				.fillMaxWidth()
				.alpha(if (weighMode && item.isSelected || !weighMode && item.isChecked) ContentAlpha.disabled else if (isInactiveSub) 0.55f else 1f)
				.padding(vertical = if (cookMode) 6.dp else 4.dp)
				.combinedClickable(
					onClick = {
						if (isSubstitute && !item.ingredient.isActiveSubstitute) {
							viewModel.selectActiveSubstitute(item.ingredient)
						} else {
							viewModel.toggleChecked(id)
						}
					},
					onLongClick = {
						if (!isInformational && !cookMode) {
							showQuickEditPriceDialog(item.ingredient, costBreakdown?.findCostItem(item.ingredient))
						}
					},
					indication = null,
					interactionSource = interactionSource
				),
			verticalAlignment = Alignment.CenterVertically
		) {
			if (cookMode) {
				Checkbox(
					checked = item.isChecked,
					onCheckedChange = { viewModel.toggleChecked(id) },
					modifier = Modifier.size(28.dp),
					colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary)
				)
				Spacer(Modifier.width(6.dp))
			} else if (isSubstitute) {
				RadioButton(
					selected = item.ingredient.isActiveSubstitute,
					onClick = { viewModel.selectActiveSubstitute(item.ingredient) },
					modifier = Modifier.size(20.dp).align(Alignment.CenterVertically),
					colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary)
				)
				Spacer(Modifier.width(4.dp))
			}

			Text(
				text = amountText,
				modifier = Modifier
					.width(if (cookMode) amountMaxWidth * 1.15f else amountMaxWidth)
					.pointerInput(Unit) {
						detectTapGestures(
							onLongPress = {
								if (item.ingredient.amount != null) {
									value = TextFieldValue(item.ingredient.amount.toStringForCooks(thousands = false))
									dialogVisible = true
								}
							}
						)
					},
				textAlign = TextAlign.End,
				lineHeight = if (cookMode) 28.sp else 24.sp,
				style = if (cookMode) MaterialTheme.typography.subtitle1.copy(
					fontWeight = FontWeight.Bold,
					color = if (item.isChecked) MaterialTheme.colors.onSurface.copy(alpha = 0.5f) else MaterialTheme.colors.primary,
					textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
				) else LocalTextStyle.current
			)
			Spacer(Modifier.width(8.dp))
			val baseItemString = buildAnnotatedString {
				if (isInformational) {
					withStyle(SpanStyle(color = MaterialTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = if (cookMode) 13.sp else 11.sp)) {
						append("ℹ ")
					}
				}
				val name = if (isInformational) {
					val noteTitle = item.ingredient.noteTitle?.trim()
					val noteValue = item.ingredient.noteValue?.trim()
					val rawItem = item.ingredient.item?.trim() ?: ""
					val cleanedItem = if (rawItem.startsWith("जानकारी:", ignoreCase = true)) {
						rawItem.removePrefix("जानकारी:").removePrefix("जानकारी :").trim()
					} else {
						rawItem
					}

					if (!noteTitle.isNullOrBlank() && !noteTitle.equals("जानकारी", ignoreCase = true) && !noteValue.isNullOrBlank()) {
						"$noteTitle: $noteValue"
					} else {
						noteValue?.ifEmpty { null } ?: cleanedItem.ifEmpty { noteTitle?.takeIf { !it.equals("जानकारी", ignoreCase = true) } ?: "" }
					}
				} else {
					item.ingredient.item ?: ""
				}
				if (item.ingredient.refId == null) {
					append(name)
				}
				else {
					pushStringAnnotation("LINK_TO_RECIPE", item.ingredient.refId.toString())
					withStyle(SpanStyle(color = MaterialTheme.colors.primary, textDecoration = TextDecoration.Underline)) {
						append(name)
					}
					pop()
				}
			}
			val fullItemString = buildAnnotatedString {
				if (item.ingredient.optional) {
					append(stringResource(R.string.optional, baseItemString.text))
				} else {
					append(baseItemString)
				}
				if (isSubstitute && !item.ingredient.isActiveSubstitute) {
					withStyle(SpanStyle(fontSize = 11.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))) {
						append(" (OR)")
					}
				}
				if (!weighMode && item.isChecked && !cookMode) {
					withStyle(SpanStyle(fontSize = 14.sp, baselineShift = BaselineShift(0.1f))) {
						append(" ✓")
					}
				}
			}
			val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
			Text(
				text = fullItemString,
				modifier = Modifier
					.weight(1f)
					.pointerInput(Unit) {
						detectTapGestures(
							onPress = {
								layoutResult.value?.getOffsetForPosition(it)?.let { offset ->
									val annotations = baseItemString.getStringAnnotations(
										tag = "LINK_TO_RECIPE",
										start = offset,
										end = offset
									)
									if (annotations.isNotEmpty()) {
										item.ingredient.refId?.let { refId ->
											openRecipe(refId = refId, item.ingredient.amount, item.ingredient.unit)
										}
									} else {
										viewModel.toggleChecked(id)
									}
								}
							},
							onLongPress = {
								if (!isInformational && !cookMode) {
									showQuickEditPriceDialog(item.ingredient, costBreakdown?.findCostItem(item.ingredient))
								}
							}
						)
					},
				onTextLayout = { layoutResult.value = it },
				lineHeight = if (cookMode) 28.sp else 24.sp,
				style = if (cookMode) MaterialTheme.typography.subtitle1.copy(
					fontWeight = FontWeight.SemiBold,
					textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
				) else LocalTextStyle.current
			)

			if (cookMode) {
				// Hide prices in Cook Mode for privacy
			} else if (isInformational) {
				// Informational / Group exclusion rows render purely as clean text without prices or pricing prompts
			} else if (!lineCostFormatted.isNullOrEmpty()) {
				Spacer(Modifier.width(8.dp))
				Surface(
					shape = RoundedCornerShape(4.dp),
					color = if (isInactiveSub) {
						MaterialTheme.colors.onSurface.copy(alpha = 0.06f)
					} else {
						MaterialTheme.colors.primary.copy(alpha = 0.10f)
					},
					modifier = Modifier.clickable {
						if (!isInformational) {
							showQuickEditPriceDialog(item.ingredient, costBreakdown?.findCostItem(item.ingredient))
						}
					}
				) {
					Text(
						text = lineCostFormatted,
						style = MaterialTheme.typography.body2.copy(
							fontWeight = FontWeight.SemiBold,
							color = if (isInactiveSub) {
								MaterialTheme.colors.onSurface.copy(alpha = 0.45f)
							} else {
								MaterialTheme.colors.primary
							}
						),
						modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
						textAlign = TextAlign.End
					)
				}
			} else if (costBreakdown != null && !item.ingredient.item.isNullOrBlank()) {
				Spacer(Modifier.width(8.dp))
				Surface(
					shape = RoundedCornerShape(6.dp),
					border = BorderStroke(1.dp, Color(0xFFFF9800)),
					color = Color(0xFFFFF3E0),
					modifier = Modifier.clickable {
						onOpenQuickMap?.invoke(item.ingredient)
					}
				) {
					Text(
						text = stringResource(R.string.add_cost_or_map),
						style = MaterialTheme.typography.caption.copy(
							fontWeight = FontWeight.Bold,
							fontSize = 11.sp,
							color = Color(0xFFE65100)
						),
						modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
						textAlign = TextAlign.End
					)
				}
			}
		}
		
		if (dialogVisible) {
			val focusRequester = remember { FocusRequester() }
			AlertDialog(
				title = { Text(stringResource(R.string.scale_to), style = MaterialTheme.typography.h2) },
				text = {
					OutlinedTextField(
						modifier = Modifier.focusRequester(focusRequester),
						value = value,
						onValueChange = { newValue ->
							if (newValue.text.all { it.isDigit() || it == separator } && newValue.text.count { it == separator } <= 1) {
								value = newValue
							}
										},
						label = { Text(item.ingredient.item ?: "") },
						trailingIcon = { Text(item.ingredient.unit ?: "", style = MaterialTheme.typography.body1) },
						keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
						singleLine = true,
						textStyle = MaterialTheme.typography.body1
					)
				},
				confirmButton = {
					TextButton(
						onClick = {
							viewModel.scale(id, value.text)
							dialogVisible = false
						}
					) {
						Text(stringResource(R.string.ok))
					}
				},
				dismissButton = {
					TextButton(
						onClick = { dialogVisible = false }
					) {
						Text(stringResource(R.string.cancel))
					}
				},
				onDismissRequest = { dialogVisible = false },
			)
			LaunchedEffect(Unit) {
				value = value.copy(selection = TextRange(value.text.length))
				focusRequester.requestFocus()
			}
		}
	}

	@Composable
	fun BatchScalerDialog(
		currentOutput: String,
		currentCost: String,
		yieldValue: String,
		placeholder: String,
		yieldUnit: String?,
		costBreakdown: RecipeCostBreakdown?,
		scaleRatio: Double,
		onApplyScale: (String) -> Unit,
		onApplyMultiplier: (Double) -> Unit,
		onResetScale: () -> Unit,
		onDismiss: () -> Unit
	) {
		val baseYieldNum = (yieldValue.ifEmpty { placeholder }).toDoubleOrNull() ?: 1.0
		var currentMultiplier by remember { mutableStateOf(scaleRatio) }
		var targetYieldInput by remember { mutableStateOf(yieldValue.ifEmpty { placeholder }) }

		val presets = listOf(0.5, 1.0, 2.0, 5.0, 10.0)

		Dialog(onDismissRequest = onDismiss) {
			Card(
				shape = RoundedCornerShape(16.dp),
				elevation = 8.dp,
				modifier = Modifier.fillMaxWidth()
			) {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(18.dp),
					verticalArrangement = Arrangement.spacedBy(14.dp)
				) {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(
							text = stringResource(R.string.batch_scaler),
							style = MaterialTheme.typography.h2.copy(fontSize = 18.sp)
						)
						TournantRoundIconButton(
							size = 28.dp,
							icon = Icons.Default.Close,
							onClick = onDismiss,
							contentDescription = stringResource(R.string.cancel)
						)
					}

					Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))

					Text(
						text = "QUICK MULTIPLIER PRESETS",
						style = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.Bold),
						color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
					)

					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(6.dp)
					) {
						presets.forEach { preset ->
							val isSelected = kotlin.math.abs(currentMultiplier - preset) < 0.05
							Surface(
								shape = RoundedCornerShape(8.dp),
								color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
								modifier = Modifier
									.weight(1f)
									.clickable {
										currentMultiplier = preset
										val scaledVal = baseYieldNum * preset
										targetYieldInput = if (scaledVal % 1.0 == 0.0) scaledVal.toInt().toString() else String.format(Locale.US, "%.2f", scaledVal)
									}
							) {
								Box(
									contentAlignment = Alignment.Center,
									modifier = Modifier.padding(vertical = 8.dp)
								) {
									Text(
										text = "${preset}x",
										style = MaterialTheme.typography.body2.copy(
											fontWeight = FontWeight.Bold,
											color = if (isSelected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
										)
									)
								}
							}
						}
					}

					OutlinedTextField(
						value = targetYieldInput,
						onValueChange = { input ->
							if (input.all { it.isDigit() || it == '.' || it == ',' } && input.count { it == '.' || it == ',' } <= 1) {
								targetYieldInput = input
								val parsed = input.replace(',', '.').toDoubleOrNull()
								if (parsed != null && baseYieldNum > 0.0) {
									currentMultiplier = parsed / baseYieldNum
								}
							}
						},
						label = { Text("${stringResource(R.string.target_output)} (${yieldUnit ?: ""})") },
						modifier = Modifier.fillMaxWidth(),
						singleLine = true,
						keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
					)

					// Live Comparison Preview Card
					val scaledCost = (costBreakdown?.totalCost ?: 0.0) * currentMultiplier
					Card(
						shape = RoundedCornerShape(10.dp),
						backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
						elevation = 0.dp,
						modifier = Modifier.fillMaxWidth()
					) {
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(12.dp),
							horizontalArrangement = Arrangement.SpaceBetween,
							verticalAlignment = Alignment.CenterVertically
						) {
							Column {
								Text(
									text = "CURRENT BATCH",
									style = MaterialTheme.typography.overline.copy(fontSize = 9.sp),
									color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
								)
								Text(
									text = currentOutput,
									style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold)
								)
								if (currentCost != "—") {
									Text(
										text = currentCost,
										style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f))
									)
								}
							}

							Text(
								text = "➜",
								style = MaterialTheme.typography.h3.copy(color = MaterialTheme.colors.primary)
							)

							Column(horizontalAlignment = Alignment.End) {
								Text(
									text = "SCALED (${String.format(Locale.US, "%.1f", currentMultiplier)}x)",
									style = MaterialTheme.typography.overline.copy(fontSize = 9.sp, color = MaterialTheme.colors.primary),
									fontWeight = FontWeight.Bold
								)
								val scaledOutputMass = if (costBreakdown?.totalMassInKg != null && costBreakdown.totalMassInKg!! > 0.0) {
									"${(costBreakdown.totalMassInKg!! * currentMultiplier).toStringForCooks()} kg"
								} else {
									"$targetYieldInput ${yieldUnit ?: ""}".trim()
								}
								Text(
									text = scaledOutputMass,
									style = MaterialTheme.typography.body2.copy(
										fontWeight = FontWeight.Bold,
										color = MaterialTheme.colors.primary
									)
								)
								if (costBreakdown != null && scaledCost > 0.0) {
									Text(
										text = CostCurrencyFormatter.formatAmount(scaledCost),
										style = MaterialTheme.typography.caption.copy(
											fontWeight = FontWeight.Bold,
											color = MaterialTheme.colors.primary
										)
									)
								}
							}
						}
					}

					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp)
					) {
						OutlinedButton(
							onClick = {
								onResetScale()
								onDismiss()
							},
							modifier = Modifier.weight(1f)
						) {
							Text(stringResource(R.string.reset_scale))
						}
						Button(
							onClick = {
								onApplyMultiplier(currentMultiplier)
								onDismiss()
							},
							modifier = Modifier.weight(1f)
						) {
							Text(stringResource(R.string.apply_scale))
						}
					}
				}
			}
		}
	}

	@Composable
	fun QuickMapOrPriceDialog(
		ingredient: Ingredient,
		allMasters: List<MasterIngredientEntity>,
		onMapAlias: (masterId: Long) -> Unit,
		onSavePrice: (name: String, price: Double, unit: String) -> Unit,
		onDismiss: () -> Unit
	) {
		var selectedTab by remember { mutableIntStateOf(0) }
		var searchQuery by remember { mutableStateOf("") }

		val rawName = ingredient.item?.trim().orEmpty()
		val filteredMasters = remember(allMasters, searchQuery, rawName) {
			if (searchQuery.isNotBlank()) {
				allMasters.filter { it.name.contains(searchQuery, ignoreCase = true) }
			} else {
				allMasters.sortedByDescending {
					if (rawName.contains(it.name, ignoreCase = true) || it.name.contains(rawName, ignoreCase = true)) 2
					else 0
				}
			}
		}

		var newName by remember { mutableStateOf(rawName) }
		var newPrice by remember { mutableStateOf("") }
		var newUnit by remember { mutableStateOf(ingredient.unit?.ifBlank { "kg" } ?: "kg") }

		Dialog(onDismissRequest = onDismiss) {
			Card(
				shape = RoundedCornerShape(16.dp),
				elevation = 8.dp,
				modifier = Modifier
					.fillMaxWidth()
					.heightIn(max = 520.dp)
			) {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(16.dp),
					verticalArrangement = Arrangement.spacedBy(12.dp)
				) {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(
							text = stringResource(R.string.map_or_price_title, rawName),
							style = MaterialTheme.typography.h2.copy(fontSize = 17.sp),
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
							modifier = Modifier.weight(1f)
						)
						TournantRoundIconButton(
							size = 28.dp,
							icon = Icons.Default.Close,
							onClick = onDismiss,
							contentDescription = stringResource(R.string.cancel)
						)
					}

					TabRow(
						selectedTabIndex = selectedTab,
						backgroundColor = MaterialTheme.colors.surface,
						contentColor = MaterialTheme.colors.primary
					) {
						Tab(
							selected = selectedTab == 0,
							onClick = { selectedTab = 0 },
							text = { Text("Link Existing", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
						)
						Tab(
							selected = selectedTab == 1,
							onClick = { selectedTab = 1 },
							text = { Text("New Price", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
						)
					}

					if (selectedTab == 0) {
						OutlinedTextField(
							value = searchQuery,
							onValueChange = { searchQuery = it },
							placeholder = { Text(stringResource(R.string.search_ingredients)) },
							modifier = Modifier.fillMaxWidth(),
							singleLine = true
						)

						if (filteredMasters.isEmpty()) {
							Box(
								modifier = Modifier
									.fillMaxWidth()
									.padding(vertical = 24.dp),
								contentAlignment = Alignment.Center
							) {
								Text(
									text = "No matching ingredients found.\nSwitch to 'New Price' to create it.",
									style = MaterialTheme.typography.body2,
									textAlign = TextAlign.Center,
									color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
								)
							}
						} else {
							LazyColumn(
								modifier = Modifier
									.fillMaxWidth()
									.weight(1f, fill = false),
								verticalArrangement = Arrangement.spacedBy(6.dp)
							) {
								items(filteredMasters) { master ->
									val isSmartMatch = rawName.contains(master.name, ignoreCase = true) || master.name.contains(rawName, ignoreCase = true)
									Surface(
										shape = RoundedCornerShape(8.dp),
										color = if (isSmartMatch) MaterialTheme.colors.primary.copy(alpha = 0.08f) else MaterialTheme.colors.onSurface.copy(alpha = 0.03f),
										border = if (isSmartMatch) BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.3f)) else null,
										modifier = Modifier
											.fillMaxWidth()
											.clickable {
												onMapAlias(master.id)
												onDismiss()
											}
									) {
										Row(
											modifier = Modifier
												.fillMaxWidth()
												.padding(horizontal = 12.dp, vertical = 10.dp),
											horizontalArrangement = Arrangement.SpaceBetween,
											verticalAlignment = Alignment.CenterVertically
										) {
											Column(modifier = Modifier.weight(1f)) {
												Row(verticalAlignment = Alignment.CenterVertically) {
													Text(
														text = master.name,
														style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold)
													)
													if (isSmartMatch) {
														Spacer(Modifier.width(6.dp))
														Surface(
															shape = RoundedCornerShape(4.dp),
															color = MaterialTheme.colors.primary
														) {
															Text(
																text = "SUGGESTED",
																style = MaterialTheme.typography.overline.copy(
																	fontSize = 8.sp,
																	color = MaterialTheme.colors.onPrimary,
																	fontWeight = FontWeight.Bold
																),
																modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
															)
														}
													}
												}
												Text(
													text = "${CostCurrencyFormatter.formatAmount(master.unitCost)} / ${master.baseUnit}",
													style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f))
												)
											}

											Button(
												onClick = {
													onMapAlias(master.id)
													onDismiss()
												},
												contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
												shape = RoundedCornerShape(6.dp)
											) {
												Text(
													text = stringResource(R.string.map_as_alias),
													fontSize = 11.sp,
													fontWeight = FontWeight.Bold
												)
											}
										}
									}
								}
							}
						}
					} else {
						OutlinedTextField(
							value = newName,
							onValueChange = { newName = it },
							label = { Text(stringResource(R.string.ingredient_name)) },
							modifier = Modifier.fillMaxWidth(),
							singleLine = true
						)
						OutlinedTextField(
							value = newPrice,
							onValueChange = {
								if (it.all { ch -> ch.isDigit() || ch == '.' || ch == ',' } && it.count { ch -> ch == '.' || ch == ',' } <= 1) {
									newPrice = it
								}
							},
							label = { Text(stringResource(R.string.unit_cost)) },
							placeholder = { Text("e.g. 700.00") },
							modifier = Modifier.fillMaxWidth(),
							singleLine = true,
							keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
						)
						OutlinedTextField(
							value = newUnit,
							onValueChange = { newUnit = it },
							label = { Text(stringResource(R.string.base_unit)) },
							modifier = Modifier.fillMaxWidth(),
							singleLine = true
						)

						Button(
							onClick = {
								val price = newPrice.replace(',', '.').toDoubleOrNull() ?: 0.0
								if (newName.isNotBlank() && newUnit.isNotBlank() && price > 0.0) {
									onSavePrice(newName.trim(), price, newUnit.trim())
									onDismiss()
								}
							},
							modifier = Modifier.fillMaxWidth()
						) {
							Text("Save Price & Map")
						}
					}
				}
			}
		}
	}

	// Parses text as markdown or html (if markwon instance null, that depends on user preference)
	private fun parseRecipeText(text: String): Spanned {
		return markwon?.toMarkdown(text) ?: text.replace("\n", "<br/>").parseAsHtml()
	}

	private fun shareRecipe(format: String) {
		lifecycleScope.launch {
			withContext(Dispatchers.IO) {
				val filename = binding.recipeDetailTitle.text.toString().ifBlank { getString(R.string.recipe) }
				(application as TournantApplication).writeRecipesToExportDir(setOf(intent.getLongExtra("RECIPE_ID", 0L)), filename, format)
				val uri = FileProvider.getUriForFile(
					application,
					BuildConfig.APPLICATION_ID + ".fileprovider",
					File(File(filesDir, "export"), "$filename.$format")
				)
				ShareCompat.IntentBuilder(this@RecipeActivity)
					.setStream(uri)
					.setType("application/$format")
					.startChooser()
			}
		}
	}

	fun openRecipe(refId: Long, yieldAmount: Double?, yieldUnit: String?) {
		startActivity(Intent(this, RecipeActivity::class.java).apply {
			putExtra("RECIPE_ID", refId)
			if (yieldAmount != null) {
				putExtra("RECIPE_YIELD_AMOUNT", yieldAmount)
				putExtra("RECIPE_YIELD_UNIT", yieldUnit)
			}
		})
	}

	fun showQuickEditPriceDialog(
		ingredient: Ingredient,
		costItem: IngredientCostItem? = null
	) {
		if (ingredient.isInformationalOnly) return
		val rawName = ingredient.item?.trim().orEmpty()
		if (rawName.isBlank()) return

		lifecycleScope.launch {
			val existingMaster = withContext(Dispatchers.IO) {
				costItem?.masterIngredient
					?: viewModel.resolveMasterIngredient(
						rawName = rawName,
						linkedRecipeId = ingredient.refId
					)
			}
			val titlesWithIds = withContext(Dispatchers.IO) {
				try {
					viewModel.getRecipeTitlesWithIds().firstOrNull() ?: emptyList()
				} catch (e: Exception) {
					emptyList()
				}
			}
			showMasterCostEditDialogInternal(ingredient, existingMaster, titlesWithIds)
		}
	}

	private fun showMasterCostEditDialogInternal(
		ingredient: Ingredient,
		existing: MasterIngredientEntity?,
		titlesWithIds: List<com.herohiman.tournant.data.RecipeTitleId>
	) {
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

		var selectedRecipeId: Long? = existing?.linkedRecipeId ?: ingredient.refId

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
		} else {
			editName.setText(ingredient.item ?: "")
			editBaseUnit.setText(ingredient.unit?.ifBlank { "kg" } ?: "kg")
			if (ingredient.refId != null) {
				switchSubRecipe.isChecked = true
				layoutSubRecipeContainer.visibility = View.VISIBLE
				val matchedTitle = titlesWithIds.firstOrNull { it.id == ingredient.refId }?.title
				if (!matchedTitle.isNullOrBlank()) {
					autoLinkedRecipe.setText(matchedTitle, false)
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

				viewModel.saveMasterIngredient(
					existing = existing,
					rawItemName = ingredient.item ?: "",
					name = name,
					unitCost = cost,
					baseUnit = baseUnit,
					category = category,
					linkedRecipeId = linkedRecipe,
					yieldRatio = yieldRatio,
					onSaved = {
						Toast.makeText(this@RecipeActivity, R.string.cost_saved, Toast.LENGTH_SHORT).show()
					}
				)
			}
			.setNegativeButton(R.string.cancel, null)
			.show()
	}

	override fun showAlarmDialog(minutes: Int) {
		val calendar = Calendar.getInstance().apply {
			add(Calendar.MINUTE, minutes)
		}
		MaterialTimePicker.Builder()
			.setTitleText(R.string.set_alarm)
			.setHour(calendar[Calendar.HOUR_OF_DAY])
			.setMinute(calendar[Calendar.MINUTE])
			.setTimeFormat(if (DateFormat.is24HourFormat(this)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
			.setPositiveButtonText(R.string.ok)
			.setNegativeButtonText(R.string.cancel)
			.setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
			.build()
			.also { picker -> picker.addOnPositiveButtonClickListener { setAlarm(picker.hour, picker.minute) } }
			.show(supportFragmentManager, "SetAlarmDialog")
	}

	private fun setAlarm(hour: Int, minute: Int) {
		try {
			startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply {
				putExtra(AlarmClock.EXTRA_MESSAGE, binding.recipe?.title)
				putExtra(AlarmClock.EXTRA_HOUR, hour)
				putExtra(AlarmClock.EXTRA_MINUTES, minute)
			})
		} catch (_: ActivityNotFoundException) {
			Toast.makeText(this, R.string.no_suitable_application, Toast.LENGTH_LONG).show()
		}
	}

	@SuppressLint("SetTextI18n")
	override fun showTimerDialog(seconds: Int) {
		val customView = InputFieldTimeBinding.inflate(LayoutInflater.from(this), null, false).apply {
			minutesField.apply {
				doOnTextChanged { text, _, _, _ ->
					minutesMinus.isEnabled = ((text.toString().toIntOrNull() ?: 0) != 0)
					secondsMinus.isEnabled = (text.toString().toIntOrNull() ?: 0) != 0 || (secondsField.text.toString().toIntOrNull() ?: 0) != 0
				}
				setText((seconds / 60).toString())
			}
			secondsField.apply {
				doOnTextChanged { text, _, _, _ ->
					secondsMinus.isEnabled = (text.toString().toIntOrNull() ?: 0) != 0 || (minutesField.text.toString().toIntOrNull() ?: 0) != 0
				}
				setText((seconds % 60).toString())
			}
			secondsField.filters += InputFilter { source, _, _, dest, _, _ ->
				if (((dest.toString() + source.toString()).toIntOrNull() ?: 0) < 60) null else ""
			}
			minutesPlus.setOnClickListener {
				minutesField.setText(((minutesField.text.toString().toIntOrNull() ?: 0) + 1).toString())
			}
			minutesMinus.setOnClickListener {
				minutesField.setText((minutesField.text.toString().toInt() - 1).toString())
			}
			secondsPlus.setOnClickListener {
				val value = secondsField.text.toString().toIntOrNull() ?: 0
				if (value == 59) {
					secondsField.setText(0.toString())
					minutesPlus.performClick()
				}
				else {
					secondsField.setText(((secondsField.text.toString().toIntOrNull() ?: 0) + 1).toString())
				}
			}
			secondsMinus.setOnClickListener {
				val value = secondsField.text.toString().toIntOrNull() ?: 0
				if (value == 0) {
					minutesMinus.performClick()
					secondsField.setText(59.toString())
				}
				else {
					secondsField.setText((value - 1).toString())
				}
			}
		}
		MaterialAlertDialogBuilder(this)
			.setTitle(R.string.set_timer)
			.setView(customView.root)
			.setPositiveButton(R.string.ok) { _, _ ->
				val min = customView.minutesField.text.toString().toIntOrNull() ?: 0
				val s = customView.secondsField.text.toString().toIntOrNull() ?: 0
				if ((min + s) != 0)
					startTimer(min * 60 + s)
			}
			.setNegativeButton(R.string.cancel, null)
			.show()
	}

	private fun startTimer(seconds: Int) {
		try {
			startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply {
				putExtra(AlarmClock.EXTRA_MESSAGE, binding.recipe?.title)
				putExtra(AlarmClock.EXTRA_LENGTH, seconds)
				putExtra(AlarmClock.EXTRA_SKIP_UI, true)
			})
			val timeString = if (seconds >= 60)
				"%02d".format(seconds / 60) + ":" + "%02d".format(seconds % 60) + " min"
			else
				"$seconds s"
			Toast.makeText(this, getString(R.string.timer_set, timeString), Toast.LENGTH_SHORT).show()
		} catch (_: ActivityNotFoundException) {
			Toast.makeText(this, R.string.no_suitable_application, Toast.LENGTH_LONG).show()
		}
	}

	private fun logPreparation() {
		MaterialDatePicker.Builder.datePicker()
			.setCalendarConstraints(
				CalendarConstraints.Builder()
					.setValidator(DateValidatorPointBackward.now())
					.build()
			)
			.setTitleText(getString(R.string.prepared_on))
			.setSelection(MaterialDatePicker.todayInUtcMilliseconds())
			.build()
			.apply {
				addOnPositiveButtonClickListener {
					viewModel.addPreparation(Date(it))
				}
			}
			.show(supportFragmentManager, "DatePicker")
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.options_recipe, menu)
		if (application.getSharedPreferences(packageName + "_preferences", MODE_PRIVATE).getInt(PREF_MODE, 0) == MODE_SYNCED)
			menu.removeItem(R.id.edit)
		menu.findItem(R.id.action_cook_mode)?.title =
			if (viewModel.cookMode.value) getString(R.string.exit_cook_mode) else getString(R.string.cook_mode)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_cook_mode -> {
				viewModel.toggleCookMode()
				val isCook = viewModel.cookMode.value
				Toast.makeText(
					this,
					if (isCook) R.string.cook_mode_active_banner else R.string.exit_cook_mode,
					Toast.LENGTH_SHORT
				).show()
				true
			}
			R.id.log_preparation -> { logPreparation(); true }
			R.id.share_json -> { shareRecipe("json"); true }
			R.id.share_zip -> { shareRecipe("zip"); true }
			R.id.share_gourmand -> {
				(application as TournantApplication).withGourmandIssueCheck(this, setOf(intent.getLongExtra("RECIPE_ID", 0L))) {
					shareRecipe("xml")
				}
				true
			}
			R.id.edit -> {
				startActivity(Intent(this, RecipeEditingActivity::class.java).apply {
					putExtra("RECIPE_ID", intent.getLongExtra("RECIPE_ID", 0L))
				})
				true
			}
			android.R.id.home -> {
				finish()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun removePreparation(date: Date) {
		viewModel.removePreparation(date)
	}
}