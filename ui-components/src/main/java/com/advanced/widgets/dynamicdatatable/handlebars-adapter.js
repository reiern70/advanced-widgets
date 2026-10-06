/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
;(function (undefined) {
	'use strict';

	Wicket.DynamicDataTable.engines.handlebars = {
		compile: function (template) {
			const compiled = window.Handlebars.compile(template);
			return function (data, meta) {
				return compiled(data, { data: meta });
			};
		}
	};
})();
