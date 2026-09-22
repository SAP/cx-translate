# Localized Labels Configuration

## Default Catalog
When resolving **CxLocalizedLabels** CX Translate selects Catalog Versions in the following way:
- Content catalog from `catalogVersionService.getSessionCatalogVersions()`
- Active version of Catalog bound to current CMS Site
- Active version of Catalog configured by property:

```properties
cxtranslations.default.catalog.id=
```


## Block Import / Upload

Allow/block translations to be imported from SAP Composable Storefront and overwrite the already existing **CxLocalizedLabel**. It is useful to run the initial import of labels from static files to the database and store them as **CxLocalizedLabel**. Later on any label changes should be introduced to the **CxLocalizedLabel** stored in DB directly instead of changing the SAP Composable Storefront translations located in static files.
Running the import / upload again would overwrite values of the already modified **CxLocalizedLabel**. It should be therefore disabled by setting the `cxtranslations.import.allowed` set to `false` after the initial load to prevent an accidental overwrite.

### Backend
Configured by a property in the `cxtranslations` extension

```properties
cxtranslations.import.allowed=true
```
### Frontend
Configured by the environment property `translationUploadButton`

```ts
translationUploadButton: true
```

### Include Empty Values for Labels Without Translations Provided

For optimization reasons, labels with no translation (also in the fallback language) are omitted from the labels bundle served via OCC. SAP Composable Storefront in dev mode displays the translation key in case there is no key found in the bundle. The following property enforces the inclusion of empty values in the payload and it is especially useful in local environments.

```properties
cxtranslations.include.empty.values=false
```

### Cache

The properties below apply to the `Online` catalog version. Caching for `Staged` version is explicitly turned off.

```properties
cxtranslations.cache.enabled=true
cxtranslations.cache.cachePolicy=LRU
cxtranslations.cache.cacheSize=10000
cxtranslations.cache.timeToLiveSeconds=3600
cxtranslations.cache.region=localizedLabelCacheRegion
```

> Setting `timeToLiveSeconds` on DEV and STG environment to lower values may be useful for testing purposes.

### Disable Resource Files

By default, all translations are loaded via OCC from the CX Translations backend, but resource files are used as a fallback. 

1. Switch off using resource files as fallback

`src/cxtranslations/translations/service/cx-translation.service.ts`

Change property from `true` to `false`:

```typescript
private useResourcesAsFallback = false;
```

2. Disable loading resources at all

`src/cxtranslations/translations/configs/cx-translations-i18n-config.provider.ts`

Uncomment the following line:

```typescript
resources: undefined, // fixes issue with trying to fetch unexisting resources
```

## Development Guide

### Adding New Labels

When using patches mechanism, the following ImpEx header can be used as a template:

```impex
INSERT_UPDATE CxLocalizedLabel; bundle(code, $contentCV); key[unique = true]; value[lang = en]; value[lang = de]; forFrontend[default = true]; forBackend; $contentCV[unique = true]; status(code)[default = check]
```

or using an Excel file with patches mechanism:

```java
importExcelData("translations.xlsx");
```

### Using Labels on the Backend

#### Existing translation code
CX Translate contains `CxMessageSourceAspect` which intercepts translation requests to implementation of `org.springframework.context.MessageSource`.
It is enabled by property (true by default)
```
cxtranslations.backendTranslations.messageSourceAspect.enabled=true
```

It tries to load label from `CxDatabaseMessageSource` and use it to translate message. If `CxLocalizedLabel` does not exist for requested key (or it's disabled) the original `MessageSource` is used.

#### New translation code
When implementing new translation code with intention of using Cx Translate the most convenient way is to use `CxLocalizedMessagesService` wrapper which uses only `CxDatabaseMessageSource` and does not fallback to other sources.

#### Details
There are 4 services/classes, which can be in different contexts and abstraction layers:

1. `CxTranslationsService` - the lowest abstraction layer service, serves models directly.
2. `CxTranslationsFacade` - the main entry point for CX Translations internals. Allows for retrieving both frontend and backend labels with or without caching.
3. `CxDatabaseMessageSource` - the `messageSource` implementation that fetches backend Messages using `CxTranslationsFacade`. It does not fall back to any other source.
4. `CxLocalizedMessagesService` - a wrapper around `CxDatabaseMessageSource` that provides a more convenient API for fetching backend messages.

### Using Labels on the Frontend

No changes required comparing to standard SAP Composable Storefront `cxTranslate` pipe usage.

### Clearing Cache

Call `CxTranslationsService.clearLabelsCache` to clear cache so that changes will be visible in frontend without delay.
