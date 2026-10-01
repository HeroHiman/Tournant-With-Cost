# Tournant Feature & Implementation Progress

This document tracks the implementation status of all feature epics, stories, and bug fixes for Tournant.

---

## Overall Status: 100% Complete

| Epic / Feature Module | Status | Verified Tests |
| :--- | :---: | :---: |
| **Epic 1: Master Cost Foundation & Live Calculator** | **100% Complete** | `RoomCostMigrationAndDaoTest`, `LiveCostCalculatorTest` |
| **Epic 2: Ingredient Deduplication & Merge** | **100% Complete** | `IngredientAliasMigrationAndDaoTest`, `MergeIngredientUseCaseTest` |
| **Epic 3: Sub-Recipe Yield & Cost Derivation** | **100% Complete** | `SubRecipeLinkMigrationAndDaoTest`, `SubRecipeYieldConfigurationTest` |
| **Epic 4: Cross-Reference Usage & Reverse-Lookup** | **100% Complete** | `ReverseLookupRecipeDaoTest` |
| **Epic 5: Configuration Backup & Restore** | **100% Complete** | `CostConfigBackupPayloadTest`, `StorageAccessFrameworkTest` |
| **Epic 6: Ingredient Substitutions with Rule of One** | **100% Complete** | `IngredientSubstitutionSchemaTest`, `IngredientSubstitutionUiTest` |
| **Epic 7: Multilingual Unit Normalization & Custom Aliases** | **100% Complete** | `UnitConverterEngineTest`, `UnitManagementUiTest` |
| **Epic 8: Line-Item Costing & Currency Localization (₹)** | **100% Complete** | `CostCurrencyFormatterTest`, `LineItemCostResolutionTest` |
| **Epic 9: Smart Search Integration** | **100% Complete** | `ReactiveSearchQueriesTest`, `StateManagementSearchViewModelTest` |
| **Epic 11: Recipe Metadata & Cost Exclusions** | **100% Complete** | `InformationalCostExclusionTest`, `YieldParserTest` |
| **Epic 12: Modern Recipe UI & Output Dashboard** | **100% Complete** | `RecipeViewerLineCostUiTest`, `YieldParserTest` |
| **Epic 14: Quick-Edit Pricing Integration** | **100% Complete** | `RecipeViewerLineCostUiTest` |
| **Epic 15: Commercial Kitchen & UX Enhancements** | **100% Complete** | `RecipeViewerLineCostUiTest` |

---

## Detailed Story Checklist

### Epic 15: Commercial Kitchen & UX Enhancements
- [x] **Story 15.1: In-Line Unpriced Item Indicator & Quick-Map Chip (Presentation & Domain Layer)**
  - Prominent tapable badge `[+ Add Cost / Map]` on unpriced / unmapped ingredient rows in `IngredientDisplay`
  - Interactive bottom dialog `QuickMapOrPriceDialog` with smart suggestion list of existing master ingredients
  - 1-tap alias binding via `RecipeViewModel.bindIngredientAlias(...)` to immediately resolve ingredients and trigger live cost updates
  - Inline new master price entry (₹/unit) directly within the dialog, saving to Room and auto-linking the alias
- [x] **Story 15.2: Interactive Batch Scaler Bottom Sheet with Common Multipliers (Presentation Layer)**
  - Added dedicated interactive trigger to the Output Summary Dashboard (tapping Total Output tile or `Scale ⚡` button)
  - Sleek modal dialog `BatchScalerDialog` with quick preset multiplier chips (`0.5x`, `1.0x`, `2.0x`, `5.0x`, `10.0x`)
  - Live side-by-side before-and-after comparison showing target yield and recalculated batch cost before applying
  - Single-tap application integrating seamlessly with `RecipeViewModel.scaleByMultiplier(...)`
- [x] **Story 15.3: Recipe Cook Mode Full-Screen Focus with Cost Masking & Step Checkoffs (Presentation Layer)**
  - 1-tap Cook Mode toggle in AppBar (`action_cook_mode` with `ic_restaurant` icon) and recipe overview card
  - Full-screen wake lock (`FLAG_KEEP_SCREEN_ON`) acquired when Cook Mode is active to prevent display timeout
  - Employee privacy financial masking: line-item costs, total batch cost, and cost per kg masked with `••••`
  - High-contrast typography (`18.sp` bold amounts, `17.sp` names) with large 28dp checkboxes and strike-through checkoffs
  - Prominent pinned operational notes banner at top of ingredients list for tray sizes and packaging details

### Epic 14: Quick-Edit Pricing Integration
- [x] **Story 14.1: Row Long-Press Listener & Price Badge Click (Presentation Layer)**
  - Attached long-press gesture to recipe ingredient rows (`IngredientDisplay`) in `RecipeActivity.kt`
  - Connected click / long-click listeners directly to ingredient price badges (`Surface`) and `+ ₹` unpriced indicators
  - Resolved `IngredientCostItem` and `MasterIngredientEntity` to trigger quick-edit dialog without leaving the Recipe Viewer
- [x] **Story 14.2: Master Data Retrieval (Domain Layer)**
  - Added `resolveMasterIngredient` in `RecipeViewModel` querying Room database by ID, exact name, or alias
  - Added `getRecipeTitlesWithIds` to supply sub-recipe autocomplete options
- [x] **Story 14.3: Dialog Injection & Live Recalculation (Presentation Layer)**
  - Reused `dialog_edit_master_ingredient.xml` directly over the Recipe Viewer
  - Built `saveMasterIngredient` in `RecipeViewModel` persisting to Room via `RecipeRepository` and linking aliases
  - Leveraging active Room `Flow` to automatically recalculate line item costs, total batch cost, and cost per kg live on save

### Epic 12: Modern Recipe UI & Output Dashboard
- [x] **Story 12.1: Fix Layout Spacing Bug (Presentation Layer)**
  - Replaced stretching FlexboxLayout with direct LinearLayout container
  - Snapped ingredients ComposeView and instructions card directly beneath header
- [x] **Story 12.2: Output Summary Dashboard & Math Correction (Domain & Presentation Layer)**
  - Built prominent `OutputSummaryDashboard` card showing Total Output, Batch Cost, and Cost / kg
  - Implemented Automatic Sum of Active Ingredient Weights (e.g. 10 kg + 9 kg = 19 kg -> ₹391.16 / kg)
  - Implemented Explicit Mass Yield Override for evaporation (e.g. 17 kg -> ₹437.18 / kg)
  - Integrated interactive batch scaling controls directly within the dashboard
- [x] **Story 12.3: Modernize Ingredient Rows & Action Layout Fix (Presentation Layer)**
  - Fixed "Ingredients" title wrapping onto two lines with single line constraints and padding tuning
  - Resolved yield text truncation ("1 कि...") by splitting `OutputSummaryDashboard` interactive section into two dedicated sub-rows:
    - Row 1: Full-width yield title, input field, and unit description (`maxLines = 2`, `weight(1f)`) preventing ellipsis on long unit strings
    - Row 2: Dedicated scaling action sub-row with `[⚡ Batch Scaler]` pill button and grouped `[↺ 1x]`, `[ - ]`, `[ + ]` touch controls (30dp)
  - Expanded ingredient list to full width and styled line-item price badges with subtle container badges

### Epic 11: Recipe Metadata & Cost Exclusions
- [x] **Story 11.1: Informational Cost Exclusion (Domain & Schema Layer)**
  - `isInformationalOnly: Boolean = false`, `noteTitle: String?`, `noteValue: String?` in `IngredientEntity` (Room migration 13 to 14)
  - `CostStatus.INFORMATIONAL_EXCLUDED` in `LiveCostCalculator` with 0.0 line cost and zero unpriced errors
- [x] **Story 11.2: Title & Tray Size Metadata UI (Presentation Layer)**
  - Toggle between ingredient mode and informational mode in recipe editor
  - Display Title (`noteTitle`) and Note (`noteValue`) text fields with "जानकारी" badge
- [x] **Story 11.3: YieldParser & "Cost per kg" Metric (Domain & Presentation Layer)**
  - Multi-pattern parser supporting parentheses `1 टीपा काजू (10 किलो)`, Devanagari numerals, and direct mass units
  - Real-time `Cost per kg: ₹...` calculation and display in Recipe Viewer header
- [x] **Story 11.4: Group-Level Informational Exclusion Zones (Data, Domain & UI Layer)**
  - Added `isInformationalGroup` flag to `IngredientGroupTitle` and auto-cascading in `addGroupTitles()` / `hideGroupTitles()`
  - Filtered informational group items (e.g. *टोटल माल*, *जानकारी*, *डब्बा*) out of `activeIngredients` in `LiveCostCalculator`, keeping Total Output true to raw materials (10 + 9 = 19 kg instead of 57 kg)
  - Assigned `CostStatus.INFORMATIONAL_EXCLUDED` with ₹0.00 line cost, eliminating false unpriced errors
  - Supported group header "Mark as Informational (जानकारी)" popup toggle with visual badge in Recipe Editor
  - Suppressed `[+ Add Cost / Map]` button and price badge for informational items in Recipe Viewer
  - Stripped redundant "जानकारी:" prefix from adapter mapping, item storage, and viewer row display

### Epic 9: Smart Search Integration
- [x] **Story 9.1: Reactive Search Queries (Data Layer)**
  - Room SQL `LIKE '%' || :query || '%'` queries for master ingredients and unit aliases
  - English and Hindi partial matching across names, categories, and aliases
  - Non-null Flow delegation in `RecipeRepository`
- [x] **Story 9.2: State Management (Domain Layer)**
  - Decoupled `MasterCostListViewModel` with `StateFlow` and `flatMapLatest`
  - Decoupled `UnitManagementViewModel` combining search query and category filter
  - Constructor injection of `CoroutineScope` for JVM testing
- [x] **Story 9.3: Toolbar Search UI (Presentation Layer)**
  - `SearchView` in `MasterCostListActivity` AppBar
  - `SearchView` in `UnitManagementActivity` AppBar with instant filtering
  - Reactive list updates and empty-state visibility handling

### Epic 8: Line-Item Costing & Currency Localization
- [x] **Story 8.1: Global Currency Localization to ₹ (Domain/UI Layer)**
  - Centralized `CostCurrencyFormatter` (`DEFAULT_CURRENCY_SYMBOL = "₹"`, `DEFAULT_CURRENCY_CODE = "INR"`)
  - Updated dialogs, summary cards, and price inputs to Indian Rupee
- [x] **Story 8.2: Line-Item Cost Resolution (Domain Layer)**
  - Computed per-ingredient cost resolution and potential substitute pricing
  - Structured breakdown model `RecipeCostBreakdown`
- [x] **Story 8.3: Line-Item UI Rendering (Presentation Layer)**
  - End-aligned cost badges on recipe ingredients in Recipe Viewer
  - Instant price recalculation upon substitute radio toggles

### Epic 7: Multilingual Unit Normalization & Custom Aliases
- [x] **Story 7.1: Multilingual Unit Normalization Engine (Domain Layer)**
  - Conversion engine across Mass (`kg`), Volume (`liter`), and Count (`units`)
  - English and Hindi culinary unit dictionary (e.g. `किलो`, `डब्बा`, `चम्मच`)
- [x] **Story 7.2: Custom Unit Management Screen (Presentation Layer)**
  - Full CRUD screen for unit aliases (`UnitManagementActivity`)
  - Category chip filter (`Mass`, `Volume`, `Count`) and live formula preview
- [x] **Story 7.3: Unit Configuration Export & Import**
  - SAF-backed JSON export and import for unit dictionaries

### Epic 6: Ingredient Substitutions with Rule of One
- [x] **Story 6.1: Substitution Database Schema & Migration**
  - Room Migration 12 to 13 (`parentIngredientPosition`, `isSubstitute`)
- [x] **Story 6.2: Substitution Costing Engine (Rule of One)**
  - Active substitute replaces parent cost without double counting
- [x] **Story 6.3: Substitution UI & Radio Toggles**
  - Radio button selection with visual "OR" dividers in ingredient lists

### Epic 5: Configuration Backup & Restore
- [x] **Story 5.1: Payload Schema & Atomic Transactional Operations**
  - `CostConfigBackupPayload` bundling master costs, unit aliases, and ingredient aliases
- [x] **Story 5.2: Moshi Serialization & Backup Manager**
  - Stream-based JSON I/O via `CostConfigBackupManager`
- [x] **Story 5.3: Storage Access Framework UI Integration**
  - System file picker for backup and restore

### Epic 4: Cross-Reference Usage & Reverse-Lookup
- [x] **Story 4.1: Reverse-Lookup Query for Master Ingredient Usage**
- [x] **Story 4.2: Usage Bottom Sheet UI**
- [x] **Story 4.3: Deep Linking to Recipe Details**

### Epic 3: Sub-Recipe Yield & Cost Derivation
- [x] **Story 3.1: Sub-Recipe Link Schema & Migration**
- [x] **Story 3.2: Derived Cost Engine with Yield Normalization**
- [x] **Story 3.3: Sub-Recipe Selector & Derived Cost Indicator UI**

### Epic 2: Ingredient Deduplication & Merge
- [x] **Story 2.1: Ingredient Alias Database Schema & Migration**
- [x] **Story 2.2: Merge Use Case & Cascade Operations**
- [x] **Story 2.3: Merge Dialog UI & Selection Flow**

### Epic 1: Master Cost Foundation & Live Calculator
- [x] **Story 1.1: Room Database Schema & DAO**
- [x] **Story 1.2: Live Cost Calculator Core Engine**
- [x] **Story 1.3: Master Cost List UI & Soft-Delete Support**
