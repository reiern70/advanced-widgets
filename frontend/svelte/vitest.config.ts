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

import { svelte } from '@sveltejs/vite-plugin-svelte';
import { svelteTesting } from '@testing-library/svelte/vite';
import { defineConfig } from 'vitest/config';

export default defineConfig({
	// the tests read the license key of the demo application, outside the frontend
	server: { fs: { allow: ['../..'] } },
	plugins: [svelte(), svelteTesting()],
	test: {
		environment: 'jsdom',
		globals: true,
		// the private components check their license keys, which are not enforced for now
		setupFiles: ['test/setup.ts'],
		include: ['test/**/*.test.ts'],
		// every run measures the coverage of the sources: a summary on the console, the full
		// report in coverage/ (index.html, and lcov.info for other tools); the run fails when the
		// coverage drops below the thresholds, a little under what the tests reach today
		coverage: {
			enabled: true,
			provider: 'v8',
			include: ['src/**/*.{ts,svelte}'],
			reporter: ['text-summary', 'html', 'lcov'],
			reportsDirectory: 'coverage',
			thresholds: { statements: 90, branches: 75, functions: 90, lines: 90 }
		}
	}
});
