/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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

import { accepts, BOOTSTRAP_LOOK, FilePicker, filePicker, filesSize, formatBytes, formatDuration, setFiles,
	setHost, standaloneHost, UPLOAD_BLOCKED_ATTRIBUTE, UPLOAD_BLOCKED_EVENT, UPLOAD_END_EVENT,
	UPLOAD_REFUSED_EVENT, uploadEnded, UploadGuard, uploadProblems, UploadProgress, uploadRefused,
	uploadStarted, type Host } from '../src/index.js';
import { format } from '../src/upload.js';

/** A stand-in for the DataTransfer jsdom lacks: its files are the files added. */
class FakeDataTransfer {
	readonly added: File[] = [];

	readonly items = { add: (file: File) => this.added.push(file) };

	get files(): File[] {
		return this.added;
	}
}

function file(name: string, size: number, type = ''): File {
	return new File(['x'.repeat(size)], name, { type });
}

/** Lets a test give an input files, which jsdom only takes as a FileList. */
function settable(input: HTMLInputElement): HTMLInputElement {
	let files: File[] = [];
	Object.defineProperty(input, 'files', {
		configurable: true,
		get: () => files,
		set: (value: File[]) => {
			files = Array.from(value);
		}
	});
	Object.defineProperty(input, 'value', {
		configurable: true,
		get: () => files.length ? 'C:\\fakepath\\' + files[0].name : '',
		set: (value: string) => {
			if (value === '') {
				files = [];
			}
		}
	});
	return input;
}

function choose(input: HTMLInputElement, ...files: File[]): void {
	input.files = files as unknown as FileList;
	input.dispatchEvent(new Event('change', { bubbles: true }));
}

/** What the browser fires once files were chosen in its dialog, which a test cannot fire. */
function trusted(input: HTMLInputElement, type: string): Event & { stopped: boolean } {
	const event = { isTrusted: true, type, target: input, stopped: false,
		stopImmediatePropagation() {
			event.stopped = true;
		} };
	return event as unknown as Event & { stopped: boolean };
}

/** Hands a picker an event of the document, as its listeners would. */
function onChoice(picker: FilePicker, event: Event): void {
	(picker as unknown as { onChoice(event: Event): void }).onChoice(event);
}

function dragEvent(type: string, files: File[] = []): Event {
	const event = new Event(type, { bubbles: true, cancelable: true });
	Object.defineProperty(event, 'dataTransfer', {
		value: { types: ['Files'], files, dropEffect: 'none' }
	});
	return event;
}

/** A request whose upload progress events a test fires. */
function fakeXhr(): XMLHttpRequest {
	const xhr = new EventTarget() as XMLHttpRequest & { status: number };
	Object.defineProperty(xhr, 'upload', { value: new EventTarget() });
	Object.defineProperty(xhr, 'status', { value: 200, writable: true });
	return xhr;
}

function progressEvent(loaded: number, total: number): ProgressEvent {
	const event = new Event('progress') as ProgressEvent;
	Object.defineProperties(event, {
		lengthComputable: { value: true },
		loaded: { value: loaded },
		total: { value: total }
	});
	return event;
}

beforeEach(() => {
	vi.useFakeTimers();
	vi.stubGlobal('DataTransfer', FakeDataTransfer);
});

afterEach(() => {
	vi.useRealTimers();
	vi.unstubAllGlobals();
	setHost();
	UploadGuard.reset();
	document.body.innerHTML = '';
});

describe('formatting', () => {
	it('writes bytes in the largest unit they make at least one of', () => {
		expect(formatBytes(512, 'en')).toBe('512 byte');
		expect(formatBytes(1536, 'en')).toBe('1.5 kB');
		expect(formatBytes(5 * 1024 * 1024, 'en')).toBe('5 MB');
		expect(formatBytes(-3, 'en')).toBe('0 byte');
		expect(formatBytes(1536, 'de')).toBe('1,5 kB');
	});

	it('writes durations in seconds, minutes and hours', () => {
		expect(formatDuration(4.2, 'en')).toBe('5 sec');
		expect(formatDuration(120, 'en')).toBe('2 min');
		expect(formatDuration(125, 'en')).toBe('2 min, 5 sec');
		expect(formatDuration(3600, 'en')).toBe('1 hr');
		expect(formatDuration(3660, 'en')).toBe('1 hr, 1 min');
	});

	it('replaces the placeholders it has values for', () => {
		expect(format('{0} of {1} {2}', 1, 2)).toBe('1 of 2 {2}');
	});

	it('accepts files by extension, MIME type and group of types', () => {
		const png = { name: 'Photo.PNG', type: 'image/png' };
		expect(accepts('', png)).toBe(true);
		expect(accepts(null, png)).toBe(true);
		expect(accepts('.png', png)).toBe(true);
		expect(accepts('.pdf, image/*', png)).toBe(true);
		expect(accepts('image/png', png)).toBe(true);
		expect(accepts('image/jpeg,.pdf', png)).toBe(false);
		expect(accepts('text/*', { name: 'notes', type: '' })).toBe(false);
	});
});

describe('FilePicker', () => {
	it('hides the input behind a button of the theme and tells what was chosen', () => {
		document.body.innerHTML = '<form><input type="file" id="f" class="field"></form>';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		const picker = FilePicker.attach('f')!;

		expect(FilePicker.of('f')).toBe(picker);
		expect(input.classList.contains('antilia-file-picker-native')).toBe(true);
		expect(input.getAttribute('aria-hidden')).toBe('true');
		expect(input.tabIndex).toBe(-1);
		expect(input.nextElementSibling).toBe(picker.element);
		expect(picker.element.classList.contains('antilia-file-picker-button')).toBe(true);
		expect(picker.button.classList.contains('antilia-file-picker-choose')).toBe(true);
		expect(picker.button.className).toContain('ant-button ant-button--secondary');
		expect(picker.button.textContent).toBe('Choose a file…');
		expect(picker.button.getAttribute('aria-describedby')).toBe('f-summary f-error');
		expect(picker.error.hidden).toBe(true);
		expect(picker.error.getAttribute('role')).toBe('alert');
		expect(picker.summary.textContent).toBe('No file chosen');
		expect(picker.clearButton.hidden).toBe(true);
		expect(picker.list.hidden).toBe(true);

		const click = vi.spyOn(input, 'click').mockImplementation(() => undefined);
		picker.button.click();
		expect(click).toHaveBeenCalledTimes(1);

		choose(input, file('notes.txt', 5));
		expect(picker.summary.textContent).toBe('notes.txt (5 byte)');
		expect(picker.clearButton.hidden).toBe(false);
		expect(picker.list.hidden).toBe(true);
	});

	it('lists several files and clears them', () => {
		document.body.innerHTML = '<input type="file" id="f" multiple>';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		const picker = new FilePicker(input, { texts: { chosen: '{0} Dateien, {1}' }, locale: 'de',
			variant: 'primary', size: 'large' });
		const changes = vi.fn();
		input.addEventListener('change', changes);

		expect(picker.button.textContent).toBe('Choose files…');
		expect(picker.button.className).toContain('ant-button--primary ant-button--large');
		choose(input, file('a.txt', 1024), file('b.txt', 512));
		expect(picker.summary.textContent).toBe('2 Dateien, 1,5 kB');
		expect(picker.list.hidden).toBe(false);
		expect(Array.from(picker.list.children).map((item) => item.textContent))
			.toEqual(['a.txt1 kB', 'b.txt512 Byte']);

		picker.clearButton.click();
		expect(input.files).toHaveLength(0);
		expect(changes).toHaveBeenCalledTimes(2);
		expect(picker.summary.textContent).toBe('No file chosen');
		expect(document.activeElement).toBe(picker.button);

		picker.clearButton.blur();
		choose(input, file('c.txt', 1));
		(document.activeElement as HTMLElement | null)?.blur();
		picker.clear();
		expect(changes).toHaveBeenCalledTimes(4);
		expect(document.activeElement).not.toBe(picker.button);
		picker.clear();
		expect(changes).toHaveBeenCalledTimes(4);
	});

	it('draws a zone files are dropped on, taking those the input accepts', () => {
		document.body.innerHTML = '<input type="file" id="f" accept=".png,image/jpeg">';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		const picker = new FilePicker(input, { design: 'drop-zone' });
		const zone = picker.zone!;
		const changes = vi.fn();
		input.addEventListener('change', changes);

		expect(picker.element.classList.contains('antilia-file-picker-drop-zone')).toBe(true);
		expect(zone.querySelector('.antilia-file-picker-icon')).not.toBeNull();
		expect(zone.textContent).toContain('Drop a file here or browse');
		expect(picker.button.getAttribute('aria-label')).toBe('Choose a file…');
		expect(picker.button.className).toContain('ant-button--ghost ant-button--small');

		const enter = dragEvent('dragenter');
		zone.dispatchEvent(enter);
		expect(enter.defaultPrevented).toBe(true);
		expect(zone.classList.contains('antilia-file-picker-zone-over')).toBe(true);
		zone.querySelector('svg')!.dispatchEvent(dragEvent('dragenter'));
		zone.dispatchEvent(dragEvent('dragleave'));
		expect(zone.classList.contains('antilia-file-picker-zone-over')).toBe(true);
		const over = dragEvent('dragover');
		zone.dispatchEvent(over);
		expect((over as DragEvent).dataTransfer!.dropEffect).toBe('copy');

		const drop = dragEvent('drop', [file('a.txt', 3), file('b.png', 4, 'image/png'),
			file('c.jpg', 5, 'image/jpeg')]);
		zone.dispatchEvent(drop);
		expect(drop.defaultPrevented).toBe(true);
		expect(zone.classList.contains('antilia-file-picker-zone-over')).toBe(false);
		expect(Array.from(input.files!).map((f) => f.name)).toEqual(['b.png']);
		expect(changes).toHaveBeenCalledTimes(1);
		expect(picker.summary.textContent).toBe('b.png (4 byte) · Not accepted: a.txt, c.jpg');

		choose(input, file('d.png', 1));
		expect(picker.summary.textContent).toBe('d.png (1 byte)');
	});

	it('opens the dialog from anywhere in the zone, and ignores drags of anything but files', () => {
		document.body.innerHTML = '<input type="file" id="f" multiple>';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		const picker = new FilePicker(input, { design: 'drop-zone' });
		const click = vi.spyOn(input, 'click').mockImplementation(() => undefined);

		picker.zone!.click();
		picker.button.click();
		expect(click).toHaveBeenCalledTimes(2);
		expect(picker.zone!.textContent).toContain('Drop files here or');

		const text = new Event('dragenter', { cancelable: true });
		Object.defineProperty(text, 'dataTransfer', { value: { types: ['text/plain'] } });
		picker.zone!.dispatchEvent(text);
		expect(text.defaultPrevented).toBe(false);
		const nothing = new Event('drop', { cancelable: true });
		picker.zone!.dispatchEvent(nothing);
		expect(nothing.defaultPrevented).toBe(false);

		expect(picker.drop([file('a', 1), file('b', 2)])).toBe(true);
		expect(input.files).toHaveLength(2);
		expect(picker.drop([])).toBe(false);
	});

	it('does nothing while the input is disabled', () => {
		document.body.innerHTML = '<input type="file" id="f" disabled>';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		const picker = new FilePicker(input, { design: 'drop-zone' });
		const click = vi.spyOn(input, 'click').mockImplementation(() => undefined);

		expect(picker.button.disabled).toBe(true);
		expect(picker.zone!.getAttribute('aria-disabled')).toBe('true');
		expect(picker.element.classList.contains('antilia-file-picker-disabled')).toBe(true);
		picker.open();
		expect(click).not.toHaveBeenCalled();
		const enter = dragEvent('dragenter');
		picker.zone!.dispatchEvent(enter);
		expect((enter as DragEvent).dataTransfer!.dropEffect).toBe('none');
		expect(picker.zone!.classList.contains('antilia-file-picker-zone-over')).toBe(false);
		picker.zone!.dispatchEvent(dragEvent('drop', [file('a', 1)]));
		expect(input.files).toHaveLength(0);
	});

	it('takes other options, and goes when the input is taken away', () => {
		document.body.innerHTML = '<input type="file" id="f">';
		const input = document.getElementById('f') as HTMLInputElement;
		let removing: ((element: Element) => void) | undefined;
		const host: Host = {
			...standaloneHost,
			onNodeRemoving(listener) {
				removing = listener;
				return () => {
					removing = undefined;
				};
			}
		};
		setHost(host);
		const picker = new FilePicker(input);
		picker.configure({ design: 'drop-zone', look: BOOTSTRAP_LOOK });
		expect(document.querySelectorAll('.antilia-file-picker')).toHaveLength(1);
		expect(picker.zone!.classList.contains('card')).toBe(true);

		removing!(document.body);
		expect(document.querySelector('.antilia-file-picker')).toBeNull();
		expect(input.hasAttribute('class')).toBe(false);
		expect(input.hasAttribute('aria-hidden')).toBe(false);
		expect(input.hasAttribute('tabindex')).toBe(false);
		expect(FilePicker.of(input)).toBeNull();
		expect(removing).toBeUndefined();
	});

	it('replaces the picker of an input attached again, and works as an action', () => {
		document.body.innerHTML = '<input type="file" id="f" class="mine"><input id="t">';
		const input = document.getElementById('f') as HTMLInputElement;
		const first = new FilePicker(input);
		const action = filePicker(input, { texts: { choose: 'Pick' } });
		expect(document.querySelectorAll('.antilia-file-picker')).toHaveLength(1);
		expect(FilePicker.of(input)).not.toBe(first);
		expect(FilePicker.of(input)!.button.textContent).toBe('Pick');
		action.update({ texts: { choose: 'Take' } });
		expect(FilePicker.of(input)!.button.textContent).toBe('Take');
		action.destroy();
		expect(input.className).toBe('mine');
		expect(document.querySelector('.antilia-file-picker')).toBeNull();

		expect(FilePicker.attach('t')).toBeNull();
		expect(FilePicker.attach('missing')).toBeNull();
		expect(FilePicker.of(null)).toBeNull();
	});

	it('adds the files chosen again or dropped to those of an input taking several', () => {
		document.body.innerHTML = '<input type="file" id="f" multiple>';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		const picker = new FilePicker(input);
		const changes = vi.fn();
		input.addEventListener('change', changes);
		const a = file('a.txt', 1);
		const b = file('b.txt', 2);
		const names = () => picker.files.map((f) => f.name);

		// the first choice in the dialog: nothing to add to
		input.files = [a] as unknown as FileList;
		const first = trusted(input, 'change');
		onChoice(picker, first);
		expect(first.stopped).toBe(false);
		input.dispatchEvent(new Event('change'));
		expect(changes).toHaveBeenCalledTimes(1);

		// the next replaced the files of the input: the page hears of all of them, once
		input.files = [b, file('a.txt', 1)] as unknown as FileList;
		const typing = trusted(input, 'input');
		onChoice(picker, typing);
		expect(typing.stopped).toBe(true);
		const choice = trusted(input, 'change');
		onChoice(picker, choice);
		expect(choice.stopped).toBe(true);
		expect(names()).toEqual(['a.txt', 'b.txt']);
		expect(changes).toHaveBeenCalledTimes(2);
		expect(picker.summary.textContent).toBe('2 files, 3 byte');

		// events of the page, and of the other inputs, are no choices in the dialog
		onChoice(picker, new Event('change'));
		onChoice(picker, trusted(document.createElement('input'), 'change'));
		expect(changes).toHaveBeenCalledTimes(2);

		// dropped files join those chosen, once each
		expect(picker.drop([file('c.txt', 3), file('b.txt', 2)])).toBe(true);
		expect(names()).toEqual(['a.txt', 'b.txt', 'c.txt']);
		expect(picker.summary.textContent).toBe('3 files, 6 byte');
	});

	it('takes each file out of the list again', () => {
		document.body.innerHTML = '<input type="file" id="f" multiple>';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		const picker = new FilePicker(input);
		choose(input, file('a.txt', 1), file('b.txt', 2), file('c.txt', 3));
		const removes = () => Array.from(picker.list.querySelectorAll('button'));

		expect(removes().map((button) => button.getAttribute('aria-label')))
			.toEqual(['Remove a.txt', 'Remove b.txt', 'Remove c.txt']);
		expect(removes()[0].className).toContain('antilia-file-picker-remove');
		removes()[1].click();
		expect(picker.files.map((f) => f.name)).toEqual(['a.txt', 'c.txt']);
		expect(document.activeElement).toBe(removes()[1]);
		removes()[1].click();
		expect(picker.list.hidden).toBe(true);
		expect(picker.summary.textContent).toBe('a.txt (1 byte)');
		expect(document.activeElement).toBe(picker.button);

		expect(picker.remove(3)).toBe(false);
		expect(picker.remove(-1)).toBe(false);
		expect(picker.remove(0)).toBe(true);
		expect(input.files).toHaveLength(0);
	});

	it('replaces the file of an input taking one', () => {
		document.body.innerHTML = '<input type="file" id="f">';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		const picker = new FilePicker(input);
		choose(input, file('a.txt', 1));
		input.files = [file('b.txt', 2)] as unknown as FileList;
		const choice = trusted(input, 'change');
		onChoice(picker, choice);
		expect(choice.stopped).toBe(false);
		expect(picker.drop([file('c.txt', 3), file('d.txt', 4)])).toBe(true);
		expect(picker.files.map((f) => f.name)).toEqual(['c.txt']);
		expect(picker.summary.textContent).toBe('c.txt (3 byte) · Not accepted: d.txt');
	});

	it('tells at once the limits the files chosen break', () => {
		document.body.innerHTML = '<form id="form"><input type="file" id="f" multiple>' +
			'<input type="file" id="g"></form>';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		settable(document.getElementById('g') as HTMLInputElement);
		const picker = new FilePicker(input, { design: 'drop-zone', locale: 'en', maxSize: 100,
			fileMaxSize: 40, fileCountMax: 2 });
		new FilePicker(document.getElementById('g') as HTMLInputElement);

		choose(input, file('a.bin', 30), file('b.bin', 30));
		expect(picker.problems()).toEqual([]);
		expect(picker.error.hidden).toBe(true);
		expect(picker.element.classList.contains('antilia-file-picker-invalid')).toBe(false);

		choose(input, file('big.bin', 50), file('b.bin', 30), file('c.bin', 30));
		expect(picker.problems().map((problem) => problem.kind))
			.toEqual(['tooLarge', 'tooMany', 'tooMuch']);
		expect(picker.error.hidden).toBe(false);
		expect(picker.error.textContent).toBe('big.bin: larger than 40 byte, the most for a file · ' +
			'3 files, more than the 2 sent at once · 110 byte together, more than the 100 byte sent at once');
		expect(picker.element.classList.contains('antilia-file-picker-invalid')).toBe(true);
		expect(picker.zone!.classList.contains('antilia-file-picker-zone-invalid')).toBe(true);
		const items = Array.from(picker.list.children);
		expect(items.map((item) => item.classList.contains('antilia-file-picker-file-invalid')))
			.toEqual([true, false, false]);
		expect(uploadProblems(input)).toHaveLength(3);
		expect(uploadProblems(document.getElementById('form'))).toHaveLength(3);
		expect(uploadProblems(null)).toEqual([]);

		picker.clear();
		expect(picker.error.hidden).toBe(true);
		expect(picker.zone!.classList.contains('antilia-file-picker-zone-invalid')).toBe(false);
	});

	it('takes several files of a name', () => {
		document.body.innerHTML = '<input type="file" id="f" multiple>';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		const picker = new FilePicker(input);

		choose(input, file('photo.png', 1), file('notes.txt', 2), file('photo.png', 3));
		expect(picker.problems()).toEqual([]);
		expect(picker.error.hidden).toBe(true);
	});

	it('lets nothing change while the files of its input, or of its form, are being sent', () => {
		document.body.innerHTML = '<form id="form"><input type="file" id="f" multiple></form>';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		const picker = new FilePicker(input, { design: 'drop-zone' });
		const click = vi.spyOn(input, 'click').mockImplementation(() => undefined);
		choose(input, file('a.txt', 1), file('b.txt', 2));

		uploadStarted('f');
		expect(picker.busy).toBe(true);
		expect(picker.button.disabled).toBe(true);
		expect(picker.element.hasAttribute('aria-busy')).toBe(true);
		expect(picker.list.querySelector('button')!.disabled).toBe(true);
		picker.open();
		expect(click).not.toHaveBeenCalled();
		expect(picker.drop([file('c.txt', 3)])).toBe(false);
		const enter = dragEvent('dragenter');
		picker.zone!.dispatchEvent(enter);
		expect((enter as DragEvent).dataTransfer!.dropEffect).toBe('none');
		picker.zone!.dispatchEvent(dragEvent('drop', [file('c.txt', 3)]));
		expect(input.files).toHaveLength(2);
		uploadEnded('other', 'done');
		expect(picker.busy).toBe(true);
		uploadEnded('f', 'done');
		expect(picker.busy).toBe(false);
		expect(picker.button.disabled).toBe(false);

		uploadStarted('form');
		expect(picker.busy).toBe(true);
		uploadEnded('form', 'failed');
		expect(picker.busy).toBe(false);
		// a form posted the usual way leaves the page, which may not happen
		uploadStarted('form', undefined, undefined, true);
		expect(picker.busy).toBe(false);
	});

	it('cannot set files where the browser has no DataTransfer or refuses them', () => {
		document.body.innerHTML = '<input type="file" id="f">';
		const input = document.getElementById('f') as HTMLInputElement;
		expect(setFiles(input, [file('a', 1)])).toBe(false);
		vi.stubGlobal('DataTransfer', undefined);
		expect(setFiles(input, [file('a', 1)])).toBe(false);
	});
});

describe('UploadProgress', () => {
	it('stays hidden until an upload of its source starts, then follows the request', () => {
		document.body.innerHTML = '<div id="p"></div><input type="file" id="f">';
		const progress = UploadProgress.attach('p', { source: 'f', locale: 'en' })!;
		expect(UploadProgress.of('p')).toBe(progress);
		expect(progress.box.hidden).toBe(true);
		expect(progress.box.className).toBe('antilia-upload-progress');
		expect(progress.status.getAttribute('role')).toBe('status');

		uploadStarted('other');
		expect(progress.box.hidden).toBe(true);

		const xhr = fakeXhr();
		uploadStarted('f', xhr);
		expect(progress.running).toBe(true);
		expect(filesSize(document.getElementById('f'))).toBe(0);
		expect(filesSize(null)).toBe(0);
		expect(progress.box.hidden).toBe(false);
		expect(progress.value.hasAttribute('value')).toBe(false);
		expect(progress.label.hidden).toBe(true);
		expect(progress.status.textContent).toBe('Starting the upload…');

		vi.advanceTimersByTime(2000);
		xhr.upload.dispatchEvent(progressEvent(1024, 4096));
		expect(progress.value.getAttribute('value')).toBe('25');
		expect(progress.label.textContent).toBe('25%');
		expect(progress.status.textContent).toBe('1 kB of 4 kB · 512 byte/s · 6 sec left');

		xhr.upload.dispatchEvent(progressEvent(4096, 4096));
		expect(progress.status.textContent).toBe('Sent 4 kB, the server takes the files…');

		uploadEnded('f', 'done');
		expect(progress.running).toBe(false);
		expect(progress.value.getAttribute('value')).toBe('100');
		expect(progress.status.textContent).toBe('Uploaded 4 kB');
		expect(progress.status.classList.contains('antilia-upload-status-done')).toBe(true);
		expect(progress.box.getAttribute('data-state')).toBe('done');
		xhr.upload.dispatchEvent(progressEvent(1, 4096));
		expect(progress.value.getAttribute('value')).toBe('100');
	});

	it('tells why an upload failed, or that it was canceled', () => {
		document.body.innerHTML = '<div id="p"></div>';
		const progress = new UploadProgress(document.getElementById('p')!, { source: 'f',
			texts: { canceled: 'Abgebrochen' } });
		progress.end('failed');
		expect(progress.box.hidden).toBe(true);

		progress.start();
		progress.update(50, 200);
		progress.end('failed', 'Too large');
		expect(progress.status.textContent).toBe('Too large');
		expect(progress.value.getAttribute('value')).toBe('25');
		expect(progress.status.classList.contains('antilia-upload-status-failed')).toBe(true);

		progress.start();
		progress.end('failed');
		expect(progress.status.textContent).toBe('The upload failed');
		expect(progress.value.getAttribute('value')).toBe('0');

		progress.start();
		progress.end('failed', undefined, 0);
		expect(progress.status.textContent).toBe('The connection to the server broke off');
		progress.start();
		progress.end('failed', undefined, 413);
		expect(progress.status.textContent).toBe('The server could not take the files (413)');
		progress.start();
		progress.end('failed', 'Refused', 500);
		expect(progress.status.textContent).toBe('Refused');

		progress.start();
		expect(progress.status.classList.contains('antilia-upload-status-failed')).toBe(false);
		progress.end('canceled');
		expect(progress.status.textContent).toBe('Abgebrochen');

		progress.start(undefined, 250);
		progress.update(300, 300);
		progress.end('done');
		expect(progress.status.textContent).toBe('Uploaded 250 byte');
		progress.start();
		progress.end('done');
		expect(progress.status.textContent).toBe('Uploaded 0 byte');
		progress.start(undefined, 2048);
		progress.end('done');
		expect(progress.status.textContent).toBe('Uploaded 2 kB');

		// the server may tell later than the request that it refused the files
		progress.fail('Not today');
		expect(progress.status.textContent).toBe('Not today');
		expect(progress.box.getAttribute('data-state')).toBe('failed');
		progress.fail();
		expect(progress.status.textContent).toBe('The upload failed');
	});

	it('tells that the files were not sent, as they break a limit', () => {
		document.body.innerHTML = '<div id="p"></div>';
		const progress = new UploadProgress(document.getElementById('p')!, { source: 'f' });
		const refused = vi.fn();
		document.addEventListener(UPLOAD_REFUSED_EVENT, refused);

		uploadRefused('other', 'No');
		expect(progress.box.hidden).toBe(true);
		uploadRefused('f', 'Too many files');
		expect(refused.mock.calls[1][0].detail).toEqual({ source: 'f', message: 'Too many files' });
		expect(progress.box.hidden).toBe(false);
		expect(progress.status.textContent).toBe('Too many files');
		expect(progress.value.getAttribute('value')).toBe('0');
		expect(progress.box.getAttribute('data-state')).toBe('failed');

		// not while an upload runs
		progress.start();
		uploadRefused('f', 'Again');
		expect(progress.running).toBe(true);
		expect(progress.status.textContent).toBe('Starting the upload…');
	});

	it('goes back to hidden when the page comes back after a form posted it away', () => {
		document.body.innerHTML = '<div id="p"></div>';
		const progress = new UploadProgress(document.getElementById('p')!, { source: 'form' });
		const back = () => {
			const event = new Event('pageshow');
			Object.defineProperty(event, 'persisted', { value: true });
			window.dispatchEvent(event);
		};
		uploadStarted('form');
		back();
		expect(progress.running).toBe(true);
		uploadStarted('form', undefined, undefined, true);
		window.dispatchEvent(new Event('pageshow'));
		expect(progress.running).toBe(true);
		back();
		expect(progress.running).toBe(false);
		expect(progress.box.hidden).toBe(true);
	});

	it('asks the server how far the upload got when the start brings no request', async () => {
		document.body.innerHTML = '<div id="p"></div><form id="form"></form>';
		const answers = [{ ok: true, json: () => Promise.resolve({ state: 'unknown' }) },
			{ ok: false, json: () => Promise.reject(new Error('none')) },
			{ ok: true, json: () => Promise.resolve({ bytesUploaded: 10, totalBytes: 40 }) }];
		const fetch = vi.fn((_url: string, init: RequestInit) => answers.length
			? Promise.resolve(answers.shift())
			// no answer, until the wait for one is over
			: new Promise((_resolve, reject) => init.signal!.addEventListener('abort',
				() => reject(new Error('aborted')))));
		vi.stubGlobal('fetch', fetch);
		const progress = new UploadProgress(document.getElementById('p')!, { source: 'form',
			statusUrl: '/status?upload=3', pollMillis: 100 });

		const xhr = fakeXhr();
		uploadStarted('form', xhr);
		xhr.upload.dispatchEvent(progressEvent(40, 40));
		expect(progress.value.hasAttribute('value')).toBe(false);
		for (let i = 0; i < 4; i++) {
			await vi.advanceTimersByTimeAsync(100);
		}
		expect(fetch).toHaveBeenCalledWith('/status?upload=3', expect.objectContaining({
			cache: 'no-store', credentials: 'same-origin' }));
		expect(progress.value.getAttribute('value')).toBe('25');
		// an answer that does not come stops no polling
		const asking = fetch.mock.calls.length;
		await vi.advanceTimersByTimeAsync(10000);
		await vi.advanceTimersByTimeAsync(100);
		expect(fetch.mock.calls.length).toBe(asking + 1);

		// once it told how far it got, a server knowing of no upload read it all
		answers.push({ ok: true, json: () => Promise.resolve({ state: 'none' }) });
		await vi.advanceTimersByTimeAsync(10000);
		await vi.advanceTimersByTimeAsync(100);
		expect(progress.status.textContent).toBe('Sent 40 byte, the server takes the files…');

		uploadEnded('form', 'done');
		const asked = fetch.mock.calls.length;
		await vi.advanceTimersByTimeAsync(1000);
		expect(fetch.mock.calls.length).toBe(asked);
		expect(progress.status.textContent).toBe('Uploaded 40 byte');
	});

	it('tells for a moment that the guard stopped a request while its upload runs', () => {
		document.body.innerHTML = '<div id="p"></div>';
		const progress = new UploadProgress(document.getElementById('p')!, { source: 'f' });
		document.dispatchEvent(new CustomEvent(UPLOAD_BLOCKED_EVENT));
		expect(progress.box.hidden).toBe(true);

		progress.start();
		progress.update(10, 100);
		document.dispatchEvent(new CustomEvent(UPLOAD_BLOCKED_EVENT));
		expect(progress.status.textContent).toBe('Wait for the upload to end: the request was stopped');
		progress.update(20, 100);
		expect(progress.status.textContent).toBe('Wait for the upload to end: the request was stopped');
		vi.advanceTimersByTime(2500);
		expect(progress.status.textContent).toContain('of 100 byte');

		progress.tell('Hello', 100);
		progress.end('canceled');
		vi.advanceTimersByTime(100);
		expect(progress.status.textContent).toBe('Upload canceled');

		progress.start();
		progress.tell('Waiting');
		expect(progress.status.textContent).toBe('Waiting');
	});

	it('goes when its element is taken away, and replaces the progress drawn before', () => {
		document.body.innerHTML = '<div id="p"></div>';
		let removing: ((element: Element) => void) | undefined;
		setHost({ ...standaloneHost, onNodeRemoving(listener) {
			removing = listener;
			return () => undefined;
		} });
		const host = document.getElementById('p')!;
		const first = new UploadProgress(host, { source: 'f' });
		const second = UploadProgress.attach(host, { source: 'f' })!;
		expect(host.children).toHaveLength(1);
		expect(UploadProgress.of(host)).toBe(second);
		expect(first.running).toBe(false);

		removing!(document.body);
		expect(host.children).toHaveLength(0);
		expect(UploadProgress.of(host)).toBeNull();
		uploadStarted('f');
		expect(second.running).toBe(false);
		expect(UploadProgress.attach('missing')).toBeNull();
		expect(UploadProgress.of('missing')).toBeNull();
	});
});

describe('UploadGuard', () => {
	it('asks before the page is left while an upload keeps the user on it', () => {
		const guard = UploadGuard.begin();
		expect(UploadGuard.keepsOnPage()).toBe(true);
		expect(UploadGuard.blocking()).toBe(false);
		const leave = new Event('beforeunload', { cancelable: true });
		window.dispatchEvent(leave);
		expect(leave.defaultPrevented).toBe(true);

		guard.end();
		guard.end();
		expect(UploadGuard.keepsOnPage()).toBe(false);
		const again = new Event('beforeunload', { cancelable: true });
		window.dispatchEvent(again);
		expect(again.defaultPrevented).toBe(false);

		const free = UploadGuard.begin({ leaving: false });
		const left = new Event('beforeunload', { cancelable: true });
		window.dispatchEvent(left);
		expect(left.defaultPrevented).toBe(false);
		free.end();
	});

	it('stops the requests but those that bypass it, marking their elements for a moment', () => {
		document.body.innerHTML = '<button id="b">blocked</button>' +
			'<div data-antilia-upload-bypass><button id="free">free</button></div>';
		const button = document.getElementById('b')!;
		const blocked = vi.fn();
		document.addEventListener(UPLOAD_BLOCKED_EVENT, blocked);

		expect(UploadGuard.blocks({ element: button })).toBe(false);
		const guard = UploadGuard.begin({ requests: true, allow: ['./callback?x=1'] });
		expect(UploadGuard.blocking()).toBe(true);

		expect(UploadGuard.blocks({ element: button, url: './other' })).toBe(true);
		expect(button.hasAttribute(UPLOAD_BLOCKED_ATTRIBUTE)).toBe(true);
		expect(blocked).toHaveBeenCalledTimes(1);
		expect(blocked.mock.calls[0][0].target).toBe(button);
		vi.advanceTimersByTime(UploadGuard.blockedMillis);
		expect(button.hasAttribute(UPLOAD_BLOCKED_ATTRIBUTE)).toBe(false);

		expect(UploadGuard.blocks({ element: document.getElementById('free') })).toBe(false);
		expect(UploadGuard.blocks({ element: button, bypass: true })).toBe(false);
		expect(UploadGuard.blocks({ url: 'callback?x=1' })).toBe(false);
		expect(UploadGuard.blocks({ url: './callback?x=2' })).toBe(true);
		expect(blocked.mock.calls[1][0].target).toBe(document);

		const another = UploadGuard.begin({ requests: true });
		expect(UploadGuard.blocks({ url: 'callback?x=1' })).toBe(true);
		another.end();
		guard.end();
		expect(UploadGuard.blocks({ element: button })).toBe(false);
	});
});

describe('Wicket.Upload', () => {
	type Subscriber = (event: unknown, ...args: unknown[]) => void;
	const topics = new Map<string, Subscriber[]>();

	function publish(topic: string, ...args: unknown[]): void {
		(topics.get(topic) ?? []).forEach((subscriber) => subscriber({}, ...args));
	}

	interface UploadGlobal {
		guard(source: string, options: object | null): void;
		field(source: string, messages?: Record<string, string>): void;
		upload(source: string): boolean;
		started(source: string, xhr?: XMLHttpRequest): void;
		ended(source: string, outcome: string, message?: string): void;
		failed(source: string, error?: { errorMessage?: unknown }): void;
		precondition(attrs: object): boolean;
	}

	async function load(): Promise<UploadGlobal> {
		// the script registers once: what it registered stays
		window.Wicket = { Upload: window.Wicket?.Upload, Event: {
			subscribe: (topic: string, subscriber: Subscriber) =>
				topics.set(topic, [...(topics.get(topic) ?? []), subscriber]),
			unsubscribe: () => undefined
		} };
		await import('../src/wicket/upload.js');
		return window.Wicket!.Upload as UploadGlobal;
	}

	it('registers once, guards the uploads of fields and ends them with their request', async () => {
		const upload = await load();
		await import('../src/wicket/upload.js');
		expect(topics.get('/ajax/call/before')).toHaveLength(1);
		document.body.innerHTML = '<input type="file" id="f"><a id="link"></a>';
		const ends = vi.fn();
		document.addEventListener(UPLOAD_END_EVENT, ends);
		const post = vi.fn();
		window.Wicket!.Ajax = { post };

		const xhr = fakeXhr();
		window.Wicket!.Timer = { f: { ajaxCallBackUrl: 'callback' } };
		upload.guard('f', { requests: true, allow: ['cb'] });
		upload.started('f', xhr);
		expect(UploadGuard.keepsOnPage()).toBe(true);
		const attrs: { c: string; u: string; pre?: unknown[] } = { c: 'link', u: 'other' };
		publish('/ajax/call/before', attrs);
		publish('/ajax/call/before', attrs);
		expect(attrs.pre).toHaveLength(1);
		expect(upload.precondition(attrs)).toBe(false);
		expect(upload.precondition({ u: 'cb' })).toBe(true);
		expect(upload.precondition({ u: 'callback' })).toBe(true);
		expect(upload.precondition({ c: 'link', antiliaUploadBypass: true })).toBe(true);

		xhr.dispatchEvent(new Event('abort'));
		xhr.dispatchEvent(new Event('loadend'));
		expect(ends).toHaveBeenCalledTimes(1);
		expect(ends.mock.calls[0][0].detail).toEqual({ source: 'f', outcome: 'canceled',
			message: undefined, status: undefined });
		expect(UploadGuard.blocking()).toBe(false);
		const later = { c: 'link' } as { c: string; pre?: unknown[] };
		publish('/ajax/call/before', later);
		expect(later.pre).toBeUndefined();

		upload.guard('f', null);
		upload.field('f', { uploadFailed: 'The upload failed.', uploadConnectionLost: 'Lost.' });
		const first = fakeXhr();
		const failing = fakeXhr();
		upload.started('f', first);
		expect(UploadGuard.keepsOnPage()).toBe(false);
		upload.started('f', failing);
		first.dispatchEvent(new Event('abort'));
		expect(ends).toHaveBeenCalledTimes(1);
		failing.dispatchEvent(new Event('error'));
		expect(ends.mock.calls[1][0].detail).toMatchObject({ outcome: 'failed', message: 'Lost.',
			status: 0 });

		// Wicket's handlers tell how a request ended, which its end alone does not
		const loading = fakeXhr();
		upload.started('f', loading);
		loading.dispatchEvent(new Event('loadend'));
		expect(ends).toHaveBeenCalledTimes(2);
		expect(post).not.toHaveBeenCalled();
		upload.ended('f', 'done');

		upload.started('f');
		upload.ended('f', 'failed', 'No room');
		upload.ended('f', 'done');
		expect(ends).toHaveBeenCalledTimes(4);
		expect(ends.mock.calls[3][0].detail.message).toBe('No room');
	});

	it('wraps the hooks of Wicket\'s field, which stay the page\'s', async () => {
		const upload = await load();
		document.body.innerHTML = '<input type="file" id="f">';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		const calls: string[] = [];
		const ends = vi.fn();
		document.addEventListener(UPLOAD_END_EVENT, ends);
		const starts = vi.fn();
		document.addEventListener('antilia-upload:start', starts);
		const instance = {
			ajaxCallBackUrl: 'cb',
			upload: vi.fn(),
			clientBeforeSendCallBack(xhr: XMLHttpRequest) {
				calls.push('before ' + (xhr ? 'xhr' : '') + ' ' + UploadGuard.keepsOnPage());
			},
			clientSideSuccessCallBack() {
				calls.push('success');
			},
			uploadErrorCallBack(error: { errorMessage?: unknown }) {
				calls.push('error ' + String(error.errorMessage));
			},
			clientSideCancelCallBack: vi.fn()
		};
		window.Wicket!.Timer = { f: instance };
		window.Wicket!.Ajax = { get: vi.fn() };
		upload.guard('f', { leaving: true });
		upload.field('f', { uploadFailed: 'Failed.' });
		upload.field('f');
		const cancel = instance.clientSideCancelCallBack;

		choose(input, file('a', 1));
		expect(upload.upload('f')).toBe(true);
		const xhr = fakeXhr();
		instance.clientBeforeSendCallBack(xhr);
		// the page's hook first, which may keep the request from going
		expect(calls).toEqual(['before xhr false']);
		expect(UploadGuard.keepsOnPage()).toBe(true);
		instance.clientSideSuccessCallBack();
		expect(calls[1]).toBe('success');
		expect(ends.mock.calls[0][0].detail.outcome).toBe('done');

		instance.clientBeforeSendCallBack(fakeXhr());
		const error = { error: true, errorMessage: 'uploadFailed' };
		instance.uploadErrorCallBack(error);
		expect(calls[3]).toBe('error uploadFailed');
		expect(ends.mock.calls[1][0].detail).toMatchObject({ outcome: 'failed', message: 'Failed.' });
		expect(instance.clientSideCancelCallBack).toBe(cancel);
		expect(starts).toHaveBeenCalledTimes(2);

		// a hook of the page saying false: Wicket's field sends nothing, and nothing starts
		const refusing = { upload: vi.fn(), clientBeforeSendCallBack: () => false };
		window.Wicket!.Timer = { h: refusing };
		upload.field('h');
		expect(refusing.clientBeforeSendCallBack()).toBe(false);
		expect(starts).toHaveBeenCalledTimes(2);

		// a field without hooks of its own
		const bare: Record<string, unknown> = { upload: vi.fn() };
		window.Wicket!.Timer = { g: bare };
		upload.field('g');
		(bare.clientBeforeSendCallBack as (xhr: XMLHttpRequest) => void)(fakeXhr());
		(bare.clientSideSuccessCallBack as () => void)();
		expect(ends.mock.calls[2][0].detail).toMatchObject({ source: 'g', outcome: 'done' });
		upload.field('missing');
	});

	it('starts the uploads of fields, but not one more, nor files breaking a limit', async () => {
		const upload = await load();
		document.body.innerHTML = '<input type="file" id="f" multiple>';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		new FilePicker(input, { fileCountMax: 1 });
		const send = vi.fn();
		const post = vi.fn();
		window.Wicket!.Timer = { f: { upload: send, ajaxCallBackUrl: 'cb' } };
		window.Wicket!.Ajax = { post };
		upload.field('f', { 'wicket.no.files.selected': 'Nothing to upload.' });
		const refused = vi.fn();
		document.addEventListener(UPLOAD_REFUSED_EVENT, refused);

		expect(upload.upload('missing')).toBe(false);
		expect(upload.upload('f')).toBe(false);
		expect(refused.mock.calls[0][0].detail).toEqual({ source: 'f',
			message: 'Nothing to upload.' });
		expect(post).toHaveBeenLastCalledWith({ u: 'cb', ep: { error: true,
			errorMessage: 'wicket.no.files.selected' } });

		choose(input, file('a', 1), file('b', 2));
		expect(upload.upload('f')).toBe(false);
		expect(refused.mock.calls[1][0].detail.message).toBe('2 files, more than the 1 sent at once');
		expect(post).toHaveBeenLastCalledWith({ u: 'cb', ep: { error: true,
			errorMessage: 'uploadTooManyFiles' } });
		expect(send).not.toHaveBeenCalled();

		choose(input, file('a', 1));
		// Wicket's field may refuse to send as well
		send.mockReturnValueOnce(false);
		expect(upload.upload('f')).toBe(false);
		expect(upload.upload('f')).toBe(true);
		expect(send).toHaveBeenCalledTimes(2);
		upload.started('f', fakeXhr());
		expect(upload.upload('f')).toBe(false);
		expect(send).toHaveBeenCalledTimes(2);
		upload.ended('f', 'done');
	});

	it('tells the progress the server\'s words of an error, or that no answer came', async () => {
		const upload = await load();
		document.body.innerHTML = '<input type="file" id="f">';
		const ends = vi.fn();
		document.addEventListener(UPLOAD_END_EVENT, ends);
		upload.field('f', { uploadTooManyFiles: 'Too many files.', uploadFailed: 'Failed.',
			uploadConnectionLost: 'Lost.' });
		const xhr = fakeXhr();
		const failure = (status: number, errorMessage: unknown) => {
			(xhr as { status: number }).status = status;
			upload.started('f', xhr);
			const error = { error: true, errorMessage };
			upload.failed('f', error);
			return error;
		};

		// what Wicket's field tells the server stays as it is
		expect(failure(200, 'uploadTooManyFiles').errorMessage).toBe('uploadTooManyFiles');
		expect(ends.mock.calls[0][0].detail).toMatchObject({ message: 'Too many files.',
			status: undefined });
		// an answer with an error: its status tells more
		expect(failure(500, 'uploadFailed').errorMessage).toBe('uploadFailed');
		expect(ends.mock.calls[1][0].detail).toMatchObject({ message: undefined, status: 500 });
		// no answer: the connection broke off
		expect(failure(0, 'uploadFailed').errorMessage).toBe('uploadFailed');
		expect(ends.mock.calls[2][0].detail).toMatchObject({ message: 'Lost.', status: 0 });
		// an error the server has no words for
		failure(200, { not: 'a key' });
		expect(ends.mock.calls[3][0].detail.message).toBe('Failed.');
		failure(200, 'unknown');
		expect(ends.mock.calls[4][0].detail.message).toBe('Failed.');

		upload.started('f');
		upload.failed('f');
		expect(ends.mock.calls[5][0].detail).toMatchObject({ outcome: 'failed', message: 'Failed.' });
		upload.failed('f', {});
		expect(ends).toHaveBeenCalledTimes(6);
	});

	it('tells the uploads of forms, sent by Ajax or posted', async () => {
		await load();
		document.body.innerHTML = '<form id="form" enctype="multipart/form-data">' +
			'<input type="file" id="f" multiple></form><form id="plain"></form>';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		const starts = vi.fn();
		const ends = vi.fn();
		document.addEventListener('antilia-upload:start', starts);
		document.addEventListener(UPLOAD_END_EVENT, ends);

		publish('/ajax/call/beforeSend', { mp: true, f: 'form' }, {});
		expect(starts).not.toHaveBeenCalled();
		input.files = [file('a', 1), file('b', 2)] as unknown as FileList;
		const xhr = new XMLHttpRequest();
		publish('/ajax/call/beforeSend', { mp: true, f: 'form' }, xhr);
		expect(starts.mock.calls[0][0].detail).toEqual({ source: 'form', xhr, total: 3,
			navigates: false });
		publish('/ajax/call/complete', { f: 'form' }, {}, 'success');
		expect(ends).not.toHaveBeenCalled();
		publish('/ajax/call/complete', { f: 'form' }, xhr, 'success');
		publish('/ajax/call/complete', { f: 'form' }, xhr, 'success');
		expect(ends).toHaveBeenCalledTimes(1);
		expect(ends.mock.calls[0][0].detail.outcome).toBe('done');

		// jQuery's object: its status tells why it failed
		const jquery = { status: 0, always: vi.fn() };
		publish('/ajax/call/beforeSend', { mp: true, f: 'form' }, jquery);
		expect(starts.mock.calls[1][0].detail.xhr).toBeUndefined();
		publish('/ajax/call/complete', { f: 'form' }, jquery, 'error');
		expect(ends.mock.calls[1][0].detail).toMatchObject({ outcome: 'failed', status: 0 });
		const answered = { status: 500 };
		publish('/ajax/call/beforeSend', { mp: true, f: 'form' }, answered);
		publish('/ajax/call/complete', { f: 'form' }, answered, 'error');
		expect(ends.mock.calls[2][0].detail).toMatchObject({ outcome: 'failed', status: 500 });

		// should Wicket never tell that a request is complete, its end ends the upload
		const silent = fakeXhr();
		publish('/ajax/call/beforeSend', { mp: true, f: 'form' }, silent);
		silent.dispatchEvent(new Event('loadend'));
		vi.advanceTimersByTime(2999);
		expect(ends).toHaveBeenCalledTimes(3);
		vi.advanceTimersByTime(1);
		expect(ends.mock.calls[3][0].detail.outcome).toBe('done');
		jquery.always.mock.calls[0][0]();
		vi.advanceTimersByTime(3000);
		expect(ends).toHaveBeenCalledTimes(4);
		const dropped = { status: 0, always: (callback: () => void) => callback() };
		publish('/ajax/call/beforeSend', { mp: true, f: 'form' }, dropped);
		vi.advanceTimersByTime(3000);
		expect(ends.mock.calls[4][0].detail).toMatchObject({ outcome: 'failed', status: 0 });

		const form = document.getElementById('form') as HTMLFormElement;
		form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
		expect(starts).toHaveBeenCalledTimes(6);
		expect(starts.mock.calls[5][0].detail).toEqual({ source: 'form', xhr: undefined, total: 3,
			navigates: true });
		form.addEventListener('submit', (event) => event.preventDefault(), { once: true });
		form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
		document.getElementById('plain')!.dispatchEvent(new Event('submit', { bubbles: true }));
		expect(starts).toHaveBeenCalledTimes(6);
	});

	it('sends no form whose files break a limit, by Ajax or posted', async () => {
		const upload = await load();
		document.body.innerHTML = '<form id="form" enctype="multipart/form-data">' +
			'<input type="file" id="f" multiple></form>' +
			'<form enctype="multipart/form-data"><input type="file" id="g"></form>';
		const input = settable(document.getElementById('f') as HTMLInputElement);
		new FilePicker(input, { fileMaxSize: 1, locale: 'en' });
		const other = settable(document.getElementById('g') as HTMLInputElement);
		new FilePicker(other, { fileMaxSize: 1 });
		const starts = vi.fn();
		const refused = vi.fn();
		document.addEventListener('antilia-upload:start', starts);
		document.addEventListener(UPLOAD_REFUSED_EVENT, refused);
		choose(input, file('a', 1));

		const attrs: { mp: boolean; f: string; pre?: Array<(attrs: object) => boolean> } =
			{ mp: true, f: 'form' };
		publish('/ajax/call/before', attrs);
		expect(attrs.pre).toHaveLength(1);
		expect(attrs.pre![0](attrs)).toBe(true);
		choose(input, file('a', 1), file('big', 2));
		expect(attrs.pre![0](attrs)).toBe(false);
		expect(refused.mock.calls[0][0].detail).toEqual({ source: 'form',
			message: 'big: larger than 1 byte, the most for a file' });
		expect(upload.precondition).not.toBe(attrs.pre![0]);

		const form = document.getElementById('form') as HTMLFormElement;
		const submit = new Event('submit', { bubbles: true, cancelable: true });
		form.dispatchEvent(submit);
		expect(submit.defaultPrevented).toBe(true);
		expect(refused).toHaveBeenCalledTimes(2);
		expect(starts).not.toHaveBeenCalled();

		// a form without an id goes nowhere either, telling no one
		choose(other, file('large', 2));
		const nameless = new Event('submit', { bubbles: true, cancelable: true });
		other.form!.dispatchEvent(nameless);
		expect(nameless.defaultPrevented).toBe(true);
		expect(refused).toHaveBeenCalledTimes(2);
	});
});
