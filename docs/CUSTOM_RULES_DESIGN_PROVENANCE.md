# Custom URL Rules: Design, Wire Format, and Provenance

## Scope

FixupXer 2.1 implements issue #6 and the agreed advanced rule system entirely
offline. Rules can:

1. remove every query parameter;
2. remove selected parameter names;
3. perform RE2/J search and replacement;
4. target one exact host;
5. target a domain and all label-boundary subdomains;
6. target a host group;
7. target a URL through an RE2/J expression or all URLs; and
8. execute in user-defined order.

Advanced behavior includes excludes, keep-only parameters, Main/Share/Browser
contexts, three phases, stop-after-match, offline redirect extraction, component
templates, live Test Lab trace, bundled templates, JSON import/export, conflict
policies, and three import rollback snapshots.

## Pipeline contract

`InputValidator → RawUrlExtractor → UrlNormalizer → PRE_CLEAN rules →
CleanerService → POST_CLEAN rules → built-in domain conversion →
POST_CONVERSION rules`.

Path, query, and fragment are not whole-string decoded. Parameter actions split
only on raw `&`, strictly percent-decode names without converting `+` to a space,
and preserve every untouched token byte-for-byte. Every mutation must remain an
absolute hierarchical HTTP(S) URL.

Each request freezes one compiled rule snapshot and preference snapshot.
Redirect extraction re-enters at PRE_CLEAN, with normalized cycle detection and
a five-reentry hard limit. Rule evaluation order within a phase is
`sortOrder`, then UUID.

## Stable bundle schema v1

The top-level JSON fields are:

- `format`: `fixupxer-custom-rules`
- `schemaVersion`: `1`
- `appVersion`
- `rules`

Rule enums use stable wire strings, never Kotlin enum ordinals or class names.
Each rule stores UUID, name, enabled state, order, phase, contexts, include
scope, excludes, typed action, stop flag, timestamps, and test vectors.
Unknown fields in schema v1 are ignored. Unknown types or a newer schema version
reject the complete bundle before any database write.

Import is atomic:

- **Add new** skips UUID conflicts and appends new rules per phase.
- **Update matching** preserves existing positions unless phase changes.
- **Replace all** uses bundle order.

All affected phases are reindexed without gaps. A validated rollback snapshot is
created in the same Room transaction before mutation.

## Limits

- Input / working URL: 2,048 characters
- Legacy fallback scan: 1,000 characters
- Custom rules: 200
- Regex source: 2,048 characters
- Regex program size: 10,000
- Replacement / template: 4,096 characters
- Scope entries: 100
- Excludes per rule: 50
- Test vectors per rule: 20
- Redirect reentries: 5
- Trace steps: 1,000
- Bundle / snapshot: 1 MiB
- Rollback snapshots: 3

User regex always uses RE2/J 1.8; there is no fallback to Java's backtracking
regex engine.

## Requirements-to-test mapping

- Frozen behavior with rules disabled: `UrlPipelineDifferentialTest`
- Raw query preservation and every action: `CustomRuleEngineTest`
- Scope labels, excludes, contexts, invalid outputs: `CustomRuleEngineTest`
- Redirect reentry/hop limit: `UrlPipelineDifferentialTest`
- Bundle round-trip and bundled assets: `RuleBundleCodecTest`
- Atomic import, rollback, cold empty database: `CustomRuleRepositoryTest`
- Room 1→2 history preservation: `CustomRuleMigrationTest`
- No-code Settings/editor route: `CustomRulesUiTest`
- 200-rule performance budget: `CustomRulesPerformanceTest`
- Zero merged permissions: `ManifestPrivacyTest`

## Clean-room provenance and attribution

The engine, UI, bundle schema, and bundled templates were independently authored
for FixupXer under GPL-3.0-or-later from the public feature requirements. No
ClearURLs, AdGuard, Brave, Firefox, URLCheck, Linkumori, or other third-party
ruleset, regex corpus, parser, or source file was copied.

RE2/J 1.8 is used unmodified as the user-regex engine. Its upstream Go License
is reproduced verbatim in `LICENSES/LicenseRef-RE2J.txt`, and attribution appears
in `NOTICE` and `README.md`.
