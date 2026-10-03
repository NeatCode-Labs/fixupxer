// SPDX-License-Identifier: GPL-3.0-or-later
/* Copyright (C) 2026 NeatCode Labs */
package com.fixupxer.utils

/** Stable destinations in the single public Help document. */
enum class HelpTopic(private val anchor: String?) {
    CONTENTS(null),
    LINK_CLEANING("link-cleaning"),
    ALTERNATIVE_FRONTENDS("alternative-frontends"),
    CUSTOM_PROXIES("custom-proxies"),
    BROWSER_MODE("browser-mode"),
    BROWSER_FRONTENDS("browser-frontends"),
    AFTER_CLEAN("after-clean"),
    CUSTOM_RULES("custom-rules"),
    TEST_LAB("test-lab"),
    TEACH_FROM_EXAMPLE("teach-from-example"),
    HISTORY_AND_BACKUP("history-and-backup"),
    APP_SETTINGS("app-settings"),
    CONFIGURATION_STATUS("configuration-status");

    val url: String
        get() = Constants.HELP_URL + (anchor?.let { "#$it" } ?: "")
}
