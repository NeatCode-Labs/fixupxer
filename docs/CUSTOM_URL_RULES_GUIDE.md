# FixupXer Custom URL Rules Guide

Custom URL rules extend FixupXer's built-in cleaners with your own offline
matching and transformation logic. Rules can remove query parameters, rewrite
URLs, or extract destinations from redirect wrappers.

Rules are powerful. Start with a narrow scope, verify the result in **Test
Lab**, and only then enable the rule for normal use.

## Open and enable custom rules

1. Open **FixupXer > Settings > Custom URL rules**.
2. Select **Manage custom rules**.
3. Enable **Enable custom rules**.
4. Select **Add rule**, or install one of the bundled **Templates**.

The master switch disables or enables the entire rule engine. Each rule also
has its own enabled switch.

## Rule processing order

Every rule belongs to one fixed phase:

1. **Before built-in cleaning** — runs before FixupXer's built-in cleaners.
   Use this for redirect wrappers or transformations that must see the original
   query.
2. **After built-in cleaning** — runs after built-in tracking removal and
   before social-domain conversion. This is the safest default for additional
   parameter cleanup.
3. **After domain conversion** — runs last, after conversions such as
   `x.com` to `fixupx.com`.

Rules execute from top to bottom within their phase. Reorder them with the drag
handle or the up/down buttons. A rule cannot be dragged into another phase;
edit the rule and select a different phase instead.

Enable **Stop remaining rules in this phase after a change** when later rules
in the same phase should not process the changed URL.

## Processing contexts

A rule can run in any combination of:

- **Main screen**
- **Share menu**
- **Browser mode**

At least one context must be selected. For example, a rule that should affect
automatically opened links but not manually pasted URLs can be limited to
**Browser mode**.

## Include scopes

The include scope decides which URLs can match:

- **All URLs** — no host restriction.
- **Exact host** — matches only the entered host. `example.com` does not match
  `www.example.com`.
- **Domain and subdomains** — `example.com` matches both `example.com` and
  hosts such as `www.example.com`.
- **Host list** — enter hosts on separate lines or separated by commas. A host
  matches its subdomains by default; prefix it with `=` for exact matching:

  ```text
  example.com
  =accounts.example.net
  ```

- **URL regex** — an RE2/J expression searched against the complete current URL
  at that phase.

Enter hosts without a scheme, path, query, or fragment.

### Regex behavior

FixupXer uses RE2/J to prevent catastrophic regex backtracking. RE2 syntax does
not support lookaround or pattern backreferences. Regex matching searches for
a match within the URL, so use `^` and `$` when the entire URL must match.

Enable **Ignore case** when the selected scope or action should use
case-insensitive matching.

## Exclude scopes

Excludes are checked after the include scope. Enter one exclude per line:

- `example.com` — domain and all subdomains
- `=example.com` — exact host only
- `regex:pattern` — URL regex

Example:

```text
=accounts.example.com
regex:^https://example\.com/signed/
```

This is useful for protecting login, payment, signed-download, and other
sensitive URLs from a broad rule.

## Actions

### Remove all parameters

Removes the complete query while preserving the path and fragment.

```text
https://example.com/page?id=1&utm_source=x#part
→ https://example.com/page#part
```

### Remove named parameters

Enter parameter names on separate lines or separated by commas. Matching is
performed on decoded parameter names while untouched query tokens retain their
original encoding and order.

```text
utm_source
utm_campaign
fbclid
```

### Keep only named parameters

Keeps only the listed query parameters and removes all others. An empty list
removes the whole query.

Use narrow host scopes for this action: removing an unknown functional
parameter can break a URL.

### Regex search and replace

Runs an RE2/J replacement on the complete current URL. Choose whether to
replace the first match or all matches. Replacement capture references such as
`$1` and `${name}` are supported when the corresponding group exists.

The result must remain a valid absolute HTTP or HTTPS URL.

### Extract redirect parameter

Finds the named query parameter and uses its value as the new URL. Choose one
decode mode:

- **NONE** — use the raw value.
- **PERCENT ONCE** — decode `%xx` sequences once without treating `+` as a
  space.
- **FORM ONCE** — decode form encoding once, including `+` as a space.
- **BASE64URL** — decode an unpadded or padded Base64 URL value.

The extracted value must be a valid HTTP or HTTPS URL. It re-enters the
pipeline so it can be cleaned normally. FixupXer detects redirect cycles and
limits re-entry to five hops.

### Template rewrite

Builds a new URL from literal text and these placeholders:

- `{scheme}`
- `{host}`
- `{port}` — empty or prefixed with `:`
- `{path}`
- `{query}` — without `?`
- `{fragment}` — without `#`

Example:

```text
https://proxy.example/{host}{path}
```

The final template output must be an absolute HTTP or HTTPS URL. Add your own
`?` or `#` delimiters when using query or fragment placeholders.

## Test Lab

Before saving:

1. Enter a representative URL under **Test Lab**.
2. Select the Main, Share, or Browser profile.
3. Select **Run test**.
4. Review the result and trace.

The trace reports whether rules were applied, skipped by context or scope,
excluded, produced no change, or generated invalid output. Test both expected
matches and URLs that must remain unchanged.

## Manage rules

- Tap a rule to edit, duplicate, or delete it.
- Use the row switch to disable a rule without deleting it.
- Use **Templates** to add the bundled privacy or redirect-wrapper examples.
- Use **Delete all rules** only when you no longer need any saved rule.

The current limits are 200 rules, 100 host/parameter entries per relevant
field, 50 excludes per rule, and a 1 MiB import bundle.

## Import, export, and rollback

**Export** writes all rules to a versioned JSON bundle at a location you select
through Android's file picker. **Import** first validates the entire bundle and
shows a preview for these modes:

- **Add new** — add rules with new IDs and skip matching IDs.
- **Update matching** — update matching IDs and add new IDs.
- **Replace all** — replace the complete current rule set.

Import is atomic: an invalid bundle changes nothing. **Undo last import**
restores the most recent pre-import snapshot.

Rule bundles can contain private domains, regexes, templates, and test URLs.
Inspect a bundle before sharing it. FixupXer never uploads or synchronizes
rules.

## Recommended workflow

1. Give the rule a descriptive name.
2. Start with **Exact host** or **Domain and subdomains**.
3. Select only the required contexts.
4. Use **After built-in cleaning** unless the action requires another phase.
5. Add excludes for sensitive paths or hosts.
6. Test a matching URL and a non-matching URL.
7. Save and verify the result in the normal app flow.
8. Export a backup after building a stable rule set.
