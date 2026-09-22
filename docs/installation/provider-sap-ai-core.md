# SAP AI Core Translation Provider

CX Translations integrates with AI models deployed on **SAP AI Core on SAP BTP**. This is the default translation provider. It supports GPT, Claude, Gemini, and many other LLM models available in SAP AI Core.

---

## Prerequisites

- Access to SAP AI Core on BTP with at least one AI model deployment
- OAuth credentials (Client ID, Client Secret, OAuth Token URL) from the SAP AI team
- Base URL of the SAP AI Core service
- Deployment IDs for each AI model you plan to use

---

## Step 1 — Import the AI Settings ImpEx

Use the `ai-settings.impex` template from the [`downloads`](/downloads/ ':ignore') directory. The file has mandatory placeholders that **must** be replaced before import.

**[Download ai-settings.impex](/downloads/ai-settings.impex ':ignore')**

> [!WARNING] Replace every `<<CHANGE_ME>>` marker and every `change_me` deployment ID before importing. Importing with placeholder values will result in non-functional translation requests.

### Macros to replace

| Macro | Description | Example |
|---|---|---|
| `$oAuthClientId` | OAuth Client ID for the BTP AI service | `sb-...!t123456` |
| `$oAuthSecret` | OAuth Client Secret | `...` |
| `$oAuthUrl` | OAuth Token URL | `https://your-subdomain.authentication.eu10.hana.ondemand.com/oauth/token` |
| `$aiBaseUrl` | Base URL of the SAP AI Core service | `https://api.ai.prod.eu-central-1.aws.ml.hana.ondemand.com` |

### Deployment IDs

In the `CxAiDeploymentConfig` section of the ImpEx, replace every `change_me` with the actual deployment ID for each AI model you plan to use. **Remove entries for models you are not using** to keep the configuration clean.

Example (keep only the models you have deployed):

```impex
INSERT_UPDATE CxAiDeploymentConfig; code[unique = true]; name    ; destination(id)                    ; deployments(key(code), value)[map-delimiter = |, key2value-delimiter = ->]
                                  ; default            ; Default ; cxTranslateDefaultBtpAiDestination ; "
                                  gpt-5-mini-> d1a2b3c4d5e6f7a8
                                  | gpt-5-nano-> e9f0a1b2c3d4e5f6
                                  | anthropic--claude-4.5-sonnet-> f7a8b9c0d1e2f3a4"
```

---

## Step 2 — Verify the Configuration in Backoffice

Navigate to the **CX Translation** perspective in Backoffice and open **Translation Settings** (under the **AI Configuration** tree node).

### Consumed Destination

The default destination is `cxTranslateDefaultBtpAiDestination`. Verify the **URL** field matches your SAP AI Core base URL.

### OAuth Credentials

The default credentials item is `cxTranslateDefaultBtpAiCredentials`. Verify:

- **Client ID**
- **Client Secret**
- **OAuth URL** — example: `https://your-tenant.authentication.eu10.hana.ondemand.com/oauth/token`

### AI Deployment Configuration

The item `CxAiDeploymentConfig` (code `default`) maps AI model codes to deployment IDs. Open it and confirm the deployment IDs are set correctly.

![ai deployment config](../images/ai-deployment-config.png)

---

## Step 3 — Review Translation Service Settings

Translation Service Settings aggregate all options used when initiating a translation.

The ImpEx creates three pre-configured settings objects based on GPT-5 models. Review and adjust as needed:

| Setting code | Name | Model | Notes |
|---|---|---|---|
| `gpt5` | GPT-5 (complex texts) | `gpt-5` | Highest quality, highest cost |
| `gpt5Mini` | GPT-5 Mini (default) | `gpt-5-mini` | Good quality/cost balance. Default. |
| `gpt5Nano` | GPT-5 Nano (simple texts) | `gpt-5-nano` | Fastest, lowest cost |

**Available fields:**

| Field | Description |
|---|---|
| Is default | If `true`, this settings object is selected by default in the translation popup |
| Translation Service API | The `CxAiTranslationServiceApi` to use |
| AI Model | The AI model to use (must be supported by the selected API) |
| Global Translation Hints | Hints added to all translation requests (e.g. "These are powertools product names") |
| Max languages in a request | How many target languages are bundled in a single request. Smaller values improve quality. |
| Cost per unit | Cost per usage metric unit — used for cost tracking |
| Registered usage / Total Cost | Running usage metrics (read/write) and calculated total cost |

---

## Advanced: Translation Service API

`CxAiTranslationServiceApi` defines the low-level HTTP interaction with the AI endpoint.

| Field | Description | Example |
|---|---|---|
| Endpoint URL | URL suffix appended to the destination base URL. Supports `{deployment}` and `{model}` placeholders. | `/v2/inference/deployments/{deployment}/chat/completions?api-version=2024-10-21` |
| Deployment Configuration | Reference to the `CxAiDeploymentConfig` item | `default` |
| Configurable parameters | Parameter names that can be customized per-request | `temperature,frequency_penalty` |
| Default parameter values | Default values for those parameters | `temperature->0.1\|frequency_penalty->0.3` |
| Logging Level | When to persist the full response object: `NEVER`, `ERROR_ONLY`, `ALWAYS` | `NEVER` |
| Request template | Groovy Simple Template Engine template for the request body. Supports `${parameters.XXX}`, `${model}`, `${requestJson}`, `${responseSchemaJson}` | See `sapBtpAiOpenAiGpt5` in Backoffice |
| JSON Path | JSON Path to extract the translation result from the response | `$.choices[0].message.content` |
| Usage Metrics Paths | Map of metric names to JSON Paths for token/cost tracking | `inputTokens->$.usage.prompt_tokens\|outputTokens->$.usage.completion_tokens` |
| Custom Request Writer Bean | Spring bean ID for custom request serialization (advanced) | *(empty)* |
| Custom Response Reader Bean | Spring bean ID for custom response deserialization (advanced) | *(empty)* |

> [!WARNING]
> - The list of AI models reflects what was available in SAP AI Core at implementation time. New models can be added as `CxTranslateAiModel` items.
> - Only models with a configured `CxAiTranslationServiceApi` and deployment ID can be used for translation.
> - Set `Logging Level` to `ALWAYS` only during development. Production logs may expose sensitive content.

---

## Available AI Models

The following models are pre-configured in `ai-settings.impex`:

| Model code | Name |
|---|---|
| `gpt-5` | GPT 5 |
| `gpt-5-mini` | GPT 5 Mini |
| `gpt-5-nano` | GPT 5 Nano |
| `gpt-4o` | GPT 4o |
| `gpt-4o-mini` | GPT 4o Mini |
| `gpt-4.1` | GPT 4.1 |
| `gpt-4.1-nano` | GPT 4.1 Nano |
| `anthropic--claude-4.5-sonnet` | Anthropic Claude 4.5 Sonnet |
| `anthropic--claude-4.5-haiku` | Anthropic Claude 4.5 Haiku |
| `anthropic--claude-4-sonnet` | Anthropic Claude 4 Sonnet |
| `anthropic--claude-3.7-sonnet` | Anthropic Claude 3.7 Sonnet |
| `anthropic--claude-3.5-sonnet` | Anthropic Claude 3.5 Sonnet |
| `gemini-2.5-pro` | Gemini 2.5 Pro |
| `gemini-2.5-flash` | Gemini 2.5 Flash |
| `gemini-2.0-flash` | Gemini 2.0 Flash |
| `sap-rpt-1-large` | SAP RPT 1 Large |
| `sap-rpt-1-small` | SAP RPT 1 Small |
| `o3-mini` | o3 Mini |
| `sonar` | Sonar |
| `sonar-pro` | Sonar Pro |
| *(and more)* | See full list in `ai-settings.impex` |

> [!NOTE] See the [AI Translation Quality Evaluation](../ai-translation-evaluation-gpt4-5.md) for a comparison of GPT-4.x and GPT-5 translation quality performed in February 2026.
