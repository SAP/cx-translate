/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException;
import com.sap.cx.translations.integration.exception.CxTranslateInterfaceTimeoutException;
import com.sap.cx.translations.integration.interceptor.CxTranslateDebugRequestInterceptor;
import com.sap.cx.translations.integration.interceptor.CxTranslateRequestHeaderInterceptor;
import com.sap.cx.translations.integration.strategy.RestErrorStrategy;
import de.hybris.platform.apiregistryservices.model.ConsumedDestinationModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.*;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Abstract rest service that provides shared configuration and error handling for {@link RestTemplate}-based services.
 * <p>
 * Subclasses are responsible for obtaining and caching their own {@link RestTemplate} instances
 * (e.g. per destination) and must implement {@link #getResponseEntity} to execute HTTP requests.
 */
public abstract class CxTranslateAbstractRestService {
    private static final Logger LOG = LoggerFactory.getLogger(CxTranslateAbstractRestService.class);

    private String endpointPath;

    private String interfaceEndpoint;
    private String interfacePropertiesName;

    private RestErrorStrategy restErrorStrategy;
    private DefaultResponseErrorHandler responseErrorHandler;

    /**
     * Configures the given {@link RestTemplate} with URI template handler, error handler,
     * content type header, JSON converter, and debug interceptor.
     *
     * @param restTemplate the template to configure
     */
    protected void setupRestTemplate(final RestTemplate restTemplate) {
        if (StringUtils.isNotBlank(interfaceEndpoint)) {
            final DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(interfaceEndpoint);
            restTemplate.setUriTemplateHandler(uriBuilderFactory);
        }

        if (responseErrorHandler != null) {
            restTemplate.setErrorHandler(responseErrorHandler);
        }

        addContentTypeHeader(restTemplate);
        setupJsonConverter(restTemplate);
        attachDebugger(restTemplate);
    }

    /**
     * Sets up content type HTTP header for every request. By default it is {@link MediaType#APPLICATION_JSON_VALUE}.
     *
     * @param restTemplate the template to add the header interceptor to
     */
    protected void addContentTypeHeader(final RestTemplate restTemplate) {
        restTemplate.getInterceptors()
                .add(new CxTranslateRequestHeaderInterceptor(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    }

    /**
     * Sets up JSON converter ({@link MappingJackson2HttpMessageConverter}) to use {@link JsonInclude.Include#NON_NULL}
     * serialization inclusion.
     *
     * @param restTemplate the template to configure the converter on
     */
    protected void setupJsonConverter(final RestTemplate restTemplate) {
        final List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();

        final Optional<HttpMessageConverter<?>> jsonConverter = messageConverters.stream()
                .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                .findFirst();

        if (jsonConverter.isPresent()) {
            messageConverters.remove(jsonConverter.get());

            final HttpMessageConverter<?> nonNullJsonConverter = createCustomJsonMessageConverter();
            messageConverters.add(nonNullJsonConverter);
        }
    }

    /**
     * Creates new JSON HTTP message converter - replaces default {@link MappingJackson2HttpMessageConverter}
     *
     * @return custom {@link HttpMessageConverter}
     */
    protected HttpMessageConverter<?> createCustomJsonMessageConverter() {
        final ObjectMapper objMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        return new MappingJackson2HttpMessageConverter(objMapper);
    }

    /**
     * Attaches debugger to all requests and responses. Configurable via properties. By default turned off.
     *
     * @param restTemplate the template to attach the debug interceptor to
     */
    protected void attachDebugger(final RestTemplate restTemplate) {
        restTemplate.getInterceptors()
                .add(new CxTranslateDebugRequestInterceptor(getInterfaceCode()));
    }

    /**
     * Generic method used to execute rest request with errors handling at the end of processing.
     *
     * @param logMessage             used to construct more detailed logs
     * @param responseEntitySupplier supplier used to execute request and return {@link ResponseEntity}
     * @return object of type @param <T>
     * @throws CxTranslateIntegrationException thrown in case of error during request
     */
    protected <RESPONSE> RESPONSE executeRequestWithErrorStrategy(
            final Supplier<ResponseEntity<RESPONSE>> responseEntitySupplier, final String logMessage)
            throws CxTranslateIntegrationException {
        final ResponseEntity<RESPONSE> responseEntity = executeRequestGetEntityWithErrorStrategy(responseEntitySupplier,
                logMessage);
        if (responseEntity != null) {
            return responseEntity.hasBody() ? responseEntity.getBody() : null;
        } else {
            return null;
        }
    }

    /**
     * Generic method used to execute rest request with errors handling at the end of processing.
     *
     * @param logMessage             used to construct more detailed logs
     * @param responseEntitySupplier supplier used to execute request and return {@link ResponseEntity}
     * @return response entity containing type @param <T>
     * @throws CxTranslateIntegrationException thrown in case of error during request
     */
    protected <RESPONSE> ResponseEntity<RESPONSE> executeRequestGetEntityWithErrorStrategy(
            final Supplier<ResponseEntity<RESPONSE>> responseEntitySupplier, final String logMessage)
            throws CxTranslateIntegrationException {
        try {
            LOG.debug("Executing request: {}", logMessage);
            return responseEntitySupplier.get();
        } catch (final HttpClientErrorException | HttpServerErrorException exception) {
            final int rawStatus = exception.getStatusCode().value();
            final HttpStatus httpStatus = HttpStatus.resolve(rawStatus);
            LOG.error(String.format("Http error [%d] during request to: %s", rawStatus, logMessage), exception);
            if (httpStatus != null) {
                getRestErrorStrategy().handleHttpStatusCode(httpStatus);
            } else {
                throw new CxTranslateIntegrationException("Http error [" + rawStatus + "] during request to: " + logMessage, exception);
            }
        } catch (final ResourceAccessException exception) {
            throw new CxTranslateInterfaceTimeoutException(exception.getMessage(), exception);
        } catch (final HttpMessageConversionException exception) {
            throw new CxTranslateIntegrationException((exception.getMessage()), exception);
        }

        return null;
    }

    /**
     * Executes an HTTP request using the given rest request descriptor, destination and URL.
     * Subclasses must provide the implementation that obtains the appropriate {@link RestTemplate}.
     *
     * @param restRequest the request descriptor
     * @param destination the consumed destination model
     * @param url         the target URL
     * @return the response entity
     */
    protected abstract <REQUEST, RESPONSE> ResponseEntity<RESPONSE> getResponseEntity(
            CxTranslateRestRequest<REQUEST, RESPONSE> restRequest, ConsumedDestinationModel destination, String url);

    /**
     * Gets endpoint path - default path
     */
    protected String getEndpointPath() {
        return endpointPath;
    }

    /**
     * Interface code that is defined by the documentation. By default it returns {@link #interfacePropertiesName}
     *
     * @return interface code
     */
    protected String getInterfaceCode() {
        return interfacePropertiesName;
    }

    /**
     * @return strategy for handling exceptions thrown by {@link RestTemplate}
     */
    protected RestErrorStrategy getRestErrorStrategy() {
        return restErrorStrategy;
    }

    public void setInterfaceEndpoint(final String interfaceEndpoint) {
        this.interfaceEndpoint = interfaceEndpoint;
    }

    public String getGroupId() {
        return null;
    }

    public void setInterfacePropertiesName(final String interfacePropertiesName) {
        this.interfacePropertiesName = interfacePropertiesName;
    }

    public void setRestErrorStrategy(final RestErrorStrategy restErrorStrategy) {
        this.restErrorStrategy = restErrorStrategy;
    }

    public void setResponseErrorHandler(final DefaultResponseErrorHandler responseErrorHandler) {
        this.responseErrorHandler = responseErrorHandler;
    }


    protected String getUri() {
        if (StringUtils.isBlank(interfaceEndpoint)) {
            throw new IllegalStateException("interfaceEndpoint is not configured for " + getClass().getSimpleName());
        }
        return interfaceEndpoint + StringUtils.defaultString(endpointPath);
    }

    public void setInterfaceEndpointPath(final String interfaceEndpointPath) {
        this.endpointPath = interfaceEndpointPath;
    }
}
