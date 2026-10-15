/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { cleanup, render } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';

import { BOOTSTRAP_LOOK, LookProvider, MarkdownPreview } from '../src/index.js';

function preview(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-markdown-preview')!;
}

afterEach(cleanup);

describe('MarkdownPreview', () => {
	it('renders the text, a region by its label, that takes the focus', () => {
		const { container } = render(<MarkdownPreview id="p" className="notes" value="# Notes"
			label="Preview" />);
		const element = preview(container);

		expect(element.id).toBe('p');
		expect(element.className).toBe('antilia-markdown-preview notes');
		expect(element.getAttribute('role')).toBe('region');
		expect(element.getAttribute('aria-label')).toBe('Preview');
		expect(element.tabIndex).toBe(0);
		expect(element.innerHTML).toBe('<h1>Notes</h1>');
	});

	it('renders the text again as it changes, and its label and placeholder', () => {
		const { container, rerender } = render(<MarkdownPreview value="*one*" />);
		const element = preview(container);
		expect(element.hasAttribute('role')).toBe(false);

		rerender(<MarkdownPreview value="**two**" className="more" />);
		expect(element.innerHTML).toBe('<p><strong>two</strong></p>');
		expect(element.className).toBe('antilia-markdown-preview more');

		rerender(<MarkdownPreview value="" placeholder="Nothing yet" label="Notes" />);
		expect(element.innerHTML).toBe('<p class="antilia-markdown-preview-empty">Nothing yet</p>');
		expect(element.getAttribute('aria-label')).toBe('Notes');

		rerender(<MarkdownPreview value="" />);
		expect(element.innerHTML).toBe('');
		expect(element.hasAttribute('aria-label')).toBe(false);
	});

	it('gives its tables the classes of the look', () => {
		const { container } = render(<LookProvider look={BOOTSTRAP_LOOK}>
			<MarkdownPreview value={'| a |\n|---|\n| b |'} /></LookProvider>);

		expect(preview(container).querySelector('table')!.className).toBe(
			'antilia-markdown-preview-table table table-sm');
	});
});
