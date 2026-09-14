# Local backup and restore

Use Settings → Backup & restore to export a JSON file or restore a previously exported file. Backups contain app settings, custom frontend domains, custom rules and saved app choices. URL history is not included. Keep the file somewhere you trust: custom rules and frontend domains may contain information you consider private.

## Browser choices and preferred browser

New exports use schema v2 and include a Browser preference for each of the nine platforms. TikTok and Instagram support embed and custom targets; X and Bluesky also support readers; Reddit and Pinterest support readers and custom targets; Facebook supports custom targets. YouTube and Threads remain Clean only in Browser mode.

Browser choices are independent of Main/Share choices. Turning a choice off can retain its target for later use. A temporarily unavailable reader may fall back to another active reader without overwriting the saved target. An unavailable embed or custom target is not silently substituted.

Version 2.8.0 also includes the preferred browser package in schema-v2 exports.
Older files without this field restore **Always ask**. A browser that is not
installed on the receiving device remains a saved preference, but cannot launch;
choose an available replacement in Browser settings or when opening a link.
Malformed package names and FixupXer itself are rejected during validation.

Only saved frontend changes are exported. Uncommitted Browser picker additions,
edits and removals are discarded when cancelled and never enter a backup.

## Importing older backups

Schema v1 remains supported. Import migrates the four old reader settings, including disabled selections, and resets the other Browser platforms to Clean only. It replaces the entire Browser map, so a newer TikTok or Instagram Browser selection does not remain enabled accidentally after restoring an older file.

An app upgrade migrates existing reader settings once. Upgrading the app does not enable new Browser conversions automatically. Known retired frontend selections are handled before validating an import; unsafe frontend domains cannot be restored as custom targets.

## Validation and interrupted restore

Restore validates the complete file before applying it, including platform/target combinations, custom-domain ownership and collisions, and rule data. Custom targets are checked against the domains in the imported backup. Invalid files are rejected without partially applying settings.

Restore replaces settings and rules. Browser dispatch is paused while restore or recovery is incomplete. If applying the backup fails, FixupXer attempts to recover the previous state and retains its recovery record until that succeeds. A failed save is not reported as a successful restore. After an interruption, reopen the app and let recovery finish before retrying a Browser action.

See [Browser mode](BROWSER_MODE_GUIDE.md) for frontend selection, fallback and Retry behavior.
