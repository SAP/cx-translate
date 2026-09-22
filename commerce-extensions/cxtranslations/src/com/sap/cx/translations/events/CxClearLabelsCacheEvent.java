/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */

package com.sap.cx.translations.events;

import de.hybris.platform.servicelayer.event.ClusterAwareEvent;
import de.hybris.platform.servicelayer.event.PublishEventContext;
import de.hybris.platform.servicelayer.event.events.AbstractEvent;

public class CxClearLabelsCacheEvent extends AbstractEvent implements ClusterAwareEvent {
	@Override
	public boolean canPublish(PublishEventContext publishEventContext) {
		return true;
	}
}
