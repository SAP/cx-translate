/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Utility class for splitting lists with optional boundary constraints.
 */
public final class CxTranslateListSplitUtil {

	private CxTranslateListSplitUtil() {
	}

	/**
	 * Splits the input list into sublists based on a predicate condition while respecting an optional
	 * boundary constraint between adjacent elements.
	 *
	 * <p>Elements are accumulated into a sublist until {@code splitWhen} returns {@code true} for
	 * the current sublist. When the condition is met, a new sublist is started, unless
	 * {@code mustStayTogether} returns {@code true} for the last element of the current sublist
	 * and the next element, in which case the split is deferred to keep them together.
	 *
	 * <p>Example:
	 * <pre>{@code
	 * List<String> input = List.of("a", "b", "c", "d", "e", "f");
	 * List<List<String>> parts = CxTranslateListSplitUtil.splitWithBoundaryConstraint(
	 *     input,
	 *     list -> list.size() >= 2,
	 *     (a, b) -> a.equals("d") && b.equals("e")
	 * );
	 * // parts = [[a, b], [c, d, e], [f]]
	 * // Note: "d" and "e" stayed together despite split condition being met
	 * }</pre>
	 *
	 * @param input            the source list to split
	 * @param splitWhen        predicate that determines when to start a new sublist based on the current
	 *                         sublist state; must not be {@code null}
	 * @param mustStayTogether predicate indicating whether two adjacent elements must remain
	 *                         together across a potential split; if {@code null}, no boundary constraint is applied
	 * @param <T>              element type
	 * @return a list of sublists in original order
	 * @throws NullPointerException if {@code input} or {@code splitWhen} is {@code null}
	 */
	public static <T> List<List<T>> splitWithBoundaryConstraint(
			final List<T> input,
			final Predicate<? super List<T>> splitWhen,
			final BiPredicate<? super T, ? super T> mustStayTogether
	) {
		Objects.requireNonNull(input, "input");
		Objects.requireNonNull(splitWhen, "splitWhen");

		final BiPredicate<? super T, ? super T> boundary = mustStayTogether != null
				? mustStayTogether
				: (a, b) -> false; // default to no boundary constraint

		if (input.isEmpty()) {
			return List.of();
		}

		final List<List<T>> result = new ArrayList<>();
		List<T> current = new ArrayList<>();
		current.add(input.get(0));

		for (int i = 1; i < input.size(); i++) {
			final T next = input.get(i);
			final T last = current.get(current.size() - 1);

			// Check if we should split: condition is met AND elements don't need to stay together
			if (splitWhen.test(current) && !boundary.test(last, next)) {
				result.add(current);
				current = new ArrayList<>();
			}

			current.add(next);
		}

		result.add(current);
		return result;
	}
}
