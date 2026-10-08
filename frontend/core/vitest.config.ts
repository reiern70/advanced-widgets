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
		include: ['test/**/*.test.ts']
	}
});
