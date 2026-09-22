/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import {
    CrossFrameEventService,
    IFeatureService,
    moduleUtils,
    SeEntryModule,
    SeTranslationModule,
    IPageInfoService
} from 'smarteditcommons';
import { CX_OPEN_CMS_TRANSLATIONS_EVENT } from 'cxtranslationssmarteditcommons';
import { CxTranslationsToolbarItemComponent } from './cxTranslationsToolbarItem';

@SeEntryModule('cxtranslationssmarteditContainer')
@NgModule({
    imports: [BrowserModule, SeTranslationModule.forChild()],
    declarations: [CxTranslationsToolbarItemComponent],
    providers: [
        moduleUtils.initialize(
            (
                featureService: IFeatureService,
                pageInfoService: IPageInfoService,
                crossFrameEventService: CrossFrameEventService
            ) => {
                featureService.addToolbarItem({
                    toolbarId: 'smartEditPerspectiveToolbar',
                    key: 'cxTranslationsToolbarItem',
                    type: 'HYBRID_ACTION',
                    nameI18nKey: 'cxtranslationssmartedit.toolbar.name',
                    descriptionI18nKey: 'cxtranslationssmartedit.toolbar.description',
                    priority: 2,
                    section: 'left',
                    iconClassName: 'icon-message-information se-toolbar-menu-ddlb--button__icon',
                    callback: async () => {
                        const pageUID = await pageInfoService.getPageUID();
                        const catalogVersionUUID =
                            await pageInfoService.getCatalogVersionUUIDFromPage();

                        crossFrameEventService.publish(CX_OPEN_CMS_TRANSLATIONS_EVENT, {
                            uid: pageUID,
                            catalogVersionUuid: catalogVersionUUID
                        });
                    }
                });
            },
            [IFeatureService, IPageInfoService, CrossFrameEventService]
        )
    ]
})
export class CxtranslationssmarteditContainerModule {}
