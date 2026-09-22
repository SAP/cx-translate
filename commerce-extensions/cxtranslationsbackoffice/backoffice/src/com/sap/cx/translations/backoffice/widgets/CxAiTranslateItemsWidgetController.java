/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.widgets;

import com.hybris.backoffice.widgets.processes.ProcessesController;
import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.annotations.ViewEvent;
import com.hybris.cockpitng.core.events.impl.DefaultCockpitEvent;
import com.hybris.cockpitng.util.notifications.event.NotificationEvent;
import com.sap.cx.translations.model.CxLocalizedLabelModel;
import com.sap.cx.translations.model.CxTranslationCronJobModel;
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;
import com.sap.cx.translations.services.config.CxLocalizedValueConfigService;
import com.sap.cx.translations.services.localizedobjects.parameters.CxStartTranslationCronJobForItemsParameters;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.servicelayer.type.TypeService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zkmax.zul.Chosenbox;
import org.zkoss.zkmax.zul.Tablechildren;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.ListModelList;

import java.util.List;
import java.util.Set;


/**
 * AI Controller for action for translating items
 */
public class CxAiTranslateItemsWidgetController extends CxAiTranslateValuesWidgetController {

	protected static final String NO_ATTRIBUTES_MESSAGE = "cxtranslations.translate.noattributes";

	@Wire
	protected Chosenbox attributesSelector;
	
	@Wire
	protected Tablechildren classificationCheckboxLabelContainer;

	@Wire
	protected Tablechildren classificationCheckboxContainer;

	@Wire
	protected Checkbox classificationCheckbox;

	@Wire
	protected Tablechildren automaticPluralsCheckboxLabelContainer;

	@Wire
	protected Tablechildren automaticPluralsCheckboxContainer;

	@Wire
	protected Checkbox automaticPluralsCheckbox;

	@Resource
	protected CxLocalizedValueConfigService cxLocalizedValueConfigService;

	@Resource
	protected TypeService typeService;


	protected final ListModelList<AttributeDescriptorModel> attributeSelectorModel = new ListModelList<>();

	@Override
	@SocketEvent(socketId = "itemsToTranslate")
	public void initializeWithContext(final Object data) {
		super.initializeWithContext(data);
		final String typeCode = dataPageable.getTypeCode();
		final boolean shouldClassificationBeVisible = shouldClassificationBeVisible(typeCode);
		final boolean shouldAutomaticPluralsBeVisible = shouldAutomaticPluralsBeVisible(typeCode);

		if (this.classificationCheckboxContainer != null) {
			classificationCheckboxContainer.setVisible(shouldClassificationBeVisible);
		}

		if (this.classificationCheckboxLabelContainer != null) {
			classificationCheckboxLabelContainer.setVisible(shouldClassificationBeVisible);
		}

		if (this.automaticPluralsCheckboxContainer != null) {
			automaticPluralsCheckboxContainer.setVisible(shouldAutomaticPluralsBeVisible);
		}

		if (this.automaticPluralsCheckboxLabelContainer != null) {
			automaticPluralsCheckboxLabelContainer.setVisible(shouldAutomaticPluralsBeVisible);
		}

        // Restore classification and automaticPlurals checkboxes from saved settings
        final SavedSettings savedSettings = (SavedSettings) Sessions.getCurrent().getAttribute(SETTINGS_SESSION_KEY);
        if (savedSettings != null) {
            if (this.classificationCheckbox != null && shouldClassificationBeVisible && savedSettings.getTranslateClassification() != null) {
                classificationCheckbox.setChecked(savedSettings.getTranslateClassification());
            }
            if (this.automaticPluralsCheckbox != null && shouldAutomaticPluralsBeVisible && savedSettings.getAutomaticPlurals() != null) {
                automaticPluralsCheckbox.setChecked(savedSettings.getAutomaticPlurals());
            }
        }

        if (dataPageable.getTotalCount() < 1) {
			return;
		}

		attributeSelectorModel.setMultiple(true);
		final Set<AttributeDescriptorModel> attributes = cxLocalizedValueConfigService.findTranslatableAttributesForTypeCode(typeCode);

		if (CollectionUtils.isNotEmpty(attributes)) {
			attributeSelectorModel.addAll(attributes);
			attributeSelectorModel.setSelection(attributes);
		}

		attributesSelector.setModel(attributeSelectorModel);

		updateTranslateButton();
	}

	private boolean shouldClassificationBeVisible(final String typeCode)
	{
		return typeService.isAssignableFrom(ProductModel._TYPECODE, typeCode) && cxLocalizedValueConfigService.findConfigForProductFeatures() != null;
	}

	private boolean shouldAutomaticPluralsBeVisible(final String typeCode) {
		return typeService.isAssignableFrom(CxLocalizedLabelModel._TYPECODE, typeCode);
	}


	@Override
	@ViewEvent(componentID = "translateButton", eventName = Events.ON_CLICK)
	public void translateButton() {

		final CxTranslationServiceSettingsModel translationSettings = aiSettingsSelectorModel.getSelection()
																							   .stream()
																							   .findFirst()
																							   .get();

		final LanguageModel sourceLanguage = primaryLanguageSelectorModel.getSelection().stream().findFirst().get();
		final List<LanguageModel> targetLanguages = secondaryLanguageSelectorModel.getSelection().stream().toList();

		final List<ItemModel> itemsToTranslate = dataPageable.getAllResults();
		final Integer elementsToProcess = itemsToTranslate.size();
		final boolean enableClassification = isClassificationEnabled();
		final CxStartTranslationCronJobForItemsParameters parameters = CxStartTranslationCronJobForItemsParameters.builder()
				.items(itemsToTranslate)
				.attributes(attributeSelectorModel.getSelection())
				.serviceSettings(translationSettings)
				.sourceLanguage(sourceLanguage)
				.targetLanguages(targetLanguages)
				.userHint(translationHint.getValue())
				.saveForApproval(approval.isChecked())
				.skipTranslated(skipTranslated.isChecked())
				.translateProductFeatures(enableClassification)
				.automaticPlurals(isAutomaticPluralsEnabled())
				.build();
		final CxTranslationCronJobModel cronJob = cxLocalizedValueService.startTranslationCronJobForItems(parameters);

		cockpitEventQueue.publishEvent(new DefaultCockpitEvent(ProcessesController.GLOBAL_EVENT_UPDATE_PROCESS_FOR_CRON_JOB, cronJob.getCode(), null));

		notificationService.notifyUser(getWidgetInstanceManager(), NOTIFICATION_TYPE, NotificationEvent.Level.SUCCESS, getLabel(SUCCESS_MESSAGE, new Object[]{elementsToProcess}));

		Sessions.getCurrent()
                .setAttribute(SETTINGS_SESSION_KEY, SavedSettings.builder()
                        .sourceLanguage(primaryLanguageSelectorModel.getSelection())
                        .targetLanguages(secondaryLanguageSelectorModel.getSelection())
                        .translationSettings(aiSettingsSelectorModel.getSelection())
                        .approval(approval.isChecked())
                        .skipTranslated(skipTranslated.isChecked())
                        .translateClassification(isClassificationEnabled())
                        .automaticPlurals(isAutomaticPluralsEnabled())
                        .build());
		sendOutput(SOCKET_OUTPUT_CLOSE, null);
	}

	@Override
	protected void addEventListeners() {
		final EventListener<Event> updateListener = new EventListener<Event>() {
			@Override
			public void onEvent(final Event event) {
				removeSourceLanguageFromTargetSelection();
				updateTranslateButton();
			}
		};

		aiSettingsSelector.addEventListener(Events.ON_SELECT, updateListener);
		primaryLanguageSelector.addEventListener(Events.ON_SELECT, updateListener);
		secondaryLanguageSelector.addEventListener(Events.ON_SELECT, updateListener);
		attributesSelector.addEventListener(Events.ON_SELECT, updateListener);
		if (classificationCheckbox != null) {
			classificationCheckbox.addEventListener(Events.ON_CHECK, updateListener);
		}
		if (automaticPluralsCheckbox != null) {
			automaticPluralsCheckbox.addEventListener(Events.ON_CHECK, updateListener);
		}

	}

	private boolean isClassificationEnabled() {
		return this.classificationCheckbox != null && this.classificationCheckboxContainer != null && this.classificationCheckboxContainer.isVisible() && this.classificationCheckbox.isChecked();
	}

	private boolean isAutomaticPluralsEnabled() {
		return this.automaticPluralsCheckbox != null && this.automaticPluralsCheckboxContainer != null && this.automaticPluralsCheckboxContainer.isVisible() && this.automaticPluralsCheckbox.isChecked();
	}

	@Override
	protected void updateTranslateButton() {
		final boolean isAiSettingsSelected = CollectionUtils.isNotEmpty(aiSettingsSelectorModel.getSelection());
		final boolean isPrimaryLanguageSelected = CollectionUtils.isNotEmpty(primaryLanguageSelectorModel.getSelection());
		final boolean isSecondaryLanguageSelected = CollectionUtils.isNotEmpty(secondaryLanguageSelectorModel.getSelection());
		final boolean isAttributeSelected = CollectionUtils.isNotEmpty(attributeSelectorModel.getSelection()) || isClassificationEnabled() || isAutomaticPluralsEnabled();

		translateButton.setDisabled(!(isAiSettingsSelected && isPrimaryLanguageSelected && isSecondaryLanguageSelected && isAttributeSelected));
	}

}
