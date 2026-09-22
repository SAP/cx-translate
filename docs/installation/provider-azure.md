# Azure Translator Provider

CX Translations integrates with the **Azure AI Translator** document translation API. It supports two translation modes:

| Mode | Description | Blob Storage required |
|---|---|---|
| `SYNCHRONOUS` | Translates one document per request (single multipart POST). Simple setup. | No |
| `ASYNC_BATCH` | Submits a batch of documents as a single async job. Better for large volumes. | Yes |

---

## Prerequisites

- An Azure AI Translator resource (multi-service or dedicated Translator resource)
- Azure endpoint URL (e.g. `https://your-resource.cognitiveservices.azure.com`)
- One of the supported authentication methods:
  - **API key** via a standalone `Ocp-Apim-Subscription-Key` header (most common)
  - **OAuth** via `ConsumedOAuthCredential`
- For `ASYNC_BATCH` mode: an Azure Blob Storage account with a SAS token that allows container creation, blob read/write/delete

---

## Step 1 — Import the Azure Translator ImpEx

### SYNCHRONOUS mode (recommended starting point)

Copy the snippet below, fill in your values, and import via the `patches` mechanism or HAC (`/hac/console/impex/import`).

```impex
# ---------- Destination Target ----------
INSERT_UPDATE DestinationTarget; id[unique = true]
                               ; cxTranslateDefaultAzureTarget

# ---------- API Key Credential ----------
# Azure uses the Ocp-Apim-Subscription-Key header for key-based auth.
# keyName must be "Ocp-Apim-Subscription-Key".
# credentialMode STANDALONE_HEADER injects it as a plain header (not prefixed with "Authorization:").
INSERT_UPDATE CxTranslateKeyBasedCredential; id[unique = true]                    ; keyName                      ; keyValue                    ; credentialMode(code)
                                           ; cxTranslateDefaultAzureCredentials   ; Ocp-Apim-Subscription-Key    ; <<YOUR_AZURE_API_KEY>>       ; STANDALONE_HEADER

# ---------- Consumed Destination ----------
# Replace with your Azure Translator endpoint, e.g.:
# https://your-resource.cognitiveservices.azure.com
# or the global endpoint: https://api.cognitive.microsofttranslator.com
INSERT_UPDATE ConsumedDestination; id[unique = true]                  ; credential(id)                      ; url                           ; destinationTarget(id)
                                 ; cxTranslateDefaultAzureDestination ; cxTranslateDefaultAzureCredentials  ; <<AZURE_TRANSLATOR_ENDPOINT>> ; cxTranslateDefaultAzureTarget

# ---------- Translation Service API ----------
INSERT_UPDATE CxAzureTranslatorServiceApi; code[unique = true]  ; name                     ; destination(id)
                                         ; defaultAzureApi      ; Default Azure Translator  ; cxTranslateDefaultAzureDestination

# ---------- Translation Service Settings (SYNCHRONOUS) ----------
INSERT_UPDATE CxAzureTranslatorServiceSettings; code[unique = true]    ; name                      ; azureTranslatorServiceApi(code); translationMode(code); isDefault
                                              ; defaultAzureSettings   ; Azure Translator Default  ; defaultAzureApi                ; SYNCHRONOUS          ; true
```

### ASYNC_BATCH mode

Use this ImpEx instead of the one above when you want batch processing.

```impex
# Same credential and destination as SYNCHRONOUS — no changes needed there.
# Only the settings item changes.

INSERT_UPDATE CxAzureTranslatorServiceSettings; code[unique = true]         ; name                           ; azureTranslatorServiceApi(code); translationMode(code); isDefault
                                              ; defaultAzureBatchSettings   ; Azure Translator Async Batch   ; defaultAzureApi                ; ASYNC_BATCH          ; false
```

> [!IMPORTANT] `ASYNC_BATCH` mode also requires the `cxtranslate.integrations.azuretranslator.storage.sas.url` property to be set — see [Step 3](#step-3--configure-blob-storage-for-async_batch-mode-only).

---

## Step 2 — Verify the Configuration in Backoffice

Open the **CX Translation** perspective in Backoffice and navigate to **Translation Settings**.

### Credential

Verify `cxTranslateDefaultAzureCredentials`:

- `keyName`: `Ocp-Apim-Subscription-Key`
- `keyValue`: your Azure API key
- `credentialMode`: `STANDALONE_HEADER` — this injects the header as `Ocp-Apim-Subscription-Key: <value>`, not as an `Authorization` header

### Translation Service Settings

Open `defaultAzureSettings` to review optional fields:

| Field | Description | Default |
|---|---|---|
| Name | Display name in the translation popup | — |
| Is default | Selected by default in the translation popup | `false` |
| Azure Translator Service API | Reference to `CxAzureTranslatorServiceApi` | — |
| Translation mode | `SYNCHRONOUS` or `ASYNC_BATCH` | `SYNCHRONOUS` |
| Category | Custom model category ID from Azure Custom Translator (maps to the `category` query parameter) | *(empty)* |
| Allow fallback | If `false`, sets `allowFallback=false` query parameter to prevent fallback to the default Azure model | *(unset)* |
| Glossaries | List of `CxTranslateGlossary` items — see [Step 4](#step-4--configure-glossaries-optional) | *(empty)* |

---

## Step 3 — Configure Blob Storage for `ASYNC_BATCH` mode only

`ASYNC_BATCH` mode requires an Azure Blob Storage account. CX Translations uses a SAS (Shared Access Signature) URL to access the storage.

### 1. Create a SAS token

In the Azure Portal for your Blob Storage account, create a **SAS token** at the **account level** (not container level) with the following permissions:

- Service: `Blob`
- Resource types: `Container`, `Object`
- Permissions: `Read`, `Write`, `Delete`, `List`, `Create`
- Set an appropriate expiry date

Copy the generated **Blob Service SAS URL**.

### 2. Set the SAP Commerce property

Add the following property to your SAP Commerce configuration (e.g. `local.properties` or the `patches` extension):

```properties
# Full Azure Blob Service SAS URL
# Example: https://youraccount.blob.core.windows.net?sv=2020-08-04&ss=b&srt=sco&sp=rwdlc&se=...
cxtranslate.integrations.azuretranslator.storage.sas.url=<<AZURE_BLOB_SAS_URL>>

# Optional: name of the container to use (default: cxtranslate-translations)
# cxtranslate.integrations.azuretranslator.storage.container=cxtranslate-translations
```

CX Translations will create the container automatically if it does not exist. Temporary blobs (source documents, translated results, glossaries) are uploaded per-job and deleted after the job completes.

---

## Step 4 — Configure Glossaries *(optional)*

Glossaries allow you to enforce specific translations for terminology. For Azure, glossary support differs by translation mode.

### SYNCHRONOUS mode — inline multipart file

Attach a glossary CSV/TSV as a Media item and reference it in `CxTranslateGlossary`. The file is sent inline as a multipart part in each translation request.

```impex
# 1. Upload the glossary file as a Media item in Backoffice
#    Format: two-column CSV or TSV, e.g.:
#    "source_term","target_term"
#    "Powertools","Powertools"

# 2. Create the glossary entry
INSERT_UPDATE CxTranslateGlossary; sourceLanguage(isocode); targetLanguage(isocode); media(code)         ; glossaryFormat
                                 ; en                     ; de                     ; myAzureGlossary     ; tsv

# 3. Attach to Azure settings
UPDATE CxAzureTranslatorServiceSettings; code[unique = true]  ; glossaries(sourceLanguage(isocode), targetLanguage(isocode))[mode = append]
                                       ; defaultAzureSettings ; en:de
```

### ASYNC_BATCH mode — external blob URL or inline file

In `ASYNC_BATCH` mode, glossaries can be provided either as:

- **Inline Media file** — CX Translations uploads the file to Blob Storage per-job and deletes it afterwards (same as `SYNCHRONOUS`, no extra setup)
- **Pre-uploaded Blob Storage URL** — store a persistent SAS URL in `externalGlossaryRef`

```impex
# Option A: Media file (uploaded per-job, cleaned up automatically)
INSERT_UPDATE CxTranslateGlossary; sourceLanguage(isocode); targetLanguage(isocode); media(code)          ; glossaryFormat
                                 ; en                     ; de                     ; myAzureGlossaryMedia ; tsv

# Option B: Pre-uploaded Blob URL with SAS token
INSERT_UPDATE CxTranslateGlossary; sourceLanguage(isocode); targetLanguage(isocode); externalGlossaryRef
                                 ; en                     ; de                     ; https://youraccount.blob.core.windows.net/glossaries/en-de.tsv?sv=...
```

---

## Notes

- Azure Translator API version `2024-05-01` is used.
- Each `SYNCHRONOUS` request translates one document to one target language. CX Translations issues one request per target language per value.
- `ASYNC_BATCH` mode batches multiple documents into a single Azure job. This reduces API call overhead for large translation sets but requires Blob Storage.
- The Azure `SYNCHRONOUS` endpoint (`/translator/document:translate`) natively handles HTML and XLIFF documents, preserving markup during translation.
- CX Translations handles the UTF-8 BOM that Azure occasionally prepends to translated documents.
