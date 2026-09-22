# System Setup — ImpEx & Scripts

Import the following files to create the required system data. All files are available in the [`downloads`](/downloads/ ':ignore') directory of this documentation. Import them via the `patches` extension or the HAC console (`/hac/console/impex/import`).

Import the files in the order listed below.

---

## 1. `usergroups.impex` — User Groups and Access Rights

**[Download usergroups.impex](/downloads/usergroups.impex ':ignore')**

Creates the backoffice user groups and roles required by CX Translations, sets catalog-version access rights, and grants sync-job permissions.

**Required adjustments:**

| Macro | Description | Example |
|---|---|---|
| `$contentCatalog` | ID of your content catalog | `electronicsContentCatalog` |
| `$productCatalog` | ID of your product catalog | `electronicsProductCatalog` |
| `$classificationCatalog` | ID of your classification catalog | `electronicsClassification` |
| `$contentCatalogSyncJob` | Code of the content catalog sync job | `electronicsContentCatalog-sync` |

The file creates the following groups (hierarchical — each inherits from the one above):

| Group | Description |
|---|---|
| `cxTranslationsBasicGroup` | Base group — read access to all CX Translate item types. Not a Backoffice role. Do not assign users directly. |
| `cxTranslationsConfiguratorGroup` | Manages Localized Object configurations and autoconfiguration cron jobs. |
| `cxTranslationsTranslatorGroup` | Manages labels, values, and translation jobs; approves AI translations. |
| `cxTranslationsAiConfiguratorGroup` | Manages AI translation settings. |
| `cxTranslationsAdminGroup` | Full access to all CX Translate items. |

Demo users (`cxtranslationsconfigurator`, `cxtranslationstranslator`, `cxtranslationsaiconfigurator`, `cxtranslationsadmin`) are created with default password `*:1234` and are **inactive by default**. Activate them in Backoffice or change passwords before use. **Remove them or change passwords before production deployment.**

---

## 2. `synchronisation.impex` — Catalog Sync Configuration

**[Download synchronisation.impex](/downloads/synchronisation.impex ':ignore')**

Adds `CxLocalizedBundle` and `CxLocalizedLabel` to the root types of the content catalog sync job, and excludes the `labels` attribute from deep copy during sync (labels become shared references, not duplicates).

**Required adjustments:**

| Macro | Description |
|---|---|
| `$contentCatalogSyncJob` | Code of the content catalog sync job (same as in `usergroups.impex`) |

---

## 3. [OPTIONAL] `admin-component.impex` — CMS Admin Component

**[Download admin-component.impex](/downloads/admin-component.impex ':ignore')**

Creates the `CxTranslationsAdminComponent` CMS flex component and places it in the `SiteLogoSlot`. Also creates user-group–based CMS restrictions so only members of `frontendTranslatorGroup` and `cmstranslatorgroup` (ASM) can see the component.

**Required adjustments:**

| Macro | Description |
|---|---|
| `$contentCatalog` | ID of your content catalog |

The macro `$contentCV` is derived automatically. In a **multicountry setup**, use the **global content catalog** (the catalog without any parents).

---

## 4. [OPTIONAL] `admin-component-labels.impex` — UI Labels for the Admin Component

**[Download admin-component-labels.impex](/downloads/admin-component-labels.impex ':ignore')**

Inserts the `CxLocalizedBundle` named `translations` and populates it with all UI label keys used by the CX Translations admin component. English (`en`) and Polish (`pl`) translations are provided out of the box.

**Required adjustments:**

| Macro | Description |
|---|---|
| `$contentCatalog` | ID of your content catalog |

> [!NOTE] Add `value[lang=XX]` columns for any additional project languages.

---

## 5. `cx-translate-cronjob-retention.impex` — CronJob Cleanup

**[Download cx-translate-cronjob-retention.impex](/downloads/cx-translate-cronjob-retention.impex ':ignore')**

Creates retention rules and cron jobs that clean up completed `CxTranslationCronJob` and `CxApproveTranslationsCronJob` items older than 48 hours, triggered daily at midnight.

No project-specific adjustments required. Optionally adjust `retentionTimeSeconds` or `cronExpression`.

---

## 6. [OPTIONAL] `autoconfigure.groovy` — Auto-configure Localized Values

**[Download autoconfigure.groovy](/downloads/autoconfigure.groovy ':ignore')**

Runs `CxLocalizedValueConfigService.autoConfigure(true)` to automatically generate the Localized Values configuration by scanning all item types and their localizable attributes.

Import via the **HAC Groovy console** (`/hac/console/scripting`) after all ImpEx files have been imported. Required only if you are using the **Localized Values** feature.

For more details see [Localized Values — Autoconfiguration](../localizedValues.md#autoconfiguration).

---

## 7. [OPTIONAL] AI Translation Settings

If you plan to use automated translations, configure the relevant provider **after** the above files have been imported.

See [Step 5 — Configure Automated Translations](../addingCXTranslate.md#step-5--configure-automated-translations-optional) for a list of providers and links to their configuration guides.

---

## Step 4 — Execute Content Catalog Synchronisation

After importing all ImpEx files, run a **content catalog synchronisation** to publish the new CMS component and labels from Staged to Online.

---

## Summary

| File | When to import | Notes |
|---|---|---|
| `usergroups.impex` | Required — system setup | Adjust catalog macros and sync job code |
| `synchronisation.impex` | Required — system setup | Adjust sync job code |
| `admin-component.impex` | Optional — required for the storefront admin component | Adjust content catalog macro |
| `admin-component-labels.impex` | Optional — required for the storefront admin component | Add language columns as needed |
| `cx-translate-cronjob-retention.impex` | Recommended — system setup | No adjustments needed |
| `autoconfigure.groovy` | Optional — only if using Localized Values | Run after all ImpEx imports via HAC |
| AI provider ImpEx | Optional — only if using automated translations | Provider-specific; see individual guides |
