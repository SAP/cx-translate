/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import {
    CrossFrameEventService,
    IDecoratorService,
    IFeatureService,
    IPerspectiveService,
    moduleUtils,
    SeTranslationModule,
    SeEntryModule
} from 'smarteditcommons';
import {
    CxCmsTranslationsModule,
    CX_OPEN_CMS_TRANSLATIONS_EVENT,
    CxOpenCmsTranslationsEvent
} from 'cxtranslationssmarteditcommons';
import { CxTranslationsDecoratorModule } from './cxTranslationsDecorator';
import { CxCmsTranslationsPopupRegistry } from './cxTranslationsDecorator/CxCmsTranslationsPopupRegistry';

@SeEntryModule('cxtranslationssmartedit')
@NgModule({
    imports: [
        BrowserModule,
        SeTranslationModule.forChild(),
        CxCmsTranslationsModule,
        CxTranslationsDecoratorModule
    ],
    providers: [
        moduleUtils.initialize(
            (
                decoratorService: IDecoratorService,
                featureService: IFeatureService,
                perspectiveService: IPerspectiveService
            ) => {
                decoratorService.addMappings({
                    '.*': ['cxTranslationsDecorator']
                });

                featureService.addDecorator({
                    key: 'cxTranslationsDecorator',
                    nameI18nKey: 'cxtranslationssmartedit.decorator.name'
                });

                perspectiveService.register({
                    key: 'cxTranslationsPerspective',
                    nameI18nKey: 'cxtranslationssmartedit.perspective.name',
                    descriptionI18nKey: 'cxtranslationssmartedit.perspective.description',
                    features: ['cxTranslationsToolbarItem', 'cxTranslationsDecorator'],
                    perspectives: []
                });
            },
            [IDecoratorService, IFeatureService, IPerspectiveService]
        ),
        moduleUtils.initialize(
            (
                crossFrameEventService: CrossFrameEventService,
                popupRegistry: CxCmsTranslationsPopupRegistry
            ) => {
                crossFrameEventService.subscribe(
                    CX_OPEN_CMS_TRANSLATIONS_EVENT,
                    (_eventId: string, target: unknown) => {
                        popupRegistry.getActiveHost()?.open(target as CxOpenCmsTranslationsEvent);
                    }
                );
            },
            [CrossFrameEventService, CxCmsTranslationsPopupRegistry]
        )
    ]
})
export class CxtranslationssmarteditModule {}
