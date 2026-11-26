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

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { BOOTSTRAP_LOOK, DEFAULT_RETRY_DELAYS, encodeMetadata, fingerprintOf, probe,
	ResumableUploader, resumableUploader, retryDelaysFor, terminate, TusError, TusStorage, TusUpload,
	UploadGuard, withQuery,
	type ResumableItem, type ResumableQuestion, type TusState } from '../src/index.js';

/** A request the fake server got. */
interface Received {
	method: string;
	url: string;
	headers: Record<string, string>;
	body: Blob | null;
	xhr: FakeXhr;
}

/** An upload the fake server keeps. */
interface Kept {
	length: number;
	offset: number;
	metadata: string;
}

/**
 * A tus server in memory: creates uploads at /files, answers HEAD, PATCH and DELETE below it.
 * A test holds PATCHes to answer them itself, or plans answers of its own.
 */
class TusServer {
	uploads = new Map<string, Kept>();

	received: Received[] = [];

	/** Whether PATCHes wait for the test, in held, instead of being answered. */
	holding = false;

	held: Received[] = [];

	/** The name it stores a whole file under, as Wicket's resource tells it; none when null. */
	storing: string | null = null;

	/** Answers planned for the next requests of a method, before the server's own. */
	planned: Array<{ method: string; answer: (request: Received) => void }> = [];

	private next = 0;

	plan(method: string, answer: (request: Received) => void): void {
		this.planned.push({ method, answer });
	}

	handle(request: Received): void {
		this.received.push(request);
		const plan = this.planned.findIndex((planned) => planned.method === request.method);
		if (plan >= 0) {
			const [planned] = this.planned.splice(plan, 1);
			planned.answer(request);
			return;
		}
		const xhr = request.xhr;
		const id = idOf(request.url);
		switch (request.method) {
			case 'POST': {
				const name = 'u' + (++this.next);
				const length = Number(request.headers['upload-length']);
				this.uploads.set(name, { length, offset: 0,
					metadata: request.headers['upload-metadata'] ?? '' });
				xhr.respond(201, { location: '/files/' + name,
					...(length === 0 ? this.stored() : {}) });
				return;
			}
			case 'HEAD': {
				const kept = this.uploads.get(id);
				if (!kept) {
					xhr.respond(404);
					return;
				}
				xhr.respond(200, { 'upload-offset': String(kept.offset),
					'upload-length': String(kept.length),
					...(kept.offset === kept.length ? this.stored() : {}) });
				return;
			}
			case 'PATCH': {
				if (this.holding) {
					this.held.push(request);
					return;
				}
				this.accept(request, request.body?.size ?? 0);
				return;
			}
			case 'DELETE':
				xhr.respond(this.uploads.delete(id) ? 204 : 404);
				return;
			default:
				xhr.respond(405);
		}
	}

	/** Takes bytes of a PATCH and answers it. */
	accept(request: Received, bytes: number): void {
		const kept = this.uploads.get(idOf(request.url));
		if (!kept) {
			request.xhr.respond(404);
			return;
		}
		if (Number(request.headers['upload-offset']) !== kept.offset) {
			request.xhr.respond(409);
			return;
		}
		kept.offset += bytes;
		request.xhr.progress(bytes);
		request.xhr.respond(204, { 'upload-offset': String(kept.offset),
			...(kept.offset === kept.length ? this.stored() : {}) });
	}

	private stored(): Record<string, string> {
		return this.storing ? { 'wicket-upload-file-name': this.storing } : {};
	}

	of(method: string): Received[] {
		return this.received.filter((request) => request.method === method);
	}
}

let server: TusServer;

/** The name of an upload in its address, without the query. */
function idOf(url: string): string {
	return url.split('?')[0].split('/files/')[1];
}

/** An XMLHttpRequest talking to the fake server, answering in a microtask. */
class FakeXhr {
	upload = new EventTarget();

	method = '';

	url = '';

	headers: Record<string, string> = {};

	status = 0;

	responseText = '';

	withCredentials = false;

	onload: (() => void) | null = null;

	onerror: (() => void) | null = null;

	onabort: (() => void) | null = null;

	ontimeout: (() => void) | null = null;

	done = false;

	private answerHeaders: Record<string, string> = {};

	open(method: string, url: string): void {
		this.method = method;
		this.url = url;
	}

	setRequestHeader(name: string, value: string): void {
		this.headers[name.toLowerCase()] = value;
	}

	getResponseHeader(name: string): string | null {
		return this.answerHeaders[name.toLowerCase()] ?? null;
	}

	send(body: Blob | null): void {
		server.handle({ method: this.method, url: this.url, headers: this.headers, body,
			xhr: this });
	}

	abort(): void {
		if (!this.done) {
			this.done = true;
			this.onabort?.();
		}
	}

	progress(loaded: number): void {
		const event = new Event('progress');
		Object.defineProperty(event, 'loaded', { value: loaded });
		this.upload.dispatchEvent(event);
	}

	respond(status: number, headers: Record<string, string> = {}, text = ''): void {
		if (this.done) {
			return;
		}
		this.done = true;
		void Promise.resolve().then(() => {
			this.status = status;
			this.answerHeaders = Object.fromEntries(Object.entries(headers)
				.map(([name, value]) => [name.toLowerCase(), value]));
			this.responseText = text;
			this.onload?.();
		});
	}

	fail(): void {
		if (!this.done) {
			this.done = true;
			void Promise.resolve().then(() => this.onerror?.());
		}
	}
}

/** Lets the microtasks and the promises waiting on them run. */
async function settle(): Promise<void> {
	for (let index = 0; index < 30; index++) {
		await Promise.resolve();
	}
}

function file(size: number, name = 'film.mp4', lastModified = 1): File {
	return new File([new Uint8Array(size)], name, { type: 'video/mp4', lastModified });
}

const ENDPOINT = 'http://localhost/files';

beforeEach(() => {
	server = new TusServer();
	vi.stubGlobal('XMLHttpRequest', FakeXhr);
	localStorage.clear();
});

afterEach(() => {
	vi.useRealTimers();
	vi.unstubAllGlobals();
	vi.restoreAllMocks();
	UploadGuard.reset();
	localStorage.clear();
	document.body.innerHTML = '';
});

describe('the tus client', () => {
	it('creates the upload with the length and the metadata, then sends the file in chunks', async () => {
		const states: TusState[] = [];
		const progress: number[] = [];
		const success = vi.fn();
		const upload = new TusUpload(file(10), { endpoint: ENDPOINT, chunkSize: 4,
			headers: { 'X-Custom': 'yes' }, metadata: { album: 'Été' },
			onState: (state) => states.push(state), onProgress: (sent) => progress.push(sent),
			onSuccess: success });
		upload.start();
		await settle();

		expect(states).toEqual(['starting', 'uploading', 'done']);
		expect(success).toHaveBeenCalledWith(upload);
		const [create] = server.of('POST');
		expect(create.url).toBe(ENDPOINT);
		expect(create.headers['tus-resumable']).toBe('1.0.0');
		expect(create.headers['upload-length']).toBe('10');
		expect(create.headers['x-custom']).toBe('yes');
		expect(create.headers['upload-metadata']).toBe(encodeMetadata({ filename: 'film.mp4',
			filetype: 'video/mp4', lastmodified: '1', album: 'Été' }));
		const patches = server.of('PATCH');
		expect(patches.map((patch) => patch.headers['upload-offset'])).toEqual(['0', '4', '8']);
		expect(patches.map((patch) => patch.body?.size)).toEqual([4, 4, 2]);
		expect(patches[0].headers['content-type']).toBe('application/offset+octet-stream');
		expect(patches[0].url).toBe('http://localhost/files/u1');
		expect(progress.at(-1)).toBe(10);
		expect(upload.url).toBe('http://localhost/files/u1');
		expect(server.uploads.get('u1')!.offset).toBe(10);
		// done: nothing is kept to go on with
		expect(TusStorage.find(upload.fingerprint)).toBeNull();
	});

	it('encodes metadata in base64 as UTF-8, leaving out keys that cannot be sent', () => {
		expect(encodeMetadata({ filename: 'a.txt', empty: '', 'bad key': 'x', 'bad,key': 'y' }))
			.toBe('filename YS50eHQ=,empty');
		expect(encodeMetadata({ name: 'Ñandú' })).toBe('name w5FhbmTDug==');
	});

	it('pauses, keeping what the server got, and goes on from where the server says', async () => {
		server.holding = true;
		const upload = new TusUpload(file(10), { endpoint: ENDPOINT, chunkSize: 10 });
		upload.start();
		await settle();
		expect(upload.state).toBe('uploading');
		const [first] = server.held;
		// the server took 6 bytes as the user paused
		server.uploads.get('u1')!.offset = 6;
		const progress: number[] = [];
		upload.options.onProgress = (sent) => progress.push(sent);
		upload.pause();
		expect(upload.state).toBe('paused');
		expect(first.xhr.done).toBe(true);
		await settle();
		// asked how far the server got
		expect(upload.offset).toBe(6);
		expect(progress).toEqual([6]);
		expect(TusStorage.find(upload.fingerprint)?.url).toBe('http://localhost/files/u1');

		server.holding = false;
		upload.start();
		await settle();
		expect(server.of('HEAD')).toHaveLength(2);
		const patches = server.of('PATCH');
		expect(patches[1].headers['upload-offset']).toBe('6');
		expect(patches[1].body?.size).toBe(4);
		expect(upload.state).toBe('done');
	});

	it('goes on after a reload, by the address kept for the file', async () => {
		server.holding = true;
		const first = new TusUpload(file(10), { endpoint: ENDPOINT });
		first.start();
		await settle();
		server.uploads.get('u1')!.offset = 3;
		// the page went
		first.abort();
		expect(first.state).toBe('paused');

		server.holding = false;
		const again = new TusUpload(file(10), { endpoint: ENDPOINT });
		again.start();
		await settle();
		expect(server.of('POST')).toHaveLength(1);
		expect(server.of('PATCH').at(-1)!.headers['upload-offset']).toBe('3');
		expect(again.state).toBe('done');
	});

	it('keeps nothing when told not to remember', async () => {
		server.holding = true;
		const upload = new TusUpload(file(10), { endpoint: ENDPOINT, remember: false });
		upload.start();
		await settle();
		expect(TusStorage.list(ENDPOINT)).toEqual([]);
	});

	it('creates the upload again when the server no longer has it', async () => {
		TusStorage.save({ fingerprint: fingerprintOf(file(10), ENDPOINT), url: ENDPOINT + '/gone',
			endpoint: ENDPOINT, name: 'film.mp4', size: 10, type: 'video/mp4', lastModified: 1,
			stored: 1 });
		const upload = new TusUpload(file(10), { endpoint: ENDPOINT });
		upload.start();
		await settle();
		expect(server.of('HEAD')[0].url).toBe(ENDPOINT + '/gone');
		expect(server.of('POST')).toHaveLength(1);
		expect(upload.state).toBe('done');
	});

	it('fails an upload whose grant the server refuses, keeping it for a page loaded again',
		async () => {
			const fingerprint = fingerprintOf(file(10), ENDPOINT);
			TusStorage.save({ fingerprint, url: ENDPOINT + '/old', endpoint: ENDPOINT,
				name: 'film.mp4', size: 10, type: 'video/mp4', lastModified: 1, stored: 1 });
			server.plan('HEAD', (request) => request.xhr.respond(403,
				{ 'Wicket-Upload-Error': 'resumableUploadGrantRefused' }));
			const upload = new TusUpload(file(10), { endpoint: ENDPOINT });
			upload.start();
			await settle();
			expect(upload.state).toBe('failed');
			expect(upload.error).toMatchObject({ status: 403, kind: 'refused',
				key: 'resumableUploadGrantRefused' });
			expect(server.of('POST')).toHaveLength(0);
			expect(TusStorage.find(fingerprint)!.url).toBe(ENDPOINT + '/old');
		});

	it('creates the upload again when the server keeps one of another length', async () => {
		server.uploads.set('other', { length: 99, offset: 5, metadata: '' });
		TusStorage.save({ fingerprint: fingerprintOf(file(10), ENDPOINT),
			url: ENDPOINT + '/other', endpoint: ENDPOINT, name: 'film.mp4', size: 10,
			type: 'video/mp4', lastModified: 1, stored: 1 });
		const upload = new TusUpload(file(10), { endpoint: ENDPOINT });
		upload.start();
		await settle();
		expect(server.of('POST')).toHaveLength(1);
		expect(upload.url).toBe('http://localhost/files/u1');
	});

	it('tries again after a broken connection, asking the server where it stopped', async () => {
		vi.useFakeTimers();
		server.plan('PATCH', (request) => {
			server.uploads.get('u1')!.offset = 2;
			request.xhr.fail();
		});
		const states: TusState[] = [];
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT, retryDelays: [500],
			onState: (state) => states.push(state) });
		upload.start();
		await settle();
		expect(upload.state).toBe('retrying');
		expect(upload.retryDelay).toBe(500);
		expect(upload.error?.kind).toBe('network');
		await vi.advanceTimersByTimeAsync(500);
		await settle();
		expect(upload.state).toBe('done');
		expect(server.of('HEAD')).toHaveLength(1);
		expect(server.of('PATCH')[1].headers['upload-offset']).toBe('2');
		expect(states).toEqual(['starting', 'uploading', 'retrying', 'starting', 'uploading',
			'done']);
	});

	it('fails for good once the tries are spent, telling what the server said', async () => {
		vi.useFakeTimers();
		const fail = (request: Received) => request.xhr.respond(503,
			{ 'content-type': 'text/plain; charset=UTF-8' }, 'Busy, come back later');
		server.plan('PATCH', fail);
		server.plan('PATCH', fail);
		const error = vi.fn();
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT, retryDelays: [100],
			onError: error });
		upload.start();
		await settle();
		await vi.advanceTimersByTimeAsync(100);
		await settle();
		expect(upload.state).toBe('failed');
		expect(error).toHaveBeenCalledTimes(1);
		const failure = error.mock.calls[0][0] as TusError;
		expect(failure.status).toBe(503);
		expect(failure.kind).toBe('server');
		expect(failure.message).toBe('Busy, come back later');

		// started again, it asks where it stopped and goes on
		upload.start();
		await settle();
		expect(upload.state).toBe('done');
	});

	it('does not try again what the server refused', async () => {
		server.plan('POST', (request) => request.xhr.respond(413,
			{ 'content-type': 'text/plain' }, 'Larger than 100 MB, the most for a file'));
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT });
		upload.start();
		await settle();
		expect(upload.state).toBe('failed');
		expect(upload.error).toMatchObject({ status: 413, kind: 'refused',
			message: 'Larger than 100 MB, the most for a file' });
		expect(upload.error!.retryable).toBe(false);
	});

	it('tells an endpoint that is not there as refused, not as an upload gone', async () => {
		server.plan('POST', (request) => request.xhr.respond(404, {}, '<html>'));
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT, retryDelays: [] });
		upload.start();
		await settle();
		expect(upload.error).toMatchObject({ status: 404, kind: 'refused',
			message: 'The server answered 404' });
	});

	it('takes an answer outside the protocol for a failure', async () => {
		server.plan('POST', (request) => request.xhr.respond(201));
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT, retryDelays: [] });
		upload.start();
		await settle();
		expect(upload.error).toMatchObject({ kind: 'protocol' });

		server.plan('PATCH', (request) => request.xhr.respond(204, { 'upload-offset': 'x' }));
		const other = new TusUpload(file(9, 'other'), { endpoint: ENDPOINT, retryDelays: [] });
		other.start();
		await settle();
		expect(other.state).toBe('failed');
		expect(other.error!.message).toContain('Upload-Offset');
	});

	it('takes the connection for lost once the tries get no answer, and goes on when started again',
		async () => {
			server.plan('PATCH', (request) => request.xhr.fail());
			const errors = vi.fn();
			const upload = new TusUpload(file(8), { endpoint: ENDPOINT, retryDelays: [],
				onError: errors });
			upload.start();
			await settle();
			expect(upload.state).toBe('lost');
			expect(errors).not.toHaveBeenCalled();
			upload.start();
			await settle();
			expect(upload.state).toBe('done');
		});

	it('tries again for as long as told, waiting longer each time', async () => {
		expect(DEFAULT_RETRY_DELAYS).toEqual([0, 1000, 3000, 5000, 10000, 20000, 1000]);
		expect(retryDelaysFor(5000)).toEqual([0, 1000, 3000, 1000]);
		expect(retryDelaysFor(100000)).toEqual([0, 1000, 3000, 5000, 10000, 20000, 30000, 30000,
			1000]);
		expect(retryDelaysFor(0)).toEqual([]);

		vi.useFakeTimers();
		server.plan('PATCH', (request) => request.xhr.fail());
		server.plan('HEAD', (request) => request.xhr.fail());
		server.plan('HEAD', (request) => request.xhr.fail());
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT, retryTimeout: 1000 });
		upload.start();
		await settle();
		expect([upload.state, upload.retryDelay]).toEqual(['retrying', 0]);
		await vi.advanceTimersByTimeAsync(0);
		await settle();
		expect([upload.state, upload.retryDelay]).toEqual(['retrying', 1000]);
		await vi.advanceTimersByTimeAsync(1000);
		await settle();
		expect(upload.state).toBe('lost');
	});

	it('takes the connection for lost once offline for longer than the tries take', async () => {
		vi.useFakeTimers();
		vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);
		server.plan('PATCH', (request) => request.xhr.fail());
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT, retryDelays: [500, 500] });
		upload.start();
		await settle();
		expect(upload.state).toBe('offline');
		await vi.advanceTimersByTimeAsync(999);
		expect(upload.state).toBe('offline');
		await vi.advanceTimersByTimeAsync(1);
		expect(upload.state).toBe('lost');
	});

	it('waits while offline and goes on once online again', async () => {
		const online = vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);
		server.plan('PATCH', (request) => request.xhr.fail());
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT });
		upload.start();
		await settle();
		expect(upload.state).toBe('offline');
		online.mockReturnValue(true);
		window.dispatchEvent(new Event('online'));
		await settle();
		expect(upload.state).toBe('done');
	});

	it('takes a chunk without progress for lost', async () => {
		vi.useFakeTimers();
		server.holding = true;
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT, stallMillis: 1000,
			retryDelays: [0] });
		upload.start();
		await settle();
		expect(server.held).toHaveLength(1);
		server.held[0].xhr.progress(1);
		await vi.advanceTimersByTimeAsync(999);
		expect(upload.state).toBe('uploading');
		await vi.advanceTimersByTimeAsync(1);
		expect(upload.state).toBe('retrying');
		expect(upload.error!.message).toBe('The upload stalled');
	});

	it('tells what the server holds while a chunk is sent, when told to ask it', async () => {
		vi.useFakeTimers();
		server.holding = true;
		const progress: number[] = [];
		const upload = new TusUpload(file(10), { endpoint: ENDPOINT, pollMillis: 500,
			onProgress: (sent) => progress.push(sent) });
		upload.start();
		await settle();
		const [chunk] = server.held;
		// the browser handed it all to the network: that is not what the server holds
		chunk.xhr.progress(10);
		expect(progress).toEqual([0]);
		server.uploads.get('u1')!.offset = 4;
		await vi.advanceTimersByTimeAsync(500);
		await settle();
		expect(progress).toEqual([0, 4]);
		await vi.advanceTimersByTimeAsync(500);
		await settle();
		// the same again tells nothing
		expect(progress).toEqual([0, 4]);
		expect(server.of('HEAD')).toHaveLength(2);
		server.uploads.get('u1')!.offset = 0;
		server.holding = false;
		server.accept(chunk, 10);
		await settle();
		expect(upload.state).toBe('done');
		expect(progress.at(-1)).toBe(10);
		// no more asking once the chunk went through
		await vi.advanceTimersByTimeAsync(2000);
		expect(server.of('HEAD')).toHaveLength(2);
	});

	it('takes a chunk handed whole to the network for sent, as long as the server answers', async () => {
		vi.useFakeTimers();
		server.holding = true;
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT, stallMillis: 1000,
			retryDelays: [0] });
		upload.start();
		await settle();
		server.held[0].xhr.progress(8);
		await vi.advanceTimersByTimeAsync(5000);
		expect(upload.state).toBe('uploading');
		expect(server.of('HEAD').length).toBeGreaterThanOrEqual(4);

		// the server no longer answers: the chunk is lost
		server.plan('HEAD', (request) => request.xhr.fail());
		await vi.advanceTimersByTimeAsync(1000);
		await settle();
		expect(upload.state).toBe('retrying');
		expect(upload.error!.message).toBe('The upload stalled');
	});

	it('takes a request without an answer for lost', async () => {
		vi.useFakeTimers();
		server.plan('POST', () => undefined);
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT, stallMillis: 1000,
			retryDelays: [] });
		upload.start();
		await vi.advanceTimersByTimeAsync(1000);
		await settle();
		expect(upload.state).toBe('lost');
	});

	it('sends PATCH as POST when proxies refuse it', async () => {
		server.plan('POST', (request) => {
			if (request.headers['x-http-method-override'] === 'PATCH') {
				server.accept(request, request.body!.size);
			} else {
				server.plan('POST', (patch) => server.accept(patch, patch.body!.size));
				server.uploads.set('u9', { length: 8, offset: 0, metadata: '' });
				request.xhr.respond(201, { location: '/files/u9' });
			}
		});
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT, overridePatchMethod: true });
		upload.start();
		await settle();
		expect(upload.state).toBe('done');
		expect(server.of('POST')[1].headers['x-http-method-override']).toBe('PATCH');
	});

	it('cancels: stops and deletes the upload on the server, which cannot go on then', async () => {
		server.holding = true;
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT });
		upload.start();
		await settle();
		await upload.cancel();
		expect(upload.state).toBe('canceled');
		expect(server.of('DELETE')[0].url).toBe('http://localhost/files/u1');
		expect(server.uploads.size).toBe(0);
		expect(TusStorage.list(ENDPOINT)).toEqual([]);
		upload.start();
		expect(upload.state).toBe('canceled');
		await upload.cancel();
		expect(server.of('DELETE')).toHaveLength(1);
	});

	it('sends an empty file by creating it', async () => {
		const upload = new TusUpload(file(0), { endpoint: ENDPOINT });
		upload.start();
		await settle();
		expect(upload.state).toBe('done');
		expect(server.of('PATCH')).toHaveLength(0);
	});

	it('does nothing when started while it runs, nor when paused while it does not', async () => {
		server.holding = true;
		const upload = new TusUpload(file(8), { endpoint: ENDPOINT });
		upload.pause();
		expect(upload.state).toBe('idle');
		upload.start();
		upload.start();
		await settle();
		expect(server.of('POST')).toHaveLength(1);
	});

	it('probes and terminates uploads by their addresses', async () => {
		server.uploads.set('a', { length: 10, offset: 4, metadata: '' });
		expect(await probe(ENDPOINT + '/a')).toEqual({ offset: 4, length: 10 });
		expect(await probe(ENDPOINT + '/b')).toBeNull();
		expect(await terminate(ENDPOINT + '/a')).toBe(true);
		expect(await terminate(ENDPOINT + '/a')).toBe(true);
		server.plan('DELETE', (request) => request.xhr.respond(423));
		expect(await terminate(ENDPOINT + '/c')).toBe(false);
		server.plan('HEAD', (request) => request.xhr.fail());
		expect(await probe(ENDPOINT + '/c')).toBeNull();
	});

	it('keeps the uploads of an endpoint, the oldest first, and survives a storage that throws', () => {
		const stored = (fingerprint: string, endpoint: string, at: number) => ({ fingerprint,
			url: endpoint + '/' + fingerprint, endpoint, name: fingerprint, size: 1, type: '',
			lastModified: 0, stored: at });
		TusStorage.save(stored('b', ENDPOINT, 2));
		TusStorage.save(stored('a', ENDPOINT, 1));
		TusStorage.save(stored('c', 'http://other/files', 0));
		localStorage.setItem('antilia-tus::broken', '{');
		expect(TusStorage.list(ENDPOINT).map((upload) => upload.fingerprint)).toEqual(['a', 'b']);
		TusStorage.remove('a');
		expect(TusStorage.find('a')).toBeNull();
		expect(TusStorage.find('broken')).toBeNull();

		vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
			throw new Error('full');
		});
		expect(() => TusStorage.save(stored('d', ENDPOINT, 3))).not.toThrow();
	});
});

describe('the resumable uploader', () => {
	function host(): HTMLDivElement {
		document.body.innerHTML = '<div id="uploads"></div>';
		return document.getElementById('uploads') as HTMLDivElement;
	}

	function rowOf(uploader: ResumableUploader, index: number): HTMLLIElement {
		return uploader.list.children[index] as HTMLLIElement;
	}

	function buttonsOf(row: HTMLElement): HTMLButtonElement[] {
		return Array.from(row.querySelectorAll<HTMLButtonElement>('button:not([hidden])'));
	}

	it('draws the picker and sends each file chosen, one at a time', async () => {
		const completed: ResumableItem[] = [];
		const changes = vi.fn();
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, chunkSize: 4,
			locale: 'en', onComplete: (item) => completed.push(item), onChange: changes });
		expect(uploader.element.classList).toContain('antilia-resumable-upload');
		expect(uploader.input.type).toBe('file');
		expect(uploader.input.multiple).toBe(true);
		expect(uploader.picker.zone).not.toBeNull();
		expect(uploader.toolbar.hidden).toBe(true);
		expect(ResumableUploader.of('uploads')).toBe(uploader);

		server.holding = true;
		uploader.add([file(8, 'a.mp4'), file(6, 'b.mp4')]);
		await settle();
		expect(uploader.items.map((item) => item.state)).toEqual(['uploading', 'queued']);
		expect(UploadGuard.keepsOnPage()).toBe(true);
		const first = rowOf(uploader, 0);
		expect(first.querySelector('.antilia-resumable-upload-name')!.textContent).toBe('a.mp4');
		expect(first.querySelector('.antilia-resumable-upload-size')!.textContent).toBe('8 byte');
		expect(buttonsOf(first).map((button) => button.getAttribute('aria-label')))
			.toEqual(['Pause a.mp4', 'Cancel a.mp4']);
		expect(rowOf(uploader, 1).querySelector('.antilia-upload-status')!.textContent)
			.toBe('Waiting for its turn');

		server.held[0].xhr.progress(2);
		expect(first.querySelector('progress')!.getAttribute('value')).toBe('25');
		expect(first.querySelector('.antilia-upload-status')!.textContent).toBe('2 byte of 8 byte');

		server.holding = false;
		server.accept(server.held.shift()!, 4);
		await settle();
		expect(uploader.items.map((item) => item.state)).toEqual(['done', 'done']);
		expect(completed.map((item) => item.name)).toEqual(['a.mp4', 'b.mp4']);
		expect(completed[0].url).toBe('http://localhost/files/u1');
		expect(first.classList).toContain('antilia-resumable-upload-item-done');
		expect(first.querySelector('.antilia-upload-status')!.textContent).toBe('Uploaded 8 byte');
		expect(buttonsOf(first).map((button) => button.getAttribute('aria-label')))
			.toEqual(['Remove a.mp4 from the list']);
		expect(UploadGuard.keepsOnPage()).toBe(false);
		expect(changes).toHaveBeenCalled();
		expect(uploader.clearButton.hidden).toBe(false);

		uploader.clearFinished();
		expect(uploader.items).toEqual([]);
		expect(uploader.toolbar.hidden).toBe(true);
	});

	it('sends several at once when told, and lets each be paused and resumed', async () => {
		server.holding = true;
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, parallel: 2 });
		uploader.add([file(8, 'a'), file(8, 'b'), file(8, 'c')]);
		await settle();
		expect(uploader.items.map((item) => item.state)).toEqual(['uploading', 'uploading',
			'queued']);

		const [a] = uploader.items;
		uploader.pause(a.id);
		await settle();
		// a slot freed: the third starts
		expect(uploader.items.map((item) => item.state)).toEqual(['paused', 'uploading',
			'uploading']);
		expect(rowOf(uploader, 0).classList).toContain('antilia-resumable-upload-item-paused');
		expect(rowOf(uploader, 0).querySelector('.antilia-upload-status')!.textContent)
			.toBe('Paused at 0 byte of 8 byte');
		expect(buttonsOf(rowOf(uploader, 0))[0].getAttribute('aria-label')).toBe('Resume a');

		uploader.resume(a.id);
		expect(uploader.items[0].state).toBe('queued');
		uploader.pauseAll();
		await settle();
		expect(uploader.items.map((item) => item.state)).toEqual(['paused', 'paused', 'paused']);
		expect(uploader.pauseAllButton.hidden).toBe(true);
		expect(uploader.resumeAllButton.hidden).toBe(false);
		expect(UploadGuard.keepsOnPage()).toBe(false);

		server.holding = false;
		uploader.resumeAll();
		await settle();
		expect(uploader.items.map((item) => item.state)).toEqual(['done', 'done', 'done']);
	});

	it('waits for the start buttons when not starting by itself', async () => {
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, autoStart: false });
		uploader.add([file(8, 'a')]);
		expect(uploader.items[0].state).toBe('ready');
		const row = rowOf(uploader, 0);
		expect(row.querySelector('.antilia-upload-status')!.textContent).toBe('Ready to send');
		const [start] = buttonsOf(row);
		expect(start.getAttribute('aria-label')).toBe('Start a');
		start.click();
		await settle();
		expect(uploader.items[0].state).toBe('done');
	});

	it('refuses a file larger than the most at once, and takes one file unless multiple', () => {
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, fileMaxSize: 5,
			multiple: false, locale: 'en' });
		expect(uploader.input.multiple).toBe(false);
		uploader.add([file(8, 'big'), file(2, 'small')]);
		expect(uploader.items).toHaveLength(1);
		expect(uploader.items[0]).toMatchObject({ name: 'big', state: 'failed',
			error: 'Larger than 5 byte, the most for a file' });
		expect(rowOf(uploader, 0).classList).toContain('antilia-resumable-upload-item-failed');
		expect(server.received).toEqual([]);
	});

	it('takes the files chosen in the picker, and leaves the picker empty', async () => {
		server.holding = true;
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, accept: 'video/*' });
		expect(uploader.input.accept).toBe('video/*');
		let files: File[] = [file(8)];
		Object.defineProperty(uploader.input, 'files', { get: () => files, configurable: true });
		Object.defineProperty(uploader.input, 'value', { get: () => files.length ? 'x' : '',
			set: () => {
				files = [];
			}, configurable: true });
		uploader.input.dispatchEvent(new Event('change'));
		await settle();
		expect(uploader.items).toHaveLength(1);
		expect(files).toEqual([]);
		// the same file again is the one in the list
		uploader.add([file(8)]);
		expect(uploader.items).toHaveLength(1);
	});

	it('tells the files the server failed, why, and lets them be tried again', async () => {
		server.plan('POST', (request) => request.xhr.respond(413, { 'content-type': 'text/plain' },
			'Too large for the server'));
		server.plan('PATCH', (request) => request.xhr.respond(400));
		const errors = vi.fn();
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, onError: errors,
			retryDelays: [] });
		uploader.add([file(8, 'a'), file(8, 'b')]);
		await settle();
		expect(uploader.items.map((item) => item.error)).toEqual(['Too large for the server',
			'The server refused the upload (400)']);
		expect(errors).toHaveBeenCalledTimes(2);
		uploader.resume(uploader.items[1].id);
		await settle();
		expect(uploader.items[1].state).toBe('done');
	});

	it('tells a broken connection, and how long until it tries again', async () => {
		vi.useFakeTimers();
		server.plan('PATCH', (request) => request.xhr.fail());
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, locale: 'en',
			retryDelays: [3000] });
		uploader.add([file(8, 'a')]);
		await settle();
		expect(rowOf(uploader, 0).querySelector('.antilia-upload-status')!.textContent)
			.toBe('The connection broke off, trying again in 3 sec');
		await vi.advanceTimersByTimeAsync(3000);
		await settle();
		expect(uploader.items[0].state).toBe('done');

		server.plan('PATCH', (request) => request.xhr.fail());
		const other = new ResumableUploader(host(), { endpoint: ENDPOINT, locale: 'en',
			retryDelays: [] });
		other.add([file(8, 'b')]);
		await settle();
		expect(other.items[0].state).toBe('lost');
		expect(other.items[0].error).toBeNull();
		expect(rowOf(other, 0).querySelector('.antilia-upload-status')!.textContent)
			.toBe('Connection lost at 0 byte of 8 byte: waiting for it');
	});

	it('asks whether to resume once the server answers again after the connection was lost',
		async () => {
			vi.useFakeTimers();
			server.plan('PATCH', (request) => {
				server.uploads.get('u1')!.offset = 3;
				request.xhr.fail();
			});
			const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, locale: 'en',
				retryDelays: [], probeDelays: [1000] });
			uploader.add([file(8, 'a'), file(8, 'b')]);
			await settle();
			expect(uploader.items.map((item) => item.state)).toEqual(['lost', 'lost']);
			expect(uploader.questionElement.hidden).toBe(true);
			const requests = server.received.length;

			// the first look gets no answer, the second one does
			server.plan('HEAD', (request) => request.xhr.fail());
			await vi.advanceTimersByTimeAsync(1000);
			await settle();
			expect(server.received.length).toBe(requests + 1);
			expect(uploader.questionElement.hidden).toBe(true);
			await vi.advanceTimersByTimeAsync(1000);
			await settle();
			expect(server.of('HEAD').at(-1)!.url).toBe(ENDPOINT + '/u1');
			expect(uploader.questionElement.hidden).toBe(false);
			expect(uploader.questionElement.textContent)
				.toContain('The connection is back. Resume the uploads?');
			await vi.advanceTimersByTimeAsync(5000);
			expect(server.received.length, 'nothing sent before the answer').toBe(requests + 2);

			uploader.questionElement.querySelector<HTMLButtonElement>(
				'.antilia-resumable-upload-question-resume')!.click();
			await settle();
			expect(uploader.items.map((item) => item.state)).toEqual(['done', 'done']);
			expect(server.of('PATCH')[1].headers['upload-offset']).toBe('3');
			expect(uploader.questionElement.hidden).toBe(true);
		});

	it('cancels the uploads instead when the user says so, or goes on by itself when not asking',
		async () => {
			vi.useFakeTimers();
			server.plan('PATCH', (request) => request.xhr.fail());
			const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT,
				retryDelays: [], probeDelays: [10] });
			uploader.add([file(8, 'a')]);
			await settle();
			await vi.advanceTimersByTimeAsync(10);
			await settle();
			uploader.questionElement.querySelector<HTMLButtonElement>(
				'.antilia-resumable-upload-question-cancel')!.click();
			await settle();
			expect(uploader.items[0].state).toBe('canceled');
			expect(server.of('DELETE')).toHaveLength(1);

			server.plan('PATCH', (request) => request.xhr.fail());
			const other = new ResumableUploader(host(), { endpoint: ENDPOINT,
				retryDelays: [], probeDelays: [10], askingToResume: false });
			other.add([file(8, 'b')]);
			await settle();
			expect(other.items[0].state).toBe('lost');
			await vi.advanceTimersByTimeAsync(10);
			await settle();
			expect(other.items[0].state).toBe('done');
			expect(other.questionElement.hidden).toBe(true);
		});

	it('lets the page ask instead of the uploader', async () => {
		vi.useFakeTimers();
		server.plan('PATCH', (request) => request.xhr.fail());
		const questions: ResumableQuestion[] = [];
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, retryDelays: [],
			probeDelays: [10], onQuestion: (question) => {
				questions.push(question);
				return true;
			} });
		uploader.add([file(8, 'a')]);
		await settle();
		await vi.advanceTimersByTimeAsync(10);
		await settle();
		expect(questions).toHaveLength(1);
		expect(questions[0].reason).toBe('reconnected');
		expect(questions[0].items.map((item: ResumableItem) => [item.name, item.state]))
			.toEqual([['a', 'lost']]);
		expect(uploader.questionElement.hidden).toBe(true);
		questions[0].resume();
		await settle();
		expect(uploader.items[0].state).toBe('done');
	});

	it('cancels a file, deleting it on the server, and takes it out of the list', async () => {
		server.holding = true;
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT });
		uploader.add([file(8, 'a'), file(8, 'b')]);
		await settle();
		const [a, b] = uploader.items;
		await uploader.cancel(a.id);
		await settle();
		expect(server.of('DELETE')).toHaveLength(1);
		expect(uploader.items.map((item) => item.state)).toEqual(['canceled', 'uploading']);
		expect(rowOf(uploader, 0).querySelector('.antilia-upload-status')!.textContent)
			.toBe('Canceled');
		uploader.remove(a.id);
		expect(uploader.items.map((item) => item.id)).toEqual([b.id]);
		// one not finished goes on the server too
		uploader.remove(b.id);
		await settle();
		expect(server.of('DELETE')).toHaveLength(2);
		expect(uploader.items).toEqual([]);
		expect(document.activeElement).toBe(uploader.picker.button);
	});

	it('cancels a file waiting for its turn without asking the server', async () => {
		server.holding = true;
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT });
		uploader.add([file(8, 'a'), file(8, 'b')]);
		await settle();
		const [, b] = uploader.items;
		const [, stop] = buttonsOf(rowOf(uploader, 1));
		expect(stop.getAttribute('aria-label')).toBe('Cancel b');
		stop.click();
		await settle();
		expect(uploader.items[1].state).toBe('canceled');
		expect(server.of('DELETE')).toHaveLength(0);
		buttonsOf(rowOf(uploader, 1))[0].click();
		expect(uploader.items.map((item) => item.id)).not.toContain(b.id);
	});

	it('shows the uploads an earlier visit left, and goes on with a file chosen again', async () => {
		server.uploads.set('kept', { length: 10, offset: 4, metadata: '' });
		const fingerprint = fingerprintOf(file(10, 'film.mp4'), ENDPOINT);
		TusStorage.save({ fingerprint, url: ENDPOINT + '/kept', endpoint: ENDPOINT,
			name: 'film.mp4', size: 10, type: 'video/mp4', lastModified: 1, stored: 1 });
		TusStorage.save({ fingerprint: 'gone', url: ENDPOINT + '/gone', endpoint: ENDPOINT,
			name: 'gone.mp4', size: 10, type: '', lastModified: 1, stored: 2 });
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, locale: 'en' });
		expect(uploader.items.map((item) => item.state)).toEqual(['interrupted', 'interrupted']);
		await settle();
		// the server no longer has the second one
		expect(uploader.items).toHaveLength(1);
		expect(TusStorage.find('gone')).toBeNull();
		const row = rowOf(uploader, 0);
		expect(row.querySelector('.antilia-upload-status')!.textContent)
			.toBe('Stopped at 4 byte of 10 byte: choose the file again to go on');
		expect(buttonsOf(row).map((button) => button.getAttribute('aria-label')))
			.toEqual(['Continue film.mp4: choose it again', 'Cancel film.mp4']);
		expect(uploader.questionElement.hidden).toBe(false);
		expect(uploader.questionElement.textContent).toContain(
			'Uploads you started earlier did not end. Continue them? Choose their files again.');
		const opened = vi.spyOn(uploader.input, 'click').mockImplementation(() => undefined);
		uploader.questionElement.querySelector<HTMLButtonElement>(
			'.antilia-resumable-upload-question-resume')!.click();
		buttonsOf(row)[0].click();
		expect(opened).toHaveBeenCalledTimes(2);

		uploader.add([file(10, 'film.mp4')]);
		await settle();
		expect(uploader.items).toHaveLength(1);
		expect(uploader.items[0].state).toBe('done');
		expect(server.of('POST')).toHaveLength(0);
		expect(server.of('PATCH')[0].headers['upload-offset']).toBe('4');
	});

	it('keeps the uploads of an earlier visit the server did not tell about, without showing them',
		async () => {
			TusStorage.save({ fingerprint: 'refused', url: ENDPOINT + '/refused', endpoint: ENDPOINT,
				name: 'refused.mp4', size: 10, type: '', lastModified: 1, stored: 1 });
			TusStorage.save({ fingerprint: 'unanswered', url: ENDPOINT + '/unanswered',
				endpoint: ENDPOINT, name: 'unanswered.mp4', size: 10, type: '', lastModified: 1,
				stored: 2 });
			server.plan('HEAD', (request) => request.xhr.respond(403,
				{ 'Wicket-Upload-Error': 'resumableUploadGrantRefused' }));
			server.plan('HEAD', (request) => request.xhr.fail());
			const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, locale: 'en' });
			await settle();
			expect(uploader.items).toHaveLength(0);
			expect(uploader.questionElement.hidden).toBe(true);
			expect(TusStorage.find('refused')).not.toBeNull();
			expect(TusStorage.find('unanswered')).not.toBeNull();
		});

	it('offers the uploads the server keeps for it, and goes on with one once its file is chosen',
		async () => {
			server.uploads.set('kept', { length: 10, offset: 4, metadata: '' });
			const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, locale: 'en',
				rememberedAs: 'page:files', recalled: [{ id: 'kept', name: 'film.mp4', size: 10,
					type: 'video/mp4', lastModified: 1, sent: 4 }] });
			await settle();
			expect(uploader.items.map((item) => item.state)).toEqual(['interrupted']);
			expect(TusStorage.list(ENDPOINT)[0]).toMatchObject({ url: ENDPOINT + '/kept',
				rememberedAs: 'page:files' });
			uploader.add([file(10, 'film.mp4')]);
			await settle();
			expect(uploader.items[0].state).toBe('done');
			expect(server.of('POST')).toHaveLength(0);
			expect(server.of('PATCH')[0].headers['upload-offset']).toBe('4');
		});

	it('keeps an upload the server offers once, without the id of a session in its address', () => {
		TusStorage.save({ fingerprint: fingerprintOf(file(10), ENDPOINT), url: ENDPOINT + '/kept',
			endpoint: ENDPOINT, name: 'film.mp4', size: 10, type: 'video/mp4', lastModified: 1,
			stored: 1 });
		const recalled = [{ id: 'kept', name: 'film.mp4', size: 10, type: 'video/mp4',
			lastModified: 1, sent: 4 }];
		new ResumableUploader(host(), { endpoint: ENDPOINT, locale: 'en', recalled });
		expect(TusStorage.list(ENDPOINT)).toHaveLength(1);
		new ResumableUploader(host(), { endpoint: ENDPOINT + ';jsessionid=abc', locale: 'en',
			recalled: [{ ...recalled[0], id: 'other' }] });
		expect(TusStorage.list(ENDPOINT + ';jsessionid=abc').map((stored) => stored.url))
			.toEqual([ENDPOINT + '/other']);
	});

	it('tells the server the place of the uploader and the date of the file', async () => {
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, locale: 'en',
			rememberedAs: 'page:files' });
		uploader.add([file(8)]);
		await settle();
		expect(server.of('POST')[0].headers['upload-metadata']).toBe(encodeMetadata({
			filename: 'film.mp4', filetype: 'video/mp4', lastmodified: '1', place: 'page:files' }));
	});

	it('offers only the uploads of its place an earlier page left, not those another page sends',
		async () => {
			['mine', 'elsewhere', 'sending'].forEach((name) =>
				server.uploads.set(name, { length: 10, offset: 4, metadata: '' }));
			const keep = (name: string, rememberedAs: string, alive = 0) => TusStorage.save({
				fingerprint: name, url: ENDPOINT + '/' + name, endpoint: ENDPOINT, name,
				size: 10, type: '', lastModified: 1, stored: 1, rememberedAs, alive });
			keep('mine', 'Page:field');
			keep('elsewhere', 'Other:field');
			keep('sending', 'Page:field', Date.now());
			const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT,
				rememberedAs: 'Page:field' });
			await settle();
			expect(uploader.items.map((item) => item.name)).toEqual(['mine']);

			uploader.questionElement.querySelector<HTMLButtonElement>(
				'.antilia-resumable-upload-question-cancel')!.click();
			await settle();
			expect(uploader.items).toHaveLength(0);
			expect(server.uploads.has('mine')).toBe(false);
			expect(TusStorage.find('mine')).toBeNull();
			expect(uploader.questionElement.hidden).toBe(true);
		});

	it('lists nothing of an earlier visit when not recalling, and takes another window of use',
		async () => {
			server.uploads.set('kept', { length: 10, offset: 4, metadata: '' });
			TusStorage.save({ fingerprint: 'f', url: ENDPOINT + '/kept', endpoint: ENDPOINT,
				name: 'film.mp4', size: 10, type: '', lastModified: 1, stored: 1,
				alive: Date.now() - 30000 });
			const quiet = new ResumableUploader(host(), { endpoint: ENDPOINT, recalling: false });
			await settle();
			expect(quiet.items).toHaveLength(0);
			expect(server.of('HEAD')).toHaveLength(0);
			quiet.destroy();

			const patient = new ResumableUploader(host(), { endpoint: ENDPOINT, inUseMillis: 60000 });
			await settle();
			expect(patient.items, 'in use by another page for a minute').toHaveLength(0);
			patient.destroy();

			const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT });
			await settle();
			expect(uploader.items.map((item) => item.state), 'no longer in use after 20 seconds')
				.toEqual(['interrupted']);
		});

	it('cancels an upload an earlier visit left', async () => {
		server.uploads.set('kept', { length: 10, offset: 4, metadata: '' });
		TusStorage.save({ fingerprint: 'f', url: ENDPOINT + '/kept', endpoint: ENDPOINT,
			name: 'film.mp4', size: 10, type: '', lastModified: 1, stored: 1 });
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT });
		await settle();
		await uploader.cancel(uploader.items[0].id);
		expect(server.uploads.size).toBe(0);
		expect(TusStorage.find('f')).toBeNull();
		expect(uploader.items[0].state).toBe('canceled');
	});

	it('remembers nothing when told not to', async () => {
		TusStorage.save({ fingerprint: 'f', url: ENDPOINT + '/kept', endpoint: ENDPOINT,
			name: 'film.mp4', size: 10, type: '', lastModified: 1, stored: 1 });
		const uploader = new ResumableUploader(host(), { endpoint: ENDPOINT, remember: false });
		expect(uploader.items).toEqual([]);
		expect(server.received).toEqual([]);
	});

	it('takes other options and texts, in another look, and goes away with its element', async () => {
		server.holding = true;
		const element = host();
		const action = resumableUploader(element, { endpoint: ENDPOINT, keepingOnPage: false });
		const uploader = ResumableUploader.of(element)!;
		uploader.add([file(8, 'a')]);
		await settle();
		expect(UploadGuard.keepsOnPage()).toBe(false);
		action.update({ endpoint: ENDPOINT, design: 'button', look: BOOTSTRAP_LOOK,
			texts: { pauseAll: 'Alles anhalten', pause: '{0} anhalten', list: 'Dateien' } });
		expect(uploader.picker.zone).toBeNull();
		expect(uploader.pauseAllButton.textContent).toBe('Alles anhalten');
		expect(uploader.list.getAttribute('aria-label')).toBe('Dateien');
		expect(UploadGuard.keepsOnPage()).toBe(true);
		uploader.pauseAllButton.click();
		await settle();
		expect(uploader.items[0].state).toBe('paused');
		uploader.resumeAllButton.click();
		await settle();
		expect(uploader.items[0].state).toBe('uploading');

		action.destroy();
		await settle();
		expect(element.children).toHaveLength(0);
		expect(ResumableUploader.of(element)).toBeNull();
		expect(UploadGuard.keepsOnPage()).toBe(false);
		// the upload stopped, to go on after a reload
		expect(server.held[0].xhr.done).toBe(true);
		expect(TusStorage.list(ENDPOINT)).toHaveLength(1);
	});

	it('attaches by id, or not without the element', () => {
		host();
		expect(ResumableUploader.attach('missing', { endpoint: ENDPOINT })).toBeNull();
		expect(ResumableUploader.attach('uploads', { endpoint: ENDPOINT })).not.toBeNull();
		expect(ResumableUploader.of('missing')).toBeNull();
	});
});

describe('Wicket.Upload.resumable', () => {
	const SETTINGS = { inputName: 'field', resourceUrl: ENDPOINT, ajaxCallBackUrl: 'callback',
		maxSize: 100, fileMaxSize: 50, fileCountMax: -1, uploadToken: 'to ken',
		grantExpires: 1767225600000, parallelUploads: 2, remembering: true };

	async function glue(heard: unknown[]): Promise<{ resumable(id: string, settings: object,
		attributes?: object): ResumableUploader | null }> {
		window.Wicket = { Upload: window.Wicket?.Upload,
			Event: { subscribe: () => undefined, unsubscribe: () => undefined },
			Ajax: { Call: class {
				ajax(attributes: unknown) {
					heard.push(attributes);
				}
			} } };
		await import('../src/wicket/upload.js');
		return window.Wicket!.Upload as { resumable(id: string, settings: object,
			attributes?: object): ResumableUploader | null };
	}

	it('sends the files with the query of Wicket\'s field and tells its callback each file stored',
		async () => {
			const heard: unknown[] = [];
			const upload = await glue(heard);
			document.body.innerHTML = '<p><input type="file" id="field"/></p>';
			const input = document.getElementById('field') as HTMLInputElement;
			const uploader = upload.resumable('field', SETTINGS, { c: 'x', ep: { veil: 'no' } })!;
			expect(uploader.input).toBe(input);
			expect(input.id).toBe('field');
			expect(uploader.host.id).toBe('field-uploader');
			expect(uploader.host.contains(input)).toBe(true);
			server.storing = 'stored-1';
			uploader.add([file(8)]);
			await settle();
			const query = '?uploadId=field&maxSize=100&fileMaxSize=50&fileCountMax=-1' +
				'&uploadToken=to%20ken&grantExpires=1767225600000';
			expect(server.of('POST')[0].url).toBe(ENDPOINT + query);
			expect(server.of('PATCH')[0].url).toBe(ENDPOINT + '/u1' + query);
			expect(heard).toEqual([{ c: 'x', u: 'callback', m: 'POST', ep: [{ name: 'veil',
				value: 'no' }, { name: 'error', value: 'false' }, { name: 'filesInfo',
				value: JSON.stringify([{ fileName: 'stored-1', clientFileName: 'film.mp4',
					contentType: 'video/mp4' }]) }] }]);
			expect(upload.resumable('missing', SETTINGS)).toBeNull();
		});

	it('sends the context of the field with its grant', async () => {
		const upload = await glue([]);
		document.body.innerHTML = '<input type="file" id="field"/>';
		const uploader = upload.resumable('field', { ...SETTINGS, uploadContext: 'videos' })!;
		uploader.add([file(8)]);
		await settle();
		expect(server.of('POST')[0].url).toContain('&grantExpires=1767225600000&uploadContext=videos');
	});

	it('tells the callback the key of a refusal, and a cancellation', async () => {
		const heard: Array<{ ep: Array<{ name: string; value: string }> }> = [];
		const upload = await glue(heard);
		document.body.innerHTML = '<input type="file" id="field"/>';
		const uploader = upload.resumable('field', { ...SETTINGS,
			errorTexts: { resumableUploadTooMany: 'Too many!' } })!;
		server.plan('POST', (request) => request.xhr.respond(403,
			{ 'wicket-upload-error': 'resumableUploadTooMany' }));
		uploader.add([file(8)]);
		await settle();
		expect(heard.map((call) => call.ep)).toEqual([[{ name: 'error', value: 'true' },
			{ name: 'errorMessage', value: 'resumableUploadTooMany' }]]);
		expect(uploader.list.textContent).toContain('Too many!');

		server.holding = true;
		uploader.add([file(9, 'other.mp4')]);
		await settle();
		await uploader.cancel(uploader.items[1].id);
		await settle();
		expect(heard[1].ep).toEqual([{ name: 'error', value: 'true' },
			{ name: 'errorMessage', value: 'upload.canceled' }]);
	});

	it('lets the page ask whether to go on, with the place of the field', async () => {
		vi.useFakeTimers();
		const upload = await glue([]);
		document.body.innerHTML = '<input type="file" id="field"/>';
		const asked: Array<{ reason: string; files: unknown[]; resume(): void }> = [];
		const resumable = upload.resumable as (id: string, settings: object, attributes: object,
			ask: (question: { reason: string; files: unknown[]; resume(): void }) => boolean) =>
			ResumableUploader;
		const uploader = resumable('field', { ...SETTINGS, rememberedAs: 'Page:field',
			retryDelays: [], probeDelays: [10] }, {}, (question) => {
			asked.push(question);
			return true;
		});
		server.plan('PATCH', (request) => request.xhr.fail());
		uploader.add([file(8)]);
		await settle();
		expect(TusStorage.list(ENDPOINT)[0].rememberedAs).toBe('Page:field');
		await vi.advanceTimersByTimeAsync(10);
		await settle();
		expect(asked.map((question) => [question.reason, question.files])).toEqual([
			['reconnected', [{ name: 'film.mp4', size: 8, sent: 0 }]]]);
		expect(uploader.questionElement.hidden).toBe(true);
		asked[0].resume();
		await settle();
		expect(uploader.items[0].state).toBe('done');
	});

	it('lets the field start, cancel, pause and resume its files from the server', async () => {
		const heard: unknown[] = [];
		const upload = await glue(heard);
		document.body.innerHTML = '<input type="file" id="field"/>';
		const uploader = upload.resumable('field', { ...SETTINGS, autoStart: false })!;
		const timer = (window.Wicket!.Timer as Record<string, Record<string, () => void>>).field;
		uploader.add([file(8, 'a'), file(8, 'b')]);
		expect(uploader.items.map((item) => item.state)).toEqual(['ready', 'ready']);
		server.holding = true;
		timer.upload();
		await settle();
		expect(uploader.items.map((item) => item.state)).toEqual(['uploading', 'uploading']);
		timer.pause();
		await settle();
		expect(uploader.items.map((item) => item.state)).toEqual(['paused', 'paused']);
		timer.resume();
		await settle();
		expect(uploader.items.map((item) => item.state)).toEqual(['uploading', 'uploading']);
		timer.cancel();
		await settle();
		expect(uploader.items.map((item) => item.state)).toEqual(['canceled', 'canceled']);
		expect(server.of('DELETE')).toHaveLength(2);
	});

	it('takes the input of a new rendering of the field in place of the old one', async () => {
		const upload = await glue([]);
		document.body.innerHTML = '<input type="file" id="field"/>';
		const first = upload.resumable('field', SETTINGS)!;
		const fresh = document.createElement('input');
		fresh.type = 'file';
		document.getElementById('field')!.replaceWith(fresh);
		fresh.id = 'field';
		const second = upload.resumable('field', SETTINGS)!;
		expect(second.host).toBe(first.host);
		expect(second.input).toBe(fresh);
		expect(document.querySelectorAll('input[type=file]').length).toBe(1);
		expect(ResumableUploader.of(first.host)).toBe(second);
	});
});

describe('the uploads to Wicket\'s resource', () => {
	it('keeps the name the server stored a file under, and goes on with a stored one', async () => {
		server.storing = 'kept';
		const done: TusUpload[] = [];
		const upload = new TusUpload(file(10), { endpoint: ENDPOINT, query: 'a=1',
			onSuccess: (finished) => done.push(finished) });
		upload.start();
		await settle();
		expect(upload.storedName).toBe('kept');
		expect(done).toEqual([upload]);
		expect(server.received.every((request) => request.url.endsWith('?a=1'))).toBe(true);
		expect(TusStorage.find(upload.fingerprint)).toBeNull();

		// the answer of the last chunk got lost: asked, the server tells the name
		const again = new TusUpload(file(10), { endpoint: ENDPOINT, query: 'a=2' });
		TusStorage.save({ fingerprint: again.fingerprint, url: ENDPOINT + '/u1', endpoint: ENDPOINT,
			name: 'film.mp4', size: 10, type: 'video/mp4', lastModified: 1, stored: 1 });
		again.start();
		await settle();
		expect(again.state).toBe('done');
		expect(again.storedName).toBe('kept');
		expect(server.of('HEAD')[0].url).toBe(ENDPOINT + '/u1?a=2');
		expect(server.of('PATCH').length).toBe(1);
	});

	it('stores an empty file as the server creates it', async () => {
		server.storing = 'empty';
		const upload = new TusUpload(file(0), { endpoint: ENDPOINT });
		upload.start();
		await settle();
		expect(upload.state).toBe('done');
		expect(upload.storedName).toBe('empty');
	});

	it('tells the key of a refusal, and probes and terminates with the query', async () => {
		server.plan('POST', (request) => request.xhr.respond(507,
			{ 'wicket-upload-error': 'resumableUploadNoRoom' }));
		const errors: TusError[] = [];
		const upload = new TusUpload(file(10), { endpoint: ENDPOINT, retryDelays: [],
			onError: (error) => errors.push(error) });
		upload.start();
		await settle();
		expect(errors[0].key).toBe('resumableUploadNoRoom');
		expect(new TusError('x', 0, 'network').key).toBeNull();

		server.uploads.set('u9', { length: 3, offset: 1, metadata: '' });
		expect(await probe(ENDPOINT + '/u9', { query: 'q=1' })).toEqual({ offset: 1, length: 3 });
		expect(await terminate(ENDPOINT + '/u9', { query: 'q=1' })).toBe(true);
		expect(server.received.slice(-2).map((request) => request.url))
			.toEqual([ENDPOINT + '/u9?q=1', ENDPOINT + '/u9?q=1']);
		expect(withQuery('a?b', 'c')).toBe('a?b&c');
		expect(withQuery('a', undefined)).toBe('a');
	});
});
