/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Client side of ThemeEditorPanel: reads the theme properties the preview gets from its theme
 * into the editor's fields, sets the edited ones on the preview through the CSS object model, so
 * no inline style is needed, and writes the CSS class defining the theme. Each editor is known by
 * the markup id of its element.
 */
;(function () {
	'use strict';

	if (typeof(Wicket.ThemeEditor) !== 'undefined') {
		return;
	}

	const HEX = /^#([0-9a-f])([0-9a-f])([0-9a-f])$/i;

	const editors = {};

	function fields(cfg) {
		return Array.prototype.slice.call(document.getElementById(cfg.editor)
			.querySelectorAll('[data-theme-property]'));
	}

	function preview(cfg) {
		return document.getElementById(cfg.preview);
	}

	function css(cfg) {
		const editor = document.getElementById(cfg.editor);
		const name = editor.querySelector('.theme-editor-class').value.trim() || 'my-theme';
		return '/* put the classes "antilia-theme ' + name + '" on a page or a component */\n' +
			'.' + name + ' {\n' + fields(cfg).map(function (field) {
			return '\t' + field.getAttribute('data-theme-property') + ': ' + field.value + ';';
		}).join('\n') + '\n}\n';
	}

	function update(cfg) {
		document.getElementById(cfg.editor).querySelector('.theme-editor-css').value = css(cfg);
	}

	function apply(cfg, field) {
		preview(cfg).style.setProperty(field.getAttribute('data-theme-property'), field.value);
		update(cfg);
	}

	Wicket.ThemeEditor = {

		init: function (cfg) {
			editors[cfg.editor] = cfg;
			const editor = document.getElementById(cfg.editor);
			['input', 'change'].forEach(function (type) {
				editor.addEventListener(type, function (event) {
					if (event.target.hasAttribute('data-theme-property')) {
						apply(cfg, event.target);
					} else if (event.target.classList.contains('theme-editor-class')) {
						update(cfg);
					}
				});
			});
			editor.querySelector('.theme-editor-copy').addEventListener('click', function (event) {
				Wicket.Clipboard.copy(editor.querySelector('.theme-editor-css').value,
					event.currentTarget);
			});
			this.load(cfg.editor);
		},

		/**
		 * Drops the edits and reads the values of the preview's theme into the fields.
		 *
		 * @param id the markup id of the editor
		 */
		load: function (id) {
			const cfg = editors[id];
			const target = preview(cfg);
			const all = fields(cfg);
			all.forEach(function (field) {
				target.style.removeProperty(field.getAttribute('data-theme-property'));
			});
			const style = window.getComputedStyle(target);
			all.forEach(function (field) {
				const value = style.getPropertyValue(field.getAttribute('data-theme-property')).trim();
				field.value = field.type === 'color' ? value.replace(HEX, '#$1$1$2$2$3$3') : value;
				// a select drawn as a drop-down shows the value set here
				const dropdown = Wicket.Dropdown && Wicket.Dropdown.of(field);
				if (dropdown) {
					dropdown.update();
				}
			});
			update(cfg);
		}
	};
})();
