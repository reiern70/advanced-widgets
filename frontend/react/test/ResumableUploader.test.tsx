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

import { createRef } from 'react';
import { act, cleanup, render } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { BOOTSTRAP_LOOK, LookProvider, ResumableUploader, type ResumableItem,
	type ResumableUploaderHandle } from '../src/index.js';

/** An XMLHttpRequest answering as a tus server keeping every byte. */
class FakeXhr {
	static uploads = new Map<string, number>();

	static requests: string[] = [];

	upload = new EventTarget();

	status = 0;

	responseText = '';

	withCredentials = false;

	onload: (() => void) | null = null;

	onerror: (() => void) | null = null;

	onabort: (() => void) | null = null;

	ontimeout: (() => void) | null = null;

	private method = '';

	private url = '';

	private headers: Record<string, string> = {};

	private answer: Record<string, string> = {};

	open(method: string, url: string): void {
		this.method = method;
		this.url = url;
	}

	setRequestHeader(name: string, value: string): void {
		this.headers[name.toLowerCase()] = value;
	}

	getResponseHeader(name: string): string | null {
		return this.answer[name.toLowerCase()] ?? null;
	}

	abort(): void {
		this.onabort?.();
	}

	send(body: Blob | null): void {
		FakeXhr.requests.push(this.method + ' ' + this.url);
		const id = this.url.split('/files/')[1];
		if (this.method === 'POST') {
			const name = 'u' + (FakeXhr.uploads.size + 1);
			FakeXhr.uploads.set(name, 0);
			this.reply(201, { location: '/files/' + name });
		} else if (this.method === 'PATCH') {
			const offset = (FakeXhr.uploads.get(id) ?? 0) + (body?.size ?? 0);
			FakeXhr.uploads.set(id, offset);
			this.reply(204, { 'upload-offset': String(offset) });
		} else {
			this.reply(404);
		}
	}

	private reply(status: number, headers: Record<string, string> = {}): void {
		void Promise.resolve().then(() => {
			this.status = status;
			this.answer = headers;
			this.onload?.();
		});
	}
}

async function settle(): Promise<void> {
	await act(async () => {
		for (let index = 0; index < 30; index++) {
			await Promise.resolve();
		}
	});
}

const ENDPOINT = 'http://localhost/files';

beforeEach(() => {
	FakeXhr.uploads.clear();
	FakeXhr.requests = [];
	vi.stubGlobal('XMLHttpRequest', FakeXhr);
	localStorage.clear();
});

afterEach(() => {
	cleanup();
	vi.unstubAllGlobals();
	localStorage.clear();
});

describe('ResumableUploader', () => {
	it('draws the picker into its element and sends the files, telling each finished', async () => {
		const completed: ResumableItem[] = [];
		const changes = vi.fn();
		const ref = createRef<ResumableUploaderHandle>();
		const { container } = render(<ResumableUploader ref={ref} id="up" className="mine"
			endpoint={ENDPOINT} metadata={() => ({ album: 'x' })}
			onComplete={(item) => completed.push(item)} onChange={changes} />);
		const element = container.querySelector('#up')!;
		expect(element.className).toBe('mine');
		expect(element.querySelector('.antilia-resumable-upload')).not.toBeNull();
		expect(element.querySelector('.antilia-file-picker-zone')).not.toBeNull();

		act(() => ref.current!.add([new File(['hello'], 'a.txt')]));
		await settle();
		expect(completed.map((item) => [item.name, item.state, item.url])).toEqual([
			['a.txt', 'done', 'http://localhost/files/u1']]);
		expect(changes).toHaveBeenCalled();
		expect(ref.current!.uploader!.items).toHaveLength(1);
		expect(FakeXhr.requests).toEqual(['POST ' + ENDPOINT, 'PATCH http://localhost/files/u1']);
	});

	it('pauses and resumes all through its handle, waiting for the start buttons when told', async () => {
		const ref = createRef<ResumableUploaderHandle>();
		render(<ResumableUploader ref={ref} endpoint={ENDPOINT} autoStart={false} />);
		act(() => ref.current!.add([new File(['a'], 'a.txt'), new File(['b'], 'b.txt')]));
		expect(ref.current!.uploader!.items.map((item) => item.state)).toEqual(['ready', 'ready']);
		act(() => ref.current!.resumeAll());
		act(() => ref.current!.pauseAll());
		await settle();
		expect(ref.current!.uploader!.items.map((item) => item.state))
			.toEqual(['paused', 'paused']);
	});

	it('takes other props without losing its files, and a new uploader for another endpoint', async () => {
		const ref = createRef<ResumableUploaderHandle>();
		const { rerender } = render(<LookProvider look="antilia"><ResumableUploader ref={ref}
			endpoint={ENDPOINT} autoStart={false} /></LookProvider>);
		act(() => ref.current!.add([new File(['a'], 'a.txt')]));
		const first = ref.current!.uploader;
		rerender(<LookProvider look={BOOTSTRAP_LOOK}><ResumableUploader ref={ref}
			endpoint={ENDPOINT} autoStart={false} design="button"
			texts={{ start: 'Go {0}' }} /></LookProvider>);
		expect(ref.current!.uploader).toBe(first);
		expect(first!.items).toHaveLength(1);
		expect(first!.list.querySelector('button')!.getAttribute('aria-label')).toBe('Go a.txt');
		expect(first!.picker.zone).toBeNull();

		rerender(<LookProvider look={BOOTSTRAP_LOOK}><ResumableUploader ref={ref}
			endpoint="http://localhost/other" autoStart={false} /></LookProvider>);
		expect(ref.current!.uploader).not.toBe(first);
		expect(ref.current!.uploader!.items).toEqual([]);
	});
});
