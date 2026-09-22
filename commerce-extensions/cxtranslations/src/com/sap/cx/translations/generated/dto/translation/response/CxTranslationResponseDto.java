
package com.sap.cx.translations.generated.dto.translation.response;

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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "translations",
    "rawMessage",
    "status",
    "usageInfo"
})
public class CxTranslationResponseDto implements Serializable
{

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("translations")
    private List<CxTranslationOutputText> translations = new ArrayList<CxTranslationOutputText>();
    @JsonProperty("rawMessage")
    private String rawMessage;
    @JsonProperty("status")
    private Status status;
    @JsonProperty("usageInfo")
    private Map<String, Double> usageInfo;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
    private final static long serialVersionUID = 3102533447001762050L;

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("translations")
    public List<CxTranslationOutputText> getTranslations() {
        return translations;
    }

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("translations")
    public void setTranslations(List<CxTranslationOutputText> translations) {
        this.translations = translations;
    }

    @JsonProperty("rawMessage")
    public String getRawMessage() {
        return rawMessage;
    }

    @JsonProperty("rawMessage")
    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    @JsonProperty("status")
    public Status getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(Status status) {
        this.status = status;
    }

    @JsonProperty("usageInfo")
    public Map<String, Double> getUsageInfo() {
        return usageInfo;
    }

    @JsonProperty("usageInfo")
    public void setUsageInfo(Map<String, Double> usageInfo) {
        this.usageInfo = usageInfo;
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
        sb.append(CxTranslationResponseDto.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("translations");
        sb.append('=');
        sb.append(((this.translations == null)?"<null>":this.translations));
        sb.append(',');
        sb.append("rawMessage");
        sb.append('=');
        sb.append(((this.rawMessage == null)?"<null>":this.rawMessage));
        sb.append(',');
        sb.append("status");
        sb.append('=');
        sb.append(((this.status == null)?"<null>":this.status));
        sb.append(',');
        sb.append("usageInfo");
        sb.append('=');
        sb.append(((this.usageInfo == null)?"<null>":this.usageInfo));
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
        result = ((result* 31)+((this.usageInfo == null)? 0 :this.usageInfo.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.translations == null)? 0 :this.translations.hashCode()));
        result = ((result* 31)+((this.rawMessage == null)? 0 :this.rawMessage.hashCode()));
        result = ((result* 31)+((this.status == null)? 0 :this.status.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CxTranslationResponseDto) == false) {
            return false;
        }
        CxTranslationResponseDto rhs = ((CxTranslationResponseDto) other);
        return ((((((this.usageInfo == rhs.usageInfo)||((this.usageInfo!= null)&&this.usageInfo.equals(rhs.usageInfo)))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.translations == rhs.translations)||((this.translations!= null)&&this.translations.equals(rhs.translations))))&&((this.rawMessage == rhs.rawMessage)||((this.rawMessage!= null)&&this.rawMessage.equals(rhs.rawMessage))))&&((this.status == rhs.status)||((this.status!= null)&&this.status.equals(rhs.status))));
    }

    public enum Status {

        SUCCESS("Success"),
        ERROR("Error");
        private final String value;
        private final static Map<String, Status> CONSTANTS = new HashMap<String, Status>();

        static {
            for (Status c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Status(String value) {
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
        public static Status fromValue(String value) {
            Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
