/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Lets the user move a floating panel by dragging its title bar, with the mouse, a pen or a finger.
 * Once FloatingPanel.install() ran, a panel inside an element marked data-antilia-floating-draggable
 * can be moved; it is shifted by the CSS translate property, its title bar kept inside the browser
 * window. A panel shown in a dialog, such as the overlay window of a dynamic data table, moves the
 * dialog, so the dialog does not clip it.
 */

const HEADER = '[data-antilia-floating-draggable] .antilia-floating-panel-header';

const DRAGGING_CLASS = 'antilia-floating-panel-dragging';

function offsetOf(element: HTMLElement): { x: number; y: number } {
	const parts = (element.style.translate || '').split(' ');
	return {
		x: parseFloat(parts[0]) || 0,
		y: parseFloat(parts[1]) || 0
	};
}

function clamp(value: number, min: number, max: number): number {
	return Math.min(Math.max(value, min), Math.max(min, max));
}

let installed = 0;

export const FloatingPanel = {

	/**
	 * @param panel the element with the class antilia-floating-panel
	 * @returns the element moved when the panel is dragged: the dialog the panel is shown in, if
	 *          any, else the panel
	 */
	movedElement(panel: HTMLElement): HTMLElement {
		return panel.closest<HTMLElement>('[role="dialog"]') || panel;
	},

	/**
	 * Moves a panel by a distance from where it is, keeping its title bar inside the browser
	 * window.
	 *
	 * @param panel the element with the class antilia-floating-panel
	 * @param dx the horizontal distance, in pixels
	 * @param dy the vertical distance, in pixels
	 */
	moveBy(panel: HTMLElement, dx: number, dy: number): void {
		const moved = FloatingPanel.movedElement(panel);
		const offset = offsetOf(moved);
		const rect = moved.getBoundingClientRect();
		const header = panel.querySelector('.antilia-floating-panel-header');
		const headerHeight = header ? header.getBoundingClientRect().bottom - rect.top :
			rect.height;
		const root = document.compatMode === 'BackCompat' ? document.body :
			document.documentElement;
		const x = clamp(rect.left + dx, 0, root.clientWidth - rect.width) - rect.left;
		const y = clamp(rect.top + dy, 0, root.clientHeight - headerHeight) - rect.top;
		moved.style.translate = (offset.x + x) + 'px ' + (offset.y + y) + 'px';
	},

	onPointerDown(event: PointerEvent): void {
		const header = (event.target as Element | null)?.closest?.<HTMLElement>(HEADER);
		if (!header || event.button !== 0) {
			return;
		}
		const panel = header.closest<HTMLElement>('.antilia-floating-panel');
		if (!panel) {
			return;
		}
		let lastX = event.clientX;
		let lastY = event.clientY;
		event.preventDefault();
		header.setPointerCapture?.(event.pointerId);
		panel.classList.add(DRAGGING_CLASS);

		const move = (moveEvent: PointerEvent) => {
			FloatingPanel.moveBy(panel, moveEvent.clientX - lastX, moveEvent.clientY - lastY);
			lastX = moveEvent.clientX;
			lastY = moveEvent.clientY;
		};
		const end = () => {
			panel.classList.remove(DRAGGING_CLASS);
			header.removeEventListener('pointermove', move);
			header.removeEventListener('pointerup', end);
			header.removeEventListener('pointercancel', end);
			header.removeEventListener('lostpointercapture', end);
		};
		header.addEventListener('pointermove', move);
		header.addEventListener('pointerup', end);
		header.addEventListener('pointercancel', end);
		header.addEventListener('lostpointercapture', end);
	},

	/**
	 * Makes the floating panels in elements marked data-antilia-floating-draggable movable.
	 * Installing again only counts; the listener is removed when every installation was undone.
	 *
	 * @returns undoes this installation
	 */
	install(): () => void {
		if (installed++ === 0) {
			document.addEventListener('pointerdown', FloatingPanel.onPointerDown);
		}
		let undone = false;
		return () => {
			if (!undone && (undone = true) && --installed === 0) {
				document.removeEventListener('pointerdown', FloatingPanel.onPointerDown);
			}
		};
	}
};
