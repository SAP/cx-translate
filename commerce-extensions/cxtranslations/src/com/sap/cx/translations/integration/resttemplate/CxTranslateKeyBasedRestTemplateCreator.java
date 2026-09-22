/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.integration.resttemplate;

import com.sap.cx.translations.enums.CxTranslateKeyBasedCredentialMode;
import com.sap.cx.translations.integration.interceptor.CxTranslateRequestHeaderInterceptor;
import de.hybris.platform.apiregistryservices.model.ConsumedDestinationModel;
import de.hybris.platform.apiregistryservices.model.CxTranslateKeyBasedCredentialModel;
import de.hybris.platform.outboundservices.cache.DestinationRestTemplateId;
import de.hybris.platform.outboundservices.client.impl.AbstractRestTemplateCreator;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.web.client.RestTemplate;

public class CxTranslateKeyBasedRestTemplateCreator extends AbstractRestTemplateCreator {
    @Override
    protected RestTemplate createRestTemplate(final ConsumedDestinationModel destination) {
        final RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());

        final CxTranslateKeyBasedCredentialModel credential = (CxTranslateKeyBasedCredentialModel) destination.getCredential();
        final CxTranslateRequestHeaderInterceptor authenticationInterceptor =
                new CxTranslateRequestHeaderInterceptor(resolveHeaderName(credential), resolveHeaderValue(credential));
        addInterceptors(restTemplate, authenticationInterceptor);
        addMessageConverters(restTemplate);
        return restTemplate;
    }

    private String resolveHeaderName(final CxTranslateKeyBasedCredentialModel credential) {
        if (CxTranslateKeyBasedCredentialMode.STANDALONE_HEADER.equals(credential.getMode())) {
            return credential.getKeyName();
        }
        return "Authorization";
    }

    private String resolveHeaderValue(final CxTranslateKeyBasedCredentialModel credential) {
        if (CxTranslateKeyBasedCredentialMode.STANDALONE_HEADER.equals(credential.getMode())) {
            return credential.getKeyValue();
        }
        return "%s %s".formatted(credential.getKeyName(), credential.getKeyValue());
    }

    @Override
    protected DestinationRestTemplateId getDestinationRestTemplateId(final ConsumedDestinationModel destination) {
        return new CxTranslateKeyBasedDestinationRestTemplateId(destination);
    }

    @Override
    public boolean isApplicable(final ConsumedDestinationModel destination) {
        return destination != null && destination.getCredential() instanceof CxTranslateKeyBasedCredentialModel;
    }

    private static class CxTranslateKeyBasedDestinationRestTemplateId implements DestinationRestTemplateId {
        private final ConsumedDestinationModel destination;

        public CxTranslateKeyBasedDestinationRestTemplateId(final ConsumedDestinationModel destination) {
            this.destination = destination;
        }

        @Override
        public ConsumedDestinationModel getDestination() {
            return destination;
        }

        @Override
        public Class<? extends RestTemplate> getRestTemplateType() {
            return RestTemplate.class;
        }

        @Override
        public int hashCode() {
            final CxTranslateKeyBasedCredentialModel credentials = (CxTranslateKeyBasedCredentialModel) destination.getCredential();
            return new HashCodeBuilder(17, 37).append(destination).append(credentials.getKeyName()).append(credentials.getKeyValue()).build();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final CxTranslateKeyBasedDestinationRestTemplateId that = (CxTranslateKeyBasedDestinationRestTemplateId) o;
            final CxTranslateKeyBasedCredentialModel thisCredentials = (CxTranslateKeyBasedCredentialModel) this.getDestination().getCredential();
            final CxTranslateKeyBasedCredentialModel thatCredentials = (CxTranslateKeyBasedCredentialModel) that.getDestination().getCredential();

            return new EqualsBuilder()
                    .append(destination, that.destination)
                    .append(getRestTemplateType(), that.getRestTemplateType())
                    .append(thisCredentials, thatCredentials)
                    .append(thisCredentials.getKeyName(), thatCredentials.getKeyName())
                    .append(thisCredentials.getKeyValue(), thatCredentials.getKeyValue())
                    .isEquals();
        }
    }
}
