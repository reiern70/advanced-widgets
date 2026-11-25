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

/** How long to wait before each try after a failure, in milliseconds. */
export const DEFAULT_RETRY_DELAYS: readonly number[] = Object.freeze([0, 1000, 3000, 5000, 10000,
	20000]);

/** How long a request sending a chunk may go without progress before it is taken for lost. */
export const DEFAULT_STALL_MILLIS = 30000;

/** The content type of the chunks, as tus wants it. */
const CHUNK_TYPE = 'application/offset+octet-stream';

/**
 * The header in which Wicket's ResumableUploadResourceReference tells the name it stored a whole
 * file under: in the answer completing the upload, and in those to HEAD after it.
 */
export const STORED_NAME_HEADER = 'Wicket-Upload-File-Name';

/** The header in which Wicket's ResumableUploadResourceReference tells the key of a refusal. */
export const ERROR_KEY_HEADER = 'Wicket-Upload-Error';

/** Where TusStorage keeps the addresses of the uploads, before their fingerprints. */
const STORAGE_PREFIX = 'antilia-tus::';

/** Where an upload is. */
export type TusState = 'idle' | 'starting' | 'uploading' | 'paused' | 'retrying' | 'offline' |
	'done' | 'failed' | 'canceled';

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
	/** How long to wait before each try after a failure; DEFAULT_RETRY_DELAYS when left out. */
	retryDelays?: readonly number[];
	/** How long a chunk may go without progress before it is taken for lost; 0 never. */
	stallMillis?: number;
	/**
	 * How often to ask the server how many bytes it holds while a chunk is being sent, its answer
	 * being the progress then, rather than what the browser handed to the network; 0, the default,
	 * never. Only for servers answering HEAD while a PATCH runs, as the ResumableUploads of the
	 * widgets do: tusd locks the upload instead.
	 */
	pollMillis?: number;
	/** Whether the address of the upload is kept to go on after a reload, true by default. */
	remember?: boolean;
	/** Whether to send PATCH as POST with X-HTTP-Method-Override, for proxies refusing PATCH. */
	overridePatchMethod?: boolean;
	/** Called as bytes go: those the server holds or is being sent, of all. */
	onProgress?(sent: number, total: number, upload: TusUpload): void;
	/** Called whenever the state changes. */
	onState?(state: TusState, upload: TusUpload): void;
	/** Called once the server holds the whole file. */
	onSuccess?(upload: TusUpload): void;
	/** Called once the upload failed for good. */
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
 * @returns the fingerprint of a file sent to a tus server: the same file chosen again, to the same
 * server, has the same one
 */
export function fingerprintOf(file: Blob, endpoint: string): string {
	const named = file as Partial<File>;
	return [absoluteUrl(endpoint), named.name ?? '', file.size, file.type,
		named.lastModified ?? 0].join('::');
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
	const kind: TusErrorKind = status === 0 ? 'network'
		: status === 404 || status === 410 ? 'gone'
			: status === 409 || status === 423 || status === 429 || (status >= 500 && status !== 501)
				? 'server' : 'refused';
	const plain = (answer.header('Content-Type') ?? '').toLowerCase().startsWith('text/plain');
	const text = plain && answer.text.length < 500 ? answer.text.trim() : '';
	return new TusError(text || `The server answered ${status}`, status, kind,
		answer.header(ERROR_KEY_HEADER));
}

/**
 * @returns an address with a query added, if any
 */
export function withQuery(url: string, query: string | undefined): string {
	return query ? url + (url.includes('?') ? '&' : '?') + query : url;
}

/** An upload kept in localStorage, to go on with after a reload. */
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
	 * @returns the uploads not finished yet to a tus server, the oldest first
	 */
	list(endpoint: string): StoredUpload[] {
		const store = storage();
		if (!store) {
			return [];
		}
		const absolute = absoluteUrl(endpoint);
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

/** The options of a request outside an upload: its headers and credentials. */
export type TusRequestOptions = Pick<TusUploadOptions, 'headers' | 'withCredentials' | 'query'>;

function request(method: string, url: string, options: TusRequestOptions | undefined):
	Promise<Answer> {
	return new Promise((resolve) => {
		const xhr = new XMLHttpRequest();
		xhr.open(method, withQuery(url, options?.query), true);
		xhr.withCredentials = !!options?.withCredentials;
		Object.entries({ ...options?.headers, 'Tus-Resumable': TUS_RESUMABLE })
			.forEach(([name, value]) => xhr.setRequestHeader(name, value));
		xhr.onload = () => resolve(answerOf(xhr));
		xhr.onerror = () => resolve({ status: 0, header: () => null, text: '' });
		xhr.send(null);
	});
}

/**
 * Deletes an upload on the server, finished or not (the termination extension of tus).
 *
 * @returns whether the server deleted it, or did not have it
 */
export async function terminate(url: string, options?: TusRequestOptions): Promise<boolean> {
	const answer = await request('DELETE', url, options);
	return answer.status === 204 || answer.status === 200 || answer.status === 404 ||
		answer.status === 410;
}

/**
 * Asks the server how far an upload got.
 *
 * @returns the bytes it holds and those of the file; null when it does not have the upload, or
 * did not answer
 */
export async function probe(url: string, options?: TusRequestOptions):
	Promise<{ offset: number; length: number | null } | null> {
	const answer = await request('HEAD', url, options);
	const offset = Number(answer.header('Upload-Offset'));
	if (answer.status < 200 || answer.status >= 300 || answer.header('Upload-Offset') === null ||
		!Number.isFinite(offset)) {
		return null;
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

	/** The run going on: a pause, a cancellation or a start begin another one. */
	private run = 0;

	private xhr: XMLHttpRequest | null = null;

	private timer: number | undefined = undefined;

	private online: (() => void) | null = null;

	/** Whether the offset is the server's, rather than one to ask it for. */
	private offsetKnown = false;

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
		return fingerprintOf(this.file, this.options.endpoint);
	}

	/** Whether it is being sent, or about to be after a failure. */
	get running(): boolean {
		return this.state === 'starting' || this.state === 'uploading' ||
			this.state === 'retrying' || this.state === 'offline';
	}

	/**
	 * Starts sending the file, or goes on after a pause or a failure: asks the server first where it
	 * stopped. Does nothing while it runs, once done or canceled.
	 */
	start(): void {
		if (this.running || this.state === 'done' || this.state === 'canceled') {
			return;
		}
		this.attempts = 0;
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
			await terminate(url, this.options).catch(() => false);
		}
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

	private remember(): void {
		if (this.options.remember === false || !this.url) {
			return;
		}
		const named = this.file as Partial<File>;
		TusStorage.save({ fingerprint: this.fingerprint, url: this.url,
			endpoint: absoluteUrl(this.options.endpoint), name: named.name ?? '', size: this.size,
			type: this.file.type, lastModified: named.lastModified ?? 0, stored: Date.now() });
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
					}, this.file.slice(start, end), polling ? undefined
						: (loaded) => this.progress(start + loaded));
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
	 * file, is forgotten, to be created again.
	 */
	private async ask(run: number): Promise<void> {
		const answer = await this.send(run, 'HEAD', this.url!, {});
		if (answer.status === 403 || answer.status === 404 || answer.status === 410) {
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
		const named = this.file as Partial<File>;
		const metadata = { filename: named.name ?? '', filetype: this.file.type,
			...this.options.metadata };
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
		this.url = new URL(location, absoluteUrl(this.options.endpoint)).href;
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
		const delays = this.options.retryDelays ?? DEFAULT_RETRY_DELAYS;
		if (error.retryable && typeof navigator !== 'undefined' && navigator.onLine === false) {
			this.waitOnline(run);
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
		this.setState('failed');
		this.options.onError?.(error, this);
	}

	private waitOnline(run: number): void {
		this.stopWaitingOnline();
		this.online = () => {
			this.stopWaitingOnline();
			if (run === this.run) {
				void this.go(run);
			}
		};
		window.addEventListener('online', this.online);
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
			xhr.open(verb, withQuery(url, this.options.query), true);
			xhr.withCredentials = !!this.options.withCredentials;
			Object.entries(all).forEach(([name, value]) => xhr.setRequestHeader(name, value));
			const stallMillis = this.options.stallMillis ?? DEFAULT_STALL_MILLIS;
			let stalled = false;
			let stall: number | undefined = undefined;
			const watch = () => {
				window.clearTimeout(stall);
				if (body && stallMillis > 0) {
					stall = window.setTimeout(() => {
						stalled = true;
						xhr.abort();
					}, stallMillis);
				}
			};
			const done = () => {
				window.clearTimeout(stall);
				if (this.xhr === xhr) {
					this.xhr = null;
				}
			};
			xhr.upload.addEventListener('progress', (event) => {
				if (body && event.loaded >= body.size) {
					// all handed to the network: the server's answer, or an error, tells the rest
					window.clearTimeout(stall);
				} else {
					watch();
				}
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

/** Where a file of a ResumableUploader is. */
export type ResumableItemState = 'interrupted' | 'ready' | 'queued' | 'starting' | 'uploading' |
	'paused' | 'retrying' | 'offline' | 'done' | 'failed' | 'canceled';

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
	clearFinished: 'Clear finished'
};

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
	/** Whether to ask before the page is left while files are being sent, true by default. */
	keepingOnPage?: boolean;
	/** The design of the picker, a zone to drop files on by default. */
	design?: FilePickerDesign;
	/** How long to wait before each try after a failure; DEFAULT_RETRY_DELAYS when left out. */
	retryDelays?: readonly number[];
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
		this.list = create('ul', look, 'resumable-upload-list');
		this.list.setAttribute('aria-label', this.texts.list);
		this.announcer = create('div', look, 'resumable-upload-announcer');
		this.announcer.setAttribute('role', 'status');
		this.host.append(this.element);
		this.configureInput();
		this.picker = new FilePicker(this.input, this.pickerOptions());
		this.element.append(this.toolbar, this.list, this.announcer);

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
			const fingerprint = fingerprintOf(file, this.options.endpoint);
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
		const upload = new TusUpload(file, { endpoint: options.endpoint,
			chunkSize: options.chunkSize, metadata: options.metadata?.(file),
			headers: options.headers, query: options.query, withCredentials: options.withCredentials,
			retryDelays: options.retryDelays, stallMillis: options.stallMillis,
			pollMillis: options.pollMillis,
			remember: options.remember, overridePatchMethod: options.overridePatchMethod,
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
		if (state === 'paused' || state === 'retrying' || state === 'offline') {
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
		if (state === 'paused' || state === 'done' || state === 'failed' || state === 'canceled') {
			this.pump();
		}
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
	 * Starts a file waiting for its start button, or resumes one paused or failed: it waits for its
	 * turn if as many as are sent at once are being sent.
	 */
	resume(id: string): void {
		const row = this.find(id);
		if (!row?.upload || !['ready', 'paused', 'failed'].includes(row.state)) {
			return;
		}
		row.state = 'queued';
		this.render(row);
		this.pump();
		this.changed();
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
		if (row.state === 'queued') {
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
			await terminate(url, this.options).catch(() => false);
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

	/** Pauses every file being sent or waiting for its turn. */
	pauseAll(): void {
		this.rows.filter((row) => row.state === 'queued' || ACTIVE.includes(row.state))
			.forEach((row) => this.pause(row.id));
	}

	/** Starts or resumes every file waiting for its start button, paused or failed. */
	resumeAll(): void {
		this.rows.filter((row) => row.upload && ['ready', 'paused', 'failed'].includes(row.state))
			.forEach((row) => {
				row.state = 'queued';
				this.render(row);
			});
		this.pump();
		this.changed();
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
	 * Shows the uploads to the endpoint left unfinished by an earlier visit, once the server told
	 * how far each got; those it no longer has are forgotten.
	 */
	private restore(): void {
		if (this.options.remember === false || !this.options.endpoint) {
			return;
		}
		TusStorage.list(this.options.endpoint).forEach((stored) => {
			if (this.rows.some((row) => row.fingerprint === stored.fingerprint)) {
				return;
			}
			const row = this.newRow(stored.name, stored.size, stored.fingerprint);
			row.state = 'interrupted';
			row.url = stored.url;
			this.render(row);
			void probe(stored.url, this.options).then((found) => {
				if (row.state !== 'interrupted' || this.destroyed) {
					return;
				}
				if (!found || found.offset >= stored.size ||
					(found.length !== null && found.length !== stored.size)) {
					TusStorage.remove(stored.fingerprint);
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
		toggleParts(row.element, look, state === 'paused' || state === 'interrupted',
			'resumable-upload-item-paused');
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
		row.play.hidden = !(row.upload && ['ready', 'paused', 'failed'].includes(state));
		label(row.play, format(state === 'ready' ? this.texts.start : this.texts.resume, name));
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
			['ready', 'paused', 'failed'].includes(row.state));
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
