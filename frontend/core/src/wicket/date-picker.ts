/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-date-picker.js: registers Wicket.DatePicker for DatePickerBehavior, which calls
 * Wicket.DatePicker.attach(id, options).
 */

import { DatePicker } from '../date-picker.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.DatePicker === undefined) {
	useWicketHost();
	Wicket.DatePicker = DatePicker;
}
