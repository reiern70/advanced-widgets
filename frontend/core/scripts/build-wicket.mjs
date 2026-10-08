/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Builds the scripts and style sheets of ui-components from the core, under the names and in the
 * packages the Java classes load them from: dist/wicket is a resource directory of ui-components,
 * whose build minifies them into *.min.js and *.min.css.
 *
 * Each script is an IIFE that sets the Wicket host and registers the old global, such as
 * Wicket.Dropdown, so the Java side and the pages need no change.
 */

import { build } from 'esbuild';
import { copyFile, mkdir, rm } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const out = join(root, 'dist', 'wicket');

const BANNER = '/*\n * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).\n' +
	' * All rights reserved.\n *\n * Built from frontend/core, do not edit.\n */';

/** The scripts: their entry in src/wicket and their path in ui-components. */
const SCRIPTS = [
	['src/wicket/autocomplete.ts', 'com/advanced/widgets/autocomplete/wicket-autocomplete.js'],
	['src/wicket/chips.ts', 'com/advanced/widgets/chips/wicket-chips.js'],
	['src/wicket/clipboard.ts', 'com/advanced/widgets/clipboard/wicket-clipboard.js'],
	['src/wicket/dropdown.ts', 'com/advanced/widgets/dropdown/wicket-dropdown.js'],
	['src/wicket/floating-panel.ts', 'com/advanced/widgets/floating/wicket-floating-panel.js'],
	['src/wicket/infinite-list.ts', 'com/advanced/widgets/jsonlist/json-infinite-list.js'],
	['src/wicket/movable-columns.ts', 'com/advanced/widgets/table/wicket-movable-columns.js'],
	['src/wicket/resizable-columns.ts', 'com/advanced/widgets/table/wicket-resizable-columns.js'],
	['src/wicket/scrollbar.ts', 'com/advanced/widgets/scrollbar/wicket-scrollbar.js'],
	['src/wicket/templates.ts', 'com/advanced/widgets/template/wicket-templates.js']
];

/** The style sheets: their file in styles and their path in ui-components. */
const STYLES = [
	['styles/autocomplete.css', 'com/advanced/widgets/autocomplete/wicket-autocomplete.css'],
	['styles/chips.css', 'com/advanced/widgets/chips/wicket-chips.css'],
	['styles/clipboard.css', 'com/advanced/widgets/clipboard/wicket-clipboard.css'],
	['styles/dropdown.css', 'com/advanced/widgets/dropdown/wicket-dropdown.css'],
	['styles/floating-panel.css', 'com/advanced/widgets/floating/wicket-floating-panel.css'],
	['styles/infinite-list.css', 'com/advanced/widgets/jsonlist/json-infinite-list.css'],
	['styles/movable-columns.css', 'com/advanced/widgets/table/wicket-movable-columns.css'],
	['styles/resizable-columns.css', 'com/advanced/widgets/table/wicket-resizable-columns.css'],
	['styles/scrollbar.css', 'com/advanced/widgets/scrollbar/wicket-scrollbar.css']
];

await rm(out, { recursive: true, force: true });

for (const [entry, path] of SCRIPTS) {
	await build({
		entryPoints: [join(root, entry)],
		outfile: join(out, path),
		bundle: true,
		format: 'iife',
		target: 'es2020',
		charset: 'utf8',
		legalComments: 'none',
		banner: { js: BANNER },
		logLevel: 'warning'
	});
}

for (const [file, path] of STYLES) {
	await mkdir(dirname(join(out, path)), { recursive: true });
	await copyFile(join(root, file), join(out, path));
}

console.log(`Built ${SCRIPTS.length} scripts and ${STYLES.length} style sheets for Wicket into ${out}`);
