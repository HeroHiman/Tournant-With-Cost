---
stepsCompleted:
  - step-01-validate-prerequisites
  - step-02-design-epics
  - step-03-create-stories
  - step-04-final-validation
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-Tournant-2026-09-03/prd.md
  - _bmad-output/planning-artifacts/architecture/architecture-Tournant-2026-09-03/ARCHITECTURE-SPINE.md
  - _bmad-output/brainstorming/brainstorm-live-cost-calculator-2026-09-03/.memlog.md
---

# Tournant - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for the **Live Cost Calculator** feature in **Tournant**, decomposing the requirements from the PRD, Architecture Spine, and locked-in Edge Case specifications into implementable user stories.

## Requirements Inventory

### Functional Requirements

- **FR-1: Master Inventory Listing & Search**: Searchable RecyclerView list of all active master ingredients displaying name, base unit, and formatted currency price per unit.
- **FR-2: Add & Edit Master Ingredient**: Dialog/form interface to create and update raw materials with name, baseUnit (kg, g, L, ml, pcs), pricePerBaseUnit, and defaultWastagePercent.
- **FR-3: Soft-Delete & Foreign Key Integrity Protection**: Deleting a master ingredient linked to recipes blocks hard deletion (`ForeignKey.RESTRICT`) and flags `isActive = false` (soft delete), retaining historical recipe links while removing from new selectors.
- **FR-4: Structured Unit Selection & Dual Ingredient System**: Recipe editing supports Costed ingredients linked to MasterItem records with restricted compatible units per `UnitDimension` (MASS, VOLUME, COUNT), as well as Uncosted/free-text ingredients with visual distinction.
- **FR-5: Deterministic Unit Normalization Engine**: Pure Kotlin conversion matrix converting recipe units to master base unit; applies 1:1 density fallback ($1.0\text{ kg/L}$) for fluid liquids; prompts inline guidance (*"Specify grams for accurate costing"*) for invalid cross-conversions.
- **FR-6: Target Yield Scaling**: Recipe detail header features editable Target Production input; dynamically computes $\text{ScaleFactor} = \frac{\text{Target Yield}}{\text{Base Yield}}$; scales ingredient amounts, raw material costs, and yield-dependent overheads; falls back to 1x base yield on blank/zero/negative input.
- **FR-7: Recipe Overhead Management**: Add, edit, and delete overheads with name, cost/rate, and calculation type (`FIXED`, `PER_KG_YIELD`, `PER_HOUR_LABOR`); evaluates labor to ₹0 with warning indicator if recipe prepTime is missing.
- **FR-8: Sticky Cost Summary Card**: Fixed bottom card displaying Total Raw Materials, Total Overheads, Total Funds Required, and Unit Cost Badge (Cost per Kg); displays `⚠️ Total: ₹X (N items uncosted)` when uncosted ingredients are present.
- **FR-9: App Bar Privacy Toggle**: Action menu item with Eye Icon (`ic_visibility_on` / `ic_visibility_off`) in `RecipeActivity` top toolbar toggling `isFinancialViewEnabled` state.
- **FR-10: View Visibility Behavior**: When `isFinancialViewEnabled == false`, the sticky bottom cost card, overheads section, and ingredient unit price labels are set to `View.GONE`, while the target production yield scaler remains fully functional.
- **FR-11: Sanitized Clipboard Copy & Share**: Clipboard export checks `isFinancialViewEnabled`; when false, strips all cost columns, rates, overheads, and financial totals, outputting only scaled ingredient amounts and cooking instructions.

### NonFunctional Requirements

- **NFR-1 (Calculation Latency - SM-1)**: Recalculation of total batch cost and scaled ingredient quantities must complete in $< 16\text{ ms}$ (60 FPS UI smooth response) upon target yield change.
- **NFR-2 (Zero Financial Leakage - SM-2)**: 100% of price elements, overheads, and clipboard copies are scrubbed when FOH mode is active.
- **NFR-3 (Data Integrity & Crash Prevention)**: Foreign key constraints (`ForeignKey.RESTRICT`) prevent orphaned records; soft delete preserves historical recipe costing.
- **NFR-4 (State Persistence)**: `isFinancialViewEnabled` persists in AndroidX DataStore across device rotation, backgrounding, and app restarts.
- **NFR-5 (Testability & Architecture Purity)**: Calculation engine (`RecipeCostCalculator`, `UnitNormalizer`) must remain pure Kotlin functions testable with JUnit without Android framework mocks.
- **NFR-6 (Internationalization & Locale)**: All currency values format via `CurrencyFormatter` adhering to system locale or user override without hardcoded currency symbols.

### Additional Requirements

- **Room Database Migration 8 $\rightarrow$ 9**: Explicit `MIGRATION_8_9` in `RecipeRoomDatabase.kt` creating `MasterIngredient` table, `RecipeOverhead` table, and adding `masterItemId` column and index to `Ingredient` table; verified with Room Migration Test Suite.
- **Layered MVVM + Unidirectional Data Flow (UDF)**: Combine Room reactive flow, `targetYieldStateFlow`, and `visibilityDataStore.isFinancialViewEnabledFlow` in `RecipeViewModel` into an immutable `StateFlow<RecipeCostUiState>`.
- **Relational Composite Query**: Room `@Relation` query in `RecipeDao` fetching `RecipeWithCostingData` in a single query flow.

### UX Design Requirements

- **UX-DR1: Master Inventory Screen (`MasterInventoryActivity`)**: Material 3 layout with searchable RecyclerView, Floating Action Button (+) for adding ingredients, item click to edit dialog, and soft-delete toggle.
- **UX-DR2: Recipe Detail Header Target Production Card**: Prominent editable card (`Target Production: [ ___ ] Kg`) with real-time numeric validation, hint text, and fallback to base yield.
- **UX-DR3: Ingredient Row Cost Annotations**: Subdued secondary right-aligned text (`@ ₹50/L = ₹500`) next to quantity, setting `View.GONE` when FOH mode is active.
- **UX-DR4: Overheads Section & Add Dialog**: Expandable/clean section listing active overhead expenses with subtotal; "Add Overhead" button launching a dialog with Name, Amount, and Type (Per Kg, Fixed, Per Hour).
- **UX-DR5: Sticky Bottom Cost Summary Card**: Fixed bottom card with Material 3 elevation showing raw material subtotal, overhead subtotal, bold Total Cost, and highlighted Cost per Kg badge; includes `⚠️ Total (N items uncosted)` indicator when applicable.
- **UX-DR6: Toolbar Eye Action Toggle**: Top right action menu item toggling between `ic_visibility_on` and `ic_visibility_off` with smooth state transition and DataStore binding.

### FR Coverage Map

| Requirement | Title | Mapped Epic |
| :--- | :--- | :--- |
| **FR-1** | Master Inventory Listing & Search | **Epic 1: Raw Material Price Book & Inventory Management** |
| **FR-2** | Add & Edit Master Ingredient | **Epic 1: Raw Material Price Book & Inventory Management** |
| **FR-3** | Soft-Delete & Foreign Key Integrity Protection | **Epic 1: Raw Material Price Book & Inventory Management** |
| **FR-4** | Structured Unit Selection & Dual Ingredient System | **Epic 2: Recipe Ingredient Linking & Unit Normalization** |
| **FR-5** | Deterministic Unit Normalization Engine | **Epic 2: Recipe Ingredient Linking & Unit Normalization** |
| **FR-6** | Target Yield Scaling | **Epic 3: Live Batch Costing & Overhead Scaling** |
| **FR-7** | Recipe Overhead Management | **Epic 3: Live Batch Costing & Overhead Scaling** |
| **FR-8** | Sticky Cost Summary Card | **Epic 3: Live Batch Costing & Overhead Scaling** |
| **FR-9** | App Bar Privacy Toggle | **Epic 4: Front-of-House Privacy Mode & Sanitized Recipe Sharing** |
| **FR-10**| View Visibility Behavior (Masking) | **Epic 4: Front-of-House Privacy Mode & Sanitized Recipe Sharing** |
| **FR-11**| Sanitized Clipboard Copy & Share | **Epic 4: Front-of-House Privacy Mode & Sanitized Recipe Sharing** |

## Epic List

### Epic 1: Raw Material Price Book & Inventory Management
Establish and maintain a centralized catalog of raw materials, configure base measurement units, manage current procurement prices, and safely soft-delete ingredients without corrupting existing recipes.
**FRs covered:** FR-1, FR-2, FR-3

### Epic 2: Recipe Ingredient Linking & Unit Normalization
Link recipe ingredients to central master items using structured, compatible measurement units or leave non-standard items uncosted, with an automated normalization engine converting units and applying fluid density fallbacks.
**FRs covered:** FR-4, FR-5

### Epic 3: Live Batch Costing & Overhead Scaling
Enter any daily target production yield to instantly calculate scaled ingredient weights, attach customizable overhead expenses (fuel, labor, packaging), and view real-time batch production costs and cost-per-kg in a sticky summary card.
**FRs covered:** FR-6, FR-7, FR-8

### Epic 4: Front-of-House Privacy Mode & Sanitized Recipe Sharing
Safely hand tablets to kitchen floor staff or copy recipe cards to clipboard with a one-tap privacy toggle that suppresses all ingredient prices, overhead expenses, and profit margins on screen and in text exports.
**FRs covered:** FR-9, FR-10, FR-11

---

## Epic 1: Raw Material Price Book & Inventory Management

Establish and maintain a centralized catalog of raw materials, configure base measurement units, manage current procurement prices, and safely soft-delete ingredients without corrupting existing recipes.

### Story 1.1: Room Database Schema & Migration 8 -> 9 for Master Inventory

As a Developer / Kitchen Owner,  
I want the Room database migrated from version 8 to 9 with the `MasterIngredient` table, indices, and DAO,  
So that raw ingredient procurement rates are stored centrally with verified data integrity.

**Acceptance Criteria:**

**Given** a Room database at schema version 8 with existing recipes,  
**When** `MIGRATION_8_9` executes,  
**Then** the `MasterIngredient` table is created with columns: `id` (INTEGER PK AUTOINCREMENT), `name` (TEXT NOT NULL), `baseUnit` (TEXT NOT NULL), `pricePerBaseUnit` (REAL NOT NULL), `defaultWastagePercent` (REAL NOT NULL DEFAULT 0.0), `isActive` (INTEGER NOT NULL DEFAULT 1), and `updatedAt` (INTEGER NOT NULL).  
**And** `masterItemId` (INTEGER nullable) column and index are added to `Ingredient`.  
**And** all existing recipes and ingredients are 100% preserved without corruption or data loss.  
**And** `MasterIngredientDao` provides reactive `Flow<List<MasterIngredientEntity>>` of active ingredients, CRUD operations (`insert`, `update`, `softDelete`, `getById`), and `countRecipeUsages(masterIngredientId: Int): Int`.

### Story 1.2: Master Inventory Screen & Search

As a Kitchen Manager,  
I want a dedicated Master Inventory screen with real-time search,  
So that I can quickly browse all raw materials and inspect current rates.

**Acceptance Criteria:**

**Given** the user opens the Navigation Drawer,  
**When** "Master Inventory" is selected,  
**Then** `MasterInventoryActivity` launches displaying a Material 3 `RecyclerView` observing active ingredients from `MasterIngredientDao`.  
**Given** the Master Inventory list,  
**When** viewed,  
**Then** each row displays the Ingredient Name on the left, and the formatted price per base unit (e.g. `₹800 / kg` via `CurrencyFormatter`) on the right.  
**Given** the search input field in the toolbar,  
**When** the user types a query (e.g., "Cashew"),  
**Then** the list filters dynamically in real time matching ingredient names case-insensitively.

### Story 1.3: Add & Edit Master Ingredient Dialog

As a Kitchen Owner,  
I want a dialog to add new raw materials and update existing market prices,  
So that supplier price fluctuations are immediately recorded.

**Acceptance Criteria:**

**Given** `MasterInventoryActivity`,  
**When** the user taps the Floating Action Button (+),  
**Then** an "Add Raw Material" dialog opens with fields: Name (text), Base Unit (dropdown: `kg`, `g`, `L`, `ml`, `pcs`), Price per Unit (positive decimal), and optional Default Wastage % (0–100%).  
**Given** valid inputs in the dialog,  
**When** the user taps Save,  
**Then** the entity is inserted into Room DB with `isActive = 1`, and the list updates immediately.  
**Given** an existing ingredient row in the list,  
**When** tapped,  
**Then** an "Edit Raw Material" dialog opens pre-populated with current values; saving updates `pricePerBaseUnit` and `updatedAt`.  
**Given** non-numeric, blank, or negative price input,  
**When** Save is tapped,  
**Then** an inline error is displayed and the database write is blocked.

### Story 1.4: Safe Soft-Deletion & Deactivation Protection

As a Kitchen Manager,  
I want deleting an in-use ingredient to soft-delete rather than crash or break recipes,  
So that existing recipes retain historical costing while retiring obsolete raw materials.

**Acceptance Criteria:**

**Given** a master ingredient with `countRecipeUsages > 0`,  
**When** the user selects Delete on that ingredient,  
**Then** a confirmation dialog alerts: *"This ingredient is used in recipes. Deactivate instead of deleting?"*.  
**Given** the user confirms deactivation,  
**When** processed,  
**Then** `isActive` is set to `false`, removing it from active search/dropdowns while preserving its record in Room DB.  
**Given** an unlinked master ingredient (`countRecipeUsages == 0`),  
**When** the user confirms deletion,  
**Then** it is permanently removed from the database via `MasterIngredientDao.delete()`.

---

## Epic 2: Recipe Ingredient Linking & Unit Normalization

Link recipe ingredients to central master items using structured, compatible measurement units or leave non-standard items uncosted, with an automated normalization engine converting units and applying fluid density fallbacks.

### Story 2.1: Pure Kotlin Unit Normalization Matrix & Fluid Density Engine

As a Developer / Kitchen Chef,  
I want a pure Kotlin `UnitNormalizer` conversion engine supporting `MASS`, `VOLUME`, and `COUNT` with a 1.0 kg/L fluid density fallback,  
So that ingredient quantities can be converted accurately into master base units for live costing calculations.

**Acceptance Criteria:**

**Given** two units within the same dimension (e.g. `250 g` to `kg`, `500 ml` to `L`, or `2 doz` to `pcs`),  
**When** `UnitNormalizer.normalize(amount, fromUnit, targetBaseUnit)` is called,  
**Then** it returns the exact mathematically normalized amount (e.g. `0.25 kg`, `0.5 L`, `24 pcs`).  
**Given** a cross-dimension conversion between `VOLUME` and `MASS` for culinary liquids (e.g. recipe specifies `500 ml` of milk, master base unit is `kg`),  
**When** normalized,  
**Then** it applies the standard liquid density fallback (1.0 g/ml = 1.0 kg/L) yielding `0.5 kg`.  
**Given** an invalid or incompatible conversion (e.g. dry solid cashews from `cup` to `kg`),  
**When** normalized,  
**Then** it returns a typed `NormalizationResult.Incompatible` carrying an actionable error message (*"Specify grams for accurate costing"*).  
**Given** the `UnitNormalizer` test suite,  
**When** run via JUnit 4 (`./gradlew testDebugUnitTest`),  
**Then** all unit conversion and density test cases pass with zero Android framework dependencies (`NFR-5`).

### Story 2.2: Recipe Ingredient Entity Linking & Relational DAO Queries

As a Developer,  
I want `IngredientEntity` linked to `MasterIngredientEntity` via `masterItemId` with composite Room query models,  
So that recipes can load their ingredients along with their linked master procurement rates in a single reactive query.

**Acceptance Criteria:**

**Given** `IngredientEntity`,  
**When** defined in Room,  
**Then** it contains `@ColumnInfo(name = "masterItemId") val masterItemId: Int? = null` with `ForeignKey.RESTRICT`.  
**Given** `RecipeDao`,  
**When** `getRecipeWithCostedIngredients(recipeId: Int)` is called,  
**Then** it returns a reactive `Flow<RecipeWithCostedIngredients>` combining the `RecipeEntity`, its `IngredientEntity` records, and their corresponding `MasterIngredientEntity` records.  
**Given** an ingredient linked to a master item,  
**When** `MasterIngredientDao.countRecipeUsages(masterItemId)` is called,  
**Then** it returns the count of linked ingredient rows across all recipes.

### Story 2.3: Autocomplete & Dual Ingredient System in Recipe Editor

As a Recipe Author,  
I want ingredient editing in `RecipeEditingActivity` to autocomplete against Master Inventory items and constrain units, while still allowing uncosted free-text items,  
So that I can easily cost important ingredients without friction when entering garnishes or pinches.

**Acceptance Criteria:**

**Given** the ingredient name input in `RecipeEditingActivity` / `IngredientEditingAdapter`,  
**When** the user types (e.g., "Cas..."),  
**Then** an autocomplete dropdown displays active master ingredients (`isActive = 1`) matching the text.  
**Given** the user selects a master ingredient from the dropdown,  
**When** selected,  
**Then** `masterItemId` is attached, and the Unit selector restricts options to compatible units within that item's `UnitDimension` (e.g. for `kg`, choices are `kg`, `g`, `mg`, `lb`, `oz`).  
**Given** the user types an ingredient not in the master inventory or a free-form item (e.g. "Pinch of Saffron"),  
**When** saved,  
**Then** `masterItemId` is saved as `null` (uncosted ingredient), allowing the recipe to save cleanly without errors.  
**Given** a linked ingredient with an incompatible unit,  
**When** the recipe is validated,  
**Then** an inline prompt (*"Specify grams for accurate costing"*) warns the user before saving.

---

## Epic 3: Live Batch Costing & Overhead Scaling

Enter any daily target production yield to instantly calculate scaled ingredient weights, attach customizable overhead expenses (fuel, labor, packaging), and view real-time batch production costs and cost-per-kg in a sticky summary card.

### Story 3.1: Recipe Overhead Schema, Entities & DAO (`RecipeOverheadEntity`)

As a Developer / Kitchen Manager,  
I want `RecipeOverheadEntity` and its DAO defined in Room DB with foreign key cascade to `RecipeEntity`,  
So that recipes can persist multiple overhead expenses with rates and calculation types.

**Acceptance Criteria:**

**Given** `RecipeRoomDatabase`,  
**When** `RecipeOverheadEntity` is defined,  
**Then** it contains columns: `id` (INTEGER PK AUTOINCREMENT), `recipeId` (INTEGER FK cascade to `Recipe.id`), `name` (TEXT NOT NULL), `type` (TEXT: `FIXED`, `PER_KG_YIELD`, `PER_HOUR_LABOR`), and `rate` (REAL NOT NULL), with an index on `recipeId`.  
**Given** `RecipeOverheadDao`,  
**When** queried,  
**Then** it provides reactive `Flow<List<RecipeOverheadEntity>>` by `recipeId`, and CRUD operations (`insert`, `update`, `delete`).  
**Given** an existing recipe is deleted from the database,  
**When** processed,  
**Then** all associated overhead entities are cascaded and deleted automatically.

### Story 3.2: Pure Kotlin Recipe Cost & Scaling Calculator Engine

As a Developer / Chef,  
I want a pure Kotlin `RecipeCostCalculator` that computes batch scale factor, ingredient subtotals, overhead amounts, and total cost per kg,  
So that calculation logic is fast (< 16 ms), accurate, and decoupled from UI lifecycles.

**Acceptance Criteria:**

**Given** a recipe's base yield, target yield, ingredients with unit prices, and overhead list,  
**When** `RecipeCostCalculator.compute()` is called,  
**Then** it calculates ScaleFactor = (Target Yield / Base Yield), scaled ingredient quantities, material subtotals, overhead costs, total funds required, and cost per kg.  
**Given** invalid target yield input (e.g. blank, 0, or negative),  
**When** computed,  
**Then** it safely falls back to a 1.0x base yield scale.  
**Given** an overhead with type `PER_HOUR_LABOR` where recipe prep time is 0 or unassigned,  
**When** computed,  
**Then** the labor cost evaluates to 0.0 and flags a warning indicator.  
**Given** uncosted ingredients present in the recipe,  
**When** computed,  
**Then** `uncostedItemCount` accurately reflects the count and total raw materials sum excludes them without crashing.  
**Given** the benchmark unit test suite for `RecipeCostCalculator`,  
**When** executed with a recipe of 50 ingredients,  
**Then** computation executes in < 5 ms (comfortably within the < 16 ms threshold for `NFR-1`).

### Story 3.3: Overheads UI & "Add Overhead" Dialog in Recipe

As a Kitchen Operator,  
I want to view and manage overhead expenses directly within the recipe screen,  
So that I can add specific utilities, packaging, or labor costs to any recipe.

**Acceptance Criteria:**

**Given** `RecipeActivity` (or editing view),  
**When** viewing the Overheads section,  
**Then** it displays a clean list of existing overheads with names, rate badges, and computed subtotals.  
**Given** the user taps the "+ Add Overhead" button,  
**When** clicked,  
**Then** a dialog appears with inputs for Expense Name (text), Cost/Rate (decimal), and Type (Radio/Dropdown: `Per Kg / Yield`, `Fixed per Batch`, `Per Hour Labor`).  
**Given** valid inputs,  
**When** Save is tapped,  
**Then** the overhead is inserted into Room DB and the list updates immediately.  
**Given** an existing overhead in the list,  
**When** the user taps edit or delete (trash icon),  
**Then** the record updates or is removed from the recipe.

### Story 3.4: Target Production Card & Sticky Bottom Cost Summary Card

As a Chef / Kitchen Manager,  
I want an interactive Target Production card and a Sticky Bottom Cost Card on the Recipe screen,  
So that I can dynamically plan production runs and observe live cost and ingredient changes.

**Acceptance Criteria:**

**Given** `RecipeActivity`,  
**When** opened,  
**Then** the header renders an editable `Target Production: [ ___ ] Kg` card defaulting to the recipe's base yield.  
**Given** the user types a new target yield (e.g., "20"),  
**When** entered,  
**Then** `RecipeViewModel` emits updated `RecipeCostUiState` in real time, updating all displayed ingredient quantities and row prices (`@ ₹50/L = ₹500`).  
**Given** the Sticky Bottom Cost Card,  
**When** rendered,  
**Then** it displays Raw Material Total, Overheads Total, bold Total Batch Cost, and a prominent Cost per Kg badge.  
**Given** uncosted ingredients in the recipe,  
**When** rendered,  
**Then** the card displays a visible warning badge: `⚠️ Total: ₹X (N items uncosted)`.

---

## Epic 4: Front-of-House Privacy Mode & Sanitized Recipe Sharing

Safely hand tablets to kitchen floor staff or copy recipe cards to clipboard with a one-tap privacy toggle that suppresses all ingredient prices, overhead expenses, and profit margins on screen and in text exports.

### Story 4.1: Persistent Privacy State in AndroidX DataStore (`VisibilityDataStore`)

As a Kitchen Owner,  
I want the Front-of-House privacy state to persist in AndroidX DataStore,  
So that costs remain hidden across device rotation, backgrounding, and app restarts until explicitly toggled.

**Acceptance Criteria:**

**Given** `VisibilityDataStore`,  
**When** injected into `RecipeViewModel`,  
**Then** it exposes `isFinancialViewEnabledFlow: Flow<Boolean>` defaulting to `true` (financial view active).  
**Given** the user toggles privacy mode,  
**When** `setFinancialViewEnabled(enabled: Boolean)` is called,  
**Then** the preference is written asynchronously to DataStore without blocking the UI thread.  
**Given** privacy mode is set to `false`,  
**When** the user rotates the device or restarts the app,  
**Then** `isFinancialViewEnabledFlow` emits `false` on launch, maintaining privacy mode persistence (`NFR-4`).

### Story 4.2: Recipe Activity Toolbar Eye Action Toggle & View Masking

As a Chef / Kitchen Owner,  
I want a top app bar Eye toggle icon that hides all prices, overheads, and total cost cards from the screen with one tap,  
So that kitchen floor staff cannot view proprietary margins or raw material costs.

**Acceptance Criteria:**

**Given** `RecipeActivity` top toolbar,  
**When** rendered,  
**Then** it displays an Eye action icon (`ic_visibility_on` when visible, `ic_visibility_off` when hidden).  
**Given** the user taps the Eye icon,  
**When** toggled off (`isFinancialViewEnabled == false`),  
**Then** the toolbar icon switches to `ic_visibility_off`,  
**And** the Sticky Bottom Cost Card is set to `View.GONE`,  
**And** the Recipe Overheads list container is set to `View.GONE`,  
**And** the right-aligned price texts (`@ ₹50/L = ₹500`) on ingredient rows are set to `View.GONE`,  
**And** the Target Production scaler remains fully visible and functional for quantity scaling.  
**Given** privacy mode is toggled back on,  
**When** clicked,  
**Then** all cost cards, overheads, and unit price labels are restored to `View.VISIBLE`.  
**Given** automated UI tests for `RecipeActivity`,  
**When** privacy mode is off,  
**Then** zero TextView elements contain currency symbols or financial figures (`NFR-2`).

### Story 4.3: Sanitized Clipboard Copy & Recipe Share

As a Kitchen Operator,  
I want copying a recipe to the clipboard to automatically strip all financial data when Front-of-House mode is active,  
So that sharing recipes with kitchen staff or external chats never leaks cost numbers.

**Acceptance Criteria:**

**Given** `RecipeActivity` with `isFinancialViewEnabled == false`,  
**When** the user taps "Copy to Clipboard" or "Share",  
**Then** the generated text contains only the recipe title, scaled target yield, scaled ingredient amounts, and preparation instructions, with all prices, rates, overheads, and totals completely omitted (`NFR-2`).  
**Given** `RecipeActivity` with `isFinancialViewEnabled == true`,  
**When** the user taps "Copy to Clipboard",  
**Then** the text appends the full financial breakdown, overhead expenses, and cost-per-kg summary.  
**Given** unit tests for `RecipeTextFormatter` (or clipboard generator),  
**When** executed with privacy mode enabled,  
**Then** output string verification confirms 0 occurrences of price tags, currency symbols, or overhead rate values.





