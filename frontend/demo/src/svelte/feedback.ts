/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/** A message of the app, of a level as Wicket's feedback messages have it. */
export interface Message {
	level: 'info' | 'error';
	text: string;
}
