
package com.sap.cx.translations.generated.dto.translation.deepl.request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "text",
    "source_lang",
    "target_lang",
    "context",
    "show_billed_characters",
    "split_sentences",
    "preserve_formatting",
    "formality",
    "model_type",
    "glossary_id",
    "tag_handling",
    "outline_detection",
    "non_splitting_tags",
    "splitting_tags",
    "ignore_tags"
})
public class CxTranslationsDeepLRequest implements Serializable
{

    /**
     * Text to be translated. Only UTF-8-encoded plain text is supported. The parameter may be specified
     * multiple times and translations are returned in the same order as they are requested. Each of the
     * parameter values may contain multiple sentences.
     * (Required)
     *
     */
    @JsonProperty("text")
    @JsonPropertyDescription("Text to be translated. Only UTF-8-encoded plain text is supported. The parameter may be specified\nmultiple times and translations are returned in the same order as they are requested. Each of the\nparameter values may contain multiple sentences.")
    private List<String> text = new ArrayList<String>();
    /**
     * Language of the text to be translated. Options currently available:
     *  * `AR` - Arabic [1]
     *  * `BG` - Bulgarian
     *  * `CS` - Czech
     *  * `DA` - Danish
     *  * `DE` - German
     *  * `EL` - Greek
     *  * `EN` - English
     *  * `ES` - Spanish
     *  * `ET` - Estonian
     *  * `FI` - Finnish
     *  * `FR` - French
     *  * `HU` - Hungarian
     *  * `ID` - Indonesian
     *  * `IT` - Italian
     *  * `JA` - Japanese
     *  * `KO` - Korean
     *  * `LT` - Lithuanian
     *  * `LV` - Latvian
     *  * `NB` - Norwegian (Bokmål)
     *  * `NL` - Dutch
     *  * `PL` - Polish
     *  * `PT` - Portuguese (all Portuguese varieties mixed)
     *  * `RO` - Romanian
     *  * `RU` - Russian
     *  * `SK` - Slovak
     *  * `SL` - Slovenian
     *  * `SV` - Swedish
     *  * `TR` - Turkish
     *  * `UK` - Ukrainian
     *  * `ZH` - Chinese
     *
     * If this parameter is omitted, the API will attempt to detect the language of the text and translate it.
     *
     * [1] Please note that Arabic has not yet been added to the `/languages` endpoint because it does not
     * yet support document translation; only text translation is supported for Arabic at this time. When
     * document translation support is added for Arabic, we will a) remove this note and b) add Arabic to
     * the `/languages` endpoint.
     *
     */
    @JsonProperty("source_lang")
    @JsonPropertyDescription("Language of the text to be translated. Options currently available:\n * `AR` - Arabic [1]\n * `BG` - Bulgarian\n * `CS` - Czech\n * `DA` - Danish\n * `DE` - German\n * `EL` - Greek\n * `EN` - English\n * `ES` - Spanish\n * `ET` - Estonian\n * `FI` - Finnish\n * `FR` - French\n * `HU` - Hungarian\n * `ID` - Indonesian\n * `IT` - Italian\n * `JA` - Japanese\n * `KO` - Korean\n * `LT` - Lithuanian\n * `LV` - Latvian\n * `NB` - Norwegian (Bokm\u00e5l)\n * `NL` - Dutch\n * `PL` - Polish\n * `PT` - Portuguese (all Portuguese varieties mixed)\n * `RO` - Romanian\n * `RU` - Russian\n * `SK` - Slovak\n * `SL` - Slovenian\n * `SV` - Swedish\n * `TR` - Turkish\n * `UK` - Ukrainian\n * `ZH` - Chinese\n\nIf this parameter is omitted, the API will attempt to detect the language of the text and translate it.\n\n[1] Please note that Arabic has not yet been added to the `/languages` endpoint because it does not \nyet support document translation; only text translation is supported for Arabic at this time. When \ndocument translation support is added for Arabic, we will a) remove this note and b) add Arabic to \nthe `/languages` endpoint.")
    private SourceLanguageText sourceLang;
    /**
     * The language into which the text should be translated. Options currently available:
     *  * `AR` - Arabic [1]
     *  * `BG` - Bulgarian
     *  * `CS` - Czech
     *  * `DA` - Danish
     *  * `DE` - German
     *  * `EL` - Greek
     *  * `EN` - English (unspecified variant for backward compatibility; please select `EN-GB` or `EN-US` instead)
     *  * `EN-GB` - English (British)
     *  * `EN-US` - English (American)
     *  * `ES` - Spanish
     *  * `ET` - Estonian
     *  * `FI` - Finnish
     *  * `FR` - French
     *  * `HU` - Hungarian
     *  * `ID` - Indonesian
     *  * `IT` - Italian
     *  * `JA` - Japanese
     *  * `KO` - Korean
     *  * `LT` - Lithuanian
     *  * `LV` - Latvian
     *  * `NB` - Norwegian (Bokmål)
     *  * `NL` - Dutch
     *  * `PL` - Polish
     *  * `PT` - Portuguese (unspecified variant for backward compatibility; please select `PT-BR` or `PT-PT` instead)
     *  * `PT-BR` - Portuguese (Brazilian)
     *  * `PT-PT` - Portuguese (all Portuguese variants excluding Brazilian Portuguese)
     *  * `RO` - Romanian
     *  * `RU` - Russian
     *  * `SK` - Slovak
     *  * `SL` - Slovenian
     *  * `SV` - Swedish
     *  * `TR` - Turkish
     *  * `UK` - Ukrainian
     *  * `ZH` - Chinese (unspecified variant for backward compatibility; please select `ZH-HANS` or `ZH-HANT` instead)
     *  * `ZH-HANS` - Chinese (simplified)
     *  * `ZH-HANT` - Chinese (traditional)
     *
     *  [1] Please note that Arabic and traditional Chinese have not yet been added to the /languages
     *  endpoint because these languages do not yet support document translation; only text translation
     *  is supported for Arabic and traditional Chinese at this time. When document translation support
     *  is added for these languages, we will a) remove this note and b) add the languages to the /languages
     *  endpoint where appropriate.
     * (Required)
     *
     */
    @JsonProperty("target_lang")
    @JsonPropertyDescription("The language into which the text should be translated. Options currently available:\n * `AR` - Arabic [1]\n * `BG` - Bulgarian\n * `CS` - Czech\n * `DA` - Danish\n * `DE` - German\n * `EL` - Greek\n * `EN` - English (unspecified variant for backward compatibility; please select `EN-GB` or `EN-US` instead)\n * `EN-GB` - English (British)\n * `EN-US` - English (American)\n * `ES` - Spanish\n * `ET` - Estonian\n * `FI` - Finnish\n * `FR` - French\n * `HU` - Hungarian\n * `ID` - Indonesian\n * `IT` - Italian\n * `JA` - Japanese\n * `KO` - Korean\n * `LT` - Lithuanian\n * `LV` - Latvian\n * `NB` - Norwegian (Bokm\u00e5l)\n * `NL` - Dutch\n * `PL` - Polish\n * `PT` - Portuguese (unspecified variant for backward compatibility; please select `PT-BR` or `PT-PT` instead)\n * `PT-BR` - Portuguese (Brazilian)\n * `PT-PT` - Portuguese (all Portuguese variants excluding Brazilian Portuguese)\n * `RO` - Romanian\n * `RU` - Russian\n * `SK` - Slovak\n * `SL` - Slovenian\n * `SV` - Swedish\n * `TR` - Turkish\n * `UK` - Ukrainian\n * `ZH` - Chinese (unspecified variant for backward compatibility; please select `ZH-HANS` or `ZH-HANT` instead)\n * `ZH-HANS` - Chinese (simplified)\n * `ZH-HANT` - Chinese (traditional)\n\n [1] Please note that Arabic and traditional Chinese have not yet been added to the /languages \n endpoint because these languages do not yet support document translation; only text translation \n is supported for Arabic and traditional Chinese at this time. When document translation support \n is added for these languages, we will a) remove this note and b) add the languages to the /languages \n endpoint where appropriate.")
    private TargetLanguageText targetLang;
    /**
     * The `context` parameter makes it possible to include additional context that can influence a translation but is not translated itself.
     * This additional context can potentially improve translation quality when translating short, low-context source texts such
     * as product names on an e-commerce website, article headlines on a news website, or UI elements.
     *
     *
     * For example...
     *   - When translating a product name, you might pass the product description as context.
     *   - When translating a news article headline, you might pass the first few sentences or a summary of the article as context.
     *
     *
     * For best results, we recommend sending a few complete sentences of context in the same language as the source text.
     * There is no size limit for the `context` parameter itself, but the request body size limit of 128 KiB still applies to
     * all text translation requests.
     *
     *
     * If you send a request with multiple `text` parameters, the `context` parameter will be applied to each one.
     *
     *
     * Characters included in the `context` parameter will not be counted toward billing (i.e. there is no additional
     * cost for using the `context` parameter, and only characters sent in the text parameter(s) will be counted toward
     * billing for text translation even when the `context` parameter is included in a request).
     *
     */
    @JsonProperty("context")
    @JsonPropertyDescription("The `context` parameter makes it possible to include additional context that can influence a translation but is not translated itself. \nThis additional context can potentially improve translation quality when translating short, low-context source texts such \nas product names on an e-commerce website, article headlines on a news website, or UI elements.\n\n\nFor example...\n  - When translating a product name, you might pass the product description as context. \n  - When translating a news article headline, you might pass the first few sentences or a summary of the article as context.\n\n\nFor best results, we recommend sending a few complete sentences of context in the same language as the source text. \nThere is no size limit for the `context` parameter itself, but the request body size limit of 128 KiB still applies to \nall text translation requests. \n\n\nIf you send a request with multiple `text` parameters, the `context` parameter will be applied to each one. \n\n\nCharacters included in the `context` parameter will not be counted toward billing (i.e. there is no additional \ncost for using the `context` parameter, and only characters sent in the text parameter(s) will be counted toward \nbilling for text translation even when the `context` parameter is included in a request).")
    private String context;
    /**
     * When true, the response will include the billed_characters parameter, giving the
     * number of characters from the request that will be counted by DeepL for billing purposes.
     *
     */
    @JsonProperty("show_billed_characters")
    @JsonPropertyDescription("When true, the response will include the billed_characters parameter, giving the \nnumber of characters from the request that will be counted by DeepL for billing purposes.")
    private Boolean showBilledCharacters;
    /**
     * Sets whether the translation engine should first split the input into sentences. For text translations where
     * `tag_handling` is not set to `html`, the default value is `1`, meaning the engine splits on punctuation and on newlines.
     *
     * For text translations where `tag_handling=html`, the default value is `nonewlines`, meaning the engine splits on punctuation only, ignoring newlines.
     *
     * The use of `nonewlines` as the default value for text translations where `tag_handling=html` is new behavior that was implemented in November 2022,
     * when HTML handling was moved out of beta.
     *
     * Possible values are:
     *
     *  * `0` - no splitting at all, whole input is treated as one sentence
     *  * `1` (default when `tag_handling` is not set to `html`) - splits on punctuation and on newlines
     *  * `nonewlines` (default when `tag_handling=html`) - splits on punctuation only, ignoring newlines
     *
     * For applications that send one sentence per text parameter, we recommend setting `split_sentences` to `0`, in order to prevent the engine from splitting the sentence unintentionally.
     *
     *
     * Please note that newlines will split sentences when `split_sentences=1`. We recommend cleaning files so they don't contain breaking sentences or setting the parameter `split_sentences` to `nonewlines`.
     *
     */
    @JsonProperty("split_sentences")
    @JsonPropertyDescription("Sets whether the translation engine should first split the input into sentences. For text translations where \n`tag_handling` is not set to `html`, the default value is `1`, meaning the engine splits on punctuation and on newlines.\n\nFor text translations where `tag_handling=html`, the default value is `nonewlines`, meaning the engine splits on punctuation only, ignoring newlines.  \n\nThe use of `nonewlines` as the default value for text translations where `tag_handling=html` is new behavior that was implemented in November 2022, \nwhen HTML handling was moved out of beta. \n\nPossible values are: \n\n * `0` - no splitting at all, whole input is treated as one sentence\n * `1` (default when `tag_handling` is not set to `html`) - splits on punctuation and on newlines\n * `nonewlines` (default when `tag_handling=html`) - splits on punctuation only, ignoring newlines\n\nFor applications that send one sentence per text parameter, we recommend setting `split_sentences` to `0`, in order to prevent the engine from splitting the sentence unintentionally.\n\n\nPlease note that newlines will split sentences when `split_sentences=1`. We recommend cleaning files so they don't contain breaking sentences or setting the parameter `split_sentences` to `nonewlines`.")
    private SplitSentencesOption splitSentences = SplitSentencesOption.fromValue("1");
    /**
     * Sets whether the translation engine should respect the original formatting, even if it would usually correct some aspects.
     *
     * The formatting aspects affected by this setting include:
     *  * Punctuation at the beginning and end of the sentence
     *  * Upper/lower case at the beginning of the sentence
     *
     */
    @JsonProperty("preserve_formatting")
    @JsonPropertyDescription("Sets whether the translation engine should respect the original formatting, even if it would usually correct some aspects.\n\nThe formatting aspects affected by this setting include:\n * Punctuation at the beginning and end of the sentence\n * Upper/lower case at the beginning of the sentence")
    private Boolean preserveFormatting = false;
    /**
     * Sets whether the translated text should lean towards formal or informal language.
     * This feature currently only works for target languages
     * `DE` (German),
     * `FR` (French),
     * `IT` (Italian),
     * `ES` (Spanish),
     * `NL` (Dutch),
     * `PL` (Polish),
     * `PT-BR` and `PT-PT` (Portuguese),
     * `JA` (Japanese),
     * and `RU` (Russian).
     * Learn more about the plain/polite feature for Japanese [here](https://support.deepl.com/hc/en-us/articles/6306700061852-About-the-plain-polite-feature-in-Japanese).
     * Setting this parameter with a target language that does not support formality will fail,
     * unless one of the `prefer_...` options are used.
     * Possible options are:
     *   * `default` (default)
     *   * `more` - for a more formal language
     *   * `less` - for a more informal language
     *   * `prefer_more` - for a more formal language if available, otherwise fallback to default formality
     *   * `prefer_less` - for a more informal language if available, otherwise fallback to default formality
     *
     */
    @JsonProperty("formality")
    @JsonPropertyDescription("Sets whether the translated text should lean towards formal or informal language.\nThis feature currently only works for target languages\n`DE` (German),\n`FR` (French),\n`IT` (Italian),\n`ES` (Spanish),\n`NL` (Dutch),\n`PL` (Polish),\n`PT-BR` and `PT-PT` (Portuguese),\n`JA` (Japanese),\nand `RU` (Russian).\nLearn more about the plain/polite feature for Japanese [here](https://support.deepl.com/hc/en-us/articles/6306700061852-About-the-plain-polite-feature-in-Japanese).\nSetting this parameter with a target language that does not support formality will fail,\nunless one of the `prefer_...` options are used.\nPossible options are:\n  * `default` (default)\n  * `more` - for a more formal language\n  * `less` - for a more informal language\n  * `prefer_more` - for a more formal language if available, otherwise fallback to default formality\n  * `prefer_less` - for a more informal language if available, otherwise fallback to default formality")
    private Formality formality = Formality.fromValue("default");
    /**
     * Specifies which DeepL model should be used for translation.
     *
     * Possible values are:
     * * `latency_optimized` (default) - uses lower latency “classic” translation models, which support all language pairs;
     *   default value
     * * `quality_optimized` - uses higher latency, improved quality “next-gen” translation models, which support only a
     *   subset of language pairs; if a language pair that is not supported by next-gen models is included in the
     *   request, it will fail. Consider using prefer_quality_optimized instead.
     * * `prefer_quality_optimized` - prioritizes use of higher latency, improved quality “next-gen” translation models,
     *   which support only a subset of DeepL languages; if a request includes a language pair not supported by
     *   next-gen models, the request will fall back to latency_optimized classic models.
     *
     */
    @JsonProperty("model_type")
    @JsonPropertyDescription("Specifies which DeepL model should be used for translation.\n\nPossible values are:\n* `latency_optimized` (default) - uses lower latency \u201cclassic\u201d translation models, which support all language pairs;\n  default value\n* `quality_optimized` - uses higher latency, improved quality \u201cnext-gen\u201d translation models, which support only a\n  subset of language pairs; if a language pair that is not supported by next-gen models is included in the\n  request, it will fail. Consider using prefer_quality_optimized instead.\n* `prefer_quality_optimized` - prioritizes use of higher latency, improved quality \u201cnext-gen\u201d translation models,\n  which support only a subset of DeepL languages; if a request includes a language pair not supported by\n  next-gen models, the request will fall back to latency_optimized classic models.")
    private ModelType modelType;
    @JsonProperty("glossary_id")
    private Object glossaryId;
    /**
     * Sets which kind of tags should be handled. Options currently available:
     *  * `xml`: Enable XML tag handling; see [XML Handling](https://www.deepl.com/docs-api/xml).
     *  * `html`: Enable HTML tag handling; see [HTML Handling](https://www.deepl.com/docs-api/html).
     *
     */
    @JsonProperty("tag_handling")
    @JsonPropertyDescription("Sets which kind of tags should be handled. Options currently available:\n * `xml`: Enable XML tag handling; see [XML Handling](https://www.deepl.com/docs-api/xml).\n * `html`: Enable HTML tag handling; see [HTML Handling](https://www.deepl.com/docs-api/html).")
    private TagHandlingOption tagHandling;
    /**
     * The automatic detection of the XML structure won't yield best results in all XML files. You can disable this automatic mechanism altogether by setting the `outline_detection` parameter to `false` and selecting the tags that should be considered structure tags. This will split sentences using the `splitting_tags` parameter.
     *
     *
     * In the example below, we achieve the same results as the automatic engine by disabling automatic detection with `outline_detection=0` and setting the parameters manually to `tag_handling=xml`, `split_sentences=nonewlines`,  and `splitting_tags=par,title`.
     *  * Example request:
     *    ```
     *    <document>
     *      <meta>
     *        <title>A document's title</title>
     *      </meta>
     *      <content>
     *        <par>This is the first sentence. Followed by a second one.</par>
     *        <par>This is the third sentence.</par>
     *      </content>
     *    </document>
     *    ```
     *  * Example response:
     *    ```
     *    <document>
     *      <meta>
     *        <title>Der Titel eines Dokuments</title>
     *      </meta>
     *      <content>
     *        <par>Das ist der erste Satz. Gefolgt von einem zweiten.</par>
     *        <par>Dies ist der dritte Satz.</par>
     *      </content>
     *    </document>
     *    ```
     * While this approach is slightly more complicated, it allows for greater control over the structure of the translation output.
     *
     */
    @JsonProperty("outline_detection")
    @JsonPropertyDescription("The automatic detection of the XML structure won't yield best results in all XML files. You can disable this automatic mechanism altogether by setting the `outline_detection` parameter to `false` and selecting the tags that should be considered structure tags. This will split sentences using the `splitting_tags` parameter.\n\n\nIn the example below, we achieve the same results as the automatic engine by disabling automatic detection with `outline_detection=0` and setting the parameters manually to `tag_handling=xml`, `split_sentences=nonewlines`,  and `splitting_tags=par,title`.\n * Example request:\n   ```\n   <document>\n     <meta>\n       <title>A document's title</title>\n     </meta>\n     <content>\n       <par>This is the first sentence. Followed by a second one.</par>\n       <par>This is the third sentence.</par>\n     </content>\n   </document>\n   ```\n * Example response:\n   ```\n   <document>\n     <meta>\n       <title>Der Titel eines Dokuments</title>\n     </meta>\n     <content>\n       <par>Das ist der erste Satz. Gefolgt von einem zweiten.</par>\n       <par>Dies ist der dritte Satz.</par>\n     </content>\n   </document>\n   ```\nWhile this approach is slightly more complicated, it allows for greater control over the structure of the translation output.")
    private Boolean outlineDetection = true;
    /**
     * List of XML tags which never split sentences.
     *
     *
     * For some XML files, finding tags with textual content and splitting sentences using those tags won't yield the best results. The following example shows the engine splitting sentences on `par` tags and proceeding to translate the parts separately, resulting in an incorrect translation:
     *  * Example request:
     * ```
     * <par>The firm said it had been </par><par> conducting an internal investigation.</par>
     * ```
     *  * Example response:
     * ```
     * <par>Die Firma sagte, es sei eine gute Idee gewesen.</par><par> Durchführung einer internen Untersuchung.</par>
     * ```
     *
     *
     * As this can lead to bad translations, this type of structure should either be avoided, or the `non_splitting_tags` parameter should be set. The following example shows the same call, with the parameter set to `par`:
     *  * Example request:
     * ```
     * <par>The firm said it had been </par><par> conducting an internal investigation.</par>
     * ```
     *  * Example response:
     * ```
     * <par>Die Firma sagte, dass sie</par><par> eine interne Untersuchung durchgeführt</par><par> habe</par><par>.</par>
     * ```
     *
     *
     * This time, the sentence is translated as a whole. The XML tags are now considered markup and copied into the translated sentence. As the translation of the words "had been" has moved to another position in the German sentence, the two par tags are duplicated (which is expected here).
     *
     */
    @JsonProperty("non_splitting_tags")
    @JsonPropertyDescription("List of XML tags which never split sentences.\n\n\nFor some XML files, finding tags with textual content and splitting sentences using those tags won't yield the best results. The following example shows the engine splitting sentences on `par` tags and proceeding to translate the parts separately, resulting in an incorrect translation:\n * Example request:\n```\n<par>The firm said it had been </par><par> conducting an internal investigation.</par>\n```\n * Example response:\n```\n<par>Die Firma sagte, es sei eine gute Idee gewesen.</par><par> Durchf\u00fchrung einer internen Untersuchung.</par>\n```\n\n\nAs this can lead to bad translations, this type of structure should either be avoided, or the `non_splitting_tags` parameter should be set. The following example shows the same call, with the parameter set to `par`:\n * Example request:\n```\n<par>The firm said it had been </par><par> conducting an internal investigation.</par>\n```\n * Example response:\n```\n<par>Die Firma sagte, dass sie</par><par> eine interne Untersuchung durchgef\u00fchrt</par><par> habe</par><par>.</par>\n```\n\n\nThis time, the sentence is translated as a whole. The XML tags are now considered markup and copied into the translated sentence. As the translation of the words \"had been\" has moved to another position in the German sentence, the two par tags are duplicated (which is expected here).")
    private Object nonSplittingTags;
    /**
     * List of XML tags which always cause splits.
     *
     *
     * See the example in the `outline_detection` parameter's description.
     *
     */
    @JsonProperty("splitting_tags")
    @JsonPropertyDescription("List of XML tags which always cause splits.\n\n\nSee the example in the `outline_detection` parameter's description.")
    private Object splittingTags;
    /**
     * List of XML tags that indicate text not to be translated.
     *
     *
     * Use this parameter to ensure that elements in the original text are not altered in the translation (e.g., trademarks, product names) and insert tags into your original text. In the following example, the `ignore_tags` parameter is set to `keep`:
     *  * Example request:
     *    ```
     *    Please open the page <keep>Settings</keep> to configure your system.
     *    ```
     *  * Example response:
     *    ```
     *    Bitte öffnen Sie die Seite <keep>Settings</keep> um Ihr System zu konfigurieren.
     *    ```
     *
     */
    @JsonProperty("ignore_tags")
    @JsonPropertyDescription("List of XML tags that indicate text not to be translated.\n\n\nUse this parameter to ensure that elements in the original text are not altered in the translation (e.g., trademarks, product names) and insert tags into your original text. In the following example, the `ignore_tags` parameter is set to `keep`:\n * Example request:\n   ```\n   Please open the page <keep>Settings</keep> to configure your system.\n   ```\n * Example response:\n   ```\n   Bitte \u00f6ffnen Sie die Seite <keep>Settings</keep> um Ihr System zu konfigurieren.\n   ```")
    private Object ignoreTags;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
    private final static long serialVersionUID = 8094174172888222801L;

    /**
     * Text to be translated. Only UTF-8-encoded plain text is supported. The parameter may be specified
     * multiple times and translations are returned in the same order as they are requested. Each of the
     * parameter values may contain multiple sentences.
     * (Required)
     *
     */
    @JsonProperty("text")
    public List<String> getText() {
        return text;
    }

    /**
     * Text to be translated. Only UTF-8-encoded plain text is supported. The parameter may be specified
     * multiple times and translations are returned in the same order as they are requested. Each of the
     * parameter values may contain multiple sentences.
     * (Required)
     *
     */
    @JsonProperty("text")
    public void setText(List<String> text) {
        this.text = text;
    }

    /**
     * Language of the text to be translated. Options currently available:
     *  * `AR` - Arabic [1]
     *  * `BG` - Bulgarian
     *  * `CS` - Czech
     *  * `DA` - Danish
     *  * `DE` - German
     *  * `EL` - Greek
     *  * `EN` - English
     *  * `ES` - Spanish
     *  * `ET` - Estonian
     *  * `FI` - Finnish
     *  * `FR` - French
     *  * `HU` - Hungarian
     *  * `ID` - Indonesian
     *  * `IT` - Italian
     *  * `JA` - Japanese
     *  * `KO` - Korean
     *  * `LT` - Lithuanian
     *  * `LV` - Latvian
     *  * `NB` - Norwegian (Bokmål)
     *  * `NL` - Dutch
     *  * `PL` - Polish
     *  * `PT` - Portuguese (all Portuguese varieties mixed)
     *  * `RO` - Romanian
     *  * `RU` - Russian
     *  * `SK` - Slovak
     *  * `SL` - Slovenian
     *  * `SV` - Swedish
     *  * `TR` - Turkish
     *  * `UK` - Ukrainian
     *  * `ZH` - Chinese
     *
     * If this parameter is omitted, the API will attempt to detect the language of the text and translate it.
     *
     * [1] Please note that Arabic has not yet been added to the `/languages` endpoint because it does not
     * yet support document translation; only text translation is supported for Arabic at this time. When
     * document translation support is added for Arabic, we will a) remove this note and b) add Arabic to
     * the `/languages` endpoint.
     *
     */
    @JsonProperty("source_lang")
    public SourceLanguageText getSourceLang() {
        return sourceLang;
    }

    /**
     * Language of the text to be translated. Options currently available:
     *  * `AR` - Arabic [1]
     *  * `BG` - Bulgarian
     *  * `CS` - Czech
     *  * `DA` - Danish
     *  * `DE` - German
     *  * `EL` - Greek
     *  * `EN` - English
     *  * `ES` - Spanish
     *  * `ET` - Estonian
     *  * `FI` - Finnish
     *  * `FR` - French
     *  * `HU` - Hungarian
     *  * `ID` - Indonesian
     *  * `IT` - Italian
     *  * `JA` - Japanese
     *  * `KO` - Korean
     *  * `LT` - Lithuanian
     *  * `LV` - Latvian
     *  * `NB` - Norwegian (Bokmål)
     *  * `NL` - Dutch
     *  * `PL` - Polish
     *  * `PT` - Portuguese (all Portuguese varieties mixed)
     *  * `RO` - Romanian
     *  * `RU` - Russian
     *  * `SK` - Slovak
     *  * `SL` - Slovenian
     *  * `SV` - Swedish
     *  * `TR` - Turkish
     *  * `UK` - Ukrainian
     *  * `ZH` - Chinese
     *
     * If this parameter is omitted, the API will attempt to detect the language of the text and translate it.
     *
     * [1] Please note that Arabic has not yet been added to the `/languages` endpoint because it does not
     * yet support document translation; only text translation is supported for Arabic at this time. When
     * document translation support is added for Arabic, we will a) remove this note and b) add Arabic to
     * the `/languages` endpoint.
     *
     */
    @JsonProperty("source_lang")
    public void setSourceLang(SourceLanguageText sourceLang) {
        this.sourceLang = sourceLang;
    }

    /**
     * The language into which the text should be translated. Options currently available:
     *  * `AR` - Arabic [1]
     *  * `BG` - Bulgarian
     *  * `CS` - Czech
     *  * `DA` - Danish
     *  * `DE` - German
     *  * `EL` - Greek
     *  * `EN` - English (unspecified variant for backward compatibility; please select `EN-GB` or `EN-US` instead)
     *  * `EN-GB` - English (British)
     *  * `EN-US` - English (American)
     *  * `ES` - Spanish
     *  * `ET` - Estonian
     *  * `FI` - Finnish
     *  * `FR` - French
     *  * `HU` - Hungarian
     *  * `ID` - Indonesian
     *  * `IT` - Italian
     *  * `JA` - Japanese
     *  * `KO` - Korean
     *  * `LT` - Lithuanian
     *  * `LV` - Latvian
     *  * `NB` - Norwegian (Bokmål)
     *  * `NL` - Dutch
     *  * `PL` - Polish
     *  * `PT` - Portuguese (unspecified variant for backward compatibility; please select `PT-BR` or `PT-PT` instead)
     *  * `PT-BR` - Portuguese (Brazilian)
     *  * `PT-PT` - Portuguese (all Portuguese variants excluding Brazilian Portuguese)
     *  * `RO` - Romanian
     *  * `RU` - Russian
     *  * `SK` - Slovak
     *  * `SL` - Slovenian
     *  * `SV` - Swedish
     *  * `TR` - Turkish
     *  * `UK` - Ukrainian
     *  * `ZH` - Chinese (unspecified variant for backward compatibility; please select `ZH-HANS` or `ZH-HANT` instead)
     *  * `ZH-HANS` - Chinese (simplified)
     *  * `ZH-HANT` - Chinese (traditional)
     *
     *  [1] Please note that Arabic and traditional Chinese have not yet been added to the /languages
     *  endpoint because these languages do not yet support document translation; only text translation
     *  is supported for Arabic and traditional Chinese at this time. When document translation support
     *  is added for these languages, we will a) remove this note and b) add the languages to the /languages
     *  endpoint where appropriate.
     * (Required)
     *
     */
    @JsonProperty("target_lang")
    public TargetLanguageText getTargetLang() {
        return targetLang;
    }

    /**
     * The language into which the text should be translated. Options currently available:
     *  * `AR` - Arabic [1]
     *  * `BG` - Bulgarian
     *  * `CS` - Czech
     *  * `DA` - Danish
     *  * `DE` - German
     *  * `EL` - Greek
     *  * `EN` - English (unspecified variant for backward compatibility; please select `EN-GB` or `EN-US` instead)
     *  * `EN-GB` - English (British)
     *  * `EN-US` - English (American)
     *  * `ES` - Spanish
     *  * `ET` - Estonian
     *  * `FI` - Finnish
     *  * `FR` - French
     *  * `HU` - Hungarian
     *  * `ID` - Indonesian
     *  * `IT` - Italian
     *  * `JA` - Japanese
     *  * `KO` - Korean
     *  * `LT` - Lithuanian
     *  * `LV` - Latvian
     *  * `NB` - Norwegian (Bokmål)
     *  * `NL` - Dutch
     *  * `PL` - Polish
     *  * `PT` - Portuguese (unspecified variant for backward compatibility; please select `PT-BR` or `PT-PT` instead)
     *  * `PT-BR` - Portuguese (Brazilian)
     *  * `PT-PT` - Portuguese (all Portuguese variants excluding Brazilian Portuguese)
     *  * `RO` - Romanian
     *  * `RU` - Russian
     *  * `SK` - Slovak
     *  * `SL` - Slovenian
     *  * `SV` - Swedish
     *  * `TR` - Turkish
     *  * `UK` - Ukrainian
     *  * `ZH` - Chinese (unspecified variant for backward compatibility; please select `ZH-HANS` or `ZH-HANT` instead)
     *  * `ZH-HANS` - Chinese (simplified)
     *  * `ZH-HANT` - Chinese (traditional)
     *
     *  [1] Please note that Arabic and traditional Chinese have not yet been added to the /languages
     *  endpoint because these languages do not yet support document translation; only text translation
     *  is supported for Arabic and traditional Chinese at this time. When document translation support
     *  is added for these languages, we will a) remove this note and b) add the languages to the /languages
     *  endpoint where appropriate.
     * (Required)
     *
     */
    @JsonProperty("target_lang")
    public void setTargetLang(TargetLanguageText targetLang) {
        this.targetLang = targetLang;
    }

    /**
     * The `context` parameter makes it possible to include additional context that can influence a translation but is not translated itself.
     * This additional context can potentially improve translation quality when translating short, low-context source texts such
     * as product names on an e-commerce website, article headlines on a news website, or UI elements.
     *
     *
     * For example...
     *   - When translating a product name, you might pass the product description as context.
     *   - When translating a news article headline, you might pass the first few sentences or a summary of the article as context.
     *
     *
     * For best results, we recommend sending a few complete sentences of context in the same language as the source text.
     * There is no size limit for the `context` parameter itself, but the request body size limit of 128 KiB still applies to
     * all text translation requests.
     *
     *
     * If you send a request with multiple `text` parameters, the `context` parameter will be applied to each one.
     *
     *
     * Characters included in the `context` parameter will not be counted toward billing (i.e. there is no additional
     * cost for using the `context` parameter, and only characters sent in the text parameter(s) will be counted toward
     * billing for text translation even when the `context` parameter is included in a request).
     *
     */
    @JsonProperty("context")
    public String getContext() {
        return context;
    }

    /**
     * The `context` parameter makes it possible to include additional context that can influence a translation but is not translated itself.
     * This additional context can potentially improve translation quality when translating short, low-context source texts such
     * as product names on an e-commerce website, article headlines on a news website, or UI elements.
     *
     *
     * For example...
     *   - When translating a product name, you might pass the product description as context.
     *   - When translating a news article headline, you might pass the first few sentences or a summary of the article as context.
     *
     *
     * For best results, we recommend sending a few complete sentences of context in the same language as the source text.
     * There is no size limit for the `context` parameter itself, but the request body size limit of 128 KiB still applies to
     * all text translation requests.
     *
     *
     * If you send a request with multiple `text` parameters, the `context` parameter will be applied to each one.
     *
     *
     * Characters included in the `context` parameter will not be counted toward billing (i.e. there is no additional
     * cost for using the `context` parameter, and only characters sent in the text parameter(s) will be counted toward
     * billing for text translation even when the `context` parameter is included in a request).
     *
     */
    @JsonProperty("context")
    public void setContext(String context) {
        this.context = context;
    }

    /**
     * When true, the response will include the billed_characters parameter, giving the
     * number of characters from the request that will be counted by DeepL for billing purposes.
     *
     */
    @JsonProperty("show_billed_characters")
    public Boolean getShowBilledCharacters() {
        return showBilledCharacters;
    }

    /**
     * When true, the response will include the billed_characters parameter, giving the
     * number of characters from the request that will be counted by DeepL for billing purposes.
     *
     */
    @JsonProperty("show_billed_characters")
    public void setShowBilledCharacters(Boolean showBilledCharacters) {
        this.showBilledCharacters = showBilledCharacters;
    }

    /**
     * Sets whether the translation engine should first split the input into sentences. For text translations where
     * `tag_handling` is not set to `html`, the default value is `1`, meaning the engine splits on punctuation and on newlines.
     *
     * For text translations where `tag_handling=html`, the default value is `nonewlines`, meaning the engine splits on punctuation only, ignoring newlines.
     *
     * The use of `nonewlines` as the default value for text translations where `tag_handling=html` is new behavior that was implemented in November 2022,
     * when HTML handling was moved out of beta.
     *
     * Possible values are:
     *
     *  * `0` - no splitting at all, whole input is treated as one sentence
     *  * `1` (default when `tag_handling` is not set to `html`) - splits on punctuation and on newlines
     *  * `nonewlines` (default when `tag_handling=html`) - splits on punctuation only, ignoring newlines
     *
     * For applications that send one sentence per text parameter, we recommend setting `split_sentences` to `0`, in order to prevent the engine from splitting the sentence unintentionally.
     *
     *
     * Please note that newlines will split sentences when `split_sentences=1`. We recommend cleaning files so they don't contain breaking sentences or setting the parameter `split_sentences` to `nonewlines`.
     *
     */
    @JsonProperty("split_sentences")
    public SplitSentencesOption getSplitSentences() {
        return splitSentences;
    }

    /**
     * Sets whether the translation engine should first split the input into sentences. For text translations where
     * `tag_handling` is not set to `html`, the default value is `1`, meaning the engine splits on punctuation and on newlines.
     *
     * For text translations where `tag_handling=html`, the default value is `nonewlines`, meaning the engine splits on punctuation only, ignoring newlines.
     *
     * The use of `nonewlines` as the default value for text translations where `tag_handling=html` is new behavior that was implemented in November 2022,
     * when HTML handling was moved out of beta.
     *
     * Possible values are:
     *
     *  * `0` - no splitting at all, whole input is treated as one sentence
     *  * `1` (default when `tag_handling` is not set to `html`) - splits on punctuation and on newlines
     *  * `nonewlines` (default when `tag_handling=html`) - splits on punctuation only, ignoring newlines
     *
     * For applications that send one sentence per text parameter, we recommend setting `split_sentences` to `0`, in order to prevent the engine from splitting the sentence unintentionally.
     *
     *
     * Please note that newlines will split sentences when `split_sentences=1`. We recommend cleaning files so they don't contain breaking sentences or setting the parameter `split_sentences` to `nonewlines`.
     *
     */
    @JsonProperty("split_sentences")
    public void setSplitSentences(SplitSentencesOption splitSentences) {
        this.splitSentences = splitSentences;
    }

    /**
     * Sets whether the translation engine should respect the original formatting, even if it would usually correct some aspects.
     *
     * The formatting aspects affected by this setting include:
     *  * Punctuation at the beginning and end of the sentence
     *  * Upper/lower case at the beginning of the sentence
     *
     */
    @JsonProperty("preserve_formatting")
    public Boolean getPreserveFormatting() {
        return preserveFormatting;
    }

    /**
     * Sets whether the translation engine should respect the original formatting, even if it would usually correct some aspects.
     *
     * The formatting aspects affected by this setting include:
     *  * Punctuation at the beginning and end of the sentence
     *  * Upper/lower case at the beginning of the sentence
     *
     */
    @JsonProperty("preserve_formatting")
    public void setPreserveFormatting(Boolean preserveFormatting) {
        this.preserveFormatting = preserveFormatting;
    }

    /**
     * Sets whether the translated text should lean towards formal or informal language.
     * This feature currently only works for target languages
     * `DE` (German),
     * `FR` (French),
     * `IT` (Italian),
     * `ES` (Spanish),
     * `NL` (Dutch),
     * `PL` (Polish),
     * `PT-BR` and `PT-PT` (Portuguese),
     * `JA` (Japanese),
     * and `RU` (Russian).
     * Learn more about the plain/polite feature for Japanese [here](https://support.deepl.com/hc/en-us/articles/6306700061852-About-the-plain-polite-feature-in-Japanese).
     * Setting this parameter with a target language that does not support formality will fail,
     * unless one of the `prefer_...` options are used.
     * Possible options are:
     *   * `default` (default)
     *   * `more` - for a more formal language
     *   * `less` - for a more informal language
     *   * `prefer_more` - for a more formal language if available, otherwise fallback to default formality
     *   * `prefer_less` - for a more informal language if available, otherwise fallback to default formality
     *
     */
    @JsonProperty("formality")
    public Formality getFormality() {
        return formality;
    }

    /**
     * Sets whether the translated text should lean towards formal or informal language.
     * This feature currently only works for target languages
     * `DE` (German),
     * `FR` (French),
     * `IT` (Italian),
     * `ES` (Spanish),
     * `NL` (Dutch),
     * `PL` (Polish),
     * `PT-BR` and `PT-PT` (Portuguese),
     * `JA` (Japanese),
     * and `RU` (Russian).
     * Learn more about the plain/polite feature for Japanese [here](https://support.deepl.com/hc/en-us/articles/6306700061852-About-the-plain-polite-feature-in-Japanese).
     * Setting this parameter with a target language that does not support formality will fail,
     * unless one of the `prefer_...` options are used.
     * Possible options are:
     *   * `default` (default)
     *   * `more` - for a more formal language
     *   * `less` - for a more informal language
     *   * `prefer_more` - for a more formal language if available, otherwise fallback to default formality
     *   * `prefer_less` - for a more informal language if available, otherwise fallback to default formality
     *
     */
    @JsonProperty("formality")
    public void setFormality(Formality formality) {
        this.formality = formality;
    }

    /**
     * Specifies which DeepL model should be used for translation.
     *
     * Possible values are:
     * * `latency_optimized` (default) - uses lower latency “classic” translation models, which support all language pairs;
     *   default value
     * * `quality_optimized` - uses higher latency, improved quality “next-gen” translation models, which support only a
     *   subset of language pairs; if a language pair that is not supported by next-gen models is included in the
     *   request, it will fail. Consider using prefer_quality_optimized instead.
     * * `prefer_quality_optimized` - prioritizes use of higher latency, improved quality “next-gen” translation models,
     *   which support only a subset of DeepL languages; if a request includes a language pair not supported by
     *   next-gen models, the request will fall back to latency_optimized classic models.
     *
     */
    @JsonProperty("model_type")
    public ModelType getModelType() {
        return modelType;
    }

    /**
     * Specifies which DeepL model should be used for translation.
     *
     * Possible values are:
     * * `latency_optimized` (default) - uses lower latency “classic” translation models, which support all language pairs;
     *   default value
     * * `quality_optimized` - uses higher latency, improved quality “next-gen” translation models, which support only a
     *   subset of language pairs; if a language pair that is not supported by next-gen models is included in the
     *   request, it will fail. Consider using prefer_quality_optimized instead.
     * * `prefer_quality_optimized` - prioritizes use of higher latency, improved quality “next-gen” translation models,
     *   which support only a subset of DeepL languages; if a request includes a language pair not supported by
     *   next-gen models, the request will fall back to latency_optimized classic models.
     *
     */
    @JsonProperty("model_type")
    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    @JsonProperty("glossary_id")
    public Object getGlossaryId() {
        return glossaryId;
    }

    @JsonProperty("glossary_id")
    public void setGlossaryId(Object glossaryId) {
        this.glossaryId = glossaryId;
    }

    /**
     * Sets which kind of tags should be handled. Options currently available:
     *  * `xml`: Enable XML tag handling; see [XML Handling](https://www.deepl.com/docs-api/xml).
     *  * `html`: Enable HTML tag handling; see [HTML Handling](https://www.deepl.com/docs-api/html).
     *
     */
    @JsonProperty("tag_handling")
    public TagHandlingOption getTagHandling() {
        return tagHandling;
    }

    /**
     * Sets which kind of tags should be handled. Options currently available:
     *  * `xml`: Enable XML tag handling; see [XML Handling](https://www.deepl.com/docs-api/xml).
     *  * `html`: Enable HTML tag handling; see [HTML Handling](https://www.deepl.com/docs-api/html).
     *
     */
    @JsonProperty("tag_handling")
    public void setTagHandling(TagHandlingOption tagHandling) {
        this.tagHandling = tagHandling;
    }

    /**
     * The automatic detection of the XML structure won't yield best results in all XML files. You can disable this automatic mechanism altogether by setting the `outline_detection` parameter to `false` and selecting the tags that should be considered structure tags. This will split sentences using the `splitting_tags` parameter.
     *
     *
     * In the example below, we achieve the same results as the automatic engine by disabling automatic detection with `outline_detection=0` and setting the parameters manually to `tag_handling=xml`, `split_sentences=nonewlines`,  and `splitting_tags=par,title`.
     *  * Example request:
     *    ```
     *    <document>
     *      <meta>
     *        <title>A document's title</title>
     *      </meta>
     *      <content>
     *        <par>This is the first sentence. Followed by a second one.</par>
     *        <par>This is the third sentence.</par>
     *      </content>
     *    </document>
     *    ```
     *  * Example response:
     *    ```
     *    <document>
     *      <meta>
     *        <title>Der Titel eines Dokuments</title>
     *      </meta>
     *      <content>
     *        <par>Das ist der erste Satz. Gefolgt von einem zweiten.</par>
     *        <par>Dies ist der dritte Satz.</par>
     *      </content>
     *    </document>
     *    ```
     * While this approach is slightly more complicated, it allows for greater control over the structure of the translation output.
     *
     */
    @JsonProperty("outline_detection")
    public Boolean getOutlineDetection() {
        return outlineDetection;
    }

    /**
     * The automatic detection of the XML structure won't yield best results in all XML files. You can disable this automatic mechanism altogether by setting the `outline_detection` parameter to `false` and selecting the tags that should be considered structure tags. This will split sentences using the `splitting_tags` parameter.
     *
     *
     * In the example below, we achieve the same results as the automatic engine by disabling automatic detection with `outline_detection=0` and setting the parameters manually to `tag_handling=xml`, `split_sentences=nonewlines`,  and `splitting_tags=par,title`.
     *  * Example request:
     *    ```
     *    <document>
     *      <meta>
     *        <title>A document's title</title>
     *      </meta>
     *      <content>
     *        <par>This is the first sentence. Followed by a second one.</par>
     *        <par>This is the third sentence.</par>
     *      </content>
     *    </document>
     *    ```
     *  * Example response:
     *    ```
     *    <document>
     *      <meta>
     *        <title>Der Titel eines Dokuments</title>
     *      </meta>
     *      <content>
     *        <par>Das ist der erste Satz. Gefolgt von einem zweiten.</par>
     *        <par>Dies ist der dritte Satz.</par>
     *      </content>
     *    </document>
     *    ```
     * While this approach is slightly more complicated, it allows for greater control over the structure of the translation output.
     *
     */
    @JsonProperty("outline_detection")
    public void setOutlineDetection(Boolean outlineDetection) {
        this.outlineDetection = outlineDetection;
    }

    /**
     * List of XML tags which never split sentences.
     *
     *
     * For some XML files, finding tags with textual content and splitting sentences using those tags won't yield the best results. The following example shows the engine splitting sentences on `par` tags and proceeding to translate the parts separately, resulting in an incorrect translation:
     *  * Example request:
     * ```
     * <par>The firm said it had been </par><par> conducting an internal investigation.</par>
     * ```
     *  * Example response:
     * ```
     * <par>Die Firma sagte, es sei eine gute Idee gewesen.</par><par> Durchführung einer internen Untersuchung.</par>
     * ```
     *
     *
     * As this can lead to bad translations, this type of structure should either be avoided, or the `non_splitting_tags` parameter should be set. The following example shows the same call, with the parameter set to `par`:
     *  * Example request:
     * ```
     * <par>The firm said it had been </par><par> conducting an internal investigation.</par>
     * ```
     *  * Example response:
     * ```
     * <par>Die Firma sagte, dass sie</par><par> eine interne Untersuchung durchgeführt</par><par> habe</par><par>.</par>
     * ```
     *
     *
     * This time, the sentence is translated as a whole. The XML tags are now considered markup and copied into the translated sentence. As the translation of the words "had been" has moved to another position in the German sentence, the two par tags are duplicated (which is expected here).
     *
     */
    @JsonProperty("non_splitting_tags")
    public Object getNonSplittingTags() {
        return nonSplittingTags;
    }

    /**
     * List of XML tags which never split sentences.
     *
     *
     * For some XML files, finding tags with textual content and splitting sentences using those tags won't yield the best results. The following example shows the engine splitting sentences on `par` tags and proceeding to translate the parts separately, resulting in an incorrect translation:
     *  * Example request:
     * ```
     * <par>The firm said it had been </par><par> conducting an internal investigation.</par>
     * ```
     *  * Example response:
     * ```
     * <par>Die Firma sagte, es sei eine gute Idee gewesen.</par><par> Durchführung einer internen Untersuchung.</par>
     * ```
     *
     *
     * As this can lead to bad translations, this type of structure should either be avoided, or the `non_splitting_tags` parameter should be set. The following example shows the same call, with the parameter set to `par`:
     *  * Example request:
     * ```
     * <par>The firm said it had been </par><par> conducting an internal investigation.</par>
     * ```
     *  * Example response:
     * ```
     * <par>Die Firma sagte, dass sie</par><par> eine interne Untersuchung durchgeführt</par><par> habe</par><par>.</par>
     * ```
     *
     *
     * This time, the sentence is translated as a whole. The XML tags are now considered markup and copied into the translated sentence. As the translation of the words "had been" has moved to another position in the German sentence, the two par tags are duplicated (which is expected here).
     *
     */
    @JsonProperty("non_splitting_tags")
    public void setNonSplittingTags(Object nonSplittingTags) {
        this.nonSplittingTags = nonSplittingTags;
    }

    /**
     * List of XML tags which always cause splits.
     *
     *
     * See the example in the `outline_detection` parameter's description.
     *
     */
    @JsonProperty("splitting_tags")
    public Object getSplittingTags() {
        return splittingTags;
    }

    /**
     * List of XML tags which always cause splits.
     *
     *
     * See the example in the `outline_detection` parameter's description.
     *
     */
    @JsonProperty("splitting_tags")
    public void setSplittingTags(Object splittingTags) {
        this.splittingTags = splittingTags;
    }

    /**
     * List of XML tags that indicate text not to be translated.
     *
     *
     * Use this parameter to ensure that elements in the original text are not altered in the translation (e.g., trademarks, product names) and insert tags into your original text. In the following example, the `ignore_tags` parameter is set to `keep`:
     *  * Example request:
     *    ```
     *    Please open the page <keep>Settings</keep> to configure your system.
     *    ```
     *  * Example response:
     *    ```
     *    Bitte öffnen Sie die Seite <keep>Settings</keep> um Ihr System zu konfigurieren.
     *    ```
     *
     */
    @JsonProperty("ignore_tags")
    public Object getIgnoreTags() {
        return ignoreTags;
    }

    /**
     * List of XML tags that indicate text not to be translated.
     *
     *
     * Use this parameter to ensure that elements in the original text are not altered in the translation (e.g., trademarks, product names) and insert tags into your original text. In the following example, the `ignore_tags` parameter is set to `keep`:
     *  * Example request:
     *    ```
     *    Please open the page <keep>Settings</keep> to configure your system.
     *    ```
     *  * Example response:
     *    ```
     *    Bitte öffnen Sie die Seite <keep>Settings</keep> um Ihr System zu konfigurieren.
     *    ```
     *
     */
    @JsonProperty("ignore_tags")
    public void setIgnoreTags(Object ignoreTags) {
        this.ignoreTags = ignoreTags;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CxTranslationsDeepLRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("text");
        sb.append('=');
        sb.append(((this.text == null)?"<null>":this.text));
        sb.append(',');
        sb.append("sourceLang");
        sb.append('=');
        sb.append(((this.sourceLang == null)?"<null>":this.sourceLang));
        sb.append(',');
        sb.append("targetLang");
        sb.append('=');
        sb.append(((this.targetLang == null)?"<null>":this.targetLang));
        sb.append(',');
        sb.append("context");
        sb.append('=');
        sb.append(((this.context == null)?"<null>":this.context));
        sb.append(',');
        sb.append("showBilledCharacters");
        sb.append('=');
        sb.append(((this.showBilledCharacters == null)?"<null>":this.showBilledCharacters));
        sb.append(',');
        sb.append("splitSentences");
        sb.append('=');
        sb.append(((this.splitSentences == null)?"<null>":this.splitSentences));
        sb.append(',');
        sb.append("preserveFormatting");
        sb.append('=');
        sb.append(((this.preserveFormatting == null)?"<null>":this.preserveFormatting));
        sb.append(',');
        sb.append("formality");
        sb.append('=');
        sb.append(((this.formality == null)?"<null>":this.formality));
        sb.append(',');
        sb.append("modelType");
        sb.append('=');
        sb.append(((this.modelType == null)?"<null>":this.modelType));
        sb.append(',');
        sb.append("glossaryId");
        sb.append('=');
        sb.append(((this.glossaryId == null)?"<null>":this.glossaryId));
        sb.append(',');
        sb.append("tagHandling");
        sb.append('=');
        sb.append(((this.tagHandling == null)?"<null>":this.tagHandling));
        sb.append(',');
        sb.append("outlineDetection");
        sb.append('=');
        sb.append(((this.outlineDetection == null)?"<null>":this.outlineDetection));
        sb.append(',');
        sb.append("nonSplittingTags");
        sb.append('=');
        sb.append(((this.nonSplittingTags == null)?"<null>":this.nonSplittingTags));
        sb.append(',');
        sb.append("splittingTags");
        sb.append('=');
        sb.append(((this.splittingTags == null)?"<null>":this.splittingTags));
        sb.append(',');
        sb.append("ignoreTags");
        sb.append('=');
        sb.append(((this.ignoreTags == null)?"<null>":this.ignoreTags));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.additionalProperties == null)?"<null>":this.additionalProperties));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.showBilledCharacters == null)? 0 :this.showBilledCharacters.hashCode()));
        result = ((result* 31)+((this.outlineDetection == null)? 0 :this.outlineDetection.hashCode()));
        result = ((result* 31)+((this.targetLang == null)? 0 :this.targetLang.hashCode()));
        result = ((result* 31)+((this.tagHandling == null)? 0 :this.tagHandling.hashCode()));
        result = ((result* 31)+((this.preserveFormatting == null)? 0 :this.preserveFormatting.hashCode()));
        result = ((result* 31)+((this.formality == null)? 0 :this.formality.hashCode()));
        result = ((result* 31)+((this.modelType == null)? 0 :this.modelType.hashCode()));
        result = ((result* 31)+((this.splittingTags == null)? 0 :this.splittingTags.hashCode()));
        result = ((result* 31)+((this.sourceLang == null)? 0 :this.sourceLang.hashCode()));
        result = ((result* 31)+((this.splitSentences == null)? 0 :this.splitSentences.hashCode()));
        result = ((result* 31)+((this.ignoreTags == null)? 0 :this.ignoreTags.hashCode()));
        result = ((result* 31)+((this.context == null)? 0 :this.context.hashCode()));
        result = ((result* 31)+((this.nonSplittingTags == null)? 0 :this.nonSplittingTags.hashCode()));
        result = ((result* 31)+((this.text == null)? 0 :this.text.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.glossaryId == null)? 0 :this.glossaryId.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CxTranslationsDeepLRequest) == false) {
            return false;
        }
        CxTranslationsDeepLRequest rhs = ((CxTranslationsDeepLRequest) other);
        return (((((((((((((((((this.showBilledCharacters == rhs.showBilledCharacters)||((this.showBilledCharacters!= null)&&this.showBilledCharacters.equals(rhs.showBilledCharacters)))&&((this.outlineDetection == rhs.outlineDetection)||((this.outlineDetection!= null)&&this.outlineDetection.equals(rhs.outlineDetection))))&&((this.targetLang == rhs.targetLang)||((this.targetLang!= null)&&this.targetLang.equals(rhs.targetLang))))&&((this.tagHandling == rhs.tagHandling)||((this.tagHandling!= null)&&this.tagHandling.equals(rhs.tagHandling))))&&((this.preserveFormatting == rhs.preserveFormatting)||((this.preserveFormatting!= null)&&this.preserveFormatting.equals(rhs.preserveFormatting))))&&((this.formality == rhs.formality)||((this.formality!= null)&&this.formality.equals(rhs.formality))))&&((this.modelType == rhs.modelType)||((this.modelType!= null)&&this.modelType.equals(rhs.modelType))))&&((this.splittingTags == rhs.splittingTags)||((this.splittingTags!= null)&&this.splittingTags.equals(rhs.splittingTags))))&&((this.sourceLang == rhs.sourceLang)||((this.sourceLang!= null)&&this.sourceLang.equals(rhs.sourceLang))))&&((this.splitSentences == rhs.splitSentences)||((this.splitSentences!= null)&&this.splitSentences.equals(rhs.splitSentences))))&&((this.ignoreTags == rhs.ignoreTags)||((this.ignoreTags!= null)&&this.ignoreTags.equals(rhs.ignoreTags))))&&((this.context == rhs.context)||((this.context!= null)&&this.context.equals(rhs.context))))&&((this.nonSplittingTags == rhs.nonSplittingTags)||((this.nonSplittingTags!= null)&&this.nonSplittingTags.equals(rhs.nonSplittingTags))))&&((this.text == rhs.text)||((this.text!= null)&&this.text.equals(rhs.text))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.glossaryId == rhs.glossaryId)||((this.glossaryId!= null)&&this.glossaryId.equals(rhs.glossaryId))));
    }


    /**
     * Sets whether the translated text should lean towards formal or informal language.
     * This feature currently only works for target languages
     * `DE` (German),
     * `FR` (French),
     * `IT` (Italian),
     * `ES` (Spanish),
     * `NL` (Dutch),
     * `PL` (Polish),
     * `PT-BR` and `PT-PT` (Portuguese),
     * `JA` (Japanese),
     * and `RU` (Russian).
     * Learn more about the plain/polite feature for Japanese [here](https://support.deepl.com/hc/en-us/articles/6306700061852-About-the-plain-polite-feature-in-Japanese).
     * Setting this parameter with a target language that does not support formality will fail,
     * unless one of the `prefer_...` options are used.
     * Possible options are:
     *   * `default` (default)
     *   * `more` - for a more formal language
     *   * `less` - for a more informal language
     *   * `prefer_more` - for a more formal language if available, otherwise fallback to default formality
     *   * `prefer_less` - for a more informal language if available, otherwise fallback to default formality
     *
     */
    public enum Formality {

        DEFAULT("default"),
        MORE("more"),
        LESS("less"),
        PREFER_MORE("prefer_more"),
        PREFER_LESS("prefer_less");
        private final String value;
        private final static Map<String, Formality> CONSTANTS = new HashMap<String, Formality>();

        static {
            for (Formality c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Formality(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static Formality fromValue(String value) {
            Formality constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * Specifies which DeepL model should be used for translation.
     *
     * Possible values are:
     * * `latency_optimized` (default) - uses lower latency “classic” translation models, which support all language pairs;
     *   default value
     * * `quality_optimized` - uses higher latency, improved quality “next-gen” translation models, which support only a
     *   subset of language pairs; if a language pair that is not supported by next-gen models is included in the
     *   request, it will fail. Consider using prefer_quality_optimized instead.
     * * `prefer_quality_optimized` - prioritizes use of higher latency, improved quality “next-gen” translation models,
     *   which support only a subset of DeepL languages; if a request includes a language pair not supported by
     *   next-gen models, the request will fall back to latency_optimized classic models.
     *
     */
    public enum ModelType {

        QUALITY_OPTIMIZED("quality_optimized"),
        PREFER_QUALITY_OPTIMIZED("prefer_quality_optimized"),
        LATENCY_OPTIMIZED("latency_optimized");
        private final String value;
        private final static Map<String, ModelType> CONSTANTS = new HashMap<String, ModelType>();

        static {
            for (ModelType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        ModelType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static ModelType fromValue(String value) {
            ModelType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * Language of the text to be translated. Options currently available:
     *  * `AR` - Arabic [1]
     *  * `BG` - Bulgarian
     *  * `CS` - Czech
     *  * `DA` - Danish
     *  * `DE` - German
     *  * `EL` - Greek
     *  * `EN` - English
     *  * `ES` - Spanish
     *  * `ET` - Estonian
     *  * `FI` - Finnish
     *  * `FR` - French
     *  * `HU` - Hungarian
     *  * `ID` - Indonesian
     *  * `IT` - Italian
     *  * `JA` - Japanese
     *  * `KO` - Korean
     *  * `LT` - Lithuanian
     *  * `LV` - Latvian
     *  * `NB` - Norwegian (Bokmål)
     *  * `NL` - Dutch
     *  * `PL` - Polish
     *  * `PT` - Portuguese (all Portuguese varieties mixed)
     *  * `RO` - Romanian
     *  * `RU` - Russian
     *  * `SK` - Slovak
     *  * `SL` - Slovenian
     *  * `SV` - Swedish
     *  * `TR` - Turkish
     *  * `UK` - Ukrainian
     *  * `ZH` - Chinese
     *
     * If this parameter is omitted, the API will attempt to detect the language of the text and translate it.
     *
     * [1] Please note that Arabic has not yet been added to the `/languages` endpoint because it does not
     * yet support document translation; only text translation is supported for Arabic at this time. When
     * document translation support is added for Arabic, we will a) remove this note and b) add Arabic to
     * the `/languages` endpoint.
     *
     */
    public enum SourceLanguageText {

        AR("AR"),
        BG("BG"),
        CS("CS"),
        DA("DA"),
        DE("DE"),
        EL("EL"),
        EN("EN"),
        ES("ES"),
        ET("ET"),
        FI("FI"),
        FR("FR"),
        HU("HU"),
        ID("ID"),
        IT("IT"),
        JA("JA"),
        KO("KO"),
        LT("LT"),
        LV("LV"),
        NB("NB"),
        NL("NL"),
        PL("PL"),
        PT("PT"),
        RO("RO"),
        RU("RU"),
        SK("SK"),
        SL("SL"),
        SV("SV"),
        TR("TR"),
        UK("UK"),
        ZH("ZH");
        private final String value;
        private final static Map<String, SourceLanguageText> CONSTANTS = new HashMap<String, SourceLanguageText>();

        static {
            for (SourceLanguageText c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        SourceLanguageText(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static SourceLanguageText fromValue(String value) {
            SourceLanguageText constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * Sets whether the translation engine should first split the input into sentences. For text translations where
     * `tag_handling` is not set to `html`, the default value is `1`, meaning the engine splits on punctuation and on newlines.
     *
     * For text translations where `tag_handling=html`, the default value is `nonewlines`, meaning the engine splits on punctuation only, ignoring newlines.
     *
     * The use of `nonewlines` as the default value for text translations where `tag_handling=html` is new behavior that was implemented in November 2022,
     * when HTML handling was moved out of beta.
     *
     * Possible values are:
     *
     *  * `0` - no splitting at all, whole input is treated as one sentence
     *  * `1` (default when `tag_handling` is not set to `html`) - splits on punctuation and on newlines
     *  * `nonewlines` (default when `tag_handling=html`) - splits on punctuation only, ignoring newlines
     *
     * For applications that send one sentence per text parameter, we recommend setting `split_sentences` to `0`, in order to prevent the engine from splitting the sentence unintentionally.
     *
     *
     * Please note that newlines will split sentences when `split_sentences=1`. We recommend cleaning files so they don't contain breaking sentences or setting the parameter `split_sentences` to `nonewlines`.
     *
     */
    public enum SplitSentencesOption {

        _0("0"),
        _1("1"),
        NONEWLINES("nonewlines");
        private final String value;
        private final static Map<String, SplitSentencesOption> CONSTANTS = new HashMap<String, SplitSentencesOption>();

        static {
            for (SplitSentencesOption c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        SplitSentencesOption(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static SplitSentencesOption fromValue(String value) {
            SplitSentencesOption constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * Sets which kind of tags should be handled. Options currently available:
     *  * `xml`: Enable XML tag handling; see [XML Handling](https://www.deepl.com/docs-api/xml).
     *  * `html`: Enable HTML tag handling; see [HTML Handling](https://www.deepl.com/docs-api/html).
     *
     */
    public enum TagHandlingOption {

        XML("xml"),
        HTML("html");
        private final String value;
        private final static Map<String, TagHandlingOption> CONSTANTS = new HashMap<String, TagHandlingOption>();

        static {
            for (TagHandlingOption c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        TagHandlingOption(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static TagHandlingOption fromValue(String value) {
            TagHandlingOption constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * The language into which the text should be translated. Options currently available:
     *  * `AR` - Arabic [1]
     *  * `BG` - Bulgarian
     *  * `CS` - Czech
     *  * `DA` - Danish
     *  * `DE` - German
     *  * `EL` - Greek
     *  * `EN` - English (unspecified variant for backward compatibility; please select `EN-GB` or `EN-US` instead)
     *  * `EN-GB` - English (British)
     *  * `EN-US` - English (American)
     *  * `ES` - Spanish
     *  * `ET` - Estonian
     *  * `FI` - Finnish
     *  * `FR` - French
     *  * `HU` - Hungarian
     *  * `ID` - Indonesian
     *  * `IT` - Italian
     *  * `JA` - Japanese
     *  * `KO` - Korean
     *  * `LT` - Lithuanian
     *  * `LV` - Latvian
     *  * `NB` - Norwegian (Bokmål)
     *  * `NL` - Dutch
     *  * `PL` - Polish
     *  * `PT` - Portuguese (unspecified variant for backward compatibility; please select `PT-BR` or `PT-PT` instead)
     *  * `PT-BR` - Portuguese (Brazilian)
     *  * `PT-PT` - Portuguese (all Portuguese variants excluding Brazilian Portuguese)
     *  * `RO` - Romanian
     *  * `RU` - Russian
     *  * `SK` - Slovak
     *  * `SL` - Slovenian
     *  * `SV` - Swedish
     *  * `TR` - Turkish
     *  * `UK` - Ukrainian
     *  * `ZH` - Chinese (unspecified variant for backward compatibility; please select `ZH-HANS` or `ZH-HANT` instead)
     *  * `ZH-HANS` - Chinese (simplified)
     *  * `ZH-HANT` - Chinese (traditional)
     *
     *  [1] Please note that Arabic and traditional Chinese have not yet been added to the /languages
     *  endpoint because these languages do not yet support document translation; only text translation
     *  is supported for Arabic and traditional Chinese at this time. When document translation support
     *  is added for these languages, we will a) remove this note and b) add the languages to the /languages
     *  endpoint where appropriate.
     *
     */
    public enum TargetLanguageText {

        AR("AR"),
        BG("BG"),
        CS("CS"),
        DA("DA"),
        DE("DE"),
        EL("EL"),
        EN_GB("EN-GB"),
        EN_US("EN-US"),
        ES("ES"),
        ET("ET"),
        FI("FI"),
        FR("FR"),
        HU("HU"),
        ID("ID"),
        IT("IT"),
        JA("JA"),
        KO("KO"),
        LT("LT"),
        LV("LV"),
        NB("NB"),
        NL("NL"),
        PL("PL"),
        PT_BR("PT-BR"),
        PT_PT("PT-PT"),
        RO("RO"),
        RU("RU"),
        SK("SK"),
        SL("SL"),
        SV("SV"),
        TR("TR"),
        UK("UK"),
        ZH("ZH"),
        ZH_HANS("ZH-HANS"),
        ZH_HANT("ZH-HANT");
        private final String value;
        private final static Map<String, TargetLanguageText> CONSTANTS = new HashMap<String, TargetLanguageText>();

        static {
            for (TargetLanguageText c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        TargetLanguageText(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static TargetLanguageText fromValue(String value) {
            TargetLanguageText constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
