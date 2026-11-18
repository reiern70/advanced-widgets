/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Veils the page, or a single component, while Ajax requests are in flight.
 *
 * The veil goes up at /ajax/call/init, which Wicket's Ajax publishes within the event's handler,
 * before it throttles the request or queues it behind another one of its channel, so that a click
 * waiting its turn is veiled already; it comes down when the request is done, or once it is plain
 * that it will never run: throttled and replaced by a later one, dropped or ignored by its channel,
 * stopped by its precondition. A request Wicket's Ajax runs without that topic is veiled as it is
 * sent.
 *
 * The veil is transparent and blocks the mouse; while it covers the element a key goes to, Enter
 * and Space are dropped too, but where they type: Space in a text field, Enter in a text area. A
 * press of the mouse whose release a veil caught, the veil having come up in between, as that of
 * a field's change does when the press leaves the field, gets its click given back once the veil
 * is down, if its element is still in the page. If a request is still running after the target's
 * spinner delay, the veil gets the 'wicket-veil-busy' class, which shows a spinner; once shown,
 * the spinner stays for at least the target's minimum time, so it does not flicker. A request
 * carrying the extra parameter 'wicket_nb' is never veiled.
 *
 * A local veil can also be raised by the server, for a component it is about to update through a
 * WebSocket push: a WebSocket text message {"wicketVeil":"show","id":"<markup id>"} raises it,
 * Wicket.Veil.hide(id) - evaluated after the pushed update - or {"wicketVeil":"hide",...} lowers it.
 */
;(function (undefined) {
	'use strict';

	if (typeof(Wicket.Veil) === "object") {
		return;
	}

	const NO_VEIL_PARAMETER = 'wicket_nb';
	const VEIL_CLASS = 'wicket-veil';
	const BUSY_CLASS = 'wicket-veil-busy';
	const HOST_CLASS = 'wicket-veil-host';
	const STATIC_HOST_CLASS = 'wicket-veil-host-static';
	const WEBSOCKET_MESSAGE_TOPIC = '/websocket/message';
	const MESSAGE_PREFIX = '{"wicketVeil"';
	// how often a click a veil caught looks whether the veil is gone, in milliseconds
	const REPLAY_POLL = 50;
	// the types of input whose Space acts rather than types
	const BUTTON_INPUTS = ['button', 'submit', 'reset', 'image', 'checkbox', 'radio', 'file',
		'color', 'range'];

	let pageTarget = null;
	const localTargets = new Map();
	let subscribed = false;

	// the requests veiled since /ajax/call/init, by their attributes, and the one each throttle id
	// holds back in Wicket's throttler, as far as this knows
	const holds = new Map();
	const throttled = new Map();
	let reconcileTimer = null;

	// the press going on, the click a veil caught, to give back, and the keys whose keydown was
	// dropped, so that their keyup is too
	let press = null;
	let replay = null;
	let replayTimer = null;
	const droppedKeys = new Set();

	// the veil scrolls with the content of its host, so it is moved back over the visible part
	function follow(target) {
		const style = target.veil.style;
		const top = target.host.scrollTop;
		const left = target.host.scrollLeft;
		style.top = top ? top + 'px' : '';
		style.bottom = top ? -top + 'px' : '';
		style.left = left ? left + 'px' : '';
		style.right = left ? -left + 'px' : '';
	}

	function createTarget(id, options) {
		const target = {
			id: id,
			delay: options.delay,
			minimum: options.minimum,
			count: 0,
			raised: 0,
			host: null,
			veil: null,
			shownAt: -1,
			spinnerTimer: null,
			hideTimer: null
		};
		target.onScroll = function () {
			follow(target);
		};
		return target;
	}

	function configure(target, options) {
		target.delay = options.delay;
		target.minimum = options.minimum;
	}

	function isOptedOut(attrs) {
		const ep = attrs.ep;
		if (Array.isArray(ep)) {
			return ep.some(function (parameter) {
				return parameter && parameter.name === NO_VEIL_PARAMETER;
			});
		}
		return !!ep && typeof(ep) === "object" &&
			Object.prototype.hasOwnProperty.call(ep, NO_VEIL_PARAMETER);
	}

	function findTarget(attrs) {
		let node = attrs.event && attrs.event.target;
		if (!node || !node.isConnected) {
			node = typeof(attrs.c) === "string" ? document.getElementById(attrs.c) : null;
		}
		for (; node && node !== document; node = node.parentNode) {
			const target = node.id && localTargets.get(node.id);
			if (target) {
				return target;
			}
		}
		return pageTarget;
	}

	function dropStaleTargets() {
		for (const [id, target] of localTargets) {
			if (target.count === 0 && !document.getElementById(id)) {
				localTargets.delete(id);
			}
		}
	}

	function attach(target, host) {
		host.classList.add(HOST_CLASS);
		if (getComputedStyle(host).position === 'static') {
			host.classList.add(STATIC_HOST_CLASS);
		}
		host.appendChild(target.veil);
		host.addEventListener('scroll', target.onScroll);
		target.host = host;
		follow(target);
	}

	function detach(target) {
		target.host.classList.remove(HOST_CLASS, STATIC_HOST_CLASS);
		target.host.removeEventListener('scroll', target.onScroll);
	}

	function hide(target) {
		const clock = Wicket.Veil._clock;
		clock.clearTimeout(target.spinnerTimer);
		clock.clearTimeout(target.hideTimer);
		target.spinnerTimer = null;
		target.hideTimer = null;
		target.shownAt = -1;
		if (target.veil && target.veil.parentNode) {
			target.veil.parentNode.removeChild(target.veil);
		}
		if (target.host && target !== pageTarget) {
			detach(target);
		}
		target.veil = null;
		target.host = null;
	}

	function show(target) {
		const clock = Wicket.Veil._clock;
		if (target.hideTimer !== null) {
			if (document.body.contains(target.veil)) {
				// the previous request's spinner is still on its minimum time: carry on with it
				clock.clearTimeout(target.hideTimer);
				target.hideTimer = null;
				return;
			}
			hide(target);
		}

		const veil = document.createElement('div');
		veil.className = VEIL_CLASS;
		target.veil = veil;
		if (target === pageTarget) {
			document.body.appendChild(veil);
			target.host = document.body;
		} else {
			attach(target, document.getElementById(target.id));
		}

		target.spinnerTimer = clock.setTimeout(function () {
			target.spinnerTimer = null;
			veil.classList.add(BUSY_CLASS);
			target.shownAt = clock.now();
		}, target.delay);
	}

	function reattach(target) {
		if (target === pageTarget || !target.veil || document.body.contains(target.veil)) {
			return;
		}
		const host = document.getElementById(target.id);
		if (host) {
			detach(target);
			attach(target, host);
		}
	}

	function release(target) {
		const clock = Wicket.Veil._clock;
		if (target.shownAt < 0) {
			hide(target);
			return;
		}
		const remaining = target.minimum - (clock.now() - target.shownAt);
		if (remaining > 0) {
			// the update may have replaced the element, and the veil with it
			reattach(target);
			target.hideTimer = clock.setTimeout(function () {
				hide(target);
			}, remaining);
		} else {
			hide(target);
		}
	}

	function acquire(target) {
		target.count++;
		if (target.count === 1) {
			show(target);
		}
	}

	function releaseOne(target) {
		if (target.count > 0) {
			target.count--;
			if (target.count === 0) {
				release(target);
			}
		}
	}

	function lowerAll() {
		const targets = Array.from(localTargets.values());
		if (pageTarget !== null) {
			targets.push(pageTarget);
		}
		targets.forEach(function (target) {
			target.count = 0;
			target.raised = 0;
			hide(target);
		});
	}

	function parseChannel(channel) {
		const parsed = typeof(channel) === "string" ? /^([^|]+)\|(d|s|a)$/.exec(channel) : null;
		return parsed ? { name: parsed[1], type: parsed[2] } : { name: '0', type: 's' };
	}

	function throttleOf(attrs) {
		const settings = attrs.tr;
		return settings && settings.id !== undefined && settings.id !== null ?
			String(settings.id) : null;
	}

	// whether a request of the channel runs, other requests then waiting their turn
	function isBusy(name) {
		const manager = Wicket.channelManager;
		const channel = manager && manager.channels && manager.channels[name];
		return !!channel && channel.busy === true;
	}

	// whether Wicket's throttler holds a request back under the id
	function isThrottling(id) {
		const entries = Wicket.Throttler && Wicket.Throttler.entries;
		return !!entries && entries[id] !== undefined;
	}

	function inThrottler(hold) {
		return hold.throttle !== null && throttled.get(hold.throttle) === hold &&
			isThrottling(hold.throttle);
	}

	// lets a request's veil down: it is done, or will never run
	function end(hold) {
		if (holds.get(hold.attrs) !== hold) {
			return;
		}
		holds.delete(hold.attrs);
		if (hold.throttle !== null && throttled.get(hold.throttle) === hold) {
			throttled.delete(hold.throttle);
		}
		delete hold.attrs.wicketVeil;
		releaseOne(hold.target);
	}

	// lets down the veils of the requests that will never run: those no channel runs, nor queues,
	// nor the throttler holds back, and those Wicket's Ajax started but neither sent nor finished,
	// which it does at once, so that an error stopped them
	function reconcile() {
		reconcileTimer = null;
		for (const hold of Array.from(holds.values())) {
			if (hold.state === 'sent' || inThrottler(hold) ||
				hold.state === 'waiting' && isBusy(hold.channel)) {
				continue;
			}
			end(hold);
		}
	}

	function reconcileLater() {
		if (reconcileTimer === null) {
			reconcileTimer = Wicket.Veil._clock.setTimeout(reconcile, 0);
		}
	}

	function onInit(jqEvent, attrs) {
		if (!attrs || typeof(attrs) !== "object") {
			return;
		}
		const throttle = throttleOf(attrs);
		if (throttle !== null) {
			// the throttler keeps one request per id: the one it holds back is replaced by this one
			const previous = throttled.get(throttle);
			if (previous && isThrottling(throttle)) {
				end(previous);
			}
			throttled.delete(throttle);
		}
		if (isOptedOut(attrs)) {
			return;
		}
		dropStaleTargets();
		const target = findTarget(attrs);
		if (target === null) {
			return;
		}
		const channel = parseChannel(attrs.ch);
		if (throttle === null && isBusy(channel.name)) {
			if (channel.type === 'a') {
				// an active channel ignores a request while another one runs
				return;
			}
			if (channel.type === 'd') {
				// a drop channel forgets the requests waiting their turn
				for (const hold of Array.from(holds.values())) {
					if (hold.channel === channel.name && hold.state === 'waiting' &&
						!inThrottler(hold)) {
						end(hold);
					}
				}
			}
		}
		const hold = { attrs: attrs, target: target, channel: channel.name, throttle: throttle,
			state: 'waiting' };
		holds.set(attrs, hold);
		if (throttle !== null) {
			throttled.set(throttle, hold);
		}
		attrs.wicketVeil = target;
		acquire(target);
		reconcileLater();
	}

	function advance(attrs, state) {
		const hold = attrs && holds.get(attrs);
		if (hold) {
			hold.state = state;
			reconcileLater();
		}
		return !!hold;
	}

	function onBefore(jqEvent, attrs) {
		advance(attrs, 'started');
	}

	function onBeforeSend(jqEvent, attrs) {
		if (!attrs || advance(attrs, 'sent') || attrs.wicketVeil || isOptedOut(attrs)) {
			return;
		}
		// a request run without /ajax/call/init is veiled as it is sent
		dropStaleTargets();
		const target = findTarget(attrs);
		if (target === null) {
			return;
		}
		attrs.wicketVeil = target;
		acquire(target);
	}

	function onDone(jqEvent, attrs) {
		const hold = attrs && holds.get(attrs);
		if (hold) {
			end(hold);
		} else if (attrs && attrs.wicketVeil) {
			const target = attrs.wicketVeil;
			delete attrs.wicketVeil;
			releaseOne(target);
		}
		reconcileLater();
	}

	function isVeil(element) {
		return !!element && !!element.classList && element.classList.contains(VEIL_CLASS);
	}

	// whether a veil covers the element: the page's, or a local one over an element around it
	function isCovered(element) {
		if (pageTarget !== null && pageTarget.veil !== null) {
			return true;
		}
		for (const target of localTargets.values()) {
			if (target.veil !== null && target.host !== null && !!element &&
				target.host.contains(element)) {
				return true;
			}
		}
		return false;
	}

	// whether the key types where it goes rather than acts
	function types(element, key) {
		if (element.tagName === 'TEXTAREA' || element.isContentEditable) {
			return true;
		}
		if (key !== ' ') {
			return false;
		}
		return element.tagName === 'SELECT' ||
			element.tagName === 'INPUT' && BUTTON_INPUTS.indexOf(element.type) < 0;
	}

	// drops Enter and Space going to an element under a veil, but where they type, and the keyup
	// of a key whose keydown it dropped
	function onKey(event) {
		const key = event.key === 'Spacebar' ? ' ' : event.key;
		// Enter ending the composition of a character, as an input method has it, types
		if (key !== 'Enter' && key !== ' ' || event.isComposing) {
			return;
		}
		if (event.type === 'keyup') {
			if (!droppedKeys.delete(key)) {
				return;
			}
		} else {
			const target = event.target && event.target.nodeType === 1 ? event.target :
				document.activeElement;
			if (!target || !isCovered(target) || types(target, key)) {
				return;
			}
			droppedKeys.add(key);
		}
		event.preventDefault();
		event.stopImmediatePropagation();
	}

	function idOf(element) {
		const withId = element.closest('[id]');
		return withId ? withId.id : null;
	}

	function isPrimary(event) {
		return event.isPrimary !== false && event.button === 0;
	}

	function cancelReplay() {
		Wicket.Veil._clock.clearTimeout(replayTimer);
		replayTimer = null;
		replay = null;
	}

	// notes a press no veil covers, which a veil coming up may catch the release of
	function onPointerDown(event) {
		const target = event.target && event.target.nodeType === 1 ? event.target : null;
		if (!target || isVeil(target) || isCovered(target)) {
			// a press on a veil gives nothing back, and leaves the click it caught before alone
			press = null;
			return;
		}
		press = isPrimary(event) ? { target: target, id: idOf(target) } : null;
		// a press elsewhere: the click caught before is no longer what the user wants
		cancelReplay();
	}

	// keeps the click of a press whose release a veil caught, to give it back once the veil is
	// down: to what the press and the release are on together, as the browser would have clicked
	function onPointerUp(event) {
		const down = press;
		press = null;
		if (!down || !isPrimary(event)) {
			return;
		}
		// a touch keeps its pointerup on the element touched: what is under it tells of a veil
		if (!isVeil(document.elementFromPoint(event.clientX, event.clientY))) {
			return;
		}
		const released = document.elementsFromPoint(event.clientX, event.clientY)
			.find(function (element) {
				return !isVeil(element);
			});
		let target = down.target;
		while (target && released && !target.contains(released)) {
			target = target.parentElement;
		}
		if (!target || target === document.body || target === document.documentElement) {
			return;
		}
		cancelReplay();
		replay = { target: target, id: idOf(target) };
		replayTimer = Wicket.Veil._clock.setTimeout(giveBack, REPLAY_POLL);
	}

	function onPointerCancel() {
		press = null;
	}

	// gives the click a veil caught back to its element once no veil covers it: the element
	// itself if it is still in the page, else the one that took its markup id, as an Ajax answer
	// replacing it leaves; none if it is no longer shown, or disabled
	function giveBack() {
		replayTimer = null;
		const caught = replay;
		if (!caught) {
			return;
		}
		const target = caught.target.isConnected ? caught.target :
			caught.id ? document.getElementById(caught.id) : null;
		if (!target) {
			replay = null;
			return;
		}
		if (isCovered(target)) {
			replayTimer = Wicket.Veil._clock.setTimeout(giveBack, REPLAY_POLL);
			return;
		}
		replay = null;
		if (target.getClientRects().length === 0 || target.matches(':disabled')) {
			return;
		}
		if (typeof(target.click) === "function") {
			target.click();
		} else {
			target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true,
				composed: true, view: window, detail: 1 }));
		}
	}

	function onDomNodeAdded() {
		for (const target of localTargets.values()) {
			reattach(target);
		}
	}

	function onWebSocketMessage(jqEvent, message) {
		if (typeof(message) !== "string" || message.indexOf(MESSAGE_PREFIX) !== 0) {
			return;
		}
		let command;
		try {
			command = JSON.parse(message);
		} catch (e) {
			return;
		}
		if (command.wicketVeil === 'show') {
			Wicket.Veil.show(command.id);
		} else if (command.wicketVeil === 'hide') {
			Wicket.Veil.hide(command.id);
		}
	}

	// the listener, telling Wicket's log of an error rather than throwing it: Wicket.Event tells a
	// topic to its listeners one after the other, and Wicket's Ajax sends a request after it told
	// /ajax/call/init, so an error would stop both
	function safely(listener) {
		return function (jqEvent, attrs) {
			try {
				listener(jqEvent, attrs);
			} catch (error) {
				Wicket.Log.error('Wicket.Veil:', error);
			}
		};
	}

	function subscribe() {
		if (subscribed === false) {
			subscribed = true;
			Wicket.Event.subscribe(Wicket.Event.Topic.AJAX_CALL_INIT, safely(onInit));
			Wicket.Event.subscribe(Wicket.Event.Topic.AJAX_CALL_BEFORE, safely(onBefore));
			Wicket.Event.subscribe(Wicket.Event.Topic.AJAX_CALL_BEFORE_SEND, safely(onBeforeSend));
			Wicket.Event.subscribe(Wicket.Event.Topic.AJAX_CALL_DONE, safely(onDone));
			Wicket.Event.subscribe(Wicket.Event.Topic.DOM_NODE_ADDED, onDomNodeAdded);
			Wicket.Event.subscribe(WEBSOCKET_MESSAGE_TOPIC, onWebSocketMessage);
			window.addEventListener('keydown', onKey, true);
			window.addEventListener('keyup', onKey, true);
			window.addEventListener('pointerdown', onPointerDown, true);
			window.addEventListener('pointerup', onPointerUp, true);
			window.addEventListener('pointercancel', onPointerCancel, true);
		}
	}

	Wicket.Veil = {

		/**
		 * Veils the whole page during every Ajax request that no local veil claims.
		 *
		 * @param options {Object} - 'delay': milliseconds before the spinner shows,
		 *      'minimum': milliseconds the spinner stays once shown
		 */
		page: function (options) {
			subscribe();
			if (pageTarget === null) {
				pageTarget = createTarget(null, options);
			} else {
				configure(pageTarget, options);
			}
		},

		/**
		 * Veils only the element with the given id, during the Ajax requests fired by
		 * components inside it.
		 *
		 * @param id {String} - the markup id of the element to veil
		 * @param options {Object} - as for page()
		 */
		local: function (id, options) {
			subscribe();
			const target = localTargets.get(id);
			if (target) {
				configure(target, options);
			} else {
				localTargets.set(id, createTarget(id, options));
			}
		},

		/**
		 * Raises the local veil registered for the given id, as an Ajax request from inside it
		 * would, with the same timings. Each call has to be matched by a call to hide().
		 *
		 * @param id {String} - the markup id of a component with a local veil
		 */
		show: function (id) {
			const target = localTargets.get(id);
			if (target && document.getElementById(id)) {
				target.raised++;
				acquire(target);
			}
		},

		/**
		 * Lowers the local veil raised by show(), respecting the spinner's minimum time. Calls
		 * without a matching show() are ignored, and leave the veil of a running Ajax request
		 * alone.
		 *
		 * @param id {String} - the markup id of a component with a local veil
		 */
		hide: function (id) {
			const target = localTargets.get(id);
			if (target && target.raised > 0) {
				target.raised--;
				releaseOne(target);
			}
		},

		_clock: {
			now: function () {
				return Date.now();
			},
			setTimeout: function (fn, delay) {
				return window.setTimeout(fn, delay);
			},
			clearTimeout: function (timer) {
				if (timer !== null) {
					window.clearTimeout(timer);
				}
			}
		},

		/**
		 * @param element {Element} - an element of the page
		 * @return {boolean} whether a veil covers it: the page's, or a local one around it
		 */
		isCovered: function (element) {
			return isCovered(element);
		},

		_reset: function () {
			lowerAll();
			pageTarget = null;
			localTargets.clear();
			holds.clear();
			throttled.clear();
			Wicket.Veil._clock.clearTimeout(reconcileTimer);
			reconcileTimer = null;
			cancelReplay();
			press = null;
			droppedKeys.clear();
		}
	};
})();
