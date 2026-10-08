/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * react-demo.js: renders the React demo into every element with data-framework-demo="react".
 */

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { settingsOf } from './data.js';
import { ReactDemo } from './ReactDemo.js';

document.querySelectorAll<HTMLElement>('[data-framework-demo="react"]').forEach((element) => {
	const { url, texts } = settingsOf(element);
	createRoot(element).render(<StrictMode><ReactDemo url={url} texts={texts} /></StrictMode>);
});
