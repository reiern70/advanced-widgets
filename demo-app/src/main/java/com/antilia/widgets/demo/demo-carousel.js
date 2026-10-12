/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The carousel of the demos (DemoCarousel): shows one slide at a time and moves on by itself every
 * few seconds, unless the visitor prefers reduced motion. It stops while the pointer or the focus is
 * in it, and for good once the visitor stops it with its button; a bar fills up until the next
 * slide. The arrows, the arrow keys and a swipe move it by hand, a counter tells where it is. Without this script the slides scroll sideways.
 */
var AntiliaDemoCarousel = (function () {
	'use strict';

	var INTERVAL = 6000;

	function init(root) {
		var carousel = root && root.querySelector('.carousel');
		if (!carousel || carousel.hasAttribute('data-ready')) {
			return;
		}
		carousel.setAttribute('data-ready', '');
		var track = carousel.querySelector('.carousel__track');
		var slides = Array.prototype.slice.call(track.children);
		var count = carousel.querySelector('.carousel__count');
		var progress = carousel.querySelector('.carousel__progress');
		var play = carousel.querySelector('[data-carousel="play"]');
		var reduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
		var current = 0;
		var playing = !reduced;
		var hovered = false;
		var focused = false;
		var timer = null;

		function show(index) {
			current = (index + slides.length) % slides.length;
			track.style.transform = 'translateX(' + (-100 * current) + '%)';
			slides.forEach(function (slide, i) {
				var visible = i === current;
				slide.setAttribute('aria-hidden', String(!visible));
				slide.inert = !visible;
			});
			count.textContent = (current + 1) + ' / ' + slides.length;
			schedule();
		}

		function schedule() {
			window.clearTimeout(timer);
			var running = playing && !hovered && !focused && !document.hidden;
			// the bar fills up anew for the next slide
			progress.classList.remove('carousel__progress--running');
			if (running) {
				void progress.offsetWidth;
				progress.firstElementChild.style.animationDuration = INTERVAL + 'ms';
				progress.classList.add('carousel__progress--running');
				timer = window.setTimeout(function () {
					show(current + 1);
				}, INTERVAL);
			}
		}

		function setPlaying(value) {
			playing = value;
			var label = carousel.getAttribute(playing ? 'data-label-pause' : 'data-label-play');
			play.setAttribute('aria-label', label);
			play.title = label;
			play.firstElementChild.textContent = playing ? '\u275A\u275A' : '\u25B6';
			schedule();
		}

		carousel.querySelector('[data-carousel="previous"]').addEventListener('click', function () {
			show(current - 1);
		});
		carousel.querySelector('[data-carousel="next"]').addEventListener('click', function () {
			show(current + 1);
		});
		play.addEventListener('click', function () {
			setPlaying(!playing);
		});
		carousel.addEventListener('mouseenter', function () {
			hovered = true;
			schedule();
		});
		carousel.addEventListener('mouseleave', function () {
			hovered = false;
			schedule();
		});
		carousel.addEventListener('focusin', function () {
			focused = true;
			schedule();
		});
		carousel.addEventListener('focusout', function (event) {
			focused = carousel.contains(event.relatedTarget);
			schedule();
		});
		carousel.addEventListener('keydown', function (event) {
			if (event.key === 'ArrowLeft') {
				show(current - 1);
			} else if (event.key === 'ArrowRight') {
				show(current + 1);
			}
		});
		document.addEventListener('visibilitychange', schedule);

		var startX = null;
		track.addEventListener('touchstart', function (event) {
			startX = event.touches[0].clientX;
		}, { passive: true });
		track.addEventListener('touchend', function (event) {
			if (startX !== null) {
				var dx = event.changedTouches[0].clientX - startX;
				if (Math.abs(dx) > 40) {
					show(current + (dx < 0 ? 1 : -1));
				}
				startX = null;
			}
		});

		carousel.classList.add('carousel--scripted');
		setPlaying(playing);
		show(0);
	}

	return { init: init };
})();
