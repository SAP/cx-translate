/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.saptranslationhub;

import com.ibm.icu.text.PluralRules;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper for composing and parsing ICU MessageFormat plural messages for SAP Translation Hub.
 * <p>
 * When sending plural groups to SAP Translation Hub via XLIFF, individual plural form labels
 * (e.g. key_one, key_few, key_many, key_other) are composed into a single ICU plural message:
 * <pre>
 * {CNT, plural, one{# item in cart} few{# items in cart} many{# items in cart} other{# items in cart}}
 * </pre>
 * After translation, the response is parsed back into individual plural form values.
 * <p>
 * The composition includes all plural forms required by the <em>target</em> language (determined via
 * ICU4J CLDR PluralRules). When the source language does not have a matching form, a fallback
 * priority is used to select the closest available source text.
 */
public class CxSapTranslationHubPluralHelper {

    private static final String PLURAL_VARIABLE = "CNT";

    /**
     * Fallback priority when a target plural category has no matching source.
     * E.g., if target needs "few" but source only has "one" and "other", use "other".
     */
    private static final Map<String, List<String>> FALLBACK_PRIORITY = Map.of(
            "zero", List.of("other", "many", "few", "one", "two"),
            "one", List.of("other", "few", "many", "two", "zero"),
            "two", List.of("few", "one", "other", "many", "zero"),
            "few", List.of("other", "many", "one", "two", "zero"),
            "many", List.of("other", "few", "one", "two", "zero"),
            "other", List.of("many", "few", "one", "two", "zero")
    );

    /**
     * Pattern to parse an ICU plural message:
     * {CNT, plural, one{...} few{...} other{...}}
     */
    private static final Pattern ICU_PLURAL_PATTERN = Pattern.compile(
            "\\{\\s*" + PLURAL_VARIABLE + "\\s*,\\s*plural\\s*,\\s*(.+)\\}\\s*$", Pattern.DOTALL);

    /**
     * Pattern to match individual plural category clauses like: one{# item}
     * Handles nested braces by counting brace depth.
     */
    private static final Pattern CATEGORY_PATTERN = Pattern.compile(
            "(zero|one|two|few|many|other)\\{");

    private CxSapTranslationHubPluralHelper() {
    }

    /**
     * Composes an ICU plural message from a map of plural category to source text.
     * Includes all categories required by the target locale, filling in from source using fallback logic.
     *
     * @param sourceForms  map of plural category (e.g. "one", "other") to source text
     * @param targetLocale the target locale to determine required plural categories
     * @param prefix       text before the plural expression (e.g. "You have ")
     * @param suffix       text after the plural expression (may be empty)
     * @return composed ICU plural message string
     */
    public static String composePluralMessage(final Map<String, String> sourceForms,
                                              final Locale targetLocale,
                                              final String prefix,
                                              final String suffix) {
        final Set<String> targetCategories = PluralRules.forLocale(targetLocale).getKeywords();

        final StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(prefix)) {
            sb.append(prefix);
        }
        sb.append("{").append(PLURAL_VARIABLE).append(", plural,");

        for (final String category : targetCategories) {
            final String text = resolveSourceText(category, sourceForms);
            sb.append(" ").append(category).append("{").append(text).append("}");
        }

        sb.append("}");
        if (StringUtils.isNotEmpty(suffix)) {
            sb.append(suffix);
        }
        return sb.toString();
    }

    /**
     * Simplified composition: just wraps the given forms into ICU format without prefix/suffix.
     *
     * @param sourceForms  map of plural category to source text
     * @param targetLocale target locale for determining required categories
     * @return composed ICU plural message
     */
    public static String composePluralMessage(final Map<String, String> sourceForms,
                                              final Locale targetLocale) {
        return composePluralMessage(sourceForms, targetLocale, "", "");
    }

    /**
     * Parses an ICU plural message and extracts the individual plural category values.
     *
     * @param icuMessage the ICU plural message (e.g. "{CNT, plural, one{...} other{...}}")
     * @return map of plural category to translated text, or empty map if parsing fails
     */
    public static Map<String, String> parsePluralMessage(final String icuMessage) {
        if (StringUtils.isBlank(icuMessage)) {
            return Map.of();
        }

        final Matcher outerMatcher = ICU_PLURAL_PATTERN.matcher(icuMessage.trim());
        if (!outerMatcher.find()) {
            return Map.of();
        }

        final String categoriesStr = outerMatcher.group(1);
        return parseCategoryValues(categoriesStr);
    }

    /**
     * Checks if a given text looks like an ICU plural message that this helper can handle.
     */
    public static boolean isPluralMessage(final String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        return ICU_PLURAL_PATTERN.matcher(text.trim()).find();
    }

    /**
     * Gets the required plural categories for a given locale from CLDR.
     */
    public static Set<String> getRequiredCategories(final Locale locale) {
        return PluralRules.forLocale(locale).getKeywords();
    }

    private static String resolveSourceText(final String targetCategory, final Map<String, String> sourceForms) {
        // Direct match
        if (sourceForms.containsKey(targetCategory)) {
            return sourceForms.get(targetCategory);
        }

        // Fallback
        final List<String> fallbacks = FALLBACK_PRIORITY.getOrDefault(targetCategory, List.of("other"));
        for (final String fallback : fallbacks) {
            if (sourceForms.containsKey(fallback)) {
                return sourceForms.get(fallback);
            }
        }

        // Last resort: return any available value
        return sourceForms.values().stream().findFirst().orElse("");
    }

    /**
     * Parses "one{# item} few{# items} other{# items}" into a map.
     * Handles nested braces correctly.
     */
    private static Map<String, String> parseCategoryValues(final String input) {
        final Map<String, String> result = new LinkedHashMap<>();
        final Matcher matcher = CATEGORY_PATTERN.matcher(input);

        while (matcher.find()) {
            final String category = matcher.group(1);
            final int contentStart = matcher.end(); // position after the opening {
            final String value = extractBracedContent(input, contentStart);
            if (value != null) {
                result.put(category, value);
            }
        }

        return result;
    }

    /**
     * Extracts content between braces starting at the given position, handling nested braces.
     * <p>
     * Examples:
     * <ul>
     * <li>Input: "# item}", startPos=0 → returns "# item"</li>
     * <li>Input: "# {count} items}", startPos=0 → returns "# {count} items"</li>
     * <li>Input: "{nested {value}} text}", startPos=0 → returns "{nested {value}} text"</li>
     * </ul>
     *
     * @param input    the string containing the braced content
     * @param startPos position right after the opening brace
     * @return the content between matching braces, or null if braces don't match
     */
    private static String extractBracedContent(final String input, final int startPos) {
        int depth = 1;
        int pos = startPos;
        while (pos < input.length() && depth > 0) {
            final char c = input.charAt(pos);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            pos++;
        }
        if (depth == 0) {
            return input.substring(startPos, pos - 1);
        }
        return null;
    }
}

