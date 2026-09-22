# Basic Configuration

After completing the [System Setup](system-setup.md), perform the following configuration steps.

---

## Localized Labels

### Default Catalog

When resolving `CxLocalizedLabels`, CX Translations selects catalog versions in the following order:

1. Content catalog from `catalogVersionService.getSessionCatalogVersions()`
2. Active version of the catalog bound to the current CMS site
3. Active version of the catalog configured by the property:

```properties
cxtranslations.default.catalog.id=
```

### Label Import / Upload Control

Allow or block importing translations from SAP Composable Storefront to prevent accidental overwrite of already-modified labels. Run the initial import once (with the property set to `true`), then disable it:

```properties
# Backend (cxtranslations extension)
cxtranslations.import.allowed=true
```

The corresponding frontend upload button is controlled separately — see [Frontend Admin Component](frontend-admin-component.md#step-3--enable-the-upload-button-in-the-storefront).

> [!WARNING] Set `cxtranslations.import.allowed=false` in production after the initial label load to prevent accidental overwrites.

### Cache

These properties apply to the `Online` catalog version. Caching for `Staged` is disabled by default.

```properties
cxtranslations.cache.enabled=true
cxtranslations.cache.cachePolicy=LRU
cxtranslations.cache.cacheSize=10000
cxtranslations.cache.timeToLiveSeconds=3600
cxtranslations.cache.region=localizedLabelCacheRegion
```

> Reduce `timeToLiveSeconds` on dev / staging for faster testing feedback.

---

## Localized Values

### Autoconfiguration

The Localized Values autoconfiguration runs automatically during system update (project data phase) when `cxtranslations.autoconfig.enabled=true` (the default). It scans all item types with localizable string attributes and creates `CxLocalizedObjectConfig` entries.

Key properties:

```properties
# Enable/disable the initial autoconfiguration run during system update
cxtranslations.autoconfig.enabled=true

# Only create config if at least one non-null value exists for the attribute
cxtranslations.autoconfig.checkdata.enabled=true

# Comma-separated list of catalog name patterns to exclude
cxtranslations.autoconfig.excluded.catalogPatterns=_boconfig

# Comma-separated list of item types to exclude
cxtranslations.autoconfig.excluded.types=Theme,SavedQuery,RendererTemplate,WorkflowTemplate,AbstractWorkflowAction,WorkflowActionTemplate,AutomatedWorkflowActionTemplate,AbstractConstraint

# Comma-separated type.property pairs to exclude
cxtranslations.autoconfig.excluded.properties=BaseSite.locale,

# Behaviour when autoconfiguration re-runs: KEEP_EXISTING or REPLACE
cxtranslations.autoconfig.merge.mode=REPLACE
```

To run autoconfiguration manually after installation, use the `autoconfigure.groovy` script from the HAC Groovy console — see [System Setup](system-setup.md#6-optional-autoconfiguratiogroovy--auto-configure-localized-values).

For a full reference including manual cron-job execution, see [Localized Values — Autoconfiguration](../localizedValues.md#autoconfiguration).

---

## Proposed Translation Cleanup

The following properties control whether `CxProposedTranslation` items are automatically removed in specific scenarios after an AI translation run:

```properties
# Remove proposed translation if 'Skip translated' was selected and there was nothing to translate
cxtranslations.proposedtranslation.autoremove.missing.targetlanguages=false

# Remove proposed translation if the source language value was missing
cxtranslations.proposedtranslation.autoremove.missing.sourcevalue=false
```
