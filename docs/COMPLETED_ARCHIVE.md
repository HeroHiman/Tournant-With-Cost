# Completed Stories and Bug Fixes Archive

This archive provides technical documentation for all recently implemented stories, architectural improvements, and bug fixes in Tournant.

---

## 1. Implemented Stories
 
### Epic 15: Commercial Kitchen & UX Enhancements

#### Story 15.1: In-Line Unpriced Item Indicator & Quick-Map Chip (Presentation & Domain Layer)
- **Summary**: Added an in-line `[+ Add Cost / Map]` badge to unpriced/unmapped ingredient rows in the Recipe Viewer's `IngredientDisplay`. Tapping the chip launches the `QuickMapOrPriceDialog`, presenting a smart suggestion list of existing master ingredients for 1-tap alias binding, as well as an option to create a brand new master unit price (₹/unit). Binding aliases immediately triggers Room Flow re-emission, recalculating the recipe line costs, total cost, and cost per kg live on screen.
- **Technical Files Modified**:
  - [`RecipeActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeActivity.kt)
  - [`RecipeViewModel.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeViewModel.kt)
  - [`RecipeRepository.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/RecipeRepository.kt)
  - [`strings.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/values/strings.xml)
  - [`RecipeViewerLineCostUiTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/RecipeViewerLineCostUiTest.kt)

#### Story 15.2: Interactive Batch Scaler Bottom Sheet with Common Multipliers (Presentation Layer)
- **Summary**: Implemented the `BatchScalerDialog` triggered directly by tapping the Total Output tile or `Scale ⚡` button in the `OutputSummaryDashboard`. Provides one-tap commercial multiplier preset chips (`0.5x`, `1.0x`, `2.0x`, `5.0x`, `10.0x`), custom numerical multiplier / target output inputs, and a live side-by-side comparison table contrasting current vs. target yield and batch costs before applying.
- **Technical Files Modified**:
  - [`RecipeActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeActivity.kt)
  - [`RecipeViewModel.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeViewModel.kt)
  - [`strings.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/values/strings.xml)
  - [`RecipeViewerLineCostUiTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/RecipeViewerLineCostUiTest.kt)

#### Story 15.3: Recipe Cook Mode Full-Screen Focus with Cost Masking & Step Checkoffs (Presentation Layer)
- **Summary**: Implemented an employee-friendly Cook Mode toggle accessible via the AppBar (`action_cook_mode` with `ic_restaurant` icon) and recipe overview card. Keeps device screen awake continuously (`FLAG_KEEP_SCREEN_ON`). Masks financial information (line costs, total batch cost, cost per kg) with `••••` for privacy. Pins informational and operational notes (`isInformationalOnly == true`, e.g. tray size, packaging notes) in a dedicated top banner. Increases ingredient typography (`18.sp` bold amounts, `17.sp` names) with large 28dp checkboxes and strike-through checkoffs.
- **Technical Files Modified**:
  - [`RecipeActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeActivity.kt)
  - [`RecipeViewModel.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeViewModel.kt)
  - [`options_recipe.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/menu/options_recipe.xml)
  - [`strings.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/values/strings.xml)
  - [`RecipeViewerLineCostUiTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/RecipeViewerLineCostUiTest.kt)

---

### Epic 14: Quick-Edit Pricing Integration
+
+#### Story 14.1: Row Long-Press Listener & Price Badge Click (Presentation Layer)
+- **Summary**: Added seamless quick-edit interactions directly in the Recipe Viewer's `IngredientDisplay` Compose layout. Long-pressing an ingredient row or text triggers `showQuickEditPriceDialog`, retrieving the corresponding master ingredient ID or record for that specific item (e.g. "काजू"). Tapping the price badge (`Surface`) or the unpriced `+ ₹` badge also directly opens the quick edit dialog.
+- **Technical Files Modified**:
+  - [`RecipeActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeActivity.kt)
+
+#### Story 14.2: Master Data Retrieval (Domain Layer)
+- **Summary**: Added `resolveMasterIngredient` and `getRecipeTitlesWithIds` in `RecipeViewModel`. Queries `RecipeRepository` and Room's `MasterIngredientDao` / `IngredientAliasDao` to look up by linked recipe ID, direct master ingredient name, or alias raw name, pre-populating current base price, unit, category, and sub-recipe link options.
+- **Technical Files Modified**:
+  - [`RecipeViewModel.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeViewModel.kt)
+
+#### Story 14.3: Dialog Injection & Live Recalculation (Presentation Layer)
+- **Summary**: Injected `dialog_edit_master_ingredient.xml` directly over the Recipe Viewer. On tapping "Save", persisted changes to Room via `RecipeViewModel.saveMasterIngredient(...)` and automatically registered aliases when raw ingredient names differ. Because `_masterIngredients` is an active Room `Flow`, database mutations immediately trigger `LiveCostCalculator` recalculation live on the screen without requiring a page refresh.
+- **Technical Files Modified**:
+  - [`RecipeActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeActivity.kt)
+  - [`RecipeViewModel.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeViewModel.kt)
+  - [`RecipeViewerLineCostUiTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/RecipeViewerLineCostUiTest.kt)
+
+---
+
 ### Epic 12: Modern Recipe UI & Output Dashboard

#### Story 12.1: Fix Layout Spacing Bug (Presentation Layer)
- **Summary**: Removed the enclosing `FlexboxLayout` with `app:alignItems="stretch"` wrapping `recipe_detail_ingredients` and `recipe_detail_instructions` in `activity_recipe.xml`. Placed `recipe_detail_ingredients` and `recipe_detail_instructions` directly into the vertical `LinearLayout` container with matching `android:layout_marginHorizontal="16dp"` and `android:layout_marginBottom="16dp"`, snapping the ingredients card immediately beneath the header details card and completely eliminating the phantom vertical gap.
- **Technical Files Modified**:
  - [`activity_recipe.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/layout/activity_recipe.xml)

#### Story 12.2: Output Summary Dashboard & Math Correction (Domain & Presentation Layer)
- **Summary**: Replaced the plain single-line text string with a dedicated Material summary card (`OutputSummaryDashboard`) embedded at the top of the ingredients section. Corrected cost and yield math by calculating the **Sum of All Active Ingredient Weights** (e.g. 10 kg cashew + 9 kg sugar = 19 kg) as the divisor for true Cost / kg (`₹7432.00 / 19 = ₹391.16 / kg`), with explicit mass yield override support for evaporation (e.g. 17 kg). Displays interactive yield scaling controls and three distinct financial dashboard metrics:
  - **Total Output**: Active raw material mass sum or explicit yield weight (e.g. `19 kg` or `17 kg`)
  - **Batch Cost**: Total batch cost formatted in Indian Rupee (`₹7,432.00`)
  - **Cost / kg**: True cost per kilogram (`₹391.16 / kg` or `₹437.18 / kg`)
- **Technical Files Modified**:
  - [`LiveCostCalculator.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/cost/LiveCostCalculator.kt)
  - [`YieldParser.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/cost/YieldParser.kt)
  - [`RecipeViewModel.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeViewModel.kt)
  - [`RecipeActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeActivity.kt)
  - [`strings.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/values/strings.xml)
  - [`RecipeViewerLineCostUiTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/RecipeViewerLineCostUiTest.kt)

#### Story 12.3: Modernize Ingredient Rows & Action Layout Fix (Presentation Layer)
- **Summary**: Resolved title text wrapping ("Ingredi" / "ents") by setting `maxLines = 1`, `softWrap = false`, and consolidating the action buttons. Consolidated scaling buttons (`RepeatOne`, `Remove`, `Add`) directly into the Yield / Scale row inside `OutputSummaryDashboard` using compact 28dp icons, leaving only Copy and Weigh buttons in the header row. Styled line-item price badges with a subtle primary-tinted `Surface` container (`RoundedCornerShape(4.dp)`), ensuring prices on the right edge are never occluded.
- **Technical Files Modified**:
  - [`Button.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/elements/Button.kt)
  - [`RecipeActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeActivity.kt)

---

### Epic 11: Recipe Metadata, Informational Cost Exclusions & Yield Parsing

#### Story 11.1: Informational Cost Exclusion (Domain & Schema Layer)
- **Summary**: Added `isInformationalOnly`, `noteTitle`, and `noteValue` to `Ingredient` and `IngredientEntity` (Room migration 13 to 14) and updated `LiveCostCalculator` with `CostStatus.INFORMATIONAL_EXCLUDED` to exclude non-material notes (e.g. "17 किलो डब्बा में पैक") from inflating recipe cost totals and false unpriced warnings.
- **Technical Files Modified**:
  - [`Ingredient.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/Ingredient.kt) & [`IngredientEntity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/IngredientEntity.kt)
  - [`RecipeRoomDatabase.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/RecipeRoomDatabase.kt) (Migration 13 to 14, schema `14.json`)
  - [`Recipe.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/Recipe.kt) & [`RecipeWithIngredientsAndPreparations.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/RecipeWithIngredientsAndPreparations.kt)
  - [`LiveCostCalculator.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/cost/LiveCostCalculator.kt)
  - [`InformationalCostExclusionTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/InformationalCostExclusionTest.kt)

#### Story 11.2: Title & Tray Size Metadata UI (Presentation Layer)
- **Summary**: Updated Recipe Editor ingredient rows to support marking items as informational. When enabled, ingredient amount/unit/item dropdowns are replaced with dedicated Title (e.g., "ट्रे Size") and Note/विवरण fields with a distinct "जानकारी" badge.
- **Technical Files Modified**:
  - [`recycler_item_ingredient_editing.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/layout/recycler_item_ingredient_editing.xml)
  - [`options_ingredient.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/menu/options_ingredient.xml)
  - [`IngredientEditingAdapter.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/adapter/IngredientEditingAdapter.kt)
  - [`strings.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/values/strings.xml)

#### Story 11.3: YieldParser & "Cost per kg" Metric (Domain & Presentation Layer)
- **Summary**: Implemented `YieldParser` capable of extracting target mass amounts and units from complex yield descriptions (e.g., "1 टीपा काजू (10 किलो)", "500 ग्राम", Devanagari numerals), computing normalized cost per kilogram (`totalCost / effectiveKg`), and rendering `Cost per kg: ₹...` in the Recipe Viewer header.
- **Technical Files Modified**:
  - [`YieldParser.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/cost/YieldParser.kt)
  - [`RecipeViewModel.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeViewModel.kt)
  - [`RecipeActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeActivity.kt)
  - [`YieldParserTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/YieldParserTest.kt)

#### Story 11.4: Group-Level Informational Exclusion Zones (Data, Domain & UI Layer)
- **Summary**: Upgraded group handling so that entire groups (such as "टोटल माल", "जानकारी", "डब्बा", or custom informational groups) act as exclusion zones. `IngredientGroupTitle` carries `isInformationalGroup`, cascading `isInformationalOnly = true` to all child items during save. `LiveCostCalculator` ignores informational groups and items when summing active mass for Total Output ($10\text{ kg} + 9\text{ kg} = 19\text{ kg}$ instead of 57 kg) and assigns `CostStatus.INFORMATIONAL_EXCLUDED` with ₹0.00 line cost. In the Recipe Editor, group headers support a 1-tap "Mark as Informational (जानकारी)" toggle with a badge, while the Recipe Viewer suppresses price badges and `[+ Add Cost / Map]` prompts for all enclosed notes. Stripped redundant `"जानकारी:"` string prefixes across the adapter, item mapping, and viewer rows to keep the UI clean under the amber group badge.
- **Technical Files Modified**:
  - [`IngredientLine.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/IngredientLine.kt)
  - [`Extensions.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/Extensions.kt)
  - [`LiveCostCalculator.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/cost/LiveCostCalculator.kt)
  - [`IngredientEditingAdapter.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/adapter/IngredientEditingAdapter.kt)
  - [`recycler_item_ingredient_editing_group.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/layout/recycler_item_ingredient_editing_group.xml)
  - [`RecipeActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeActivity.kt)
  - [`RecipeViewerLineCostUiTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/RecipeViewerLineCostUiTest.kt)

---

### Story 9.1: Reactive Search Queries (Data Layer)
- **Summary**: Implemented reactive Room SQL queries supporting dynamic partial-match search (`LIKE '%' || :query || '%'`) across master ingredient names, categories, and unit aliases in both English and Hindi (Devanagari script).
- **Technical Files Modified**:
  - [`MasterIngredientDao.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/MasterIngredientDao.kt): Added `searchMasterIngredients(query: String): Flow<List<MasterIngredientEntity>>`, `searchActiveMasterIngredients(query: String)`, and synchronous list variants.
  - [`UnitAliasDao.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/UnitAliasDao.kt): Added `searchUnitAliases(query: String): Flow<List<UnitAliasEntity>>` and `searchUnitAliasesByCategory(query: String, baseUnit: BaseUnitType)`.
  - [`RecipeRepository.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/RecipeRepository.kt): Exposed non-null flow delegation methods.
  - [`ReactiveSearchQueriesTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/ReactiveSearchQueriesTest.kt): Unit test coverage for multilingual queries, category filtering, and empty query behavior.
- **Solution**: Replaced client-side list filtering with reactive SQLite database queries emitting via Kotlin `Flow`, enabling instant search with Room invalidation tracking.

---

### Story 9.2: State Management (Domain Layer)
- **Summary**: Created decoupled ViewModels for Master Cost List and Unit Management to manage reactive search state (`searchQuery` and `categoryFilter`) and expose auto-updating `StateFlow` lists.
- **Technical Files Modified**:
  - [`MasterCostListViewModel.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/MasterCostListViewModel.kt): Encapsulates search query state, ingredient list flow (`flatMapLatest`), and active toggle/restore operations.
  - [`UnitManagementViewModel.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/UnitManagementViewModel.kt): Combines `searchQuery` and `categoryFilter` (`BaseUnitType?`) to reactively drive database queries.
  - [`StateManagementSearchViewModelTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/StateManagementSearchViewModelTest.kt): Verified query updates, category switching, and CRUD operations.
- **Solution**: Emits UI state via `StateFlow` with `SharingStarted.Lazily` on `Dispatchers.IO`. Provides constructor injection for `CoroutineScope` to enable testing in JVM test runners without blocking coroutine deadlocks.

---

### Story 9.3: Toolbar Search UI (Presentation Layer)
- **Summary**: Integrated Android `SearchView` into the top AppBars of Master Cost List and Unit Management screens, wiring user text input directly to ViewModel state flows.
- **Technical Files Modified**:
  - [`menu_master_cost.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/menu/menu_master_cost.xml): Added `action_search_master_cost` with `app:actionViewClass="androidx.appcompat.widget.SearchView"` and `app:showAsAction="always|collapseActionView"`.
  - [`strings.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/values/strings.xml): Added `search_ingredients` string resource.
  - [`MasterCostListActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/MasterCostListActivity.kt): Initialized `MasterCostListViewModel`, collected `viewModel.ingredients`, and connected `SearchView.OnQueryTextListener`.
  - [`UnitManagementActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/UnitManagementActivity.kt): Initialized `UnitManagementViewModel`, wired category chips and `SearchView` text changes directly to ViewModel flows.
- **Solution**: Removed manual in-memory filtering and manual `loadMasterIngredients()` calls. The UI reactively updates whenever the user types or whenever the database changes.

---

### Story 8.1: Global Currency Localization to ₹ (Domain/UI Layer)
- **Summary**: Localized currency globally from `$` to Indian Rupee (`₹` / `INR`) across all screens, dialogs, summaries, and cost calculation engines.
- **Technical Files Modified**:
  - [`CostCurrencyFormatter.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/cost/CostCurrencyFormatter.kt): Centralized currency formatting utility with `DEFAULT_CURRENCY_SYMBOL = "₹"` and `DEFAULT_CURRENCY_CODE = "INR"`.
  - [`LiveCostCalculator.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/cost/LiveCostCalculator.kt): Formatted total and line-item costs with centralized currency helper.
  - [`MasterCostListActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/MasterCostListActivity.kt): Formatted prices and updated currency prefix in dialog inputs.
  - [`dialog_edit_master_ingredient.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/layout/dialog_edit_master_ingredient.xml): Updated input prefix label to `₹`.
  - [`CostCurrencyFormatterTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/CostCurrencyFormatterTest.kt): Unit test coverage for zero, negative, large values, and custom symbols.
- **Solution**: Replaced hardcoded `$` symbols with standard `CostCurrencyFormatter.formatCost()` calls.

---

### Story 8.2: Line-Item Cost Resolution (Domain Layer)
- **Summary**: Extended `LiveCostCalculator` to resolve individual line-item costs alongside total recipe cost, calculating prices for both primary ingredients and potential substitutes.
- **Technical Files Modified**:
  - [`LiveCostCalculator.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/cost/LiveCostCalculator.kt): Added `IngredientCostItem` model and `RecipeCostBreakdown.lineItemCosts` map.
  - [`LineItemCostResolutionTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/LineItemCostResolutionTest.kt): Verified line cost calculations, substitute pricing resolution, and missing price fallbacks.
- **Solution**: Emitted structured cost breakdowns containing resolved cost per line item, formatted cost strings, and unit pricing factors.

---

### Story 8.3: Line-Item Cost UI Rendering (Presentation Layer)
- **Summary**: Displayed end-aligned line costs across each ingredient item in the Recipe Viewer screen.
- **Technical Files Modified**:
  - [`RecipeActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeActivity.kt): Updated `IngredientDisplay` Compose layout to render end-aligned price tags (e.g. `₹7,000.00`).
- **Solution**: Wired Compose state observation so that selecting alternative substitutes immediately recalculates and updates line-item prices and the total recipe cost badge.

---

### Story 7.1: Multilingual Unit Normalization Engine (Domain Layer)
- **Summary**: Built a conversion engine to normalize measurements across Mass, Volume, and Count, supporting English and Hindi culinary terms.
- **Technical Files Modified**:
  - [`UnitConverterEngine.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/cost/UnitConverterEngine.kt): Core conversion engine with base units (`kg`, `liter`, `count`).
  - [`UnitConverterEngineTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/UnitConverterEngineTest.kt): Validated metric, imperial, and local unit conversions (e.g. `1 डब्बा = 15 kg`, `1 चम्मच = 15 ml`).
- **Solution**: Standardized normalization using unit aliases and conversion multipliers against master ingredient base units.

---

### Story 7.2: Custom Unit Management Screen (Presentation Layer)
- **Summary**: Implemented management screen allowing users to create, edit, delete, and test custom unit aliases with category filtering.
- **Technical Files Modified**:
  - [`UnitManagementActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/UnitManagementActivity.kt): Activity handling unit alias listings, search, and filtering.
  - [`activity_unit_management.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/layout/activity_unit_management.xml), [`dialog_edit_unit_alias.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/layout/dialog_edit_unit_alias.xml), [`item_unit_alias.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/layout/item_unit_alias.xml): Layouts with Material 3 styling and live preview formulas.
- **Solution**: Full CRUD support for `UnitAliasEntity` with category chips (`Mass`, `Volume`, `Count`) and instant database sync.

---

### Story 7.3: Unit Configuration Export & Import
- **Summary**: Integrated unit alias backup/restore both independently and bundled with master cost configurations.
- **Technical Files Modified**:
  - [`CostConfigBackupManager.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/cost/CostConfigBackupManager.kt)
  - [`RecipeRepository.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/RecipeRepository.kt)
  - [`menu_unit_management.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/menu/menu_unit_management.xml)
- **Solution**: Storage Access Framework integration (`CreateDocument`, `OpenDocument`) with JSON payloads serialized via Moshi.

---

### Story 6.1 - 6.3: Ingredient Substitutions with Rule of One
- **Summary**: Supported ingredient alternatives with schema migration, costing logic where selecting a substitute replaces the parent cost, and UI with OR dividers and radio selection.
- **Technical Files Modified**:
  - [`RecipeRoomDatabase.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/RecipeRoomDatabase.kt) (Migration 12 to 13), [`13.json`](file:///workspace/recipeMaker/Tournant/app/schemas/com.herohiman.tournant.data.room.RecipeRoomDatabase/13.json)
  - [`LiveCostCalculator.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/cost/LiveCostCalculator.kt)
  - [`RecipeActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeActivity.kt), [`IngredientEditingAdapter.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/adapter/IngredientEditingAdapter.kt)
- **Solution**: Maintained tree structure using `parentIngredientPosition` and `isSubstitute`, preventing double-counting in batch cost totals.

---

## 2. Resolved Bugs

### Bug 1: Layout Inflation Crash in Master Ingredient Edit Dialog
- **Summary**: `IllegalStateException` / inflation crash occurred when opening the add/edit master ingredient dialog due to conflicting parent view attachment.
- **Technical Files Modified**:
  - [`MasterCostListActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/MasterCostListActivity.kt)
  - [`dialog_edit_master_ingredient.xml`](file:///workspace/recipeMaker/Tournant/app/src/main/res/layout/dialog_edit_master_ingredient.xml)
- **Exact Solution**: Removed redundant root attachment in `LayoutInflater.inflate(R.layout.dialog_edit_master_ingredient, null, false)` and resolved TextInputLayout ID scoping conflicts.

---

### Bug 2: Room SQL Generation Failure for getRecipesUsingMasterIngredient
- **Summary**: Room failed to generate query code because `getRecipesUsingMasterIngredient` was declared with a default body rather than an abstract method in `RecipeDao`.
- **Technical Files Modified**:
  - [`RecipeDao.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/RecipeDao.kt)
  - All test DAO fakes (`CostConfigBackupPayloadTest.kt`, `RoomCostMigrationAndDaoTest.kt`, etc.)
- **Exact Solution**: Declared `abstract suspend fun getRecipesUsingMasterIngredient(...)` annotated with `@Query` so Room KSP generated the concrete query execution.

---

### Bug 3: Reverse-Lookup Query Strictness on Formatted Ingredient Names
- **Summary**: Recipes using ingredients with slight whitespace differences or unmerged casing failed reverse-lookup checks.
- **Technical Files Modified**:
  - [`RecipeDao.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/RecipeDao.kt)
  - [`RecipeRepository.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/RecipeRepository.kt)
  - [`ReverseLookupRecipeDaoTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/ReverseLookupRecipeDaoTest.kt)
- **Exact Solution**: Enhanced query using SQL `TRIM(LOWER(item)) = TRIM(LOWER(:ingredientName))` alongside linked `masterIngredientId` checks.

---

### Bug 4: Single-Thread Coroutine Deadlock in ViewModel Unit Tests
- **Summary**: Unit tests evaluating `stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = emptyList())` inside `runBlocking` deadlocked because the single-threaded event loop blocked the emission of upstream query values.
- **Technical Files Modified**:
  - [`MasterCostListViewModel.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/MasterCostListViewModel.kt)
  - [`UnitManagementViewModel.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/UnitManagementViewModel.kt)
  - [`StateManagementSearchViewModelTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/StateManagementSearchViewModelTest.kt)
- **Exact Solution**: Permitted optional `CoroutineScope` injection in ViewModel constructors defaulting to `viewModelScope`, allowing unit tests to supply `CoroutineScope(Dispatchers.IO)` so upstream database emissions execute concurrently.

---

### Bug 5: Main-Thread Database Access and Null Dereference Crash on Opening Recipes
- **Summary**: When opening any recipe detail screen (`RecipeActivity`), the app crashed immediately with `IllegalStateException: Cannot access database on the main thread` and `NullPointerException` on sub-recipe and ingredient lookups.
- **Technical Files Modified**:
  - [`RecipeViewModel.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeViewModel.kt): Added `.flowOn(Dispatchers.IO)` and error catching to `recipeCostBreakdown` Flow.
  - [`RecipeDao.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/RecipeDao.kt) & [`RecipeRepository.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/data/room/RecipeRepository.kt): Changed `getRecipeTitleById(id: Long)` to return `String?`.
  - [`MainViewModel.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/MainViewModel.kt) & [`RecipeLinkingManager.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/elements/RecipeLinkingManager.kt): Handled null recipe titles with safe elvis defaults `?: ""`.
  - [`RecipeActivity.kt`](file:///workspace/recipeMaker/Tournant/app/src/main/java/com/herohiman/tournant/ui/RecipeActivity.kt): Safeguarded ingredient item strings and typography extraction outside of Compose lambda limitations.
  - [`RecipeViewerLineCostUiTest.kt`](file:///workspace/recipeMaker/Tournant/app/src/test/java/com/herohiman/tournant/RecipeViewerLineCostUiTest.kt): Added unit test validating resilient handling of null items and missing sub-recipes.
- **Exact Solution**: Prevented Compose `collectAsState` on the main thread from invoking synchronous DAO queries during sub-recipe resolution by shifting flow computation to `Dispatchers.IO`, and added null-safety fallbacks across all referenced recipe lookups.

