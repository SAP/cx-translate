package com.sap.cx.translations.cms;

import de.hybris.platform.cms2.model.contents.CMSItemModel;

import java.util.List;

import com.sap.cx.translations.model.CxLocalizedValueModel;

public class CxTranslateCmsTree
{
	private CMSItemModel rootItem;
	private List<CxTranslateCmsTree> children;
	private List<CxLocalizedValueModel> localizedValues;

	public CMSItemModel getRootItem()
	{
		return rootItem;
	}

	public void setRootItem(final CMSItemModel rootItem)
	{
		this.rootItem = rootItem;
	}

	public List<CxTranslateCmsTree> getChildren()
	{
		return children;
	}

	public void setChildren(final List<CxTranslateCmsTree> children)
	{
		this.children = children;
	}

	public List<CxLocalizedValueModel> getLocalizedValues()
	{
		return localizedValues;
	}

	public void setLocalizedValues(final List<CxLocalizedValueModel> localizedValues)
	{
		this.localizedValues = localizedValues;
	}
}
