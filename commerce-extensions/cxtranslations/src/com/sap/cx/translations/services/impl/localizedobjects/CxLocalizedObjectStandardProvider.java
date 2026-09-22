/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.exceptions.CxTranslationException;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxStandardLocalizedObjectConfigModel;
import com.sap.cx.translations.services.config.impl.CxLocalizedValueConfigServiceImpl;
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData.SearchCriteria;
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData.SortDirection;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValuesExtractor;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.core.model.type.TypeModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.type.TypeService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Implementation of {@link CxLocalizedObjectProvider} that works on the standard localized properties on items.
 */
public class CxLocalizedObjectStandardProvider extends CxAbstractLocalizedObjectFlexibleSearchProvider {
	private static final Logger LOG = LoggerFactory.getLogger(CxLocalizedObjectStandardProvider.class);

	private static final String MAIN_ALIAS = "locItem";
	private static final String TYPE_CODE_ALIAS = "locItemType";
	private static final String JOIN_QUERY_PART_FOR_COMPOSED_TYPE = " JOIN " + ComposedTypeModel._TYPECODE + " AS " + TYPE_CODE_ALIAS + " ON {" + TYPE_CODE_ALIAS + ".pk} = {" + MAIN_ALIAS + "." + ItemModel.ITEMTYPE + "}";

	private final String keyExpression;
	private final String itemTypeCode;

	private final List<String> keyParts = new ArrayList<>();
	private final List<String> objectIdParts = new ArrayList<>();

	private final CxLocalizedValuesExtractor valueExtractor;
	private final String joinPart;
	private final String objectIdExpression;
	private final CxLocalizedValuesSearchSupport searchSupport;
	private boolean shouldFilterByCatalog;


	public CxLocalizedObjectStandardProvider(final CxStandardLocalizedObjectConfigModel config, final CxLocalizedValueSearchData searchData, final CxLocalizedValuesSearchSupport searchSupport, final FlexibleSearchService flexibleSearchService, final ModelService modelService, final TypeService typeService, final int valueSearchLimit) {
		super(flexibleSearchService, searchData, searchSupport, valueSearchLimit);
		this.searchSupport = searchSupport;
		final String rootTypeCode = config.getType().getCode();
		this.config = config;
		this.code = config.getCode();
		this.itemTypeCode = rootTypeCode;

		this.valueExtractor = new CxDefaultLocalizedValuesExtractor(searchData, config, searchSupport, modelService, typeService);

		// Decide catalog filtering BEFORE building joins. The filter must fire whenever the config
		// declares catalog awareness with a catalogVersionField + non-empty catalogVersions, regardless
		// of whether catalog version appears as a *key field*. The previous implementation only flipped
		// this flag inside shouldIgnoreCatalogVersion() during the join recursion, so configs that were
		// catalog-aware but did not list catalog version as a key silently returned items from outside
		// the configured catalog versions (review finding #3).
		this.shouldFilterByCatalog = config.isCatalogAware()
				&& StringUtils.isNotBlank(config.getCatalogVersionField())
				&& CollectionUtils.isNotEmpty(config.getCatalogVersions());

		//adds required joins and saves 'keyParts' and 'objectIdParts'
		//for example for 'Region' which is unique identigied by its isocode and isocode of its Country:
		//fromQueryForKeyAttributes = JOIN Country AS joined ON {joined.PK} = {locItem.country}
		//keyParts = ["{joined.isocode}","{locItem.isocode}"]
		//objectIdParts is similar to keyParts but always excludes catalog version
		this.joinPart = buildJoinPartAndKeyParts();

		//keyExpression represents key (without .attribute part)
		//objectIdExpression represents object id
		//for example from above
		//objectIdExpression = CONCAT_WS('.' , {joined.isocode},{locItem.isocode})
		this.keyExpression = "CONCAT_WS(" + "'.' , {" + TYPE_CODE_ALIAS + ".code}," + String.join(",", keyParts) + ")";
		if (CollectionUtils.isEmpty(objectIdParts)) {
			throw new IllegalArgumentException("There are no unique key fields defined for Localized Object configuration " + config.getCode());
		}
		if (objectIdParts.size() == 1) {
			this.objectIdExpression = objectIdParts.get(0);
		} else {
			this.objectIdExpression = "CONCAT_WS(" + "'.' , " + String.join(",", objectIdParts) + ")";
		}
	}




	@Override
	protected Collection<CxFlexibleSearchQueryPart> getWherePartsToFilterObjectTypes(final Collection<String> objectTypes) {
		if (shouldFilterByType(objectTypes)) {
			return List.of(new CxFlexibleSearchQueryPart("{" + TYPE_CODE_ALIAS + "." + ComposedTypeModel.CODE + "} IN (?locTypeCodes)", Map.of("locTypeCodes", objectTypes)));
		}
		return Collections.emptyList();
	}

	@Override
	public String getStatisticsQuery() {
		final String commonQueryPart = "SELECT {" + TYPE_CODE_ALIAS + ".code}, count({" + MAIN_ALIAS + ".PK}) " + "FROM  {" + itemTypeCode + " AS " + MAIN_ALIAS;
        return commonQueryPart + JOIN_QUERY_PART_FOR_COMPOSED_TYPE + joinPart + " } !!WHERE_CLAUSE!! GROUP BY {" + TYPE_CODE_ALIAS + ".code}";
	}

	@Override
	public String getMainQuery(final Collection<String> configCodes) {
		final String commonQueryPart = "SELECT {" + MAIN_ALIAS + ".PK} ,(" + keyExpression + "),(" + objectIdExpression + ") FROM  {" + itemTypeCode + " AS " + MAIN_ALIAS;
		return commonQueryPart + JOIN_QUERY_PART_FOR_COMPOSED_TYPE + joinPart + " } !!WHERE_CLAUSE!! ORDER BY " + this.keyExpression + (searchData.sortDirection() == SortDirection.Descending ? " DESC" : "");
	}

	private boolean shouldFilterByType(final Collection<String> configCodes) {
		return typesWereFilteredByPredicates || configCodes != null;
	}

	private String buildJoinPartAndKeyParts() {
		return buildJoinPartAndKeyParts(config.getType(), config.getKeyFields(), MAIN_ALIAS, new MutableInt(0), true);
	}

	/**
	 * Method responsible for building expressions to work with Localized Value Key and Object ID and add necessary Joins.
	 * It processes key fields in a recursive manner.
	 *
	 * @param type            Current Composed Type. For 'Region' example main call has value 'Region' and then it's called recursively for `Country` as 'country.isocode` is a key for Region.
	 * @param uniqueKeyFields Unique key fields. For 'Region' example main call has value ["country.isocode","isocode"] and then it's called recursively for  ["isocode"]  as 'country.isocode` is a key for Region.
	 * @param previousAlias   alias from previous join, starting with MAIN_ALIAS and then joined1, joined2, etc.
	 * @param joinIndex       incremented counter for join aliases
	 * @param addToObjectId   if false current keys should not be included in Object ID. Used to not include Catalog Version related fields in Object ID
	 * @return string used to add joins in query.
	 */
	private String buildJoinPartAndKeyParts(final ComposedTypeModel type, final Collection<String> uniqueKeyFields, final String previousAlias, final MutableInt joinIndex, final boolean addToObjectId) {
		//copy to be used in lambda
		final MutableBoolean addToObjectIdCopy = new MutableBoolean(addToObjectId);

		final StringBuilder builder = new StringBuilder();
		final List<String> newKeyParts = new ArrayList<>();

		// Step 1 - we split key fields to nested ones and "simple one"
		// For 'Region' example:
		// true -> country.isocode
		// false -> isocode
		final Map<Boolean, List<String>> grouped = uniqueKeyFields.stream()
		                                                          .collect(Collectors.groupingBy(s -> s.contains(".")));

		final List<String> nestedAttributes = ListUtils.emptyIfNull(grouped.get(true));

		// Step 2 - nested key fields are grouped by their "root"
		// in following example "country" -> "country.isocode"
		final Map<String, List<String>> groupedByRoot = nestedAttributes.stream()
		                                                                .collect(Collectors.groupingBy(s -> s.split("\\.")[0]));


		//firstly we process nested attribute to add necessary joins and key parts
		groupedByRoot.entrySet().forEach(part -> {
			final Map<String, AttributeDescriptorModel> attributes = CxLocalizedValueConfigServiceImpl.getAllAttributesIncludingInherited(type);
			final String property = part.getKey();
			final AttributeDescriptorModel joiningAttribute = attributes.get(property);
            if (joiningAttribute == null) {
                throw new CxTranslationException("Could not find attribute for property %s on type %s".formatted(property,type.getCode()));
            }
			final String alias = joinIndex.getValue() == 0 ? "joined" : "joined" + joinIndex;


			if (shouldIgnoreCatalogVersion(previousAlias, joiningAttribute, addToObjectIdCopy)) {
				return;
			}

			final TypeModel attributeType = joiningAttribute.getAttributeType();
			final String JOIN_WITH_PREVIOUS_ALIAS = " JOIN " + attributeType.getCode() + " AS " + alias + " ON {" + alias + ".PK} = {" + previousAlias + "." + property + "} ";
			builder.append(JOIN_WITH_PREVIOUS_ALIAS);
			joinIndex.increment();
			builder.append(buildJoinPartAndKeyParts((ComposedTypeModel) attributeType, part.getValue()
			                                                                               .stream()
			                                                                               .map(s -> StringUtils.substringAfter(s, "."))
			                                                                               .toList(), alias, joinIndex, addToObjectIdCopy.toBoolean()));
		});

		ListUtils.emptyIfNull(grouped.get(false)).forEach(part -> {
			newKeyParts.add("{" + previousAlias + "." + part + "}");
		});

		this.keyParts.addAll(newKeyParts);

		if (addToObjectId) {
			this.objectIdParts.addAll(newKeyParts);
		}

		return builder.toString();
	}

	private boolean shouldIgnoreCatalogVersion(final String previousAlias, final AttributeDescriptorModel joiningAttribute, final MutableBoolean addToObjectIdCopy) {
		if (previousAlias.equals(MAIN_ALIAS) && joiningAttribute.getAttributeType()
		                                                        .getCode()
		                                                        .equals(CatalogVersionModel._TYPECODE)) {

			//we don't want to include fields related to catalogVersion in the Object ID
			addToObjectIdCopy.setValue(false);

			//if there is only one CatalogVersion we don't need to include it in the keys or joins.
			//Note: shouldFilterByCatalog is now set in the constructor based on config alone, independent
			//of whether catalog version appears as a key field — see review finding #3.
			if (!config.getCatalogVersions().isEmpty() && config.getCatalogVersions().size() == 1) {
				return true;
			}
		}
		return false;
	}




    @Override
	protected List<CxFlexibleSearchQueryPart> getWhereConditionsQueryParts(final CxLocalizedValueSearchData searchQueryData) {
		final List<CxFlexibleSearchQueryPart> parts = new ArrayList<>();
		if (shouldFilterByCatalog) {
			parts.add(new CxFlexibleSearchQueryPart("{" + MAIN_ALIAS + "." + this.config.getCatalogVersionField() + "} IN (?locCv)", Map.of("locCv", this.config.getCatalogVersions())));
		}
		// This counter is shared across all non-VALUE criteria to guarantee unique parameter names.
		// IMPORTANT: buildQueryPartByValue uses its own separate CxParamNameGenerator with a "val_"
		// prefix — do NOT use seeds starting with "val_" here to avoid collisions in the merged query.
		final CxParamNameGenerator counter = new CxParamNameGenerator();
		searchQueryData.criteria().stream().map(crt -> switch (crt.field()) {
			case KEY -> buildQueryByKey(crt, counter);
			case OBJECT_ID -> buildQueryByObjectId(crt, counter);
			case OBJECT_TYPE -> buildQueryByObjectType(crt, counter);
			case CATALOG_ID -> buildQueryByCatalogId(crt, counter, this.config);
			case CATALOG_VERSION_ID -> buildQueryByCatalogVersionId(crt, counter, this.config);
			case PK -> buildQueryByPK(crt, counter);
			default -> null;
		}).filter(Objects::nonNull).forEach(parts::add);

		// Criteria by value are created separately as they require specific grouping
		// for example if we have localized attributes: 'name' and 'description' and the query conditions are ["empty for en", "not empty for de"]
		// than query must look like (name[en] is null AND name[de] is not null) OR (description[en] is null AND description[de] is not null)
		final List<SearchCriteria> criteriaByValue = searchQueryData.criteria()
		                                                            .stream()
		                                                            .filter(crt -> crt.field() == CxLocalizedValueSearchData.SearchField.VALUE)
		                                                            .toList();
		if (!criteriaByValue.isEmpty()) {
			final CxFlexibleSearchQueryPart queryByValue = buildQueryPartByValue(criteriaByValue, config);
			if (queryByValue != null) {
				parts.add(queryByValue);
			}
		}

		return parts;
	}

	private CxFlexibleSearchQueryPart buildQueryPartByValue(final List<SearchCriteria> searchCriteria, final CxStandardLocalizedObjectConfigModel config) {
		// Intentionally uses a SEPARATE CxParamNameGenerator (not the one from getWhereConditionsQueryParts).
		// Reason: this method is called recursively for child configs (via config.getExtendedBy()), and
		// children only contribute SQL text — the parent provides the single canonical parameter map.
		// Each recursive level must produce the *same* parameter names (attributeParam0, attributeParam1, ...)
		// so the parent's map serves all levels. A shared counter would advance across levels and break this.
		// The seed "val_" prefix ensures no collision with the main counter's seeds (objectId, objectType, etc.).
		final CxParamNameGenerator paramCounter = new CxParamNameGenerator();
		final Map<SearchCriteria, String> parameterNames = searchCriteria.stream()
		                                                                 .collect(Collectors.toMap(crt -> crt, crt -> paramCounter.next("val_attributeParam")));

		String queryMainPart = "";
		final List<Predicate<String>> attributePredicates = searchSupport.getAttributePredicates();
		final List<AttributeDescriptorModel> activeAttributes = config.getLocalizedAttributes()
		                                                              .stream()
		                                                              .filter(attr -> attributePredicates.stream()
		                                                                                                 .allMatch(p -> p.test(attr.getQualifier())))
		                                                              .toList();

		if (CollectionUtils.isNotEmpty(activeAttributes)) {
			queryMainPart += "{" + MAIN_ALIAS + ".pk} IN ({{ SELECT {pk} FROM {" + config.getType()
			                                                                             .getCode() + "} WHERE ";
			final String conditions = activeAttributes.stream()
			                                          .map(attr -> buildValueSubQueryPartForAttribute(attr, searchCriteria, parameterNames))
			                                          .collect(Collectors.joining(" OR "));

			queryMainPart += conditions + " }})";

		}

		String partFromChildren = "";
		if (CollectionUtils.isNotEmpty(config.getExtendedBy())) {
			partFromChildren = config.getExtendedBy()
			                         .stream()
			                         .map(childConfig -> buildQueryPartByValue(searchCriteria, childConfig))
			                         .filter(Objects::nonNull)
			                         .map(CxFlexibleSearchQueryPart::queryPart)
			                         .collect(Collectors.joining(" OR "));

		}
		final boolean anyPart = StringUtils.isNotBlank(queryMainPart) || StringUtils.isNotBlank(partFromChildren);
		final boolean bothParts = StringUtils.isNotBlank(queryMainPart) && StringUtils.isNotBlank(partFromChildren);
		if (anyPart) {
			final String queryPrefix = "(";
			final String querySuffix = ")";

			final Map<String, Object> parameters = searchCriteria.stream()
			                                                     .filter(crt -> crt.value() != null && crt.value()
			                                                                                              .stringValue() != null)
			                                                     .collect(Collectors.toMap(parameterNames::get, crt -> crt.value()
			                                                                                                              .stringValue()));


			return new CxFlexibleSearchQueryPart(queryPrefix + queryMainPart + (bothParts ? " OR " : "") + partFromChildren + querySuffix, parameters);
		}
		return null;
	}

	private String buildValueSubQueryPartForAttribute(final AttributeDescriptorModel attr, final List<SearchCriteria> searchCriteria, final Map<SearchCriteria, String> parameterNames) {
		final String conditions = searchCriteria.stream().map(
				crt -> {
					final String languageQualifier = "[" + crt.value().locale().toLanguageTag() + "]:o";
					final String paramName = parameterNames.get(crt);
					final String condition = switch (crt.operator()) {
						case EQUALS -> "= ?" + paramName + " ";
						case LIKE -> "LIKE CONCAT('%',?" + paramName + ",'%' )";
						case STARTS_WITH -> "LIKE CONCAT(?" + paramName + ",'%' )";
						case ENDS_WITH -> "LIKE CONCAT('%',?" + paramName + " )";
						case IS_EMPTY -> "IS NULL";
						case IS_NOT_EMPTY -> "<> ''";
					};
					final String fieldTag = "{" + attr.getQualifier() + languageQualifier + "} ";
					return fixQueryForEmptyChecks(crt, fieldTag, condition);
				}
		).collect(Collectors.joining(" AND "));
		return "(" + conditions + ")";
	}

	private static String fixQueryForEmptyChecks(final SearchCriteria crt, final String fieldTag, final String condition) {
		String flexibleSearchCondition = fieldTag + condition;
		//Check for both NULLs and empty strings. Cast is required to handle MS SQL's TEXT type,
		//the length (8000) technically doesn't matter but this value is max working both in MS SQL in HSQLDB
		if (crt.operator() == CxLocalizedValueSearchData.SearchOperator.IS_EMPTY) {
			flexibleSearchCondition = "(" + flexibleSearchCondition + " OR CAST(" + fieldTag + " AS VARCHAR(8000)) = '')";
		}
		if (crt.operator() == CxLocalizedValueSearchData.SearchOperator.IS_NOT_EMPTY) {
			flexibleSearchCondition = "CAST(" + fieldTag + " AS VARCHAR(8000)) <> ''";
		}
		return flexibleSearchCondition;
	}

	/**
	 * Build query for OBJECT TYPE
	 *
	 * @param searchCriteria
	 * @param counter
	 * @return query part to be used in WHERE phrase
	 */
	private CxFlexibleSearchQueryPart buildQueryByObjectType(final SearchCriteria searchCriteria, final CxParamNameGenerator counter) {
		final String value = searchCriteria.value().stringValue();
		final boolean hasValue = StringUtils.isNotBlank(value);
		final String paramName = counter.next("objectType");
		final Map<String, Object> params = hasValue ? Map.of(paramName, value) : Collections.emptyMap();

		return switch (searchCriteria.operator()) {
			case EQUALS ->
					hasValue ? new CxFlexibleSearchQueryPart("{" + TYPE_CODE_ALIAS + "." + ComposedTypeModel.CODE + "} = ?" + paramName, params) : CxFlexibleSearchQueryPart.ALWAYS_FALSE;
			case STARTS_WITH ->
					hasValue ? new CxFlexibleSearchQueryPart("{" + TYPE_CODE_ALIAS + "." + ComposedTypeModel.CODE + "} LIKE CONCAT(?" + paramName + " ,'%') ", params) : null;
			case ENDS_WITH ->
					hasValue ? new CxFlexibleSearchQueryPart("{" + TYPE_CODE_ALIAS + "." + ComposedTypeModel.CODE + "} LIKE CONCAT('%', ?" + paramName + ") ", params) : null;
			case LIKE ->
					hasValue ? new CxFlexibleSearchQueryPart("{" + TYPE_CODE_ALIAS + "." + ComposedTypeModel.CODE + "} LIKE CONCAT('%', CONCAT(?" + paramName + " ,'%'))", params) : null;
			case IS_EMPTY -> CxFlexibleSearchQueryPart.ALWAYS_FALSE;
			case IS_NOT_EMPTY -> null;
			default -> null;
		};
	}

	/**
	 * Build query for CATALOG VERSION ID
	 *
	 * @param searchCriteria
	 * @param counter
	 * @return query part to be used in WHERE phrase
	 */
	private CxFlexibleSearchQueryPart buildQueryByCatalogVersionId(final SearchCriteria searchCriteria, final CxParamNameGenerator counter, final CxStandardLocalizedObjectConfigModel config) {
		if (StringUtils.isNotBlank(config.getCatalogVersionField())) {
			final String cvFieldRef = "{" + MAIN_ALIAS + "." + config.getCatalogVersionField() + "}";
			final Collection<CxFlexibleSearchQueryPart> parts = CxCatalogFilterQueryParts.forCatalogVersionId(cvFieldRef, searchCriteria, counter);
			return parts.isEmpty() ? null : parts.iterator().next();
		}
		return isEmptyOnlyQueryPart(searchCriteria);
	}

	private CxFlexibleSearchQueryPart buildQueryByCatalogId(final SearchCriteria searchCriteria, final CxParamNameGenerator counter, final CxStandardLocalizedObjectConfigModel config) {
		if (StringUtils.isNotBlank(config.getCatalogVersionField())) {
			final String cvFieldRef = "{" + MAIN_ALIAS + "." + config.getCatalogVersionField() + "}";
			final Collection<CxFlexibleSearchQueryPart> parts = CxCatalogFilterQueryParts.forCatalogId(cvFieldRef, searchCriteria, counter);
			return parts.isEmpty() ? null : parts.iterator().next();
		}
		return isEmptyOnlyQueryPart(searchCriteria);
	}

	/**
	 * Build query returning false for all except for IS EMPTY operators
	 *
	 * @param searchCriteria
	 * @return query part to be used in WHERE phrase
	 */
	private CxFlexibleSearchQueryPart isEmptyOnlyQueryPart(final SearchCriteria searchCriteria) {
		final CxFlexibleSearchQueryPart queryPart;
		queryPart = switch (searchCriteria.operator()) {
			case IS_EMPTY -> null;
			case IS_NOT_EMPTY -> CxFlexibleSearchQueryPart.ALWAYS_FALSE;
			case EQUALS -> CxFlexibleSearchQueryPart.ALWAYS_FALSE;
			case STARTS_WITH -> CxFlexibleSearchQueryPart.ALWAYS_FALSE;
			case ENDS_WITH -> CxFlexibleSearchQueryPart.ALWAYS_FALSE;
			case LIKE -> CxFlexibleSearchQueryPart.ALWAYS_FALSE;
		};
		return queryPart;
	}


    @Override
    protected boolean shouldCalculateStatisticsByForce() {
        return searchSupport.hasCriteriaForValue() && !searchSupport.isEqualsFilterForAttributeEnabled();
    }

    /**
	 * Build query for OBJECT ID
	 *
	 * @param searchCriteria
	 * @param counter
	 * @return query part to be used in WHERE phrase
	 */
	private CxFlexibleSearchQueryPart buildQueryByObjectId(final SearchCriteria searchCriteria, final CxParamNameGenerator counter) {
		final String value = searchCriteria.value() != null ? searchCriteria.value().stringValue() : null;
		final boolean hasValue = StringUtils.isNotBlank(value);
		final String paramName = counter.next("objectId");
		final Map<String, Object> params = hasValue ? Map.of(paramName, value) : Collections.emptyMap();

		return switch (searchCriteria.operator()) {
			case IS_EMPTY -> new CxFlexibleSearchQueryPart(objectIdExpression + " IS NULL ", params);
			case IS_NOT_EMPTY -> new CxFlexibleSearchQueryPart(objectIdExpression + " IS NOT NULL ", params);
			case EQUALS ->
					hasValue ? new CxFlexibleSearchQueryPart(objectIdExpression + " = ?" + paramName, params) : CxFlexibleSearchQueryPart.ALWAYS_FALSE;
			case STARTS_WITH ->
					hasValue ? new CxFlexibleSearchQueryPart(objectIdExpression + " LIKE CONCAT(?" + paramName + " ,'%') ", params) : null;
			case ENDS_WITH ->
					hasValue ? new CxFlexibleSearchQueryPart(objectIdExpression + " LIKE CONCAT('%', ?" + paramName + ") ", params) : null;
			case LIKE ->
					hasValue ? new CxFlexibleSearchQueryPart(objectIdExpression + " LIKE CONCAT('%', CONCAT(?" + paramName + " ,'%')) ", params) : null;
			default -> null;
		};
	}

	private CxFlexibleSearchQueryPart buildQueryByKey(final SearchCriteria searchCriteria, final CxParamNameGenerator counter) {
		final String valueForKey = StringUtils.substringBeforeLast(searchCriteria.value().stringValue(), ".");
		final boolean hasValue = StringUtils.isNotBlank(valueForKey);
		final String paramName = counter.next("providedKeyValue");

		if (searchCriteria.operator() == CxLocalizedValueSearchData.SearchOperator.EQUALS) {
			return hasValue ? new CxFlexibleSearchQueryPart(keyExpression + " = ?" + paramName, Map.of(paramName, valueForKey)) : CxFlexibleSearchQueryPart.ALWAYS_FALSE;
		} else {
			return null;
		}
	}

	private CxFlexibleSearchQueryPart buildQueryByPK(final SearchCriteria searchCriteria, final CxParamNameGenerator counter) {
		final String valueForPk = searchCriteria.value().stringValue();
		final boolean hasValue = StringUtils.isNotBlank(valueForPk);
		final String paramName = counter.next("pk");

		if (searchCriteria.operator() == CxLocalizedValueSearchData.SearchOperator.EQUALS) {
			return hasValue ? new CxFlexibleSearchQueryPart("{" + MAIN_ALIAS + "." + ItemModel.PK + "} = ?" + paramName, Map.of(paramName, valueForPk)) : CxFlexibleSearchQueryPart.ALWAYS_FALSE;
		} else {
			return null;
		}
	}


	@Override
	public List<String> allSupportedObjectTypes() {
		return getStatistics().keySet().stream().toList();
	}

	@Override
	public boolean canReturnObjectType(final String objectType) {
		final CxStandardLocalizedObjectConfigModel standardConfig = (CxStandardLocalizedObjectConfigModel) getConfig();
		final ComposedTypeModel type = standardConfig.getType();
		return type.getCode().equalsIgnoreCase(objectType) || type.getAllSubTypes().stream().anyMatch(t -> t.getCode().equalsIgnoreCase(objectType));
	}

	@Override
	public Stream<CxLocalizedValueModel> extractValues(final ItemWithIds itemWithId) {
		return this.valueExtractor.extractValues(itemWithId).stream();
	}

	@Override
	public int howManyValuesPerObjectForObjectType(final String objectType) {
        final int result = valueExtractor.valuesPerItemCount(objectType);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Value count for {} : {}", objectType, result);
        }
        return result;
	}
}
