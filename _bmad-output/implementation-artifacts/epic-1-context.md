# Epic 1 Context: Raw Material Price Book & Inventory Management

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Establish and maintain the app's centralized Master Price Book — the single source of truth for raw material procurement prices and base units. Price changes made here flow reactively to every recipe that references the ingredient, so kitchens record supplier fluctuations once and see updated costs everywhere. This epic covers full CRUD management of master ingredients, real-time search, and safe retirement of obsolete items that cannot corrupt or orphan existing recipe data.

## Stories

- Story 1.1: Room Database Schema & Migration 8 -> 9 for Master Inventory
- Story 1.2: Master Inventory Screen & Search
- Story 1.3: Add & Edit Master Ingredient Dialog
- Story 1.4: Safe Soft-Deletion & Deactivation Protection

## Requirements & Constraints

- The Master Price Book is the authoritative source for ingredient rates; any price/unit update must immediately propagate to all recipes referencing the item (reactive observation, not manual re-entry).
- The inventory list shows only active ingredients, each row displaying the name and the price per base unit formatted in currency.
- Real-time, case-insensitive name filtering as the user types.
- Add/Edit via a form: name, base unit (dropdown: `kg`, `g`, `L`, `ml`, `pcs`), positive decimal price per base unit, and optional default wastage % (0–100). Invalid input (non-numeric, blank, or negative price) must show an inline error and block the database write.
- Deletion must never corrupt recipes: an item still used by recipes is deactivated (soft delete), not deleted; an unused item (`countRecipeUsages == 0`) may be permanently removed.
- Soft-deleted items stay linked in historical recipes but are excluded from active search and future selectors.
- Data integrity: foreign key protection prevents orphaned ingredient rows (no hard delete while recipe references exist).
- All price display strings go through locale-aware currency formatting; no hardcoded currency symbols.

## Technical Decisions

- Room migration `MIGRATION_8_9` (declared in `RecipeRoomDatabase.kt`): creates `MasterIngredient` (columns `id` PK AUTOINCREMENT, `name` NOT NULL, `baseUnit` NOT NULL, `pricePerBaseUnit` REAL NOT NULL, `defaultWastagePercent` REAL NOT NULL DEFAULT 0.0, `isActive` INTEGER NOT NULL DEFAULT 1, `updatedAt` INTEGER NOT NULL), adds nullable `masterItemId` column plus index to `Ingredient`, and preserves 100% of existing recipes/ingredients. This migration is a shared cross-epic contract and must itself be verified with the Room Migration Test Suite.
- `MasterIngredientDao` exposes a reactive `Flow` of active ingredients plus CRUD (`insert`, `update`, `softDelete`, `getById`) and `countRecipeUsages(masterIngredientId): Int`.
- Soft delete writes `isActive = false`; the physical delete is blocked by `ForeignKey.RESTRICT` on `Ingredient.masterItemId`.
- Layers: Room 2.6.x entity/DAO in the data layer, Material 3 UI in the presentation layer, reactive flows, standard Kotlin coroutines. No Android framework mocks in unit tests.

## UX & Interaction Patterns

- `MasterInventoryActivity` is launched from the Navigation Drawer ("Master Inventory").
- Material 3 RecyclerView list with a toolbar search input; a Floating Action Button (+) opens the "Add Raw Material" dialog; tapping a row opens a pre-populated "Edit Raw Material" dialog.
- Deleting an in-use ingredient prompts a confirmation that warns it is used in recipes and offers deactivation instead.

## Cross-Story Dependencies

- Migration 8->9 is shared: the same migration also creates `RecipeOverhead` (Epic 3) and `Ingredient.masterItemId` is consumed by Epic 2 ingredient linking. Epic 1 must land the schema first; later epics depend on existing data surviving the migration intact.
- Epic 2 autocomplete reads only active master items (`isActive = 1`); soft-deleted items must keep working for historical recipe costing in Epic 3 while staying invisible to new selectors.