
package com.sap.cx.translations.generated.dto.translation.deepl.response;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "detected_source_language",
    "text",
    "billed_characters",
    "model_type_used"
})
public class Translation implements Serializable
{

    /**
     * The language detected in the source text. It reflects the value of the `source_lang` parameter, when specified.
     *
     */
    @JsonProperty("detected_source_language")
    @JsonPropertyDescription("The language detected in the source text. It reflects the value of the `source_lang` parameter, when specified.")
    private Object detectedSourceLanguage;
    /**
     * The translated text.
     *
     */
    @JsonProperty("text")
    @JsonPropertyDescription("The translated text.")
    private String text;
    /**
     * Number of characters counted by DeepL for billing purposes. Only present if the show_billed_characters parameter is set to true.
     *
     */
    @JsonProperty("billed_characters")
    @JsonPropertyDescription("Number of characters counted by DeepL for billing purposes. Only present if the show_billed_characters parameter is set to true.")
    private Integer billedCharacters;
    /**
     * Indicates the translation model used. Only present if model_type parameter is included in the request.
     *
     */
    @JsonProperty("model_type_used")
    @JsonPropertyDescription("Indicates the translation model used. Only present if model_type parameter is included in the request.")
    private String modelTypeUsed;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
    private final static long serialVersionUID = -8198013109114072703L;

    /**
     * The language detected in the source text. It reflects the value of the `source_lang` parameter, when specified.
     *
     */
    @JsonProperty("detected_source_language")
    public Object getDetectedSourceLanguage() {
        return detectedSourceLanguage;
    }

    /**
     * The language detected in the source text. It reflects the value of the `source_lang` parameter, when specified.
     *
     */
    @JsonProperty("detected_source_language")
    public void setDetectedSourceLanguage(Object detectedSourceLanguage) {
        this.detectedSourceLanguage = detectedSourceLanguage;
    }

    /**
     * The translated text.
     *
     */
    @JsonProperty("text")
    public String getText() {
        return text;
    }

    /**
     * The translated text.
     *
     */
    @JsonProperty("text")
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Number of characters counted by DeepL for billing purposes. Only present if the show_billed_characters parameter is set to true.
     *
     */
    @JsonProperty("billed_characters")
    public Integer getBilledCharacters() {
        return billedCharacters;
    }

    /**
     * Number of characters counted by DeepL for billing purposes. Only present if the show_billed_characters parameter is set to true.
     *
     */
    @JsonProperty("billed_characters")
    public void setBilledCharacters(Integer billedCharacters) {
        this.billedCharacters = billedCharacters;
    }

    /**
     * Indicates the translation model used. Only present if model_type parameter is included in the request.
     *
     */
    @JsonProperty("model_type_used")
    public String getModelTypeUsed() {
        return modelTypeUsed;
    }

    /**
     * Indicates the translation model used. Only present if model_type parameter is included in the request.
     *
     */
    @JsonProperty("model_type_used")
    public void setModelTypeUsed(String modelTypeUsed) {
        this.modelTypeUsed = modelTypeUsed;
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
        sb.append(Translation.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("detectedSourceLanguage");
        sb.append('=');
        sb.append(((this.detectedSourceLanguage == null)?"<null>":this.detectedSourceLanguage));
        sb.append(',');
        sb.append("text");
        sb.append('=');
        sb.append(((this.text == null)?"<null>":this.text));
        sb.append(',');
        sb.append("billedCharacters");
        sb.append('=');
        sb.append(((this.billedCharacters == null)?"<null>":this.billedCharacters));
        sb.append(',');
        sb.append("modelTypeUsed");
        sb.append('=');
        sb.append(((this.modelTypeUsed == null)?"<null>":this.modelTypeUsed));
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
        result = ((result* 31)+((this.detectedSourceLanguage == null)? 0 :this.detectedSourceLanguage.hashCode()));
        result = ((result* 31)+((this.modelTypeUsed == null)? 0 :this.modelTypeUsed.hashCode()));
        result = ((result* 31)+((this.text == null)? 0 :this.text.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.billedCharacters == null)? 0 :this.billedCharacters.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Translation) == false) {
            return false;
        }
        Translation rhs = ((Translation) other);
        return ((((((this.detectedSourceLanguage == rhs.detectedSourceLanguage)||((this.detectedSourceLanguage!= null)&&this.detectedSourceLanguage.equals(rhs.detectedSourceLanguage)))&&((this.modelTypeUsed == rhs.modelTypeUsed)||((this.modelTypeUsed!= null)&&this.modelTypeUsed.equals(rhs.modelTypeUsed))))&&((this.text == rhs.text)||((this.text!= null)&&this.text.equals(rhs.text))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.billedCharacters == rhs.billedCharacters)||((this.billedCharacters!= null)&&this.billedCharacters.equals(rhs.billedCharacters))));
    }

}
