# Adding CX Translate

This guide walks through adding CX Translate to a new or existing SAP Commerce Cloud project.

---

## Step 1 — Install the Commerce Extensions

Copy the three SAP Commerce extensions into your implementation and register them in `localextensions.xml`.

[Commerce Extensions Installation](installation/commerce-extensions.md)

---

## Step 2 — Install the Storefront Module

Add the Angular module to your SAP Composable Storefront application and configure i18n.

[Storefront Module Installation](installation/storefront-module.md)

---

## Step 3 — Import System Data

Import the required ImpEx files and run the initial Groovy script. This step covers user groups, catalog sync, the optional admin CMS component, and cron-job cleanup rules.

[System Setup — ImpEx & Scripts](installation/system-setup.md)

---

## Step 4 — Basic Configuration

After the system data is in place, configure the catalog default, import settings, and Localized Values autoconfiguration.

[Basic Configuration](installation/basic-configuration.md)

---

## Step 5 — Configure Automated Translations *(optional)*

Choose and configure a translation provider. Each provider has its own setup guide with ImpEx snippets.

| Provider | Description |
|---|---|
| [SAP AI Core on BTP](installation/provider-sap-ai-core.md) | LLM-based translations via SAP AI Core — GPT, Claude, Gemini and others. Default provider. |
| [SAP Translation Hub](installation/provider-sap-translation-hub.md) | SAP Translation Hub (LLM or statistical model). |
| [DeepL](installation/provider-deepl.md) | DeepL neural translation API. |
| [Azure Translator](installation/provider-azure.md) | Azure AI Translator document translation API. |

---

## Optional Features

### Frontend Admin Component

CX Translate contains an optional CMS component for the storefront. When installed it is visible to specific user groups or Assisted Service Module (ASM) agents and provides in-browser label management — view, edit, and publish labels without opening Backoffice.

[Frontend Admin Component Setup](installation/frontend-admin-component.md)

### Storefront Label Export

To initially populate the database from the existing Composable Storefront translation files, follow the [Export Storefront Labels to ImpEx](localisedLabels/localizedLabels_storefront-labels_export.md) guide.
