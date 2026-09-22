# Export Storefront Labels to Impex

The **Export Translations** script scans your SAP Composable Storefront source code, reads all existing translation bundles, and generates a ready-to-import SAP Commerce Impex file (`all_translations.impex`). This is the recommended way to perform an initial seed of your storefront labels into the **CxLocalizedLabel** model managed by the CX Translations extension.

<details>
<summary>📄 <strong>Click to view export-translations.js</strong></summary>

```javascript
const fs = require('fs');
const path = require('path');

// 🔧 Parse language codes from command line arguments (comma-separated)
// Usage: node export-translations.js en,pl,de
// Default: en
const langArg = process.argv[2];
const LANG_CODES = langArg ? langArg.split(',').map(lang => lang.trim()) : ['en'];

console.log(`\n🌐 Language settings: ${LANG_CODES.join(', ')}`);
console.log(`   (Usage: node export-translations.js <lang1,lang2,...>)`);
console.log(`   (Example: node export-translations.js en,pl,de)\n`);

// 🔍 Recursively find all folders ending with 'assets/translations'
function findTranslationDirs(startDir) {
  const result = [];

  function recurse(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        if (fullPath.endsWith('assets/src/translations') || fullPath.endsWith('assets/translations')) {
          result.push(fullPath);
        } else {
          recurse(fullPath);
        }
      }
    }
  }

  recurse(startDir);
  return result;
}

// 🧠 Extract translation keys from the export object in index.ts
function extractKeysFromExport(filePath, langCode) {
  const content = fs.readFileSync(filePath, 'utf8');
  const exportRegex = new RegExp(`export\\s+const\\s+${langCode}\\s*=\\s*{([\\s\\S]*?)};`);
  const match = content.match(exportRegex);
  if (!match) return [];

  // Extract object keys (e.g., cpqQuote,) using simple regex
  const objectBody = match[1];
  return [...objectBody.matchAll(/\b([a-zA-Z0-9_]+)\b\s*,?/g)].map(m => m[1]);
}

// 🛠 Main
const baseDir = '.';
const dirs = findTranslationDirs(baseDir);
const output = [];

for (const dir of dirs) {
  for (const lang of LANG_CODES) {
    const langDir = path.join(dir, lang);
    const indexPath = path.join(langDir, 'index.ts');
    if (fs.existsSync(indexPath)) {
      const keys = extractKeysFromExport(indexPath, lang);
      keys.forEach(key => {
        output.push(`${key};${lang};${langDir}`);
      });
    }
  }
}

// ✅ Output
function flattenJSON(obj, prefix = '') {
    const result = {};
    for (const key in obj) {
      const val = obj[key];
      const fullKey = prefix ? `${prefix}.${key}` : key;
      if (typeof val === 'object' && val !== null) {
        Object.assign(result, flattenJSON(val, fullKey));
      } else {
        result[fullKey] = val;
      }
    }
    return result;
  }

  console.log('\n📁 Generating consolidated Impex file...');

  const impexLines = [];

  // 1️⃣ Bundle header and INSERTs
  const chunkNames = [...new Set(output.map(line => line.split(';')[0]))].sort();
  impexLines.push('INSERT_UPDATE CxLocalizedBundle; code[unique = true]; $contentCV[unique = true]');
  chunkNames.forEach(chunk => {
    impexLines.push(`                               ; ${chunk}               ;`);
  });

  // 2️⃣ Translation entries
  for (const line of output) {
    const [chunk, lang, langDir] = line.split(';');
    const jsonPath = path.join(langDir, `${chunk}.json`);
    if (!fs.existsSync(jsonPath)) {
      console.warn(`⚠️ Missing JSON for ${chunk}/${lang}: ${jsonPath}`);
      continue;
    }

    const jsonData = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
    const flat = flattenJSON(jsonData, '');

    impexLines.push('');
    impexLines.push(
      `INSERT_UPDATE CxLocalizedLabel; bundle(code, $contentCV); key[unique = true]; value[lang = ${lang}]; forFrontend[default = true]; forBackend; status(code)[default = approved]; $contentCV[unique = true];`
    );

    for (const key in flat) {
      let value = flat[key];

      value = escape(value);

      impexLines.push(
        `                              ; ${chunk}                  ; ${key}                 ; ${value} ; TRUE                       ; FALSE     ;                                 ;`
      );
    }
  }

  // 3️⃣ Write to a single file
  const combinedOutPath = 'all_translations.impex';
  fs.writeFileSync(combinedOutPath, impexLines.join('\n'), 'utf8');
  console.log(`✅ Wrote combined Impex: ${combinedOutPath}`);

  function escape(value) {
   // Wrap multiline or semicolon-containing values in quotes
      if (value.includes('\n') || value.includes(';') || value.startsWith('"')) {
        value = value.toString().replace(/"/g, '""');
        value = `"${value}"`;
      }
      return value;
}
```

</details>

---

### Prerequisites

- Node.js installed (v14 or higher recommended)

---

## Usage

1. Download SAP Composable Storefront source code, using a tag that corresponds to your storefront version, from the [GitHub repository](https://github.com/SAP/spartacus).
1. Copy the script above and save it as `export-translations.js` in the **root directory** of SAP Composable Storefront source code.
1. Run the script passing the desired language codes as a comma-separated argument:

```bash
node export-translations.js <lang1,lang2,...>
```

**Examples:**

```bash
# Export English only (default)
node export-translations.js

# Export multiple languages
node export-translations.js en,pl,de
```

1. The script produces a single `all_translations.impex` file in the current directory.

---

## Importing the Impex

The generated `all_translations.impex` uses macros (e.g. `$contentCV`) that must be resolved before import. There are two ways to use it:

- **Add to project patch Impexes**. The platform will resolve macros automatically during initialization or update.
- **Import directly in HAC** — go to `/platform/impex/import` and provide the macro values manually at the top of the file before importing, for example:
  ```impex
  $contentCatalog=electronicsContentCatalog
  $contentCV=catalogVersion(CatalogVersion.catalog(Catalog.id[default=$contentCatalog]),CatalogVersion.version[default=Staged])[default=$contentCatalog:Staged]
  ```

> [!WARNING] **Existing labels will be overwritten.**
>
> The generated file uses `INSERT_UPDATE`, which will overwrite any existing `CxLocalizedLabel` entries with matching keys. Review the file before importing into a production environment.

