package com.herohiman.tournant.data.room

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.herohiman.tournant.BuildConfig
import com.herohiman.tournant.Constants.Companion.MODE_STANDALONE
import com.herohiman.tournant.Constants.Companion.MODE_SYNCED
import com.herohiman.tournant.Constants.Companion.PREF_MODE
import com.herohiman.tournant.getAppOrSystemLocale
import kotlin.reflect.full.declaredFunctions

@Database(
	entities = [RecipeEntity::class, IngredientEntity::class, KeywordEntity::class, PreparationEntity::class, RecipePinEntity::class, MasterIngredientEntity::class, UnitAliasEntity::class, IngredientAliasEntity::class],
	exportSchema = true,
	version = 12,
	autoMigrations = [AutoMigration(1, 2), AutoMigration(2, 3), AutoMigration(4, 5), AutoMigration(6, 7), AutoMigration(7, 8), AutoMigration(8, 9), AutoMigration(9, 10), AutoMigration(10, 11), AutoMigration(11, 12)]
)
abstract class RecipeRoomDatabase : RoomDatabase() {
	abstract fun recipeDao(): RecipeDao
	abstract fun masterIngredientDao(): MasterIngredientDao
	abstract fun unitAliasDao(): UnitAliasDao
	abstract fun ingredientAliasDao(): IngredientAliasDao

	companion object {
		@Volatile
		private var INSTANCE: RecipeRoomDatabase? = null
		fun getDatabase(context: Context): RecipeRoomDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = if (BuildConfig.FLAVOR == "demo") {
					Class.forName("com.herohiman.tournant.demo.DemoDatabase").kotlin.run {
						declaredFunctions.find { it.name == "create" }!!.call(objectInstance, context)
					} as RecipeRoomDatabase
				} else Room.databaseBuilder(
					context.applicationContext,
					RecipeRoomDatabase::class.java,
					if (context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
						.getInt(PREF_MODE, MODE_STANDALONE) == MODE_SYNCED)
							"synced_recipe_database"
					else "recipe_database"
				)
					// Verbose SQL logging
/*					.setQueryCallback(
					 	object : QueryCallback {
						override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
							Log.d("SQL", "QUERY $sqlQuery ARGS $bindArgs")
						}
					}, Executors.newSingleThreadExecutor()
					)
*/
					.addMigrations(MIGRATION_3_4, MIGRATION_5_6, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
					.build()
				INSTANCE = instance
				return instance
			}
		}

		val MIGRATION_3_4 = object : Migration(3, 4) {
			override fun migrate(db: SupportSQLiteDatabase) {
				db.execSQL("ALTER TABLE Ingredient RENAME TO IngredientOld")
				db.execSQL("CREATE TABLE IF NOT EXISTS Ingredient (`recipeId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `amount` REAL, `amountRange` REAL, `unit` TEXT, `item` TEXT, `refId` INTEGER, `group` TEXT, `optional` INTEGER NOT NULL, PRIMARY KEY (recipeId, position), FOREIGN KEY(`recipeId`) REFERENCES `Recipe`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )")
				db.execSQL("INSERT INTO Ingredient SELECT recipeId, position, amount, amountRange, unit, item, refId, `group`, optional FROM IngredientOld")
				db.execSQL("DROP TABLE IngredientOld")
				db.execSQL("CREATE INDEX IF NOT EXISTS `index_Ingredient_recipeId` ON Ingredient (recipeId)")
				db.execSQL("ALTER TABLE Preparation RENAME TO PreparationOld")
				db.execSQL("CREATE TABLE IF NOT EXISTS Preparation (`recipeId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `count` INTEGER NOT NULL, PRIMARY KEY (recipeId, date), FOREIGN KEY(`recipeId`) REFERENCES `Recipe`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )")
				db.execSQL("INSERT INTO Preparation SELECT recipeId, date, COUNT(*) FROM PreparationOld GROUP BY recipeId, date")
				db.execSQL("DROP TABLE PreparationOld")
				db.execSQL("CREATE INDEX IF NOT EXISTS `index_Preparation_recipeId` ON Preparation (recipeId)")
			}
		}

		val MIGRATION_5_6 = object : Migration(5, 6) {
			override fun migrate(db: SupportSQLiteDatabase) {
				val languageTag = getAppOrSystemLocale().toLanguageTag()
				db.execSQL("ALTER TABLE Recipe ADD COLUMN language TEXT NOT NULL DEFAULT `$languageTag`")
			}
		}

		val MIGRATION_8_9 = object : Migration(8, 9) {
			override fun migrate(db: SupportSQLiteDatabase) {
				db.execSQL("""
					CREATE TABLE IF NOT EXISTS `MasterIngredient` (
						`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
						`name` TEXT NOT NULL,
						`unitCost` REAL NOT NULL,
						`baseUnit` TEXT NOT NULL,
						`currency` TEXT NOT NULL DEFAULT 'USD',
						`isActive` INTEGER NOT NULL DEFAULT 1,
						`lastUpdated` INTEGER NOT NULL,
						`category` TEXT,
						`notes` TEXT
					)
				""".trimIndent())
				db.execSQL("CREATE INDEX IF NOT EXISTS `index_MasterIngredient_name` ON `MasterIngredient` (`name`)")
			}
		}

		val MIGRATION_9_10 = object : Migration(9, 10) {
			override fun migrate(db: SupportSQLiteDatabase) {
				db.execSQL("""
					CREATE TABLE IF NOT EXISTS `UnitAlias` (
						`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
						`aliasName` TEXT NOT NULL,
						`baseUnit` TEXT NOT NULL,
						`conversionFactor` REAL NOT NULL
					)
				""".trimIndent())
				db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_UnitAlias_aliasName` ON `UnitAlias` (`aliasName`)")
			}
		}

		val MIGRATION_10_11 = object : Migration(10, 11) {
			override fun migrate(db: SupportSQLiteDatabase) {
				db.execSQL("""
					CREATE TABLE IF NOT EXISTS `IngredientAlias` (
						`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
						`rawName` TEXT NOT NULL,
						`masterIngredientId` INTEGER NOT NULL,
						FOREIGN KEY(`masterIngredientId`) REFERENCES `MasterIngredient`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
					)
				""".trimIndent())
				db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_IngredientAlias_rawName` ON `IngredientAlias` (`rawName`)")
				db.execSQL("CREATE INDEX IF NOT EXISTS `index_IngredientAlias_masterIngredientId` ON `IngredientAlias` (`masterIngredientId`)")
			}
		}

		val MIGRATION_11_12 = object : Migration(11, 12) {
			override fun migrate(db: SupportSQLiteDatabase) {
				db.execSQL("ALTER TABLE `MasterIngredient` ADD COLUMN `linkedRecipeId` INTEGER DEFAULT NULL")
				db.execSQL("ALTER TABLE `MasterIngredient` ADD COLUMN `yieldRatio` REAL DEFAULT NULL")
				db.execSQL("CREATE INDEX IF NOT EXISTS `index_MasterIngredient_linkedRecipeId` ON `MasterIngredient` (`linkedRecipeId`)")
			}
		}
	}

}
