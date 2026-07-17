// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fixupxer.ui.adapters

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.fixupxer.R
import com.fixupxer.databinding.ItemCustomRuleBinding
import com.fixupxer.rules.CustomUrlRule
import com.fixupxer.rules.RulePhase
import java.util.Collections

class CustomRuleAdapter(
    private val onEdit: (CustomUrlRule) -> Unit,
    private val onEnabled: (CustomUrlRule, Boolean) -> Unit,
    private val onReordered: (RulePhase, List<String>) -> Unit
) : RecyclerView.Adapter<CustomRuleAdapter.ViewHolder>() {
    private val items = mutableListOf<CustomUrlRule>()
    private var touchHelper: ItemTouchHelper? = null

    fun attachTouchHelper(helper: ItemTouchHelper) {
        touchHelper = helper
    }

    fun submitRules(rules: List<CustomUrlRule>) {
        items.clear()
        items.addAll(rules.sortedWith(compareBy({ it.phase.ordinal }, { it.sortOrder }, { it.id })))
        notifyDataSetChanged()
    }

    fun restoreEnabled(ruleId: String) {
        items.indexOfFirst { it.id == ruleId }
            .takeIf { it >= 0 }
            ?.let(::notifyItemChanged)
    }

    fun canMove(from: Int, to: Int): Boolean =
        from in items.indices && to in items.indices && items[from].phase == items[to].phase

    fun move(from: Int, to: Int): Boolean {
        if (!canMove(from, to)) return false
        Collections.swap(items, from, to)
        notifyItemMoved(from, to)
        notifyItemChanged(from)
        notifyItemChanged(to)
        publishOrder(items[to].phase)
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemCustomRuleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemCustomRuleBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(rule: CustomUrlRule) {
            binding.textRuleName.text = rule.name
            binding.textRulePhase.text = binding.root.context.getString(
                R.string.custom_rule_phase_label,
                phaseLabel(rule.phase)
            )
            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.isChecked = rule.enabled
            binding.switchEnabled.setOnCheckedChangeListener { _, checked ->
                onEnabled(rule, checked)
            }
            binding.ruleContent.setOnClickListener { onEdit(rule) }
            binding.root.setOnClickListener { onEdit(rule) }
            binding.imageDrag.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    touchHelper?.startDrag(this)
                }
                false
            }
            binding.buttonMoveUp.isEnabled = canMove(bindingAdapterPosition, bindingAdapterPosition - 1)
            binding.buttonMoveDown.isEnabled = canMove(bindingAdapterPosition, bindingAdapterPosition + 1)
            binding.buttonMoveUp.setOnClickListener {
                move(bindingAdapterPosition, bindingAdapterPosition - 1)
                binding.root.announceForAccessibility(binding.textRuleName.text)
            }
            binding.buttonMoveDown.setOnClickListener {
                move(bindingAdapterPosition, bindingAdapterPosition + 1)
                binding.root.announceForAccessibility(binding.textRuleName.text)
            }
        }

        private fun phaseLabel(phase: RulePhase): String = binding.root.context.getString(
            when (phase) {
                RulePhase.PRE_CLEAN -> R.string.custom_rule_phase_pre
                RulePhase.POST_CLEAN -> R.string.custom_rule_phase_post
                RulePhase.POST_CONVERSION -> R.string.custom_rule_phase_final
            }
        )
    }

    private fun publishOrder(phase: RulePhase) {
        onReordered(phase, items.filter { it.phase == phase }.map { it.id })
    }
}
