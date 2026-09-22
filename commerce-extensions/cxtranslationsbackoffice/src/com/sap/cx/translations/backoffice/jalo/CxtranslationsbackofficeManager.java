/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.backoffice.jalo;

import com.sap.cx.translations.backoffice.constants.CxtranslationsbackofficeConstants;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;

public class CxtranslationsbackofficeManager extends GeneratedCxtranslationsbackofficeManager {
	public static final CxtranslationsbackofficeManager getInstance() {
		ExtensionManager em = JaloSession.getCurrentSession()
										 .getExtensionManager();
		return (CxtranslationsbackofficeManager) em.getExtension(CxtranslationsbackofficeConstants.EXTENSIONNAME);
	}

}
