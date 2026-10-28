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
 * Builds react-demo.js and svelte-demo.js, each a script with its framework inside, into the
 * package of the demo application's FrameworkTabs: dist is a resource directory of demo-app.
 *
 * It copies CKEditor 5's browser build there too, with its style sheets, its German and Spanish
 * translations and its license, for the demo of the CKEditor component: the library holds no code
 * of CKEditor, licensed under the GPL or CKSource's commercial terms, and the demo brings it, as
 * any application does. Its version goes beside it, for the demo to tell.
 */

import react from '@vitejs/plugin-react';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { copyFile, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { build } from 'vite';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const out = join(root, 'dist', 'com', 'antiliacode', 'widgets', 'demo', 'frameworks');

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
	' * Built from frontend/demo, do not edit.',
	' */'
].join('\n');

const DEMOS = [
	{ entry: 'src/react.tsx', name: 'react-demo', plugins: [react()] },
	{ entry: 'src/svelte.ts', name: 'svelte-demo', plugins: [svelte()] }
];

await rm(join(root, 'dist'), { recursive: true, force: true });

for (const demo of DEMOS) {
	await build({
		root,
		configFile: false,
		logLevel: 'warn',
		plugins: demo.plugins,
		define: { 'process.env.NODE_ENV': '"production"' },
		build: {
			outDir: out,
			emptyOutDir: false,
			target: 'es2020',
			sourcemap: false,
			lib: {
				entry: join(root, demo.entry),
				formats: ['iife'],
				name: demo.name.replace('-', '_'),
				fileName: () => `${demo.name}.js`
			},
			rollupOptions: {
				output: { banner: BANNER }
			}
		}
	});
}

/** CKEditor's files the demo serves: their path in the package and in the demo's. */
const CKEDITOR_FILES = [
	['dist/browser/ckeditor5.umd.js', 'ckeditor5.umd.js'],
	['dist/browser/ckeditor5.css', 'ckeditor5.css'],
	['dist/browser/ckeditor5-content.css', 'ckeditor5-content.css'],
	['dist/translations/de.umd.js', 'translations/de.umd.js'],
	['dist/translations/es.umd.js', 'translations/es.umd.js'],
	['LICENSE.md', 'LICENSE.md'],
	['COPYING.GPL', 'COPYING.GPL']
];

const ckeditor = dirname(createRequire(import.meta.url).resolve('ckeditor5/package.json'));
const ckeditorOut = join(root, 'dist', 'com', 'antiliacode', 'widgets', 'demo', 'ckeditor',
	'ckeditor5');
for (const [from, to] of CKEDITOR_FILES) {
	await mkdir(dirname(join(ckeditorOut, to)), { recursive: true });
	await copyFile(join(ckeditor, from), join(ckeditorOut, to));
}
const { version } = JSON.parse(await readFile(join(ckeditor, 'package.json'), 'utf8'));
await writeFile(join(ckeditorOut, 'ckeditor5.properties'),
	'# Built from frontend/demo, do not edit.\nname=CKEditor 5\nversion=' + version +
	'\nurl=https://ckeditor.com/ckeditor-5/\n');

console.log(`Built ${DEMOS.map((demo) => demo.name + '.js').join(' and ')} into ${out}, and ` +
	`copied CKEditor ${version} into ${ckeditorOut}`);
