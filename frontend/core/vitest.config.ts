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

import { defineConfig } from 'vitest/config';

export default defineConfig({
	// the tests of the veil read its script, the copy of Wicket's in ui-components, as the page
	// loads it
	server: {
		fs: { allow: ['..', '../../ui-components/src/main/java/com/antiliacode/widgets/veil'] }
	},
	test: {
		environment: 'jsdom',
		globals: true,
		include: ['test/**/*.test.ts'],
		// the style sheets a test reads as text (?raw), which Vitest gives as empty otherwise
		css: { include: [/page-loading-veil/] },
		// every run measures the coverage of the sources: a summary on the console, the full
		// report in coverage/ (index.html, and lcov.info for other tools); the run fails when the
		// coverage drops below the thresholds, a little under what the tests reach today
		coverage: {
			enabled: true,
			provider: 'v8',
			include: ['src/**/*.ts'],
			reporter: ['text-summary', 'html', 'lcov'],
			reportsDirectory: 'coverage',
			thresholds: { statements: 80, branches: 65, functions: 80, lines: 80 }
		}
	}
});
