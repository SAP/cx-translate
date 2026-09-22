package com.sap.cx.translations.cms;

import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.services.config.CxLocalizedValueConfigService;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService;

import de.hybris.platform.cms2.model.contents.CMSItemModel;
import de.hybris.platform.cms2.model.pages.AbstractPageModel;
import de.hybris.platform.cms2.model.relations.ContentSlotForPageModel;
import de.hybris.platform.cms2.model.relations.ContentSlotForTemplateModel;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.type.TypeService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Service that builds a {@link CxTranslateCmsTree} for a given root CMS item.
 *
 * <p>Starting from a root {@link CMSItemModel} it:
 * <ul>
 *     <li>uses {@link CxLocalizedValueConfigService} to resolve the translatable attributes configured for
 *     the item's type,</li>
 *     <li>uses {@link CxLocalizedValueService} to derive the {@link CxLocalizedValueModel}s of the item,</li>
 *     <li>introspects the item's attributes (via {@link TypeService} / {@link ModelService}) to discover
 *     referenced/child CMS items and recurses into them, and</li>
 *     <li>protects against cycles by tracking already visited item PKs.</li>
 * </ul>
 *
 * <p>The attributes to skip during child-item discovery are configurable via two
 * {@code project.properties} entries:
 * <ul>
 *     <li>{@code cxtranslations.cms.tree.skip.patterns} — base list (replaces the built-in defaults
 *     when set).</li>
 *     <li>{@code cxtranslations.cms.tree.skip.patterns.extra} — additional patterns merged on top of
 *     the base list without replacing it (use for project-specific additions).</li>
 * </ul>
 * Each entry is a comma-separated list of {@code TypeRegex:QualifierRegex} pairs, e.g.
 * {@code .*:containers,AbstractCMSComponent:slots}.
 */
public class CxTranslateCmsService implements InitializingBean
{
	private static final Logger LOG = LoggerFactory.getLogger(CxTranslateCmsService.class);

	/** Default base skip patterns (used when the property is not set). */
	private static final String DEFAULT_SKIP_PATTERNS =
			".*:containers,.*:restrictions,.*:parents,.*:slots";

	static final String PROP_SKIP_PATTERNS = "cxtranslations.cms.tree.skip.patterns";
	static final String PROP_SKIP_PATTERNS_EXTRA = "cxtranslations.cms.tree.skip.patterns.extra";

	private CxLocalizedValueService localizedValueService;
	private CxLocalizedValueConfigService localizedValueConfigService;
	private TypeService typeService;
	private ModelService modelService;
	private ConfigurationService configurationService;

	/** Compiled skip patterns — each entry is [typeRegex, qualifierRegex]. Populated in afterPropertiesSet. */
	private List<String[]> effectiveSkipPatterns = new ArrayList<>();

	@Override
	public void afterPropertiesSet()
	{
		final String base = configurationService.getConfiguration()
		                                        .getString(PROP_SKIP_PATTERNS, DEFAULT_SKIP_PATTERNS);
		final String extra = configurationService.getConfiguration()
		                                         .getString(PROP_SKIP_PATTERNS_EXTRA, "");

		final List<String[]> patterns = new ArrayList<>();
		parsePatterns(base, patterns);
		parsePatterns(extra, patterns);
		this.effectiveSkipPatterns = patterns;

		if (LOG.isDebugEnabled())
		{
			LOG.debug("CxTranslateCmsService skip patterns ({}): {}", patterns.size(),
					patterns.stream().map(p -> p[0] + ":" + p[1]).collect(Collectors.joining(", ")));
		}
	}

	private static void parsePatterns(final String raw, final List<String[]> target)
	{
		if (StringUtils.isBlank(raw))
		{
			return;
		}
		for (final String token : raw.split(","))
		{
			final String trimmed = token.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}
			final int colon = trimmed.indexOf(':');
			if (colon <= 0 || colon == trimmed.length() - 1)
			{
				LOG.warn("Invalid CMS tree skip pattern (expected TypeRegex:QualifierRegex): '{}'", trimmed);
				continue;
			}
			target.add(new String[]{ trimmed.substring(0, colon), trimmed.substring(colon + 1) });
		}
	}

	/**
	 * Returns {@code true} when the given attribute qualifier should be skipped for the given item type
	 * during child-item discovery.
	 */
	protected boolean shouldSkip(final String itemType, final String qualifier)
	{
		for (final String[] pattern : effectiveSkipPatterns)
		{
			try
			{
				if (itemType.matches(pattern[0]) && qualifier.matches(pattern[1]))
				{
					return true;
				}
			}
			catch (final java.util.regex.PatternSyntaxException ex)
			{
				LOG.warn("Invalid regex in skip pattern '{}:{}': {}", pattern[0], pattern[1], ex.getMessage());
			}
		}
		return false;
	}

	/**
	 * Recursively builds a {@link CxTranslateCmsTree} rooted at the provided CMS item.
	 *
	 * @param rootItem        the root CMS item to start the traversal from
	 * @param includeTemplate whether to include template content slots (for page items)
	 * @return the fully populated tree, or {@code null} if {@code rootItem} is {@code null}
	 */
	public CxTranslateCmsTree buildTree(final CMSItemModel rootItem, final boolean includeTemplate)
	{
		if (rootItem == null)
		{
			return null;
		}
		// LinkedHashMap keyed by PK preserves discovery order while protecting against cycles.
		return buildTree(rootItem, new LinkedHashMap<>(), includeTemplate);
	}

	/**
	 * Recursive worker that builds the tree for a single item while keeping track of the items already
	 * visited to avoid cycles.
	 *
	 * @param item            the current CMS item
	 * @param visited         map of already visited item PKs to the tree node created for them
	 * @param includeTemplate whether to include template content slots (for page items)
	 * @return the tree node for the item, or {@code null} if the item was already visited (cycle)
	 */
	protected CxTranslateCmsTree buildTree(final CMSItemModel item, final Map<PK, CxTranslateCmsTree> visited,
	                                       final boolean includeTemplate)
	{

		final PK pk = item.getPk();
		if (pk != null && visited.containsKey(pk))
		{
			// Cycle (or diamond) detected - do not traverse again.
			if (LOG.isDebugEnabled())
			{
				LOG.debug("Skipping already visited CMS item [{}] to avoid cycles", pk);
			}
			return null;
		}

		final CxTranslateCmsTree node = new CxTranslateCmsTree();
		node.setRootItem(item);
		if (pk != null)
		{
			visited.put(pk, node);
		}

		node.setLocalizedValues(resolveLocalizedValues(item));

		final List<CxTranslateCmsTree> children = new ArrayList<>();
		for (final CMSItemModel child : findChildCmsItems(item, includeTemplate))
		{
			final CxTranslateCmsTree childNode = buildTree(child, visited, includeTemplate);
			if (childNode != null)
			{
				children.add(childNode);
			}
		}
		node.setChildren(children);

		return node;
	}

	/**
	 * Derives the localized values for the given item using {@link CxLocalizedValueService} and filters them
	 * to the attributes that are translatable according to {@link CxLocalizedValueConfigService}.
	 *
	 * @param item the item to derive localized values for
	 * @return the (possibly empty) list of localized values
	 */
	protected List<CxLocalizedValueModel> resolveLocalizedValues(final CMSItemModel item)
	{
		final List<CxLocalizedValueModel> values = localizedValueService.getForItem(item);
		if (values == null || values.isEmpty())
		{
			return new ArrayList<>();
		}

		final Set<String> translatableAttributes = localizedValueConfigService
				.findTranslatableAttributesForTypeCode(item.getItemtype())
				.stream()
				.filter(Objects::nonNull)
				.map(AttributeDescriptorModel::getQualifier)
				.collect(Collectors.toSet());

		// If no configuration exists we keep all derived values, otherwise restrict to configured attributes.
		if (translatableAttributes.isEmpty())
		{
			return new ArrayList<>(values);
		}

		ArrayList<CxLocalizedValueModel> collect = values.stream()
		                                                 .filter(value -> value.getAttribute() == null || translatableAttributes.contains(
				                                                 value.getAttribute()))
		                                                 .collect(Collectors.toCollection(ArrayList::new));
		if (LOG.isDebugEnabled())
		{
			LOG.debug("added attributes" + collect.stream().map(CxLocalizedValueModel::getAttribute).collect(Collectors.joining(",")));
		}
		return collect;
	}

	/**
	 * Introspects the readable attributes/relations of the given item and collects the referenced CMS items.
	 * Values may be single items, collections or maps; only {@link CMSItemModel} instances are returned.
	 *
	 * <p>Attributes to skip are driven by {@link #shouldSkip(String, String)} which reads patterns from
	 * {@code cxtranslations.cms.tree.skip.patterns} and {@code cxtranslations.cms.tree.skip.patterns.extra}.
	 *
	 * @param item            the item to introspect
	 * @param includeTemplate whether to include template content slots (for page items)
	 * @return the distinct child CMS items referenced by the item (in discovery order)
	 */
	protected List<CMSItemModel> findChildCmsItems(final CMSItemModel item, final boolean includeTemplate)
	{
		final ComposedTypeModel composedType = typeService.getComposedTypeForCode(item.getItemtype());
		final Map<PK, CMSItemModel> children = new LinkedHashMap<>();
		if (item instanceof AbstractPageModel page)
		{
			if (includeTemplate)
			{
				page.getMasterTemplate().getContentSlots().stream().map(ContentSlotForTemplateModel::getContentSlot)
				    .forEach(slot -> children.put(slot.getPk(), slot));
			}

			page.getContentSlots().stream().map(ContentSlotForPageModel::getContentSlot)
			    .forEach(slot -> children.put(slot.getPk(), slot));
			return new ArrayList<>(children.values());
		}

		for (final AttributeDescriptorModel descriptor : typeService.getAttributeDescriptorsForType(composedType))
		{
			if (shouldSkip(item.getItemtype(), descriptor.getQualifier()))
			{
				if (LOG.isDebugEnabled())
				{
					LOG.debug("ignoring (skip pattern): {}/{}", item.getItemtype(), descriptor.getQualifier());
				}
				continue;
			}
			if (LOG.isDebugEnabled())
			{
				LOG.debug("traversing: {}/{}", item.getItemtype(), descriptor.getQualifier());
			}
			if (!Boolean.TRUE.equals(descriptor.getReadable()))
			{
				continue;
			}
			collectCmsItemsFromAttribute(item, descriptor, children);
		}

		// Never treat the item itself as its own child.
		if (item.getPk() != null)
		{
			children.remove(item.getPk());
		}
		return new ArrayList<>(children.values());
	}

	/**
	 * Reads a single attribute value and extracts any referenced CMS items into the accumulator.
	 */
	private void collectCmsItemsFromAttribute(final CMSItemModel item, final AttributeDescriptorModel descriptor,
	                                          final Map<PK, CMSItemModel> accumulator)
	{
		final String qualifier = descriptor.getQualifier();
		try
		{
			final Object value = modelService.getAttributeValue(item, qualifier);
			extractCmsItems(value, accumulator);
		}
		catch (final RuntimeException ex)
		{
			// Some (e.g. dynamic) attributes may fail to resolve - ignore them for traversal purposes.
			if (LOG.isDebugEnabled())
			{
				LOG.debug("Could not read attribute [{}] of CMS item [{}] while building CMS tree: {}", qualifier,
						item.getPk(), ex.getMessage());
			}
		}
	}

	/**
	 * Recursively extracts {@link CMSItemModel} instances from a value that may be a single item, a
	 * collection or a map, adding them to the accumulator keyed by PK to deduplicate.
	 */
	private void extractCmsItems(final Object value, final Map<PK, CMSItemModel> accumulator)
	{
		if (value instanceof CMSItemModel cmsItem)
		{
			final PK childPk = cmsItem.getPk();
			if (childPk != null)
			{
				accumulator.putIfAbsent(childPk, cmsItem);
			}
		}
		else if (value instanceof Collection<?> collection)
		{
			collection.forEach(element -> extractCmsItems(element, accumulator));
		}
		else if (value instanceof Map<?, ?> map)
		{
			map.values().forEach(element -> extractCmsItems(element, accumulator));
		}
		// Non-item / non-collection values (e.g. Strings, numbers, non-CMS items) are ignored.
	}

	public void setLocalizedValueService(final CxLocalizedValueService localizedValueService)
	{
		this.localizedValueService = localizedValueService;
	}

	public void setLocalizedValueConfigService(final CxLocalizedValueConfigService localizedValueConfigService)
	{
		this.localizedValueConfigService = localizedValueConfigService;
	}

	public void setTypeService(final TypeService typeService)
	{
		this.typeService = typeService;
	}

	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	public void setConfigurationService(final ConfigurationService configurationService)
	{
		this.configurationService = configurationService;
	}
}
