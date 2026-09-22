/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.azuretranslator;

import com.sap.cx.translations.integration.CxTranslateOutboundAbstractRestService;
import com.sap.cx.translations.model.CxAzureTranslatorServiceSettingsModel;
import com.sap.cx.translations.model.CxTranslateGlossaryModel;
import com.sap.cx.translations.serviceinterfaces.saptranslationhub.CxSapTranslationHubXliffHelper;
import de.hybris.platform.apiregistryservices.model.ConsumedDestinationModel;
import de.hybris.platform.servicelayer.media.MediaService;
import de.hybris.platform.util.Config;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;

/**
 * Service for interfacing with the Azure Translator document translation API.
 * <p>
 * Supports two modes:
 * <ul>
 *   <li><b>SYNCHRONOUS</b>: one multipart POST per value to /translator/document:translate. No Blob Storage needed.
 *       Each value is sent as a native {@code text/html} document. Glossary is sent as an optional inline
 *       multipart part read from the matching {@code CxTranslateGlossary} item's media.</li>
 *   <li><b>ASYNC_BATCH</b>: batch POST to /translator/document/batches. Requires an Azure Blob Storage
 *       account SAS URL configured via {@code cxtranslate.integrations.azuretranslator.storage.sas.url}.
 *       Each value is uploaded as its own {@code .html} blob; all blobs for the batch are submitted as
 *       multiple File-mode inputs in a single async batch job. Results are downloaded per-blob and mapped
 *       back by index.</li>
 * </ul>
 * <p>
 * Plural-group ICU messages are handled separately via {@link #translatePluralXliffSync} /
 * {@link #translatePluralXliffAsync}, which retain the XLIFF 1.2 path to preserve ICU syntax.
 */
public class CxAzureTranslatorInterfaceService extends CxTranslateOutboundAbstractRestService {

    private static final Logger LOG = LoggerFactory.getLogger(CxAzureTranslatorInterfaceService.class);

    private static final String STORAGE_SAS_URL_PROPERTY = "cxtranslate.integrations.azuretranslator.storage.sas.url";
    private static final String STORAGE_CONTAINER_PROPERTY = "cxtranslate.integrations.azuretranslator.storage.container";
    private static final String DEFAULT_STORAGE_CONTAINER = "cxtranslate-translations";
    private static final String API_VERSION = "2024-05-01";
    private static final String BLOB_API_VERSION = "2020-10-02";
    private static final String SYNC_TRANSLATE_PATH = "/translator/document:translate";
    private static final String BATCH_PATH = "/translator/document/batches";
    private static final int POLL_INTERVAL_MS = 2000;
    private static final int POLL_MAX_ATTEMPTS = 150; // 5 minutes max

    private static final MediaType TEXT_HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);
    private static final MediaType XLIFF_MEDIA_TYPE = new MediaType("application", "xliff+xml", StandardCharsets.UTF_8);
    private static final MediaType TEXT_PLAIN_UTF8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private static final Pattern BODY_PATTERN = Pattern.compile("(?is)<body[^>]*>(.*)</body>");

    private final RestTemplate plainRestTemplate = new RestTemplate();
    private MediaService mediaService;

    @Override
    protected void addContentTypeHeader(final RestTemplate restTemplate) {
        restTemplate.getMessageConverters().stream()
                .filter(StringHttpMessageConverter.class::isInstance)
                .map(StringHttpMessageConverter.class::cast)
                .forEach(c -> {
                    c.setDefaultCharset(StandardCharsets.UTF_8);
                    c.setSupportedMediaTypes(new ArrayList<>(List.of(
                            MediaType.TEXT_PLAIN, MediaType.TEXT_HTML, XLIFF_MEDIA_TYPE, MediaType.ALL)));
                });
    }

    // -------------------------------------------------------------------------
    // Public API — HTML translation
    // -------------------------------------------------------------------------

    /**
     * Translates HTML values synchronously (one multipart POST per value).
     * Each value is sent as a native {@code text/html} document so Azure preserves markup natively.
     *
     * @return map of key → translated HTML, in the same key order as the input
     */
    public Map<String, String> translateHtmlSync(final Map<String, String> keysToHtml,
                                                  final String targetLanguage,
                                                  final CxAzureTranslatorServiceSettingsModel settings) {
        final Map<String, String> result = new LinkedHashMap<>();
        for (final Map.Entry<String, String> entry : keysToHtml.entrySet()) {
            final byte[] bytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
            final String raw = doSyncTranslate(bytes, "document.html", TEXT_HTML_UTF8, targetLanguage, settings);
            result.put(entry.getKey(), unwrapHtmlBody(stripBom(raw)));
        }
        return result;
    }

    /**
     * Translates HTML values via the async batch API.
     * Each value is uploaded as its own {@code .html} blob; all blobs are submitted as multiple
     * File-mode inputs in a single Azure batch job. Results are downloaded per-blob and mapped back.
     *
     * @return map of key → translated HTML, in the same key order as the input
     */
    public Map<String, String> translateHtmlAsync(final Map<String, String> keysToHtml,
                                                   final String targetLanguage,
                                                   final CxAzureTranslatorServiceSettingsModel settings) {
        final String sasUrl = Config.getString(STORAGE_SAS_URL_PROPERTY, null);
        if (StringUtils.isBlank(sasUrl)) {
            throw new IllegalStateException(
                    "Async batch mode requires property " + STORAGE_SAS_URL_PROPERTY + " to be set.");
        }

        final String runId = UUID.randomUUID().toString();
        final String sourceFolder = "source/" + runId;
        final String targetFolder = "target/" + runId;
        String glossaryBlobName = null;

        // Stable ordered list for index→key mapping
        final List<Map.Entry<String, String>> entries = new ArrayList<>(keysToHtml.entrySet());

        try {
            ensureContainerExists(sasUrl);

            // Upload each value as its own HTML blob
            for (int i = 0; i < entries.size(); i++) {
                final String blobName = sourceFolder + "/" + i + ".html";
                uploadBlob(buildBlobUrl(sasUrl, blobName),
                        entries.get(i).getValue().getBytes(StandardCharsets.UTF_8),
                        "text/html; charset=UTF-8");
            }

            // Resolve glossary
            final CxTranslateGlossaryModel glossary = resolveGlossaryForLanguage(settings, targetLanguage);
            final String glossaryUrl = resolveAsyncGlossaryUrl(glossary, sasUrl, runId);
            if (glossaryUrl != null && glossaryUrl.contains(runId)) {
                glossaryBlobName = "glossary/" + runId;
            }

            // Build File-mode io pairs: one source→target blob pair per document
            final List<Map<String, String>> ioPairs = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) {
                final Map<String, String> pair = new java.util.HashMap<>();
                pair.put("sourceUrl", buildBlobUrl(sasUrl, sourceFolder + "/" + i + ".html"));
                pair.put("targetUrl", buildBlobUrl(sasUrl, targetFolder + "/" + i + ".html"));
                ioPairs.add(pair);
            }

            final ConsumedDestinationModel destination = settings.getAzureTranslatorServiceApi().getDestination();
            final RestTemplate restTemplate = getRestTemplate(destination);
            final String endpoint = destination.getUrl();

            final String jobId = submitBatchJobMulti(restTemplate, endpoint, ioPairs,
                    targetLanguage, glossaryUrl, glossary, settings);
            pollUntilComplete(restTemplate, endpoint, jobId);

            // Download results and map back to keys
            final Map<String, String> result = new LinkedHashMap<>();
            for (int i = 0; i < entries.size(); i++) {
                final String targetBlobUrl = buildBlobUrl(sasUrl, targetFolder + "/" + i + ".html");
                try {
                    final String translated = downloadBlob(targetBlobUrl);
                    if (translated != null) {
                        result.put(entries.get(i).getKey(), unwrapHtmlBody(stripBom(translated)));
                    }
                } catch (final Exception e) {
                    LOG.warn("Could not download translated blob {}/{}.html for '{}': {}",
                            targetFolder, i, targetLanguage, e.getMessage());
                }
            }
            return result;

        } finally {
            for (int i = 0; i < entries.size(); i++) {
                deleteBlob(buildBlobUrl(sasUrl, sourceFolder + "/" + i + ".html"));
                deleteBlob(buildBlobUrl(sasUrl, targetFolder + "/" + i + ".html"));
            }
            if (glossaryBlobName != null) {
                deleteBlob(buildBlobUrl(sasUrl, glossaryBlobName));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Public API — XLIFF translation (plural ICU messages only)
    // -------------------------------------------------------------------------

    /**
     * Translates a pre-built XLIFF document synchronously (multipart POST).
     * Used only for plural-group ICU messages where the XLIFF structure is needed to preserve
     * the {@code {CNT, plural, ...}} syntax.
     */
    public Map<String, String> translatePluralXliffSync(final String xliffBody, final String targetLanguage,
                                                         final CxAzureTranslatorServiceSettingsModel settings) {
        final byte[] xliffBytes = xliffBody.getBytes(StandardCharsets.UTF_8);
        final String responseXliff = doSyncTranslate(xliffBytes, "document.xlf", XLIFF_MEDIA_TYPE,
                targetLanguage, settings);
        return CxSapTranslationHubXliffHelper.parseTranslations(stripBom(responseXliff));
    }

    /**
     * Translates a pre-built XLIFF document via the async batch API (upload → submit → poll → download → cleanup).
     * Used only for plural-group ICU messages.
     */
    public Map<String, String> translatePluralXliffAsync(final String xliffBody, final String targetLanguage,
                                                          final CxAzureTranslatorServiceSettingsModel settings) {
        final String sasUrl = Config.getString(STORAGE_SAS_URL_PROPERTY, null);
        if (StringUtils.isBlank(sasUrl)) {
            throw new IllegalStateException(
                    "Async batch mode requires property " + STORAGE_SAS_URL_PROPERTY + " to be set.");
        }

        final String runId = UUID.randomUUID().toString();
        final String sourceBlobName = "source/" + runId + ".xlf";
        final String targetBlobName = "target/" + runId + ".xlf";
        String glossaryBlobName = null;

        try {
            ensureContainerExists(sasUrl);

            final String sourceBlobUrl = buildBlobUrl(sasUrl, sourceBlobName);
            final String targetBlobUrl = buildBlobUrl(sasUrl, targetBlobName);

            uploadBlob(sourceBlobUrl, xliffBody.getBytes(StandardCharsets.UTF_8), "application/xliff+xml");

            final CxTranslateGlossaryModel glossary = resolveGlossaryForLanguage(settings, targetLanguage);
            final String glossaryUrl = resolveAsyncGlossaryUrl(glossary, sasUrl, runId);
            if (glossaryUrl != null && glossaryUrl.contains(runId)) {
                glossaryBlobName = "glossary/" + runId;
            }

            final ConsumedDestinationModel destination = settings.getAzureTranslatorServiceApi().getDestination();
            final RestTemplate restTemplate = getRestTemplate(destination);
            final String endpoint = destination.getUrl();

            final Map<String, String> singlePair = new java.util.HashMap<>();
            singlePair.put("sourceUrl", sourceBlobUrl);
            singlePair.put("targetUrl", targetBlobUrl);
            final String jobId = submitBatchJobMulti(restTemplate, endpoint, List.of(singlePair),
                    targetLanguage, glossaryUrl, glossary, settings);

            pollUntilComplete(restTemplate, endpoint, jobId);

            final String translatedXliff = downloadBlob(targetBlobUrl);
            return CxSapTranslationHubXliffHelper.parseTranslations(stripBom(translatedXliff));

        } finally {
            deleteBlob(buildBlobUrl(sasUrl, sourceBlobName));
            deleteBlob(buildBlobUrl(sasUrl, targetBlobName));
            if (glossaryBlobName != null) {
                deleteBlob(buildBlobUrl(sasUrl, glossaryBlobName));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Synchronous path
    // -------------------------------------------------------------------------

    private String doSyncTranslate(final byte[] documentBytes, final String filename,
                                   final MediaType documentContentType, final String targetLanguage,
                                   final CxAzureTranslatorServiceSettingsModel settings) {
        final ConsumedDestinationModel destination = settings.getAzureTranslatorServiceApi().getDestination();
        final RestTemplate restTemplate = getRestTemplate(destination);

        final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("document", partEntity(documentBytes, filename, documentContentType));

        final CxTranslateGlossaryModel glossary = resolveGlossaryForLanguage(settings, targetLanguage);
        if (glossary != null && glossary.getMedia() != null) {
            final byte[] glossaryBytes = readGlossaryBytes(glossary);
            if (glossaryBytes != null) {
                final MediaType glossaryType = resolveGlossaryMediaType(glossary);
                body.add("glossary", partEntity(glossaryBytes, "glossary", glossaryType));
            }
        }

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(documentContentType, MediaType.ALL));

        final String url = buildSyncUrl(destination.getUrl(), targetLanguage, settings);
        final ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        return response.getBody();
    }

    private String buildSyncUrl(final String baseUrl, final String targetLanguage,
                                final CxAzureTranslatorServiceSettingsModel settings) {
        final UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(SYNC_TRANSLATE_PATH)
                .queryParam("api-version", API_VERSION)
                .queryParam("targetLanguage", targetLanguage);
        if (StringUtils.isNotBlank(settings.getCategory())) {
            builder.queryParam("category", settings.getCategory());
        }
        if (Boolean.FALSE.equals(settings.getAllowFallback())) {
            builder.queryParam("allowFallback", "false");
        }
        return builder.build().toUriString();
    }

    // -------------------------------------------------------------------------
    // Async batch path
    // -------------------------------------------------------------------------

    /**
     * Submits one batch translation job with multiple File-mode inputs (one source→target blob pair per entry).
     * Returns the job id extracted from the {@code Operation-Location} response header.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String submitBatchJobMulti(final RestTemplate restTemplate, final String endpoint,
                                        final List<Map<String, String>> ioPairs,
                                        final String targetLanguage, final String glossaryUrl,
                                        final CxTranslateGlossaryModel glossary,
                                        final CxAzureTranslatorServiceSettingsModel settings) {
        final ObjectNode root = OBJECT_MAPPER.createObjectNode();
        final ArrayNode inputs = root.putArray("inputs");

        for (final Map<String, String> pair : ioPairs) {
            final ObjectNode input = inputs.addObject();

            final ObjectNode source = input.putObject("source");
            source.put("sourceUrl", pair.get("sourceUrl"));
            source.put("storageSource", "AzureBlob");

            final ObjectNode target = input.putArray("targets").addObject();
            target.put("targetUrl", pair.get("targetUrl"));
            target.put("language", targetLanguage);
            if (StringUtils.isNotBlank(settings.getCategory())) {
                target.put("category", settings.getCategory());
            }
            if (StringUtils.isNotBlank(glossaryUrl)) {
                final String format = (glossary != null && StringUtils.isNotBlank(glossary.getGlossaryFormat()))
                        ? glossary.getGlossaryFormat()
                        : "tsv";
                final ObjectNode glossaryEntry = target.putArray("glossaries").addObject();
                glossaryEntry.put("glossaryUrl", glossaryUrl);
                glossaryEntry.put("format", format);
            }

            input.put("storageType", "File");
        }

        final String requestBody;
        try {
            requestBody = OBJECT_MAPPER.writeValueAsString(root);
        } catch (final com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize batch job request", e);
        }

        final String url = endpoint + BATCH_PATH + "?api-version=" + API_VERSION;
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final ResponseEntity<String> response = restTemplate.exchange(
                URI.create(url), HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);

        final String operationLocation = response.getHeaders().getFirst("Operation-Location");
        if (StringUtils.isBlank(operationLocation)) {
            throw new IllegalStateException("Batch job submit did not return Operation-Location header");
        }
        final String path = operationLocation.replaceAll("\\?.*", "");
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private void pollUntilComplete(final RestTemplate restTemplate, final String endpoint, final String jobId) {
        final String statusUrl = endpoint + BATCH_PATH + "/" + jobId + "?api-version=" + API_VERSION;
        for (int attempt = 0; attempt < POLL_MAX_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while polling Azure batch job " + jobId, e);
            }

            final ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(statusUrl), HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

            final String status = extractJsonField(response.getBody(), "status");
            LOG.debug("Azure batch job {} status: {}", jobId, status);

            if ("Succeeded".equalsIgnoreCase(status)) {
                return;
            }
            if ("Failed".equalsIgnoreCase(status) || "ValidationFailed".equalsIgnoreCase(status)
                    || "Canceled".equalsIgnoreCase(status)) {
                throw new IllegalStateException(
                        "Azure batch job " + jobId + " finished with status: " + status
                        + ". Response: " + response.getBody());
            }
        }
        throw new IllegalStateException("Azure batch job " + jobId + " did not complete within the polling timeout.");
    }

    // -------------------------------------------------------------------------
    // Blob storage helpers (plain REST, no SDK)
    // -------------------------------------------------------------------------

    /**
     * Creates the container if it doesn't exist. 409 Conflict (already exists) is silently ignored.
     */
    private void ensureContainerExists(final String sasUrl) {
        final int queryStart = sasUrl.indexOf('?');
        final String base = queryStart >= 0 ? sasUrl.substring(0, queryStart).replaceAll("/$", "") : sasUrl.replaceAll("/$", "");
        final String query = queryStart >= 0 ? sasUrl.substring(queryStart + 1) : "";
        final String containerUrl = base + "/" + storageContainer() + "?restype=container&" + query;
        try {
            final HttpHeaders headers = new HttpHeaders();
            headers.set("x-ms-version", BLOB_API_VERSION);
            headers.setContentLength(0);
            plainRestTemplate.exchange(URI.create(containerUrl), HttpMethod.PUT,
                    new HttpEntity<>(new byte[0], headers), Void.class);
            LOG.info("Created Azure Blob container '{}'", storageContainer());
        } catch (final org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 409) {
                LOG.debug("Azure Blob container '{}' already exists", storageContainer());
            } else {
                throw e;
            }
        }
    }

    private void uploadBlob(final String blobUrl, final byte[] content, final String contentType) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set("x-ms-blob-type", "BlockBlob");
        headers.set("x-ms-version", BLOB_API_VERSION);
        plainRestTemplate.exchange(URI.create(blobUrl), HttpMethod.PUT, new HttpEntity<>(content, headers), Void.class);
    }

    private String downloadBlob(final String blobUrl) {
        final ResponseEntity<byte[]> response = plainRestTemplate.exchange(
                URI.create(blobUrl), HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), byte[].class);
        final byte[] body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Empty response body when downloading blob: " + blobUrl);
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private void deleteBlob(final String blobUrl) {
        try {
            plainRestTemplate.exchange(URI.create(blobUrl), HttpMethod.DELETE, new HttpEntity<>(new HttpHeaders()), Void.class);
        } catch (final Exception e) {
            LOG.warn("Failed to delete temp blob {}: {}", blobUrl, e.getMessage());
        }
    }

    private String buildBlobUrl(final String sasUrl, final String blobName) {
        final int queryStart = sasUrl.indexOf('?');
        final String base = queryStart >= 0 ? sasUrl.substring(0, queryStart) : sasUrl;
        final String query = queryStart >= 0 ? sasUrl.substring(queryStart) : "";
        final String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalizedBase + "/" + storageContainer() + "/" + blobName + query;
    }

    private String storageContainer() {
        return Config.getString(STORAGE_CONTAINER_PROPERTY, DEFAULT_STORAGE_CONTAINER);
    }

    // -------------------------------------------------------------------------
    // Glossary helpers
    // -------------------------------------------------------------------------

    private CxTranslateGlossaryModel resolveGlossaryForLanguage(final CxAzureTranslatorServiceSettingsModel settings,
                                                                  final String targetLanguage) {
        if (CollectionUtils.isEmpty(settings.getGlossaries())) {
            return null;
        }
        final String targetLangPrefix = targetLanguage.contains("-")
                ? targetLanguage.substring(0, targetLanguage.indexOf('-')).toLowerCase(Locale.ROOT)
                : targetLanguage.toLowerCase(Locale.ROOT);
        return settings.getGlossaries().stream()
                .filter(g -> g.getTargetLanguage() != null
                        && g.getTargetLanguage().getIsocode().toLowerCase(Locale.ROOT).startsWith(targetLangPrefix))
                .findFirst()
                .orElse(null);
    }

    private String resolveAsyncGlossaryUrl(final CxTranslateGlossaryModel glossary,
                                            final String sasUrl, final String runId) {
        if (glossary == null) {
            return null;
        }
        if (StringUtils.isNotBlank(glossary.getExternalGlossaryRef())) {
            return glossary.getExternalGlossaryRef();
        }
        if (glossary.getMedia() == null) {
            return null;
        }
        final byte[] glossaryBytes = readGlossaryBytes(glossary);
        if (glossaryBytes == null) {
            return null;
        }
        final String ext = StringUtils.defaultIfBlank(
                StringUtils.substringAfterLast(glossary.getMedia().getRealFileName(), "."), "csv");
        final String blobName = "glossary/" + runId + "." + ext;
        final String blobUrl = buildBlobUrl(sasUrl, blobName);
        uploadBlob(blobUrl, glossaryBytes, resolveGlossaryMediaType(glossary).toString());
        return blobUrl;
    }

    private byte[] readGlossaryBytes(final CxTranslateGlossaryModel glossary) {
        try {
            return IOUtils.toByteArray(mediaService.getStreamFromMedia(glossary.getMedia()));
        } catch (final Exception e) {
            LOG.warn("Could not read glossary media {}: {}", glossary.getMedia().getCode(), e.getMessage());
            return null;
        }
    }

    private MediaType resolveGlossaryMediaType(final CxTranslateGlossaryModel glossary) {
        if ("text/tab-separated-values".equalsIgnoreCase(glossary.getMedia().getMime())) {
            return MediaType.valueOf("text/tab-separated-values");
        }
        return MediaType.valueOf("text/csv");
    }

    // -------------------------------------------------------------------------
    // Misc helpers
    // -------------------------------------------------------------------------

    /** Strips the UTF-8 BOM that Azure occasionally prepends to translated documents. */
    private static String stripBom(final String s) {
        return s != null && s.startsWith("﻿") ? s.substring(1) : s;
    }

    /**
     * Extracts the inner {@code <body>} content when Azure wraps a translated HTML document
     * in a full HTML page. Returns the input unchanged if no {@code <body>} tag is found.
     */
    private static String unwrapHtmlBody(final String html) {
        if (html == null) {
            return null;
        }
        final Matcher m = BODY_PATTERN.matcher(html);
        return m.find() ? m.group(1).trim() : html;
    }

    /** Minimal JSON field extractor — avoids pulling in Jackson just for status polling. */
    private static String extractJsonField(final String json, final String field) {
        if (json == null) {
            return null;
        }
        final String key = "\"" + field + "\"";
        final int keyIdx = json.indexOf(key);
        if (keyIdx < 0) {
            return null;
        }
        final int colonIdx = json.indexOf(':', keyIdx);
        if (colonIdx < 0) {
            return null;
        }
        final int valueStart = json.indexOf('"', colonIdx) + 1;
        final int valueEnd = json.indexOf('"', valueStart);
        if (valueStart <= 0 || valueEnd < 0) {
            return null;
        }
        return json.substring(valueStart, valueEnd);
    }

    /**
     * Wraps bytes as a named multipart part with an explicit Content-Type header.
     */
    private HttpEntity<ByteArrayResource> partEntity(final byte[] bytes, final String filename,
                                                      final MediaType contentType) {
        final ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        final HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(contentType);
        return new HttpEntity<>(resource, partHeaders);
    }

    public void setMediaService(final MediaService mediaService) {
        this.mediaService = mediaService;
    }
}
