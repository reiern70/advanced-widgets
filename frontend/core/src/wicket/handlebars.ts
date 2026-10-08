/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * handlebars-adapter.js: registers the page's Handlebars as the engine "handlebars" of the page's
 * Wicket.Templates, for HandlebarsTemplateEngine.
 */

import { registerHandlebars } from '../handlebars.js';
import { Templates, type TemplateRegistry } from '../templates.js';
import { useWicketHost, wicket } from './host.js';

useWicketHost();
registerHandlebars(undefined, (wicket().Templates as TemplateRegistry | undefined) ?? Templates);
