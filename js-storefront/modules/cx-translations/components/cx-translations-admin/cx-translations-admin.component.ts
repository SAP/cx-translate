/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewContainerRef,
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NgSelectComponent } from '@ng-select/ng-select';
import { GlobalMessageService, GlobalMessageType } from '@spartacus/core';
import { ICON_TYPE, LAUNCH_CALLER, LaunchDialogService } from '@spartacus/storefront';
import {
  BehaviorSubject,
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  Observable,
  of,
  Subject,
  Subscription,
  throwError,
} from 'rxjs';
import { catchError, map, switchMap, take } from 'rxjs/operators';
import { environment } from '../../../../../environments/environment';
import { CxTranslationService } from '../../service/cx-translation.service';

/**
 * Component displaying translations
 */
@Component({
  selector: 'cx-translations-admin',
  templateUrl: './cx-translations-admin.component.html',
  styleUrls: ['./cx-translations-admin.component.scss'],

  host: {
    '[class]': 'hostClasses',
    class: 'cx-translations-admin',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class CxTranslationsAdminComponent implements OnInit, OnDestroy {
  @ViewChild('publishButton') publishButton: ElementRef | undefined;
  @ViewChild('uploadButton') uploadButton: ElementRef | undefined;
  @ViewChild('keySuggestions') keySuggestionsSelect: NgSelectComponent | undefined;

  showKeys$: Observable<boolean>;
  previewCatalog$: Observable<boolean>;
  isAllowedToTranslate$: Observable<boolean>;
  combinedVisibility$: Observable<boolean>;

  toggleView$ = new BehaviorSubject<boolean>(false);
  visible$ = new BehaviorSubject<boolean>(false);

  hostClasses: string = '';
  subscriptions: Subscription = new Subscription();
  showUploadButton = (environment as any)['translationUploadButton'] ?? false;
  private readonly minSearchChars: number = (environment as any)['translationKeySuggestionMinCharacters'] ?? 3;
  form: FormGroup;
  iconTypes = ICON_TYPE;

  suggestions$: Observable<string[]> | undefined;
  private readonly searchTerms = new Subject<string>();

  toggleShowKeys() {
    this.cxTranslationsService.toggleShowKeys();
  }

  constructor(
    protected cxTranslationsService: CxTranslationService,
    protected globalMessageService: GlobalMessageService,
    protected fb: FormBuilder,
    protected launchDialogService: LaunchDialogService,
    protected vcr: ViewContainerRef,
    private readonly cdr: ChangeDetectorRef
  ) {
    this.showKeys$ = this.cxTranslationsService.isShowKeys().asObservable();
    this.previewCatalog$ = this.cxTranslationsService.isStagedCatalogVersion().asObservable();

    this.isAllowedToTranslate$ = this.cxTranslationsService.isAllowedToTranslate();

    this.form = this.fb.group({
      key: ['', Validators.required],
      value: ['', Validators.required],
    });

    this.combinedVisibility$ = combineLatest([this.toggleView$, this.isAllowedToTranslate$]).pipe(
      map(([toggleView, isAllowedToTranslate]: [boolean, boolean]) => toggleView && isAllowedToTranslate)
    );
  }

  ngOnInit() {
    this.suggestions$ = this.searchTerms.pipe(
      debounceTime(100), // Wait 100ms after each keystroke
      distinctUntilChanged(), // Only emit if the value changes
      switchMap((term: string) =>
        this.cxTranslationsService.labelKeySuggestions(term).pipe(
          catchError(() => {
            this.globalMessageService.add(
              {
                key: 'translations.key.suggestion.fail',
              },
              GlobalMessageType.MSG_TYPE_ERROR
            );
            return of([] as string[]);
          })
        )
      )
    );

    this.subscriptions.add(
      this.isAllowedToTranslate$.subscribe((isVisible: boolean) => {
        this.adjustHostClass('visible', isVisible);
        this.visible$.next(isVisible);
      })
    );
  }

  toggleStagedCatalogVersion() {
    this.cxTranslationsService.toggleStagedCatalogVersion();
  }

  toggleView() {
    this.toggleView$.next(!this.toggleView$.value);
  }

  saveLabel() {
    this.cxTranslationsService
      .editLabel(this.form.get('key')?.value, this.form.get('value')?.value)
      .pipe(
        catchError((error: any) => {
          this.globalMessageService.add(
            {
              key: 'translations.save.failed',
            },
            GlobalMessageType.MSG_TYPE_ERROR
          );
          return throwError(() => error);
        })
      )
      .subscribe(() => {
        this.globalMessageService.add(
          {
            key: 'translations.save.success',
          },
          GlobalMessageType.MSG_TYPE_CONFIRMATION
        );
      });
  }

  uploadTranslations() {
    const dialog = this.launchDialogService.openDialog(LAUNCH_CALLER.UPLOAD_TRANSLATIONS, this.uploadButton, this.vcr);

    dialog?.pipe(take(1)).subscribe();
  }

  publishTranslations() {
    const dialog = this.launchDialogService.openDialog(LAUNCH_CALLER.PUBLISH_TRANSLATIONS, this.publishButton, this.vcr);

    dialog?.pipe(take(1)).subscribe();
  }

  onAddNewKey(newKey: string) {
    return newKey;
  }

  onSearch(keyInput: string): void {
    // run search for min x characters - defined by environment property
    if (keyInput?.length >= this.minSearchChars) {
      this.searchTerms.next(keyInput); // Emit search term for API call
    } else {
      // otherwise clear the results
      this.keySuggestionsSelect?.handleClearClick();
    }
  }

  onKeySuggestion(newKey: string): void {
    this.form.get('key')?.setValue(newKey);
    this.fetchValueForKey(newKey);
  }

  onClear() {
    this.searchTerms.next('');
    this.form.reset();
  }

  private fetchValueForKey(key: string): void {
    this.cxTranslationsService
      .getLabelValueForKey(key)
      .pipe(
        take(1), // Automatically completes after the first emission
        catchError(() => {
          this.globalMessageService.add(
            {
              key: 'translations.valueforkey.fail',
            },
            GlobalMessageType.MSG_TYPE_ERROR
          );
          return of(null); // Fallback value if the call fails
        })
      )
      .subscribe((value: string | null) => {
        if (value) {
          this.form.get('value')?.setValue(value);
        } else {
          this.form.get('value')?.reset();
        }
      });
  }

  private adjustHostClass(cssClass: string, add: boolean) {
    const classes = new Set(this.hostClasses.split(/\s+/).filter(Boolean));
    if (add) {
      classes.add(cssClass);
    } else {
      classes.delete(cssClass);
    }
    const next = Array.from(classes).join(' ');
    if (next !== this.hostClasses) {
      this.hostClasses = next;
      this.cdr.markForCheck();
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }
}
