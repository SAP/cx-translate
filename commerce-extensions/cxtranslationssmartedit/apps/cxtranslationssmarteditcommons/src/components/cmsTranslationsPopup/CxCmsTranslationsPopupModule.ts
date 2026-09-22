/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SeTranslationModule } from 'smarteditcommons';
import { CxCmsTranslationsPopupComponent } from './CxCmsTranslationsPopupComponent';

@NgModule({
    imports: [CommonModule, FormsModule, SeTranslationModule.forChild()],
    declarations: [CxCmsTranslationsPopupComponent],
    exports: [CxCmsTranslationsPopupComponent]
})
export class CxCmsTranslationsPopupModule {}
