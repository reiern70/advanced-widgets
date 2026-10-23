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

/**
 * Stops a listener given to the {@link Host}.
 */
export type Unsubscribe = () => void;

/**
 * Everything the components need from the page they run in. In Wicket the host is backed by
 * Wicket.Event and Wicket.Log (see wicket/host.ts); in React and Svelte the frameworks call
 * destroy() on unmount, so the node events of the standalone host never fire.
 */
export interface Host {
	/** Called once an element was added by an update of the page (Wicket: DOM_NODE_ADDED). */
	onNodeAdded(listener: (element: Element) => void): Unsubscribe;

	/** Called before an element is removed by an update of the page (Wicket: DOM_NODE_REMOVING). */
	onNodeRemoving(listener: (element: Element) => void): Unsubscribe;

	/** Messages pushed by the server (Wicket: /websocket/message). */
	onMessage(listener: (message: unknown) => void): Unsubscribe;

	log: {
		error(message: string, error?: unknown): void;
	};
}

const nothing = (): Unsubscribe => () => undefined;

/**
 * The host outside Wicket: the page never removes elements behind the components' back, messages
 * are pushed by the application itself and errors go to the console.
 */
export const standaloneHost: Host = {
	onNodeAdded: nothing,
	onNodeRemoving: nothing,
	onMessage: nothing,
	log: {
		error(message, error) {
			console.error(message, error);
		}
	}
};

let current: Host = standaloneHost;

/**
 * @returns the host the components run in
 */
export function getHost(): Host {
	return current;
}

/**
 * Sets the host the components created from now on run in.
 *
 * @param host the host, or the standalone one when left out
 */
export function setHost(host?: Host): void {
	current = host ?? standaloneHost;
}

/**
 * Calls a listener when an update of the page is about to remove an element, itself or one of its
 * ancestors.
 *
 * @param element the element
 * @param listener called once at most
 * @returns stops listening
 */
export function onRemoving(element: Element, listener: () => void): Unsubscribe {
	let unsubscribe: Unsubscribe | undefined = undefined;
	let done = false;
	unsubscribe = getHost().onNodeRemoving((removed) => {
		if (!done && removed.contains(element)) {
			done = true;
			unsubscribe?.();
			listener();
		}
	});
	if (done) {
		unsubscribe();
	}
	return () => {
		done = true;
		unsubscribe?.();
	};
}
