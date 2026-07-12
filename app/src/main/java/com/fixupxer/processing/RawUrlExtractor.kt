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
 * Extracts the first accepted URL without decoding any URL component.
 */
@Singleton
class RawUrlExtractor @Inject constructor() {
    fun extract(input: String): String? = UrlProcessor.findFirstValidUrl(input)
}
