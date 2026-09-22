/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.localizedvalue;

import com.hybris.cockpitng.dataaccess.context.Context;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacadeStrategy;
import com.hybris.cockpitng.dataaccess.facades.object.exceptions.ObjectCreationException;
import com.hybris.cockpitng.dataaccess.facades.object.exceptions.ObjectDeletionException;
import com.hybris.cockpitng.dataaccess.facades.object.exceptions.ObjectNotFoundException;
import com.hybris.cockpitng.dataaccess.facades.object.exceptions.ObjectSavingException;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.core.Ordered;

/**
 * Custom {@link ObjectFacadeStrategy} for {@link CxLocalizedValueModel}.
 */
public class CxLocalizedValueObjectFacadeStrategy implements ObjectFacadeStrategy, Ordered {
	private ModelService modelService;

	@Override
	public <T> boolean canHandle(final T t) {
		return t instanceof CxLocalizedValueModel;
	}

	@Override
	public <T> T load(final String s, final Context context) throws ObjectNotFoundException {
		throw new NotImplementedException();
	}

	@Override
	public <T> void delete(final T t, final Context context) throws ObjectDeletionException {
		throw new NotImplementedException();

	}

	@Override
	public <T> T create(final String s, final Context context) throws ObjectCreationException {
		throw new NotImplementedException();
	}

	@Override
	public <T> T save(final T t, final Context context) throws ObjectSavingException {
		final ItemModel target = ((CxLocalizedValueModel) t).getTarget();
		if (target != null) {
			modelService.save(target);
		}
		return t;
	}

	@Override
	public <T> T reload(final T t, final Context context) throws ObjectNotFoundException {
		final CxLocalizedValueModel model = (CxLocalizedValueModel) t;
		final ItemModel target = model.getTarget();
		if (target != null) {
			modelService.refresh(target);
		}
		return t;
	}

	@Override
	public <T> boolean isNew(final T object) {
		return false;
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE;
	}

		public void setModelService(final ModelService modelService) {
		this.modelService = modelService;
	}
}
