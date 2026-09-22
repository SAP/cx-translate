/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import { Component } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { IExperienceService } from 'smarteditcommons';
import { CxCmsTranslationsService } from '../../services/CxCmsTranslationsService';
import {
    CxCmsLocalizedValue,
    CxCmsServiceSettingsEntry,
    CxCmsTranslateRequest,
    CxCmsTranslationNode
} from '../../services/CxCmsTranslationTree';

/**
 * Identifies the CMS item whose translation tree should be loaded into the popup.
 *
 * `catalogVersionUuid` uses SmartEdit's `catalogId/version` encoding (e.g. `apparel-ukContentCatalog/Staged`);
 * when it is missing or malformed the popup falls back to the current experience's catalog descriptor.
 */
export interface CxCmsTranslationsTarget {
    /** uid of the CMS item (e.g. the selected component or the current page). */
    uid: string;
    /** Optional catalog-version UUID (`catalogId/version`) of the item. */
    catalogVersionUuid?: string;
}

/** View model for a single localized value (one translatable attribute). */
interface ValueVM {
    /** Unique id (the localized value code) - used internally for selection only. */
    id: string;
    /** Attribute qualifier (displayed). */
    attribute: string;
    /** Map of language iso code -> raw (possibly HTML) value. */
    values: { [languageIso: string]: string };
    /** Whether the value is selected for translation. */
    selected: boolean;
    /**
     * AI-proposed translation (to be reviewed), matched by value code.
     * Map of language iso code -> raw (possibly HTML) value.
     */
    translatedValues?: { [languageIso: string]: string };
    /** Status of the AI translation proposal (e.g. `SUCCESS`). */
    translatedStatus?: string;
    /** PK of the AI translation proposal item (used for approval / rejection). */
    translatedPk?: string;
    /** Whether an AI translation proposal exists for this value. */
    hasTranslation: boolean;
}

/** View model for a node of the collapsible translation tree. */
interface TreeNodeVM {
    id: string;
    uid: string;
    itemType: string;
    values: ValueVM[];
    children: TreeNodeVM[];
    expanded: boolean;
}

/** Values shorter than this (and without markup/newlines) are rendered inline. */
const INLINE_VALUE_MAX_LENGTH = 80;

/**
 * Self-contained CMS translations popup: loads the recursive translation tree for a given
 * {@link CxCmsTranslationsTarget}, lets the user review / start AI translations and approve or
 * reject proposals.
 *
 * The component is target-agnostic: it is opened via {@link open} with an explicit uid + catalog,
 * so it can be reused both from the component decorator (for the selected component) and from the
 * perspective toolbar (for the current page).
 */
@Component({
    selector: 'cx-cms-translations-popup',
    templateUrl: './CxCmsTranslationsPopupComponent.html',
    standalone: false,
    styleUrls: ['./CxCmsTranslationsPopupComponent.scss']
})
export class CxCmsTranslationsPopupComponent {
    /** Whether the translations popup is visible. */
    public showTranslationsPopup = false;
    /** Whether the translation tree is currently being loaded. */
    public loadingTranslations = false;
    /** Error message to display if loading the translation tree failed. */
    public translationsError: string | null = null;

    /** Root of the collapsible tree view model. */
    public treeRoot: TreeNodeVM | null = null;
    /** Flat list of all values for selection / counting. */
    public allValues: ValueVM[] = [];
    /** Available language iso codes discovered in the loaded data. */
    public availableLanguages: string[] = [];
    /** Language iso codes present in the AI translation proposals (second column). */
    public availableTranslatedLanguages: string[] = [];
    /** Selected source language (single). */
    public sourceLanguage: string | null = null;
    /** Language whose values are displayed in the tree (independent of source). */
    public displayLanguage: string | null = null;
    /** Language whose AI-proposed values are shown in the second column (independent of displayLanguage). */
    public translatedDisplayLanguage: string | null = null;
    /** Selected target languages (multiple). */
    public targetLanguages = new Set<string>();

    /** Whether the target-language multiselect dropdown is open. */
    public targetDropdownOpen = false;
    /** Search term for filtering target languages. */
    public targetSearch = '';

    /** Whether a translation request is in flight. */
    public translating = false;

    /** Whether an approval request is in flight. */
    public approving = false;
    /** Whether a rejection request is in flight. */
    public rejecting = false;

    /**
     * Unified status message for the persistent status bar (translate / approve / reject
     * outcomes). Rendered outside any proposal-conditional block so it stays visible even after
     * the tree is reloaded and proposals disappear.
     */
    public statusMessage: string | null = null;
    /** Visual kind of {@link statusMessage} (drives the status-bar color). */
    public statusKind: 'info' | 'success' | 'error' = 'info';

    /** Whether the "Translation settings" modal dialog is visible. */
    public showTranslationSettingsModal = false;

    /** Whether a manual refresh is currently in flight. */
    public refreshing = false;

    /** Currently previewed long value (shown in a secondary popup). */
    public viewedValue: { attribute: string; language: string; html: SafeHtml } | null = null;

    // -----------------------------------------------------------------
    // Language display names (populated from tree response metadata)
    // -----------------------------------------------------------------

    /** Maps iso code -> session-localized display name for all configured languages. */
    public languageNames: Map<string, string> = new Map();

    // -----------------------------------------------------------------
    // Translation settings extras
    // -----------------------------------------------------------------

    /** Available translation service settings objects (populated from tree response metadata). */
    public availableServiceSettings: CxCmsServiceSettingsEntry[] = [];
    /** Code of the selected service settings object; null means "use default". */
    public selectedServiceSettingsCode: string | null = null;
    /** Whether to skip already-translated values. */
    public skipTranslated = false;
    /** Whether translations should be saved for approval (default: true). */
    public saveForApproval = true;

    // -----------------------------------------------------------------
    // Display template switch (CMS pages only)
    // -----------------------------------------------------------------

    /** Whether to include template content slots in the tree (for page items). */
    public includeTemplate = false;

    /** The CMS item currently loaded in the popup. */
    private target: CxCmsTranslationsTarget | null = null;

    private readonly safeHtmlCache = new Map<string, SafeHtml>();

    constructor(
        private cxCmsTranslationsService: CxCmsTranslationsService,
        private experienceService: IExperienceService,
        private sanitizer: DomSanitizer
    ) {}

    /**
     * Opens the popup and loads the CMS translation tree for the given target.
     */
    public async open(target: CxCmsTranslationsTarget): Promise<void> {
        this.target = target;

        this.showTranslationsPopup = true;
        this.loadingTranslations = true;
        this.translationsError = null;
        this.treeRoot = null;
        this.allValues = [];
        this.availableLanguages = [];
        this.availableTranslatedLanguages = [];
        this.targetLanguages = new Set<string>();
        this.sourceLanguage = null;
        this.displayLanguage = null;
        this.translatedDisplayLanguage = null;
        this.targetDropdownOpen = false;
        this.targetSearch = '';
        this.translating = false;
        this.approving = false;
        this.rejecting = false;
        this.statusMessage = null;
        this.statusKind = 'info';
        this.showTranslationSettingsModal = false;
        this.viewedValue = null;
        this.safeHtmlCache.clear();
        this.languageNames = new Map();
        this.availableServiceSettings = [];
        this.selectedServiceSettingsCode = null;
        this.skipTranslated = false;
        this.saveForApproval = true;
        this.includeTemplate = false;

        try {
            await this.fetchAndBuildTree(false);
        } catch (e) {
            this.translationsError = 'Failed to load translations for this component.';
        } finally {
            this.loadingTranslations = false;
        }
    }

    /**
     * Fetches the CMS translation tree for the current target and rebuilds the view model.
     *
     * @param preserveSelections when {@code true}, the previously selected display / source / target /
     *        proposal languages are restored (used when reloading after approve / reject).
     */
    private async fetchAndBuildTree(preserveSelections: boolean): Promise<void> {
        if (!this.target) {
            return;
        }
        const experience = await this.experienceService.getCurrentExperience();
        const baseSiteId = experience?.catalogDescriptor?.siteId;
        const { catalogId, catalogVersion } = this.resolveCatalog(experience);

        if (!baseSiteId || !catalogId || !catalogVersion) {
            throw new Error('Cannot resolve site/catalog from current experience.');
        }

        const prevDisplay = this.displayLanguage;
        const prevSource = this.sourceLanguage;
        const prevTranslatedDisplay = this.translatedDisplayLanguage;
        const prevTargets = new Set(this.targetLanguages);

        this.allValues = [];
        this.safeHtmlCache.clear();

        const response = await this.cxCmsTranslationsService.getCmsTranslationTree(
            baseSiteId,
            this.target.uid,
            catalogId,
            catalogVersion,
            this.includeTemplate
        );

        // Populate language name map from metadata
        this.languageNames = new Map(
            (response.metadata?.languages ?? []).map((l) => [l.isocode, l.name])
        );

        // Populate available service settings from metadata
        if (!preserveSelections) {
            this.availableServiceSettings = response.metadata?.serviceSettings ?? [];
            this.selectedServiceSettingsCode = null;
        } else {
            // Refresh service settings list but keep the current selection if still valid
            const updatedSettings = response.metadata?.serviceSettings ?? [];
            this.availableServiceSettings = updatedSettings;
            if (
                this.selectedServiceSettingsCode !== null &&
                !updatedSettings.some((s) => s.code === this.selectedServiceSettingsCode)
            ) {
                this.selectedServiceSettingsCode = null;
            }
        }

        this.treeRoot = this.buildNodeVM(response.tree, '0');
        this.initLanguages(
            (experience?.languageDescriptor as { isocode?: string } | undefined)?.isocode
        );

        if (preserveSelections) {
            if (prevDisplay && this.availableLanguages.includes(prevDisplay)) {
                this.displayLanguage = prevDisplay;
            }
            if (prevSource && this.availableLanguages.includes(prevSource)) {
                this.sourceLanguage = prevSource;
            }
            if (
                prevTranslatedDisplay &&
                this.availableTranslatedLanguages.includes(prevTranslatedDisplay)
            ) {
                this.translatedDisplayLanguage = prevTranslatedDisplay;
            }
            const restoredTargets = new Set<string>();
            prevTargets.forEach((iso) => {
                if (this.availableLanguages.includes(iso) && iso !== this.sourceLanguage) {
                    restoredTargets.add(iso);
                }
            });
            this.targetLanguages = restoredTargets;
        }
    }

    /** Closes the translations popup. */
    public closeTranslations(event?: Event): void {
        if (event) {
            event.stopPropagation();
            event.preventDefault();
        }
        this.showTranslationsPopup = false;
        this.viewedValue = null;
        this.targetDropdownOpen = false;
    }

    /**
     * Handles clicks inside the main popup: stops them from closing the popup and collapses the
     * target-language dropdown when the click happened outside of it.
     */
    public onPopupClick(event: Event): void {
        event.stopPropagation();
        this.targetDropdownOpen = false;
    }

    // -----------------------------------------------------------------
    // Tree interaction
    // -----------------------------------------------------------------

    public toggleNode(node: TreeNodeVM): void {
        node.expanded = !node.expanded;
    }

    public hasContent(node: TreeNodeVM): boolean {
        return (
            (node.values && node.values.length > 0) || (node.children && node.children.length > 0)
        );
    }

    /**
     * A node is visible only when it has values of its own or at least one visible descendant.
     * Structural nodes without any (visible) values are hidden.
     */
    public isNodeVisible(node: TreeNodeVM): boolean {
        if (node.values && node.values.length > 0) {
            return true;
        }
        return (node.children || []).some((child) => this.isNodeVisible(child));
    }

    /** Child nodes that should be rendered (see {@link isNodeVisible}). */
    public visibleChildren(node: TreeNodeVM): TreeNodeVM[] {
        return (node.children || []).filter((child) => this.isNodeVisible(child));
    }

    /** Returns true when the item type looks like a CMS Page type. */
    public isPageItemType(itemType: string): boolean {
        return !!itemType && itemType.endsWith('Page');
    }

    // -----------------------------------------------------------------
    // Selection
    // -----------------------------------------------------------------

    public get selectedCount(): number {
        return this.allValues.filter((v) => v.selected).length;
    }

    public get allSelected(): boolean {
        return this.allValues.length > 0 && this.allValues.every((v) => v.selected);
    }

    public toggleSelectAll(): void {
        const select = !this.allSelected;
        this.allValues.forEach((v) => (v.selected = select));
    }

    /**
     * Collects all ValueVM instances within the given node and its descendants.
     */
    private collectSubtreeValues(node: TreeNodeVM): ValueVM[] {
        const result: ValueVM[] = [...node.values];
        node.children.forEach((child) => result.push(...this.collectSubtreeValues(child)));
        return result;
    }

    /**
     * Toggles selection of all values in the given node's subtree.
     * If all values in the subtree are already selected, deselects them; otherwise selects all.
     */
    public toggleSelectSubtree(node: TreeNodeVM, event?: Event): void {
        if (event) {
            event.stopPropagation();
        }
        const subtreeValues = this.collectSubtreeValues(node);
        if (subtreeValues.length === 0) {
            return;
        }
        const allSubtreeSelected = subtreeValues.every((v) => v.selected);
        subtreeValues.forEach((v) => (v.selected = !allSubtreeSelected));
    }

    /** Whether all values in the given node's subtree are currently selected. */
    public isSubtreeSelected(node: TreeNodeVM): boolean {
        const subtreeValues = this.collectSubtreeValues(node);
        return subtreeValues.length > 0 && subtreeValues.every((v) => v.selected);
    }

    /** Whether some (but not all) values in the given node's subtree are selected. */
    public isSubtreeIndeterminate(node: TreeNodeVM): boolean {
        const subtreeValues = this.collectSubtreeValues(node);
        const selectedCount = subtreeValues.filter((v) => v.selected).length;
        return selectedCount > 0 && selectedCount < subtreeValues.length;
    }

    // -----------------------------------------------------------------
    // Translation settings modal
    // -----------------------------------------------------------------

    /** Opens the "Translation settings" modal dialog. */
    public openTranslationSettings(event?: Event): void {
        if (event) {
            event.stopPropagation();
            event.preventDefault();
        }
        this.showTranslationSettingsModal = true;
    }

    /** Closes the "Translation settings" modal dialog. */
    public closeTranslationSettings(event?: Event): void {
        if (event) {
            event.stopPropagation();
            event.preventDefault();
        }
        this.showTranslationSettingsModal = false;
    }

    // -----------------------------------------------------------------
    // Languages
    // -----------------------------------------------------------------

    /**
     * Returns a display label for a language iso code.
     * Format: "Name (isocode)" when a name is available, otherwise just the iso code.
     */
    public getLanguageLabel(iso: string): string {
        const name = this.languageNames.get(iso);
        return name ? `${name} (${iso})` : iso;
    }

    public onSourceLanguageChange(iso: string): void {
        this.sourceLanguage = iso;
        // A language cannot be both source and target.
        this.targetLanguages.delete(iso);
    }

    public isTargetSelected(iso: string): boolean {
        return this.targetLanguages.has(iso);
    }

    public toggleTargetLanguage(iso: string): void {
        if (this.targetLanguages.has(iso)) {
            this.targetLanguages.delete(iso);
        } else {
            this.targetLanguages.add(iso);
        }
    }

    /** Languages that can be selected as targets (all available except the source). */
    public get selectableTargetLanguages(): string[] {
        return this.availableLanguages.filter((iso) => iso !== this.sourceLanguage);
    }

    /** Target languages filtered by the current search term (matches iso code and display name). */
    public get filteredTargetLanguages(): string[] {
        const term = this.targetSearch.trim().toLowerCase();
        const selectable = this.selectableTargetLanguages;
        if (!term) {
            return selectable;
        }
        return selectable.filter(
            (iso) =>
                iso.toLowerCase().includes(term) ||
                (this.languageNames.get(iso) ?? '').toLowerCase().includes(term)
        );
    }

    /** Currently selected target languages as an ordered array (for chips). */
    public get targetLanguagesArray(): string[] {
        return this.availableLanguages.filter((iso) => this.targetLanguages.has(iso));
    }

    public toggleTargetDropdown(event: Event): void {
        event.stopPropagation();
        this.targetDropdownOpen = !this.targetDropdownOpen;
        if (this.targetDropdownOpen) {
            this.targetSearch = '';
        }
    }

    public removeTarget(iso: string, event: Event): void {
        event.stopPropagation();
        this.targetLanguages.delete(iso);
    }

    public selectAllTargets(event: Event): void {
        event.stopPropagation();
        this.filteredTargetLanguages.forEach((iso) => this.targetLanguages.add(iso));
    }

    public clearTargets(event: Event): void {
        event.stopPropagation();
        this.targetLanguages.clear();
    }

    // -----------------------------------------------------------------
    // Value rendering (smart display + HTML)
    // -----------------------------------------------------------------

    public getDisplayValue(value: ValueVM): string {
        if (!this.displayLanguage) {
            return '';
        }
        return value.values?.[this.displayLanguage] ?? '';
    }

    /** True when at least one value carries an AI translation proposal (controls the extra column). */
    public get hasAnyTranslation(): boolean {
        return this.allValues.some((v) => v.hasTranslation);
    }

    /** AI-proposed translated value for the current proposal display language. */
    public getTranslatedDisplayValue(value: ValueVM): string {
        if (!value.translatedValues || !this.translatedDisplayLanguage) {
            return '';
        }
        return value.translatedValues[this.translatedDisplayLanguage] ?? '';
    }

    /** Whether the AI translation proposal for this value completed successfully. */
    public isTranslationSuccess(value: ValueVM): boolean {
        return (value.translatedStatus || '').toUpperCase() === 'SUCCESS';
    }

    /** Placeholder rendered instead of the value when the translation status is not `SUCCESS`. */
    public translatedStatusLabel(value: ValueVM): string {
        return `<<${value.translatedStatus || ''}>>`;
    }

    public isInline(raw: string): boolean {
        if (!raw) {
            return true;
        }
        return raw.length <= INLINE_VALUE_MAX_LENGTH && !/[\n<>]/.test(raw);
    }

    public preview(raw: string): string {
        if (!raw) {
            return '';
        }
        const text = raw
            .replace(/<[^>]*>/g, ' ')
            .replace(/\s+/g, ' ')
            .trim();
        return text.length > INLINE_VALUE_MAX_LENGTH
            ? text.substring(0, INLINE_VALUE_MAX_LENGTH) + '…'
            : text;
    }

    /** Returns SafeHtml (Angular sanitization intentionally bypassed for CMS content). */
    public trust(raw: string): SafeHtml {
        const key = raw ?? '';
        let safe = this.safeHtmlCache.get(key);
        if (!safe) {
            safe = this.sanitizer.bypassSecurityTrustHtml(key);
            this.safeHtmlCache.set(key, safe);
        }
        return safe;
    }

    public openValueView(value: ValueVM, event?: Event): void {
        if (event) {
            event.stopPropagation();
            event.preventDefault();
        }
        const raw = this.getDisplayValue(value);
        this.viewedValue = {
            attribute: value.attribute,
            language: this.displayLanguage ?? '',
            html: this.trust(raw)
        };
    }

    /** Opens the value viewer for the AI-proposed translation of the given value. */
    public openTranslatedView(value: ValueVM, event?: Event): void {
        if (event) {
            event.stopPropagation();
            event.preventDefault();
        }
        const raw = this.getTranslatedDisplayValue(value);
        this.viewedValue = {
            attribute: `${value.attribute} (proposal)`,
            language: this.translatedDisplayLanguage ?? '',
            html: this.trust(raw)
        };
    }

    public closeValueView(event?: Event): void {
        if (event) {
            event.stopPropagation();
            event.preventDefault();
        }
        this.viewedValue = null;
    }

    // -----------------------------------------------------------------
    // Translate
    // -----------------------------------------------------------------

    public get canTranslate(): boolean {
        return (
            !this.translating &&
            this.selectedCount > 0 &&
            !!this.sourceLanguage &&
            this.targetLanguages.size > 0
        );
    }

    /** Starts an AI translation cron job (in the background) for the selected values. */
    public async translate(): Promise<void> {
        if (!this.canTranslate) {
            return;
        }
        this.translating = true;
        this.statusMessage = null;

        try {
            const experience = await this.experienceService.getCurrentExperience();
            const baseSiteId = experience?.catalogDescriptor?.siteId;
            if (!baseSiteId) {
                throw new Error('Cannot resolve site from current experience.');
            }

            const request: CxCmsTranslateRequest = {
                valueKeys: this.allValues.filter((v) => v.selected).map((v) => v.id),
                sourceLanguage: this.sourceLanguage as string,
                targetLanguages: this.targetLanguagesArray,
                serviceSettingsCode: this.selectedServiceSettingsCode ?? undefined,
                skipTranslated: this.skipTranslated,
                saveForApproval: this.saveForApproval
            };

            const job = await this.cxCmsTranslationsService.startTranslation(baseSiteId, request);
            this.statusMessage = `Translation started - job ${job.cronJobCode} (${job.valueCount} values).`;
            this.statusKind = 'success';

            // Translation was successfully triggered: close only the settings modal, keep the
            // main translations popup open so the user can keep reviewing / refreshing.
            this.showTranslationSettingsModal = false;

            // Reload the tree so freshly-created PENDING proposals become visible immediately.
            await this.refresh();
        } catch (e) {
            this.statusMessage = 'Failed to start translation.';
            this.statusKind = 'error';
        } finally {
            this.translating = false;
        }
    }

    // -----------------------------------------------------------------
    // Refresh (manual reload while proposals are still PENDING)
    // -----------------------------------------------------------------

    /** Whether at least one value carries a proposal still in `PENDING` status. */
    public get hasPendingTranslation(): boolean {
        return this.allValues.some((v) => (v.translatedStatus || '').toUpperCase() === 'PENDING');
    }

    /** Reloads the translation tree on demand (used to poll for PENDING proposals). */
    public async refresh(): Promise<void> {
        if (this.refreshing) {
            return;
        }
        this.refreshing = true;
        try {
            await this.fetchAndBuildTree(true);
        } catch (e) {
            this.translationsError = 'Failed to refresh translations for this component.';
        } finally {
            this.refreshing = false;
        }
    }

    // -----------------------------------------------------------------
    // Approve / Reject proposals
    // -----------------------------------------------------------------

    /** Selected values that carry an approvable / rejectable AI translation proposal. */
    public get selectedTranslatedValues(): ValueVM[] {
        return this.allValues.filter((v) => v.selected && v.hasTranslation && !!v.translatedPk);
    }

    public get selectedTranslatedCount(): number {
        return this.selectedTranslatedValues.length;
    }

    /**
     * Selected proposals that may be approved: only those whose translation completed with a
     * `SUCCESS` status (PENDING / FAILED proposals cannot be approved, only rejected).
     */
    public get selectedApprovableValues(): ValueVM[] {
        return this.selectedTranslatedValues.filter((v) => this.isTranslationSuccess(v));
    }

    public get selectedApprovableCount(): number {
        return this.selectedApprovableValues.length;
    }

    /** Whether the approve action is currently available (needs at least one SUCCESS proposal). */
    public get canApprove(): boolean {
        return (
            !this.approving &&
            !this.rejecting &&
            !this.translating &&
            this.selectedApprovableCount > 0
        );
    }

    /** Whether the reject action is currently available (any selected proposal can be rejected). */
    public get canReject(): boolean {
        return (
            !this.approving &&
            !this.rejecting &&
            !this.translating &&
            this.selectedTranslatedCount > 0
        );
    }

    /** Approves the selected proposals (calls the synchronous backend service). */
    public approveSelected(): Promise<void> {
        return this.processReview('approve');
    }

    /** Rejects (removes) the selected proposals. */
    public rejectSelected(): Promise<void> {
        return this.processReview('reject');
    }

    private async processReview(action: 'approve' | 'reject'): Promise<void> {
        // Approve only operates on SUCCESS proposals; reject can act on any selected proposal.
        const values =
            action === 'approve' ? this.selectedApprovableValues : this.selectedTranslatedValues;
        const pks = values.map((v) => v.translatedPk as string);
        if (pks.length === 0 || this.approving || this.rejecting) {
            return;
        }

        if (action === 'approve') {
            this.approving = true;
        } else {
            this.rejecting = true;
        }
        this.statusMessage = null;

        try {
            const experience = await this.experienceService.getCurrentExperience();
            const baseSiteId = experience?.catalogDescriptor?.siteId;
            if (!baseSiteId) {
                throw new Error('Cannot resolve site from current experience.');
            }

            const result =
                action === 'approve'
                    ? await this.cxCmsTranslationsService.approveTranslations(baseSiteId, pks)
                    : await this.cxCmsTranslationsService.rejectTranslations(baseSiteId, pks);

            const verb = action === 'approve' ? 'Approved' : 'Rejected';
            this.statusMessage =
                `${verb} ${result.succeeded}/${result.requested} proposal(s)` +
                (result.failed > 0 ? `, ${result.failed} failed.` : '.');
            this.statusKind = result.failed > 0 ? 'error' : 'success';

            // Reload the tree so applied / removed proposals are reflected in the UI.
            await this.fetchAndBuildTree(true);
        } catch (e) {
            this.statusMessage =
                action === 'approve'
                    ? 'Failed to approve selected proposals.'
                    : 'Failed to reject selected proposals.';
            this.statusKind = 'error';
        } finally {
            this.approving = false;
            this.rejecting = false;
        }
    }

    // -----------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------

    private buildNodeVM(node: CxCmsTranslationNode, path: string): TreeNodeVM {
        // Index AI-proposed translations by value code so they can be matched to localized values.
        const translatedByCode = new Map<string, CxCmsLocalizedValue>();
        (node.translatedValues || []).forEach((tv) => {
            if (tv && tv.code) {
                translatedByCode.set(tv.code, tv);
            }
        });

        const values: ValueVM[] = (node.localizedValues || []).map((lv, i) => {
            const code = lv.code || `${path}.${i}`;
            const translated = lv.code ? translatedByCode.get(lv.code) : undefined;
            const vm: ValueVM = {
                id: code,
                attribute: lv.attribute,
                values: this.normalizeValues(lv.values),
                selected: false,
                translatedValues: translated
                    ? this.normalizeValues(translated.values)
                    : undefined,
                translatedStatus: translated ? (translated.status ?? '') : undefined,
                translatedPk: translated ? translated.pk : undefined,
                hasTranslation: !!translated
            };
            this.allValues.push(vm);
            return vm;
        });

        const children = (node.children || []).map((child, i) =>
            this.buildNodeVM(child, `${path}.${i}`)
        );

        return {
            id: path,
            uid: node.uid,
            itemType: node.itemType,
            values,
            children,
            expanded: true
        };
    }

    /**
     * Normalizes the localized values into a plain `{ iso: value }` map.
     *
     * SAP Commerce webservices serialize a `Map` as `{ "entry": [ { "key": "en", "value": "..." } ] }`
     * (and XML/JSON variants), so both that shape and a plain map object are supported here.
     */
    private normalizeValues(raw: unknown): { [languageIso: string]: string } {
        const result: { [languageIso: string]: string } = {};
        if (!raw || typeof raw !== 'object') {
            return result;
        }

        const asRecord = raw as Record<string, unknown>;
        const entries = this.extractEntries(asRecord);
        if (entries) {
            entries.forEach((entry) => {
                if (entry && typeof entry === 'object') {
                    const e = entry as { key?: unknown; value?: unknown };
                    if (e.key != null) {
                        result[String(e.key)] = e.value == null ? '' : String(e.value);
                    }
                }
            });
            return result;
        }

        // Plain map object: { en: '...', de: '...' }
        Object.keys(asRecord).forEach((key) => {
            const val = asRecord[key];
            if (val == null || typeof val !== 'object') {
                result[key] = val == null ? '' : String(val);
            }
        });
        return result;
    }

    /** Returns the array of `{ key, value }` entries if the object uses the webservices map format. */
    private extractEntries(raw: Record<string, unknown>): unknown[] | null {
        if (Array.isArray((raw as { entry?: unknown }).entry)) {
            return (raw as { entry: unknown[] }).entry;
        }
        if (Array.isArray(raw)) {
            return raw as unknown[];
        }
        return null;
    }

    private initLanguages(experienceLanguageIso?: string): void {
        // Languages that actually carry a value in the loaded data (used to pick sensible defaults).
        const dataLanguages = new Set<string>();
        this.allValues.forEach((v) =>
            Object.keys(v.values).forEach((iso) => dataLanguages.add(iso))
        );

        // Available languages come from the site's configured languages (tree metadata) so that
        // languages not yet translated on any item can still be chosen as translation targets.
        // Fall back to the languages discovered in the data when metadata is unavailable.
        this.availableLanguages = (
            this.languageNames.size > 0
                ? Array.from(this.languageNames.keys())
                : Array.from(dataLanguages)
        ).sort();

        if (this.availableLanguages.length > 0) {
            // Prefer the experience language, then a language that actually has content (so the tree
            // shows meaningful values by default), finally any configured language.
            const firstDataLanguage = Array.from(dataLanguages).sort()[0];
            this.sourceLanguage =
                experienceLanguageIso && this.availableLanguages.includes(experienceLanguageIso)
                    ? experienceLanguageIso
                    : (firstDataLanguage ?? this.availableLanguages[0]);
            // Display defaults to the source language but can be changed independently.
            this.displayLanguage = this.sourceLanguage;
        }

        // Languages available for the AI-proposal (second) column: only languages for which at least
        // one proposal actually carries a non-empty value (e.g. before translation completes, proposals
        // may exist with a non-SUCCESS status and no values in any language yet).
        const translatedLanguages = new Set<string>();
        this.allValues.forEach((v) => {
            if (v.translatedValues) {
                Object.keys(v.translatedValues).forEach((iso) => {
                    if (v.translatedValues?.[iso]) {
                        translatedLanguages.add(iso);
                    }
                });
            }
        });
        this.availableTranslatedLanguages = Array.from(translatedLanguages).sort();

        if (this.availableTranslatedLanguages.length > 0) {
            // Prefer a proposal language different from the display language (proposals are usually
            // for the target languages), falling back to the first available one.
            this.translatedDisplayLanguage =
                this.availableTranslatedLanguages.find((iso) => iso !== this.displayLanguage) ??
                this.availableTranslatedLanguages[0];
        }
    }

    private resolveCatalog(experience: {
        catalogDescriptor?: { catalogId?: string; catalogVersion?: string };
    }): { catalogId: string | undefined; catalogVersion: string | undefined } {
        const uuid = this.target?.catalogVersionUuid;
        if (uuid && uuid.lastIndexOf('/') > -1) {
            const idx = uuid.lastIndexOf('/');
            return {
                catalogId: uuid.substring(0, idx),
                catalogVersion: uuid.substring(idx + 1)
            };
        }
        const descriptor = experience?.catalogDescriptor;
        return {
            catalogId: descriptor?.catalogId,
            catalogVersion: descriptor?.catalogVersion
        };
    }
}
