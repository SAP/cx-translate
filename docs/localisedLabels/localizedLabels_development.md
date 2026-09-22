# Localized Labels Development Guide

## Adding New Labels

When using the patches mechanism, the following ImpEx header can be used as a template:

```impex
INSERT_UPDATE CxLocalizedLabel; bundle(code, $contentCV); key[unique = true]; value[lang = en]; value[lang = de]; forFrontend[default = true]; forBackend; $contentCV[unique = true]; status(code)[default = check]
```

or using an Excel file with the patches mechanism:

```java
importExcelData("translations.xlsx");
```

## Using Labels on the Backend

 
> [!TIP] In 99% of cases for backend-based translations `LocalizedMessagesService` should be used.


There are 4 services/classes, which can be used in different contexts and abstraction layers:

1. `CxTranslationsService` - the lowest abstraction layer service, serves models directly.
2. `CxTranslationsFacade` - the main entry point for CX Translations internals. Requires Content Catalog Version to be present in the session, allows for retrieving both frontend and backend labels with or without caching.
3. `DatabaseMessageSource` - the `messageSource` implementation with an additional aspect around all other message sources in the system, which tries to fetch a message from the database first.
4. `LocalizedMessagesService` - the highest abstraction level service, serves labels only with `forBackend` set to `true`.

### Using Labels on the Frontend

No changes required compared to standard SAP Composable Storefront `cxTranslate` pipe usage.

### Clearing Cache

Call `CxTranslationsService.clearLabelsCache` to clear cache so that changes will be visible in frontend without delay.
