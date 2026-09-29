# Tournant (with Live Cost & Yield Engine)

<p align="center">
  <strong>An offline-first recipe manager, cookbook, and live production costing engine for Android.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform: Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Language: Kotlin" />
  <img src="https://img.shields.io/badge/Architecture-MVVM%20%2B%20Room-00557F" alt="Architecture" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License: Apache 2.0" />
</p>

---

## Project Overview

**Tournant** is a modern recipe manager, culinary notebook, and dynamic cost calculation application designed for home cooks, bakers, chefs, and food businesses. 

While preserving the core features of the original Tournant app—such as offline recipe organization, Markdown instruction formatting, and desktop Gourmand XML compatibility—this edition introduces an industrial-grade **Live Costing & Yield Engine**. It automatically resolves ingredient conversions, tracks sub-recipe dependencies (such as stocks, doughs, or dairy reductions), links aliases, displays reverse usage cross-references ("Where-Used" reports), and facilitates seamless configuration backups.

---

## Key Features

### 1. Live Recipe Costing & Catalog Management
- **Master Ingredient Catalog**: Maintain a centralized ingredient cost index with configurable base units (`g`, `kg`, `ml`, `l`, `unit`), standard unit costs, and categories.
- **Dynamic Cost Computation**: Automatically calculate total recipe preparation costs and per-serving costs directly from your ingredient lists.
- **Privacy Mode**: Toggle privacy with a single tap in the toolbar to mask sensitive raw ingredient prices (`•••• / kg`) during kitchen demonstrations or public walkthroughs.
- **Recipe Auto-Fetch**: Automatically harvest ingredient names from existing recipes and add missing items into your master price index in one click.

### 2. Sub-Recipe & Yield Costing Engine
- **Derived Ingredient Pricing**: Link a master ingredient directly to a sub-recipe (e.g., *Khoya* or *Paneer* derived from *Milk*, or *Vegetable Stock* from raw produce) instead of entering a static manual price.
- **Yield Ratio Scaling**: Define preparation yield ratios (e.g., 200 g output from 1 kg raw input = `0.20` yield ratio). The engine divides the base sub-recipe cost by the yield ratio to calculate the true per-unit cost.
- **Cycle-Safe Recursive Costing**: Built-in dependency tracking and cycle-detection safeguards prevent recursive loops and circular recipe dependencies.

### 3. Smart Deduplication & Unit Alias System
- **Unit Alias Mapping**: Automatic normalization for international and regional units (`किलो` $\rightarrow$ `kg`, `चमच` $\rightarrow$ `tbsp`, `पाव`, `लीटर`, ounces, pounds) mapped to fundamental physical base units.
- **Ingredient Alias Deduplication**: Clean up inconsistent recipe entries (such as *"Butter (unsalted)"*, *"Cold Butter"*, or *"Butter (cubed)"*) by safely merging them into a single primary master ingredient.
- **Referential Integrity**: All merging operations update existing foreign keys atomically and preserve historical linkages.

### 4. "Where-Used" Cross-Reference Engine
- **Reverse-Lookup Analysis**: Instantly inspect which recipes depend on a given ingredient before deleting it or modifying market prices.
- **Usage Bottom Sheet**: Tap the Where-Used icon on any catalog card to pull up a bottom sheet showing the exact number of dependent recipes.
- **Deep Linking Navigation**: Tap any recipe in the usage list to open it directly in the editor or view mode.

### 5. Configuration Backup, Export & Import
- **Storage Access Framework (SAF)**: Export and import master prices, unit aliases, and ingredient mappings using native Android file pickers (`ACTION_CREATE_DOCUMENT` and `ACTION_OPEN_DOCUMENT`).
- **Atomic Bulk Transactions**: Imports are wrapped in database transactions to protect your catalog against data corruption.
- **Moshi JSON Serialization**: Exported files are cleanly structured, human-readable JSON payloads with schema versioning.

### 6. Core Recipe Features & Compatibility
- **Markdown & Formatting**: Full Markdown rendering for rich instructions, ingredient groups, notes, and timers.
- **Proportional Scaling**: Dynamically scale yields and ingredient quantities up or down.
- **Gourmand Desktop Sync**: Import and export compatibility with Gourmand Recipe Manager (formerly Gourmet) XML files.
- **Offline & Private**: All data resides locally on the device in a SQLite/Room database with zero cloud dependencies.

---

## Architecture & Technical Stack

```
com.herohiman.tournant
├── cost/                       # Cost calculation, alias resolution & backup engine
│   ├── CostConfigBackupManager.kt
│   ├── CostConfigBackupPayload.kt
│   ├── CostPrivacyManager.kt
│   ├── IngredientSyncManager.kt
│   ├── MergeIngredientUseCase.kt
│   └── RecipeCostCalculator.kt
├── data/                       # Domain models & entities
│   ├── room/                   # Room Database, DAOs & Entities
│   │   ├── IngredientAliasDao.kt
│   │   ├── MasterIngredientDao.kt
│   │   ├── RecipeDao.kt
│   │   ├── RecipeRepository.kt
│   │   ├── RecipeRoomDatabase.kt
│   │   └── UnitAliasDao.kt
│   └── Recipe.kt
├── ui/                         # Activities, Adapters & Material Views
│   ├── MasterCostListActivity.kt
│   ├── RecipeActivity.kt
│   ├── RecipeEditingActivity.kt
│   └── MainActivity.kt
└── utils/                      # Unit converters, JSON adapters & helpers
```

- **Language**: Kotlin 1.9+ with strict typing and Coroutines/Flow.
- **Persistence**: Android Jetpack Room with custom SQLite migrations.
- **UI Framework**: Material Design 3, ViewBinding, ConstraintLayout, and Edge-to-Edge window insets.
- **JSON Serialization**: Moshi with reflection and Kotlin code generation.
- **Testing**: Pure JUnit 4 unit tests with deterministic database fakes for business logic verification.

---

## Installation & Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 (recommended for Gradle 8.x)
- Android SDK with API 35 build tools

### Build Flavors
The project is configured with two product flavors:
- `demo`: Lightweight build with mock sample data for quick evaluation.
- `full`: Complete build supporting desktop sync and full import/export functionality.

### Command Line Build

1. **Clone the repository:**
   ```bash
   git clone https://github.com/HeroHiman/Tournant-With-Cost.git
   cd Tournant-With-Cost
   ```

2. **Run Unit Tests:**
   ```bash
   ./gradlew testDemoDebugUnitTest
   ```

3. **Compile Debug APKs:**
   ```bash
   # Compile demo debug flavor
   ./gradlew compileDemoDebugSources assembleDemoDebug

   # Compile full debug flavor
   ./gradlew compileFullDebugSources assembleFullDebug
   ```

4. **Install on a connected device:**
   ```bash
   ./gradlew installDemoDebug
   ```

---

## Usage Guide

### Managing Master Ingredient Costs
1. Open the drawer menu and select **Master Cost List**.
2. Tap the **+** floating action button to enter an ingredient name, base unit (e.g. `kg`, `g`, `l`), and unit cost.
3. Tap **Auto-Fetch from Recipes** in the toolbar to import missing ingredient names across your recipe book automatically.

### Configuring Sub-Recipes & Yields
1. In the Master Cost dialog, toggle **Prepared Item / Sub-Recipe**.
2. Choose a sub-recipe (e.g., *"Boiled Milk"* or *"Vegetable Puree"*).
3. Input the standard output yield ratio (e.g., `0.20` for a 20% finished yield).
4. The system automatically computes unit cost based on the sub-recipe ingredients and marks the catalog card with a **Sub-Recipe** badge.

### Checking Where an Ingredient is Used
1. On any item in the Master Cost List, tap the **Where-Used (Info)** icon.
2. A bottom sheet appears listing all recipes referencing that item or its aliases.
3. Tap any recipe name in the list to jump straight to that recipe.

### Exporting & Importing Configurations
1. From the Master Cost List, tap the overflow menu (`...`) in the top right corner.
2. Tap **Export Configuration** to choose a destination and save a timestamped JSON backup.
3. On another device, tap **Import Configuration** and pick your backup file. The database merges existing entries and creates aliases automatically.

---

## Contributing

We welcome community contributions, bug fixes, and feature suggestions!

1. Fork the project repository.
2. Create a focused feature branch (`git checkout -b feature/my-new-feature`).
3. Commit your changes with clear, descriptive commit messages.
4. Ensure all unit tests and flavor builds pass cleanly (`./gradlew testDemoDebugUnitTest compileDemoDebugSources compileFullDebugSources`).
5. Push to your branch and open a Pull Request.

---

## License

Tournant is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for full license text.
Embedded fonts and third-party assets carry their respective open-source licenses as documented in their metadata.
