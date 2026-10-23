/*
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
 * The host of the components in a Wicket page: the node events and pushed messages are Wicket.Event
 * topics, the log is Wicket.Log. Each script built for Wicket (see scripts/build-wicket.mjs) sets
 * it before it registers its global, such as Wicket.Dropdown.
 */

import { setHost, standaloneHost, type Host, type Unsubscribe } from '../host.js';

type Subscriber = (event: unknown, payload: unknown) => void;

interface WicketGlobal {
	Event?: {
		subscribe(topic: string, subscriber: Subscriber): void;
		unsubscribe(topic: string, subscriber: Subscriber): void;
		Topic?: Record<string, string>;
	};
	Log?: {
		error(message: string, ...rest: unknown[]): void;
	};
	[name: string]: unknown;
}

declare global {
	interface Window {
		Wicket?: WicketGlobal;
	}
}

/**
 * @returns the Wicket global, made when the page has none yet
 */
export function wicket(): WicketGlobal {
	window.Wicket = window.Wicket ?? {};
	return window.Wicket;
}

/**
 * @param topic the name of the topic in Wicket.Event.Topic, null for the fallback alone
 * @param fallback the topic itself, when Wicket.Event.Topic has no such name
 */
function subscription<T>(topic: string | null, fallback: string,
	listener: (payload: T) => void): Unsubscribe {
	const events = window.Wicket?.Event;
	if (!events?.subscribe) {
		return () => undefined;
	}
	const name = (topic && events.Topic?.[topic]) || fallback;
	const subscriber: Subscriber = (_event, payload) => listener(payload as T);
	events.subscribe(name, subscriber);
	return () => events.unsubscribe?.(name, subscriber);
}

/**
 * The host backed by Wicket.Event and Wicket.Log.
 */
export const wicketHost: Host = {
	onNodeAdded: (listener) => subscription<Element>('DOM_NODE_ADDED', '/dom/node/added',
		listener),
	onNodeRemoving: (listener) => subscription<Element>('DOM_NODE_REMOVING',
		'/dom/node/removing', listener),
	onMessage: (listener) => subscription<unknown>(null, '/websocket/message', listener),
	log: {
		error(message, error) {
			const log = window.Wicket?.Log;
			if (log?.error) {
				log.error(message, error);
			} else {
				standaloneHost.log.error(message, error);
			}
		}
	}
};

/**
 * Makes the components of this script run in the Wicket page.
 */
export function useWicketHost(): void {
	setHost(wicketHost);
}
