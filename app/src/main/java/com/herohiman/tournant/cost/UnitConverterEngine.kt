package com.herohiman.tournant.cost

import com.herohiman.tournant.data.room.BaseUnitType
import com.herohiman.tournant.data.room.UnitAliasDao
import com.herohiman.tournant.data.room.UnitAliasEntity
import java.util.Locale

/**
 * Domain engine responsible for unit normalization and conversion between recipe ingredient units
 * and master ingredient base units.
 * Supports multilingual unit aliases (including Hindi, English, and user-defined custom aliases).
 */
object UnitConverterEngine {

	private const val GRAMS_PER_KG = 1000.0
	private const val GRAMS_PER_MG = 0.001
	private const val GRAMS_PER_OZ = 28.349523125
	private const val GRAMS_PER_LB = 453.59237

	private const val ML_PER_LITER = 1000.0
	private const val ML_PER_CL = 10.0
	private const val ML_PER_DL = 100.0
	private const val ML_PER_TSP = 4.92892
	private const val ML_PER_TBSP = 14.7868
	private const val ML_PER_FL_OZ = 29.5735
	private const val ML_PER_CUP = 236.588
	private const val ML_PER_PINT = 473.176
	private const val ML_PER_QUART = 946.353
	private const val ML_PER_GALLON = 3785.41

	private val WEIGHT_UNITS = mapOf(
		"g" to 1.0,
		"gram" to 1.0,
		"grams" to 1.0,
		"kg" to GRAMS_PER_KG,
		"kilogram" to GRAMS_PER_KG,
		"kilograms" to GRAMS_PER_KG,
		"mg" to GRAMS_PER_MG,
		"milligram" to GRAMS_PER_MG,
		"milligrams" to GRAMS_PER_MG,
		"oz" to GRAMS_PER_OZ,
		"ounce" to GRAMS_PER_OZ,
		"ounces" to GRAMS_PER_OZ,
		"lb" to GRAMS_PER_LB,
		"lbs" to GRAMS_PER_LB,
		"pound" to GRAMS_PER_LB,
		"pounds" to GRAMS_PER_LB
	)

	private val VOLUME_UNITS = mapOf(
		"ml" to 1.0,
		"milliliter" to 1.0,
		"milliliters" to 1.0,
		"l" to ML_PER_LITER,
		"liter" to ML_PER_LITER,
		"liters" to ML_PER_LITER,
		"cl" to ML_PER_CL,
		"centiliter" to ML_PER_CL,
		"centiliters" to ML_PER_CL,
		"dl" to ML_PER_DL,
		"deciliter" to ML_PER_DL,
		"deciliters" to ML_PER_DL,
		"tsp" to ML_PER_TSP,
		"teaspoon" to ML_PER_TSP,
		"teaspoons" to ML_PER_TSP,
		"tbsp" to ML_PER_TBSP,
		"tablespoon" to ML_PER_TBSP,
		"tablespoons" to ML_PER_TBSP,
		"fl oz" to ML_PER_FL_OZ,
		"fluid ounce" to ML_PER_FL_OZ,
		"fluid ounces" to ML_PER_FL_OZ,
		"cup" to ML_PER_CUP,
		"cups" to ML_PER_CUP,
		"pt" to ML_PER_PINT,
		"pint" to ML_PER_PINT,
		"pints" to ML_PER_PINT,
		"qt" to ML_PER_QUART,
		"quart" to ML_PER_QUART,
		"quarts" to ML_PER_QUART,
		"gal" to ML_PER_GALLON,
		"gallon" to ML_PER_GALLON,
		"gallons" to ML_PER_GALLON
	)

	private val COUNT_UNITS = setOf(
		"pc", "pcs", "piece", "pieces", "unit", "units", "item", "items", "count", "ct",
		"can", "cans", "bottle", "bottles", "bunch", "bunches", "clove", "cloves",
		"slice", "slices", "pinch", "pinches", "dash", "dashes", "package", "packages", "pkg", "pkgs",
		"इकाई", "नग", "दाने", "चुटकी"
	)

	private val DEFAULT_ALIASES: Map<String, UnitAliasEntity> by lazy {
		UnitAliasDao.getDefaultAliases().associateBy { it.aliasName.trim().lowercase(Locale.ROOT) }
	}

	/**
	 * Resolves an alias entity from custom aliases, default dictionary, or fallback units.
	 */
	fun findAlias(unit: String?, customAliases: Map<String, UnitAliasEntity> = emptyMap()): UnitAliasEntity? {
		if (unit.isNullOrBlank()) return null
		val normalized = unit.trim().lowercase(Locale.ROOT)
		val noDot = normalized.replace(".", "")

		// 1. Check custom aliases from database
		customAliases[normalized]?.let { return it }
		customAliases[noDot]?.let { return it }

		// 2. High precision standard culinary weight and volume definitions
		WEIGHT_UNITS[normalized]?.let { grams ->
			return UnitAliasEntity(
				aliasName = normalized,
				baseUnit = BaseUnitType.KG,
				conversionFactor = grams / GRAMS_PER_KG
			)
		}
		VOLUME_UNITS[normalized]?.let { mls ->
			return UnitAliasEntity(
				aliasName = normalized,
				baseUnit = BaseUnitType.LITER,
				conversionFactor = mls / ML_PER_LITER
			)
		}

		// 3. Check default multilingual dictionary (Hindi and additional units)
		DEFAULT_ALIASES[normalized]?.let { return it }
		DEFAULT_ALIASES[noDot]?.let { return it }

		// 4. Discrete count units
		if (normalized in COUNT_UNITS) {
			return UnitAliasEntity(
				aliasName = normalized,
				baseUnit = BaseUnitType.COUNT,
				conversionFactor = 1.0
			)
		}

		return null
	}

	/**
	 * Calculates the multiplication factor to convert an amount in sourceUnit to targetUnit.
	 * Returns null if the units are incompatible (e.g. mass vs volume) or cannot be resolved.
	 */
	fun resolveConversionFactor(
		sourceUnit: String?,
		targetUnit: String?,
		customAliases: Map<String, UnitAliasEntity> = emptyMap()
	): Double? {
		val source = sourceUnit?.trim()?.lowercase(Locale.ROOT)
		val target = targetUnit?.trim()?.lowercase(Locale.ROOT)

		// 1. Both units null or blank -> assume 1:1 match
		if (source.isNullOrBlank() && target.isNullOrBlank()) {
			return 1.0
		}

		// 2. Exact match
		if (source == target) {
			return 1.0
		}

		// 3. Alias resolution
		val sourceAlias = findAlias(source, customAliases)
		val targetAlias = findAlias(target, customAliases)

		// 4. Count / discrete unit handling
		val isSourceCount = source.isNullOrBlank() || source in COUNT_UNITS || sourceAlias?.baseUnit == BaseUnitType.COUNT
		val isTargetCount = target.isNullOrBlank() || target in COUNT_UNITS || targetAlias?.baseUnit == BaseUnitType.COUNT

		if (isSourceCount && isTargetCount) {
			val sourceFactor = sourceAlias?.conversionFactor ?: 1.0
			val targetFactor = targetAlias?.conversionFactor ?: 1.0
			return if (targetFactor > 0.0) sourceFactor / targetFactor else 1.0
		}

		// 5. Category-based factor calculation (KG, LITER, COUNT)
		if (sourceAlias != null && targetAlias != null) {
			if (sourceAlias.baseUnit == targetAlias.baseUnit) {
				return if (targetAlias.conversionFactor > 0.0) {
					sourceAlias.conversionFactor / targetAlias.conversionFactor
				} else {
					null
				}
			} else {
				return null // Incompatible categories
			}
		}

		// 6. Direct fallback for standard weight/volume units if one side wasn't an alias
		if (source != null && target != null) {
			val sourceWeight = WEIGHT_UNITS[source]
			val targetWeight = WEIGHT_UNITS[target]
			if (sourceWeight != null && targetWeight != null) {
				return sourceWeight / targetWeight
			}

			val sourceVolume = VOLUME_UNITS[source]
			val targetVolume = VOLUME_UNITS[target]
			if (sourceVolume != null && targetVolume != null) {
				return sourceVolume / targetVolume
			}
		}

		return null
	}

	/**
	 * Converts a quantity from sourceUnit to targetUnit.
	 */
	fun convertQuantity(
		amount: Double,
		sourceUnit: String?,
		targetUnit: String?,
		customAliases: Map<String, UnitAliasEntity> = emptyMap()
	): Double? {
		val factor = resolveConversionFactor(sourceUnit, targetUnit, customAliases) ?: return null
		return amount * factor
	}

	/**
	 * Resolves the base unit type (KG, LITER, COUNT) for a given unit name.
	 */
	fun getBaseUnitType(unitName: String?, customAliases: Map<String, UnitAliasEntity> = emptyMap()): BaseUnitType? {
		if (unitName.isNullOrBlank()) return BaseUnitType.COUNT
		return findAlias(unitName, customAliases)?.baseUnit
	}
}
