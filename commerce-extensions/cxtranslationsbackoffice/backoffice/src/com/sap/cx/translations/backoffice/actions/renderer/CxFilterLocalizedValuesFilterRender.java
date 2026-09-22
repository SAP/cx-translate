/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.actions.renderer;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionListener;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.actions.impl.DefaultActionRenderer;
import com.sap.cx.translations.backoffice.actions.CxFilterLocalizedValuesAction;
import de.hybris.platform.core.Registry;
import de.hybris.platform.servicelayer.session.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.util.*;

/**
 * Renders a filter action with checkboxes for filtering localized values.
 * Displays a popup with configurable filter options when the action button is clicked.
 * The available filter options are provided by {@link CxFilterLocalizedValuesAction}.
 */
public class CxFilterLocalizedValuesFilterRender extends DefaultActionRenderer<Collection<Object>, Object> {

    public static final String SESSION_ATTR_INCLUDE_IN_SEARCH = "cxTranslateIncludeInSearch";
    private static final Logger LOG = LoggerFactory.getLogger(CxFilterLocalizedValuesFilterRender.class);

    protected static final String SCLASS_FILTER_CONTAINER = "cx-filter-localized-values-container";
    protected static final String SCLASS_FILTER_POPUP = "cx-filter-localized-values-popup";
    protected static final String SCLASS_FILTER_CHECKBOX_GROUP = "cx-filter-localized-values-checkbox-group";
    protected static final String SCLASS_FILTER_CHECKBOX_ITEM = "cx-filter-localized-values-checkbox-item";
    protected static final String SCLASS_FILTER_TITLE = "cx-filter-localized-values-title";

    @Override
    public void render(final Component parent, final CockpitAction<Collection<Object>, Object> action,
                       final ActionContext<Collection<Object>> context, final boolean updateMode,
                       final ActionListener<Object> listener) {

        if (!this.checkDataCompatibleWithDefinedInput(action, context)) {
            parent.getChildren().clear();
            return;
        }
        final boolean enabled = action.canPerform(context);

        final Div container = new Div();
        container.setSclass(SCLASS_FILTER_CONTAINER);
        parent.appendChild(container);

        final Popup popup = createFilterPopup(context, action, listener);
        container.appendChild(popup);

        final Button filterButton = new Button();
        filterButton.setIconSclass("z-icon-filter");
        filterButton.setVisible(enabled);
        filterButton.setDisabled(!enabled);

        if (enabled) {
            filterButton.addEventListener("onClick", event -> popup.open(filterButton, "after_start"));
        }

        container.appendChild(filterButton);
    }

    /**
     * Creates the popup containing the filter checkboxes.
     */
    protected Popup createFilterPopup(final ActionContext<Collection<Object>> context,
                                      final CockpitAction<Collection<Object>, Object> action,
                                      final ActionListener<Object> listener) {
        final Popup popup = new Popup();
        popup.setSclass(SCLASS_FILTER_POPUP);
        popup.setStyle("padding: 10px;");

        final Label title = new Label(context.getLabel("cxtranslate.filterConfigs.filterLabel"));
        title.setSclass(SCLASS_FILTER_TITLE);
        title.setStyle("font-weight: bold; display: block; margin-bottom: 6px;");
        popup.appendChild(title);

        final Div content = new Div();
        content.setSclass(SCLASS_FILTER_CHECKBOX_GROUP);

        final Map<String, String> optionsToChoose = getOptionsToChoose(action);
        for (final Map.Entry<String, String> entry : optionsToChoose.entrySet()) {
            final Checkbox checkbox = createFilterCheckbox(entry.getKey(), entry.getValue(), context, action, listener);
            final Div checkboxWrapper = new Div();
            checkboxWrapper.setSclass(SCLASS_FILTER_CHECKBOX_ITEM);
            checkboxWrapper.setStyle("margin:10px;");
            checkboxWrapper.appendChild(checkbox);
            content.appendChild(checkboxWrapper);
        }

        popup.appendChild(content);
        return popup;
    }

    /**
     * Creates a single filter checkbox with a change listener.
     */
    protected Checkbox createFilterCheckbox(final String filterKey, final String filterLabel,
                                            final ActionContext<Collection<Object>> context,
                                            final CockpitAction<Collection<Object>, Object> action,
                                            final ActionListener<Object> listener) {
        final Checkbox checkbox = new Checkbox(filterLabel);
        checkbox.setChecked(getSelectedFiltersFromSession().contains(filterKey));

        checkbox.addEventListener("onCheck", (EventListener<Event>) event -> {
            final Set<String> currentFilters = getSelectedFiltersFromSession();
            if (checkbox.isChecked()) {
                currentFilters.add(filterKey);
            } else {
                currentFilters.remove(filterKey);
            }
            onFilterSelectionChanged(currentFilters, context);
        });

        return checkbox;
    }


    /**
     * Called when a filter checkbox selection changes.
     * Override this method to implement actual filtering logic.
     *
     * @param selectedFilters the updated set of selected filter keys
     * @param context         the action context
     */
    protected void onFilterSelectionChanged(final Set<String> selectedFilters,
                                            final ActionContext<Collection<Object>> context) {
        final SessionService sessionService = Registry.getApplicationContext().getBean(SessionService.class);
        sessionService.setAttribute(SESSION_ATTR_INCLUDE_IN_SEARCH, selectedFilters);
    }

    /**
     * Reads the selected filters from the session.
     * Returns an empty mutable set if nothing is stored yet.
     */
    protected Set<String> getSelectedFiltersFromSession() {
        final SessionService sessionService = Registry.getApplicationContext().getBean(SessionService.class);
        final Set<String> stored = sessionService.getAttribute(SESSION_ATTR_INCLUDE_IN_SEARCH);
        return stored != null ? new LinkedHashSet<>(stored) : new LinkedHashSet<>();
    }

    /**
     * Returns the currently selected filter keys.
     */
    public Set<String> getSelectedFilters() {
        return getSelectedFiltersFromSession();
    }

    /**
     * Retrieves the filter options from the action class.
     *
     * @param action the cockpit action, expected to be an instance of {@link CxFilterLocalizedValuesAction}
     * @return the map of filter option code to display name, or an empty map if the action type is unexpected
     */
    protected Map<String, String> getOptionsToChoose(final CockpitAction<Collection<Object>, Object> action) {
        if (action instanceof CxFilterLocalizedValuesAction) {
            return ((CxFilterLocalizedValuesAction) action).getOptionsToChoose();
        }
        LOG.warn("Unexpected action type: {}", action.getClass().getName());
        return Collections.emptyMap();
    }
}
