---
title: "PRD: Live Cost Calculator inside Individual Recipes"
created: 2026-09-03
updated: 2026-09-03
status: draft
---

# PRD: Live Cost Calculator inside Individual Recipes (Tournant)

## 0. Document Purpose
This document specifies the functional requirements, architectural constraints, data contracts, and user interactions for the **Live Cost Calculator** feature in the Tournant Android application. It serves as the single source of truth for downstream architectural design, Room database migrations, ViewModel state machines, and QA test matrices.

---

## 1. Vision
Tournant empowers commercial and artisanal kitchens (e.g., confectionery, bakeries, cloud kitchens) to calculate real-time batch production costs accurately and effortlessly. By decoupling raw ingredient market prices into a centralized Master Price Book and coupling recipes to standard units, kitchens can instantly plan daily production runs at any target yield (e.g., 20 kg of Khova or 150 pastries) with real-time material costing and overhead accounting. 

Crucially, the system protects sensitive proprietary profit margins through a one-tap **Front-of-House (FOH) mode**, ensuring kitchen floor staff only view operational quantities, preparation steps, and cooking instructions—with zero financial leakages during cooking or clipboard sharing.

---

## 2. Target User & Key User Journeys

### 2.1 Jobs To Be Done (JTBD)
- **Production Planning**: "When market raw material prices fluctuate, I want to update the price once in a master registry so that all my recipes instantly reflect current production costs without manual re-entry."
- **Batch Scaling**: "When planning daily production runs of varying batch sizes (e.g., 5 kg vs 25 kg), I want to type the target yield and instantly view scaled ingredient weights and total funds required."
- **Staff Kitchen Safety**: "When handing recipes to kitchen staff or copying preparation cards to WhatsApp/clipboard, I want to suppress all price columns and profit-sensitive margins with a single tap."

### 2.2 Key User Journeys

#### UJ-1: Rajesh Updates Raw Material Prices in Master Inventory
- **Persona & Context**: Rajesh, owner of a sweet shop, reviews weekly supplier invoices.
- **Entry State**: Authenticated in Tournant, opens the Navigation Drawer.
- **Path**:
  1. Taps **Master Inventory**.
  2. Selects **Pistachio** from the list.
  3. Edits the current rate from ₹800/kg to ₹850/kg and taps Save.
- **Climax**: Room DB updates `MasterIngredientEntity`; reactive StateFlow triggers instantaneous cost updates across all 15 recipes containing Pistachio.
- **Resolution**: Rajesh opens *Pistachio Barfi* and sees the updated cost per kg immediately reflected.

#### UJ-2: Chef Anita Scales Daily Production for a Catering Order
- **Persona & Context**: Anita, head chef, prepares for a wedding order requiring 20 kg of *Khova*.
- **Entry State**: Viewing the *Khova* recipe detail screen (base yield: 5 kg).
- **Path**:
  1. Taps the **Target Production** field in the recipe header.
  2. Types `20` (kg).
  3. The calculator computes a 4.0x scaling factor.
- **Climax**: Ingredients scale from 10 L of Milk $\rightarrow$ 40 L, yield-based gas overhead scales 4x, and the sticky bottom card highlights: Total Cost: ₹2,720 | Cost per Kg: ₹136.00.
- **Resolution**: Anita knows the exact ingredient pull and total cash outlay required for the batch.

#### UJ-3: Kitchen Hand Sunder Prepares Batch in Front-of-House Mode
- **Persona & Context**: Sunder, kitchen staff, needs the scaled recipe instructions without viewing wholesale procurement rates.
- **Entry State**: Anita opens the recipe on the kitchen tablet or shares via clipboard.
- **Path**:
  1. Anita taps the **Eye** icon in the top toolbar.
  2. The UI hides the bottom Sticky Cost Card, per-item `@ ₹/unit` labels, and the Overheads section (`View.GONE`).
  3. Anita taps **Copy to Clipboard**.
- **Climax**: The generated clipboard text formats strictly scaled ingredient weights and preparation steps, with financial figures stripped entirely.
- **Resolution**: Sunder executes the recipe accurately with zero exposure to financial data.

---

## 3. Glossary

- **Master Item (`MasterIngredientEntity`)**: A central raw material record (e.g., "Cashews", "Cow Milk") defining a base measurement unit and current procurement price.
- **Recipe Ingredient (`IngredientEntity`)**: A recipe-specific line item linked optionally to a `MasterItem` via `masterItemId` foreign key.
- **Unit Dimension**: The physical classification of measurement (`MASS`, `VOLUME`, `COUNT`) ensuring unit compatibility.
- **Base Unit**: Standardized unit for price quotation (`kg` for mass, `L` for volume, `pcs` for count).
- **Scaling Factor**: The multiplier $\frac{\text{Target Yield}}{\text{Base Yield}}$ applied to ingredient amounts and yield-variable overheads.
- **Recipe Overhead (`RecipeOverheadEntity`)**: Auxiliary production expenses categorised as `FIXED` (e.g. flat packaging), `PER_KG_YIELD` (e.g. commercial gas ₹15/kg), or `PER_HOUR_LABOR`.
- **Front-of-House (FOH) Mode**: Privacy view state (`isFinancialViewEnabled = false`) where all cost cards, price tags, and financial clipboard exports are masked.
- **Soft Delete**: Marking a master item `isActive = false` to retain foreign key references in historical recipes while removing it from autocomplete selectors.

---

## 4. Features & Functional Requirements

### 4.1 Master Inventory & Price Book Management
**Description:** A dedicated interface for managing raw materials, base measurement units, current procurement prices, and default wastage allowances.

#### FR-1: Master Inventory Listing & Search
- The system must provide a searchable RecyclerView list of all active master ingredients.
- Consequences: Each item displays name, base unit, and formatted currency price per unit.

#### FR-2: Add & Edit Master Ingredient
- Users can create and update raw materials with attributes: `name` (String), `baseUnit` (Dropdown: `kg`, `g`, `L`, `ml`, `pcs`), `pricePerBaseUnit` (Positive Double), and `defaultWastagePercent` (0–100%).
- Consequences: Saving immediately persists to Room DB and notifies all active observers.

#### FR-3: Soft-Delete & Foreign Key Integrity Protection
- Deleting a master ingredient linked to active recipes must prompt a confirmation and mark `isActive = false` (Soft Delete).
- Consequences: Soft-deleted items remain linked in existing recipes but are filtered out of autocomplete dropdowns for new ingredients. Direct SQLite cascade deletion is prevented via `ForeignKey.RESTRICT`.

---

### 4.2 Strict Unit Normalization & Ingredient Linking
**Description:** Structured unit selection and conversion matrix replacing free-text units for cost-linked ingredients.

#### FR-4: Structured Unit Selection in Recipe Editor
- In recipe editing mode, selecting or typing an ingredient autocompletes against active `MasterIngredient` records.
- When linked, the unit field restricts input to valid compatible units within the master item's `UnitDimension`:
  - `MASS`: `kg`, `g`, `mg`, `lb`, `oz`
  - `VOLUME`: `L`, `ml`, `cl`, `dl`, `fl oz`, `cup`, `tbsp`, `tsp`
  - `COUNT`: `pcs`, `units`, `doz`

#### FR-5: Unit Normalization Engine
- The math engine must convert any compatible recipe unit to the master base unit:
  $$\text{Normalized Amount} = \text{Amount} \times \text{ConversionFactor}(\text{Unit} \rightarrow \text{BaseUnit})$$
- Consequences: 250 g Cashews @ ₹800/kg normalizes to $0.25 \times 800 = ₹200.00$.
- Cross-dimension conversions (e.g., recipe unit `ml`, master unit `kg` for liquids) apply a default density factor of $1.0\text{ kg/L}$ unless a density override is specified.

---

### 4.3 Dynamic Live Cost & Yield Scaling Calculator
**Description:** Real-time mathematical engine calculating raw material costs, overheads, total batch funds, and cost per yield unit.

#### FR-6: Target Yield Scaling
- The recipe detail header must display a `Target Production` input defaulting to `Recipe.yield`.
- Editing target yield dynamically computes $\text{ScaleFactor} = \frac{\text{Target Yield}}{\text{Base Yield}}$.
- Consequences: Dynamically scales ingredient amounts, raw material subtotals, and yield-dependent overheads in real time without persisting changes unless saved.

#### FR-7: Recipe Overhead Management
- Recipes support attaching multiple overhead items categorized as:
  - `FIXED`: Constant flat cost (e.g. ₹50 box).
  - `PER_KG_YIELD`: Scaled by target yield weight (e.g. ₹15/kg gas).
  - `PER_HOUR_LABOR`: Scaled by preparation/cook time (e.g. ₹100/hr).

#### FR-8: Sticky Cost Summary Card
- The bottom of the recipe detail screen must render a sticky card displaying:
  - Total Raw Materials Cost
  - Total Overheads
  - Total Batch Cost (Funds Required)
  - Unit Cost Badge: Cost per Kg (or target yield unit)

---

### 4.4 Front-of-House (FOH) Masking & Sanitized Clipboard Export
**Description:** Privacy controls to prevent front-of-house kitchen workers from viewing cost calculations or profit margins.

#### FR-9: App Bar Privacy Toggle
- The top app bar of `RecipeActivity` must feature an Eye Action Icon (`ic_visibility_on` / `ic_visibility_off`).
- Toggling the icon switches `isFinancialViewEnabled` state stored in Android `DataStore`.

#### FR-10: View Visibility Behavior
- When `isFinancialViewEnabled == false`:
  - The Sticky Bottom Cost Card is set to `View.GONE`.
  - Right-aligned price texts (`@ ₹50/L = ₹500`) on ingredient list rows are set to `View.GONE`.
  - The entire Recipe Overheads section is set to `View.GONE`.
  - The Target Production yield scaler remains functional for quantity scaling.

#### FR-11: Sanitized Clipboard Copy
- When copying recipe text to the clipboard:
  - If `isFinancialViewEnabled == true`, append the financial breakdown and cost per kg summary.
  - If `isFinancialViewEnabled == false`, omit all cost numbers, rates, and overhead summaries, outputting only scaled ingredient amounts and cooking directions.

---

## 5. Non-Goals (Explicit)

- **Supplier Invoice OCR Scanning**: Automatic OCR import of physical vendor paper receipts is deferred to future releases.
- **Multi-Currency Real-Time FX Conversion**: Tournant will format using the device/user-selected currency symbol; live Forex rates are out of scope.
- **Inventory Stock Depletion / POS Integration**: Deducting current warehouse stock levels upon recipe completion is a future ERP module concern.

---

## 6. MVP Scope

### 6.1 In Scope (v1)
- Master Inventory Room entity, DAO, and CRUD management screen.
- Room migration `MIGRATION_8_9` with Foreign Key constraints.
- Multi-dimensional Unit Normalization Engine (`MASS`, `VOLUME`, `COUNT`).
- Reactive ViewModel `StateFlow` combining Room database queries with target yield scaling.
- Recipe detail UI with target yield editor, per-item subtotal pricing, overhead list, and sticky bottom cost card.
- Toolbar Eye toggle backed by DataStore with view masking and sanitized clipboard export.

### 6.2 Out of Scope for MVP
- Cloud sync of master price book across multiple distinct device accounts (handled via existing Tournant DB backup/sync mechanisms).
- Complex custom density tables per individual ingredient (fallback to $1.0\text{ kg/L}$ for fluid volume-to-mass).

---

## 7. Success Metrics

- **SM-1 (Calculation Latency)**: Recalculation of total batch cost and scaled ingredient quantities must complete in $< 16\text{ ms}$ (60 FPS UI smooth response) upon target yield change. *(Validates FR-6, FR-8)*
- **SM-2 (Zero Financial Leakage)**: 100% of price elements, overheads, and clipboard copies are scrubbed when FOH mode is active. *(Validates FR-9, FR-10, FR-11)*
- **SM-C1 (Counter-Metric)**: Kitchen preparation speed and readability must not degrade; standard recipe display without financial mode must remain as concise as the original Tournant recipe layout.

---

## 8. Assumptions Index

- `[ASSUMPTION 1]` Tournant uses Room SQLite version 8, requiring `MIGRATION_8_9` with auto-migration or explicit SQL scripts.
- `[ASSUMPTION 2]` Default currency symbol defaults to system locale currency (e.g. ₹ for India, $ for US, € for Europe) with an optional override in Tournant settings.
- `[ASSUMPTION 3]` Room `@Relation` will load `RecipeWithCostingData` efficiently in a single query flow.
