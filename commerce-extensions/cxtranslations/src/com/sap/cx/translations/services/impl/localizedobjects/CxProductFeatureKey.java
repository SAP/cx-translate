/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import de.hybris.platform.catalog.model.ProductFeatureModel;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * Value object representing the parts of a ProductFeature localized-value key.
 * <p>
 * Centralises the key format previously duplicated between
 * {@code CxLocalizedClassificationServiceImpl#getProductFeatureKey} (build) and
 * {@code CxProductFeatureLocalizedObjectProvider#extractBetweenFirstAndLastDot} (parse).
 *
 * <h2>Two key shapes</h2>
 * <ul>
 *   <li><b>Simple</b> — used when the config covers a single product catalog version:
 *       <pre>{@code <objectType>.<productCode>.<attribute>}</pre></li>
 *   <li><b>Catalog-aware</b> — used when the config covers more than one product catalog version,
 *       so the catalog/version need to disambiguate the product:
 *       <pre>{@code <objectType>.<catalogId>.<version>.<productCode>.<attribute>}</pre></li>
 * </ul>
 *
 * <h2>Why a value object</h2>
 * The previous code hard-coded {@code whichDot = 3} when there were &gt; 1 catalog versions and
 * {@code whichDot = 1} otherwise. A drift between the producer and the consumer would only show up
 * as silently empty filters or stray rows. By moving build &amp; parse into one place, both routes
 * stay in lockstep, parse failures surface as {@link Optional#empty()}, and the magic numbers
 * disappear.
 */
public final class CxProductFeatureKey {

	/**
	 * The object type embedded in every ProductFeature key. Kept in a constant so the literal is
	 * not duplicated in production code (replaces the {@code "ProductFeature"} string literal that
	 * previously appeared in {@code CxLocalizedClassificationServiceImpl#save}).
	 */
	public static final String OBJECT_TYPE = ProductFeatureModel._TYPECODE;

	private static final char SEPARATOR = '.';
	private static final String SEPARATOR_STR = ".";

	/** Number of dots in the simple format ({@code type.product.attribute}). */
	private static final int SIMPLE_DOT_COUNT = 2;

	/** Number of dots in the catalog-aware format ({@code type.catalog.version.product.attribute}). */
	private static final int CATALOG_AWARE_DOT_COUNT = 4;

	private final String objectType;
	private final String catalogId;       // null in the simple form
	private final String catalogVersion;  // null in the simple form
	private final String productCode;
	private final String attribute;

	private CxProductFeatureKey(final String objectType, final String catalogId, final String catalogVersion,
	                            final String productCode, final String attribute) {
		this.objectType = objectType;
		this.catalogId = catalogId;
		this.catalogVersion = catalogVersion;
		this.productCode = productCode;
		this.attribute = attribute;
	}

	/**
	 * Builds the simple key form (no catalog/version segment).
	 */
	public static CxProductFeatureKey simple(final String productCode, final String attribute) {
		Objects.requireNonNull(productCode, "productCode");
		Objects.requireNonNull(attribute, "attribute");
		return new CxProductFeatureKey(OBJECT_TYPE, null, null, productCode, attribute);
	}

	/**
	 * Builds the catalog-aware key form.
	 */
	public static CxProductFeatureKey catalogAware(final String catalogId, final String catalogVersion,
	                                               final String productCode, final String attribute) {
		Objects.requireNonNull(catalogId, "catalogId");
		Objects.requireNonNull(catalogVersion, "catalogVersion");
		Objects.requireNonNull(productCode, "productCode");
		Objects.requireNonNull(attribute, "attribute");
		return new CxProductFeatureKey(OBJECT_TYPE, catalogId, catalogVersion, productCode, attribute);
	}

	/**
	 * Parses a raw key string. Returns {@link Optional#empty()} when the input does not match
	 * either the simple or catalog-aware shape, or when the object-type prefix is wrong.
	 *
	 * @param raw            the raw key string, e.g. {@code "ProductFeature.cat.Online.SKU.color"}
	 * @param catalogAware   whether to expect the catalog-aware shape (true) or the simple one (false)
	 */
	public static Optional<CxProductFeatureKey> tryParse(final String raw, final boolean catalogAware) {
		if (StringUtils.isBlank(raw)) {
			return Optional.empty();
		}
		final int dots = StringUtils.countMatches(raw, SEPARATOR_STR);
		final int expectedDots = catalogAware ? CATALOG_AWARE_DOT_COUNT : SIMPLE_DOT_COUNT;
		if (dots != expectedDots) {
			return Optional.empty();
		}
		final String[] parts = StringUtils.split(raw, SEPARATOR);
		if (parts.length != expectedDots + 1 || !OBJECT_TYPE.equals(parts[0])) {
			return Optional.empty();
		}
		if (catalogAware) {
			return Optional.of(catalogAware(parts[1], parts[2], parts[3], parts[4]));
		}
		return Optional.of(simple(parts[1], parts[2]));
	}

	/**
	 * Extracts the product-code part from a raw key (i.e. the segment between the first dot after
	 * the object-type prefix and the last dot before the attribute). Returns {@link Optional#empty()}
	 * for malformed input.
	 *
	 * <p>Replaces {@code CxProductFeatureLocalizedObjectProvider#extractBetweenFirstAndLastDot} —
	 * which silently returned {@code null} on bad shapes and depended on a magic
	 * {@code whichDot=3} index.
	 */
	public static Optional<String> extractProductCode(final String raw, final boolean catalogAware) {
		return tryParse(raw, catalogAware).map(CxProductFeatureKey::productCode);
	}

	public String objectType() { return objectType; }

	public String catalogId() { return catalogId; }

	public String catalogVersion() { return catalogVersion; }

	public String productCode() { return productCode; }

	public String attribute() { return attribute; }

	public boolean isCatalogAware() { return catalogId != null; }

	/**
	 * Renders the key back to its raw on-the-wire form.
	 */
	public String toRawString() {
		if (isCatalogAware()) {
			return objectType + SEPARATOR + catalogId + SEPARATOR + catalogVersion + SEPARATOR + productCode + SEPARATOR + attribute;
		}
		return objectType + SEPARATOR + productCode + SEPARATOR + attribute;
	}

	@Override
	public String toString() {
		return toRawString();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (!(o instanceof final CxProductFeatureKey other)) return false;
		return Objects.equals(objectType, other.objectType)
				&& Objects.equals(catalogId, other.catalogId)
				&& Objects.equals(catalogVersion, other.catalogVersion)
				&& Objects.equals(productCode, other.productCode)
				&& Objects.equals(attribute, other.attribute);
	}

	@Override
	public int hashCode() {
		return Objects.hash(objectType, catalogId, catalogVersion, productCode, attribute);
	}
}
