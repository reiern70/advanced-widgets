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
 * Resumable uploads, over the tus protocol 1.0 (https://tus.io/protocols/resumable-upload): the
 * browser creates an upload on the server (POST, Upload-Length), sends the file in chunks (PATCH,
 * Upload-Offset), and after a pause, a broken connection or a reload of the page asks the server
 * how many bytes it holds (HEAD) and sends the rest only. Being tus, it works with any tus server,
 * the Wicket resource of the widgets (ResumableUploadResource) among them.
 *
 * new TusUpload(file, options) sends one file: start(), pause(), start() again, cancel(), which
 * deletes the upload on the server (DELETE, the termination extension). A broken connection, an
 * answer 5xx, 409, 423 or 429 is tried again after retryDelays, asking the server first where it
 * stopped; offline, the upload waits until the browser is online again. The address of the upload
 * is kept in localStorage under the fingerprint of the file (TusStorage), so that the same file
 * chosen again after a reload goes on where it stopped; remember: false keeps nothing.
 *
 * new ResumableUploader(element, options) draws, into an element the framework renders empty, the
 * file picker of the theme and a row per file: its name, its size, its progress bar with what was
 * sent, how fast and how long it still takes, and its buttons pausing, resuming and canceling it,
 * then buttons pausing and resuming all. The uploads left unfinished by an earlier visit of the
 * page show too: choosing the file again goes on with it. The guard of the uploads asks before
 * the page is left while files are being sent, unless keepingOnPage is false.
 */

import { type Look, lookOf, partClasses, toggleParts } from './look.js';
import { onRemoving, type Unsubscribe } from './host.js';
import { button, CLEAR_ICON, create, FilePicker, type FilePickerDesign, type FilePickerOptions,
	type FilePickerTexts,
	format, formatBytes, formatDuration, UploadGuard, type UploadGuardHandle } from './upload.js';

/** The version of the tus protocol spoken, sent in Tus-Resumable with every request. */
export const TUS_RESUMABLE = '1.0.0';

/** The bytes sent by a request when the options give no chunk size: 8 MiB. */
export const DEFAULT_CHUNK_SIZE = 8 * 1024 * 1024;

/** How long an upload tries again by itself by default, in milliseconds, see retryTimeout. */
export const DEFAULT_RETRY_TIMEOUT = 40000;

/** The waits before the tries again, in milliseconds, the last one again and again. */
const RETRY_STEPS: readonly number[] = [0, 1000, 3000, 5000, 10000, 20000, 30000];

/**
 * @param timeout how long to try again, in milliseconds
 * @returns the waits before the tries again, at once, then after 1, 3, 5, 10, 20 and then 30
 * seconds, which together take the timeout; none for 0, as Wicket's
 * ResumableUploadToResourceField#setRetryTimeout() makes them
 */
export function retryDelaysFor(timeout: number): number[] {
	const delays: number[] = [];
	let left = Math.max(0, timeout);
	for (let step = 0; left > 0; step++) {
		const delay = Math.min(RETRY_STEPS[Math.min(step, RETRY_STEPS.length - 1)], left);
		delays.push(delay);
		left -= delay;
	}
	return delays;
}

/** How long to wait before each try after a failure, in milliseconds: 40 seconds together. */
export const DEFAULT_RETRY_DELAYS: readonly number[] = Object.freeze(
	retryDelaysFor(DEFAULT_RETRY_TIMEOUT));

/**
 * How long a request may go without progress, or without an answer, before it is taken for lost;
 * once all of a chunk left the browser, as long as the server answers a HEAD meanwhile, it waits on.
 */
export const DEFAULT_STALL_MILLIS = 30000;

/**
 * How long to wait before each look whether the server answers again, once the connection is lost,
 * the last one again and again.
 */
export const DEFAULT_PROBE_DELAYS: readonly number[] = Object.freeze([5000, 10000, 20000, 30000]);

/**
 * How long after it last sent something an upload counts as in use by another page, which leaves
 * it alone, by default; a page sending it tells so four times as often.
 */
export const DEFAULT_IN_USE_MILLIS = 20000;

/** The content type of the chunks, as tus wants it. */
const CHUNK_TYPE = 'application/offset+octet-stream';

/**
 * The header in which Wicket's ResumableUploadResourceReference tells the name it stored a whole
 * file under: in the answer completing the upload, and in those to HEAD after it.
 */
export const STORED_NAME_HEADER = 'Wicket-Upload-File-Name';

/** The header in which Wicket's ResumableUploadResourceReference tells the key of a refusal. */
export const ERROR_KEY_HEADER = 'Wicket-Upload-Error';

/** The key a lost upload fails with, as Wicket's ResumableUploadField words it. */
export const LOST_KEY = 'resumableUploadLost';

/** How often an upload starts over at most, after the server lost it, until it is started again. */
const MAX_RESTARTS = 2;

/** Where TusStorage keeps the addresses of the uploads, before their fingerprints. */
const STORAGE_PREFIX = 'antilia-tus::';

/**
 * Where an upload is. Lost: the server did not answer through all the tries, or the browser stayed
 * offline as long, the connection lost, a VPN down for example; start() goes on once it answers.
 */
export type TusState = 'idle' | 'starting' | 'uploading' | 'paused' | 'retrying' | 'offline' |
	'lost' | 'done' | 'failed' | 'canceled';

/**
 * What went wrong: no answer (network), an upload the server no longer has (gone), an answer
 * worth trying again after a while (server: 5xx, 409, 423, 429), an answer saying no (refused:
 * the file too large, for example), or an answer outside the protocol (protocol).
 */
export type TusErrorKind = 'network' | 'gone' | 'server' | 'refused' | 'protocol';

/** A failure of an upload. */
export class TusError extends Error {
	/** The HTTP status of the answer, 0 when none came. */
	readonly status: number;

	readonly kind: TusErrorKind;

	/** The key of the refusal, as the server tells it in ERROR_KEY_HEADER; null without one. */
	readonly key: string | null;

	/**
	 * @param message what the server said, or what went wrong
	 * @param status the HTTP status of the answer, 0 when none came
	 * @param kind what went wrong
	 * @param key the key of the refusal the server told, if any
	 */
	constructor(message: string, status: number, kind: TusErrorKind, key: string | null = null) {
		super(message);
		this.name = 'TusError';
		this.status = status;
		this.kind = kind;
		this.key = key;
	}

	/** Whether trying again may help. */
	get retryable(): boolean {
		return this.kind === 'network' || this.kind === 'gone' || this.kind === 'server';
	}
}

/** How an upload goes and what it tells. */
export interface TusUploadOptions {
	/** Where uploads are created, by POST: the address of the tus server. */
	endpoint: string;
	/** The bytes sent by a request at most; DEFAULT_CHUNK_SIZE when left out, Infinity for one. */
	chunkSize?: number;
	/** What the server is told of the file, in Upload-Metadata, besides filename and filetype. */
	metadata?: Record<string, string>;
	/** Headers sent with every request. */
	headers?: Record<string, string>;
	/**
	 * A query added to the address of every request, without its '?': the signed grant of Wicket's
	 * ResumableUploadResourceReference, for example. The addresses kept to go on after a reload
	 * are kept without it, so a new page sends its own.
	 */
	query?: string;
	/** Whether the requests to another origin carry its cookies. */
	withCredentials?: boolean;
	/**
	 * How long to wait before each try after a failure; when left out, those retryDelaysFor()
	 * makes of retryTimeout. Together they are how long the upload tries again by itself, and
	 * waits to be online again, before it takes the connection for lost.
	 */
	retryDelays?: readonly number[];
	/**
	 * How long the upload tries again by itself, in milliseconds, unless retryDelays tells;
	 * DEFAULT_RETRY_TIMEOUT when left out, 0 to take the connection for lost at once.
	 */
	retryTimeout?: number;
	/** How long a chunk may go without progress before it is taken for lost; 0 never. */
	stallMillis?: number;
	/**
	 * How often to ask the server how many bytes it holds while a chunk is being sent, its answer
	 * being the progress then, rather than what the browser handed to the network; 0, the default,
	 * never. Only for servers answering HEAD while a PATCH runs, as Wicket's
	 * ResumableUploadResourceReference does: tusd locks the upload instead.
	 */
	pollMillis?: number;
	/** Whether the address of the upload is kept to go on after a reload, true by default. */
	remember?: boolean;
	/**
	 * What the address is kept with, besides the fingerprint of the file: the place of the uploader
	 * it belongs to, for the one in the same place of a later page to offer to go on with it.
	 */
	rememberedAs?: string;
	/**
	 * The part of the application the upload belongs to, as Wicket's ResumableUploadField tells
	 * it: the address is kept for it rather than for the place, and an uploader of the same context
	 * anywhere goes on with the upload.
	 */
	context?: string;
	/**
	 * How long after it last sent something the upload counts as in use for other pages, which
	 * leave it alone; DEFAULT_IN_USE_MILLIS when left out. It tells so four times as often.
	 */
	inUseMillis?: number;
	/** Whether to send PATCH as POST with X-HTTP-Method-Override, for proxies refusing PATCH. */
	overridePatchMethod?: boolean;
	/** Called as bytes go: those the server holds or is being sent, of all. */
	onProgress?(sent: number, total: number, upload: TusUpload): void;
	/** Called whenever the state changes. */
	onState?(state: TusState, upload: TusUpload): void;
	/** Called once the server holds the whole file. */
	onSuccess?(upload: TusUpload): void;
	/** Called once the upload failed for good; a lost connection is no failure, see TusState. */
	onError?(error: TusError, upload: TusUpload): void;
}

/** An answer of the server. */
interface Answer {
	status: number;
	header(name: string): string | null;
	text: string;
}

/** A request stopped on purpose: by a pause, a cancellation or a newer start. */
class Stopped extends Error {
}

/**
 * @returns an address made absolute against the page
 */
export function absoluteUrl(url: string): string {
	try {
		return new URL(url, document.baseURI).href;
	} catch {
		return url;
	}
}

/**
 * @returns how long to wait before each try after a failure: the retryDelays of options, else those
 * retryDelaysFor() makes of their retryTimeout
 */
function retryDelaysOf(options: { retryDelays?: readonly number[]; retryTimeout?: number }):
	readonly number[] {
	return options.retryDelays ?? (options.retryTimeout === undefined
		? DEFAULT_RETRY_DELAYS : retryDelaysFor(options.retryTimeout));
}

/**
 * @returns an address without the path parameters of its last segment, the id of a session a
 * server without cookies puts there for example
 */
export function withoutPathParameters(url: string): string {
	return url.replace(/;[^/?#]*(?=[?#]|$)/, '');
}

/**
 * @returns an address with the path parameters of the endpoint, unless it has some: the id of a
 * session is added to the address of an upload as a request is sent, and never kept with it
 */
function withPathParametersOf(url: string, endpoint: string | undefined): string {
	const parameters = endpoint ? /;[^/?#]*(?=[?#]|$)/.exec(endpoint) : null;
	if (!parameters || /;[^/?#]*(?=[?#]|$)/.test(url)) {
		return url;
	}
	const end = url.search(/[?#]/);
	return end < 0 ? url + parameters[0] : url.slice(0, end) + parameters[0] + url.slice(end);
}

/**
 * @returns what the address of an upload is kept for: the context of its uploader, else its place;
 * undefined for neither
 */
function scopeOf(options: { context?: string; rememberedAs?: string }): string | undefined {
	return options.context ? 'context:' + options.context
		: options.rememberedAs ? 'place:' + options.rememberedAs : undefined;
}

/**
 * @returns the fingerprint of a file sent to a tus server: the same file chosen again, to the same
 * server, for the same scope, see TusUploadOptions.context, has the same one
 */
export function fingerprintOf(file: Blob, endpoint: string, scope?: string): string {
	const named = file as Partial<File>;
	return fingerprintFor(endpoint, named.name ?? '', file.size, file.type, named.lastModified ?? 0,
		scope);
}

/**
 * @returns the fingerprint of a file of a name, size, type and date sent to a tus server, as
 * fingerprintOf() makes it
 */
function fingerprintFor(endpoint: string, name: string, size: number, type: string,
	lastModified: number, scope?: string): string {
	const parts: (string | number)[] = [absoluteUrl(withoutPathParameters(endpoint)), name, size,
		type, lastModified];
	return (scope ? [...parts, scope] : parts).join('::');
}

function base64(value: string): string {
	let binary = '';
	new TextEncoder().encode(value).forEach((byte) => {
		binary += String.fromCharCode(byte);
	});
	return btoa(binary);
}

/**
 * @returns metadata as Upload-Metadata has it: each key and its value in base64, separated by
 * commas; keys with spaces or commas are left out
 */
export function encodeMetadata(metadata: Record<string, string>): string {
	return Object.entries(metadata).filter(([key]) => /^[^\s,]+$/.test(key))
		.map(([key, value]) => value === '' ? key : `${key} ${base64(value)}`).join(',');
}

function answerOf(xhr: XMLHttpRequest): Answer {
	let text = '';
	try {
		text = xhr.responseText ?? '';
	} catch {
		text = '';
	}
	return { status: xhr.status, header: (name) => xhr.getResponseHeader(name), text };
}

/**
 * @returns the failure an answer tells; its text when the server explains in a short plain text
 */
function errorOf(answer: Answer): TusError {
	const status = answer.status;
	const key = answer.header(ERROR_KEY_HEADER);
	// a refusal naming its error is final, whatever its status
	const kind: TusErrorKind = status === 0 ? 'network'
		: key !== null ? 'refused'
			: status === 404 || status === 410 ? 'gone'
				: status === 409 || status === 423 || status === 429 ||
					(status >= 500 && status !== 501) ? 'server' : 'refused';
	const plain = (answer.header('Content-Type') ?? '').toLowerCase().startsWith('text/plain');
	const text = plain && answer.text.length < 500 ? answer.text.trim() : '';
	return new TusError(text || `The server answered ${status}`, status, kind, key);
}

/**
 * @returns an address with a query added, if any
 */
export function withQuery(url: string, query: string | undefined): string {
	return query ? url + (url.includes('?') ? '&' : '?') + query : url;
}

/** An upload kept in localStorage, to go on with after a reload. */
/** An upload the server keeps, for an uploader to offer, as Wicket's ResumableUploadField lists it. */
export interface RecalledUpload {
	/** Its id: the last segment of its address, below the endpoint. */
	id: string;
	name: string;
	size: number;
	type: string;
	/** When the file was last modified, as the browser told the server; 0 when it did not. */
	lastModified: number;
	/** The bytes the server holds. */
	sent: number;
}

export interface StoredUpload {
	/** The fingerprint of the file, see fingerprintOf(). */
	fingerprint: string;
	/** Where the server keeps the upload. */
	url: string;
	/** The absolute address of the tus server. */
	endpoint: string;
	name: string;
	size: number;
	type: string;
	lastModified: number;
	/** When it was kept, in milliseconds since the epoch. */
	stored: number;
	/** What it was kept with: the place of its uploader, see TusUploadOptions.rememberedAs. */
	rememberedAs?: string;
	/** The context of its uploader, see TusUploadOptions.context. */
	context?: string;
	/** When it was last being sent, in milliseconds since the epoch; 0 once its page was left. */
	alive?: number;
}

function storage(): Storage | null {
	try {
		return window.localStorage ?? null;
	} catch {
		return null;
	}
}

/**
 * The addresses of the uploads not finished yet, in localStorage by the fingerprints of their
 * files. A browser keeping nothing, a private window for example, keeps none.
 */
export const TusStorage = {

	/**
	 * @returns the upload of a file not finished yet, if one is kept
	 */
	find(fingerprint: string): StoredUpload | null {
		try {
			const value = storage()?.getItem(STORAGE_PREFIX + fingerprint);
			const stored = value ? JSON.parse(value) as StoredUpload : null;
			return stored && typeof stored.url === 'string' ? stored : null;
		} catch {
			return null;
		}
	},

	/**
	 * Keeps an upload not finished yet.
	 */
	save(upload: StoredUpload): void {
		try {
			storage()?.setItem(STORAGE_PREFIX + upload.fingerprint, JSON.stringify(upload));
		} catch {
			// full, or kept from the page: the upload goes on all the same, only not after a reload
		}
	},

	/**
	 * Forgets the upload of a file.
	 */
	remove(fingerprint: string): void {
		try {
			storage()?.removeItem(STORAGE_PREFIX + fingerprint);
		} catch {
			// nothing to forget
		}
	},

	/**
	 * @param upload a kept upload
	 * @param inUseMillis how long after it last sent something it counts as in use
	 * @returns whether another page sends a kept upload now
	 */
	sending(upload: StoredUpload, inUseMillis = DEFAULT_IN_USE_MILLIS): boolean {
		return Date.now() - (upload.alive ?? 0) < inUseMillis;
	},

	/**
	 * @returns the uploads not finished yet to a tus server, the oldest first
	 */
	list(endpoint: string): StoredUpload[] {
		const store = storage();
		if (!store) {
			return [];
		}
		const absolute = absoluteUrl(withoutPathParameters(endpoint));
		const found: StoredUpload[] = [];
		try {
			for (let index = 0; index < store.length; index++) {
				const key = store.key(index);
				if (key?.startsWith(STORAGE_PREFIX)) {
					const stored = TusStorage.find(key.slice(STORAGE_PREFIX.length));
					if (stored?.endpoint === absolute) {
						found.push(stored);
					}
				}
			}
		} catch {
			return [];
		}
		return found.sort((a, b) => a.stored - b.stored);
	}
};

/**
 * The options of a request outside an upload: its headers and credentials, and the endpoint whose
 * path parameters an address of an upload gets.
 */
export type TusRequestOptions = Pick<TusUploadOptions, 'headers' | 'withCredentials' | 'query'> &
	{ endpoint?: string };

function request(method: string, url: string, options: TusRequestOptions | undefined):
	Promise<Answer> {
	return new Promise((resolve) => {
		const xhr = new XMLHttpRequest();
		xhr.open(method, withQuery(withPathParametersOf(url, options?.endpoint), options?.query),
			true);
		xhr.withCredentials = !!options?.withCredentials;
		Object.entries({ ...options?.headers, 'Tus-Resumable': TUS_RESUMABLE })
			.forEach(([name, value]) => xhr.setRequestHeader(name, value));
		// a connection gone without a word answers never
		xhr.timeout = DEFAULT_STALL_MILLIS;
		xhr.onload = () => resolve(answerOf(xhr));
		xhr.onerror = xhr.ontimeout = () => resolve({ status: 0, header: () => null, text: '' });
		xhr.send(null);
	});
}

/**
 * Deletes an upload on the server, finished or not (the termination extension of tus), trying
 * again while the server is busy with it, a request just aborted still writing it for example.
 *
 * @param retryDelays how long to wait before each try again; none by default
 * @returns whether the server deleted it, or did not have it
 */
export async function terminate(url: string, options?: TusRequestOptions,
	retryDelays: readonly number[] = []): Promise<boolean> {
	for (let attempt = 0; ; attempt++) {
		const answer = await request('DELETE', url, options);
		if (answer.status === 204 || answer.status === 200 || answer.status === 404 ||
			answer.status === 410) {
			return true;
		}
		if (attempt >= retryDelays.length || !errorOf(answer).retryable) {
			return false;
		}
		await new Promise((resolve) => window.setTimeout(resolve, retryDelays[attempt]));
	}
}

/**
 * @returns whether the server answers a HEAD at an address, whatever it answers
 */
export async function reachable(url: string, options?: TusRequestOptions): Promise<boolean> {
	return (await request('HEAD', url, options)).status !== 0;
}

/**
 * Asks the server how far an upload got.
 *
 * @returns the bytes it holds and those of the file; null when it does not have the upload, or
 * did not answer
 */
export async function probe(url: string, options?: TusRequestOptions):
	Promise<{ offset: number; length: number | null } | null> {
	const found = await headOf(url, options);
	return found === 'gone' ? null : found;
}

/**
 * Asks the server how far an upload got, telling apart an upload it does not have from an answer
 * that tells nothing.
 *
 * @returns the bytes it holds and those of the file; 'gone' when the server does not have the
 * upload; null when it did not answer, or refused to tell, as for a grant too old
 */
async function headOf(url: string, options?: TusRequestOptions):
	Promise<{ offset: number; length: number | null } | 'gone' | null> {
	const answer = await request('HEAD', url, options);
	if (answer.status === 404 || answer.status === 410) {
		return 'gone';
	}
	if (answer.status < 200 || answer.status >= 300) {
		return null;
	}
	const offset = Number(answer.header('Upload-Offset'));
	if (answer.header('Upload-Offset') === null || !Number.isFinite(offset)) {
		return 'gone';
	}
	const length = answer.header('Upload-Length');
	return { offset, length: length === null ? null : Number(length) };
}

/**
 * The upload of one file to a tus server.
 */
export class TusUpload {
	readonly file: Blob;

	readonly options: TusUploadOptions;

	/** Where the server keeps the upload, once created or found again. */
	url: string | null = null;

	/** The bytes the server holds, as last told. */
	offset = 0;

	/** The name the server stored the whole file under, if it tells (STORED_NAME_HEADER). */
	storedName: string | null = null;

	state: TusState = 'idle';

	/** Why it failed, or the failure it is about to try again after. */
	error: TusError | null = null;

	/** How long it waits before the next try, while retrying, in milliseconds. */
	retryDelay = 0;

	/** The tries since the last chunk that went through. */
	private attempts = 0;

	/** The uploads created since the last start(): more than one, the server lost one. */
	private creations = 0;

	/** The run going on: a pause, a cancellation or a start begin another one. */
	private run = 0;

	private xhr: XMLHttpRequest | null = null;

	private timer: number | undefined = undefined;

	private online: (() => void) | null = null;

	/** Whether the offset is the server's, rather than one to ask it for. */
	private offsetKnown = false;

	/** When the upload last told it is being sent. */
	private alive = 0;

	/**
	 * @param file the file, or any blob
	 * @param options where it goes and what it tells
	 */
	constructor(file: Blob, options: TusUploadOptions) {
		this.file = file;
		this.options = { ...options };
	}

	/** The bytes of the file. */
	get size(): number {
		return this.file.size;
	}

	/** The fingerprint of the file, by which its address is kept. */
	get fingerprint(): string {
		return fingerprintOf(this.file, this.options.endpoint, scopeOf(this.options));
	}

	/** Whether it is being sent, or about to be after a failure. */
	get running(): boolean {
		return this.state === 'starting' || this.state === 'uploading' ||
			this.state === 'retrying' || this.state === 'offline';
	}

	/**
	 * Starts sending the file, or goes on after a pause, a failure or a lost connection: asks the
	 * server first where it stopped. Does nothing while it runs, once done or canceled.
	 */
	start(): void {
		if (this.running || this.state === 'done' || this.state === 'canceled') {
			return;
		}
		this.attempts = 0;
		this.creations = 0;
		this.error = null;
		void this.go(++this.run);
	}

	/**
	 * Stops sending: the server keeps what it got, and start() goes on from there.
	 */
	pause(): void {
		if (!this.running) {
			return;
		}
		this.stop();
		this.setState('paused');
		// the server keeps what it got of the chunk stopped: tell how far it got
		const url = this.url;
		const run = this.run;
		if (url) {
			void probe(url, this.options).then((found) => {
				if (found && run === this.run && this.state === 'paused' &&
					found.offset >= this.offset && found.offset <= this.size) {
					this.offset = found.offset;
					this.progress(found.offset);
				}
			});
		}
	}

	/**
	 * Stops sending and deletes the upload on the server; it cannot go on then.
	 */
	async cancel(): Promise<void> {
		if (this.state === 'done' || this.state === 'canceled') {
			return;
		}
		this.stop();
		const url = this.url;
		this.forget();
		this.url = null;
		this.setState('canceled');
		if (url) {
			await terminate(url, this.options, this.retryDelays()).catch(() => false);
		}
	}

	/** How long to wait before each try after a failure. */
	retryDelays(): readonly number[] {
		return retryDelaysOf(this.options);
	}

	/**
	 * Stops what runs without telling: the page goes, or the widget showing the upload.
	 */
	abort(): void {
		this.stop();
		if (this.running) {
			this.state = 'paused';
		}
	}

	private stop(): void {
		this.run++;
		this.offsetKnown = false;
		window.clearTimeout(this.timer);
		this.timer = undefined;
		this.stopWaitingOnline();
		const xhr = this.xhr;
		this.xhr = null;
		xhr?.abort();
	}

	private setState(state: TusState): void {
		this.state = state;
		this.options.onState?.(state, this);
	}

	private progress(sent: number): void {
		this.options.onProgress?.(Math.min(sent, this.size), this.size, this);
	}

	/**
	 * Keeps the address of the upload, and when it was last being sent.
	 *
	 * @param alive when it was last being sent: now by default, 0 once the page is left
	 */
	private remember(alive = Date.now()): void {
		if (this.options.remember === false || !this.url) {
			return;
		}
		const named = this.file as Partial<File>;
		this.alive = alive;
		TusStorage.save({ fingerprint: this.fingerprint, url: this.url,
			endpoint: absoluteUrl(withoutPathParameters(this.options.endpoint)),
			name: named.name ?? '', size: this.size, type: this.file.type,
			lastModified: named.lastModified ?? 0, stored: Date.now(),
			rememberedAs: this.options.rememberedAs, context: this.options.context, alive });
	}

	/** Tells now and then that the upload is being sent, for other pages to leave it alone. */
	private keepAlive(): void {
		if (Date.now() - this.alive >= (this.options.inUseMillis ?? DEFAULT_IN_USE_MILLIS) / 4) {
			this.remember();
		}
	}

	/**
	 * Tells that the page sending the upload is left, unless it is done: the next page offers to
	 * go on with it at once.
	 */
	leave(): void {
		if (this.url && this.state !== 'done' && this.state !== 'canceled') {
			this.remember(0);
		}
	}

	private forget(): void {
		if (this.options.remember !== false) {
			TusStorage.remove(this.fingerprint);
		}
	}

	private async go(run: number): Promise<void> {
		try {
			this.setState('starting');
			if (!this.url && this.options.remember !== false) {
				this.url = TusStorage.find(this.fingerprint)?.url ?? null;
				this.offsetKnown = false;
			}
			if (this.url && !this.offsetKnown) {
				await this.ask(run);
			}
			if (!this.url) {
				await this.create(run);
			}
			this.offsetKnown = true;
			this.setState('uploading');
			this.progress(this.offset);
			const chunk = Math.max(1, this.options.chunkSize ?? DEFAULT_CHUNK_SIZE);
			const polling = (this.options.pollMillis ?? 0) > 0;
			while (this.offset < this.size) {
				const start = this.offset;
				const end = Math.min(this.size, start + chunk);
				const stopPolling = polling ? this.poll(run, start) : () => undefined;
				let answer: Answer;
				try {
					answer = await this.send(run, 'PATCH', this.url!, {
						'Upload-Offset': String(start), 'Content-Type': CHUNK_TYPE
					}, this.file.slice(start, end), (loaded) => {
						this.keepAlive();
						if (!polling) {
							this.progress(start + loaded);
						}
					});
				} finally {
					stopPolling();
				}
				if (answer.status !== 204 && answer.status !== 200) {
					throw errorOf(answer);
				}
				const offset = Number(answer.header('Upload-Offset'));
				if (!Number.isFinite(offset) || offset < start || offset > this.size) {
					throw new TusError('The server told no valid Upload-Offset', answer.status,
						'protocol');
				}
				this.offset = offset;
				this.storedName = answer.header(STORED_NAME_HEADER) ?? this.storedName;
				this.attempts = 0;
				this.progress(offset);
			}
			this.forget();
			this.setState('done');
			this.options.onSuccess?.(this);
		} catch (thrown) {
			if (run === this.run && !(thrown instanceof Stopped)) {
				this.failed(run, thrown instanceof TusError ? thrown
					: new TusError(String(thrown), 0, 'protocol'));
			}
		}
	}

	/**
	 * Asks the server how many bytes of the upload it holds; one it no longer has, or of another
	 * file, is forgotten, to be created again. A refusal, of a grant too old for example, fails the
	 * upload and keeps it for a page loaded again.
	 */
	private async ask(run: number): Promise<void> {
		const answer = await this.send(run, 'HEAD', this.url!, {});
		if (answer.status === 404 || answer.status === 410) {
			this.forget();
			this.url = null;
			return;
		}
		if (answer.status < 200 || answer.status >= 300) {
			throw errorOf(answer);
		}
		const offset = Number(answer.header('Upload-Offset'));
		const length = answer.header('Upload-Length');
		if (answer.header('Upload-Offset') === null || !Number.isFinite(offset) ||
			(length !== null && Number(length) !== this.size) || offset > this.size) {
			this.forget();
			this.url = null;
			return;
		}
		this.offset = offset;
		this.storedName = answer.header(STORED_NAME_HEADER);
	}

	private async create(run: number): Promise<void> {
		if (++this.creations > 1 + MAX_RESTARTS) {
			throw new TusError('The server lost the upload again', 410, 'refused', LOST_KEY);
		}
		const named = this.file as Partial<File>;
		const metadata = { filename: named.name ?? '', filetype: this.file.type,
			lastmodified: String(named.lastModified ?? 0), ...this.options.metadata };
		const answer = await this.send(run, 'POST', absoluteUrl(this.options.endpoint), {
			'Upload-Length': String(this.size), 'Upload-Metadata': encodeMetadata(metadata)
		});
		if (answer.status !== 201) {
			const error = errorOf(answer);
			// an endpoint that is not there is no upload gone: nothing to create again
			throw error.kind === 'gone' ? new TusError(error.message, error.status, 'refused')
				: error;
		}
		const location = answer.header('Location');
		if (!location) {
			throw new TusError('The server told no Location', answer.status, 'protocol');
		}
		// behind a proxy the path the server sees may not be the browser's: relative to the endpoint
		this.url = withoutPathParameters(new URL(location, absoluteUrl(this.options.endpoint)).href);
		this.offset = 0;
		this.storedName = answer.header(STORED_NAME_HEADER);
		this.remember();
	}

	/**
	 * Asks the server every pollMillis how many bytes it holds while a chunk is being sent, telling
	 * them as the progress.
	 *
	 * @returns what stops asking
	 */
	private poll(run: number, start: number): () => void {
		const url = this.url!;
		let asking = false;
		let told = start;
		const timer = window.setInterval(() => {
			if (asking || run !== this.run) {
				return;
			}
			asking = true;
			void probe(url, this.options).then((found) => {
				asking = false;
				if (found && run === this.run && this.state === 'uploading' &&
					found.offset > told && found.offset <= this.size) {
					told = found.offset;
					this.progress(found.offset);
				}
			});
		}, this.options.pollMillis);
		return () => window.clearInterval(timer);
	}

	private failed(run: number, error: TusError): void {
		this.offsetKnown = false;
		this.error = error;
		if (error.kind === 'gone') {
			this.forget();
			this.url = null;
		}
		const delays = this.retryDelays();
		if (error.retryable && typeof navigator !== 'undefined' && navigator.onLine === false) {
			this.waitOnline(run, delays.reduce((sum, delay) => sum + delay, 0));
			return;
		}
		if (error.retryable && this.attempts < delays.length) {
			this.retryDelay = delays[this.attempts++];
			this.setState('retrying');
			this.timer = window.setTimeout(() => {
				if (run === this.run) {
					void this.go(run);
				}
			}, this.retryDelay);
			return;
		}
		if (error.kind === 'network') {
			this.setState('lost');
			return;
		}
		this.setState('failed');
		this.options.onError?.(error, this);
	}

	/**
	 * Goes on once the browser is online again; offline for longer than the tries take, the
	 * connection is lost.
	 */
	private waitOnline(run: number, most: number): void {
		this.stopWaitingOnline();
		this.online = () => {
			this.stopWaitingOnline();
			window.clearTimeout(this.timer);
			if (run === this.run) {
				void this.go(run);
			}
		};
		window.addEventListener('online', this.online);
		this.timer = window.setTimeout(() => {
			this.stopWaitingOnline();
			if (run === this.run) {
				this.setState('lost');
			}
		}, most);
		this.setState('offline');
	}

	private stopWaitingOnline(): void {
		if (this.online) {
			window.removeEventListener('online', this.online);
			this.online = null;
		}
	}

	private send(run: number, method: string, url: string, headers: Record<string, string>,
		body?: Blob, onProgress?: (loaded: number) => void): Promise<Answer> {
		return new Promise((resolve, reject) => {
			if (run !== this.run) {
				reject(new Stopped());
				return;
			}
			const xhr = new XMLHttpRequest();
			const all: Record<string, string> = { ...this.options.headers, ...headers,
				'Tus-Resumable': TUS_RESUMABLE };
			let verb = method;
			if (method === 'PATCH' && this.options.overridePatchMethod) {
				verb = 'POST';
				all['X-HTTP-Method-Override'] = 'PATCH';
			}
			xhr.open(verb, withQuery(withPathParametersOf(url, this.options.endpoint),
				this.options.query), true);
			xhr.withCredentials = !!this.options.withCredentials;
			Object.entries(all).forEach(([name, value]) => xhr.setRequestHeader(name, value));
			const stallMillis = this.options.stallMillis ?? DEFAULT_STALL_MILLIS;
			let stalled = false;
			let sentAll = false;
			let ended = false;
			let stall: number | undefined = undefined;
			const giveUp = () => {
				stalled = true;
				xhr.abort();
			};
			const watch = () => {
				window.clearTimeout(stall);
				if (stallMillis > 0 && !ended) {
					stall = window.setTimeout(() => {
						if (!sentAll) {
							giveUp();
							return;
						}
						// all handed to the network: the answer may still come, as long as the
						// server answers
						void reachable(url, this.options).then((answers) => {
							if (answers) {
								watch();
							} else if (!ended) {
								giveUp();
							}
						});
					}, stallMillis);
				}
			};
			const done = () => {
				ended = true;
				window.clearTimeout(stall);
				if (this.xhr === xhr) {
					this.xhr = null;
				}
			};
			xhr.upload.addEventListener('progress', (event) => {
				sentAll = !!body && event.loaded >= body.size;
				watch();
				onProgress?.(event.loaded);
			});
			xhr.onload = () => {
				done();
				resolve(answerOf(xhr));
			};
			xhr.onerror = () => {
				done();
				reject(new TusError('The connection to the server broke off', 0, 'network'));
			};
			xhr.ontimeout = xhr.onerror;
			xhr.onabort = () => {
				done();
				reject(stalled ? new TusError('The upload stalled', 0, 'network') : new Stopped());
			};
			this.xhr = xhr;
			watch();
			xhr.send(body ?? null);
		});
	}
}

/**
 * Where a file of a ResumableUploader is. Interrupted: left unfinished by an earlier page, until
 * the file is chosen again. Lost: waiting for the connection, see TusState.
 */
export type ResumableItemState = 'interrupted' | 'ready' | 'queued' | 'starting' | 'uploading' |
	'paused' | 'retrying' | 'offline' | 'lost' | 'done' | 'failed' | 'canceled';

/** A file of a ResumableUploader, as its callbacks tell it. */
export interface ResumableItem {
	/** Its name in the uploader. */
	readonly id: string;
	readonly name: string;
	readonly size: number;
	/** The file; null for an upload left unfinished by an earlier visit, until chosen again. */
	readonly file: File | null;
	readonly state: ResumableItemState;
	/** The bytes the server holds, or is being sent. */
	readonly sent: number;
	/** Where the server keeps the upload, once created. */
	readonly url: string | null;
	/** The name the server stored the whole file under, if it tells (STORED_NAME_HEADER). */
	readonly storedName: string | null;
	/** Why it failed. */
	readonly error: string | null;
}

/**
 * The texts of a resumable uploader. '{0}' and '{1}' stand for the values each text tells of.
 */
export interface ResumableUploaderTexts {
	/** The name of the list of the files, for assistive technology. */
	list: string;
	/** A file that waits for its start button. */
	ready: string;
	/** A file that waits until fewer are being sent. */
	queued: string;
	/** While the upload is created, or the server asked where it stopped. */
	starting: string;
	/** What was sent of how much. */
	sent: string;
	/** How fast it goes: a size per second. */
	rate: string;
	/** How long it still takes. */
	remaining: string;
	/** A file paused: what the server holds of how much. */
	paused: string;
	/** After a failure: how long until it tries again. */
	retrying: string;
	/** While the browser is offline. */
	offline: string;
	/** An upload of an earlier visit: what the server holds of how much. */
	interrupted: string;
	/** An upload waiting for the connection: what the server holds of how much. */
	disconnected: string;
	/** Once the server holds the whole file: its size. */
	done: string;
	/** Once the upload failed, unless the server tells why. */
	failed: string;
	/** Once the upload failed without an answer from the server. */
	lost: string;
	/** Once the server refused the upload: its HTTP status. */
	serverError: string;
	/** Once the user canceled the upload. */
	canceled: string;
	/** A file larger than a file may be: that size. */
	tooLarge: string;
	/** The button starting a file: its name. */
	start: string;
	/** The button pausing a file: its name. */
	pause: string;
	/** The button resuming a file: its name. */
	resume: string;
	/** The button canceling a file: its name. */
	cancel: string;
	/** The button taking a finished file out of the list: its name. */
	remove: string;
	/** The button pausing all files. */
	pauseAll: string;
	/** The button resuming all files. */
	resumeAll: string;
	/** The button taking the finished files out of the list. */
	clearFinished: string;
	/** The question once the server answers again after the connection was lost. */
	reconnected: string;
	/** The question about the uploads an earlier visit left unfinished. */
	recalled: string;
	/** The button of the question resuming the uploads. */
	resumeUploads: string;
	/** The button of the question canceling the uploads. */
	cancelUploads: string;
	/** The button of the question choosing the files of an earlier visit again. */
	continueUploads: string;
	/** The button of the question forgetting the uploads of an earlier visit. */
	discardUploads: string;
	/** The button choosing again the file of an upload of an earlier visit: its name. */
	continueFile: string;
}

/** The texts of a resumable uploader when the page gives none. */
export const DEFAULT_RESUMABLE_UPLOADER_TEXTS: ResumableUploaderTexts = {
	list: 'Uploads',
	ready: 'Ready to send',
	queued: 'Waiting for its turn',
	starting: 'Starting…',
	sent: '{0} of {1}',
	rate: '{0}/s',
	remaining: '{0} left',
	paused: 'Paused at {0} of {1}',
	retrying: 'The connection broke off, trying again in {0}',
	offline: 'Offline: goes on once the network is back',
	interrupted: 'Stopped at {0} of {1}: choose the file again to go on',
	disconnected: 'Connection lost at {0} of {1}: waiting for it',
	done: 'Uploaded {0}',
	failed: 'The upload failed',
	lost: 'The connection to the server broke off',
	serverError: 'The server refused the upload ({0})',
	canceled: 'Canceled',
	tooLarge: 'Larger than {0}, the most for a file',
	start: 'Start {0}',
	pause: 'Pause {0}',
	resume: 'Resume {0}',
	cancel: 'Cancel {0}',
	remove: 'Remove {0} from the list',
	pauseAll: 'Pause all',
	resumeAll: 'Resume all',
	clearFinished: 'Clear finished',
	reconnected: 'The connection is back. Resume the uploads?',
	recalled: 'Uploads you started earlier did not end. Continue them? Choose their files again.',
	resumeUploads: 'Resume',
	cancelUploads: 'Cancel',
	continueUploads: 'Continue',
	discardUploads: 'Discard',
	continueFile: 'Continue {0}: choose it again'
};

/**
 * What a resumable uploader asks the user: whether to go on with uploads once the server answers
 * again after the connection was lost (reconnected), or with those an earlier visit left unfinished
 * (recalled).
 */
export interface ResumableQuestion {
	readonly reason: 'reconnected' | 'recalled';
	/** The files it is about. */
	readonly items: ResumableItem[];
	/**
	 * Goes on with them; for recalled ones by opening the dialog to choose their files again, which
	 * only works while the user clicks.
	 */
	resume(): void;
	/** Cancels them, deleting them on the server. */
	cancel(): void;
}

/**
 * Where a resumable uploader sends its files, how, and what it says.
 */
export interface ResumableUploaderOptions {
	/** Where uploads are created: the address of the tus server. */
	endpoint: string;
	/** Whether several files can be chosen at once, true by default. */
	multiple?: boolean;
	/** The files the dialog offers, as the accept attribute of an input says. */
	accept?: string;
	/** The most a file may weigh, in bytes; no limit when left out. */
	fileMaxSize?: number;
	/** The bytes sent by a request at most; DEFAULT_CHUNK_SIZE when left out. */
	chunkSize?: number;
	/** How many files are sent at once, 1 by default; the others wait for their turn. */
	parallel?: number;
	/** Whether files start as soon as chosen, true by default; else each has its start button. */
	autoStart?: boolean;
	/** Whether uploads go on after a reload, by their addresses in localStorage; true by default. */
	remember?: boolean;
	/**
	 * What the uploads are remembered with, for the uploader in the same place of a later page to
	 * offer to go on with them: the class of the page and the path of the field, for example. Only
	 * the uploads remembered with the same are offered.
	 */
	rememberedAs?: string;
	/**
	 * The part of the application the uploads belong to, see TusUploadOptions.context: with one, the
	 * uploads are remembered for it, and offered and gone on with by any uploader of the same
	 * context, rather than by the one in the same place only.
	 */
	context?: string;
	/**
	 * Whether the uploader lists, as it starts, the uploads an earlier visit left unfinished and asks
	 * whether to go on with them; true by default. Without, an upload still goes on once its file is
	 * chosen again, as long as remember is.
	 */
	recalling?: boolean;
	/**
	 * The uploads the server keeps for the uploader to offer as it starts, besides those
	 * localStorage remembers: those of the user started in another browser, or in another part of
	 * the application, as Wicket's ResumableUploadField lists them. They go on once a file of the
	 * same name, size, type and date is chosen.
	 */
	recalled?: readonly RecalledUpload[];
	/**
	 * How long after it last sent something an upload counts as in use by another page, which
	 * leaves it alone rather than offering to go on with it; DEFAULT_IN_USE_MILLIS when left out.
	 */
	inUseMillis?: number;
	/**
	 * Whether to ask the user before resuming the uploads, once the server answers again after the
	 * connection was lost; true by default, else they go on by themselves.
	 */
	askingToResume?: boolean;
	/**
	 * How long to wait before each look whether the server answers again, once the connection is
	 * lost; DEFAULT_PROBE_DELAYS when left out.
	 */
	probeDelays?: readonly number[];
	/** Whether to ask before the page is left while files are being sent, true by default. */
	keepingOnPage?: boolean;
	/** The design of the picker, a zone to drop files on by default. */
	design?: FilePickerDesign;
	/** How long to wait before each try after a failure, see TusUploadOptions.retryDelays. */
	retryDelays?: readonly number[];
	/**
	 * How long a file tries again by itself before the connection counts as lost and the uploader
	 * asks, in milliseconds, see TusUploadOptions.retryTimeout; 40 seconds by default.
	 */
	retryTimeout?: number;
	/** How long a chunk may go without progress before it is taken for lost; 0 never. */
	stallMillis?: number;
	/**
	 * How often to ask the server how many bytes it holds while a chunk is being sent, for the
	 * progress; 0, the default, never. See TusUploadOptions.pollMillis: only for servers answering
	 * HEAD while a PATCH runs.
	 */
	pollMillis?: number;
	/** Headers sent with every request. */
	headers?: Record<string, string>;
	/** A query added to the address of every request, see TusUploadOptions.query. */
	query?: string;
	/**
	 * The input the files are chosen with, which the uploader takes into its element: that of
	 * Wicket's ResumableUploadToResourceField, for example. One of its own when left out.
	 */
	input?: HTMLInputElement;
	/** The words of the refusals the server tells by their keys (ERROR_KEY_HEADER). */
	errorTexts?: Record<string, string>;
	/** Whether the requests to another origin carry its cookies. */
	withCredentials?: boolean;
	/** Whether to send PATCH as POST with X-HTTP-Method-Override, for proxies refusing PATCH. */
	overridePatchMethod?: boolean;
	/** What the server is told of a file besides its name and type. */
	metadata?(file: File): Record<string, string>;
	/** Whether no file can be chosen. */
	disabled?: boolean;
	/** The texts, those of DEFAULT_RESUMABLE_UPLOADER_TEXTS where left out. */
	texts?: Partial<ResumableUploaderTexts>;
	/** The texts of the picker. */
	pickerTexts?: Partial<FilePickerTexts>;
	/** The language the sizes and durations are written in, that of the page by default. */
	locale?: string;
	/** The look; that of the element it is in when left out. */
	look?: Look;
	/** Called once the server holds the whole of a file. */
	onComplete?(item: ResumableItem): void;
	/** Called once a file failed for good. */
	onError?(item: ResumableItem, error: TusError): void;
	/** Called once the user canceled a file. */
	onCancel?(item: ResumableItem): void;
	/** Called when a file is added, taken out or changes state, with all files. */
	onChange?(items: ResumableItem[]): void;
	/**
	 * Asks the user a question in a way of the page's own, a dialog for example: returns true when
	 * it did, and answers by resume() or cancel(); else the uploader asks above its list.
	 */
	onQuestion?(question: ResumableQuestion): boolean | void;
}

/** The states in which a file is being sent, or about to be. */
const ACTIVE: readonly ResumableItemState[] = ['starting', 'uploading', 'retrying', 'offline'];

/** The states in which a file is over: its row shows a button taking it out of the list. */
const FINISHED: readonly ResumableItemState[] = ['done', 'failed', 'canceled'];

/** Two bars, for the button pausing a file. */
const PAUSE_ICON = 'M6 19h4V5H6v14zm8-14v14h4V5h-4z';

/** A triangle pointing right, for the buttons starting and resuming a file. */
const PLAY_ICON = 'M8 5v14l11-7z';

/** How far back the rate of an upload looks, in milliseconds. */
const RATE_WINDOW_MILLIS = 5000;

/** A file of the uploader, and its row. */
interface Row {
	id: string;
	name: string;
	size: number;
	file: File | null;
	fingerprint: string;
	state: ResumableItemState;
	sent: number;
	url: string | null;
	storedName: string | null;
	error: string | null;
	upload: TusUpload | null;
	/** The bytes sent and when, for the rate. */
	samples: Array<[number, number]>;
	element: HTMLLIElement;
	bar: HTMLProgressElement;
	label: HTMLSpanElement;
	status: HTMLDivElement;
	play: HTMLButtonElement;
	pause: HTMLButtonElement;
	stop: HTMLButtonElement;
}

/** A question of the uploader: about which files, and whether it asks it above its list. */
interface Question {
	reason: ResumableQuestion['reason'];
	rows: Row[];
	/** The state its files are in while they wait for the answer. */
	waitsFor: ResumableItemState;
	inUploader: boolean;
}

/** A look whether the server answers again: how many so far, the next one, and one under way. */
interface Probe {
	attempt: number;
	timer: number | undefined;
	asking: boolean;
}

/** The uploaders by their element. */
const uploaders = new WeakMap<HTMLElement, ResumableUploader>();

let counter = 0;

/**
 * Files sent in chunks to a tus server, each paused, resumed and canceled by the user, and going on
 * after a broken connection or a reload.
 */
export class ResumableUploader {
	readonly host: HTMLElement;

	options: ResumableUploaderOptions;

	/** What the uploader added to the element. */
	element!: HTMLDivElement;

	/** The file input of the picker. */
	input!: HTMLInputElement;

	picker!: FilePicker;

	/** The buttons pausing and resuming all files. */
	toolbar!: HTMLDivElement;

	pauseAllButton!: HTMLButtonElement;

	resumeAllButton!: HTMLButtonElement;

	clearButton!: HTMLButtonElement;

	/** The rows of the files. */
	list!: HTMLUListElement;

	private announcer!: HTMLDivElement;

	private rows: Row[] = [];

	private look!: Look;

	private texts!: ResumableUploaderTexts;

	private guard: UploadGuardHandle | null = null;

	private destroyed = false;

	private listeners: Array<[EventTarget, string, EventListener]> = [];

	private stopWatchingRemoval: Unsubscribe | null = null;

	/** What the uploader asks, and of which files, while one of them waits for the answer. */
	private question: Question | null = null;

	/** The question above the list, when the uploader asks it. */
	questionElement!: HTMLDivElement;

	/** The question the element shows. */
	private shownQuestion: Question | null = null;

	/** The look whether the server answers again, while files wait for the connection. */
	private probe: Probe | null = null;

	/** Stops listening for the moments to look whether the server answers again. */
	private unlistenProbe: (() => void) | null = null;

	/**
	 * @param host the element to draw into, which the framework renders empty
	 * @param options where the files go, how, and what it says
	 */
	constructor(host: HTMLElement, options: ResumableUploaderOptions) {
		this.host = host;
		this.options = { ...options };
		uploaders.get(host)?.destroy();
		uploaders.set(host, this);
		this.attach();
		this.restore();
	}

	/**
	 * Draws a resumable uploader into an element.
	 *
	 * @param host the element or its id
	 * @param options where the files go, how, and what it says
	 * @returns the uploader, null when there is no such element
	 */
	static attach(host: HTMLElement | string, options: ResumableUploaderOptions):
		ResumableUploader | null {
		const element = typeof host === 'string' ? document.getElementById(host) : host;
		return element ? new ResumableUploader(element, options) : null;
	}

	/**
	 * @param host the element or its id
	 * @returns the uploader drawn into an element, if any
	 */
	static of(host: HTMLElement | string | null | undefined): ResumableUploader | null {
		const element = typeof host === 'string' ? document.getElementById(host) : host;
		return (element && uploaders.get(element)) ?? null;
	}

	/** The files, in the order they were added. */
	get items(): ResumableItem[] {
		return this.rows.map((row) => itemOf(row));
	}

	private attach(): void {
		const look = this.look = this.options.look ?? lookOf(this.host);
		this.texts = { ...DEFAULT_RESUMABLE_UPLOADER_TEXTS, ...this.options.texts };
		this.element = create('div', look, 'resumable-upload');
		this.input = this.options.input ?? document.createElement('input');
		this.input.type = 'file';
		if (!this.options.input) {
			this.input.id = (this.host.id || 'antilia-resumable-upload-' + (++counter)) + '-files';
		}
		this.element.append(this.input);
		this.toolbar = create('div', look, 'resumable-upload-toolbar');
		this.pauseAllButton = button(look, 'resumable-upload-pause-all', 'secondary', 'small',
			PAUSE_ICON, this.texts.pauseAll);
		this.resumeAllButton = button(look, 'resumable-upload-resume-all', 'secondary', 'small',
			PLAY_ICON, this.texts.resumeAll);
		this.clearButton = button(look, 'resumable-upload-clear', 'ghost', 'small', CLEAR_ICON,
			this.texts.clearFinished);
		this.toolbar.append(this.pauseAllButton, this.resumeAllButton, this.clearButton);
		this.questionElement = create('div', look, 'resumable-upload-question');
		this.questionElement.hidden = true;
		this.list = create('ul', look, 'resumable-upload-list');
		this.list.setAttribute('aria-label', this.texts.list);
		this.announcer = create('div', look, 'resumable-upload-announcer');
		this.announcer.setAttribute('role', 'status');
		this.host.append(this.element);
		this.configureInput();
		this.picker = new FilePicker(this.input, this.pickerOptions());
		this.element.append(this.questionElement, this.toolbar, this.list, this.announcer);

		this.listen(this.input, 'change', () => {
			const files = Array.from(this.input.files ?? []);
			if (files.length) {
				this.add(files);
				this.picker.clear();
			}
		});
		this.listen(this.pauseAllButton, 'click', () => this.pauseAll());
		this.listen(this.resumeAllButton, 'click', () => this.resumeAll());
		this.listen(this.clearButton, 'click', () => this.clearFinished());
		// once the page is left, the next one offers to go on with what is being sent
		this.listen(window, 'pagehide', () => this.rows.forEach((row) => row.upload?.leave()));
		this.stopWatchingRemoval = onRemoving(this.host, () => this.destroy());
		this.rows.forEach((row) => this.list.append(row.element));
		this.renderToolbar();
	}

	private configureInput(): void {
		this.input.multiple = this.options.multiple !== false;
		if (this.options.accept) {
			this.input.accept = this.options.accept;
		} else {
			this.input.removeAttribute('accept');
		}
		this.input.disabled = !!this.options.disabled;
	}

	private pickerOptions(): FilePickerOptions {
		return { design: this.options.design ?? 'drop-zone', texts: this.options.pickerTexts,
			locale: this.options.locale, look: this.look };
	}

	private listen(target: EventTarget, type: string, listener: EventListener): void {
		target.addEventListener(type, listener);
		this.listeners.push([target, type, listener]);
	}

	/**
	 * Takes other options: the files being sent keep going as they started.
	 */
	configure(options: ResumableUploaderOptions): void {
		this.options = { ...options };
		this.look = this.options.look ?? lookOf(this.host);
		this.texts = { ...DEFAULT_RESUMABLE_UPLOADER_TEXTS, ...this.options.texts };
		this.configureInput();
		this.picker.configure(this.pickerOptions());
		this.pauseAllButton.replaceWith(this.pauseAllButton = this.rebuilt(this.pauseAllButton,
			button(this.look, 'resumable-upload-pause-all', 'secondary', 'small', PAUSE_ICON,
				this.texts.pauseAll)));
		this.resumeAllButton.replaceWith(this.resumeAllButton = this.rebuilt(this.resumeAllButton,
			button(this.look, 'resumable-upload-resume-all', 'secondary', 'small', PLAY_ICON,
				this.texts.resumeAll)));
		this.clearButton.replaceWith(this.clearButton = this.rebuilt(this.clearButton,
			button(this.look, 'resumable-upload-clear', 'ghost', 'small', CLEAR_ICON,
				this.texts.clearFinished)));
		this.list.setAttribute('aria-label', this.texts.list);
		this.rows.forEach((row) => this.render(row));
		this.renderToolbar();
		// in the new texts and look
		this.shownQuestion = null;
		this.renderQuestion();
		this.updateGuard();
	}

	/**
	 * @returns the new button, listened to as the old one was
	 */
	private rebuilt(old: HTMLButtonElement, next: HTMLButtonElement): HTMLButtonElement {
		const listeners = this.listeners.filter(([target]) => target === old);
		listeners.forEach(([, type, listener]) => {
			old.removeEventListener(type, listener);
			next.addEventListener(type, listener);
		});
		this.listeners = this.listeners.map((entry) => entry[0] === old
			? [next, entry[1], entry[2]] : entry);
		return next;
	}

	/**
	 * Adds files: each goes on with an upload of the same file left unfinished, or starts anew;
	 * those larger than fileMaxSize fail at once. Unless multiple, the first file only.
	 */
	add(files: readonly File[]): void {
		const taken = this.options.multiple === false ? files.slice(0, 1) : files;
		taken.forEach((file) => {
			const fingerprint = fingerprintOf(file, this.options.endpoint, scopeOf(this.options));
			const same = this.rows.find((row) => row.fingerprint === fingerprint &&
				!FINISHED.includes(row.state));
			if (same && same.state !== 'interrupted') {
				return;
			}
			const row = same ?? this.newRow(file.name, file.size, fingerprint);
			row.file = file;
			row.error = null;
			const max = this.options.fileMaxSize;
			if (max !== undefined && max >= 0 && file.size > max) {
				row.state = 'failed';
				row.error = format(this.texts.tooLarge, formatBytes(max, this.options.locale));
				row.upload = null;
			} else {
				row.upload = this.newUpload(row, file);
				row.state = this.options.autoStart === false ? 'ready' : 'queued';
			}
			this.render(row);
			this.announce(row);
		});
		this.pump();
		this.changed();
	}

	private newRow(name: string, size: number, fingerprint: string): Row {
		const look = this.look;
		const element = create('li', look, 'resumable-upload-item');
		const head = create('div', look, 'resumable-upload-head');
		const title = create('span', look, 'resumable-upload-name', name);
		const sizeText = create('span', look, 'resumable-upload-size',
			formatBytes(size, this.options.locale));
		const actions = create('span', look, 'resumable-upload-actions');
		const play = button(look, 'resumable-upload-play', 'ghost', 'small', PLAY_ICON, '', true);
		const pause = button(look, 'resumable-upload-pause', 'ghost', 'small', PAUSE_ICON, '', true);
		const stop = button(look, 'resumable-upload-stop', 'ghost', 'small', CLEAR_ICON, '', true);
		actions.append(play, pause, stop);
		head.append(title, sizeText, actions);
		const bar = create('div', look, 'progress');
		bar.classList.add(...partClasses(look, 'progress-still').split(' ').filter(Boolean));
		const value = create('progress', look, 'progress-value');
		value.max = 100;
		value.setAttribute('aria-label', name);
		const label = create('span', look, 'progress-label');
		label.setAttribute('aria-hidden', 'true');
		bar.append(value, label);
		const status = create('div', look, 'upload-status');
		element.append(head, bar, status);
		const row: Row = { id: 'antilia-resumable-' + (++counter), name, size, file: null,
			fingerprint, state: 'ready', sent: 0, url: null, storedName: null, error: null,
			upload: null, samples: [],
			element, bar: value, label, status, play, pause, stop };
		element.dataset.id = row.id;
		play.addEventListener('click', () => this.resume(row.id));
		pause.addEventListener('click', () => this.pause(row.id));
		stop.addEventListener('click', () => {
			if (FINISHED.includes(row.state)) {
				this.remove(row.id);
			} else {
				void this.cancel(row.id);
			}
		});
		this.rows.push(row);
		this.list.append(element);
		return row;
	}

	private newUpload(row: Row, file: File): TusUpload {
		const options = this.options;
		// the server keeps the place of an upload, for a later page in the same place to offer it
		const place: Record<string, string> = options.rememberedAs ? { place: options.rememberedAs }
			: {};
		const upload = new TusUpload(file, { endpoint: options.endpoint,
			chunkSize: options.chunkSize, metadata: { ...place, ...options.metadata?.(file) },
			headers: options.headers, query: options.query, withCredentials: options.withCredentials,
			retryDelays: options.retryDelays, retryTimeout: options.retryTimeout,
			stallMillis: options.stallMillis,
			pollMillis: options.pollMillis, remember: options.remember,
			rememberedAs: options.rememberedAs, context: options.context,
			inUseMillis: options.inUseMillis, overridePatchMethod: options.overridePatchMethod,
			onProgress: (sent) => {
				if (row.upload === upload) {
					this.onProgress(row, sent);
				}
			},
			onState: (state) => {
				if (row.upload === upload) {
					this.onUploadState(row, upload, state);
				}
			}
		});
		return upload;
	}

	private onProgress(row: Row, sent: number): void {
		if (this.destroyed) {
			return;
		}
		row.sent = sent;
		row.url = row.upload?.url ?? row.url;
		const now = Date.now();
		row.samples.push([now, sent]);
		while (row.samples.length > 2 && now - row.samples[0][0] > RATE_WINDOW_MILLIS) {
			row.samples.shift();
		}
		this.render(row);
	}

	private onUploadState(row: Row, upload: TusUpload, state: TusState): void {
		if (this.destroyed || state === 'idle') {
			return;
		}
		row.state = state;
		row.url = upload.url ?? row.url;
		if (state === 'uploading' || state === 'starting') {
			row.samples = [];
			row.error = null;
		}
		if (state === 'paused' || state === 'retrying' || state === 'offline' || state === 'lost') {
			row.sent = upload.offset;
		}
		if (state === 'failed') {
			row.error = this.messageOf(upload.error);
		}
		this.render(row);
		if (state !== 'uploading') {
			this.announce(row);
		}
		if (state === 'done') {
			row.sent = row.size;
			row.storedName = upload.storedName;
			this.render(row);
			this.options.onComplete?.(itemOf(row));
		} else if (state === 'failed' && upload.error) {
			this.options.onError?.(itemOf(row), upload.error);
		} else if (state === 'canceled') {
			this.options.onCancel?.(itemOf(row));
		}
		if (state === 'lost') {
			this.lost();
		}
		if (state === 'paused' || state === 'done' || state === 'failed' || state === 'canceled') {
			this.pump();
		}
		this.changed();
	}

	/**
	 * A file lost the connection: those waiting for their turn wait for it too, while the uploader
	 * looks whether the server answers again.
	 */
	private lost(): void {
		this.rows.filter((row) => row.state === 'queued').forEach((row) => {
			row.state = 'lost';
			this.render(row);
		});
		this.renderQuestion();
		this.watch();
	}

	/**
	 * Looks whether the server answers again while files wait for the connection: after a while,
	 * longer each time, and whenever the browser is online again or the page looked at again.
	 */
	private watch(): void {
		if (this.probe || this.destroyed) {
			return;
		}
		const probe: Probe = { attempt: 0, timer: undefined, asking: false };
		this.probe = probe;
		const later = () => {
			const delays = this.options.probeDelays ?? DEFAULT_PROBE_DELAYS;
			probe.timer = window.setTimeout(check,
				delays[Math.min(probe.attempt++, delays.length - 1)]);
		};
		const check = () => {
			if (this.probe !== probe || probe.asking) {
				return;
			}
			const lost = this.rows.filter((row) => row.state === 'lost');
			if (!lost.length) {
				this.unwatch();
				return;
			}
			window.clearTimeout(probe.timer);
			probe.asking = true;
			const url = lost.find((row) => row.url)?.url ?? absoluteUrl(this.options.endpoint);
			void reachable(url, this.options).then((answers) => {
				probe.asking = false;
				if (this.probe !== probe) {
					return;
				}
				if (answers) {
					this.unwatch();
					this.reconnected();
				} else {
					later();
				}
			});
		};
		const listener = () => check();
		this.listen(window, 'online', listener);
		this.listen(window, 'focus', listener);
		this.listen(document, 'visibilitychange', listener);
		this.unlistenProbe = () => {
			this.listeners.filter(([, , each]) => each === listener).forEach(([target, type]) =>
				target.removeEventListener(type, listener));
			this.listeners = this.listeners.filter(([, , each]) => each !== listener);
		};
		later();
	}

	/** Stops looking whether the server answers again. */
	private unwatch(): void {
		if (this.probe) {
			window.clearTimeout(this.probe.timer);
			this.probe = null;
		}
		this.unlistenProbe?.();
		this.unlistenProbe = null;
	}

	/**
	 * Goes on with the files that waited for the connection, now that the server answers again, or
	 * asks the user first.
	 */
	private reconnected(): void {
		const lost = this.rows.filter((row) => row.state === 'lost');
		if (!lost.length) {
			return;
		}
		if (this.options.askingToResume === false) {
			this.resumeRows(lost);
		} else {
			this.ask('reconnected', lost);
		}
	}

	/**
	 * Asks the user whether to go on with files: through onQuestion, else above the list until
	 * the user answers.
	 */
	private ask(reason: ResumableQuestion['reason'], rows: Row[]): void {
		const question: Question = { reason, rows,
			waitsFor: reason === 'recalled' ? 'interrupted' : 'lost', inUploader: true };
		this.question = question;
		const asked = this.options.onQuestion?.({
			reason, items: rows.map((row) => itemOf(row)),
			resume: () => this.answer(question, true),
			cancel: () => this.answer(question, false)
		});
		question.inUploader = asked !== true;
		this.renderQuestion();
		if (question.inUploader) {
			this.announcer.textContent = this.questionText();
		}
	}

	/**
	 * Answers a question: goes on with its files, or cancels them.
	 */
	private answer(question: Question, resume: boolean): void {
		const rows = question.rows.filter((row) => this.rows.includes(row) &&
			row.state === question.waitsFor);
		if (question.reason === 'recalled') {
			if (resume) {
				this.input.click();
			} else {
				rows.forEach((row) => this.remove(row.id));
			}
		} else if (resume) {
			this.resumeRows(rows);
		} else {
			rows.forEach((row) => void this.cancel(row.id));
		}
		this.renderQuestion();
	}

	private questionText(): string {
		return this.question?.reason === 'recalled' ? this.texts.recalled : this.texts.reconnected;
	}

	/**
	 * Shows the question above the list, while a file it is about waits for the answer.
	 */
	private renderQuestion(): void {
		const question = this.question;
		if (question && !this.rows.some((row) => question.rows.includes(row) &&
			row.state === question.waitsFor)) {
			this.question = null;
		}
		const element = this.questionElement;
		const shown = !!this.question?.inUploader && !this.destroyed;
		if (!shown) {
			element.hidden = true;
			element.replaceChildren();
			this.shownQuestion = null;
			return;
		}
		const current = this.question!;
		if (this.shownQuestion === current) {
			return;
		}
		this.shownQuestion = current;
		const look = this.look;
		const recalled = current.reason === 'recalled';
		const text = create('span', look, 'resumable-upload-question-text', this.questionText());
		const actions = create('span', look, 'resumable-upload-question-actions');
		const yes = button(look, 'resumable-upload-question-resume', 'primary', 'small',
			PLAY_ICON, recalled ? this.texts.continueUploads : this.texts.resumeUploads);
		const no = button(look, 'resumable-upload-question-cancel', 'secondary', 'small',
			CLEAR_ICON, recalled ? this.texts.discardUploads : this.texts.cancelUploads);
		yes.addEventListener('click', () => this.answer(current, true));
		no.addEventListener('click', () => this.answer(current, false));
		actions.append(yes, no);
		element.replaceChildren(text, actions);
		element.dataset.reason = current.reason;
		element.hidden = false;
	}

	/**
	 * Lets files that waited for the connection wait for their turn again.
	 */
	private resumeRows(rows: Row[]): void {
		rows.filter((row) => row.upload && ['lost', 'paused', 'failed', 'ready'].includes(row.state))
			.forEach((row) => {
				row.state = 'queued';
				this.render(row);
			});
		this.pump();
		this.renderQuestion();
		this.changed();
	}

	private messageOf(error: TusError | null): string {
		if (!error) {
			return this.texts.failed;
		}
		const known = error.key ? this.options.errorTexts?.[error.key] : undefined;
		if (known) {
			return known;
		}
		if (error.status === 0) {
			return this.texts.lost;
		}
		return error.message && !error.message.startsWith('The server answered')
			? error.message : format(this.texts.serverError, error.status);
	}

	/**
	 * Starts the files waiting for their turn, as many as are sent at once.
	 */
	private pump(): void {
		if (this.destroyed) {
			return;
		}
		const parallel = Math.max(1, this.options.parallel ?? 1);
		let active = this.rows.filter((row) => ACTIVE.includes(row.state)).length;
		for (const row of this.rows) {
			if (active >= parallel) {
				break;
			}
			if (row.state === 'queued' && row.upload) {
				active++;
				row.upload.start();
			}
		}
		this.updateGuard();
	}

	private find(id: string): Row | undefined {
		return this.rows.find((row) => row.id === id);
	}

	/**
	 * Starts a file waiting for its start button, or resumes one paused, failed or waiting for the
	 * connection: it waits for its turn if as many as are sent at once are being sent. A file of an
	 * earlier visit goes on once chosen again: this opens the dialog to choose it, which only works
	 * while the user clicks.
	 */
	resume(id: string): void {
		const row = this.find(id);
		if (row?.state === 'interrupted') {
			// an upload of an earlier visit goes on once its file is chosen again
			this.input.click();
			return;
		}
		if (!row?.upload || !['ready', 'paused', 'failed', 'lost'].includes(row.state)) {
			return;
		}
		this.resumeRows([row]);
	}

	/** Starts a file waiting for its start button: resume() does. */
	start(id: string): void {
		this.resume(id);
	}

	/**
	 * Pauses a file being sent, or waiting for its turn.
	 */
	pause(id: string): void {
		const row = this.find(id);
		if (!row) {
			return;
		}
		if (row.state === 'queued' || row.state === 'lost') {
			row.state = 'paused';
			this.render(row);
			this.announce(row);
			this.updateGuard();
			this.changed();
		} else if (ACTIVE.includes(row.state)) {
			row.upload?.pause();
		}
	}

	/**
	 * Cancels a file: the server deletes what it got, the row stays, canceled, until taken out.
	 */
	async cancel(id: string): Promise<void> {
		const row = this.find(id);
		if (!row || FINISHED.includes(row.state)) {
			return;
		}
		const upload = row.upload;
		if (upload && upload.state !== 'idle') {
			await upload.cancel();
			return;
		}
		// not started yet, or left by an earlier visit
		const url = row.url ?? TusStorage.find(row.fingerprint)?.url ?? null;
		TusStorage.remove(row.fingerprint);
		row.state = 'canceled';
		this.render(row);
		this.announce(row);
		this.options.onCancel?.(itemOf(row));
		this.pump();
		this.changed();
		if (url) {
			await terminate(url, this.options, retryDelaysOf(this.options)).catch(() => false);
		}
	}

	/**
	 * Takes a file out of the list, canceling it first unless it is done.
	 */
	remove(id: string): void {
		const row = this.find(id);
		if (!row) {
			return;
		}
		if (!FINISHED.includes(row.state)) {
			void this.cancel(id);
		}
		row.upload = null;
		row.element.remove();
		this.rows = this.rows.filter((other) => other !== row);
		this.pump();
		this.renderToolbar();
		this.changed();
		(this.rows.length ? this.list.querySelector<HTMLButtonElement>('button:not([hidden])')
			: this.picker.button)?.focus();
	}

	/** Pauses every file being sent, waiting for its turn or for the connection. */
	pauseAll(): void {
		this.rows.filter((row) => row.state === 'queued' || row.state === 'lost' ||
			ACTIVE.includes(row.state)).forEach((row) => this.pause(row.id));
	}

	/**
	 * Starts or resumes every file waiting for its start button or for the connection, paused or
	 * failed.
	 */
	resumeAll(): void {
		this.resumeRows(this.rows.filter((row) => row.upload &&
			['ready', 'paused', 'failed', 'lost'].includes(row.state)));
	}

	/** Cancels every file not finished, deleting it on the server. */
	async cancelAll(): Promise<void> {
		await Promise.all(this.rows.filter((row) => !FINISHED.includes(row.state))
			.map((row) => this.cancel(row.id)));
	}

	/** Takes the files done, failed or canceled out of the list. */
	clearFinished(): void {
		this.rows.filter((row) => FINISHED.includes(row.state)).forEach((row) => {
			row.element.remove();
			row.upload = null;
		});
		this.rows = this.rows.filter((row) => !FINISHED.includes(row.state));
		this.renderToolbar();
		this.changed();
		this.picker.button.focus();
	}

	/**
	 * Keeps the uploads the server offers, options.recalled, as localStorage would have them, unless
	 * it does already: the uploader then offers them as those of an earlier visit, and goes on with
	 * one once its file is chosen.
	 */
	private keepRecalled(place: string | null): void {
		const endpoint = this.options.endpoint!;
		const base = withoutPathParameters(endpoint);
		const kept = TusStorage.list(endpoint).filter((stored) => this.belongs(stored))
			.map((stored) => absoluteUrl(stored.url));
		(this.options.recalled ?? []).forEach((upload) => {
			const url = absoluteUrl(base + '/' + encodeURIComponent(upload.id));
			if (!kept.includes(url)) {
				TusStorage.save({ fingerprint: fingerprintFor(endpoint, upload.name, upload.size,
					upload.type, upload.lastModified, scopeOf(this.options)), url,
					endpoint: absoluteUrl(base), name: upload.name, size: upload.size,
					type: upload.type, lastModified: upload.lastModified, stored: Date.now(),
					rememberedAs: place ?? undefined, context: this.options.context, alive: 0 });
			}
		});
	}

	/**
	 * @returns whether the uploader goes on with a kept upload: one of its context, else one of its
	 * place
	 */
	private belongs(stored: StoredUpload): boolean {
		return this.options.context ? stored.context === this.options.context
			: !stored.context && (stored.rememberedAs ?? null) === (this.options.rememberedAs ?? null);
	}

	/**
	 * Shows the uploads to the endpoint an earlier visit left unfinished, remembered with the same
	 * place as this uploader, once the server told how far each got, and asks whether to go on
	 * with them; those it no longer has are forgotten. Those another page is sending are left alone.
	 */
	private restore(): void {
		if (this.options.remember === false || this.options.recalling === false ||
			!this.options.endpoint) {
			return;
		}
		const place = this.options.rememberedAs ?? null;
		this.keepRecalled(place);
		const asked = TusStorage.list(this.options.endpoint).map((stored) => {
			if (!this.belongs(stored) ||
				TusStorage.sending(stored, this.options.inUseMillis) ||
				this.rows.some((row) => row.fingerprint === stored.fingerprint)) {
				return Promise.resolve();
			}
			const row = this.newRow(stored.name, stored.size, stored.fingerprint);
			row.state = 'interrupted';
			row.url = stored.url;
			this.render(row);
			return headOf(stored.url, this.options).then((found) => {
				if (row.state !== 'interrupted' || this.destroyed) {
					return;
				}
				if (found === null || found === 'gone' || found.offset >= stored.size ||
					(found.length !== null && found.length !== stored.size)) {
					// no answer, or a refusal: kept for a page that gets one
					if (found !== null) {
						TusStorage.remove(stored.fingerprint);
					}
					row.element.remove();
					this.rows = this.rows.filter((other) => other !== row);
					this.renderToolbar();
					this.changed();
					return;
				}
				row.sent = found.offset;
				this.render(row);
				this.changed();
			});
		});
		this.renderToolbar();
		void Promise.all(asked).then(() => {
			const interrupted = this.rows.filter((row) => row.state === 'interrupted');
			if (interrupted.length && !this.destroyed) {
				this.ask('recalled', interrupted);
			}
		});
	}

	private statusOf(row: Row): string {
		const texts = this.texts;
		const locale = this.options.locale;
		const of = () => [formatBytes(row.sent, locale), formatBytes(row.size, locale)];
		switch (row.state) {
			case 'interrupted':
				return format(texts.interrupted, ...of());
			case 'ready':
				return texts.ready;
			case 'queued':
				return texts.queued;
			case 'starting':
				return texts.starting;
			case 'paused':
				return format(texts.paused, ...of());
			case 'retrying':
				return format(texts.retrying, formatDuration((row.upload?.retryDelay ?? 0) / 1000,
					locale));
			case 'offline':
				return texts.offline;
			case 'lost':
				return format(texts.disconnected, ...of());
			case 'done':
				return format(texts.done, formatBytes(row.size, locale));
			case 'failed':
				return row.error ?? texts.failed;
			case 'canceled':
				return texts.canceled;
			default:
				return this.progressOf(row);
		}
	}

	private progressOf(row: Row): string {
		const texts = this.texts;
		const locale = this.options.locale;
		const parts = [format(texts.sent, formatBytes(row.sent, locale),
			formatBytes(row.size, locale))];
		const first = row.samples[0];
		const last = row.samples[row.samples.length - 1];
		if (first && last && last[0] - first[0] >= 500 && last[1] > first[1]) {
			const rate = (last[1] - first[1]) * 1000 / (last[0] - first[0]);
			parts.push(format(texts.rate, formatBytes(rate, locale)),
				format(texts.remaining, formatDuration((row.size - row.sent) / rate, locale)));
		}
		return parts.join(' · ');
	}

	private render(row: Row): void {
		if (this.destroyed) {
			return;
		}
		const look = this.look;
		const state = row.state;
		row.element.dataset.state = state;
		toggleParts(row.element, look, state === 'done', 'resumable-upload-item-done');
		toggleParts(row.element, look, state === 'failed' || state === 'canceled',
			'resumable-upload-item-failed');
		toggleParts(row.element, look, state === 'paused' || state === 'interrupted' ||
			state === 'lost', 'resumable-upload-item-paused');
		const percent = state === 'done' ? 100
			: row.size > 0 ? Math.floor(row.sent * 100 / row.size) : 0;
		if (state === 'starting' && row.sent === 0) {
			row.bar.removeAttribute('value');
			row.bar.textContent = '';
			row.label.hidden = true;
		} else {
			row.bar.value = percent;
			row.bar.setAttribute('value', String(percent));
			row.bar.textContent = percent + '%';
			row.label.textContent = percent + '%';
			row.label.hidden = false;
		}
		row.status.textContent = this.statusOf(row);
		toggleParts(row.status, look, state === 'done', 'upload-status-done');
		toggleParts(row.status, look, state === 'failed' || state === 'canceled',
			'upload-status-failed');

		const name = row.name;
		const label = (element: HTMLButtonElement, text: string) => {
			element.setAttribute('aria-label', text);
			element.title = text;
		};
		row.play.hidden = !(state === 'interrupted' ||
			(row.upload && ['ready', 'paused', 'failed', 'lost'].includes(state)));
		label(row.play, format(state === 'ready' ? this.texts.start
			: state === 'interrupted' ? this.texts.continueFile : this.texts.resume, name));
		row.pause.hidden = !(state === 'queued' || ACTIVE.includes(state));
		label(row.pause, format(this.texts.pause, name));
		const finished = FINISHED.includes(state);
		label(row.stop, format(finished ? this.texts.remove : this.texts.cancel, name));
		this.renderToolbar();
	}

	private renderToolbar(): void {
		const rows = this.rows;
		this.toolbar.hidden = !rows.length;
		this.list.hidden = !rows.length;
		this.pauseAllButton.hidden = !rows.some((row) => row.state === 'queued' ||
			ACTIVE.includes(row.state));
		this.resumeAllButton.hidden = !rows.some((row) => row.upload &&
			['ready', 'paused', 'failed', 'lost'].includes(row.state));
		this.clearButton.hidden = !rows.some((row) => FINISHED.includes(row.state));
		this.list.toggleAttribute('aria-busy', rows.some((row) => ACTIVE.includes(row.state)));
	}

	private announce(row: Row): void {
		this.announcer.textContent = `${row.name}: ${this.statusOf(row)}`;
	}

	private updateGuard(): void {
		const busy = !this.destroyed && this.options.keepingOnPage !== false &&
			this.rows.some((row) => row.state === 'queued' || ACTIVE.includes(row.state));
		if (busy && !this.guard) {
			this.guard = UploadGuard.begin({ leaving: true });
		} else if (!busy && this.guard) {
			this.guard.end();
			this.guard = null;
		}
	}

	private changed(): void {
		if (!this.destroyed) {
			this.renderQuestion();
			this.options.onChange?.(this.items);
		}
	}

	/**
	 * Takes the uploader away: the files being sent stop, and go on after a reload.
	 */
	destroy(): void {
		if (this.destroyed) {
			return;
		}
		this.destroyed = true;
		this.unwatch();
		this.rows.forEach((row) => row.upload?.abort());
		this.guard?.end();
		this.guard = null;
		this.listeners.forEach(([target, type, listener]) =>
			target.removeEventListener(type, listener));
		this.listeners = [];
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
		this.picker.destroy();
		this.element.remove();
		if (uploaders.get(this.host) === this) {
			uploaders.delete(this.host);
		}
	}
}

function itemOf(row: Row): ResumableItem {
	return { id: row.id, name: row.name, size: row.size, file: row.file, state: row.state,
		sent: row.sent, url: row.url, storedName: row.storedName, error: row.error };
}

/**
 * A resumable uploader as a Svelte action, or for any page: resumableUploader(element, options).
 *
 * @returns update() taking other options, destroy() taking the uploader away
 */
export function resumableUploader(element: HTMLElement, options: ResumableUploaderOptions): {
	update(options: ResumableUploaderOptions): void;
	destroy(): void;
} {
	const instance = new ResumableUploader(element, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
