# FixupXer Custom URL Rules Guide

Custom URL rules extend FixupXer's built-in cleaners with your own offline
matching and transformation logic. Rules can remove query parameters, rewrite
URLs, or extract destinations from redirect wrappers.

Rules are powerful. Start with a narrow scope, verify the result in **Test
Lab**, and only then enable the rule for normal use.

## Quick navigation

- [Build your first rule](#your-first-rule-step-by-step)
- [Choose a processing phase](#which-phase-should-i-choose)
- [Choose an include scope](#include-scopes)
- [Configure actions and see examples](#actions)
- [Test safely in Test Lab](#test-lab)
- [Fix common beginner mistakes](#common-beginner-mistakes)

## The 30-second mental model

Every rule answers three questions:

1. **Where should it run?** The context and include/exclude scopes decide which
   URLs can reach the action.
2. **When should it run?** The phase decides whether the rule sees the URL
   before or after FixupXer's built-in processing.
3. **What should it do?** The action removes parameters, extracts a destination,
   or rewrites the URL.

For this URL:

```text
https://shop.example.com/product/42?color=blue&utm_source=email#reviews
```

- `https` is the scheme.
- `shop.example.com` is the host.
- `/product/42` is the path.
- `color=blue&utm_source=email` is the query.
- `reviews` is the fragment.

Most beginner rules need only a host scope and a parameter-removal action. You
do not need regex for ordinary tracking parameters.

## Open and enable custom rules

1. Open **FixupXer > Settings > Custom URL rules**.
2. Enable **Enable custom rules** in Settings.
3. Select **Manage custom rules**.
4. Select **Add rule**, or install one of the bundled **Templates**.

Custom rules are off by default after a new installation or an update from
v2.0. The master switch in Settings disables or enables the entire rule engine.
Each rule also has its own enabled switch in the rule library and editor.

## Your first rule, step by step

This example removes two fictional newsletter parameters from `example.com`
and its subdomains while keeping the functional `article` parameter.

> `example.com` is a placeholder. Replace it with the real website you want to
> target.

Select **Add rule**, then fill the editor like this:

- **Rule name:** `Remove example.com newsletter tags`
- **Rule enabled:** On
- **Processing phase:** `After built-in cleaning`
- **Apply in:** Main screen, Share menu, and Browser mode
- **Include scope:** `Domain and subdomains`
- **Host, host list, or regex:** `example.com`
- **Exclude scopes:** Leave empty
- **Action:** `Remove named parameters`
- **Parameter names:** enter these on separate lines:

  ```text
  newsletter_id
  campaign_code
  ```

- **Ignore case:** On
- **Stop remaining rules in this phase:** Off

In **Test Lab**, use:

```text
https://news.example.com/read?article=42&newsletter_id=weekly&campaign_code=july#comments
```

Expected result:

```text
https://news.example.com/read?article=42#comments
```

Also test a URL that must not change:

```text
https://another-site.example/read?newsletter_id=weekly
```

If both results are correct, select **Save**. Make sure the master **Enable
custom rules** switch is still on.

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

### Which phase should I choose?

Use this beginner rule:

- Choose **After built-in cleaning** for removing extra tracking parameters.
- Choose **Before built-in cleaning** for a redirect wrapper whose destination
  is stored in a query parameter. If built-in cleaning removed that parameter
  first, there would be nothing left to extract.
- Choose **After domain conversion** only when the rule must see the converted
  host. For example, a rule scoped to `fixupx.com` cannot match before an
  `x.com` link is converted.

Example pipeline:

```text
Input
https://go.example.com/?target=https%3A%2F%2Fshop.example%2Fp%3Futm_source%3Dmail

Before built-in cleaning
Extract target → https://shop.example/p?utm_source=mail

Built-in cleaning
Remove utm_source → https://shop.example/p
```

An extracted redirect destination starts the pipeline again. This is why the
destination can still receive normal built-in cleaning.

## Processing contexts

A rule can run in any combination of:

- **Main screen**
- **Share menu**
- **Browser mode**

At least one context must be selected. For example, a rule that should affect
automatically opened links but not manually pasted URLs can be limited to
**Browser mode**.

Common choices:

- Select all three contexts when the same cleanup should always happen.
- Select only **Browser mode** for an aggressive rule you want on automatically
  opened links but not on manually pasted or shared links.
- Select only **Share menu** when preparing a special URL format for messaging
  apps.
- Select **Main screen** while experimenting, then add the other contexts after
  Test Lab and normal-flow testing succeed.

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

### Scope examples

If the scope is **Exact host** with:

```text
example.com
```

it matches `https://example.com/page`, but not
`https://www.example.com/page` or `https://news.example.com/page`.

If the scope is **Domain and subdomains** with the same value, all three URLs
match.

For **Host list**, this value:

```text
example.com
=static.example.net
```

matches `example.com` plus every `*.example.com` subdomain, and matches only
the exact `static.example.net` host. It does not match
`cdn.static.example.net`.

Use a **URL regex** when the path matters. This scope matches `/download` on
`example.com` or `www.example.com`, but not other paths:

```regex
^https://(?:www\.)?example\.com/download(?:[/?#]|$)
```

### Regex behavior

FixupXer uses RE2/J to prevent catastrophic regex backtracking. RE2 syntax does
not support lookaround or pattern backreferences. Regex matching searches for
a match within the URL, so use `^` and `$` when the entire URL must match.

Regex characters such as `.`, `?`, `+`, `(`, and `)` have special meanings.
For example, `example\.com` uses `\.` to mean a literal dot. If you do not know
regex yet, prefer a host scope and one of the parameter actions.

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

For example, suppose a rule removes all parameters from `example.com`, but
account and checkout links need their parameters. Keep the include scope as
**Domain and subdomains**, then enter:

```text
=accounts.example.com
regex:^https://shop\.example\.com/checkout(?:[/?#]|$)
```

The first line protects only `accounts.example.com`. Remove the `=` if all of
its subdomains should also be protected. The second line protects only the
checkout path on `shop.example.com`.

## Actions

### Remove all parameters

Removes the complete query while preserving the path and fragment.

Example editor values:

- **Processing phase:** `After built-in cleaning`
- **Include scope:** `Exact host`
- **Scope value:** `links.example.com`
- **Action:** `Remove all parameters`

```text
https://links.example.com/page?id=1&utm_source=x#part
→ https://links.example.com/page#part
```

Use this only when the target website never needs query parameters. Otherwise,
use **Remove named parameters**.

### Remove named parameters

Enter parameter names on separate lines or separated by commas. Matching is
performed on decoded parameter names while untouched query tokens retain their
original encoding and order.

Example action value:

```text
campaign_code
newsletter_id
```

Example:

```text
https://example.com/article?id=42&campaign_code=july&newsletter_id=weekly
→ https://example.com/article?id=42
```

Names are literal. `utm_*` is not a wildcard; list every name you want removed.
Enable **Ignore case** if `Campaign_Code` and `campaign_code` should be treated
as the same name.

### Keep only named parameters

Keeps only the listed query parameters and removes all others. An empty list
removes the whole query.

Use narrow host scopes for this action: removing an unknown functional
parameter can break a URL.

Example editor values:

- **Include scope:** `Exact host`
- **Scope value:** `video.example.com`
- **Action:** `Keep only named parameters`
- **Parameter names:** `v` and `list`, on separate lines

```text
https://video.example.com/watch?v=abc123&list=favorites&tracking=mail&theme=dark
→ https://video.example.com/watch?v=abc123&list=favorites
```

Here `v` and `list` are assumed to be functional. Confirm the real website's
requirements before using a keep-only rule.

### Regex search and replace

Runs an RE2/J replacement on the complete current URL. Choose whether to
replace the first match or all matches. Replacement capture references such as
`$1` and `${name}` are supported when the corresponding group exists.

The result must remain a valid absolute HTTP or HTTPS URL.

Example: rename `/old-product/42` to `/product/42` while keeping everything
after the ID.

- **Include scope:** `Exact host`
- **Scope value:** `shop.example.com`
- **Action:** `Regex search and replace`
- **Pattern:**

  ```regex
  ^(https://shop\.example\.com)/old-product/([0-9]+)(.*)$
  ```

- **Regex replacement:** `$1/product/$2$3`
- **Replace all matches:** Off

```text
https://shop.example.com/old-product/42?color=blue#reviews
→ https://shop.example.com/product/42?color=blue#reviews
```

`$1`, `$2`, and `$3` insert the text captured by the three parenthesized
groups. Test regex rewrites carefully: replacing too much can create an
invalid URL.

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

Example: unwrap a fictional `go.example.com` redirect.

- **Processing phase:** `Before built-in cleaning`
- **Include scope:** `Exact host`
- **Scope value:** `go.example.com`
- **Action:** `Extract redirect parameter`
- **Parameter name:** `target`
- **Decode mode:** `PERCENT ONCE`
- **Stop remaining rules in this phase:** On

```text
https://go.example.com/out?target=https%3A%2F%2Fdestination.example%2Fpage%3Fid%3D42
→ https://destination.example/page?id=42
```

Choose the decode mode that matches the wrapper. Start with **PERCENT ONCE**
for values containing `%3A`, `%2F`, and similar sequences. Use **FORM ONCE**
only when the wrapper uses form encoding, where `+` means a space.

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

Example: send documentation paths to another host and intentionally drop the
old query and fragment.

- **Include scope:** `Exact host`
- **Scope value:** `docs.example.com`
- **Action:** `Template rewrite`
- **Template:** `https://archive.example.net{path}`

```text
https://docs.example.com/guides/setup?source=menu#android
→ https://archive.example.net/guides/setup
```

To preserve the query and fragment, use:

```text
https://archive.example.net{path}?{query}#{fragment}
```

This can leave a trailing `?` or `#` when the source URL has no query or
fragment. If that matters, use a regex rewrite with separate rules for the
different URL shapes.

## Test Lab

Before saving:

1. Enter a representative URL under **Test Lab**.
2. Select the Main, Share, or Browser profile.
3. Select **Run test**.
4. Review the result and trace.

The trace reports whether rules were applied, skipped by context or scope,
excluded, produced no change, or generated invalid output. Test both expected
matches and URLs that must remain unchanged.

Test Lab runs the complete FixupXer pipeline with the unsaved draft inserted
among your saved rules. The final result can therefore include built-in
cleaning and changes from other enabled custom rules.

Useful trace statuses include:

- **APPLIED** — the rule matched and changed the URL.
- **NO_OP** — the rule matched, but there was nothing to change. For example,
  the requested parameter was not present or was already removed.
- **SCOPE_MISS** — the URL did not match the include scope.
- **CONTEXT_MISS** — the selected Test profile is not enabled for the rule.
- **EXCLUDED** — an exclude protected this URL.
- **INVALID_OUTPUT** — the action tried to create something that was not a
  valid HTTP or HTTPS URL, so FixupXer kept the previous safe URL.

For every rule, try at least these cases:

1. A matching URL containing the data you want changed.
2. A matching URL that needs no change.
3. A URL from another host.
4. An excluded URL, if the rule has excludes.
5. A URL with a fragment such as `#comments`, to confirm it is preserved when
   expected.

### Example Test Lab session

For the first-rule example earlier in this guide:

```text
Test profile: MAIN
Input:  https://news.example.com/read?article=42&newsletter_id=weekly
Result: https://news.example.com/read?article=42
Trace:  POST_CLEAN: Remove example.com newsletter tags — APPLIED
```

Then test:

```text
Input:  https://other.example/read?newsletter_id=weekly
Result: https://other.example/read?newsletter_id=weekly
Trace:  POST_CLEAN: Remove example.com newsletter tags — SCOPE_MISS
```

## Start quickly with Templates

The large **Templates** button adds ready-made example rules to **Your rule
library**. It is useful when you want working rules immediately or want to
learn by opening and editing complete examples instead of starting with an
empty editor.

Tap **Templates**, then choose one of these bundled sets:

- **Privacy basics** adds two rules that remove common `utm_*` campaign
  parameters and click identifiers such as `fbclid`, `gclid`, and `msclkid`.
- **Offline redirect wrappers** adds two rules that extract the real
  destination from Facebook and LinkedIn outbound redirect links.

The selected rules appear in the library as ordinary editable rules. You can
tap them to inspect their scope and action, test them in Test Lab, disable
them, reorder them, duplicate them, or delete them. Choosing the same set
again does not create duplicate copies; the import result reports how many
rules were added or skipped.

Templates are stored and processed entirely offline. If the master **Enable
custom rules** switch in Settings is off, the template rules remain saved but
do not run until you enable it.

> **Templates** is a library of ready-made rules. It is different from the
> **Template rewrite** action, which builds a new URL from placeholders inside
> one rule.

## Manage rules

- Tap a rule to edit, duplicate, or delete it.
- Use the row switch to disable a rule without deleting it.
- Use **Templates** to add the bundled examples described above.
- Use **Delete all rules** only when you no longer need any saved rule.

The current limits are 200 rules, 100 host/parameter entries per relevant
field, 50 excludes per rule, and a 1 MiB import bundle.

When several rules share a phase, imagine passing a piece of paper down the
list. The first rule receives the phase's starting URL; every following rule
receives the result left by the rule above it.

Example:

```text
Starting URL:
https://example.com/page?campaign_code=july&session=abc

Rule 1 removes campaign_code:
https://example.com/page?session=abc

Rule 2 keeps only session:
https://example.com/page?session=abc
```

Reversing those two rules happens to produce the same result, but many regex,
redirect, and template combinations do not. Use drag ordering deliberately.

## Import, export, and rollback

**Export** writes all rules to a versioned JSON bundle at a location you select
through Android's file picker. **Import** first validates the entire bundle and
shows a preview for these modes:

- **Add new** — add rules with new IDs and skip matching IDs.
- **Update matching** — update matching IDs and add new IDs.
- **Replace all** — replace the complete current rule set.

Example: your app currently has rules `A` and `B`, while the imported file has
rules `B` and `C`.

- **Add new** keeps local `A` and `B`, skips imported `B`, and adds `C`.
- **Update matching** keeps `A`, updates `B` from the file, and adds `C`.
- **Replace all** removes the local set and leaves only imported `B` and `C`.

Matching uses each rule's internal ID, not its displayed name. Two rules with
the same name can still be different rules.

Import is atomic: an invalid bundle changes nothing. **Undo last import**
restores the most recent pre-import snapshot.

Rule bundles can contain private domains, regexes, templates, and test URLs.
Inspect a bundle before sharing it. FixupXer never uploads or synchronizes
rules.

## Common beginner mistakes

### The rule does nothing

Check all of these:

- The master **Enable custom rules** switch is on.
- The rule's own switch is on.
- The current Main, Share, or Browser context is selected.
- The host value does not contain `https://`, a path, or a trailing query.
- An exclude is not protecting the URL.
- The chosen phase still contains the parameter or host you expect to match.
- Another earlier rule has not already removed or rewritten it.

Use Test Lab and read the trace status; it usually identifies the missed
condition directly.

### `www.example.com` does not match

An **Exact host** scope for `example.com` intentionally excludes `www`. Change
the scope to **Domain and subdomains**, or enter the exact `www.example.com`
host.

### I entered `https://example.com` as a host

Host fields accept only:

```text
example.com
```

Use a URL regex only when you need to match the scheme, path, query, or
fragment.

### I entered `utm_*`, but parameters remain

Parameter actions do not support wildcards. Enter each complete name on its
own line, or use a carefully tested regex replacement.

### My redirect extraction reports invalid output

The decoded parameter must become a complete `http://` or `https://` URL.
Check that you selected the correct parameter name and decode mode. If the
value still begins with `%` after **PERCENT ONCE**, it may be encoded twice;
FixupXer intentionally decodes only one layer per redirect action.

### My template removed information

A template preserves only the placeholders you include. If the template omits
`{query}` or `{fragment}`, that part is intentionally dropped.

### A broad rule broke a website

Disable the individual rule immediately. Narrow it to one exact host, add an
exclude, or replace **Remove all parameters** with **Remove named parameters**.
The original URL remains visible in the Main/Share before-and-after display and
can also remain in history when history is enabled.

## Recommended workflow

1. Give the rule a descriptive name.
2. Start with **Exact host** or **Domain and subdomains**.
3. Select only the required contexts.
4. Use **After built-in cleaning** unless the action requires another phase.
5. Add excludes for sensitive paths or hosts.
6. Test a matching URL and a non-matching URL.
7. Save and verify the result in the normal app flow.
8. Export a backup after building a stable rule set.
