/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.smartedit.jalo;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import com.sap.cx.translations.smartedit.constants.CxtranslationssmarteditConstants;
import org.apache.log4j.Logger;

@SuppressWarnings("PMD")
public class CxtranslationssmarteditManager extends GeneratedCxtranslationssmarteditManager
{
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger( CxtranslationssmarteditManager.class.getName() );

	public static final CxtranslationssmarteditManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (CxtranslationssmarteditManager) em.getExtension(CxtranslationssmarteditConstants.EXTENSIONNAME);
	}

}
