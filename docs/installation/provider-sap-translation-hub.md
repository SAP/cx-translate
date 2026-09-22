# SAP Translation Hub Provider

CX Translations supports **SAP Translation Hub** as a translation provider. It uses the XLIFF 1.2 document translation API and supports both the LLM-based and the statistical translation model.

---

## Prerequisites

- Access to SAP Translation Hub (BTP service)
- OAuth credentials (Client ID, Client Secret, Token URL) or API key
- SAP Translation Hub service endpoint URL

---

## Step 1 — Import the SAP Translation Hub ImpEx

Copy the snippet below, fill in your values, and import it via the `patches` mechanism or the HAC ImpEx console (`/hac/console/impex/import`).

```impex
# ---------- Destination Target ----------
INSERT_UPDATE DestinationTarget; id[unique = true]
                               ; cxTranslateDefaultSapTHTarget

# ---------- Credentials (OAuth) ----------
INSERT_UPDATE ConsumedOAuthCredential; id[unique = true]                    ; oauthurl                ; clientId         ; clientSecret
                                     ; cxTranslateDefaultSapTHCredentials  ; <<OAUTH_TOKEN_URL>>     ; <<CLIENT_ID>>    ; <<CLIENT_SECRET>>

# ---------- Consumed Destination ----------
# Example URL: https://api.translate.com/v2
INSERT_UPDATE ConsumedDestination; id[unique = true]                  ; credential(id)                      ; url                     ; destinationTarget(id)
                                 ; cxTranslateDefaultSapTHDestination ; cxTranslateDefaultSapTHCredentials  ; <<SAP_TH_ENDPOINT_URL>> ; cxTranslateDefaultSapTHTarget

# ---------- Translation Service API ----------
INSERT_UPDATE CxSapTranslationHubServiceApi; code[unique = true]  ; name                    ; destination(id)
                                           ; defaultSapThApi      ; Default SAP Trans. Hub  ; cxTranslateDefaultSapTHDestination

# ---------- Translation Service Settings ----------
INSERT_UPDATE CxSapTranslationHubServiceSettings; code[unique = true]  ; name                       ; sapTranslationHubServiceApi(code); isDefault; model(code)
                                                ; defaultSapThSettings ; SAP Translation Hub Default ; defaultSapThApi                  ; true     ; llm
```

---

## Step 2 — Verify the Configuration in Backoffice

Open the **CX Translation** perspective in Backoffice and navigate to **Translation Settings**.

### Consumed Destination

Verify the URL of `cxTranslateDefaultSapTHDestination` matches your SAP Translation Hub endpoint.

### Credentials

Verify the OAuth credentials are populated correctly.

### Translation Service Settings

Open `defaultSapThSettings` and review the available configuration fields:

| Field | Description | Default |
|---|---|---|
| Name | Display name used in the translation popup | — |
| Is default | If `true`, selected by default when starting a translation | `false` |
| SAP Translation Hub Service API | Reference to the API configuration | — |
| Model | Translation model: `llm` (LLM-based) or `default` (statistical) | `llm` |
| Domain | Optional domain for specialised vocabulary (e.g. `SAP_Fiori`) | *(empty)* |
| Enable HTML tag handling | XML-escapes HTML tags before sending and unescapes on response | `true` |
| Direct text translation threshold (bytes) | Values larger than this are sent as `text/html` instead of being wrapped in XLIFF | `1024` |
| Max XLIFF batch size (bytes) | Maximum UTF-8 size of a generated XLIFF request batch | `4096` |

---

## Notes

- SAP Translation Hub uses **XLIFF 1.2** for document translation. CX Translations handles XLIFF wrapping and parsing automatically.
- Plural-form ICU messages are preserved through the XLIFF structure — the `{CNT, plural, ...}` syntax survives translation.
- If HTML content exceeds `directTextTranslationThresholdBytes`, it is sent as raw `text/html` to avoid XLIFF size limits.
