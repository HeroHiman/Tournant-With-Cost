"""
Story 2.1: Ingredient Scaling - Phase 2 Implementation
Comprehensive scaling system for recipe ingredients
"""

Epic 2 Goal: "Provide advanced recipe management features for experienced users"
Priority: HIGH

## Overview
This implementation completes Story 2.1 (Ingredient Scaling) from Epic 2, building on the existing scaling infrastructure in Phase 1 to provide comprehensive ingredient scaling capabilities for experienced users.

## Core Requirements

### 1. Scaling System
**Mathematical precision with unit preservation**

- **Core Implementation**: Enhanced `Ingredient.withScaledAmount()` method with comprehensive scaling logic
- **Precision Handling**: Proper decimal and fractional scaling with configurable precision
- **Range Support**: Simultaneous scaling of single amounts and amount ranges (e.g., "2-3 cups" → "4-6 cups")
- **Null Safety**: Graceful handling of null amounts, units, and other optional fields
- **Unit Independence**: Scaling preserves units without modification ("cups" → "cups", "g" → "g")

### 2. User Interface Controls
**Seamless integration with existing recipe view UI**

- **Yield Slider**: Flexible yield adjustment with visual feedback
- **Ingredient-level Scaling**: Direct scaling of individual ingredients with custom amounts
- **Visual Feedback**: Clear display of original vs. scaled amounts
- **Weigh Mode**: Optional weighing mode for mass-based calculations
- **Reset Capability**: Quick return to original ingredient amounts

### 3. Dependency Management
**Leverage existing architectural patterns**

- **Repository Pattern**: Utilize existing Room database queries
- **ViewModel Architecture**: Leverage existing RecipeViewModel implementation
- **Flow Integration**: Use Kotlin Flows for real-time UI updates
- **State Management**: Combine current and target yield values for scaling ratio

## Technical Specifications

### Data Layer

**Ingredient.kt Enhancements**
```kotlin
fun withScaledAmount(factor: Double): Ingredient {
    // Core scaling logic preserving all properties
    // Handles amount, amountRange, unit, item, refId, group, optional
}
```

**RecipeViewModel.kt Integration**
```kotlin
private val _scaleRatio = combine(_recipeYieldValue, _targetYieldValue) {
    targetYield?.div(currentYield ?: 1.0) ?: 1.0
}

val ingredientsScaled = combine(_ingredients, _scaleRatio) {
    ingredients.map { item ->
        when (item) {
            is IngredientGroupTitle -> item
            is IngredientItem -> item.copy(
                ingredient = item.ingredient.withScaledAmount(scale)
            )
        }
    }
}
```

### Business Logic

**Scaling Strategy**
- **Yield-based**: Scale all ingredients proportionally based on yield value
- **Ingredient-specific**: Scale individual ingredients by custom factor
- **Mixed mode**: Support both yield-level and ingredient-level scaling simultaneously
- **Range preservation**: Maintain relative relationships within ingredient ranges

**Precision Management**
- **Decimal preservation**: Maintain appropriate precision based on original values
- **Fraction handling**: Support fractional values (0.25, 0.5, 0.75)
- **Scientific notation**: Handle very small or large values appropriately
- **Rounding**: Use appropriate rounding to avoid floating-point precision issues

### User Experience

**Input Methods**
- **Text field**: Direct yield value input
- **Increment/decrement**: Simple +/- buttons for common scaling
- **Ingredient override**: Direct amount specification for individual ingredients
- **Preview mode**: Side-by-side comparison of original vs scaled

**Display Features**
- **Dual amounts**: Show both original and scaled values
- **Unit labeling**: Clear indication of measurement units
- **Visual indicators**: Clear differentiation between scaled and original
- **Mass calculations**: Optional weighing mode for mass-based calculations

## Implementation Approach

### 1. Core Method Implementation
**Enhance Ingredient.withScaledAmount()**

- **Input validation**: Handle null and zero values appropriately
- **Mathematical scaling**: Apply scaling factor with precision
- **Property preservation**: Maintain all non-numeric properties
- **Edge case handling**: Special cases for null amounts, zero scaling, etc.

### 2. ViewModel Integration
**Complete RecipeViewModel enhancements**

- **State management**: Add scaling-related state variables
- **Scale calculations**: Implement scaling ratio calculations
- **User actions**: Add scale up, scale down, scale reset methods
- **Ingredient scaling**: Add ingredient-specific scaling methods

### 3. UI Integration
**Enhance RecipeActivity.kt**

- **Add scaling controls**: Scale up/down/reset buttons
- **Implement yield input**: Allow direct yield value modification
- **Display scaled values**: Show both original and scaled amounts
- **Link handling**: Implement ingredient-to-recipe linking (Story 2.2)

### 4. Test Coverage
**Comprehensive unit and UI tests**

- **Unit tests**: Test Ingredient scaling logic
- **Integration tests**: Test ViewModel and repository interactions
- **UI tests**: Test user interactions and display updates
- **Edge cases**: Test boundary conditions and error scenarios

## Success Metrics

### Quality Metrics
- **Correctness**: 100% test coverage for scaling logic
- **Precision**: Maintain mathematical accuracy within 0.01 unit
- **Compatibility**: Backward compatible with existing recipe data
- **Performance**: Scaling operations complete in <10ms per ingredient

### User Experience Metrics
- **Usability**: Intuitive scaling controls with clear feedback
- **Accessibility**: Support for screen readers and accessibility features
- **Learning curve**: Easy-to-understand scaling interface
- **Efficiency**: Quick scaling operations with minimal user effort

## Dependencies

### Phase 1 (Already Complete)
- ✅ Core recipe management infrastructure
- ✅ Ingredient data model (Ingredient.kt)
- ✅ Repository pattern implementation
- ✅ ViewModel architecture
- ✅ Basic scaling infrastructure (moreYield(), lessYield())

### Required Enhancements
- ✅ Enhanced Ingredient.withScaledAmount() method
- ✅ Comprehensive test coverage
- ✅ UI improvements for scaling controls
- ✅ Documentation and user guides

## Testing Strategy

### Unit Tests
```kotlin
@Test fun `test scaling with decimal amounts preserves precision`
@Test fun `test scaling with fractional amounts handles fractions correctly`
@Test fun `test scaling preserves ingredient ranges`
@Test fun `test scaling null amount preserves null`
@Test fun `test scaling with zero factor returns original ingredient`
// ... additional edge cases
```

### Integration Tests
- ViewModel scaling behavior
- Repository integration
- UI state management

### Performance Tests
- Large recipe scaling (>100 ingredients)
- Repeated scaling operations
- Memory usage with large datasets

## Files Modified

### Core Implementation
1. **app/src/main/java/com/herohiman/tournant/data/Ingredient.kt**
   - Enhanced `withScaledAmount()` method
   - Added comprehensive scaling logic

2. **app/src/main/java/com/herohiman/tournant/ui/RecipeViewModel.kt**
   - Added scaling ratio calculations
   - Enhanced yield and ingredient scaling
   - Added scaleUp(), scaleDown(), scaleReset() methods

3. **app/src/main/java/com/herohiman/tournant/ui/RecipeActivity.kt**
   - Added scaling UI controls
   - Enhanced yield input handling
   - Added ingredient-specific scaling dialog

### Test Coverage
1. **app/src/test/java/com/herohiman/tournant/IngredientScalingTest.kt**
   - Comprehensive unit tests for Ingredient scaling
   - Edge case coverage
   - Precision validation tests

## Dependencies

### Required Libraries
- **Kotlin** 1.9+
- **AndroidX** (ViewModel, LiveData, etc.)
- **Room** (Database)
- **Coroutines** (Async operations)
- **Jetpack Compose** (UI)

### External Dependencies
- **No external dependencies** - Uses existing project libraries

## Risk Assessment

### High Risk
- **Precision errors**: Floating-point arithmetic in scaling
- **Null handling**: Proper handling of optional fields
- **Performance**: Scaling large ingredient lists

### Medium Risk
- **Memory usage**: Scaling with large datasets
- **UI responsiveness**: Slow scaling with many ingredients
- **State management**: Complex scaling state synchronization

### Low Risk
- **Backward compatibility**: Existing recipes continue to work
- **User adoption**: Intuitive interface design
- **Documentation**: Clear user guides and examples

## Implementation Timeline

### Phase 1: Core Scaling Infrastructure
- Days 1-3: Enhance Ingredient.withScaledAmount() method
- Days 4-6: Implement ViewModel scaling logic
- Days 7-10: Basic UI integration

### Phase 2: Advanced Features
- Days 11-13: Add ingredient-specific scaling
- Days 14-16: Implement preview mode
- Days 17-20: Comprehensive testing

### Phase 3: polish and Documentation
- Days 21-23: UI improvements and polish
- Days 24-26: Documentation and user guides
- Days 27-30: Final testing and deployment

## Conclusion

This implementation of Story 2.1 (Ingredient Scaling) will provide Tournant users with robust ingredient scaling capabilities that:

1. **Maintain precision**: Proper handling of decimal and fractional values
2. **Preserve data**: All ingredient properties maintained during scaling
3. **Provide flexibility**: Multiple scaling methods (yield-based, ingredient-specific)
4. **Deliver performance**: Efficient scaling with real-time UI updates
5. **Ensure quality**: Comprehensive test coverage and validation

The enhanced scaling system will significantly improve the user experience for experienced users who need precise recipe adjustments, while maintaining full backward compatibility with existing recipe data.
