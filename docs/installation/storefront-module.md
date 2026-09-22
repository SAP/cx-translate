# Storefront Module Installation

## Prerequisites

- SAP Composable Storefront (2211 or compatible)
- Commerce extensions already installed — see [Commerce Extensions Installation](commerce-extensions.md)

---

## Steps

1. Copy `js-storefront/cxtranslations` into the `src` directory of your SAP Composable Storefront implementation.

1. Import `CxTranslationsModule` in your `AppModule`. It must be loaded **after** the module that introduces i18n services (`CoreModule`, `SpartacusModule`, or `CommonModule`) but **before** any other module that uses translations:

   ```ts
   import { CxTranslationsModule } from 'cxtranslations';

   @NgModule({
     imports: [
       SpartacusModule,       // or CoreModule / CommonModule
       CxTranslationsModule,  // must come right after
       // ...
     ]
   })
   export class AppModule {}
   ```

   > [!TIP] If you encounter issues, check the import order. `CxTranslationsService` must be registered *before* the translation service from any other module; otherwise the `translationService` injection token gets overwritten.

1. If the Assisted Service Module (ASM) is used, also import `CxTranslationsModuleASM`:

   ```ts
   import { CxTranslationsModuleASM } from 'cxtranslations';
   ```

1. Disable the frontend fallback language. CX Translations relies on the **backend-side** language fallback mechanism. Enabling a frontend fallback may cause conflicts. Set `fallbackLang` to `false` in the `i18n` configuration:

   ```ts
   i18n: {
     resources: translations,
     chunks: translationChunksConfig,
     fallbackLang: false
   }
   ```

1. *(Optional)* If you plan to use the [Frontend Admin Component](frontend-admin-component.md), add the `translationUploadButton` flag to your environment files. See the [Frontend Admin Component](frontend-admin-component.md#step-3--enable-the-upload-button-in-the-storefront) guide for details.
