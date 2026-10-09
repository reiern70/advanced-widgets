/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
const out = join(root, 'dist', 'com', 'antilia', 'widgets', 'demo', 'frameworks');

const BANNER = '/*\n * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).\n' +
	' * All rights reserved.\n *\n * Built from frontend/demo, do not edit.\n */';

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
