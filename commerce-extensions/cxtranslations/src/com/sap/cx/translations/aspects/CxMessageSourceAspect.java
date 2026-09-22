/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.aspects;

import com.sap.cx.translations.messages.CxDatabaseMessageSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;

/**
 * Aspect wrapping {@link MessageSource} instances to delegate logic to {@link CxDatabaseMessageSource}
 */
public class CxMessageSourceAspect {
	private static final Logger LOG = LoggerFactory.getLogger(CxMessageSourceAspect.class);
	private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

	private final MessageSource messageSource;
	private volatile boolean enabled;

	public CxMessageSourceAspect(final MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public Object delegateToDatabaseMessageSource(final ProceedingJoinPoint pjp) throws Throwable {
		if (!enabled) {
			return pjp.proceed();
		}

		if (pjp.getTarget() instanceof CxDatabaseMessageSource) {
			return pjp.proceed();
		}

		if (Boolean.TRUE.equals(ACTIVE.get())) {
			return pjp.proceed();
		}

		final Object[] args = pjp.getArgs();
		String message = null;

		ACTIVE.set(Boolean.TRUE);
		try {
			if (args.length == 2) {
				message = messageSource.getMessage((MessageSourceResolvable) args[0], (Locale) args[1]);
			} else if (args.length == 3) {
				message = messageSource.getMessage((String) args[0], (Object[]) args[1], (Locale) args[2]);
			} else if (args.length == 4) {
				message = messageSource.getMessage((String) args[0], (Object[]) args[1], (String) args[2], (Locale) args[3]);
			}
		} catch (NoSuchMessageException e) {
			LOG.debug("Message not found in database message source: {}", e.getMessage());
		} finally {
			ACTIVE.set(Boolean.FALSE);
		}

		if (message != null) {
			return message;
		} else {
			return pjp.proceed();
		}
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}
}
