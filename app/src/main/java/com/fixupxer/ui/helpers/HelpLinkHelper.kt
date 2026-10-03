// SPDX-License-Identifier: GPL-3.0-or-later
/* Copyright (C) 2026 NeatCode Labs */
package com.fixupxer.ui.helpers

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.fixupxer.R
import com.fixupxer.utils.HelpTopic

/** One visual and browser-handoff contract for contextual Help links. */
object HelpLinkHelper {
    fun bind(view: View, context: Context, @StringRes title: Int, topic: HelpTopic) {
        val activity = context.findActivity()
        if (activity == null) {
            view.visibility = View.GONE
            return
        }
        view.findViewById<TextView>(R.id.textHelpTitle).setText(title)
        view.contentDescription = context.getString(
            R.string.help_link_description, context.getString(title),
        )
        ViewCompat.setAccessibilityDelegate(view, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = Button::class.java.name
            }
        })
        view.setOnClickListener { open(activity, topic) }
    }

    fun open(activity: Activity, topic: HelpTopic = HelpTopic.CONTENTS): Boolean =
        UrlActionHelper.openUrlInExternalBrowser(
            activity.findViewById(android.R.id.content), activity, topic.url,
        )

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> if (baseContext === this) null else baseContext.findActivity()
        else -> null
    }
}
