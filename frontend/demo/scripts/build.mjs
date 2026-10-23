/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
 */

import react from '@vitejs/plugin-react';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { rm } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { build } from 'vite';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const out = join(root, 'dist', 'com', 'antiliacode', 'widgets', 'demo', 'frameworks');

const BANNER = [
	'/*',
	' * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)',
	' *',
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

console.log(`Built ${DEMOS.map((demo) => demo.name + '.js').join(' and ')} into ${out}`);
