/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.saptranslationhub;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper for building and parsing XLIFF 1.2 documents used by the SAP Translation Hub document translation API.
 */
public class CxSapTranslationHubXliffHelper {

    private CxSapTranslationHubXliffHelper() {
    }

    /**
     * Represents a single translation unit to include in the XLIFF request.
     */
    public record TransUnit(String id, String sourceText, String note) {
    }

    /**
     * Builds an XLIFF 1.2 document string from the given translation units.
     *
     * @param sourceLanguage BCP-47 source language tag (e.g. "en")
     * @param targetLanguage BCP-47 target language tag (e.g. "de-DE")
     * @param units          translation units to include
     * @return XLIFF XML string ready to POST
     */
    public static String buildXliff(final String sourceLanguage, final String targetLanguage,
                                    final List<TransUnit> units) {
        try {
            final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            doc.setXmlStandalone(true);

            final Element xliff = doc.createElement("xliff");
            xliff.setAttribute("version", "1.2");
            doc.appendChild(xliff);

            final Element file = doc.createElement("file");
            file.setAttribute("original", "messages");
            file.setAttribute("source-language", sourceLanguage);
            file.setAttribute("target-language", targetLanguage);
            file.setAttribute("datatype", "plaintext");
            xliff.appendChild(file);

            final Element body = doc.createElement("body");
            file.appendChild(body);

            for (final TransUnit unit : units) {
                final Element transUnit = doc.createElement("trans-unit");
                transUnit.setAttribute("id", unit.id());
                transUnit.setAttribute("xml:space", "preserve");
                body.appendChild(transUnit);

                final Element source = doc.createElement("source");
                source.setTextContent(unit.sourceText());
                transUnit.appendChild(source);

                if (unit.note() != null && !unit.note().isBlank()) {
                    final Element note = doc.createElement("note");
                    note.setTextContent(unit.note());
                    transUnit.appendChild(note);
                }
            }

            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            final StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to build XLIFF document", e);
        }
    }

    /**
     * Parses an XLIFF 1.2 response and extracts translated text for each trans-unit id.
     *
     * @param xliffResponse the XLIFF XML response string
     * @return map of trans-unit id to translated text
     */
    public static Map<String, String> parseTranslations(final String xliffResponse) {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Disable external entities for security
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            final Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xliffResponse)));

            final Map<String, String> result = new LinkedHashMap<>();
            final NodeList transUnits = doc.getElementsByTagName("trans-unit");
            for (int i = 0; i < transUnits.getLength(); i++) {
                final Element transUnit = (Element) transUnits.item(i);
                final String id = transUnit.getAttribute("id");
                final NodeList targets = transUnit.getElementsByTagName("target");
                if (targets.getLength() > 0) {
                    result.put(id, targets.item(0).getTextContent());
                }
            }
            return result;
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to parse XLIFF response", e);
        }
    }
}

