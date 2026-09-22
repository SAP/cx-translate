/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

import {HttpClient} from '@angular/common/http';
import {Inject, Injectable, InjectionToken, isDevMode, Optional} from '@angular/core';
import {
  BaseSiteService,
  I18nConfig,
  I18NEXT_INSTANCE,
  I18nextTranslationService,
  Occ,
  OccEndpointsService,
  TranslationChunkService,
  UserIdService,
} from '@spartacus/core';
import {i18n, ResourceLanguage, TOptions} from 'i18next';
import {BehaviorSubject, combineLatest, EMPTY, from, iif, merge, Observable, of, Subscriber} from 'rxjs';
import {catchError, finalize, map, mergeMap, shareReplay, startWith, switchMap, tap, toArray} from 'rxjs/operators';
import BaseSite = Occ.BaseSite;

enum TranslationSource {
  cxTranslate,
  resources,
  fallbackFromMissingKey,
  initialWhitespace,
}

interface TranslatedValue {
  value: string,
  translationSource: TranslationSource,
}

//Injection token that allows to provide addictional observables that emmit if visibility of the Admin UI component
//should be refreshed. Created initially for ASM companion module (cx-translations-asm) to know to do the refresh after an agent logs in or out.
export const USER_REFRESH_SOURCES = new InjectionToken<Observable<unknown>[]>(
  'USER_REFRESH_SOURCES'
);

/**
 * A customized implementation of I18nextTranslationService.
 * It provides additional functionality including:
 *  -loading chunks from the backend's database
 *  -returning labels keys instead of values
 *  -returning labels from staged catalog version
 *  -editing label values / adding new labels
 *  -uploading currently loaded labels to backend (supposed to be used as a step in a migration away from file-based translations).
 *
 *  Some implementation notes:
 *    -names of chunks loaded from backend are prefixed with additional information about current site and catalog version (whether it's staged or not)
 *      this is done in order to use/load the correct version of the label
 *      for example 'common' chunk can become `powertools-spa/translations/chunks/common` for online version
 *        and `powertools-spa/translations/staged/chunks/common` for staged
 *
 *    -see comment under translate() method for overview of translation process
 */
@Injectable()
export class CxTranslationService extends I18nextTranslationService {
  public static readonly ONLINE_TRANSLATIONS_PATH = '/translations/chunks/';
  public static readonly STAGED_TRANSLATIONS_PATH = '/translations/staged/chunks/';
  public static readonly TRANSLATIONS_CHUNK = 'translations';

  private readonly CX_NON_BREAKING_SPACE = String.fromCharCode(160);

  private showKeys$ = new BehaviorSubject(false);
  private stagedCatalogVersion$ = new BehaviorSubject(false);

  private useResourcesAsFallback = true;

  private chunkResponseCache = new Map<string, any>();
  private chunkRequestsInProgress = new Map<string, Observable<any>>();

  constructor(
    protected http: HttpClient,
    protected occEndpointsService: OccEndpointsService,
    protected baseSiteService: BaseSiteService,
    protected userIdService: UserIdService,
    protected override config: I18nConfig,
    protected override translationChunk: TranslationChunkService,
    @Inject(I18NEXT_INSTANCE) protected override i18next: i18n,
    @Optional() @Inject(USER_REFRESH_SOURCES)
    private extraRefreshSources: Observable<unknown>[] | null
  ) {
    super(config, translationChunk, i18next);
  }

  /**
   * Translates a value.
   *  High level overview of the flow of the translation
   *  -generate internal cx translate key/chunk name which is site/catalog version specific
   *    -for example common.menu can become powertools-spa/translations/chunks/common:common.menu or powertools-spa/translations/staged/chunks/common:common.menu
   *    -similarly for chunk name common -> powertools-spa/translations/chunks/common or powertools-spa/translations/staged/chunks/common
   *  -check if there is already loaded value for cx translate key
   *    -if yes - return value
   *    -if no - try to load the relevant chunk from the database
   *      -check again if the value is loaded for cx translate key
   *        -if yes - return value
   *        -if no - fallback to translation using default resource-based chunks
   *
   *  In case of passing key array, all are being translated and the first one (ordered by the place in the array) for which system resolves the translation is being returned.
   *
   * @param key key to translate
   * @param options translation options
   * @param whitespaceUntilLoaded should whitespace be returned until value is ready.
   */
  override translate(key: string | string[], options?: any, whitespaceUntilLoaded?: boolean): Observable<string> {
    if (typeof key === "string") {
      return this.translateSingle(key, options, whitespaceUntilLoaded).pipe(
        map(result => result.value)
      );
    } else {
      if (key.length === 0) {
        return EMPTY;
      }
      //we handle `whitespaceUntilLoaded` explicitly in this method, so false is passed when translating single values
      const observableArray = key.map(singleKey => this.translateSingle(singleKey, options, false));
      const combinedObservable = combineLatest(observableArray).pipe(
        map(resultArray => this.findFirstResolvedTranslation(resultArray))
      );
      if (whitespaceUntilLoaded) {
        return combinedObservable.pipe(
          startWith(this.CX_NON_BREAKING_SPACE)
        )
      }
      return combinedObservable;
    }

  }

  translateSingle(key: string, options?: any, whitespaceUntilLoaded?: boolean): Observable<TranslatedValue> {
    const originalChunkName = this.translationChunk.getChunkNameForKey(key);
    const originalNamespacedKey = this.getCxNamespacedKey(key, originalChunkName);
    return this.showKeys$.pipe(
      switchMap((showKeys) =>
        iif(
          () => showKeys && !originalChunkName.includes(CxTranslationService.TRANSLATIONS_CHUNK, 0),
          new BehaviorSubject({value: key, translationSource: TranslationSource.cxTranslate}), // show key
          combineLatest([this.baseSiteService.getActive(), this.stagedCatalogVersion$]).pipe(
            switchMap(([site, isPreviewCatalog]) => {
              const cxTranslationsChunkName = this.getCxTranslationsChunkName(site, originalChunkName, isPreviewCatalog ? 'staged' : '');
              const cxTranslationsNamespacedKey = this.getCxNamespacedKey(key, cxTranslationsChunkName);
              return new Observable<TranslatedValue>((subscriber) => {
                // show value
                const translate = () => {
                  if (!this.i18next.isInitialized) {
                    return;
                  }
                  if (this.i18next.exists(cxTranslationsNamespacedKey, options as TOptions)) {
                    subscriber.next({
                      value: this.i18next.t(cxTranslationsNamespacedKey, options as any) as string,
                      translationSource: TranslationSource.cxTranslate,
                    });
                  } else {
                    if (whitespaceUntilLoaded) {
                      subscriber.next({
                        value: this.CX_NON_BREAKING_SPACE,
                        translationSource: TranslationSource.initialWhitespace
                      });
                    }
                    const url = this.occEndpointsService.buildUrl(isPreviewCatalog ? 'getStagedChunk' : 'getChunk', {
                      urlParams: {lang: this.i18next.language, chunkName: originalChunkName},
                    });
                    this.getWithCache(url)
                      .pipe(
                        tap((data) => {
                          if (!this.i18next.exists(cxTranslationsNamespacedKey, options as TOptions)) {
                            this.i18next.addResourceBundle(this.i18next.language, cxTranslationsChunkName, data);
                          }
                        })
                      )
                      .subscribe(() => {
                        if (!this.i18next.exists(cxTranslationsNamespacedKey, options as TOptions)) {
                          // fallback to original chunk name
                          if (this.useResourcesAsFallback) {
                            this.translateFromResources(key, originalChunkName, originalNamespacedKey, subscriber, options as TOptions);
                          } else {
                            // skip original chunk names and fallback to fallback value
                            this.handleMissingKey(key, originalChunkName, originalNamespacedKey, subscriber);
                          }
                        } else {
                          subscriber.next({
                          value: this.i18next.t(cxTranslationsNamespacedKey, options as any) as string,
                             translationSource: TranslationSource.cxTranslate
                          });
                        }
                      });
                  }
                };

                translate();
                this.i18next.on('languageChanged', translate);
                this.i18next.on('loaded', translate);
                return () => {
                  this.i18next.off('languageChanged', translate);
                  this.i18next.off('loaded', translate);
                };
              });
            })
          )
        )
      )
    );
  }

  /**
   * Example:
   *
   * <pre>
   *   const keys = {
   *       addToCart: {
   *         addToCart: {
   *           t1: "CustomTranslation1",
   *           buttons: {
   *             button1: "button1",
   *             button2: "button2"
   *           }
   *         },
   *       },
   *       pdp: {
   *         pdp: {
   *           save: "save",
   *           cancel: "cancel"
   *         },
   *         searchBox: {
   *           placeholder: "Placeholder"
   *         }
   *       }
   *     }
   * </pre>
   */
  uploadTranslations(): Observable<void> {
    return this.baseSiteService.getAll().pipe(
      switchMap((sites) => {
        const keys = this.i18next.services.resourceStore.data[this.i18next.language];
        const filteredKeys: ResourceLanguage = {};

        if (keys) {
          //we only want to upload file-based translations so chunks that we loaded from backend's DB are skipped by name convention
          Object.keys(keys)
            .filter((key) => !this.isCxTranslationsChunkName(sites, key))
            .forEach((key) => (filteredKeys[key] = keys[key]));
        }

        const url = this.occEndpointsService.buildUrl('uploadTranslations', {
          urlParams: {lang: this.i18next.language},
        });

        return this.http.post<void>(url, {...filteredKeys});
      })
    );
  }

  editLabel(key: string, value: string): Observable<void> {
    return this.baseSiteService.getActive().pipe(
      switchMap((site) => {
        const url = this.occEndpointsService.buildUrl('editLabel');
        const originalChunkName = this.translationChunk.getChunkNameForKey(key);

        return this.http
          .post<void>(url, {
            key,
            value,
            bundle: originalChunkName,
          })
          .pipe(
            // After the edit was saved, refresh the (staged) chunk in i18next so the new
            // value is visible immediately, without waiting for a page reload or a
            // staged-version toggle. We can't rely on `i18next.reloadResources` here,
            // because these chunks were never loaded through i18next's backend plugin -
            // they were fetched directly via HttpClient from the custom OCC endpoint and
            // injected via `addResourceBundle`. So we have to refetch ourselves, replace
            // the stale bundle and notify subscribers.
            switchMap(() => this.refreshStagedChunk(site, originalChunkName))
          );
      })
    );
  }

  /**
   * Re-fetches the given staged chunk from the OCC endpoint, replaces the cached
   * resource bundle in i18next and fires a `loaded` event so that active
   * `translateSingle(...)` subscriptions re-evaluate and emit the new value.
   */
  private refreshStagedChunk(site: string, originalChunkName: string): Observable<void> {
    const cxTranslationsChunkName = this.getCxTranslationsChunkName(site, originalChunkName, 'staged');
    const chunkUrl = this.occEndpointsService.buildUrl('getStagedChunk', {
      urlParams: {lang: this.i18next.language, chunkName: originalChunkName},
    });

    // Drop any HTTP cache entry for this chunk so the request actually hits the backend.
    this.clearChunkCache();

    return this.http.get(chunkUrl).pipe(
      tap((data) => {
        // Remove the stale bundle first - `addResourceBundle` without overwrite=true
        // would otherwise be a no-op when the namespace already exists.
        this.i18next.removeResourceBundle(this.i18next.language, cxTranslationsChunkName);
        this.i18next.addResourceBundle(this.i18next.language, cxTranslationsChunkName, data);
        // `translateSingle` subscribes to the i18next `loaded` event to re-translate.
        // i18next's EventEmitter exposes `emit`, but it isn't on the public typings.
        (this.i18next as unknown as { emit: (event: string, ...args: any[]) => void })
          .emit('loaded');
      }),
      map(() => undefined as void),
      catchError(() => of(undefined as void))
    );
  }

  labelKeySuggestions(term: string): Observable<string[]> {
    if (term?.length > 0) {
      return this.baseSiteService.getActive().pipe(
        switchMap((site) => {
          const url = this.occEndpointsService.buildUrl('labelKeySuggestions', {
            urlParams: {key: term},
          });

          return this.http.get<string[]>(url);
        })
      );
    }
    return of<string[]>([]);
  }

  getLabelValueForKey(key: string): Observable<string | null> {
    return this.baseSiteService.getActive().pipe(
      switchMap((site) => {
        const url = this.occEndpointsService.buildUrl('labelValue', {
          urlParams: {key: key},
        });
        return this.http.get<string | null>(url, {responseType: 'text' as 'json'});
      })
    );
  }

  toggleShowKeys() {
    this.showKeys$.next(!this.showKeys$.value);
  }

  isShowKeys() {
    return this.showKeys$;
  }

  toggleStagedCatalogVersion() {
    this.stagedCatalogVersion$.next(!this.stagedCatalogVersion$.value);
  }

  isStagedCatalogVersion() {
    return this.stagedCatalogVersion$;
  }

  /**
   * Translate label with standard OOTB logic
   * @param key of the label
   * @param chunkName of the label
   * @param namespacedKey namespace of the key
   * @param subscriber subscriber
   * @param options passed to i18next
   */
  private translateFromResources(key: string, chunkName: string, namespacedKey: string, subscriber: Subscriber<TranslatedValue>, options: TOptions) {
    if (this.i18next.exists(namespacedKey, options)) {
      subscriber.next({value: this.i18next.t(namespacedKey, options as any) as string, translationSource: TranslationSource.resources});
    } else {
      this.i18next.loadNamespaces(chunkName, () => {
        if (!this.i18next.exists(namespacedKey, options)) {
          this.handleMissingKey(key, chunkName, namespacedKey, subscriber);
        } else {
          subscriber.next({
            value: this.i18next.t(namespacedKey, options as any) as string,
            translationSource: TranslationSource.resources
          });
        }
      });
    }
  }

  /**
   * Handle missing key
   * @param key of the label not found
   * @param chunkName of the label
   * @param namespacedKey namespace of the key
   * @param subscriber subscriber
   * @private
   */
  private handleMissingKey(key: string, chunkName: string, namespacedKey: string, subscriber: Subscriber<TranslatedValue>) {
    this.cxReportMissingKey(key, chunkName);
    subscriber.next({
      value: this.getFallbackValue(namespacedKey),
      translationSource: TranslationSource.fallbackFromMissingKey
    });
  }

  /**
   * Copy of {@link I18nextTranslationService.getNamespacedKey} method
   */
  private getCxNamespacedKey(key: string, chunk: string): string {
    return chunk + this.NAMESPACE_SEPARATOR + key;
  }

  /**
   * Copy of {@link I18nextTranslationService.reportMissingKey} method
   */
  private cxReportMissingKey(key: string, chunkName: string) {
    if (isDevMode()) {
      // eslint-disable-next-line no-console
      console.warn(`Translation key missing '${key}' in the chunk '${chunkName}'`);
    }
  }

  public getChunkTemplate = () => `{{site}}${CxTranslationService.ONLINE_TRANSLATIONS_PATH}{{chunk}}`;
  public getStagedChunkTemplate = () => `{{site}}${CxTranslationService.STAGED_TRANSLATIONS_PATH}{{chunk}}`;

  public isCxTranslationsChunkName(sites: BaseSite[], chunkName: string) {
    return (
      sites.filter(
        (site) =>
          chunkName.startsWith(this.getCxTranslationsChunkName(site.uid!!, '')) ||
          chunkName.startsWith(this.getCxTranslationsChunkName(site.uid!!, '', 'staged'))
      ).length > 0
    );
  }

  // not used for now
  public getCxTranslationsChunkNames(chunkName: string): Observable<string[]> {
    return this.baseSiteService.getAll().pipe(
      mergeMap((sites) => from(sites).pipe(map((site) => this.getCxTranslationsChunkName(site.uid!!, chunkName)))),
      toArray()
    );
  }

  private getCxTranslationsChunkName(site: string, chunkName: string, version: string = '') {
    if (version.toLowerCase() === 'staged') {
      return this.getStagedChunkTemplate().replace('{{site}}', site).replace('{{chunk}}', chunkName);
    } else {
      return this.getChunkTemplate().replace('{{site}}', site).replace('{{chunk}}', chunkName);
    }
  }

  publishTranslations(): Observable<void> {
    return this.baseSiteService.getActive().pipe(
      switchMap((site) => {
        const url = this.occEndpointsService.buildUrl('publishTranslations');
        return this.http.post<void>(url,null).pipe(
          tap(() => {
            this.clearChunkCache();
            this.i18next.reloadResources();
          })
        );
      })
    );
  }

  isAllowedToTranslate(): Observable<boolean> {
    const userRefresh$ = this.getUserRefreshObservable();
    return userRefresh$.pipe(
      switchMap(() => {
        const url = this.occEndpointsService.buildUrl('isAllowedToTranslate');
        return this.http.get<boolean>(url).pipe(
          catchError(() => of(false))
        );
      })
    );
  }

  protected getUserRefreshObservable(): Observable<void> {
    const userId$ = this.userIdService.takeUserId();
    const sources = [userId$, ... this.extraRefreshSources || []];
    return merge(...sources).pipe(map(() => undefined));
  }


  // Helper that shares and caches HTTP responses to avoid redundant requests for the same chunk.
  // Errors are not cached — the in-flight entry is removed so the next subscriber retries.
  getWithCache(url: string): Observable<any> {
    if (this.chunkResponseCache.has(url)) {
      return of(this.chunkResponseCache.get(url));
    }

    if (this.chunkRequestsInProgress.has(url)) {
      return this.chunkRequestsInProgress.get(url)!;
    }

    const request$ = this.http.get(url).pipe(
      tap((result) => {
        this.chunkResponseCache.set(url, result);
      }),
      catchError(() => of({})),
      finalize(() => {
        this.chunkRequestsInProgress.delete(url);
      }),
      shareReplay(1)
    );

    this.chunkRequestsInProgress.set(url, request$);

    return request$;
  }

  private clearChunkCache(): void {
    this.chunkResponseCache.clear();
    this.chunkRequestsInProgress.clear();
  }

  private findFirstResolvedTranslation(resultArray: TranslatedValue[]): string {
    for (const translatedValue of resultArray) {
      if (translatedValue.translationSource !== TranslationSource.fallbackFromMissingKey) {
        return translatedValue.value;
      }
    }

    return resultArray[0].value;
  }
}
