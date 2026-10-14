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
import { copyFile, mkdir, rm, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const out = join(root, 'dist', 'wicket');

const BANNER = '/*\n * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).\n' +
	' * All rights reserved.\n *\n * Built from frontend/core, do not edit.\n */';

/** The scripts: their entry in src/wicket and their path in ui-components. */
const SCRIPTS = [
	['src/wicket/autocomplete.ts', 'com/antilia/widgets/autocomplete/antilia-autocomplete.js'],
	['src/wicket/chips.ts', 'com/antilia/widgets/chips/antilia-chips.js'],
	['src/wicket/clipboard.ts', 'com/antilia/widgets/clipboard/antilia-clipboard.js'],
	['src/wicket/date-picker.ts', 'com/antilia/widgets/datepicker/antilia-date-picker.js'],
	['src/wicket/data-table.ts', 'com/antilia/widgets/dynamicdatatable/ant-data-table.js'],
	['src/wicket/desktop.ts', 'com/antilia/widgets/desktop/antilia-desktop.js'],
	['src/wicket/dropdown.ts', 'com/antilia/widgets/dropdown/antilia-dropdown.js'],
	['src/wicket/floating-panel.ts', 'com/antilia/widgets/floating/antilia-floating-panel.js'],
	['src/wicket/handlebars.ts', 'com/antilia/widgets/dynamicdatatable/handlebars-adapter.js'],
	['src/wicket/infinite-list.ts', 'com/antilia/widgets/jsonlist/json-infinite-list.js'],
	['src/wicket/menu.ts', 'com/antilia/widgets/menu/antilia-menu.js'],
	['src/wicket/modal.ts', 'com/antilia/widgets/modal/antilia-modal.js'],
	['src/wicket/movable-columns.ts', 'com/antilia/widgets/table/antilia-movable-columns.js'],
	['src/wicket/notifications.ts', 'com/antilia/widgets/notification/antilia-notifications.js'],
	['src/wicket/palette.ts', 'com/antilia/widgets/palette/antilia-palette.js'],
	['src/wicket/popover.ts', 'com/antilia/widgets/popover/antilia-popover.js'],
	['src/wicket/resizable-columns.ts', 'com/antilia/widgets/table/antilia-resizable-columns.js'],
	['src/wicket/scrollbar.ts', 'com/antilia/widgets/scrollbar/antilia-scrollbar.js'],
	['src/wicket/split-panel.ts', 'com/antilia/widgets/split/antilia-split-panel.js'],
	['src/wicket/tabs.ts', 'com/antilia/widgets/tabs/antilia-tabs.js'],
	['src/wicket/templates.ts', 'com/antilia/widgets/template/antilia-templates.js'],
	['src/wicket/tree.ts', 'com/antilia/widgets/tree/json-tree.js']
];

/** The style sheets and the images they use: their file in styles and their path in ui-components. */
const STYLES = [
	['styles/autocomplete.css', 'com/antilia/widgets/autocomplete/antilia-autocomplete.css'],
	['styles/button.css', 'com/antilia/widgets/button/antilia-button.css'],
	['styles/chips.css', 'com/antilia/widgets/chips/antilia-chips.css'],
	['styles/clipboard.css', 'com/antilia/widgets/clipboard/antilia-clipboard.css'],
	['styles/data-table.css', 'com/antilia/widgets/dynamicdatatable/ant-data-table.css'],
	['styles/data-table-check.svg', 'com/antilia/widgets/dynamicdatatable/data-table-check.svg'],
	['styles/date-picker.css', 'com/antilia/widgets/datepicker/antilia-date-picker.css'],
	['styles/desktop.css', 'com/antilia/widgets/desktop/antilia-desktop.css'],
	['styles/dropdown.css', 'com/antilia/widgets/dropdown/antilia-dropdown.css'],
	['styles/floating-panel.css', 'com/antilia/widgets/floating/antilia-floating-panel.css'],
	['styles/infinite-list.css', 'com/antilia/widgets/jsonlist/json-infinite-list.css'],
	['styles/menu.css', 'com/antilia/widgets/menu/antilia-menu.css'],
	['styles/modal.css', 'com/antilia/widgets/modal/antilia-modal.css'],
	['styles/movable-columns.css', 'com/antilia/widgets/table/antilia-movable-columns.css'],
	['styles/notifications.css', 'com/antilia/widgets/notification/antilia-notifications.css'],
	['styles/palette.css', 'com/antilia/widgets/palette/antilia-palette.css'],
	['styles/popover.css', 'com/antilia/widgets/popover/antilia-popover.css'],
	['styles/progress-bar.css', 'com/antilia/widgets/progress/antilia-progress-bar.css'],
	['styles/resizable-columns.css', 'com/antilia/widgets/table/antilia-resizable-columns.css'],
	['styles/scrollbar.css', 'com/antilia/widgets/scrollbar/antilia-scrollbar.css'],
	['styles/split-panel.css', 'com/antilia/widgets/split/antilia-split-panel.css'],
	['styles/tabs.css', 'com/antilia/widgets/tabs/antilia-tabs.css'],
	['styles/tree.css', 'com/antilia/widgets/tree/antilia-tree.css']
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

/*
 * The table of the looks, which Java's LookPart and Look read as properties: the aspect of every
 * part, as aspect.<part>, and the classes the looks give it, as <look>.<part>. tsc has built
 * dist/look.js before.
 */
const { ANTILIA_LOOK, BOOTSTRAP_LOOK, LOOK_PARTS } = await import(
	pathToFileURL(join(root, 'dist', 'look.js')).href);
const looks = join(out, 'com/antilia/widgets/theme/antilia-looks.properties');
await mkdir(dirname(looks), { recursive: true });
await writeFile(looks, ['# Built from frontend/core/src/look.ts, do not edit.',
	...Object.entries(LOOK_PARTS).map(([part, aspect]) => `aspect.${part}=${aspect}`),
	...[ANTILIA_LOOK, BOOTSTRAP_LOOK].flatMap((look) => Object.entries(look.classes)
		.map(([part, classes]) => `${look.name}.${part}=${classes}`))].join('\n') + '\n');

console.log(`Built ${SCRIPTS.length} scripts and ${STYLES.length} style sheets for Wicket into ${out}`);
