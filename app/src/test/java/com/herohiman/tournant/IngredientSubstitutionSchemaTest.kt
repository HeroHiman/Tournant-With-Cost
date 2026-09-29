package com.herohiman.tournant

import androidx.sqlite.db.SupportSQLiteDatabase
import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.Recipe
import com.herohiman.tournant.data.room.IngredientEntity
import com.herohiman.tournant.data.room.RecipeEntity
import com.herohiman.tournant.data.room.RecipeRoomDatabase
import com.herohiman.tournant.data.room.RecipeWithIngredientsAndPreparations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class IngredientSubstitutionSchemaTest {

	private val executedSqlStatements = mutableListOf<String>()

	@Before
	fun setUp() {
		executedSqlStatements.clear()
	}

	@Test
	fun `migration from version 12 to 13 adds substituteGroupId isActiveSubstitute and index`() {
		val fakeDb = Proxy.newProxyInstance(
			SupportSQLiteDatabase::class.java.classLoader,
			arrayOf(SupportSQLiteDatabase::class.java)
		) { _, method, args ->
			if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
				executedSqlStatements.add(args[0] as String)
			}
			null
		} as SupportSQLiteDatabase

		RecipeRoomDatabase.MIGRATION_12_13.migrate(fakeDb)

		assertEquals(3, executedSqlStatements.size)

		val addSubstituteGroupIdSql = executedSqlStatements[0]
		assertTrue(addSubstituteGroupIdSql.contains("ALTER TABLE `Ingredient` ADD COLUMN `substituteGroupId` TEXT DEFAULT NULL"))

		val addIsActiveSubstituteSql = executedSqlStatements[1]
		assertTrue(addIsActiveSubstituteSql.contains("ALTER TABLE `Ingredient` ADD COLUMN `isActiveSubstitute` INTEGER NOT NULL DEFAULT 1"))

		val createIndexSql = executedSqlStatements[2]
		assertTrue(createIndexSql.contains("CREATE INDEX IF NOT EXISTS `index_Ingredient_substituteGroupId` ON `Ingredient` (`substituteGroupId`)"))
	}

	@Test
	fun `ingredient entity defaults substitute fields safely`() {
		val entity = IngredientEntity(
			recipeId = 1L,
			position = 0,
			amount = 100.0,
			amountRange = null,
			unit = "g",
			item = "Flour",
			refId = null,
			group = null,
			optional = false
		)

		assertNull(entity.substituteGroupId)
		assertTrue(entity.isActiveSubstitute)
	}

	@Test
	fun `ingredient domain class defaults substitute fields safely`() {
		val ingredient = Ingredient(
			amount = 100.0,
			unit = "g",
			item = "Sugar"
		)

		assertNull(ingredient.substituteGroupId)
		assertTrue(ingredient.isActiveSubstitute)
	}

	@Test
	fun `recipe to entity conversion preserves substitute fields`() {
		val recipe = Recipe(
			id = 10L,
			title = "Sweet Dough",
			ingredients = mutableListOf(
				Ingredient(
					amount = 500.0,
					unit = "g",
					item = "Gud (Jaggery)",
					substituteGroupId = "sub_sweetener_1",
					isActiveSubstitute = true
				),
				Ingredient(
					amount = 750.0,
					unit = "g",
					item = "Sugar",
					substituteGroupId = "sub_sweetener_1",
					isActiveSubstitute = false
				),
				Ingredient(
					amount = 1000.0,
					unit = "g",
					item = "Wheat Flour"
				)
			)
		)

		val (_, ingredientEntities, _, _) = recipe.toRecipeWithIngredientsAndPreparations()

		assertEquals(3, ingredientEntities.size)

		val gudEntity = ingredientEntities[0]
		assertEquals("Gud (Jaggery)", gudEntity.item)
		assertEquals("sub_sweetener_1", gudEntity.substituteGroupId)
		assertTrue(gudEntity.isActiveSubstitute)

		val sugarEntity = ingredientEntities[1]
		assertEquals("Sugar", sugarEntity.item)
		assertEquals("sub_sweetener_1", sugarEntity.substituteGroupId)
		assertFalse(sugarEntity.isActiveSubstitute)

		val flourEntity = ingredientEntities[2]
		assertEquals("Wheat Flour", flourEntity.item)
		assertNull(flourEntity.substituteGroupId)
		assertTrue(flourEntity.isActiveSubstitute)
	}

	@Test
	fun `recipe with ingredients and preparations toRecipe round trip preserves substitute fields`() {
		val recipeEntity = Recipe(
			id = 20L,
			title = "Kheer"
		).toRecipeWithIngredientsAndPreparations().recipe

		val ingredientEntities = listOf(
			IngredientEntity(
				recipeId = 20L,
				position = 0,
				amount = 1.0,
				amountRange = null,
				unit = "l",
				item = "Milk",
				refId = null,
				group = null,
				optional = false,
				substituteGroupId = null,
				isActiveSubstitute = true
			),
			IngredientEntity(
				recipeId = 20L,
				position = 1,
				amount = 200.0,
				amountRange = null,
				unit = "g",
				item = "Jaggery",
				refId = null,
				group = null,
				optional = false,
				substituteGroupId = "sweetener_group",
				isActiveSubstitute = false
			),
			IngredientEntity(
				recipeId = 20L,
				position = 2,
				amount = 150.0,
				amountRange = null,
				unit = "g",
				item = "Sugar",
				refId = null,
				group = null,
				optional = false,
				substituteGroupId = "sweetener_group",
				isActiveSubstitute = true
			)
		)

		val wrapper = RecipeWithIngredientsAndPreparations(
			recipe = recipeEntity,
			ingredients = ingredientEntities
		)

		val domainRecipe = wrapper.toRecipe()

		assertEquals(3, domainRecipe.ingredients.size)

		val jaggery = domainRecipe.ingredients[1]
		assertEquals("Jaggery", jaggery.item)
		assertEquals("sweetener_group", jaggery.substituteGroupId)
		assertFalse(jaggery.isActiveSubstitute)

		val sugar = domainRecipe.ingredients[2]
		assertEquals("Sugar", sugar.item)
		assertEquals("sweetener_group", sugar.substituteGroupId)
		assertTrue(sugar.isActiveSubstitute)
	}
}
