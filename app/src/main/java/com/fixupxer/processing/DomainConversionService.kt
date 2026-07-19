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

package com.fixupxer.processing

import com.fixupxer.UrlProcessor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps built-in social-domain conversion separate from custom rules.
 */
@Singleton
class DomainConversionService @Inject constructor(
    private val urlProcessor: UrlProcessor
) {
    fun convert(
        url: String,
        enabled: Boolean,
        selections: ProxySelections,
    ): String = urlProcessor.applyDomainConversions(
        url = url,
        convertToAlternative = enabled,
        selections = selections,
    )
}
