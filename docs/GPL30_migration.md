# Project-wide Migration to **GNU General Public License v3.0**

The steps below let maintainers/automation bots migrate **FixupXer** and every historical commit *retroactively* to GPL-3.0 (or GPL-3.0-or-later) while keeping legal compliance, CI/CD, and ecosystem artefacts intact.  Follow them **in order**; tasks that require human sign-off are flagged ⚠️.

> NOTE  Where the term *repository* is used it refers to **all three** variants: `FixupXer_app`, the GitHub mirror, and any downstream forks that the core team controls.

---
## A  Prerequisites & Scope Confirmation
1. Identify **copyright holders** of all code _and_ embedded assets.
   • run `git shortlog -sne` to list contributors.  
   • for external libraries/assets ensure they are already GPL-compatible or have dual-license.
2. Verify no file is subject to a more restrictive licence (e.g. proprietary third-party icons). Replace or seek relicensing.
3. Create `docs/licenses/audit-YYYY-MM.md` → store evidence for each dependency and asset.

---
## B  Licence File & Boilerplate Update
1. Add root-level `LICENSE` **exactly** matching the SPDX text for *GPL-3.0-only* **or** *GPL-3.0-or-later* (choose one).  
2. Update `gradle/libs.versions.toml` → `license = "GPL-3.0-or-later"` for internal modules.
3. ⚙️  CI step: add `licensee` GitHub action to fail build if new files miss SPDX header.

### Source header template (Kotlin/Java)
```kotlin
// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * FixupXer - URL Enhancer
 * Copyright (C) 2020-2025  <copyright holders>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, **either version 3 of the License, or
 * (at your option) any later version**.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
```
Automate insertion using `addlicense` or `spotless`.  
*Commit all header insertions in a **single dedicated commit** immediately after adding `LICENSE` to keep Git history clear.*

---
## C  Retroactive Licensing Clause
Because the same legal entity owns the codebase, you may state:
> "All versions of FixupXer, including historical commits prior to version X.Y.Z, are hereby relicensed under the terms of the GNU General Public License version 3 or any later version at your option.  This supersedes any previous licence notices."

Steps:
1. Commit the clause to `LICENSE` *and* `README.md`.
2. Tag release `vPRE-GPL` (signed with **GPG** for authenticity) pointing to the last commit **before** the licence change for traceability.
3. ⚠️  If **external contributors** exist → obtain written consent (email or PR approval). Where consent is missing, you must either:  
   • remove/replace their code, or  
   • keep dual licensing for those files.
4. Add Git note `relicense-consents` with copies of the consents.

---
## D  Repository Hygiene & Automation
1. Run `reuse lint --strict` (https://reuse.software) until repository is 100 % compliant.
2. Update `build.gradle.kts` → `publishing { license.set("GPL-3.0-or-later") }`.
3. Update Play Store & FDroid metadata:  
   • `GITHUB/fixupxer/metadata/com.fixupxer.yml -> License: GPL-3.0-or-later` *(use full SPDX identifier).*  
   • If publishing Maven/AAR artefacts, update the generated POM `<licenses>` tag accordingly.
4. Regenerate `NOTICE` files via Gradle task.

---
## E  Binary Distribution Requirements
1. Ensure **complete Corresponding Source** is offered alongside APK/AAB (FDroid does this automatically). For Play Store releases add a link in “Privacy Policy”.
2. Include copy of GPL in-app (`assets/licence.html`).
3. Splash screen → menu → “About” must reference GPL-3.0.
4. Add a Gradle `packagingOptions { resources { includes += "/LICENSE*" } }` (or equivalent Assets copy task) so the full GPL text is **always** bundled inside APK/AAB artefacts.

---
## F  Future Contributions & DCO Workflow
1. Adopt the **Developer Certificate of Origin**: every commit **must** include a `Signed-off-by: Name <email>` line (`git commit -s` does this automatically).
2. Add `CONTRIBUTING.md` detailing the DCO, how to sign commits, and confirming that contributions are licensed under GPL-3.0-or-later.
3. Configure a GitHub Action (e.g. `Signed-off-by` DCO bot) to block any pull-request lacking a valid sign-off.
4. A separate CLA is **not required**.

---
## G  Checklist (CI will fail if any box unchecked)  
- [ ] `LICENSE` file present and matches SPDX checksum.  
- [ ] All source files contain GPL header **and an SPDX license identifier line**.  
- [ ] Docs, store & POM metadata updated with `GPL-3.0-or-later`.  
- [ ] Reuse.software passes.  
- [ ] Consents archived.  
- [ ] App shows GPL notice in-app and ships GPL text inside APK/AAB.  
- [ ] **Every commit after migration contains a valid DCO `Signed-off-by` line**.  

---
*End of GPL migration guide — awaiting further orders* 