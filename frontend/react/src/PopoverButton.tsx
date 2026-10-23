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

import { useEffect, useLayoutEffect, useRef, useState, type ButtonHTMLAttributes,
	type MouseEvent, type ReactNode } from 'react';
import { createPortal } from 'react-dom';

import { Popover, type PopoverOptions } from '@antilia/core';

import { useLook } from './Look.js';

export interface PopoverButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>,
	'content'> {
	/** What the popover shows besides the content, and where and how: the core's options. */
	options?: Omit<PopoverOptions, 'content'>;
	/** What the popover shows after its body: any React nodes, a form for example. */
	content?: ReactNode;
	/** Called as the popover opened. */
	onOpen?: (popover: Popover) => void;
	/** The button: an icon, a text or both. */
	children?: ReactNode;
}

/**
 * A button opening a popover pointing at it on click, the core's Popover, and closing it on a
 * second click. Its content is rendered by React into the popover through a portal while it is
 * open, events and context working as anywhere in the app. The popover is in the look of the
 * closest LookProvider, unless the options give one. The popover closes as the button goes away.
 */
export function PopoverButton({ options, content, onOpen, onClick, children, type = 'button',
	...rest }: PopoverButtonProps) {
	const look = useLook();
	const shown = useRef<Popover | null>(null);
	// where the content renders while the popover is open
	const [target, setTarget] = useState<HTMLElement | null>(null);

	useEffect(() => () => shown.current?.close(), []);

	// placed again once its content rendered
	useLayoutEffect(() => {
		if (target) {
			shown.current?.place();
		}
	}, [target]);

	const toggle = (event: MouseEvent<HTMLButtonElement>) => {
		onClick?.(event);
		if (event.defaultPrevented) {
			return;
		}
		const holder = content != null ? document.createElement('div') : null;
		const opened = Popover.toggle(event.currentTarget, { look, ...options,
			content: holder ?? undefined,
			onClose: () => {
				if (shown.current === opened) {
					shown.current = null;
					setTarget(null);
				}
				options?.onClose?.();
			} });
		shown.current = opened;
		setTarget(opened ? holder : null);
		if (opened) {
			onOpen?.(opened);
		}
	};

	return (
		<>
			<button {...rest} type={type} onClick={toggle}>{children}</button>
			{target && content != null && createPortal(content, target)}
		</>
	);
}
