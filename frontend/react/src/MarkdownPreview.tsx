/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useLayoutEffect, useRef, type HTMLAttributes } from 'react';

import { MarkdownPreview as CoreMarkdownPreview, partClasses } from '@antilia/core';

import { useLook } from './Look.js';

export interface MarkdownPreviewProps extends Omit<HTMLAttributes<HTMLDivElement>,
	'children' | 'placeholder'> {
	/** The text, in Markdown. */
	value: string;
	/** The accessible name of the preview, a region of the page. */
	label?: string;
	/** What the preview says while the text is empty, nothing when left out. */
	placeholder?: string;
}

/**
 * A preview of Markdown: the text as a page shows it, with GitHub's tables, task lists and
 * strikethrough, its fenced code blocks highlighted by the grammars of the code editor. The core
 * renders it, an element at a time, never parsing HTML: HTML in the text shows as text, and links
 * and pictures take only web addresses. Give it the text of a {@link CodeEditor} as the user types
 * it, and a height to scroll.
 */
export function MarkdownPreview(props: MarkdownPreviewProps) {
	const { value, label, placeholder, className, ...rest } = props;
	const element = useRef<HTMLDivElement>(null);
	const instance = useRef<CoreMarkdownPreview | null>(null);
	const look = useLook();
	const latest = useRef(props);
	latest.current = props;

	useLayoutEffect(() => {
		const current = latest.current;
		const preview = new CoreMarkdownPreview(element.current!, { value: current.value,
			label: current.label, placeholder: current.placeholder, look });
		instance.current = preview;
		return () => {
			preview.destroy();
			instance.current = null;
		};
	}, [look]);

	useLayoutEffect(() => {
		instance.current?.setValue(value);
	}, [value]);

	useLayoutEffect(() => {
		const preview = instance.current;
		if (preview && ((preview.options.label ?? '') !== (label ?? '') ||
			(preview.options.placeholder ?? '') !== (placeholder ?? ''))) {
			preview.configure({ label: label ?? '', placeholder: placeholder ?? '' });
		}
	}, [label, placeholder]);

	// the classes and the attributes the core gives the element, so that React keeps them
	return (
		<div {...rest} ref={element} tabIndex={0} role={label ? 'region' : undefined}
			aria-label={label || undefined}
			className={[partClasses(look, 'markdown-preview'), className].filter(Boolean).join(' ')} />
	);
}
