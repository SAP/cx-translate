
package com.sap.cx.translations.generated.dto.translation.response;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "key",
    "languageToValueMap",
    "feedback"
})
public class CxTranslationOutputText implements Serializable
{

    @JsonProperty("key")
    private String key;
    /**
     *
     * (Required)
     *
     */
    @JsonProperty("languageToValueMap")
    private Map<String, String> languageToValueMap;
    @JsonProperty("feedback")
    private String feedback;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
    private final static long serialVersionUID = -7738275601277043107L;

    @JsonProperty("key")
    public String getKey() {
        return key;
    }

    @JsonProperty("key")
    public void setKey(String key) {
        this.key = key;
    }

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("languageToValueMap")
    public Map<String, String> getLanguageToValueMap() {
        return languageToValueMap;
    }

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("languageToValueMap")
    public void setLanguageToValueMap(Map<String, String> languageToValueMap) {
        this.languageToValueMap = languageToValueMap;
    }

    @JsonProperty("feedback")
    public String getFeedback() {
        return feedback;
    }

    @JsonProperty("feedback")
    public void setFeedback(String feedback) {
        this.feedback = feedback;
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
        sb.append(CxTranslationOutputText.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("key");
        sb.append('=');
        sb.append(((this.key == null)?"<null>":this.key));
        sb.append(',');
        sb.append("languageToValueMap");
        sb.append('=');
        sb.append(((this.languageToValueMap == null)?"<null>":this.languageToValueMap));
        sb.append(',');
        sb.append("feedback");
        sb.append('=');
        sb.append(((this.feedback == null)?"<null>":this.feedback));
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
        result = ((result* 31)+((this.languageToValueMap == null)? 0 :this.languageToValueMap.hashCode()));
        result = ((result* 31)+((this.feedback == null)? 0 :this.feedback.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.key == null)? 0 :this.key.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CxTranslationOutputText) == false) {
            return false;
        }
        CxTranslationOutputText rhs = ((CxTranslationOutputText) other);
        return (((((this.languageToValueMap == rhs.languageToValueMap)||((this.languageToValueMap!= null)&&this.languageToValueMap.equals(rhs.languageToValueMap)))&&((this.feedback == rhs.feedback)||((this.feedback!= null)&&this.feedback.equals(rhs.feedback))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.key == rhs.key)||((this.key!= null)&&this.key.equals(rhs.key))));
    }

}
