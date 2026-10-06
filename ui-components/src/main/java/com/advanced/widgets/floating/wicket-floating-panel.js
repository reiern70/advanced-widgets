/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
/*
 * Lets the user move a floating panel by dragging its title bar, with the mouse, a pen or a finger.
 * A panel inside an element marked data-wicket-floating-draggable can be moved; it is shifted by
 * the CSS translate property, its title bar kept inside the browser window. A panel shown in a
 * dialog, such as the overlay window of a dynamic data table, moves the dialog, so the dialog does
 * not clip it.
 */
;(function (undefined) {
	'use strict';

	if (typeof(Wicket) === "undefined") {
		window.Wicket = { };
	}

	if (typeof(Wicket.FloatingPanel) !== "undefined") {
		return;
	}

	const HEADER = '[data-wicket-floating-draggable] .wicket-floating-panel-header';

	const DRAGGING_CLASS = 'wicket-floating-panel-dragging';

	const offsetOf = function (panel) {
		const parts = (panel.style.translate || '').split(' ');
		return {
			x: parseFloat(parts[0]) || 0,
			y: parseFloat(parts[1]) || 0
		};
	};

	const clamp = function (value, min, max) {
		return Math.min(Math.max(value, min), Math.max(min, max));
	};

	Wicket.FloatingPanel = {

		/**
		 * @param panel {Element} - the element with the class wicket-floating-panel
		 * @return the element moved when the panel is dragged: the dialog the panel is shown in,
		 *         if any, else the panel
		 */
		movedElement: function (panel) {
			return panel.closest('[role="dialog"]') || panel;
		},

		/**
		 * Moves a panel by a distance from where it is, keeping its title bar inside the browser
		 * window.
		 *
		 * @param panel {Element} - the element with the class wicket-floating-panel
		 * @param dx {Number} - the horizontal distance, in pixels
		 * @param dy {Number} - the vertical distance, in pixels
		 */
		moveBy: function (panel, dx, dy) {
			const moved = Wicket.FloatingPanel.movedElement(panel);
			const offset = offsetOf(moved);
			const rect = moved.getBoundingClientRect();
			const header = panel.querySelector('.wicket-floating-panel-header');
			const headerHeight = header ? header.getBoundingClientRect().bottom - rect.top :
				rect.height;
			const root = document.compatMode === 'BackCompat' ? document.body :
				document.documentElement;
			const viewWidth = root.clientWidth;
			const viewHeight = root.clientHeight;
			const x = clamp(rect.left + dx, 0, viewWidth - rect.width) - rect.left;
			const y = clamp(rect.top + dy, 0, viewHeight - headerHeight) - rect.top;
			moved.style.translate = (offset.x + x) + 'px ' + (offset.y + y) + 'px';
		},

		onPointerDown: function (event) {
			const header = event.target && event.target.closest && event.target.closest(HEADER);
			if (!header || event.button !== 0) {
				return;
			}
			const panel = header.closest('.wicket-floating-panel');
			let lastX = event.clientX;
			let lastY = event.clientY;
			event.preventDefault();
			header.setPointerCapture(event.pointerId);
			panel.classList.add(DRAGGING_CLASS);

			const move = function (moveEvent) {
				Wicket.FloatingPanel.moveBy(panel, moveEvent.clientX - lastX,
					moveEvent.clientY - lastY);
				lastX = moveEvent.clientX;
				lastY = moveEvent.clientY;
			};
			const end = function () {
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
		}
	};

	document.addEventListener('pointerdown', Wicket.FloatingPanel.onPointerDown);
})();
