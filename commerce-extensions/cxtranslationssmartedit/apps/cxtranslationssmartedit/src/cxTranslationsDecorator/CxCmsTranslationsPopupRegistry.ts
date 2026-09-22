/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import { Injectable } from '@angular/core';
import { CxCmsTranslationsPopupComponent } from 'cxtranslationssmarteditcommons';

/**
 * Inner-frame registry of live {@link CxCmsTranslationsPopupComponent} hosts.
 *
 * The component decorator is mapped to every component, so decorator instances (each hosting a
 * translations popup) exist across the page. Each host registers itself here on init and
 * deregisters on destroy, allowing the cross-frame "open translations" handler to open the popup
 * for the current page without depending on a specific selected component.
 */
@Injectable()
export class CxCmsTranslationsPopupRegistry {
    private readonly hosts = new Set<CxCmsTranslationsPopupComponent>();

    public register(host: CxCmsTranslationsPopupComponent): void {
        this.hosts.add(host);
    }

    public deregister(host: CxCmsTranslationsPopupComponent): void {
        this.hosts.delete(host);
    }

    /** Returns any live popup host, or {@code null} when none is currently mounted. */
    public getActiveHost(): CxCmsTranslationsPopupComponent | null {
        const iterator = this.hosts.values().next();
        return iterator.done ? null : iterator.value;
    }
}
