/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
		include: ['test/**/*.test.ts']
	}
});
