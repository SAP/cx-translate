/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.handler;

import com.hybris.backoffice.attributechooser.Attribute;
import com.hybris.backoffice.attributechooser.AttributesChooserNodesOperationsHandler;
import com.hybris.cockpitng.tree.util.TreeUtils;
import org.zkoss.zul.AbstractTreeModel;
import org.zkoss.zul.DefaultTreeModel;
import org.zkoss.zul.TreeNode;

import java.text.Collator;
import java.util.Comparator;

/**
 * Overwrite the {@link AttributesChooserNodesOperationsHandler} to avoid sorting the attributes when selecting them
 */
public class CxTranslationsAttributesChooserNodesOperationsHandler extends AttributesChooserNodesOperationsHandler {

	@Override
	public void moveNodesBetweenTrees(AbstractTreeModel<TreeNode<Attribute>> sourceModel, AbstractTreeModel<TreeNode<Attribute>> targetModel) {
		this.moveNodesBetweenTrees(sourceModel, targetModel, false);
	}
}
