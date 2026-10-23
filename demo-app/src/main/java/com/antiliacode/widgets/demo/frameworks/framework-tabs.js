/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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

/*
 * The Wicket, React and Svelte tabs of a demo (FrameworkTabs): shows the panel of the tab chosen
 * and hides the others. The Wicket panel holds the page's components and is only shown and hidden.
 * The panel of a framework is marked data-mount the first time it is shown and gets the event
 * framework-app:mount, upon which the bundle of the framework renders its app into it.
 *
 * The tab chosen is remembered in the browser's storage, when there is one, so that the next demo
 * opens in the same framework.
 */
(function () {
	'use strict';

	var STORAGE_KEY = 'antilia-widgets.framework';

	function remembered() {
		try {
			return window.localStorage.getItem(STORAGE_KEY);
		} catch (e) {
			return null;
		}
	}

	function remember(framework) {
		try {
			window.localStorage.setItem(STORAGE_KEY, framework);
		} catch (e) {
			// no storage, as in a private window: the tab is not remembered
		}
	}

	function init(tablist) {
		var tabs = Array.prototype.slice.call(tablist.querySelectorAll('[role="tab"]'));
		var panelOf = function (tab) {
			return document.getElementById(tab.getAttribute('aria-controls'));
		};
		var wicket = tabs.filter(function (tab) {
			return tab.getAttribute('data-framework') === 'wicket';
		})[0];
		if (wicket && panelOf(wicket)) {
			panelOf(wicket).setAttribute('role', 'tabpanel');
			panelOf(wicket).setAttribute('aria-labelledby', wicket.id);
		}

		function show(tab, focus) {
			tabs.forEach(function (other) {
				var selected = other === tab;
				var panel = panelOf(other);
				other.setAttribute('aria-selected', String(selected));
				other.tabIndex = selected ? 0 : -1;
				if (panel) {
					panel.hidden = !selected;
				}
			});
			var panel = panelOf(tab);
			if (panel && panel.hasAttribute('data-framework-app') && !panel.hasAttribute('data-mount')) {
				panel.setAttribute('data-mount', 'true');
				panel.dispatchEvent(new CustomEvent('framework-app:mount', { bubbles: true }));
			}
			if (focus) {
				tab.focus();
			}
		}

		tablist.addEventListener('click', function (event) {
			var tab = event.target.closest('[role="tab"]');
			if (tab && tabs.indexOf(tab) >= 0) {
				show(tab, false);
				remember(tab.getAttribute('data-framework'));
			}
		});

		tablist.addEventListener('keydown', function (event) {
			var index = tabs.indexOf(document.activeElement);
			if (index < 0) {
				return;
			}
			var next = { ArrowRight: index + 1, ArrowLeft: index - 1, Home: 0,
				End: tabs.length - 1 }[event.key];
			if (next === undefined) {
				return;
			}
			event.preventDefault();
			var tab = tabs[(next + tabs.length) % tabs.length];
			show(tab, true);
			remember(tab.getAttribute('data-framework'));
		});

		var chosen = remembered();
		var initial = tabs.filter(function (tab) {
			return tab.getAttribute('data-framework') === chosen;
		})[0];
		if (initial) {
			show(initial, false);
		}
	}

	function start() {
		document.querySelectorAll('.framework-tabs[role="tablist"]').forEach(init);
	}

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', start);
	} else {
		start();
	}
})();
