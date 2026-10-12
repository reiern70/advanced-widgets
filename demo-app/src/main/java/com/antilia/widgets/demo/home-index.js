/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The index of the demos on the home page: as the pointer or the focus goes over a demo, the
 * preview beside the index shows its screenshot, its name and what it shows. It starts with the
 * first demo. Without this script the preview stays empty and hidden.
 */
(function () {
	'use strict';

	function init() {
		var index = document.querySelector('.index__body');
		if (!index) {
			return;
		}
		var preview = index.querySelector('.index__preview');
		var shot = preview.querySelector('.index__preview-shot');
		var name = preview.querySelector('.index__preview-name');
		var description = preview.querySelector('.index__preview-description');
		var links = Array.prototype.slice.call(index.querySelectorAll('.index__link'));
		var current = null;

		function show(link) {
			if (link === current) {
				return;
			}
			if (current) {
				current.classList.remove('index__link--previewed');
			}
			current = link;
			link.classList.add('index__link--previewed');
			shot.src = link.getAttribute('data-image');
			shot.hidden = false;
			name.textContent = link.querySelector('.index__name').textContent;
			description.textContent = link.getAttribute('data-description');
			preview.classList.remove('index__preview--fresh');
			void preview.offsetWidth;
			preview.classList.add('index__preview--fresh');
		}

		links.forEach(function (link) {
			link.addEventListener('mouseenter', function () {
				show(link);
			});
			link.addEventListener('focus', function () {
				show(link);
			});
		});
		if (links.length) {
			preview.classList.add('index__preview--ready');
			show(links[0]);
		}
	}

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', init);
	} else {
		init();
	}
})();
