/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import { AfterViewInit, Component, OnDestroy, ViewChild } from '@angular/core';
import { AbstractDecorator, SeCustomComponent } from 'smarteditcommons';
import { CxCmsTranslationsPopupComponent } from 'cxtranslationssmarteditcommons';
import { CxCmsTranslationsPopupRegistry } from './CxCmsTranslationsPopupRegistry';

@SeCustomComponent('cx-translations-decorator')
@Component({
    selector: 'cx-translations-decorator',
    templateUrl: './CxTranslationsDecoratorComponent.html',
    standalone: false,
    styleUrls: ['./CxTranslationsDecoratorComponent.scss']
})
export class CxTranslationsDecoratorComponent
    extends AbstractDecorator
    implements AfterViewInit, OnDestroy
{
    /** Shared CMS translations popup hosted by the decorator. */
    @ViewChild(CxCmsTranslationsPopupComponent)
    public translationsPopup!: CxCmsTranslationsPopupComponent;

    constructor(private popupRegistry: CxCmsTranslationsPopupRegistry) {
        super();
    }

    ngAfterViewInit(): void {
        this.popupRegistry.register(this.translationsPopup);
    }

    ngOnDestroy(): void {
        this.popupRegistry.deregister(this.translationsPopup);
        super.ngOnDestroy();
    }

    /** Opens the shared translations popup for the currently selected component. */
    public openTranslations(event: Event): void {
        event.stopPropagation();
        event.preventDefault();

        this.translationsPopup.open({
            uid: this.smarteditComponentId,
            catalogVersionUuid: this.smarteditCatalogVersionUuid
        });
    }
}
