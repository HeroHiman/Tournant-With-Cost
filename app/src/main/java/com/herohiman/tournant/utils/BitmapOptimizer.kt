package com.herohiman.tournant.utils

import kotlin.math.roundToInt

object BitmapOptimizer {

	const val DEFAULT_MAX_DIMENSION = 1920
	const val THUMBNAIL_MAX_DIMENSION = 512

	fun calculateInSampleSize(
		rawWidth: Int,
		rawHeight: Int,
		reqWidth: Int,
		reqHeight: Int
	): Int {
		if (rawWidth <= 0 || rawHeight <= 0 || reqWidth <= 0 || reqHeight <= 0) {
			return 1
		}

		var inSampleSize = 1

		if (rawHeight > reqHeight || rawWidth > reqWidth) {
			val halfHeight = rawHeight / 2
			val halfWidth = rawWidth / 2

			while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
				inSampleSize *= 2
			}
		}

		return inSampleSize
	}

	fun shouldDownsample(
		rawWidth: Int,
		rawHeight: Int,
		maxDimension: Int = DEFAULT_MAX_DIMENSION
	): Boolean {
		return rawWidth > maxDimension || rawHeight > maxDimension
	}

	fun getOptimizedDimensions(
		rawWidth: Int,
		rawHeight: Int,
		maxDimension: Int = DEFAULT_MAX_DIMENSION
	): Pair<Int, Int> {
		if (rawWidth <= 0 || rawHeight <= 0 || maxDimension <= 0) {
			return Pair(maxOf(1, rawWidth), maxOf(1, rawHeight))
		}

		if (rawWidth <= maxDimension && rawHeight <= maxDimension) {
			return Pair(rawWidth, rawHeight)
		}

		val aspectRatio = rawWidth.toDouble() / rawHeight.toDouble()

		return if (rawWidth >= rawHeight) {
			val targetWidth = maxDimension
			val targetHeight = (maxDimension / aspectRatio).roundToInt().coerceAtLeast(1)
			Pair(targetWidth, targetHeight)
		} else {
			val targetHeight = maxDimension
			val targetWidth = (maxDimension * aspectRatio).roundToInt().coerceAtLeast(1)
			Pair(targetWidth, targetHeight)
		}
	}

}
