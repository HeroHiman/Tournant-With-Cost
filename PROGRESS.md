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

---

## Detailed Story Checklist

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
