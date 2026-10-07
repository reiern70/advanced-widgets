/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Scroll bars that look the same in every browser: the native scroll bars of an element are
 * hidden and slim bars of its own, a track with a thumb in the colors of the theme, are drawn over
 * its edges. The element keeps scrolling natively, with the wheel, the keys and touch; the thumb
 * can be dragged and a click on the track pages towards the click.
 *
 * Wicket.Scrollbar.attach(elementOrId, options) puts the element into a wrapper, a
 * div.wicket-scrollbar holding the bars, which do not scroll with the content. Attaching an element
 * that is in a wrapper already, for example one Ajax put in place of the previous one, reuses it.
 */
;(function (undefined) {
	'use strict';

	if (typeof(Wicket) === "undefined") {
		window.Wicket = { };
	}

	if (typeof(Wicket.Scrollbar) !== "undefined") {
		return;
	}

	const WRAPPER_CLASS = 'wicket-scrollbar';

	const HOST_CLASS = 'wicket-scrollbar-host';

	const TRACK_CLASS = 'wicket-scrollbar-track';

	const THUMB_CLASS = 'wicket-scrollbar-thumb';

	const MIN_THUMB = 24;

	const scrolls = function (overflow) {
		return overflow === 'auto' || overflow === 'scroll';
	};

	/**
	 * The scroll bars of an element.
	 *
	 * @param element the scrolling element
	 * @param options {axis}, axis being 'x', 'y' or 'both'; by default the axes along which the
	 *            CSS of the element lets it scroll
	 */
	Wicket.Scrollbar = function (element, options) {
		this.element = element;
		this.options = options || { };
		this.attach();
	};

	Wicket.Scrollbar.prototype = {

		attach : function () {
			const element = this.element;
			let wrapper = element.parentElement;
			if (!wrapper || !wrapper.classList.contains(WRAPPER_CLASS)) {
				wrapper = document.createElement('div');
				wrapper.className = WRAPPER_CLASS;
				element.parentNode.insertBefore(wrapper, element);
				wrapper.appendChild(element);
			}
			Array.prototype.slice.call(wrapper.children).forEach(function (child) {
				if (child.classList.contains(TRACK_CLASS)) {
					child.remove();
				}
			});
			this.wrapper = wrapper;
			element.classList.add(HOST_CLASS);

			const style = window.getComputedStyle(element);
			const axis = this.options.axis;
			this.bars = [];
			if (axis === 'y' || axis === 'both' || (!axis && scrolls(style.overflowY))) {
				this.bars.push(this.newBar(true));
			}
			if (axis === 'x' || axis === 'both' || (!axis && scrolls(style.overflowX))) {
				this.bars.push(this.newBar(false));
			}
			wrapper.classList.toggle(WRAPPER_CLASS + '--both', this.bars.length === 2);

			this.scrollHandler = Wicket.bind(this.update, this);
			element.addEventListener('scroll', this.scrollHandler, { passive: true });
			const self = this;
			if (window.ResizeObserver) {
				this.resizeObserver = new window.ResizeObserver(function () {
					self.update();
				});
				this.observeSizes();
			}
			if (window.MutationObserver) {
				this.mutationObserver = new window.MutationObserver(function () {
					self.observeSizes();
					self.update();
				});
				this.mutationObserver.observe(element, { childList: true });
			}
			Wicket.Scrollbar.instances.push(this);
			this.update();
		},

		/**
		 * Watches the size of the element and of its children, whose sizes make the size of what
		 * it scrolls.
		 */
		observeSizes : function () {
			const observer = this.resizeObserver;
			if (!observer) {
				return;
			}
			observer.disconnect();
			observer.observe(this.element);
			Array.prototype.forEach.call(this.element.children, function (child) {
				observer.observe(child);
			});
		},

		newBar : function (vertical) {
			const track = document.createElement('div');
			track.className = TRACK_CLASS + ' ' + TRACK_CLASS + (vertical ? '--y' : '--x');
			track.setAttribute('aria-hidden', 'true');
			const thumb = document.createElement('div');
			thumb.className = THUMB_CLASS;
			track.appendChild(thumb);
			this.wrapper.appendChild(track);
			const bar = { vertical: vertical, track: track, thumb: thumb };
			track.addEventListener('pointerdown', Wicket.bind(function (event) {
				this.onPointerDown(bar, event);
			}, this));
			return bar;
		},

		/**
		 * Sizes and places the thumbs for the current scroll position, and hides the bar of an
		 * axis along which nothing scrolls.
		 */
		update : function () {
			const element = this.element;
			if (!element.isConnected) {
				this.destroy();
				return;
			}
			this.bars.forEach(function (bar) {
				const client = bar.vertical ? element.clientHeight : element.clientWidth;
				const size = bar.vertical ? element.scrollHeight : element.scrollWidth;
				const position = bar.vertical ? element.scrollTop : element.scrollLeft;
				const overflows = size - client > 1;
				bar.track.hidden = !overflows;
				if (!overflows) {
					return;
				}
				const length = bar.vertical ? bar.track.clientHeight : bar.track.clientWidth;
				const thumb = Math.min(length, Math.max(MIN_THUMB, length * client / size));
				const offset = (length - thumb) * position / (size - client);
				bar.range = { length: length, thumb: thumb, scroll: size - client };
				if (bar.vertical) {
					bar.thumb.style.height = thumb + 'px';
					bar.thumb.style.transform = 'translateY(' + offset + 'px)';
				} else {
					bar.thumb.style.width = thumb + 'px';
					bar.thumb.style.transform = 'translateX(' + offset + 'px)';
				}
			});
		},

		/**
		 * Starts dragging the thumb, or pages towards a click on the track.
		 */
		onPointerDown : function (bar, event) {
			if (event.button !== 0 || !bar.range) {
				return;
			}
			event.preventDefault();
			const element = this.element;
			const vertical = bar.vertical;
			if (event.target !== bar.thumb) {
				const rect = bar.thumb.getBoundingClientRect();
				const before = vertical ? event.clientY < rect.top : event.clientX < rect.left;
				const page = (vertical ? element.clientHeight : element.clientWidth) * 0.9;
				element.scrollBy({
					top: vertical ? (before ? -page : page) : 0,
					left: vertical ? 0 : (before ? -page : page),
					behavior: 'smooth'
				});
				return;
			}
			const start = vertical ? event.clientY : event.clientX;
			const from = vertical ? element.scrollTop : element.scrollLeft;
			const ratio = bar.range.scroll / Math.max(1, bar.range.length - bar.range.thumb);
			const thumb = bar.thumb;
			const wrapper = this.wrapper;
			thumb.setPointerCapture(event.pointerId);
			wrapper.classList.add(WRAPPER_CLASS + '--dragging');
			const move = function (moveEvent) {
				const delta = ((vertical ? moveEvent.clientY : moveEvent.clientX) - start) * ratio;
				if (vertical) {
					element.scrollTop = from + delta;
				} else {
					element.scrollLeft = from + delta;
				}
			};
			const end = function () {
				thumb.removeEventListener('pointermove', move);
				thumb.removeEventListener('pointerup', end);
				thumb.removeEventListener('pointercancel', end);
				wrapper.classList.remove(WRAPPER_CLASS + '--dragging');
			};
			thumb.addEventListener('pointermove', move);
			thumb.addEventListener('pointerup', end);
			thumb.addEventListener('pointercancel', end);
		},

		/**
		 * Removes the bars, giving the element its native scroll bars back; the element stays in
		 * the wrapper.
		 */
		destroy : function () {
			if (this.destroyed) {
				return;
			}
			this.destroyed = true;
			this.element.removeEventListener('scroll', this.scrollHandler);
			if (this.resizeObserver) {
				this.resizeObserver.disconnect();
			}
			if (this.mutationObserver) {
				this.mutationObserver.disconnect();
			}
			this.bars.forEach(function (bar) {
				bar.track.remove();
			});
			this.element.classList.remove(HOST_CLASS);
			const instances = Wicket.Scrollbar.instances;
			const index = instances.indexOf(this);
			if (index >= 0) {
				instances.splice(index, 1);
			}
		}
	};

	/**
	 * The live scroll bars.
	 */
	Wicket.Scrollbar.instances = [];

	/**
	 * Gives an element the scroll bars of this script, replacing those it had already.
	 *
	 * @param element the element or its id
	 * @param options see Wicket.Scrollbar
	 * @return the scroll bars, or nothing if there is no such element
	 */
	Wicket.Scrollbar.attach = function (element, options) {
		const target = typeof element === 'string' ? Wicket.$(element) : element;
		if (!target) {
			return undefined;
		}
		Wicket.Scrollbar.detach(target);
		return new Wicket.Scrollbar(target, options);
	};

	/**
	 * Removes the scroll bars of this script from an element, if it has them.
	 *
	 * @param element the element
	 */
	Wicket.Scrollbar.detach = function (element) {
		Wicket.Scrollbar.instances.slice().forEach(function (instance) {
			if (instance.element === element) {
				instance.destroy();
			}
		});
	};

	if (Wicket.Event && Wicket.Event.subscribe) {
		Wicket.Event.subscribe(Wicket.Event.Topic.DOM_NODE_REMOVING, function (jqEvent, node) {
			Wicket.Scrollbar.instances.slice().forEach(function (instance) {
				if (node === instance.element || (node.contains && node.contains(instance.element))) {
					instance.destroy();
				}
			});
		});
	}
})();
