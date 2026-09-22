# Installation

> [!NOTE] This page has been reorganised into more granular guides. Please use the links below.

- [Commerce Extensions Installation](installation/commerce-extensions.md)
- [Storefront Module Installation](installation/storefront-module.md)
- [System Setup — ImpEx & Scripts](installation/system-setup.md)
- [Basic Configuration](installation/basic-configuration.md)

The content below is kept for reference.

---

## Prerequisites

- SAP Commerce Cloud with the `patches` extension (or any mechanism to run ImpEx files during system setup)
- SAP Composable Storefront

---

## Step 1 — Add Commerce Extensions

1. Clone the repository.
1. Copy the `commerce-extensions/cx-translations` directory into `bin/custom` of your Commerce implementation. It contains three extensions:
   - `cxtranslations`
   - `cxtranslationsbackoffice`
   - `cxtranslationssmartedit` (preview)
1. Add extensions to your `localextensions.xml`:
```xml
<extension name="cxtranslations"/>
<extension name="cxtranslationsbackoffice"/>
<extension name="cxtranslationssmartedit"/>
```
1. Run `ant clean all` and start the server.

---

## Step 2 — Add the Storefront Module

1. Copy `js-storefront/cxtranslations` into the `src` directory of your SAP Composable Storefront implementation.


1. Import `CxTranslationsModule` in your `AppModule`. It must be loaded **after** the module that introduces i18n services (`CoreModule`, `SpartacusModule`, or `CommonModule`) but **before** any other module that uses translations:

```ts
import { CxTranslationsModule } from 'cxtranslations';

@NgModule({
  imports: [
    SpartacusModule,       // or CoreModule / CommonModule
    CxTranslationsModule,  // must come right after
    ...
  ]
})
export class AppModule {}
```

1. If the Assisted Service Module (ASM) is used, also import `CxTranslationsModuleASM`.

1. Disable frontend fallback language

 CX Translation relies on the **backend-side** language fallback mechanism. Enabling a frontend-side fallback may cause issues. Disable it by setting `fallbackLang` to `false` in the `i18n` configuration:

 ```ts
 i18n: {
   resources: translations,
   chunks: translationChunksConfig,
   fallbackLang: false
 }
 ```

1. (Optional) If you plan to use the storefront admin component and use the labels upload button. Add the `translationUploadButton` property to the SAP Composable Storefront environment files (see examples under `js-storefront/environments`). For safety, disable it in production:

```ts
translationUploadButton: false  // production
translationUploadButton: true   // dev / staging
```

> [!TIP] **Installation issue hint**
> If you encounter issues, check the order of module imports. `CxTranslationsService` must be registered *before* the translation service from any other module, otherwise the `translationService` injection token gets overwritten.

---

## Step 3 — System Setup (ImpEx & Scripts)

The following files need to be imported to set up the system data. All files are available in the [`downloads`](downloads/) directory of this documentation. Import them via the `patches` extension or the HAC console.

The files must be imported in the order listed below.

---

### 1. `usergroups.impex` — User Groups and Access Rights

**[Download usergroups.impex](downloads/usergroups.impex)**

Creates the backoffice user groups and roles required by CX Translations, sets catalog-version access rights, and grants sync job permissions.

**Required adjustments:**

| Macro | Description |
|---|---|
| `$contentCatalog` | ID of your content catalog (e.g. `electronicsContentCatalog`) |
| `$productCatalog` | ID of your product catalog |
| `$classificationCatalog` | ID of your classification catalog |
| `$contentCatalogSyncJob` | Code of the content catalog sync job (e.g. `electronicsContentCatalog-sync`) |

The file creates:
- `cxTranslationsBasicGroup` — base group with read access to all CX Translate item types
- `cxTranslationsConfiguratorGroup` — can manage Localized Object configurations
- `cxTranslationsTranslatorGroup` — can manage labels, values and translation jobs
- `cxTranslationsAiConfiguratorGroup` — can manage AI translation settings
- `cxTranslationsAdminGroup` — full access to all CX Translate items
- Sample demo users (`cxtranslationsconfigurator`, `cxtranslationstranslator`, `cxtranslationsaiconfigurator`, `cxtranslationsadmin`) with default password `*:1234` — **remove or change these for production**

---

### 2. `synchronisation.impex` — Catalog Sync Configuration

**[Download synchronisation.impex](downloads/synchronisation.impex)**

Adds `CxLocalizedBundle` and `CxLocalizedLabel` to the root types of the content catalog sync job, and excludes the `labels` attribute from deep copy during sync (so labels are references, not duplicated).

**Required adjustments:**

| Macro | Description |
|---|---|
| `$contentCatalogSyncJob` | Code of the content catalog sync job (same as in `usergroups.impex`) |

---

### 3. [OPTIONAL] `admin-component.impex` — CMS Admin Component

**[Download admin-component.impex](downloads/admin-component.impex)**

Creates the `CxTranslationsAdminComponent` CMS flex component and places it in the `SiteLogoSlot`. Also creates user-group-based CMS restrictions so only members of `frontendTranslatorGroup` and `cmstranslatorgroup` (ASM) can see the component.

**Required adjustments:**

| Macro | Description |
|---|---|
| `$contentCatalog` | ID of your content catalog |

The macro `$contentCV` is derived automatically. If you have a **multicountry setup**, use the **global content catalog** (the catalog without any parents).

---

### 4. [OPTIONAL]  `admin-component-labels.impex` — UI Labels for the Admin Component

**[Download admin-component-labels.impex](downloads/admin-component-labels.impex)**

Inserts the `CxLocalizedBundle` named `translations` and populates it with all UI label keys used by the CX Translations admin component (English and Polish translations are provided). Add further `value[lang=XX]` columns as needed for your project languages.

**Required adjustments:**

| Macro | Description |
|---|---|
| `$contentCatalog` | ID of your content catalog |

> [!NOTE] Only English (`en`) and Polish (`pl`) values are provided out of the box. Add columns for additional languages as required by your project.

---

### 5. `cx-translate-cronjob-retention.impex` — CronJob Cleanup

**[Download cx-translate-cronjob-retention.impex](downloads/cx-translate-cronjob-retention.impex)**

Creates retention rules and CronJobs that clean up completed `CxTranslationCronJob` and `CxApproveTranslationsCronJob` items older than 48 hours (172800 seconds), triggered daily at midnight.

No project-specific adjustments required. Optionally adjust `retentionTimeSeconds` or `cronExpression` to match your operational preferences.

---

### 6. `ai-settings.impex` — AI Translation Configuration *(optional)*

**[Download ai-settings.impex](downloads/ai-settings.impex)**

Configures the AI-powered translation integration: AI models, BTP AI destination, OAuth credentials, deployment mappings, and translation service settings.

> [!WARNING] **This file requires mandatory adjustments before import.** Look for all `<<CHANGE_ME>>` markers.

**Required adjustments:**

| Macro / Field | Description |
|---|---|
| `$oAuthClientId` | OAuth client ID for the BTP AI service |
| `$oAuthSecret` | OAuth client secret |
| `$oAuthUrl` | OAuth token URL |
| `$aiBaseUrl` | Base URL of the BTP AI service |
| `CxAiDeploymentConfig` deployments map | Replace every `change_me` value with the actual deployment ID for each AI model you plan to use. Remove entries for models you are not using. |

For full AI configuration instructions see [AI Configuration](aitranslations_configuration.md).

---

### 7. `autoconfigure.groovy` — Auto-configure Localized Values *(optional)*

**[Download autoconfigure.groovy](downloads/autoconfigure.groovy)**

Runs `CxLocalizedValueConfigService.autoConfigure(true)` to automatically generate the Localized Values configuration by scanning all item types and their localizable attributes.

Import via the **HAC Groovy console** (`/hac/console/scripting`) after all ImpEx files have been imported. This step is required only if you are using the **Localized Values** feature.

---


## Step 4 — Execute Content Catalog Synchronisation

After importing all ImpEx files, run a **content catalog synchronisation** to publish the new CMS component and labels from the Staged to the Online catalog version.

---

## Summary of System Setup Actions

| File | When to import | Notes |
|---|---|---|
| `usergroups.impex` | System setup / essential data | Adjust catalog macros and sync job code |
| `synchronisation.impex` | System setup / essential data | Adjust sync job code |
| `admin-component.impex` | System setup / essential data | Adjust content catalog macro |
| `admin-component-labels.impex` | System setup / essential data | Adjust content catalog macro; add language columns as needed |
| `cx-translate-cronjob-retention.impex` | System setup / essential data | No adjustments needed |
| `ai-settings.impex` | System setup — only if using AI translations | Replace all `<<CHANGE_ME>>` values |
| `autoconfigure.groovy` | HAC console — only if using Localized Values | Run after all impex imports |
| `admin-component-test-data.impex` | After accelerator test data — only if needed | Adjust content catalog macro |
