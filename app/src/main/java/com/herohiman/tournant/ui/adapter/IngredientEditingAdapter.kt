package com.herohiman.tournant.ui.adapter

import android.annotation.SuppressLint
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.textfield.TextInputLayout
import com.herohiman.tournant.R
import com.herohiman.tournant.data.Ingredient
import com.herohiman.tournant.data.IngredientLine
import com.herohiman.tournant.data.IngredientLine.IngredientGroupTitle
import com.herohiman.tournant.data.IngredientLine.IngredientItem
import com.herohiman.tournant.data.RecipeTitleId
import com.herohiman.tournant.databinding.RecyclerItemIngredientEditingBinding
import com.herohiman.tournant.databinding.RecyclerItemIngredientEditingGroupBinding
import com.herohiman.tournant.toStringForCooks
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

class IngredientEditingAdapter(
	private val ingredientEditingInterface: IngredientEditingInterface,
	private val ingredientLines: MutableList<IngredientLine>,
	private var titlesWithIds: List<RecipeTitleId>,
	private var itemSuggestions: List<String>,
	private var unitSuggestions: List<String>
): RecyclerView.Adapter<ViewHolder>() {

	companion object {
		private const val VIEW_TYPE_INGREDIENT = 0
		private const val VIEW_TYPE_GROUP = 1
	}

	class IngredientViewHolder(val binding: RecyclerItemIngredientEditingBinding) : ViewHolder(binding.root)
	class GroupTitleViewHolder(val binding: RecyclerItemIngredientEditingGroupBinding) : ViewHolder(binding.root)

	override fun getItemViewType(position: Int): Int {
		return if (ingredientLines[position] is IngredientItem) 0 else 1
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return if (viewType == VIEW_TYPE_INGREDIENT)
			IngredientViewHolder(RecyclerItemIngredientEditingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
		else
			GroupTitleViewHolder(RecyclerItemIngredientEditingGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		when (holder.itemViewType) {
			VIEW_TYPE_INGREDIENT -> fillIngredientInformation(holder as IngredientViewHolder, (ingredientLines[position] as IngredientItem).ingredient)
			VIEW_TYPE_GROUP -> fillGroupInformation(holder as GroupTitleViewHolder, ingredientLines[position] as IngredientGroupTitle)
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	fun fillIngredientInformation(holder: IngredientViewHolder, ingredient: Ingredient) {
		with (holder) {
			binding.ingredient = ingredient
			val isRef = ingredient.refId != null

			// Set hints for the first ingredient row
			listOf(
				binding.editAmountContainer to R.string.amount,
				binding.editUnitContainer to R.string.unit,
				binding.editItemContainer to R.string.ingredient
			).forEach {
				it.first.hint = if (ingredientLines.take(bindingAdapterPosition).filterIsInstance<IngredientItem>().isEmpty())
					it.first.context.getString(it.second)
				else
					null
			}

			binding.editAmount.apply {
				keyListener = DigitsKeyListener.getInstance("0123456789-" + DecimalFormatSymbols.getInstance().decimalSeparator)
				setText(ingredient.amount.toStringForCooks(false).plus(
					ingredient.amountRange.let {
						if (it != null) "-" + it.toStringForCooks(false) else ""
					}
				))
				doAfterTextChanged {
					if (it.toString().isEmpty()) {
						ingredient.amount = null
						ingredient.amountRange = null
					} else {
						val numbers = it.toString().split("-")
						try {
							ingredient.amount = NumberFormat.getInstance().parse(numbers[0])?.toDouble()
							ingredient.amountRange = if (numbers.size == 2) NumberFormat.getInstance().parse(numbers[1])?.toDouble() else null
						} catch (_: Exception) { }
					}
				}
			}

			// Prevents new lines on enter press (even for multiline text fields)
			val onEditorActionListener = TextView.OnEditorActionListener { view, actionId, _ ->
				if (actionId == EditorInfo.IME_NULL) {
					view.focusSearch(View.FOCUS_DOWN)?.requestFocus()
					true
				} else {
					false
				}
			}

			val inputFilter = InputFilter { source: CharSequence, start: Int, end: Int, _: Spanned, _: Int, _: Int ->
				val newText = source.subSequence(start, end).toString()
				if (newText.contains("\n")) newText.replace("\n", "") else null
			}

			binding.editUnit.apply {
				setOnEditorActionListener(onEditorActionListener)
				setRawInputType(InputType.TYPE_CLASS_TEXT)
				setSimpleItems(unitSuggestions.toTypedArray())
				threshold = 3
				filters = arrayOf(inputFilter)
			}

			binding.editItem.apply {
				setOnEditorActionListener(onEditorActionListener)
				setRawInputType(InputType.TYPE_CLASS_TEXT)
				filters = arrayOf(inputFilter)
				setSimpleItems(
					if (isRef)titlesWithIds.map { it.title }.toTypedArray()
					else itemSuggestions.toTypedArray()
				)
				threshold = if (isRef) 0 else 3
				if (isRef) {
					setText(titlesWithIds.find { it.id == ingredient.refId }?.title ?: "")
					doAfterTextChanged { editable ->
						ingredient.refId = titlesWithIds.find { it.title == editable.toString() }?.id
					}
					onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
						if (!hasFocus && ingredient.refId == null) {
							binding.editItem.text = null
						}
					}
				}
			}

			binding.editItemContainer.apply {
				endIconMinSize = 0
				endIconMode = if (isRef) TextInputLayout.END_ICON_DROPDOWN_MENU else TextInputLayout.END_ICON_NONE
			}


			if (ingredient.isInformationalOnly) {
				binding.editAmountContainer.visibility = View.GONE
				binding.editUnitContainer.visibility = View.GONE
				binding.editItemContainer.visibility = View.GONE
				binding.textSubstituteBadge.visibility = View.GONE
				binding.textInfoBadge.visibility = View.VISIBLE
				binding.editNoteTitleContainer.visibility = View.VISIBLE
				binding.editNoteValueContainer.visibility = View.VISIBLE
			} else {
				binding.editAmountContainer.visibility = View.VISIBLE
				binding.editUnitContainer.visibility = View.VISIBLE
				binding.editItemContainer.visibility = View.VISIBLE
				binding.textInfoBadge.visibility = View.GONE
				binding.editNoteTitleContainer.visibility = View.GONE
				binding.editNoteValueContainer.visibility = View.GONE

				val isSubstitute = !ingredient.substituteGroupId.isNullOrBlank()
				if (isSubstitute) {
					binding.textSubstituteBadge.visibility = View.VISIBLE
					binding.textSubstituteBadge.text = if (ingredient.isActiveSubstitute) {
						"${binding.textSubstituteBadge.context.getString(R.string.substitute_or)} ✓"
					} else {
						binding.textSubstituteBadge.context.getString(R.string.substitute_or)
					}
					binding.textSubstituteBadge.alpha = if (ingredient.isActiveSubstitute) 1.0f else 0.6f
				} else {
					binding.textSubstituteBadge.visibility = View.GONE
				}
			}

			binding.editNoteTitle.apply {
				setOnEditorActionListener(onEditorActionListener)
				filters = arrayOf(inputFilter)
				if (text?.toString() != (ingredient.noteTitle ?: "")) {
					setText(ingredient.noteTitle ?: "")
				}
				doAfterTextChanged {
					ingredient.noteTitle = it?.toString()?.trim()?.ifEmpty { null }
					updateInformationalItemString(ingredient)
				}
			}

			binding.editNoteValue.apply {
				setOnEditorActionListener(onEditorActionListener)
				filters = arrayOf(inputFilter)
				if (text?.toString() != (ingredient.noteValue ?: "")) {
					setText(ingredient.noteValue ?: "")
				}
				doAfterTextChanged {
					ingredient.noteValue = it?.toString()?.trim()?.ifEmpty { null }
					updateInformationalItemString(ingredient)
				}
			}

			binding.editPosition.setOnTouchListener { _, event ->
				if (event.action == MotionEvent.ACTION_DOWN) {
					ingredientEditingInterface.startDrag(holder)
				}
				false
			}

			binding.editOptions.setOnClickListener { view ->
				PopupMenu(view.context, view).apply {
					inflate(R.menu.options_ingredient)
					if (ingredient.optional)
						menu.findItem(R.id.toggle_optional).title = view.context.getString(R.string.make_mandatory)

					val toggleInfoItem = menu.findItem(R.id.toggle_informational)
					val toggleSubItem = menu.findItem(R.id.toggle_substitute)
					val setActiveSubItem = menu.findItem(R.id.set_active_substitute)
					val toggleOptionalItem = menu.findItem(R.id.toggle_optional)

					val isItemSubstitute = !ingredient.substituteGroupId.isNullOrBlank()
					if (ingredient.isInformationalOnly) {
						toggleInfoItem?.title = view.context.getString(R.string.mark_as_ingredient)
						toggleOptionalItem?.isVisible = false
						toggleSubItem?.isVisible = false
						setActiveSubItem?.isVisible = false
					} else {
						toggleInfoItem?.title = view.context.getString(R.string.mark_as_informational)
						if (isItemSubstitute) {
							toggleSubItem?.title = view.context.getString(R.string.unlink_substitute)
							setActiveSubItem?.isVisible = !ingredient.isActiveSubstitute
						} else {
							toggleSubItem?.title = view.context.getString(R.string.link_as_substitute)
							setActiveSubItem?.isVisible = false
						}
					}

					setOnMenuItemClickListener { item ->
						when (item.itemId) {
							R.id.remove_ingredient -> {
								val pos = holder.bindingAdapterPosition
								val removedIng = (ingredientLines.getOrNull(pos) as? IngredientItem)?.ingredient
								ingredientLines.removeAt(pos)
								notifyItemRemoved(pos)

								val subGroupId = removedIng?.substituteGroupId
								if (!subGroupId.isNullOrBlank()) {
									val remaining = ingredientLines.filterIsInstance<IngredientItem>()
										.filter { it.ingredient.substituteGroupId == subGroupId }
									if (remaining.size == 1) {
										remaining.first().ingredient.substituteGroupId = null
										remaining.first().ingredient.isActiveSubstitute = true
										notifyDataSetChanged()
									} else if (remaining.isNotEmpty() && remaining.none { it.ingredient.isActiveSubstitute }) {
										remaining.first().ingredient.isActiveSubstitute = true
										notifyDataSetChanged()
									}
								}
								true
							}

							R.id.toggle_optional -> {
								ingredient.optional = !ingredient.optional
								notifyItemChanged(holder.bindingAdapterPosition)
								true
							}

							R.id.toggle_informational -> {
								ingredient.isInformationalOnly = !ingredient.isInformationalOnly
								if (ingredient.isInformationalOnly) {
									ingredient.substituteGroupId = null
									ingredient.isActiveSubstitute = true
									ingredient.optional = false
									if (ingredient.noteTitle.isNullOrBlank() && ingredient.noteValue.isNullOrBlank()) {
										if (!ingredient.item.isNullOrBlank()) {
											ingredient.noteTitle = "जानकारी"
											ingredient.noteValue = ingredient.item
										}
									}
									updateInformationalItemString(ingredient)
								} else {
									if (ingredient.item.isNullOrBlank()) {
										val title = ingredient.noteTitle?.trim() ?: ""
										val value = ingredient.noteValue?.trim() ?: ""
										ingredient.item = if (title.isNotEmpty() && value.isNotEmpty()) "$title: $value" else title.ifEmpty { value.ifEmpty { null } }
									}
								}
								notifyItemChanged(holder.bindingAdapterPosition)
								true
							}

							R.id.toggle_substitute -> {
								val pos = holder.bindingAdapterPosition
								if (isItemSubstitute) {
									val oldGroupId = ingredient.substituteGroupId
									ingredient.substituteGroupId = null
									ingredient.isActiveSubstitute = true

									val remaining = ingredientLines.filterIsInstance<IngredientItem>()
										.filter { it.ingredient.substituteGroupId == oldGroupId }
									if (remaining.size == 1) {
										remaining.first().ingredient.substituteGroupId = null
										remaining.first().ingredient.isActiveSubstitute = true
									} else if (remaining.isNotEmpty() && remaining.none { it.ingredient.isActiveSubstitute }) {
										remaining.first().ingredient.isActiveSubstitute = true
									}
									notifyDataSetChanged()
								} else {
									val prevItem = ingredientLines.take(pos).filterIsInstance<IngredientItem>().lastOrNull()
									if (prevItem == null) {
										android.widget.Toast.makeText(view.context, R.string.cannot_link_first_ingredient, android.widget.Toast.LENGTH_SHORT).show()
									} else {
										val groupId = prevItem.ingredient.substituteGroupId ?: "sub_${System.currentTimeMillis()}"
										prevItem.ingredient.substituteGroupId = groupId
										prevItem.ingredient.isActiveSubstitute = true
										ingredient.substituteGroupId = groupId
										ingredient.isActiveSubstitute = false
										notifyDataSetChanged()
									}
								}
								true
							}

							R.id.set_active_substitute -> {
								val groupId = ingredient.substituteGroupId
								if (!groupId.isNullOrBlank()) {
									ingredientLines.filterIsInstance<IngredientItem>()
										.filter { it.ingredient.substituteGroupId == groupId }
										.forEach { it.ingredient.isActiveSubstitute = (it.ingredient == ingredient) }
									notifyDataSetChanged()
								}
								true
							}

							else -> false
						}
					}
					show()
				}
			}
		}
	}

	fun fillGroupInformation(holder: GroupTitleViewHolder, group: IngredientGroupTitle) {

		with (holder) {
			binding.group = group

			if (group.title == null) {
				val group = ingredientLines.take(bindingAdapterPosition).findLast { it is IngredientGroupTitle } as IngredientGroupTitle
				binding.showTitle.text = group.title
			}

			binding.editOptions.setOnClickListener { view ->
				PopupMenu(view.context, view).apply {
					inflate(R.menu.options_ingredient)
					menu.removeItem(R.id.toggle_optional)
					menu.removeItem(R.id.toggle_substitute)
					menu.removeItem(R.id.set_active_substitute)

					val toggleInfoItem = menu.findItem(R.id.toggle_informational)
					if (group.isInformationalGroup) {
						toggleInfoItem?.title = view.context.getString(R.string.mark_as_ingredient)
					} else {
						toggleInfoItem?.title = view.context.getString(R.string.mark_as_informational)
					}

					setOnMenuItemClickListener { item ->
						when (item.itemId) {
							R.id.remove_ingredient -> {
								val pos = holder.bindingAdapterPosition
								ingredientLines.removeAt(pos)
								val stopIndex = pos + ingredientLines.subList(
									pos,
									ingredientLines.size
								).indexOfFirst {
									it is IngredientGroupTitle
								}
								ingredientLines.removeAt(stopIndex)
								notifyItemRemoved(pos)
								notifyItemRemoved(stopIndex)
								true
							}
							R.id.toggle_informational -> {
								group.isInformationalGroup = !group.isInformationalGroup
								// Cascade informational exclusion status to all items in this group
								val pos = holder.bindingAdapterPosition
								val subList = ingredientLines.drop(pos + 1)
								for (line in subList) {
									if (line is IngredientGroupTitle) break
									if (line is IngredientItem) {
										line.ingredient.isInformationalOnly = group.isInformationalGroup
										if (group.isInformationalGroup) {
											line.ingredient.substituteGroupId = null
											line.ingredient.isActiveSubstitute = true
											line.ingredient.optional = false
											if (line.ingredient.noteTitle.isNullOrBlank() && line.ingredient.noteValue.isNullOrBlank()) {
												if (!line.ingredient.item.isNullOrBlank()) {
													line.ingredient.noteTitle = "जानकारी"
													line.ingredient.noteValue = line.ingredient.item
												}
											}
											updateInformationalItemString(line.ingredient)
										}
									}
								}
								notifyDataSetChanged()
								true
							}
							else -> false
						}
					}
					show()
				}
			}
		}
	}

	override fun onViewAttachedToWindow(holder: ViewHolder) {
		if (focus) {
			val ingHolder = holder as? IngredientViewHolder
			if (ingHolder != null) {
				if (ingHolder.binding.editNoteTitleContainer.visibility == View.VISIBLE) {
					ingHolder.binding.editNoteTitle.requestFocus()
				} else {
					ingHolder.binding.editAmount.requestFocus()
				}
			}
			(holder as? GroupTitleViewHolder)?.binding?.editTitle?.requestFocus()
			focus = false
		}
	}

	private fun updateInformationalItemString(ingredient: Ingredient) {
		if (ingredient.isInformationalOnly) {
			val title = ingredient.noteTitle?.trim() ?: ""
			val value = ingredient.noteValue?.trim() ?: ""
			ingredient.item = if (title.isNotEmpty() && value.isNotEmpty()) {
				"$title: $value"
			} else {
				title.ifEmpty { value.ifEmpty { null } }
			}
		}
	}

	override fun getItemCount(): Int {
		return ingredientLines.size
	}

	private var focus = false
	fun onItemInserted() {
		focus = true
	}

	interface IngredientEditingInterface {
		fun startDrag(holder: ViewHolder)
	}

}