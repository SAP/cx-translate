/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.renderers;

import com.hybris.cockpitng.components.Editor;
import com.hybris.cockpitng.core.config.impl.jaxb.listview.ListColumn;
import com.hybris.cockpitng.dataaccess.context.impl.DefaultContext;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacade;
import com.hybris.cockpitng.dataaccess.facades.permissions.PermissionFacade;
import com.hybris.cockpitng.dataaccess.facades.type.DataType;
import com.hybris.cockpitng.dataaccess.services.PropertyValueService;
import com.hybris.cockpitng.editors.EditorListener;
import com.hybris.cockpitng.engine.WidgetInstanceManager;
import com.hybris.cockpitng.util.UITools;
import com.hybris.cockpitng.util.notifications.NotificationService;
import com.hybris.cockpitng.util.notifications.event.NotificationEvent;
import com.hybris.cockpitng.util.notifications.event.NotificationEventTypes;
import com.hybris.cockpitng.widgets.collectionbrowser.CollectionBrowserController;
import com.hybris.cockpitng.widgets.collectionbrowser.mold.impl.listview.renderer.DefaultListCellRenderer;
import com.hybris.cockpitng.widgets.common.WidgetComponentRenderer;
import com.sap.cx.translations.backoffice.CxTranslationsBackofficeConstants;
import com.sap.cx.translations.enums.CxTranslationStatus;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Listcell;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.hybris.cockpitng.widgets.baseeditorarea.DefaultEditorAreaController.MODEL_VALUE_CHANGED;

/**
 * Renderer for a localized string property allowing for inline edit
 */
public class CxLocalizedStringInlineEditWidgetComponentRenderer extends DefaultListCellRenderer implements WidgetComponentRenderer<Listcell, ListColumn, Object> {

	private static final Logger LOG = LoggerFactory.getLogger(CxLocalizedStringInlineEditWidgetComponentRenderer.class.getName());
	private static final String STRING_EDITOR_ID = "com.hybris.cockpitng.editor.defaulttext";
	private static final String SAVE_BUTTON_LABEL_KEY = "hmc.organizer.editor.save.title";
	private static final String SAVE_BUTTON_INPROGRESS_LABEL_KEY = "cxtranslations.inlineeditor.inprogress";
	private static final String BUTTON_SCLASS = "save-button y-btn-primary";
	private static final String BUTTON_DISABLED_SCLASS = "save-button y-btn-warning";

	private Boolean readonly = false;
	private Boolean primaryLanguage = false;

	private PropertyValueService propertyValueService;
	private ObjectFacade objectFacade;
	private NotificationService notificationService;
	private CxTranslateCommerceCommonI18NService cxTranslateCommerceCommonI18NService;
	private ModelService modelService;
	private PermissionFacade permissionFacade;


	@Override
	protected void renderInternal(final Listcell listcell, final ListColumn columnConfiguration, final Object object, final DataType dataType, final WidgetInstanceManager widgetInstanceManger) {

		final Locale locale = getLocale(primaryLanguage, widgetInstanceManger);
		final HtmlBasedComponent editorContainer = this.createEditor(listcell, columnConfiguration, object, dataType, widgetInstanceManger, locale);

		this.fireComponentRendered(editorContainer, listcell, columnConfiguration, object);
		this.fireComponentRendered(listcell, columnConfiguration, object);
	}

	protected HtmlBasedComponent createEditor(final Listcell listcell, final ListColumn columnConfiguration, final Object object, final DataType dataType, final WidgetInstanceManager widgetInstanceManager, final Locale locale) {

		boolean computedReadOnly = readonly;

		// set as readonly in case the current user has no modification rights to the object
		if (!readonly && !permissionFacade.canChangeInstance(object)) {
			computedReadOnly = true;
		}

		final Editor editor = new Editor();
		if (modelService.isRemoved(object)) {
			return editor;
		}

		editor.setProperty(columnConfiguration.getQualifier());
		editor.setType(String.class.getName());
		editor.setWidgetInstanceManager(widgetInstanceManager);
		editor.setDefaultEditor(STRING_EDITOR_ID);
		editor.setEditorLabel(columnConfiguration.getQualifier());
		editor.setReadOnly(computedReadOnly || isProposedTranslationInPendingState(object));
		editor.setReadableLocales(permissionFacade.getAllReadableLocalesForCurrentUser());
		editor.setWritableLocales(permissionFacade.getAllWritableLocalesForCurrentUser());

		final Map<Locale, Object> localisedValuesMap = this.propertyValueService.readValue(object, columnConfiguration.getQualifier(), Collections.singletonList(locale));
		editor.setInitialValue(localisedValuesMap.get(locale));
		editor.setAttribute("hyperlink-candidate", Boolean.FALSE);
		editor.setWidgetInstanceManager(widgetInstanceManager);
		editor.afterCompose();

		if (!computedReadOnly) {

			final Hbox hbox = new Hbox();
			hbox.setWidth("100%");
			hbox.setPack("stretch");

			// add the same styling as the one used by default implementation
			UITools.modifySClass(hbox, "yw-listview-cell-label", true);

			final Cell editorCell = new Cell();
			editorCell.setParent(hbox);
			editorCell.appendChild(editor);

			final Button button = new Button();

			button.setSclass(BUTTON_SCLASS);
			button.setLabel(Labels.getLabel(SAVE_BUTTON_LABEL_KEY));
			button.addEventListener(Events.ON_CLICK, event -> {
				updateValue(object, columnConfiguration.getQualifier(), (String) editor.getValue(), widgetInstanceManager, locale, true);
				adjustButtonStatus(object, button, null, editor);
			});

			adjustButtonStatus(object, button, null, editor);

			final Cell buttonCell = new Cell();
			buttonCell.setParent(hbox);
			buttonCell.appendChild(button);
			listcell.appendChild(hbox);

			// overwrite the ON CLICK event to avoid loosing focus and opening the editor
			editor.addEventListener(Events.ON_CLICK, event -> {
				// enable the SAVE button whenever the editor is clicked
				adjustButtonStatus(object, button, event, editor);
			});

			// store change on model, save only on enter or ok
			editor.addEventListener(Editor.ON_EDITOR_EVENT, event -> {
				updateValue(object, columnConfiguration.getQualifier(), (String) editor.getValue(), widgetInstanceManager, locale, Events.ON_OK.equals(event.getData()) || EditorListener.ENTER_PRESSED.equals(event.getData()));
				adjustButtonStatus(object, button, event, editor);
			});

			return hbox;

		} else {
			UITools.modifySClass(editor, "yw-listview-cell-label", true);
		}

		listcell.appendChild(editor);
		return editor;
	}

	protected boolean isProposedTranslationInPendingState(final Object object) {
		return object instanceof CxProposedTranslationModel && CxTranslationStatus.PENDING == ((CxProposedTranslationModel) object).getTranslationStatus();
	}

	// enable save button only for modified items or when first entering the editor
	protected void adjustButtonStatus(final Object object, final Button button, final Event textFieldEvent, final Editor editor) {
		if (isProposedTranslationInPendingState(object)) {
			button.setDisabled(true);
			button.setLabel(Labels.getLabel(SAVE_BUTTON_INPROGRESS_LABEL_KEY));
			button.setSclass(BUTTON_DISABLED_SCLASS);
			editor.setWidth("0");
		}
		// when entering the editor for the first time enable the button
		else if (textFieldEvent != null && (Events.ON_FOCUS.equals(textFieldEvent.getName()) || Events.ON_CLICK.equals(textFieldEvent.getName()))) {
			button.setDisabled(false);
			button.setSclass(BUTTON_SCLASS);
		} else {
			// otherwise only enable the button if the data is modified
			button.setDisabled(!isModified(object));
			button.setSclass(BUTTON_SCLASS);
		}
	}

	protected boolean isModified(final Object object) {
		if (object instanceof CxLocalizedValueModel && ((CxLocalizedValueModel) object).getTarget() != null) {
			return objectFacade.isModified(((CxLocalizedValueModel) object).getTarget()) || objectFacade.isNew(((CxLocalizedValueModel) object).getTarget());
		}
		return objectFacade.isModified(object) || objectFacade.isNew(object);
	}

	protected Locale getLocale(final Boolean primaryLanguage, final WidgetInstanceManager widgetInstanceManager) {

		// get the selected languages from the widgets model
		String localeIsoCode = null;
		if (primaryLanguage) {
			localeIsoCode = widgetInstanceManager.getModel()
												 .getValue(CxTranslationsBackofficeConstants.PRIMARY_LANGUAGE_PARAM, String.class);
		} else {
			localeIsoCode = widgetInstanceManager.getModel()
												 .getValue(CxTranslationsBackofficeConstants.SECONDARY_LANGUAGE_PARAM, String.class);
		}

		Locale locale = null;
		if (StringUtils.isNotBlank(localeIsoCode)) {
			try {
				locale = cxTranslateCommerceCommonI18NService.getLocale(localeIsoCode);
			} catch (final Exception e) {
				LOG.error("No locale for iso code {}", localeIsoCode);
			}
		}

		// if no languages were selected choose the current locale as default
		if (locale == null) {
			locale = cxTranslateCommerceCommonI18NService.getCurrentLocale();
		}

		return locale;
	}

	protected void updateValue(final Object object, final String qualifier, final String value, final WidgetInstanceManager widgetInstanceManager, final Locale locale, final boolean save) {
		if (object != null) {
			final Map<Locale, String> valuesMap = new HashMap<>();
			valuesMap.put(locale, value);

			// set localised value on model
			try {
				this.modelService.setAttributeValue(object, qualifier, valuesMap);
			} catch (final Exception ex) {
				LOG.error(ex.getMessage(), ex);
				this.handleObjectSavingFailure(object, widgetInstanceManager);
			}

			// save the current item and inform the widget about the modified item
			if (save) {
				saveObject(object, widgetInstanceManager);
			}
		}

	}

	private void saveObject(Object object, WidgetInstanceManager widgetInstanceManager) {
		try {
			// disable reloading the whole list view to avoid loosing focus on the modified item
			final DefaultContext context = new DefaultContext();
			context.addAttribute(CollectionBrowserController.SHOULD_RELOAD_AFTER_UPDATE, Boolean.FALSE);
			context.addAttribute(ObjectFacade.CTX_PARAM_SUPPRESS_EVENT, Boolean.TRUE);

			if (object instanceof CxLocalizedValueModel) {
				this.modelService.save(((CxLocalizedValueModel) object).getTarget());
			} else {
				this.modelService.save(object);
			}

			widgetInstanceManager.getModel().setValue(MODEL_VALUE_CHANGED, Boolean.TRUE);
			widgetInstanceManager.getModel().changed();

			this.handleObjectSavingSuccess(object, widgetInstanceManager);
		} catch (final ModelSavingException ex) {
			LOG.error(ex.getMessage(), ex);
			this.handleObjectSavingFailure(object, widgetInstanceManager);
		}
	}

	protected void handleObjectSavingFailure(final Object savedObject, final WidgetInstanceManager widgetInstanceManager) {
		notificationService.notifyUser(widgetInstanceManager, NotificationEventTypes.EVENT_TYPE_OBJECT_UPDATE, NotificationEvent.Level.FAILURE, Collections.singletonList(savedObject));
	}


	protected void handleObjectSavingSuccess(final Object savedObject, final WidgetInstanceManager widgetInstanceManager) {
		notificationService.notifyUser(widgetInstanceManager, NotificationEventTypes.EVENT_TYPE_OBJECT_UPDATE, NotificationEvent.Level.SUCCESS, Collections.singletonList(savedObject));
	}

		public void setPropertyValueService(final PropertyValueService propertyValueService) {
		this.propertyValueService = propertyValueService;
	}

		public void setReadonly(final Boolean readonly) {
		this.readonly = readonly;
	}

	public Boolean getReadonly() {
		return readonly;
	}

		public void setObjectFacade(final ObjectFacade objectFacade) {
		this.objectFacade = objectFacade;
	}

		public void setNotificationService(final NotificationService notificationService) {
		this.notificationService = notificationService;
	}

		public void setPrimaryLanguage(final Boolean primaryLanguage) {
		this.primaryLanguage = primaryLanguage;
	}

	public Boolean getPrimaryLanguage() {
		return primaryLanguage;
	}

		public void setCxTranslateCommerceCommonI18NService(final CxTranslateCommerceCommonI18NService cxTranslateCommerceCommonI18NService) {
		this.cxTranslateCommerceCommonI18NService = cxTranslateCommerceCommonI18NService;
	}

		public void setPermissionFacade(final PermissionFacade permissionFacade) {
		this.permissionFacade = permissionFacade;
	}

		public void setModelService(final ModelService modelService) {
		this.modelService = modelService;
	}
}
