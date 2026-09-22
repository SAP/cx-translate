/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

/**
 * Implementation classes for the localized-objects search and translation subsystem.
 *
 * <h2>Lifecycle</h2>
 * <p>Nearly every class in this package is constructed <em>per {@code search()} invocation</em>
 * and must not be cached across requests or threads.  Specifically:</p>
 * <ul>
 *   <li>{@code CxLocalizedObjectsContext} — built once per search call; holds provider references.</li>
 *   <li>{@code CxLocalizedValuesSearchSupport} — wraps the search criteria predicates for one call.</li>
 *   <li>{@code CxLocalizedObjectStandardProvider} and {@code CxProductFeatureLocalizedObjectProvider}
 *       — created by the factory on each call; carry per-call SQL state such as {@code keyParts}
 *       and the brute-force value cache.</li>
 *   <li>{@code CxTranslationsValuesPageableImpl} — represents a single pageable result set;
 *       do not share across threads or reuse after the underlying transaction ends.</li>
 * </ul>
 *
 * <h2>Provider hierarchy</h2>
 * <pre>
 * CxLocalizedObjectProvider               (API interface)
 *   └── CxAbstractItemBasedLocalizedObjectProvider   (item-load + pagination math)
 *         └── CxAbstractLocalizedObjectFlexibleSearchProvider  (FlexibleSearch SQL support)
 *               ├── CxLocalizedObjectStandardProvider           (standard typed attributes)
 *               └── CxProductFeatureLocalizedObjectProvider     (classification features)
 * </pre>
 *
 * <h2>Key format</h2>
 * <p>Standard provider keys: {@code <ObjectType>.<objectId>.<attribute>}.<br>
 * ProductFeature keys (multi-catalog): {@code <ObjectType>.<catalogId>.<catalogVersionId>.<objectId>.<attribute>}.</p>
 *
 * @see com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider
 * @see com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService
 */
package com.sap.cx.translations.services.impl.localizedobjects;
