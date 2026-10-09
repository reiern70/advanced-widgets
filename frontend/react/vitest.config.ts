/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { defineConfig } from 'vitest/config';

export default defineConfig({
	// the tests read the license key of the demo application, outside the frontend
	server: { fs: { allow: ['../..'] } },
	test: {
		environment: 'jsdom',
		globals: true,
		include: ['test/**/*.test.tsx'],
		// every run measures the coverage of the sources: a summary on the console, the full
		// report in coverage/ (index.html, and lcov.info for other tools); the run fails when the
		// coverage drops below the thresholds, a little under what the tests reach today
		coverage: {
			enabled: true,
			provider: 'v8',
			include: ['src/**/*.{ts,tsx}'],
			reporter: ['text-summary', 'html', 'lcov'],
			reportsDirectory: 'coverage',
			thresholds: { statements: 90, branches: 85, functions: 85, lines: 90 }
		}
	}
});
