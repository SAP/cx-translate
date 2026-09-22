# Commerce Extensions Installation

## Prerequisites

- SAP Commerce Cloud (2211 or compatible)
- `patches` extension or another mechanism to run ImpEx files during system setup

---

## Steps

1. Clone or download the CX Translations repository.

1. Copy the `commerce-extensions` directory into `bin/custom` of your SAP Commerce implementation. It contains three extensions:

   | Extension | Purpose |
   |---|---|
   | `cxtranslations` | Core extension — data model, OCC API, translation services |
   | `cxtranslationsbackoffice` | Backoffice perspective, widgets, and actions |
   | `cxtranslationssmartedit` | SmartEdit integration *(preview)* |

1. Register the extensions in your `localextensions.xml`:

   ```xml
   <extension name="cxtranslations"/>
   <extension name="cxtranslationsbackoffice"/>
   <extension name="cxtranslationssmartedit"/>
   ```

   > [!NOTE] `cxtranslationssmartedit` is a preview extension. It can be omitted if SmartEdit integration is not needed.

1. Run a full build and start the server:

   ```bash
   ant clean all
   ```
1. In case of issue with SmartEdit dependencies please use the `ant upgradeCustomSmartEdit` task, see here for an example: https://help.sap.com/docs/SAP_COMMERCE_CLOUD_PUBLIC_CLOUD/75d4c3895cb346008545900bffe851ce/0b21cda43b414f0eb76137a4c464ced2.html?locale=en-US