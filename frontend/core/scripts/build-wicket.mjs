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
	['src/wicket/dropdown.ts', 'com/advanced/widgets/dropdown/wicket-dropdown.js']
];

/** The style sheets: their file in styles and their path in ui-components. */
const STYLES = [
	['styles/dropdown.css', 'com/advanced/widgets/dropdown/wicket-dropdown.css']
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
