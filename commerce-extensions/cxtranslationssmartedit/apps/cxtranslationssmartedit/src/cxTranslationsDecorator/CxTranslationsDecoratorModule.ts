/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'smarteditcommons';
import { CxCmsTranslationsPopupModule } from 'cxtranslationssmarteditcommons';
import { CxTranslationsDecoratorComponent } from './CxTranslationsDecoratorComponent';
import { CxCmsTranslationsPopupRegistry } from './CxCmsTranslationsPopupRegistry';

@NgModule({
    imports: [CommonModule, FormsModule, TooltipModule, CxCmsTranslationsPopupModule],
    declarations: [CxTranslationsDecoratorComponent],
    providers: [CxCmsTranslationsPopupRegistry],
    exports: [CxTranslationsDecoratorComponent]
})
export class CxTranslationsDecoratorModule {}
