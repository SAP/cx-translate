/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import {HTTP_INTERCEPTORS} from "@angular/common/http";
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgSelectModule } from '@ng-select/ng-select';
import { Config, I18nModule, provideConfig, provideDefaultConfig, TranslationService } from '@spartacus/core';
import { FormErrorsModule, IconModule, KeyboardFocusModule, SpinnerModule } from '@spartacus/storefront';
import { CxTranslationsAdminComponent } from './components/cx-translations-admin/cx-translations-admin.component';
import { CxTranslationsDialogModalLayoutConfig } from './components/cx-translations-admin/cx-translations-modals-layout.config';
import { CxTranslationsPublishingConfirmationDialogComponent } from './components/cx-translations-admin/cx-translations-publishing-confirmation-dialog/cx-translations-publishing-confirmation-dialog.component';
import { CxTranslationsUploadConfirmationDialogComponent } from './components/cx-translations-admin/cx-translations-upload-confirmation-dialog/cx-translations-upload-confirmation-dialog.component';
import { cxTranslationsCmsConfig } from './configs/cx-translations.cms-config';
import { cxTranslationsOccConfig } from './configs/cx-translations.occ-config';
import './model';
import { CxTranslationService } from './service/cx-translation.service';
import {CxTranslationsAuthHttpInterceptor} from "./interceptors/cx-translations-auth-http-interceptor";

@NgModule({
  declarations: [
    CxTranslationsAdminComponent,
    CxTranslationsUploadConfirmationDialogComponent,
    CxTranslationsPublishingConfirmationDialogComponent,
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    FormErrorsModule,
    IconModule,
    I18nModule,
    KeyboardFocusModule,
    SpinnerModule,
    NgSelectModule,
  ],
  providers: [
    CxTranslationService,
    {
      provide: TranslationService,
      useExisting: CxTranslationService,
    },
    {
      provide: HTTP_INTERCEPTORS,
      useExisting: CxTranslationsAuthHttpInterceptor,
      multi: true,
    },
    provideConfig(<Config>{
      ...cxTranslationsOccConfig,
      ...cxTranslationsCmsConfig,
    }),
    provideDefaultConfig(CxTranslationsDialogModalLayoutConfig),
  ],
})
export class CxTranslationsModule {}
