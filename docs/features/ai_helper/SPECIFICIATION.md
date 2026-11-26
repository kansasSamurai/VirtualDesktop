# Feature: AI Project Standards Inventory & Posture Tool

## 1. Objective & Scope

* **Goal:** Inventory explicitly registered AI-enabled projects, evaluate each project's AI vendor setup and compliance with evolving personal engineering standards, and provide a preview/remediation workflow inside the Swing desktop application. This reduces manual overhead when AI tooling and personal conventions evolve across multiple repositories.
* **In Scope:**
* Explicit configuration/registration of AI project folders (with optional discovery hints for unregistered repos containing AI artifacts and per-project archetype tags).
* Read-only project scanning for AI instruction files, skills/rules/config folders, and personal standard markers/artifacts, plus basic metadata (e.g., line/size counts, timestamps) and multi-file precedence/conflict flags.
* Versioned personal standard profiles (policy definitions) with explicit per-project waivers/overrides.
* A dark-mode Swing UI using semantic status color indicators, a `SmartGrid` (list mode) project list on the left, and a right-hand posture panel with summary cards (Supported AI Vendors, Per-Vendor Setup Health, Personal Standards Compliance).
* Drill-down into a vendor's health details in the posture panel using a breadcrumb bar and `CardLayout`, plus dry-run diff/remediation previews before applying edits.

* **Non-Goals (Do NOT Build):**
* Automatic or blind bulk file overwrites across projects without explicit user selection and dry-run/diff preview.
* Crawling arbitrary unconfigured directories as mandatory scans or treating non-AI third-party clones as in-scope by default.
* Automated direct code generation from Figma/Claude Design into Swing layout code.

## 2. System Touchpoints (Where It Plugs In)

* **Existing Interfaces / Base Classes:** TBD (targeted as a tool within the existing Java Swing desktop application framework; exact tool SPI/base classes not specified in conversation)
* **Reference Implementation:** TBD (not specified in conversation)
* **Files / Registries to Update:** TBD (exact manifest/service registry paths not specified in conversation; expected to target Java 8 initially with a path toward Java 11/17+ and decouple scanner/policy logic from Swing UI classes)

## 3. Data & State Contracts

* **State Model:** TBD (high-level entities discussed include registered projects, archetype tags, detected AI vendors, vendor health reports/findings, versioned personal standard profiles, compliance status/waivers, and proposed remediation actions/diffs; exact types and default values TBD)
* **Persistence:** TBD (exact storage paths and schema formats TBD; discussed using versioned policy definitions and explicit project registry configuration)
* **Events / API:** TBD (exact typed events and method signatures TBD; UI navigation planned around `CardLayout` views such as `"POSTURE"` and `"VENDOR_DETAIL"` driven by breadcrumb/selection actions)

## 4. Behavior & Edge Cases

* **Primary Flow:**

1. User configures/selects registered AI project folders (and active personal standard version/filter) and triggers a scan.
2. Scanner inspects registered folders and computes vendor detection, per-vendor setup health, and personal standard compliance/drift.
3. User selects a project in the left-hand `SmartGrid` list to view its right-hand posture cards (Supported AI Vendors, Per-Vendor Setup Health, Personal Standards Compliance).
4. User clicks a Vendor Setup Health card to switch the posture panel via `CardLayout` + breadcrumb navigation to that vendor's detailed view, and navigates back via the breadcrumb.
5. User inspects drift/remediation actions and reviews a dry-run diff/preview before applying selected changes or recording a waiver.

* **Edge Cases & Failure Modes:**
* *Empty / Corrupt State:* TBD (exact fallback behavior for empty project lists, unreadable/malformed config or policy files, and missing paths TBD)
* *Invalid Input / Limits:* TBD (exact thresholds for oversized context files, invalid globs/frontmatter, broken file references, and error surfacing TBD)
* *Lifecycle (Open / Minimize / Dispose):* TBD (exact behavior on tool open, minimize, close/dispose, reload, and project-switch breadcrumb retention TBD)

## 5. Acceptance Criteria (Testable Proof)

* [ ] **AC-1:** Given registered AI project folders and an active personal standard version, when a scan runs, then the `SmartGrid` project list displays status indicators and selecting a project renders the three posture cards (Supported AI Vendors, Per-Vendor Setup Health, Personal Standards Compliance).
* [ ] **AC-2:** Given a project with a detected AI vendor on the posture screen, when the user clicks that vendor's health card, then the posture panel switches via `CardLayout` to the vendor detail view with an updated breadcrumb that navigates back to the posture overview.
* [ ] **AC-3:** Unit and smoke tests pass with zero regressions.

## 6. Open Questions

* [ ] `[NEEDS CLARIFICATION]` What are the exact Swing tool SPI interfaces, base classes, and registration files (`manifest.json` / service descriptors / launcher hooks) in the host desktop app?
* [ ] `[NEEDS CLARIFICATION]` What exact persistence format and file location should store the registered project list, archetype tags, waivers, and versioned personal standard rules?
* [ ] `[NEEDS CLARIFICATION]` When selecting a different project in the left list while viewing a `VENDOR_DETAIL` card, should the view reset to `POSTURE` or stay on `VENDOR_DETAIL` for the newly selected project?
* [ ] `[NEEDS CLARIFICATION]` What are the exact expected behaviors and fallbacks for empty/corrupt state, input/size limits, and tool open/minimize/dispose lifecycle events?

## Developer Notes

* Initial mockup is now `mockup_20261124.jpg`, was generated as `Gemini_Generated_Image_pwf9s6pwf9s6pwf9.jpg`
