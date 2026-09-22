# Localised Labels

## Business Context

SAP Composable Storefront comes with the i18next library as a core of application localization. It is powerful and solves a lot of problems, however, in SAP Composable Storefront it is limited to bundled static resource files. As a result, it is not possible to change a label or add a new language bundle without the deployment. Translations are also tied to the implementation/code, not to the CMS site as the content catalog does, so it is impossible to use different label translations e.g. per different brands. To solve all the issues above, the CX Translations package has been created.
**CxLocalizedLabel**s represent the labels to be used as SAP Composable Storefront labels or used in the backend logic.
They can be found in the new translation perspective.
As with any other type in the system, it is searchable and editable. Labels are catalog version aware, which means there are always two versions of the same label - **Staged** and **Online** (visible to the customers).

- [Manage labels on the Storefront](/localisedLabels/localizedLabels_storefront.md)
- [Manage labels in Backoffice](/localisedLabels/localizedLabels_backoffice.md)
- [Labels configuration](/localisedLabels/localizedLabels_configuration.md)
- [Development guide for Labels](/localisedLabels/localizedLabels_development.md)
- [Export Storefront Labels to Impex](/localisedLabels/localizedLabels_storefront-labels_export.md)
