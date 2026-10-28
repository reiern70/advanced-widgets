/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import { copyFile, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const out = join(root, 'dist', 'wicket');

const BANNER = [
	'/*',
	' * Licensed under the Apache License, Version 2.0 (the "License");',
	' * you may not use this file except in compliance with the License.',
	' * You may obtain a copy of the License at',
	' *',
	' *     http://www.apache.org/licenses/LICENSE-2.0',
	' *',
	' * Unless required by applicable law or agreed to in writing, software',
	' * distributed under the License is distributed on an "AS IS" BASIS,',
	' * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.',
	' * See the License for the specific language governing permissions and',
	' * limitations under the License.',
	' *',
	' * Built from frontend/core, do not edit.',
	' */'
].join('\n');

/** The scripts: their entry in src/wicket and their path in ui-components. */
const SCRIPTS = [
	['src/wicket/autocomplete.ts', 'com/antiliacode/widgets/autocomplete/antilia-autocomplete.js'],
	['src/wicket/breadcrumb.ts', 'com/antiliacode/widgets/breadcrumb/antilia-breadcrumb.js'],
	['src/wicket/button.ts', 'com/antiliacode/widgets/button/antilia-button.js'],
	['src/wicket/chips.ts', 'com/antiliacode/widgets/chips/antilia-chips.js'],
	['src/wicket/ck-editor.ts', 'com/antiliacode/widgets/ckeditor/antilia-ckeditor.js'],
	['src/wicket/clipboard.ts', 'com/antiliacode/widgets/clipboard/antilia-clipboard.js'],
	['src/wicket/code-editor.ts', 'com/antiliacode/widgets/codeeditor/antilia-code-editor.js'],
	['src/wicket/date-picker.ts', 'com/antiliacode/widgets/datepicker/antilia-date-picker.js'],
	['src/wicket/data-table.ts', 'com/antiliacode/widgets/dynamicdatatable/ant-data-table.js'],
	['src/wicket/desktop.ts', 'com/antiliacode/widgets/desktop/antilia-desktop.js'],
	['src/wicket/dropdown.ts', 'com/antiliacode/widgets/dropdown/antilia-dropdown.js'],
	['src/wicket/floating-panel.ts', 'com/antiliacode/widgets/floating/antilia-floating-panel.js'],
	['src/wicket/handlebars.ts', 'com/antiliacode/widgets/dynamicdatatable/handlebars-adapter.js'],
	['src/wicket/infinite-list.ts', 'com/antiliacode/widgets/jsonlist/json-infinite-list.js'],
	['src/wicket/menu.ts', 'com/antiliacode/widgets/menu/antilia-menu.js'],
	['src/wicket/modal.ts', 'com/antiliacode/widgets/modal/antilia-modal.js'],
	['src/wicket/movable-columns.ts', 'com/antiliacode/widgets/table/antilia-movable-columns.js'],
	['src/wicket/notifications.ts', 'com/antiliacode/widgets/notification/antilia-notifications.js'],
	['src/wicket/palette.ts', 'com/antiliacode/widgets/palette/antilia-palette.js'],
	['src/wicket/popover.ts', 'com/antiliacode/widgets/popover/antilia-popover.js'],
	['src/wicket/resizable-columns.ts', 'com/antiliacode/widgets/table/antilia-resizable-columns.js'],
	['src/wicket/resizable.ts', 'com/antiliacode/widgets/resizable/antilia-resizable.js'],
	['src/wicket/rich-text-editor.ts', 'com/antiliacode/widgets/richtext/antilia-rich-text-editor.js'],
	['src/wicket/rich-text-highlight.ts',
		'com/antiliacode/widgets/richtext/antilia-rich-text-highlight.js'],
	['src/wicket/scrollbar.ts', 'com/antiliacode/widgets/scrollbar/antilia-scrollbar.js'],
	['src/wicket/split-panel.ts', 'com/antiliacode/widgets/split/antilia-split-panel.js'],
	['src/wicket/tabs.ts', 'com/antiliacode/widgets/tabs/antilia-tabs.js'],
	['src/wicket/templates.ts', 'com/antiliacode/widgets/template/antilia-templates.js'],
	['src/wicket/tree.ts', 'com/antiliacode/widgets/tree/json-tree.js'],
	['src/wicket/upload.ts', 'com/antiliacode/widgets/upload/antilia-upload.js']
];

/** The style sheets and the images they use: their file in styles and their path in ui-components. */
const STYLES = [
	['styles/autocomplete.css', 'com/antiliacode/widgets/autocomplete/antilia-autocomplete.css'],
	['styles/breadcrumb.css', 'com/antiliacode/widgets/breadcrumb/antilia-breadcrumb.css'],
	['styles/button.css', 'com/antiliacode/widgets/button/antilia-button.css'],
	['styles/chips.css', 'com/antiliacode/widgets/chips/antilia-chips.css'],
	['styles/ck-editor.css', 'com/antiliacode/widgets/ckeditor/antilia-ckeditor.css'],
	['styles/clipboard.css', 'com/antiliacode/widgets/clipboard/antilia-clipboard.css'],
	['styles/code-editor.css', 'com/antiliacode/widgets/codeeditor/antilia-code-editor.css'],
	['styles/data-table.css', 'com/antiliacode/widgets/dynamicdatatable/ant-data-table.css'],
	['styles/data-table-check.svg', 'com/antiliacode/widgets/dynamicdatatable/data-table-check.svg'],
	['styles/date-picker.css', 'com/antiliacode/widgets/datepicker/antilia-date-picker.css'],
	['styles/desktop.css', 'com/antiliacode/widgets/desktop/antilia-desktop.css'],
	['styles/dropdown.css', 'com/antiliacode/widgets/dropdown/antilia-dropdown.css'],
	['styles/floating-panel.css', 'com/antiliacode/widgets/floating/antilia-floating-panel.css'],
	['styles/infinite-list.css', 'com/antiliacode/widgets/jsonlist/json-infinite-list.css'],
	['styles/menu.css', 'com/antiliacode/widgets/menu/antilia-menu.css'],
	['styles/modal.css', 'com/antiliacode/widgets/modal/antilia-modal.css'],
	['styles/movable-columns.css', 'com/antiliacode/widgets/table/antilia-movable-columns.css'],
	['styles/notifications.css', 'com/antiliacode/widgets/notification/antilia-notifications.css'],
	['styles/palette.css', 'com/antiliacode/widgets/palette/antilia-palette.css'],
	['styles/popover.css', 'com/antiliacode/widgets/popover/antilia-popover.css'],
	['styles/progress-bar.css', 'com/antiliacode/widgets/progress/antilia-progress-bar.css'],
	['styles/resizable.css', 'com/antiliacode/widgets/resizable/antilia-resizable.css'],
	['styles/resizable-columns.css', 'com/antiliacode/widgets/table/antilia-resizable-columns.css'],
	['styles/rich-text-editor.css', 'com/antiliacode/widgets/richtext/antilia-rich-text-editor.css'],
	['styles/scrollbar.css', 'com/antiliacode/widgets/scrollbar/antilia-scrollbar.css'],
	['styles/split-panel.css', 'com/antiliacode/widgets/split/antilia-split-panel.css'],
	['styles/tabs.css', 'com/antiliacode/widgets/tabs/antilia-tabs.css'],
	['styles/tree.css', 'com/antiliacode/widgets/tree/antilia-tree.css'],
	['styles/upload.css', 'com/antiliacode/widgets/upload/antilia-upload.css']
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
const looks = join(out, 'com/antiliacode/widgets/theme/antilia-looks.properties');
await mkdir(dirname(looks), { recursive: true });
await writeFile(looks, ['# Built from frontend/core/src/look.ts, do not edit.',
	...Object.entries(LOOK_PARTS).map(([part, aspect]) => `aspect.${part}=${aspect}`),
	...[ANTILIA_LOOK, BOOTSTRAP_LOOK].flatMap((look) => Object.entries(look.classes)
		.map(([part, classes]) => `${look.name}.${part}=${classes}`))].join('\n') + '\n');

// the library the rich text editor is based on, as the bundle holds it, for the page to tell
const require = createRequire(join(root, 'package.json'));
const version = async (name) => {
	let folder = dirname(require.resolve(name));
	while (folder !== dirname(folder)) {
		try {
			const found = JSON.parse(await readFile(join(folder, 'package.json'), 'utf8'));
			if (found.name === name) {
				return found.version;
			}
		} catch {
			// no package.json here: one folder up
		}
		folder = dirname(folder);
	}
	throw new Error('No version of ' + name);
};
const library = join(out, 'com/antiliacode/widgets/richtext/antilia-rich-text-library.properties');
await writeFile(library, ['# Built from frontend/core/package.json, do not edit.',
	'name=Tiptap', `version=${await version('@tiptap/core')}`, 'url=https://tiptap.dev']
	.join('\n') + '\n');

console.log(`Built ${SCRIPTS.length} scripts and ${STYLES.length} style sheets for Wicket into ${out}`);
