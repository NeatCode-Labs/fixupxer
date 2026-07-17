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

package com.fixupxer.processing

enum class ChangeOperationType {
    PARAMETERS_REMOVED,
    REDIRECT_EXTRACTED,
    HOST_CONVERTED,
    CUSTOM_RULE_APPLIED,
    /**
     * A cleaner changed a same-host URL without removing identifiable query
     * parameter names, such as canonicalising an Amazon product link.
     */
    URL_CANONICALIZED
}

/**
 * Non-sensitive description of one URL-processing change.
 *
 * Parameter values and complete URLs must never be stored here.
 */
data class ChangeOperation(
    val type: ChangeOperationType,
    /** Cleaner/rule display name, e.g. "Instagram", "Redirect Unwrapper", or a custom rule name. */
    val source: String,
    /** Parameter names only — never values. Only for [ChangeOperationType.PARAMETERS_REMOVED]. */
    val parameterNames: List<String> = emptyList(),
    /** Host names only — never full URLs. For redirect and host-conversion operations. */
    val fromHost: String? = null,
    val toHost: String? = null
)
