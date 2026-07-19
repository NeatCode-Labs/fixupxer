// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  NeatCode Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fixupxer.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fixupxer.PreferencesManager
import com.fixupxer.R
import com.fixupxer.databinding.DialogProxyPickerBinding
import com.fixupxer.databinding.ItemProxyActionBinding
import com.fixupxer.databinding.ItemProxyGroupHeaderBinding
import com.fixupxer.databinding.ItemProxyOptionBinding
import com.fixupxer.databinding.ItemProxySectionHeaderBinding
import com.fixupxer.ui.helpers.FrontendDisplayHelper
import com.fixupxer.ui.helpers.SnackbarHelper
import com.fixupxer.utils.AlternativeFrontendCatalog
import com.fixupxer.utils.FrontendRole
import com.fixupxer.utils.FrontendTarget
import com.fixupxer.utils.ProxyPlatform
import com.fixupxer.utils.ProxyRoster
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Shared bottom-sheet picker for alternative frontends on all supported platforms.
 */
object ProxyPickerDialogHelper {

    private sealed class PickerItem {
        data class SectionHeader(
            val titleRes: Int,
            val subtitleRes: Int,
            val showTopDivider: Boolean,
        ) : PickerItem()

        data class Header(val titleRes: Int) : PickerItem()

        data class Option(
            val target: FrontendTarget,
            val checked: Boolean,
            val showDelete: Boolean,
        ) : PickerItem()

        enum class ActionType {
            ADD_CUSTOM,
            RESTORE_BUILT_INS,
            EDIT_TOGGLE,
        }

        data class Action(val type: ActionType, val labelRes: Int) : PickerItem()
    }

    fun show(
        context: Context,
        layoutInflater: LayoutInflater,
        platform: ProxyPlatform,
        preferencesManager: PreferencesManager,
        onChanged: () -> Unit,
    ) {
        val binding = DialogProxyPickerBinding.inflate(layoutInflater)
        var editMode = false
        var dialog: BottomSheetDialog? = null

        lateinit var adapter: ProxyPickerAdapter

        fun refresh() {
            val targets = ProxyRoster.activeTargets(platform)
            val disabled = preferencesManager.getDisabledBuiltIns(platform)
            val selected = preferencesManager.getSelectedProxyDomain(platform)
            val showEmpty = targets.isEmpty()

            binding.recyclerViewProxyPicker.isVisible = !showEmpty
            binding.emptyStateProxyPicker.isVisible = showEmpty
            binding.buttonEmptyAddCustom.isVisible = showEmpty
            binding.buttonEmptyRestoreBuiltIns.isVisible = disabled.isNotEmpty()

            if (showEmpty) {
                binding.textViewProxyEmptyMessage.text =
                    context.getString(R.string.proxy_empty_message)
            } else {
                adapter.submit(
                    buildFullItems(
                        targets = targets,
                        selectedDomain = selected,
                        editMode = editMode,
                        hasDisabledBuiltIns = disabled.isNotEmpty(),
                    )
                )
            }
        }

        binding.textViewProxyPickerTitle.text = context.getString(
            R.string.proxy_picker_title,
            context.getString(FrontendDisplayHelper.platformNameRes(platform))
        )
        binding.proxyPickerInfoIcon.setOnClickListener {
            showInfoDialog(context, platform)
        }

        adapter = ProxyPickerAdapter(
            onOptionClick = { target ->
                if (editMode) return@ProxyPickerAdapter
                preferencesManager.setSelectedProxyDomain(platform, target.domain)
                onChanged()
                dialog?.dismiss()
            },
            onDeleteClick = { target ->
                handleDelete(
                    context = context,
                    platform = platform,
                    target = target,
                    preferencesManager = preferencesManager,
                    snackbarAnchor = binding.root,
                    onChanged = {
                        refresh()
                        onChanged()
                    }
                )
            },
            onActionClick = { action ->
                when (action) {
                    PickerItem.ActionType.ADD_CUSTOM -> showAddCustomDialog(context, platform, preferencesManager) {
                        refresh()
                        onChanged()
                    }
                    PickerItem.ActionType.RESTORE_BUILT_INS -> {
                        val before = preferencesManager.getSelectedProxyDomain(platform)
                        preferencesManager.restoreBuiltIns(platform)
                        refresh()
                        if (before != preferencesManager.getSelectedProxyDomain(platform)) {
                            onChanged()
                        }
                    }
                    PickerItem.ActionType.EDIT_TOGGLE -> {
                        editMode = !editMode
                        refresh()
                    }
                }
            }
        )

        binding.recyclerViewProxyPicker.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewProxyPicker.adapter = adapter

        binding.buttonEmptyAddCustom.setOnClickListener {
            showAddCustomDialog(context, platform, preferencesManager) {
                refresh()
                onChanged()
            }
        }
        binding.buttonEmptyRestoreBuiltIns.setOnClickListener {
            val before = preferencesManager.getSelectedProxyDomain(platform)
            preferencesManager.restoreBuiltIns(platform)
            refresh()
            if (before != preferencesManager.getSelectedProxyDomain(platform)) {
                onChanged()
            }
        }

        refresh()

        dialog = BottomSheetDialog(context).apply {
            setContentView(binding.root)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.isDraggable = true
            show()
        }
    }

    /**
     * Selection-only picker for Browser privacy Reader targets.
     *
     * The empty-state restore action re-enables only the platform's built-in
     * READER targets (never embed/automatic ones) and reports it through
     * [onRosterRestored] so the calling dialog can refresh its rows. The caller
     * owns draft Save/Cancel semantics and may roll the restore back.
     */
    fun showPrivacySelection(
        context: Context,
        layoutInflater: LayoutInflater,
        platform: ProxyPlatform,
        preferencesManager: PreferencesManager,
        selectedTargetId: String?,
        onRosterRestored: () -> Unit = {},
        onSelected: (FrontendTarget) -> Unit,
    ) {
        val binding = DialogProxyPickerBinding.inflate(layoutInflater)
        var dialog: BottomSheetDialog? = null

        lateinit var adapter: ProxyPickerAdapter

        fun refresh() {
            val disabled = preferencesManager.getDisabledBuiltIns(platform)
            val allReaders = AlternativeFrontendCatalog.builtInReaders(platform)
            val readers = allReaders.filter { it.id !in disabled }
            val showEmpty = readers.isEmpty()

            binding.recyclerViewProxyPicker.isVisible = !showEmpty
            binding.emptyStateProxyPicker.isVisible = showEmpty
            binding.buttonEmptyAddCustom.isVisible = false
            binding.buttonEmptyRestoreBuiltIns.isVisible = readers.size < allReaders.size

            if (showEmpty) {
                binding.textViewProxyEmptyMessage.text =
                    context.getString(R.string.proxy_empty_message)
            } else {
                adapter.submit(
                    buildPrivacySelectionItems(
                        readers = readers,
                        selectedTargetId = selectedTargetId,
                    )
                )
            }
        }

        binding.textViewProxyPickerTitle.text = context.getString(
            R.string.privacy_picker_title,
            context.getString(FrontendDisplayHelper.platformNameRes(platform))
        )
        binding.proxyPickerInfoIcon.setOnClickListener {
            showInfoDialog(context, platform)
        }

        adapter = ProxyPickerAdapter(
            onOptionClick = { target ->
                onSelected(target)
                dialog?.dismiss()
            },
            onDeleteClick = { },
            onActionClick = { },
        )

        binding.recyclerViewProxyPicker.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewProxyPicker.adapter = adapter

        binding.buttonEmptyRestoreBuiltIns.setOnClickListener {
            preferencesManager.restoreBuiltInReaders(platform)
            refresh()
            onRosterRestored()
        }

        refresh()

        dialog = BottomSheetDialog(context).apply {
            setContentView(binding.root)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.isDraggable = true
            show()
        }
    }

    private fun buildFullItems(
        targets: List<FrontendTarget>,
        selectedDomain: String?,
        editMode: Boolean,
        hasDisabledBuiltIns: Boolean,
    ): List<PickerItem> {
        val items = mutableListOf<PickerItem>()
        val custom = targets.filter { it.id.startsWith("custom:") }
        val builtIn = targets.filterNot { it.id.startsWith("custom:") }
        var sectionHeaderCount = 0

        fun addSectionHeader(titleRes: Int, subtitleRes: Int) {
            items.add(
                PickerItem.SectionHeader(
                    titleRes = titleRes,
                    subtitleRes = subtitleRes,
                    showTopDivider = sectionHeaderCount > 0,
                )
            )
            sectionHeaderCount++
        }

        fun appendGroup(titleRes: Int, filter: (FrontendTarget) -> Boolean) {
            val group = builtIn.filter(filter)
            if (group.isEmpty()) return
            items.add(PickerItem.Header(titleRes))
            group.forEach { target ->
                items.add(
                    PickerItem.Option(
                        target = target,
                        checked = target.domain == selectedDomain,
                        showDelete = editMode,
                    )
                )
            }
        }

        val embedTargets = builtIn.filter { it.role == FrontendRole.EMBED }
        if (embedTargets.isNotEmpty()) {
            addSectionHeader(R.string.proxy_section_embed, R.string.proxy_section_embed_subtitle)
            embedTargets.forEach { target ->
                items.add(
                    PickerItem.Option(
                        target = target,
                        checked = target.domain == selectedDomain,
                        showDelete = editMode,
                    )
                )
            }
        }

        val recommended = builtIn.filter { it.role == FrontendRole.READER && !it.communityGroup }
        val automatic = builtIn.filter { it.role == FrontendRole.AUTOMATIC }
        val community = builtIn.filter { it.role == FrontendRole.READER && it.communityGroup }
        val experimental = builtIn.filter { it.role == FrontendRole.EXPERIMENTAL }
        val hasBuiltInPrivacy = recommended.isNotEmpty() ||
            automatic.isNotEmpty() ||
            community.isNotEmpty() ||
            experimental.isNotEmpty()

        if (hasBuiltInPrivacy) {
            addSectionHeader(R.string.proxy_section_privacy, R.string.proxy_section_privacy_subtitle)
        }

        appendGroup(R.string.proxy_group_recommended) {
            it.role == FrontendRole.READER && !it.communityGroup
        }
        appendGroup(R.string.proxy_group_automatic) { it.role == FrontendRole.AUTOMATIC }
        appendGroup(R.string.proxy_group_community) {
            it.role == FrontendRole.READER && it.communityGroup
        }
        appendGroup(R.string.proxy_group_experimental) { it.role == FrontendRole.EXPERIMENTAL }

        if (custom.isNotEmpty()) {
            if (!hasBuiltInPrivacy) {
                addSectionHeader(R.string.proxy_section_privacy, R.string.proxy_section_privacy_subtitle)
            }
            items.add(PickerItem.Header(R.string.proxy_group_custom))
            custom.forEach { target ->
                items.add(
                    PickerItem.Option(
                        target = target,
                        checked = target.domain == selectedDomain,
                        showDelete = editMode,
                    )
                )
            }
        }

        if (hasDisabledBuiltIns) {
            items.add(
                PickerItem.Action(
                    PickerItem.ActionType.RESTORE_BUILT_INS,
                    R.string.proxy_action_restore_builtins,
                )
            )
        }
        items.add(PickerItem.Action(PickerItem.ActionType.ADD_CUSTOM, R.string.proxy_action_add_custom))
        items.add(
            PickerItem.Action(
                PickerItem.ActionType.EDIT_TOGGLE,
                if (editMode) R.string.proxy_action_done_editing else R.string.proxy_action_edit,
            )
        )
        return items
    }

    private fun buildPrivacySelectionItems(
        readers: List<FrontendTarget>,
        selectedTargetId: String?,
    ): List<PickerItem> {
        val items = mutableListOf<PickerItem>()

        fun appendGroup(titleRes: Int, filter: (FrontendTarget) -> Boolean) {
            val group = readers.filter(filter)
            if (group.isEmpty()) return
            items.add(PickerItem.Header(titleRes))
            group.forEach { target ->
                items.add(
                    PickerItem.Option(
                        target = target,
                        checked = target.id == selectedTargetId,
                        showDelete = false,
                    )
                )
            }
        }

        appendGroup(R.string.proxy_group_recommended) { !it.communityGroup }
        appendGroup(R.string.proxy_group_community) { it.communityGroup }
        return items
    }

    private fun handleDelete(
        context: Context,
        platform: ProxyPlatform,
        target: FrontendTarget,
        preferencesManager: PreferencesManager,
        snackbarAnchor: View,
        onChanged: () -> Unit,
    ) {
        val isCustom = target.id.startsWith("custom:")
        val activeBefore = ProxyRoster.activeTargets(platform)
        val isLast = activeBefore.size <= 1

        fun performDelete() {
            val selectedBefore = preferencesManager.getSelectedProxyDomain(platform)
            if (isCustom) {
                preferencesManager.removeCustomProxy(platform, target.domain)
                reselectAfterRemoval(platform, preferencesManager, selectedBefore, target.domain)
            } else {
                preferencesManager.disableBuiltIn(platform, target.id)
            }
            onChanged()
            if (!isCustom) {
                SnackbarHelper.showShortWithAction(
                    anchor = snackbarAnchor,
                    message = context.getString(
                        R.string.proxy_undo_disabled,
                        FrontendDisplayHelper.displayLabel(target),
                    ),
                    actionLabel = context.getString(R.string.undo),
                ) {
                    preferencesManager.enableBuiltIn(platform, target.id)
                    if (selectedBefore == target.domain &&
                        ProxyRoster.activeTargets(platform).any { it.domain == selectedBefore }
                    ) {
                        preferencesManager.setSelectedProxyDomain(platform, selectedBefore)
                    }
                    onChanged()
                }
            }
        }

        if (isLast) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.proxy_delete_last_title)
                .setMessage(R.string.proxy_delete_last_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    performDelete()
                    if (ProxyRoster.activeTargets(platform).isEmpty()) {
                        preferencesManager.clearSelectedProxyDomain(platform)
                        onChanged()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            performDelete()
        }
    }

    private fun reselectAfterRemoval(
        platform: ProxyPlatform,
        preferencesManager: PreferencesManager,
        selectedBefore: String?,
        removedDomain: String,
    ) {
        if (selectedBefore != removedDomain) return
        val next = ProxyRoster.activeTargets(platform).firstOrNull()?.domain
        if (next != null) {
            preferencesManager.setSelectedProxyDomain(platform, next)
        } else {
            preferencesManager.clearSelectedProxyDomain(platform)
        }
    }

    private fun showAddCustomDialog(
        context: Context,
        platform: ProxyPlatform,
        preferencesManager: PreferencesManager,
        onAdded: () -> Unit,
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_custom_proxy, null)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.customProxyInputLayout)
        val input = view.findViewById<TextInputEditText>(R.id.customProxyInput)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.proxy_add_dialog_title)
            .setView(view)
            .setPositiveButton(R.string.add, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val domain = ProxyRoster.normalizeCustomProxyInput(input.text?.toString() ?: "")
            val errorRes = when {
                !ProxyRoster.isValidProxyDomainFormat(domain) ->
                    R.string.proxy_error_invalid_domain
                ProxyRoster.isDuplicate(platform, domain) ->
                    R.string.proxy_error_duplicate
                ProxyRoster.isReservedDomain(domain) ->
                    R.string.proxy_error_reserved_domain
                else -> null
            }
            if (errorRes != null) {
                inputLayout.error = context.getString(errorRes)
            } else {
                preferencesManager.addCustomProxy(platform, domain)
                onAdded()
                dialog.dismiss()
            }
        }
    }

    private fun showInfoDialog(context: Context, platform: ProxyPlatform) {
        val intro = context.getString(FrontendDisplayHelper.infoIntroRes(platform))
        val generic = HtmlCompat.fromHtml(
            context.getString(R.string.proxy_picker_info_generic),
            HtmlCompat.FROM_HTML_MODE_LEGACY,
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.proxy_picker_info_title)
            .setMessage("$intro\n\n$generic")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private class ProxyPickerAdapter(
        private val onOptionClick: (FrontendTarget) -> Unit,
        private val onDeleteClick: (FrontendTarget) -> Unit,
        private val onActionClick: (PickerItem.ActionType) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var items: List<PickerItem> = emptyList()

        fun submit(newItems: List<PickerItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int = when (items[position]) {
            is PickerItem.SectionHeader -> VIEW_SECTION_HEADER
            is PickerItem.Header -> VIEW_HEADER
            is PickerItem.Option -> VIEW_OPTION
            is PickerItem.Action -> VIEW_ACTION
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                VIEW_SECTION_HEADER -> SectionHeaderViewHolder(
                    ItemProxySectionHeaderBinding.inflate(inflater, parent, false),
                )
                VIEW_HEADER -> HeaderViewHolder(
                    ItemProxyGroupHeaderBinding.inflate(inflater, parent, false),
                )
                VIEW_OPTION -> OptionViewHolder(
                    ItemProxyOptionBinding.inflate(inflater, parent, false),
                )
                else -> ActionViewHolder(
                    ItemProxyActionBinding.inflate(inflater, parent, false),
                )
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is PickerItem.SectionHeader -> (holder as SectionHeaderViewHolder).bind(item)
                is PickerItem.Header -> (holder as HeaderViewHolder).bind(item)
                is PickerItem.Option -> (holder as OptionViewHolder).bind(item, onOptionClick, onDeleteClick)
                is PickerItem.Action -> (holder as ActionViewHolder).bind(item, onActionClick)
            }
        }

        override fun getItemCount(): Int = items.size

        private class SectionHeaderViewHolder(
            private val binding: ItemProxySectionHeaderBinding,
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: PickerItem.SectionHeader) {
                binding.sectionTopDivider.isVisible = item.showTopDivider
                binding.textViewSectionTitle.setText(item.titleRes)
                binding.textViewSectionSubtitle.setText(item.subtitleRes)
                ViewCompat.setAccessibilityHeading(binding.textViewSectionTitle, true)
            }
        }

        private class HeaderViewHolder(
            private val binding: ItemProxyGroupHeaderBinding,
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: PickerItem.Header) {
                binding.textViewGroupHeader.setText(item.titleRes)
                ViewCompat.setAccessibilityHeading(binding.textViewGroupHeader, true)
            }
        }

        private class OptionViewHolder(
            private val binding: ItemProxyOptionBinding,
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(
                item: PickerItem.Option,
                onOptionClick: (FrontendTarget) -> Unit,
                onDeleteClick: (FrontendTarget) -> Unit,
            ) {
                val context = binding.root.context
                binding.proxyDomainText.text =
                    FrontendDisplayHelper.optionPrimaryLabel(context, item.target)
                binding.proxyRadio.isChecked = item.checked

                if (item.target.role == FrontendRole.EXPERIMENTAL) {
                    binding.proxySubtitleText.isVisible = true
                    binding.proxySubtitleText.setText(R.string.proxy_experimental_warning)
                } else {
                    binding.proxySubtitleText.isVisible = false
                }

                binding.proxyDeleteButton.isVisible = item.showDelete
                binding.proxyDeleteButton.contentDescription =
                    if (item.target.id.startsWith("custom:")) {
                        context.getString(R.string.delete_proxy_button_desc)
                    } else {
                        context.getString(
                            R.string.disable_builtin_button_desc,
                            FrontendDisplayHelper.displayLabel(item.target),
                        )
                    }
                binding.proxyDeleteButton.setOnClickListener { onDeleteClick(item.target) }

                binding.root.setOnClickListener { onOptionClick(item.target) }
                binding.root.contentDescription = binding.proxyDomainText.text
                binding.root.isClickable = true
                binding.root.isFocusable = true
                ViewCompat.setAccessibilityDelegate(
                    binding.root,
                    object : AccessibilityDelegateCompat() {
                        override fun onInitializeAccessibilityNodeInfo(
                            host: View,
                            info: AccessibilityNodeInfoCompat,
                        ) {
                            super.onInitializeAccessibilityNodeInfo(host, info)
                            info.className = RadioButton::class.java.name
                            info.isCheckable = true
                            info.isChecked = item.checked
                        }
                    },
                )
            }
        }

        private class ActionViewHolder(
            private val binding: ItemProxyActionBinding,
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: PickerItem.Action, onActionClick: (PickerItem.ActionType) -> Unit) {
                binding.buttonProxyAction.setText(item.labelRes)
                binding.buttonProxyAction.setOnClickListener { onActionClick(item.type) }
            }
        }

        companion object {
            private const val VIEW_SECTION_HEADER = 0
            private const val VIEW_HEADER = 1
            private const val VIEW_OPTION = 2
            private const val VIEW_ACTION = 3
        }
    }
}
