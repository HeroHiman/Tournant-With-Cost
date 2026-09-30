# Completed Stories and Bug Fixes Archive

This archive provides technical documentation for all recently implemented stories, architectural improvements, and bug fixes in Tournant.

---

## 1. Implemented Stories

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
