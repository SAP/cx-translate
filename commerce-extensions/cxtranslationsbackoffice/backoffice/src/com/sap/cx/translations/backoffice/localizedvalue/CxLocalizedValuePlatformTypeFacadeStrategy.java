/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.localizedvalue;

import com.hybris.backoffice.cockpitng.dataaccess.facades.type.DefaultPlatformTypeFacadeStrategy;
import com.hybris.cockpitng.dataaccess.facades.type.DataAttribute;
import com.hybris.cockpitng.dataaccess.facades.type.exceptions.TypeNotFoundException;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.Ordered;
import org.springframework.objenesis.ObjenesisHelper;

import java.lang.reflect.Method;

public class CxLocalizedValuePlatformTypeFacadeStrategy extends DefaultPlatformTypeFacadeStrategy implements Ordered {
	@Override
	protected DataAttribute convertAttribute(final AttributeDescriptorModel attributeDescriptorModel) throws TypeNotFoundException {

		final DataAttribute dataAttribute = super.convertAttribute(attributeDescriptorModel);

		if (attributeDescriptorModel.getQualifier()
									.equals(CxLocalizedValueModel.VALUE)) {
			final Enhancer enhancer = new Enhancer();
			enhancer.setUseCache(false);
			enhancer.setSuperclass(DataAttribute.class);
			enhancer.setCallbackType(MethodInterceptor.class);
			final Class aClass = enhancer.createClass();
			Enhancer.registerCallbacks(aClass, new Callback[]{new MethodInterceptor() {
				@Override
				public Object intercept(final Object o, final Method method, final Object[] objects, final MethodProxy methodProxy) throws Throwable {
					if ("isSearchable"
							  .equals(method.getName())) {
						return true;
					}
					return methodProxy.invoke(dataAttribute, objects);
				}
			}});
			final Object o = ObjenesisHelper.newInstance(aClass);
			return (DataAttribute) o;
		}
		return dataAttribute;
	}

	@Override
	public boolean canHandle(final String typeCode) {
		return typeCode.equals(CxLocalizedValueModel._TYPECODE);
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE;
	}
}
