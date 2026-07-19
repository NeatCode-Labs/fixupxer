// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2026  NeatCode Labs
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

package com.fixupxer

import android.view.LayoutInflater
import android.view.View
import com.fixupxer.databinding.ItemConfigurationStatusRowBinding
import com.fixupxer.ui.helpers.ConfigurationStatusDialogHelper
import com.fixupxer.ui.helpers.ConfigurationStatusDialogHelper.DetailSemanticType
import com.google.android.material.R as MaterialR
import androidx.appcompat.R as AppCompatR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ConfigurationStatusDetailRowTest {

    @Test
    fun `status row layout binds semantic icon and full text`() {
        val context = RuntimeEnvironment.getApplication()
        val binding = ItemConfigurationStatusRowBinding.inflate(LayoutInflater.from(context))
        val text = "Browser integration: On (active)"

        binding.textStatusLine.text = text
        val iconRes = ConfigurationStatusDialogHelper.iconResFor(DetailSemanticType.ACTIVE)
        binding.imageStatusIcon.setImageResource(iconRes)

        assertEquals(text, binding.textStatusLine.text.toString())
        assertEquals(iconRes, ConfigurationStatusDialogHelper.iconResFor(DetailSemanticType.ACTIVE))
        assertEquals(
            AppCompatR.attr.colorPrimary,
            ConfigurationStatusDialogHelper.iconTintAttrFor(DetailSemanticType.ACTIVE),
        )
    }

    @Test
    fun `status row icon is decorative for accessibility`() {
        val context = RuntimeEnvironment.getApplication()
        val binding = ItemConfigurationStatusRowBinding.inflate(LayoutInflater.from(context))

        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, binding.imageStatusIcon.importantForAccessibility)
        assertFalse(binding.imageStatusIcon.isFocusable)
    }
}
