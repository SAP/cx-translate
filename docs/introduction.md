# Introduction

## Important Notes

> [!WARNING]

> * There is a possibility that, at some point, the product team will either introduce their own version of this solution or adopt it into the product or evolve in way that is not compatible with the CX Translate. In such cases, solution migration may be necessary. 


## Releases
[Release notes](https://github.com/SAP/cx-translate/releases)


## CX Translate Benefits

The CX Translate project was created to address real-life challenges related to managing and translating various types of content. To simplify installation and maintenance, CX Translate is developed as a single package, though it allows users to choose specific functionalities based on project needs. 
The main functionalities offered by the solution are:

### Localized Labels Management

Manage SAP Composable Storefront frontend translations in a database, eliminating costly code changes and deployments for each translation or typographical error fix.

#### Benefits:
* Enables role-based access via backoffice in the new CX Translate perspective for frontend translations.
* Leverage backoffice functionality to export/import data (Localised Labels) as Excel files, enhancing collaboration with translation experts.
* Simplifies the process of fixing misspellings or adding/changing translations by locating the exact object in need of modification (switch to display keys in frontend instead of translated values).
* Test changes in frontend providing manual changes in **Staged** catalog before publishing to **Online** with the help of a new CMS component on the frontend.
* Automatically collect all labels from various files in SAP Composable Storefront when starting new projects, thus streamlining initial setup.
* Proven in live production projects, saving hundreds of hours of labour for both customers and implementation teams.

For details go [Localized Labels](/localizedLabels.md) .

### Localized Values

SAP Commerce Cloud is a platform with numerous item types and attributes requiring translations.
Traditional approaches involve translating each attribute individually. This leads to the creation of complex export impexes or extensive manual documentation describing what needs to be translated and where it can be found in backoffice. This results in processes that are often laborious, error-prone, and challenging to validate.

#### Solution:
* Automatically recognize and display all elements requiring translation in a single view.
* Present translation values side-by-side with original values, enabling efficient one-by-one translations without needing to open detailed views.
* Allow export/import of translation data to/from Excel, facilitating collaboration with third-party translation companies.

For details go [Localized Values](/localizedValues.md) .

### AI Translations

CX Translate offers an integration with AI translation solutions, extendable as needed.
It supports usage and cost tracking based on configurable metrics.

#### AI driven automatic translation flow:
* Select individual or multiple values / items for translation.
* For Frontend labels automatic handling of plural forms is available, so the correct plural forms in target language are generated.
* Choose a source language and one or more target languages to translate to.
* Decide if proposed translation should apply automatically or there is a need of manual review step.
* Use CronJob to send asynchronously selected data for translation. 
* Optionally: manually review, modify and adjust proposed translations.

> [!NOTE] **AI Translation Considerations**
> 
> * AI translation output must always be reviewed.
> * It may be worthwhile to add configurations for other/new LLM models as they become available in the future.
> * The default prompts may not yet be fully optimized in terms of accuracy, reliability, or token usage.
> * Currently, GPT-5 models are used by default. They differ in capabilities to translate complex texts and cost of usage. [Evaluation of translation quality performed in FEB 2026](/ai-translation-evaluation-gpt4-5.md)

### Third-party Translation Provider support

By default, CX Translate uses LLM deployed on SAP BTP. There is a possibility to add support to third party translation service, DeepL is provided as an example implementation.

For details go [Automated Translations Powered by AI](/aitranslations_configuration.md) .
