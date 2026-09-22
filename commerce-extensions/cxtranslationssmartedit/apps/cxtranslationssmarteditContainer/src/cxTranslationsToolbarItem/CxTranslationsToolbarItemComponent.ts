/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import { Component, Inject } from '@angular/core';
import { ToolbarItemInternal, TOOLBAR_ITEM } from 'smarteditcommons';

@Component({
    selector: 'cx-translations-toolbar-item',
    templateUrl: './CxTranslationsToolbarItemComponent.html',
    standalone: false
})
export class CxTranslationsToolbarItemComponent {
    constructor(@Inject(TOOLBAR_ITEM) public toolbarItem: ToolbarItemInternal) {}
}
