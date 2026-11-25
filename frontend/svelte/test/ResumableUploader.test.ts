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

import { cleanup, render } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { BOOTSTRAP_LOOK, LOOK_CONTEXT, ResumableUploader, type ResumableItem } from '../src/index.js';

/** An XMLHttpRequest answering as a tus server keeping every byte. */
class FakeXhr {
	static uploads = new Map<string, number>();

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

	private answer: Record<string, string> = {};

	open(method: string, url: string): void {
		this.method = method;
		this.url = url;
	}

	setRequestHeader(): void {
	}

	getResponseHeader(name: string): string | null {
		return this.answer[name.toLowerCase()] ?? null;
	}

	abort(): void {
		this.onabort?.();
	}

	send(body: Blob | null): void {
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
	for (let index = 0; index < 30; index++) {
		await Promise.resolve();
	}
}

const ENDPOINT = 'http://localhost/files';

beforeEach(() => {
	FakeXhr.uploads.clear();
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
		const onchange = vi.fn();
		const { container, component } = render(ResumableUploader, { props: { id: 'up',
			class: 'mine', endpoint: ENDPOINT, metadata: () => ({ album: 'x' }),
			oncomplete: (item: ResumableItem) => completed.push(item), onchange } });
		const element = container.querySelector('#up')!;
		expect(element.className).toBe('mine');
		expect(element.querySelector('.antilia-resumable-upload')).not.toBeNull();

		component.add([new File(['hello'], 'a.txt')]);
		await settle();
		expect(completed.map((item) => [item.name, item.state, item.url])).toEqual([
			['a.txt', 'done', 'http://localhost/files/u1']]);
		expect(onchange).toHaveBeenCalled();
		expect(component.uploader()!.items).toHaveLength(1);
	});

	it('pauses and resumes all, and takes other props without losing its files', async () => {
		const { component, rerender } = render(ResumableUploader, {
			props: { endpoint: ENDPOINT, autoStart: false },
			context: new Map([[LOOK_CONTEXT, BOOTSTRAP_LOOK]]) });
		component.add([new File(['a'], 'a.txt'), new File(['b'], 'b.txt')]);
		const first = component.uploader()!;
		expect(first.items.map((item) => item.state)).toEqual(['ready', 'ready']);
		expect(first.list.className).toContain('list-group');
		component.resumeAll();
		component.pauseAll();
		await settle();
		expect(first.items.map((item) => item.state)).toEqual(['paused', 'paused']);

		await rerender({ endpoint: ENDPOINT, autoStart: false, design: 'button',
			texts: { resume: 'Go on with {0}' } });
		flushSync();
		expect(component.uploader()).toBe(first);
		expect(first.picker.zone).toBeNull();
		expect(first.list.querySelector('button:not([hidden])')!.getAttribute('aria-label'))
			.toBe('Go on with a.txt');

		await rerender({ endpoint: 'http://localhost/other', autoStart: false });
		flushSync();
		expect(component.uploader()).not.toBe(first);
		expect(component.uploader()!.items).toEqual([]);
	});
});
