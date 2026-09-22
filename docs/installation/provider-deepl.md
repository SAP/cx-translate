# DeepL Translation Provider

CX Translations integrates with the **DeepL API** for neural machine translation. Both the DeepL API Free and DeepL API Pro endpoints are supported.

---

## Prerequisites

- A DeepL API key (Free or Pro)
- The correct API endpoint URL:
  - **Free**: `https://api-free.deepl.com/v2/translate`
  - **Pro**: `https://api.deepl.com/v2/translate`

---

## Step 1 — Import the DeepL ImpEx

Copy the snippet below, fill in your values, and import it via the `patches` mechanism or the HAC ImpEx console (`/hac/console/impex/import`).

```impex
# ---------- Destination Target ----------
INSERT_UPDATE DestinationTarget; id[unique = true]
                               ; cxTranslateDefaultDeepLTarget

# ---------- API Key Credential ----------
# keyName must be "DeepL-Auth-Key" — this is the HTTP header name used by DeepL
INSERT_UPDATE CxTranslateKeyBasedCredential; id[unique = true]                  ; keyName        ; keyValue
                                           ; cxTranslateDefaultDeepLCredentials ; DeepL-Auth-Key ; <<YOUR_DEEPL_API_KEY>>

# ---------- Consumed Destination ----------
# Use https://api-free.deepl.com/v2/translate for Free tier
# Use https://api.deepl.com/v2/translate for Pro tier
INSERT_UPDATE ConsumedDestination; id[unique = true]                  ; credential(id)                     ; url                                     ; destinationTarget(id)
                                 ; cxTranslateDefaultDeepLDestination ; cxTranslateDefaultDeepLCredentials ; https://api-free.deepl.com/v2/translate ; cxTranslateDefaultDeepLTarget

# ---------- Translation Service API ----------
INSERT_UPDATE CxDeepLTranslationServiceApi; code[unique = true]; name              ; destination(id)
                                          ; defaultDeeplApi    ; Default DeepL API ; cxTranslateDefaultDeepLDestination

# ---------- Translation Service Settings ----------
INSERT_UPDATE CxDeepLTranslationServiceSettings; code[unique = true]  ; name          ; deepLServiceApi(code); isDefault
                                               ; defaultDeeplSettings ; DeepL Default ; defaultDeeplApi      ; true
```

---

## Step 2 — Verify the Configuration in Backoffice

Open the **CX Translation** perspective in Backoffice and navigate to **Translation Settings**.

### Credential

Verify `cxTranslateDefaultDeepLCredentials` has the correct API key. The `keyName` field must be `DeepL-Auth-Key` — this is the HTTP header name required by DeepL.

> [!NOTE] `CxTranslateKeyBasedCredential` injects the credential as a standalone HTTP header (`DeepL-Auth-Key: <value>`). This is different from OAuth credentials.

### Translation Service Settings

Open `defaultDeeplSettings` to review optional fields:

| Field | Description | Values / Default |
|---|---|---|
| Name | Display name in the translation popup | — |
| Is default | Selected by default in the translation popup | `false` |
| DeepL Service API | Reference to `CxDeepLTranslationServiceApi` | — |
| Model type | DeepL model variant | `LATENCY_OPTIMIZED`, `QUALITY_OPTIMIZED`, `PREFER_QUALITY_OPTIMIZED` |
| Split sentences | How to split source text into sentences | `NO_SPLITTING`, `QUALITY_OPTIMIZED`, `PREFER_QUALITY_OPTIMIZED` |
| Preserve formatting | Preserve formatting of source text | `true` / `false` / *(unset)* |
| Formality | Formality level of the output | `DEFAULT`, `MORE`, `LESS`, `PREFER_MORE`, `PREFER_LESS` |
| Tag handling | How HTML/XML tags are handled | `XML`, `HTML` |

---

## Step 3 — Configure Glossaries *(optional)*

A glossary allows you to enforce specific translations for terminology (e.g. brand names, product names). CX Translations supports two ways to attach a glossary to a DeepL translation.

### Option A — Remote Glossary API ID

If you have already created a glossary via the DeepL Glossaries API (or want to manage glossaries externally), store its ID in the `externalGlossaryRef` field:

```impex
# ---------- Glossary (external API ID) ----------
INSERT_UPDATE CxTranslateGlossary; sourceLanguage(isocode); targetLanguage(isocode); externalGlossaryRef
                                 ; en                     ; de                     ; <<DEEPL_GLOSSARY_ID>>
```

### Option B — Media File Upload

If you prefer to manage the glossary as a file in SAP Commerce, upload the glossary as a CSV or TSV Media item and reference it:

```impex
# 1. Upload the glossary file as a Media item via Backoffice or ImpEx
#    (File must be a two-column CSV/TSV: source_term,target_term)

# 2. Reference the Media in CxTranslateGlossary
INSERT_UPDATE CxTranslateGlossary; sourceLanguage(isocode); targetLanguage(isocode); media(code)       ; glossaryFormat
                                 ; en                     ; de                     ; myGlossaryMedia   ; tsv
```

> [!NOTE] Use `glossaryFormat` to explicitly set the format (`csv` or `tsv`) when the file extension alone is ambiguous.

### Attaching the Glossary to Translation Settings

After creating the `CxTranslateGlossary` item(s), attach them to the translation settings:

```impex
# Attach glossary to DeepL settings
# Multiple glossaries can be added — one per target language
UPDATE CxDeepLTranslationServiceSettings; code[unique = true]  ; glossaries(sourceLanguage(isocode), targetLanguage(isocode))[mode = append]
                                        ; defaultDeeplSettings ; en:de
```

Or link them via Backoffice: open `defaultDeeplSettings` → **Glossaries** relation → add entries.

---

## Notes

- DeepL does not support bulk requests with multiple target languages in a single call. CX Translations issues one request per target language.
- The `formality` option is only available for supported language pairs (see the DeepL API documentation for the current list).
- Glossary entries are matched case-insensitively by DeepL.
