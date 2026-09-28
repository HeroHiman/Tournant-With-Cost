package com.herohiman.tournant

import com.herohiman.tournant.utils.ActionableError
import com.herohiman.tournant.utils.BitmapOptimizer
import com.herohiman.tournant.utils.RecipeErrorCategory
import com.herohiman.tournant.utils.RecipeErrorHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

class RecipePerformanceAndErrorHandlingTest {

	@Test
	fun `inSampleSize calculation scales down high resolution photos by powers of two`() {
		// 4000x3000 photo targeting 1000x750 -> half is 2000x1500 (ratio 2), half of that is 1000x750 (ratio 4)
		val sampleSize = BitmapOptimizer.calculateInSampleSize(4000, 3000, 1000, 750)
		assertEquals(4, sampleSize)

		// 8000x6000 photo targeting 1000x750 -> ratio 8
		val largeSampleSize = BitmapOptimizer.calculateInSampleSize(8000, 6000, 1000, 750)
		assertEquals(8, largeSampleSize)
	}

	@Test
	fun `inSampleSize returns one when photo dimensions match or are smaller than requested`() {
		val sameSize = BitmapOptimizer.calculateInSampleSize(800, 600, 800, 600)
		assertEquals(1, sameSize)

		val smallerSize = BitmapOptimizer.calculateInSampleSize(400, 300, 800, 600)
		assertEquals(1, smallerSize)

		val invalidSize = BitmapOptimizer.calculateInSampleSize(0, 0, 800, 600)
		assertEquals(1, invalidSize)
	}

	@Test
	fun `aspect ratio is preserved when optimizing image dimensions`() {
		// Landscape 4000x2000 (aspect ratio 2.0), max dimension 1920
		val (landW, landH) = BitmapOptimizer.getOptimizedDimensions(4000, 2000, 1920)
		assertEquals(1920, landW)
		assertEquals(960, landH)

		// Portrait 2000x4000 (aspect ratio 0.5), max dimension 1920
		val (portW, portH) = BitmapOptimizer.getOptimizedDimensions(2000, 4000, 1920)
		assertEquals(960, portW)
		assertEquals(1920, portH)

		// Small image 400x300 should not upscale
		val (smallW, smallH) = BitmapOptimizer.getOptimizedDimensions(400, 300, 1920)
		assertEquals(400, smallW)
		assertEquals(300, smallH)
	}

	@Test
	fun `shouldDownsample identifies when image exceeds max bounds`() {
		assertTrue(BitmapOptimizer.shouldDownsample(2048, 1536, 1920))
		assertFalse(BitmapOptimizer.shouldDownsample(1280, 720, 1920))
	}

	@Test
	fun `error handler classifies out of memory error with recovery suggestion`() {
		val oom = OutOfMemoryError("Failed to allocate a 64MB bitmap")
		val classified = RecipeErrorHandler.classifyError(oom)

		assertEquals(RecipeErrorCategory.OUT_OF_MEMORY, classified.category)
		assertEquals("Insufficient Memory", classified.title)
		assertTrue(classified.canRetry)
		assertTrue(classified.actionableSuggestion.contains("smaller"))
	}

	@Test
	fun `error handler classifies storage io error with permission suggestion`() {
		val fnf = FileNotFoundException("/storage/recipes/photo.jpg not found")
		val classifiedFnf = RecipeErrorHandler.classifyError(fnf)
		assertEquals(RecipeErrorCategory.FILE_IO, classifiedFnf.category)
		assertEquals("File Not Found", classifiedFnf.title)
		assertTrue(classifiedFnf.canRetry)

		val io = IOException("Permission denied writing to export directory")
		val classifiedIo = RecipeErrorHandler.classifyError(io)
		assertEquals(RecipeErrorCategory.FILE_IO, classifiedIo.category)
		assertEquals("Storage Access Error", classifiedIo.title)
		assertTrue(classifiedIo.actionableSuggestion.contains("permissions"))
	}

	@Test
	fun `error handler classifies file format parse error with format suggestion`() {
		val parseError = RuntimeException("Corrupted JSON data at line 42")
		val classified = RecipeErrorHandler.classifyError(parseError)

		assertEquals(RecipeErrorCategory.FORMAT_CORRUPTED, classified.category)
		assertEquals("Unrecognized File Format", classified.title)
		assertFalse(classified.canRetry)
		assertTrue(classified.actionableSuggestion.contains("Tournant JSON"))
	}

	@Test
	fun `error handler classifies validation error with field check suggestion`() {
		val validationEx = IllegalArgumentException("Recipe title cannot be blank")
		val classified = RecipeErrorHandler.classifyError(validationEx)

		assertEquals(RecipeErrorCategory.VALIDATION, classified.category)
		assertEquals("Invalid Input", classified.title)
		assertFalse(classified.canRetry)
		assertTrue(classified.actionableSuggestion.contains("required fields"))
	}

}
