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

import com.fixupxer.cleaners.CleanerService
import com.fixupxer.rules.CustomRuleEngine
import com.fixupxer.rules.CustomRuleRepository
import com.fixupxer.rules.RulePhase
import com.fixupxer.rules.RuleSnapshot
import com.fixupxer.rules.RuleTraceStatus
import com.fixupxer.rules.RuleTraceStep
import com.fixupxer.utils.Constants
import com.fixupxer.utils.InputValidator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlProcessingOrchestrator @Inject constructor(
    private val extractor: RawUrlExtractor,
    private val normalizer: UrlNormalizer,
    private val cleanerService: CleanerService,
    private val domainConversionService: DomainConversionService,
    private val ruleEngine: CustomRuleEngine,
    private val customRuleRepository: CustomRuleRepository
) {
    suspend fun process(
        rawInput: String,
        options: ProcessingOptions,
        snapshotOverride: RuleSnapshot? = null
    ): PipelineProcessingResult {
        val validation = InputValidator.validate(rawInput)
        require(validation is InputValidator.ValidationResult.Valid) { "Invalid URL input" }
        val extracted = extractor.extract(validation.value)
            ?: throw IllegalArgumentException("Invalid URL format")
        val comparison = normalizer.normalize(extracted).comparisonUrl
        val snapshot = snapshotOverride ?: if (options.customRulesEnabled) {
            customRuleRepository.awaitSnapshot()
        } else {
            RuleSnapshot.EMPTY
        }

        var current = comparison
        var builtinChanged = false
        var domainChanged = false
        var customChanged = false
        val trace = mutableListOf<RuleTraceStep>()
        val seen = mutableSetOf<String>()

        for (hop in 0..Constants.MAX_PIPELINE_REENTRIES) {
            val cycleKey = normalizer.normalize(current).cycleKey
            if (!seen.add(cycleKey)) {
                addSystemTrace(trace, RuleTraceStatus.CYCLE, current, "Redirect cycle detected")
                break
            }
            if (hop >= Constants.MAX_PIPELINE_REENTRIES) {
                addSystemTrace(trace, RuleTraceStatus.HOP_LIMIT, current, "Redirect hop limit reached")
                break
            }

            val pre = ruleEngine.applyPhase(
                current,
                RulePhase.PRE_CLEAN,
                options.profile,
                snapshot,
                options.traceEnabled
            )
            current = pre.url
            customChanged = customChanged || pre.changed
            trace.addBounded(pre.trace)
            if (pre.redirectRequested) continue

            if (options.cleanTracking) {
                val cleaned = if (options.useCache) {
                    cleanerService.deepClean(current)
                } else {
                    cleanerService.deepCleanWithoutCache(current)
                }
                builtinChanged = builtinChanged || cleaned != current
                current = cleaned
            }

            val post = ruleEngine.applyPhase(
                current,
                RulePhase.POST_CLEAN,
                options.profile,
                snapshot,
                options.traceEnabled
            )
            current = post.url
            customChanged = customChanged || post.changed
            trace.addBounded(post.trace)
            if (post.redirectRequested) continue

            val converted = domainConversionService.convert(
                current,
                options.convertDomains,
                options.instagramProxy,
                options.tiktokProxy
            )
            domainChanged = domainChanged || converted != current
            current = converted

            val final = ruleEngine.applyPhase(
                current,
                RulePhase.POST_CONVERSION,
                options.profile,
                snapshot,
                options.traceEnabled
            )
            current = final.url
            customChanged = customChanged || final.changed
            trace.addBounded(final.trace)
            if (final.redirectRequested) continue

            return PipelineProcessingResult(
                originalUrl = comparison,
                url = current,
                wasAlreadyClean = current == comparison,
                builtinChanged = builtinChanged,
                domainConverted = domainChanged,
                customRuleChanged = customChanged,
                rulesRevision = snapshot.revision,
                trace = trace
            )
        }

        return PipelineProcessingResult(
            originalUrl = comparison,
            url = current,
            wasAlreadyClean = current == comparison,
            builtinChanged = builtinChanged,
            domainConverted = domainChanged,
            customRuleChanged = customChanged,
            rulesRevision = snapshot.revision,
            trace = trace
        )
    }

    private fun MutableList<RuleTraceStep>.addBounded(steps: List<RuleTraceStep>) {
        val remaining = Constants.MAX_TRACE_STEPS - size
        if (remaining > 0) addAll(steps.take(remaining))
    }

    private fun addSystemTrace(
        trace: MutableList<RuleTraceStep>,
        status: RuleTraceStatus,
        url: String,
        message: String
    ) {
        if (trace.size >= Constants.MAX_TRACE_STEPS) return
        trace += RuleTraceStep(
            ruleId = "pipeline",
            ruleName = "Pipeline",
            phase = RulePhase.PRE_CLEAN,
            status = status,
            before = url,
            after = url,
            message = message
        )
    }
}
