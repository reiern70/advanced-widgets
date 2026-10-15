/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Uploads: a file picker drawn over an <input type="file">, the same in every browser and in the
 * colors of the theme; the progress of an upload, as a progress bar of the theme telling what was
 * sent, how fast and how long it still takes; and the guard keeping the user on the page, and
 * other requests waiting, while an upload runs.
 *
 * new FilePicker(input, options) hides the input, which stays in the page and holds the files, and
 * puts the picker right after it: a button of the theme opening the browser's dialog, or a zone the
 * files are dropped on, and the list of the files chosen. Files dropped on the picker go into the
 * input, which fires its 'input' and 'change' events, so a form, an Ajax behavior or the binding of
 * a framework gets them as if they were chosen in the dialog. An input taking several files keeps
 * those chosen before: files chosen in the dialog or dropped are added to them, and each can be
 * taken out of the list again. The picker tells at once when the files break the limits the server
 * sets (options maxSize, fileMaxSize and fileCountMax), so they are not sent only to be refused, and
 * lets nothing change while its files are being sent.
 *
 * new UploadProgress(element, options) draws the progress into an element the framework renders
 * empty, hidden while no upload runs. It follows the uploads of its source, the input or the form
 * whose files are sent, as uploadStarted(), uploadEnded() and uploadRefused() tell them: how far the server got
 * reading them, which it tells at statusUrl, else the progress events of the request, when the
 * start brings it. The server's count is what arrived; the request's counts what the browser
 * handed to the network, whose buffers may hold megabytes the server did not read yet.
 *
 * UploadGuard.begin() asks before the page is left until the upload ends and, if asked to, stops
 * the other requests of the page meanwhile: UploadGuard.blocks(request) tells the page's Ajax
 * engine whether to let a request go (Wicket: wicket/upload.ts). The requests of elements in one
 * marked data-antilia-upload-bypass, those marked to bypass and those to the URLs the upload
 * allows go on.
 */

import { buttonClasses, type ButtonSize, type ButtonVariant } from './button.js';
import { onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, toggleParts, type Look, type LookPart } from './look.js';

const SVG = 'http://www.w3.org/2000/svg';

/** An arrow going up out of a tray, for the button choosing files. */
const UPLOAD_ICON = 'M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z';

/** A cloud with an arrow going up into it, for the zone files are dropped on. */
const CLOUD_ICON = 'M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 ' +
	'10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 ' +
	'5 5h-3z';

/** An x, for the button clearing the files chosen. */
const CLEAR_ICON = 'M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 ' +
	'17.59 19 19 17.59 13.41 12z';

/** Fired at the element whose upload starts, see uploadStarted(). */
export const UPLOAD_START_EVENT = 'antilia-upload:start';

/** Fired at the element whose upload ended, see uploadEnded(). */
export const UPLOAD_END_EVENT = 'antilia-upload:end';

/** Fired at the element whose files were not sent, as they break a limit, see uploadRefused(). */
export const UPLOAD_REFUSED_EVENT = 'antilia-upload:refused';

/** Fired at the element whose request the guard stopped, or at the document. */
export const UPLOAD_BLOCKED_EVENT = 'antilia-upload:blocked';

/** Marks an element whose requests, and those of the elements in it, the guard lets go. */
export const UPLOAD_BYPASS_ATTRIBUTE = 'data-antilia-upload-bypass';

/** Marks, for a moment, an element whose request the guard stopped. */
export const UPLOAD_BLOCKED_ATTRIBUTE = 'data-antilia-upload-blocked';

/** How a file picker looks: a button, or a zone to drop files on with a button in it. */
export type FilePickerDesign = 'button' | 'drop-zone';

/** The designs of a file picker. */
export const FILE_PICKER_DESIGNS: readonly FilePickerDesign[] = ['button', 'drop-zone'];

/**
 * The texts of a file picker. '{0}' and '{1}' stand for the values each text tells of.
 */
export interface FilePickerTexts {
	/** The button of a picker of one file. */
	choose: string;
	/** The button of a picker of several files. */
	chooseMany: string;
	/** What the zone of a picker of one file says before its button. */
	drop: string;
	/** What the zone of a picker of several files says before its button. */
	dropMany: string;
	/** The button in the zone. */
	browse: string;
	/** What the picker says while no file is chosen. */
	none: string;
	/** What the picker says of the files chosen: their number and their size. */
	chosen: string;
	/** The button clearing the files chosen. */
	clear: string;
	/** What the picker says of dropped files the input does not accept: their names. */
	rejected: string;
	/** What the picker says of files larger than a file may be: their names and that size. */
	tooLarge: string;
	/** What the picker says of more files than are sent at once: their number and the most. */
	tooMany: string;
	/** What the picker says of files weighing more together than is sent at once: their size and the most. */
	tooMuch: string;
	/** What the picker says of several files of a name, where the server keeps one of each: the names. */
	sameName: string;
	/** The button taking a file out of the files chosen: its name. */
	remove: string;
}

/** The texts of a file picker when the page gives none. */
export const DEFAULT_FILE_PICKER_TEXTS: FilePickerTexts = {
	choose: 'Choose a file…',
	chooseMany: 'Choose files…',
	drop: 'Drop a file here or',
	dropMany: 'Drop files here or',
	browse: 'browse',
	none: 'No file chosen',
	chosen: '{0} files, {1}',
	clear: 'Clear',
	rejected: 'Not accepted: {0}',
	tooLarge: '{0}: larger than {1}, the most for a file',
	tooMany: '{0} files, more than the {1} sent at once',
	tooMuch: '{0} together, more than the {1} sent at once',
	sameName: '{0}: several files of this name, the server keeps one',
	remove: 'Remove {0}'
};

/**
 * How a file picker looks and what it says.
 */
export interface FilePickerOptions {
	/** A button, the default, or a zone to drop files on. */
	design?: FilePickerDesign;
	/** The variant of the button choosing files, 'secondary' by default ('ghost' in a zone). */
	variant?: ButtonVariant;
	/** The size of the button choosing files, 'medium' by default ('small' in a zone). */
	size?: ButtonSize;
	/** The texts, those of DEFAULT_FILE_PICKER_TEXTS where left out. */
	texts?: Partial<FilePickerTexts>;
	/** The language the sizes are written in, that of the page by default. */
	locale?: string;
	/** The look of the picker; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/** The most the files may weigh together, in bytes; no limit when left out. */
	maxSize?: number;
	/** The most a file may weigh, in bytes; no limit when left out. */
	fileMaxSize?: number;
	/** The most files sent at once; no limit when left out. */
	fileCountMax?: number;
	/**
	 * Whether the server keeps one file of each name, as a folder does, so that several of a name
	 * are not sent; false by default.
	 */
	uniqueNames?: boolean;
}

/** Which limit the files chosen break. */
export type UploadProblemKind = 'tooLarge' | 'tooMany' | 'tooMuch' | 'sameName';

/** A limit the files chosen break, which keeps them from being sent. */
export interface UploadProblem {
	kind: UploadProblemKind;
	/** What the picker says of it. */
	message: string;
}

/**
 * @returns the text with '{0}', '{1}', ... replaced by the values
 */
export function format(text: string, ...values: unknown[]): string {
	return text.replace(/\{(\d+)\}/g, (match, index: string) =>
		Number(index) < values.length ? String(values[Number(index)]) : match);
}

const BYTE_UNITS = ['byte', 'kilobyte', 'megabyte', 'gigabyte', 'terabyte'] as const;

/**
 * @param bytes a number of bytes
 * @param locale the language, that of the page by default
 * @returns the bytes in the largest unit under which they are at least one, 1024 of a unit making
 * the next one, with at most one decimal: '512 byte', '1.5 kB', '12 MB'
 */
export function formatBytes(bytes: number, locale?: string): string {
	let value = Math.max(0, bytes);
	let unit = 0;
	while (value >= 1024 && unit < BYTE_UNITS.length - 1) {
		value /= 1024;
		unit++;
	}
	return new Intl.NumberFormat(locale, { style: 'unit', unit: BYTE_UNITS[unit],
		unitDisplay: 'short', maximumFractionDigits: unit === 0 ? 0 : 1 }).format(value);
}

/**
 * @param seconds a duration
 * @param locale the language, that of the page by default
 * @returns the duration in whole seconds under a minute, else in minutes and seconds: '45 sec',
 * '2 min, 5 sec'
 */
export function formatDuration(seconds: number, locale?: string): string {
	const whole = Math.max(0, Math.ceil(seconds));
	const unit = (value: number, name: 'second' | 'minute' | 'hour') => new Intl.NumberFormat(
		locale, { style: 'unit', unit: name, unitDisplay: 'short' }).format(value);
	if (whole < 60) {
		return unit(whole, 'second');
	}
	if (whole < 3600) {
		const rest = whole % 60;
		return rest ? `${unit(Math.floor(whole / 60), 'minute')}, ${unit(rest, 'second')}`
			: unit(whole / 60, 'minute');
	}
	const minutes = Math.floor((whole % 3600) / 60);
	return minutes ? `${unit(Math.floor(whole / 3600), 'hour')}, ${unit(minutes, 'minute')}`
		: unit(Math.floor(whole / 3600), 'hour');
}

/**
 * @param accept what an input accepts, as its accept attribute says: file name extensions such as
 * '.pdf', MIME types such as 'image/png' and groups of them such as 'image/*', separated by commas
 * @param file the file
 * @returns whether the file is one of them; any file when accept is empty
 */
export function accepts(accept: string | null | undefined, file: { name: string; type: string }):
	boolean {
	const patterns = (accept ?? '').split(',').map((pattern) => pattern.trim().toLowerCase())
		.filter(Boolean);
	if (!patterns.length) {
		return true;
	}
	const name = file.name.toLowerCase();
	const type = (file.type || '').toLowerCase();
	return patterns.some((pattern) => {
		if (pattern.startsWith('.')) {
			return name.endsWith(pattern);
		}
		if (pattern.endsWith('/*')) {
			return type.startsWith(pattern.slice(0, -1));
		}
		return type === pattern;
	});
}

function create<K extends keyof HTMLElementTagNameMap>(tag: K, look: Look, part: LookPart,
	text?: string): HTMLElementTagNameMap[K] {
	const element = document.createElement(tag);
	element.className = partClasses(look, part);
	if (text !== undefined) {
		element.textContent = text;
	}
	return element;
}

function icon(path: string, classes: string): SVGSVGElement {
	const svg = document.createElementNS(SVG, 'svg');
	svg.setAttribute('class', classes);
	svg.setAttribute('viewBox', '0 0 24 24');
	svg.setAttribute('aria-hidden', 'true');
	svg.setAttribute('focusable', 'false');
	const shape = document.createElementNS(SVG, 'path');
	shape.setAttribute('fill', 'currentColor');
	shape.setAttribute('d', path);
	svg.append(shape);
	return svg;
}

/**
 * @returns a button of the theme with an icon, a text or both
 */
function button(look: Look, part: LookPart, variant: ButtonVariant, size: ButtonSize,
	iconPath: string | null, text: string, iconOnly = false): HTMLButtonElement {
	const element = document.createElement('button');
	element.type = 'button';
	element.className = [buttonClasses({ variant, size, iconOnly, look }), partClasses(look, part)]
		.filter(Boolean).join(' ');
	if (iconPath) {
		const box = create('span', look, 'button-icon');
		box.setAttribute('aria-hidden', 'true');
		box.append(icon(iconPath, ''));
		element.append(box);
	}
	if (iconOnly) {
		element.setAttribute('aria-label', text);
		element.title = text;
	} else {
		element.append(create('span', look, 'button-label', text));
	}
	return element;
}

/**
 * @returns whether a drag carries files, rather than text or a link
 */
function carriesFiles(event: DragEvent): boolean {
	const types = event.dataTransfer?.types;
	return !!types && Array.from(types).includes('Files');
}

/**
 * Puts files into a file input, as if they were chosen in the browser's dialog, and fires its
 * 'input' and 'change' events.
 *
 * @returns whether the browser let the files in
 */
export function setFiles(input: HTMLInputElement, files: readonly File[]): boolean {
	if (typeof DataTransfer !== 'function') {
		return false;
	}
	const transfer = new DataTransfer();
	files.forEach((file) => transfer.items.add(file));
	try {
		input.files = transfer.files;
	} catch {
		return false;
	}
	input.dispatchEvent(new Event('input', { bubbles: true }));
	input.dispatchEvent(new Event('change', { bubbles: true }));
	return true;
}

/**
 * @returns whether two files are the same file, chosen or dropped twice
 */
function sameFile(a: File, b: File): boolean {
	return a === b || (a.name === b.name && a.size === b.size && a.lastModified === b.lastModified);
}

/**
 * @returns the files followed by those added that are not among them yet
 */
function merge(files: readonly File[], added: readonly File[]): File[] {
	const merged = [...files];
	added.forEach((file) => {
		if (!merged.some((other) => sameFile(other, file))) {
			merged.push(file);
		}
	});
	return merged;
}

/** The pickers by their input. */
const pickers = new WeakMap<HTMLInputElement, FilePicker>();

let counter = 0;

function resolveInput(input: HTMLInputElement | string | null | undefined): HTMLInputElement | null {
	const element = typeof input === 'string' ? document.getElementById(input) : input;
	return element instanceof HTMLInputElement && element.type === 'file' ? element : null;
}

/**
 * The picker of a file input.
 */
export class FilePicker {
	readonly input: HTMLInputElement;

	options: FilePickerOptions;

	/** What the picker added after the input. */
	element!: HTMLDivElement;

	/** The button opening the browser's dialog. */
	button!: HTMLButtonElement;

	/** The zone files are dropped on, in the drop-zone design. */
	zone: HTMLDivElement | null = null;

	/** What the picker says of the files chosen. */
	summary!: HTMLSpanElement;

	/** The button clearing the files chosen. */
	clearButton!: HTMLButtonElement;

	/** The list of the files chosen. */
	list!: HTMLUListElement;

	/** What keeps the files chosen from being sent, see problems(). */
	error!: HTMLDivElement;

	/** Whether the files of the input are being sent: nothing changes them meanwhile. */
	busy = false;

	private look!: Look;

	private texts!: FilePickerTexts;

	/** The drags over the picker, which enter and leave its children too. */
	private dragging = 0;

	/** The names of the dropped files last rejected, said until files are chosen again. */
	private rejected: string[] = [];

	/** The files the input held after its last change, which a choice in the dialog adds to. */
	private chosen: File[] = [];

	private listeners: Array<[EventTarget, string, EventListener, boolean]> = [];

	private stopWatchingRemoval: Unsubscribe | null = null;

	/**
	 * @param input the file input
	 * @param options how the picker looks and what it says
	 */
	constructor(input: HTMLInputElement, options?: FilePickerOptions) {
		this.input = input;
		this.options = { ...options };
		pickers.get(input)?.destroy();
		pickers.set(input, this);
		this.attach();
	}

	/**
	 * Draws the picker of a file input.
	 *
	 * @param input the input or its id
	 * @param options how the picker looks and what it says
	 * @returns the picker, null when there is no such file input
	 */
	static attach(input: HTMLInputElement | string, options?: FilePickerOptions): FilePicker | null {
		const element = resolveInput(input);
		return element ? new FilePicker(element, options) : null;
	}

	/**
	 * @param input the input or its id
	 * @returns the picker of a file input, if it has one
	 */
	static of(input: HTMLInputElement | string | null | undefined): FilePicker | null {
		const element = resolveInput(input);
		return (element && pickers.get(element)) ?? null;
	}

	/** Whether the input takes several files. */
	get multiple(): boolean {
		return this.input.multiple;
	}

	/** The files the input holds. */
	get files(): File[] {
		return Array.from(this.input.files ?? []);
	}

	private attach(): void {
		const input = this.input;
		const design = this.options.design ?? 'button';
		this.look = this.options.look ?? lookOf(input);
		this.texts = { ...DEFAULT_FILE_PICKER_TEXTS, ...this.options.texts };
		const look = this.look;
		const id = input.id || ('antilia-file-picker-' + (++counter));

		input.classList.add('antilia-file-picker-native');
		input.tabIndex = -1;
		input.setAttribute('aria-hidden', 'true');

		this.element = create('div', look, 'file-picker');
		this.element.classList.add('antilia-file-picker-' + design);
		const choose = this.multiple ? this.texts.chooseMany : this.texts.choose;
		this.summary = create('span', look, 'file-picker-summary');
		this.summary.id = id + '-summary';
		this.summary.setAttribute('aria-live', 'polite');
		this.clearButton = button(look, 'file-picker-clear', 'ghost', 'small', CLEAR_ICON,
			this.texts.clear, true);
		this.list = create('ul', look, 'file-picker-files');
		this.error = create('div', look, 'file-picker-error');
		this.error.id = id + '-error';
		this.error.setAttribute('role', 'alert');

		if (design === 'drop-zone') {
			this.zone = create('div', look, 'file-picker-zone');
			this.zone.append(icon(CLOUD_ICON, partClasses(look, 'file-picker-icon')));
			const prompt = create('span', look, 'file-picker-prompt',
				(this.multiple ? this.texts.dropMany : this.texts.drop) + ' ');
			this.button = button(look, 'file-picker-choose', this.options.variant ?? 'ghost',
				this.options.size ?? 'small', null, this.texts.browse);
			this.button.setAttribute('aria-label', choose);
			prompt.append(this.button);
			const status = document.createElement('span');
			status.className = 'antilia-file-picker-status';
			status.append(this.summary, this.clearButton);
			this.zone.append(prompt, status);
			this.element.append(this.zone, this.list, this.error);
		} else {
			this.button = button(look, 'file-picker-choose', this.options.variant ?? 'secondary',
				this.options.size ?? 'medium', UPLOAD_ICON, choose);
			this.element.append(this.button, this.summary, this.clearButton, this.list, this.error);
		}
		this.button.setAttribute('aria-describedby', this.summary.id + ' ' + this.error.id);
		input.after(this.element);

		this.listen(this.button, 'click', () => this.open());
		this.listen(this.clearButton, 'click', () => {
			this.clear();
			// the clear button hides: the focus goes to the button choosing files
			this.button.focus();
		});
		// before the listeners of the page hear of a choice in the dialog, which replaced the
		// files the input held
		this.listen(document, 'input', (event) => this.onChoice(event), true);
		this.listen(document, 'change', (event) => this.onChoice(event), true);
		this.listen(input, 'change', () => {
			this.rejected = [];
			this.chosen = this.files;
			this.render();
		});
		if (this.zone) {
			this.listen(this.zone, 'click', (event) => {
				if (!(event.target as Element).closest('button')) {
					this.open();
				}
			});
		}
		this.listen(this.element, 'dragenter', (event) => this.onDrag(event as DragEvent, 1));
		this.listen(this.element, 'dragover', (event) => this.onDrag(event as DragEvent, 0));
		this.listen(this.element, 'dragleave', (event) => this.onDrag(event as DragEvent, -1));
		this.listen(this.element, 'drop', (event) => this.onDrop(event as DragEvent));
		this.listen(document, UPLOAD_START_EVENT, (event) => this.onUpload(event, true));
		this.listen(document, UPLOAD_END_EVENT, (event) => this.onUpload(event, false));
		this.chosen = this.files;
		this.render();
		this.stopWatchingRemoval = onRemoving(input, () => this.destroy());
	}

	private listen(target: EventTarget, type: string, listener: EventListener, capture = false):
		void {
		target.addEventListener(type, listener, capture);
		this.listeners.push([target, type, listener, capture]);
	}

	/**
	 * Takes other texts, another design or another look.
	 *
	 * @param options how the picker looks and what it says
	 */
	configure(options?: FilePickerOptions): void {
		this.detach();
		this.element.remove();
		this.options = { ...options };
		pickers.set(this.input, this);
		this.attach();
	}

	/**
	 * Opens the browser's dialog choosing files, unless the input is disabled or its files are
	 * being sent.
	 */
	open(): void {
		if (!this.input.disabled && !this.busy) {
			this.input.click();
		}
	}

	/**
	 * Clears the files chosen, firing the input's 'input' and 'change' events.
	 */
	clear(): void {
		if (!this.input.files?.length && !this.input.value) {
			return;
		}
		this.input.value = '';
		this.input.dispatchEvent(new Event('input', { bubbles: true }));
		this.input.dispatchEvent(new Event('change', { bubbles: true }));
	}

	/**
	 * Takes a file out of the files chosen, firing the input's 'input' and 'change' events.
	 *
	 * @param index the place of the file among the files chosen
	 * @returns whether a file went
	 */
	remove(index: number): boolean {
		const files = this.files;
		if (index < 0 || index >= files.length) {
			return false;
		}
		files.splice(index, 1);
		if (!files.length) {
			this.clear();
			return true;
		}
		return setFiles(this.input, files);
	}

	/**
	 * @returns the limits the files chosen break, which keep them from being sent: a file larger
	 * than fileMaxSize, more files than fileCountMax, more than maxSize together, several files of
	 * a name where the server keeps one of each (uniqueNames)
	 */
	problems(): UploadProblem[] {
		const { maxSize, fileMaxSize, fileCountMax, uniqueNames, locale } = this.options;
		const files = this.files;
		const texts = this.texts;
		const problems: UploadProblem[] = [];
		const large = fileMaxSize !== undefined && fileMaxSize >= 0
			? files.filter((file) => file.size > fileMaxSize) : [];
		if (large.length) {
			problems.push({ kind: 'tooLarge', message: format(texts.tooLarge,
				large.map((file) => file.name).join(', '), formatBytes(fileMaxSize!, locale)) });
		}
		if (fileCountMax !== undefined && fileCountMax >= 0 && files.length > fileCountMax) {
			problems.push({ kind: 'tooMany', message: format(texts.tooMany, files.length,
				fileCountMax) });
		}
		const size = files.reduce((sum, file) => sum + file.size, 0);
		if (maxSize !== undefined && maxSize >= 0 && size > maxSize) {
			problems.push({ kind: 'tooMuch', message: format(texts.tooMuch,
				formatBytes(size, locale), formatBytes(maxSize, locale)) });
		}
		const names = files.map((file) => file.name);
		const repeated = uniqueNames
			? names.filter((name, index) => names.indexOf(name) !== index) : [];
		if (repeated.length) {
			problems.push({ kind: 'sameName', message: format(texts.sameName,
				Array.from(new Set(repeated)).join(', ')) });
		}
		return problems;
	}

	/**
	 * Draws the files the input holds, what keeps them from being sent, and whether it is disabled,
	 * again.
	 */
	render(): void {
		const files = this.files;
		const disabled = this.input.disabled || this.busy;
		const locale = this.options.locale;
		this.button.disabled = disabled;
		this.clearButton.disabled = disabled;
		this.element.classList.toggle('antilia-file-picker-disabled', disabled);
		this.element.toggleAttribute('aria-busy', this.busy);
		this.zone?.setAttribute('aria-disabled', String(disabled));

		const texts = this.texts;
		let summary: string;
		if (!files.length) {
			summary = texts.none;
		} else if (files.length === 1) {
			summary = `${files[0].name} (${formatBytes(files[0].size, locale)})`;
		} else {
			summary = format(texts.chosen, files.length,
				formatBytes(files.reduce((sum, file) => sum + file.size, 0), locale));
		}
		if (this.rejected.length) {
			summary += ' · ' + format(texts.rejected, this.rejected.join(', '));
		}
		this.summary.textContent = summary;
		this.clearButton.hidden = !files.length;

		const fileMaxSize = this.options.fileMaxSize;
		this.list.replaceChildren(...(files.length > 1 ? files : []).map((file, index) => {
			const item = create('li', this.look, 'file-picker-file');
			toggleParts(item, this.look, fileMaxSize !== undefined && fileMaxSize >= 0 &&
				file.size > fileMaxSize, 'file-picker-file-invalid');
			const remove = button(this.look, 'file-picker-remove', 'ghost', 'small', CLEAR_ICON,
				format(texts.remove, file.name), true);
			remove.disabled = disabled;
			remove.addEventListener('click', () => this.onRemove(index));
			item.append(create('span', this.look, 'file-picker-name', file.name),
				create('span', this.look, 'file-picker-size', formatBytes(file.size, locale)), remove);
			return item;
		}));
		this.list.hidden = files.length < 2;

		const problems = this.problems();
		this.error.textContent = problems.map((problem) => problem.message).join(' · ');
		this.error.hidden = !problems.length;
		this.element.classList.toggle('antilia-file-picker-invalid', problems.length > 0);
		if (this.zone) {
			toggleParts(this.zone, this.look, problems.length > 0, 'file-picker-zone-invalid');
		}
	}

	/**
	 * Takes a file out of the list, the focus going to the button of the next file, or to the
	 * button choosing files once the list is gone.
	 */
	private onRemove(index: number): void {
		this.remove(index);
		const buttons = Array.from(this.list.querySelectorAll('button'));
		(this.list.hidden || !buttons.length ? this.button
			: buttons[Math.min(index, buttons.length - 1)]).focus();
	}

	/**
	 * A choice in the browser's dialog replaced the files of the input: in an input taking several
	 * files, the files chosen before stay, those chosen are added, and the page hears of all of
	 * them instead.
	 */
	private onChoice(event: Event): void {
		if (!event.isTrusted || event.target !== this.input || !this.multiple ||
			!this.chosen.length || typeof DataTransfer !== 'function') {
			return;
		}
		event.stopImmediatePropagation();
		if (event.type === 'change') {
			setFiles(this.input, merge(this.chosen, this.files));
		}
	}

	/**
	 * Locks the picker while the files of its input, or of its form, are being sent; a form posted
	 * the usual way leaves the page, which may not happen.
	 */
	private onUpload(event: Event, busy: boolean): void {
		const detail = (event as CustomEvent<UploadStartDetail>).detail;
		if (detail?.source && !detail.navigates &&
			(detail.source === this.input.id || detail.source === this.input.form?.id)) {
			this.busy = busy;
			this.render();
		}
	}

	private onDrag(event: DragEvent, step: number): void {
		if (!carriesFiles(event)) {
			return;
		}
		event.preventDefault();
		const disabled = this.input.disabled || this.busy;
		if (event.dataTransfer) {
			event.dataTransfer.dropEffect = disabled ? 'none' : 'copy';
		}
		this.dragging = Math.max(0, this.dragging + step);
		this.setOver(this.dragging > 0 && !disabled);
	}

	private setOver(over: boolean): void {
		this.element.classList.toggle('antilia-file-picker-over', over);
		if (this.zone) {
			toggleParts(this.zone, this.look, over, 'file-picker-zone-over');
		}
	}

	private onDrop(event: DragEvent): void {
		if (!carriesFiles(event)) {
			return;
		}
		event.preventDefault();
		this.dragging = 0;
		this.setOver(false);
		if (this.input.disabled || this.busy) {
			return;
		}
		this.drop(Array.from(event.dataTransfer?.files ?? []));
	}

	/**
	 * Puts dropped files into the input: those it accepts, added to the files chosen before, or the
	 * first of them only, instead, unless it takes several. The picker tells the names of the others.
	 *
	 * @param files the files dropped
	 * @returns whether files went into the input
	 */
	drop(files: readonly File[]): boolean {
		if (this.busy) {
			return false;
		}
		const accept = this.input.getAttribute('accept');
		const accepted = files.filter((file) => accepts(accept, file));
		const taken = this.multiple ? accepted : accepted.slice(0, 1);
		const rejected = files.filter((file) => !taken.includes(file)).map((file) => file.name);
		const set = taken.length > 0 &&
			setFiles(this.input, this.multiple ? merge(this.files, taken) : taken);
		this.rejected = rejected;
		this.render();
		return set;
	}

	private detach(): void {
		this.listeners.forEach(([target, type, listener, capture]) =>
			target.removeEventListener(type, listener, capture));
		this.listeners = [];
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
	}

	/**
	 * Takes the picker away and shows the native input again.
	 */
	destroy(): void {
		this.detach();
		this.element.remove();
		const input = this.input;
		input.classList.remove('antilia-file-picker-native');
		if (!input.classList.length) {
			input.removeAttribute('class');
		}
		input.removeAttribute('tabindex');
		input.removeAttribute('aria-hidden');
		if (pickers.get(input) === this) {
			pickers.delete(input);
		}
	}
}

/**
 * A file picker as a Svelte action, or for any page: filePicker(input, options).
 *
 * @returns update() taking other options, destroy() taking the picker away
 */
export function filePicker(input: HTMLInputElement, options?: FilePickerOptions): {
	update(options?: FilePickerOptions): void;
	destroy(): void;
} {
	const instance = new FilePicker(input, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}

/** How an upload ended. */
export type UploadOutcome = 'done' | 'canceled' | 'failed';

/** What uploadStarted() tells. */
export interface UploadStartDetail {
	/** The id of the input or the form whose files are sent. */
	source: string;
	/** The request sending them, whose progress events tell how far it got, if known. */
	xhr?: XMLHttpRequest;
	/** The bytes of the files sent, if known. */
	total?: number;
	/** Whether the page goes with the upload: a form posted the usual way. */
	navigates?: boolean;
}

/** What uploadEnded() tells. */
export interface UploadEndDetail {
	/** The id of the input or the form whose files were sent. */
	source: string;
	outcome: UploadOutcome;
	/** Why it failed, as the server or the browser tells. */
	message?: string;
	/** The HTTP status the server answered a failed upload with, 0 when no answer came. */
	status?: number;
}

/** What uploadRefused() tells. */
export interface UploadRefusedDetail {
	/** The id of the input or the form whose files were not sent. */
	source: string;
	/** Why. */
	message?: string;
}

function fire(type: string, source: string, detail: object): void {
	const target = document.getElementById(source) ?? document;
	target.dispatchEvent(new CustomEvent(type, { bubbles: true, detail: { ...detail, source } }));
}

/**
 * @returns the bytes of the files an element sends: those of a file input, or of the file inputs
 * in a form
 */
export function filesSize(element: Element | null): number {
	const inputs = element instanceof HTMLInputElement ? [element]
		: Array.from(element?.querySelectorAll('input[type="file"]') ?? []) as HTMLInputElement[];
	return inputs.flatMap((input) => Array.from(input.files ?? []))
		.reduce((sum, file) => sum + file.size, 0);
}

/**
 * Tells that the files of an input or a form are being sent, firing UPLOAD_START_EVENT at it.
 *
 * @param source the id of the input or the form
 * @param xhr the request sending them, if known
 * @param total the bytes of the files sent; those the element holds when left out
 * @param navigates whether the page goes with the upload: a form posted the usual way
 */
export function uploadStarted(source: string, xhr?: XMLHttpRequest, total?: number,
	navigates = false): void {
	fire(UPLOAD_START_EVENT, source, { xhr,
		total: total ?? filesSize(document.getElementById(source)), navigates });
}

/**
 * Tells that the files of an input or a form were sent, or not, firing UPLOAD_END_EVENT at it.
 *
 * @param source the id of the input or the form
 * @param outcome how the upload ended
 * @param message why it failed
 * @param status the HTTP status the server answered a failed upload with, 0 when no answer came
 */
export function uploadEnded(source: string, outcome: UploadOutcome, message?: string,
	status?: number): void {
	fire(UPLOAD_END_EVENT, source, { outcome, message, status });
}

/**
 * Tells that the files of an input or a form were not sent, as they break a limit, firing
 * UPLOAD_REFUSED_EVENT at it.
 *
 * @param source the id of the input or the form
 * @param message why
 */
export function uploadRefused(source: string, message?: string): void {
	fire(UPLOAD_REFUSED_EVENT, source, { message });
}

/**
 * @returns the limits the files of an input, or of the inputs in a form, break, as their pickers
 * tell (see FilePicker.problems())
 */
export function uploadProblems(element: Element | null): UploadProblem[] {
	const inputs = element instanceof HTMLInputElement ? [element]
		: Array.from(element?.querySelectorAll('input[type="file"]') ?? []) as HTMLInputElement[];
	return inputs.flatMap((input) => FilePicker.of(input)?.problems() ?? []);
}

/**
 * The texts of the progress of an upload. '{0}' and '{1}' stand for sizes or durations.
 */
export interface UploadProgressTexts {
	/** While the upload starts and nothing is known yet. */
	starting: string;
	/** What was sent of how much. */
	sent: string;
	/** How fast it goes: a size per second. */
	rate: string;
	/** How long it still takes. */
	remaining: string;
	/** Once all was sent, while the server takes the files. */
	processing: string;
	/** Once the upload ended well: its size. */
	done: string;
	/** Once the user canceled the upload. */
	canceled: string;
	/** Once the upload failed, unless the server tells why. */
	failed: string;
	/** Once the upload failed without an answer from the server. */
	lost: string;
	/** Once the server answered the upload with an error: its HTTP status. */
	serverError: string;
	/** For a moment after a request the guard stopped. */
	blocked: string;
}

/** The texts of the progress of an upload when the page gives none. */
export const DEFAULT_UPLOAD_PROGRESS_TEXTS: UploadProgressTexts = {
	starting: 'Starting the upload…',
	sent: '{0} of {1}',
	rate: '{0}/s',
	remaining: '{0} left',
	processing: 'Sent {0}, the server takes the files…',
	done: 'Uploaded {0}',
	canceled: 'Upload canceled',
	failed: 'The upload failed',
	lost: 'The connection to the server broke off',
	serverError: 'The server could not take the files ({0})',
	blocked: 'Wait for the upload to end: the request was stopped'
};

/**
 * Whose uploads a progress shows and how it learns how far they got.
 */
export interface UploadProgressOptions {
	/** The id of the input or the form whose uploads it shows. */
	source?: string;
	/**
	 * Where the server tells how far it got reading the upload, as JSON with bytesUploaded and
	 * totalBytes, asked for every pollMillis while it runs; without it, the progress follows the
	 * progress events of the request.
	 */
	statusUrl?: string;
	/** How often to ask the server, 500 milliseconds by default. */
	pollMillis?: number;
	/** How long to wait for an answer of the server, 10 seconds by default. */
	pollTimeoutMillis?: number;
	/** The texts, those of DEFAULT_UPLOAD_PROGRESS_TEXTS where left out. */
	texts?: Partial<UploadProgressTexts>;
	/** The language the sizes and durations are written in, that of the page by default. */
	locale?: string;
	/** The look of the progress; that of the element it is in when left out. */
	look?: Look;
}

/** What the server tells of an upload. */
interface UploadStatus {
	bytesUploaded?: number;
	totalBytes?: number;
}

/** The progresses by their element. */
const progresses = new WeakMap<Element, UploadProgress>();

/**
 * The progress of the uploads of an input or a form, drawn into an element.
 */
export class UploadProgress {
	readonly host: HTMLElement;

	options: UploadProgressOptions;

	/** The box of the bar and the status, hidden while no upload runs. */
	box!: HTMLDivElement;

	/** The bar's <progress>. */
	value!: HTMLProgressElement;

	/** The bar's label. */
	label!: HTMLSpanElement;

	/** What was sent, how fast and how long it still takes. */
	status!: HTMLDivElement;

	/** Whether an upload runs. */
	running = false;

	private look!: Look;

	private texts!: UploadProgressTexts;

	private started = 0;

	private loaded = 0;

	private total = 0;

	/** The bytes of the files sent, as the start told. */
	private expected = 0;

	/** Whether the server told how far it got reading the upload running. */
	private reported = false;

	/** Whether the page goes with the upload running: a form posted the usual way. */
	private navigates = false;

	private timer: number | undefined = undefined;

	/** The status told for a moment instead of the progress, such as a request stopped. */
	private note: string | null = null;

	private noteTimer: number | undefined = undefined;

	private listeners: Array<[EventTarget, string, EventListener]> = [];

	private stopWatchingRemoval: Unsubscribe | null = null;

	/**
	 * @param host the element to draw into, which the framework renders empty
	 * @param options whose uploads it shows and how it learns how far they got
	 */
	constructor(host: HTMLElement, options?: UploadProgressOptions) {
		this.host = host;
		this.options = { ...options };
		progresses.get(host)?.destroy();
		progresses.set(host, this);
		this.attach();
	}

	/**
	 * Draws the progress of uploads into an element.
	 *
	 * @param host the element or its id
	 * @param options whose uploads it shows and how it learns how far they got
	 * @returns the progress, null when there is no such element
	 */
	static attach(host: HTMLElement | string, options?: UploadProgressOptions):
		UploadProgress | null {
		const element = typeof host === 'string' ? document.getElementById(host) : host;
		return element ? new UploadProgress(element, options) : null;
	}

	/**
	 * @param host the element or its id
	 * @returns the progress drawn into an element, if any
	 */
	static of(host: HTMLElement | string | null | undefined): UploadProgress | null {
		const element = typeof host === 'string' ? document.getElementById(host) : host;
		return (element && progresses.get(element)) ?? null;
	}

	private attach(): void {
		const look = this.look = this.options.look ?? lookOf(this.host);
		this.texts = { ...DEFAULT_UPLOAD_PROGRESS_TEXTS, ...this.options.texts };
		this.box = create('div', look, 'upload-progress');
		this.box.hidden = true;
		const bar = create('div', look, 'progress');
		this.value = create('progress', look, 'progress-value');
		this.value.max = 100;
		this.label = create('span', look, 'progress-label');
		this.label.setAttribute('aria-hidden', 'true');
		bar.append(this.value, this.label);
		this.status = create('div', look, 'upload-status');
		this.status.setAttribute('role', 'status');
		this.box.append(bar, this.status);
		this.host.append(this.box);

		this.listen(document, UPLOAD_START_EVENT, (event) => {
			const detail = (event as CustomEvent<UploadStartDetail>).detail;
			if (detail?.source === this.options.source) {
				this.start(detail.xhr, detail.total, detail.navigates);
			}
		});
		this.listen(document, UPLOAD_END_EVENT, (event) => {
			const detail = (event as CustomEvent<UploadEndDetail>).detail;
			if (detail?.source === this.options.source) {
				this.end(detail.outcome, detail.message, detail.status);
			}
		});
		this.listen(document, UPLOAD_REFUSED_EVENT, (event) => {
			const detail = (event as CustomEvent<UploadRefusedDetail>).detail;
			if (detail?.source === this.options.source && !this.running) {
				this.loaded = 0;
				this.total = 0;
				this.fail(detail.message);
			}
		});
		// back to a page left by posting a form: the upload went with it
		this.listen(window, 'pageshow', (event) => {
			if ((event as PageTransitionEvent).persisted && this.running && this.navigates) {
				this.running = false;
				this.stopPolling();
				this.box.hidden = true;
			}
		});
		this.listen(document, UPLOAD_BLOCKED_EVENT, () => {
			if (this.running) {
				this.tell(this.texts.blocked);
			}
		});
		this.stopWatchingRemoval = onRemoving(this.host, () => this.destroy());
	}

	private listen(target: EventTarget, type: string, listener: EventListener): void {
		target.addEventListener(type, listener);
		this.listeners.push([target, type, listener]);
	}

	/**
	 * Shows that an upload starts, its progress unknown until the server or the request tells it.
	 *
	 * @param xhr the request sending the files, whose progress events it follows unless it asks the
	 * server at statusUrl
	 * @param total the bytes of the files sent, if known
	 * @param navigates whether the page goes with the upload: a form posted the usual way
	 */
	start(xhr?: XMLHttpRequest, total?: number, navigates = false): void {
		this.stopPolling();
		this.running = true;
		this.started = Date.now();
		this.loaded = 0;
		this.total = 0;
		this.expected = total ?? 0;
		this.reported = false;
		this.navigates = navigates;
		this.note = null;
		this.box.hidden = false;
		this.setState(null);
		this.draw();
		if (this.options.statusUrl) {
			this.poll();
		} else if (xhr?.upload) {
			this.track(xhr);
		}
	}

	/**
	 * Follows the progress events of a request sending files.
	 */
	track(xhr: XMLHttpRequest): void {
		xhr.upload.addEventListener('progress', (event) => {
			if (this.running && event.lengthComputable) {
				this.update(event.loaded, event.total);
			}
		});
	}

	private poll(): void {
		const url = this.options.statusUrl;
		if (!url || !this.running) {
			return;
		}
		this.timer = window.setTimeout(() => {
			// an answer that does not come stops no polling
			const abort = new AbortController();
			const timeout = window.setTimeout(() => abort.abort(),
				this.options.pollTimeoutMillis ?? 10000);
			fetch(url, { cache: 'no-store', credentials: 'same-origin', signal: abort.signal })
				.then((response): Promise<UploadStatus> | UploadStatus =>
					response.ok ? response.json() : {})
				.then((status) => {
					if (!this.running) {
						return;
					}
					if (status.totalBytes && status.totalBytes > 0) {
						this.reported = true;
						this.update(status.bytesUploaded ?? 0, status.totalBytes);
					} else if (this.reported && this.total) {
						// the server read it all, and takes the files
						this.update(this.total, this.total);
					}
				})
				.catch(() => undefined)
				.finally(() => {
					window.clearTimeout(timeout);
					this.poll();
				});
		}, this.options.pollMillis ?? 500);
	}

	private stopPolling(): void {
		window.clearTimeout(this.timer);
		this.timer = undefined;
	}

	/**
	 * Shows how far the upload got.
	 *
	 * @param loaded the bytes sent
	 * @param total the bytes to send
	 */
	update(loaded: number, total: number): void {
		this.loaded = Math.max(0, Math.min(loaded, total));
		this.total = Math.max(0, total);
		this.draw();
	}

	/**
	 * Shows how the upload running ended. The bar stays until the next upload starts.
	 *
	 * @param outcome how it ended
	 * @param message why it failed, instead of the text failed
	 * @param status the HTTP status the server answered a failed upload with, 0 when no answer
	 * came, which the text tells unless the message does
	 */
	end(outcome: UploadOutcome, message?: string, status?: number): void {
		if (!this.running) {
			return;
		}
		if (outcome === 'failed') {
			this.fail(message || (status === undefined ? undefined
				: status === 0 ? this.texts.lost : format(this.texts.serverError, status)));
			return;
		}
		this.stop();
		if (outcome === 'done') {
			if (!this.total) {
				this.total = this.loaded || this.expected;
			}
			this.loaded = this.total;
			this.setBar(100);
			// the size of the files, rather than of the request carrying them
			this.status.textContent = format(this.texts.done,
				formatBytes(this.expected || this.total, this.options.locale));
		} else {
			this.setBar(this.total ? this.percent() : 0);
			this.status.textContent = this.texts.canceled;
		}
		this.setState(outcome);
	}

	/**
	 * Shows that the upload failed, or that the files were not sent, even after the bar showed it
	 * ended: the server may tell later than the request that it refused the files.
	 *
	 * @param message why, instead of the text failed
	 */
	fail(message?: string): void {
		this.stop();
		this.setBar(this.total ? this.percent() : 0);
		this.status.textContent = message || this.texts.failed;
		this.setState('failed');
	}

	private stop(): void {
		this.running = false;
		this.stopPolling();
		this.note = null;
		window.clearTimeout(this.noteTimer);
		this.box.hidden = false;
	}

	/**
	 * Tells something for a moment, instead of the progress.
	 */
	tell(text: string, millis = 2500): void {
		this.note = text;
		this.draw();
		window.clearTimeout(this.noteTimer);
		this.noteTimer = window.setTimeout(() => {
			this.note = null;
			if (this.running) {
				this.draw();
			}
		}, millis);
	}

	private percent(): number {
		return this.total > 0 ? Math.floor(this.loaded * 100 / this.total) : 0;
	}

	private setBar(percent: number | null): void {
		if (percent === null) {
			this.value.removeAttribute('value');
			this.value.textContent = '';
			this.label.hidden = true;
		} else {
			this.value.value = percent;
			this.value.setAttribute('value', String(percent));
			this.value.textContent = percent + '%';
			this.label.textContent = percent + '%';
			this.label.hidden = false;
		}
	}

	private setState(outcome: UploadOutcome | null): void {
		toggleParts(this.status, this.look, outcome === 'done', 'upload-status-done');
		toggleParts(this.status, this.look, outcome === 'failed' || outcome === 'canceled',
			'upload-status-failed');
		this.box.setAttribute('data-state', outcome ?? 'running');
	}

	private draw(): void {
		const texts = this.texts;
		const locale = this.options.locale;
		if (!this.total) {
			this.setBar(null);
			this.status.textContent = this.note ?? texts.starting;
			return;
		}
		const percent = this.percent();
		this.setBar(percent);
		if (this.note) {
			this.status.textContent = this.note;
			return;
		}
		if (this.loaded >= this.total) {
			this.status.textContent = format(texts.processing, formatBytes(this.total, locale));
			return;
		}
		const seconds = (Date.now() - this.started) / 1000;
		const parts = [format(texts.sent, formatBytes(this.loaded, locale),
			formatBytes(this.total, locale))];
		if (seconds > 0.2 && this.loaded > 0) {
			const rate = this.loaded / seconds;
			parts.push(format(texts.rate, formatBytes(rate, locale)),
				format(texts.remaining, formatDuration((this.total - this.loaded) / rate, locale)));
		}
		this.status.textContent = parts.join(' · ');
	}

	/**
	 * Takes the progress away.
	 */
	destroy(): void {
		this.running = false;
		this.stopPolling();
		window.clearTimeout(this.noteTimer);
		this.listeners.forEach(([target, type, listener]) =>
			target.removeEventListener(type, listener));
		this.listeners = [];
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
		this.box.remove();
		if (progresses.get(this.host) === this) {
			progresses.delete(this.host);
		}
	}
}

/**
 * What the guard does while an upload runs.
 */
export interface UploadGuardOptions {
	/** Whether to ask before the page is left, true by default. */
	leaving?: boolean;
	/** Whether to stop the other requests of the page, false by default. */
	requests?: boolean;
	/** The URLs whose requests go on all the same, such as the one the upload reports to. */
	allow?: readonly string[];
}

/** A request the page is about to send, which the guard lets go or stops. */
export interface GuardedRequest {
	/** The element whose request it is, if any. */
	element?: Element | null;
	/** Where it goes. */
	url?: string | null;
	/** Whether the request was marked to go on while uploads run. */
	bypass?: boolean;
}

/** Ends what UploadGuard.begin() started. */
export interface UploadGuardHandle {
	end(): void;
}

const guards = new Set<Required<UploadGuardOptions>>();

function absolute(url: string): string {
	try {
		return new URL(url, document.baseURI).href;
	} catch {
		return url;
	}
}

function onBeforeUnload(event: BeforeUnloadEvent): string | undefined {
	if (!UploadGuard.keepsOnPage()) {
		return undefined;
	}
	event.preventDefault();
	// the browsers say their own words, but some want a value
	event.returnValue = '';
	return '';
}

/**
 * Keeps the user on the page, and the other requests of the page waiting, while uploads run.
 */
export const UploadGuard = {

	/** How long an element whose request was stopped is marked, in milliseconds. */
	blockedMillis: 1200,

	/**
	 * Guards the page until the handle returned ends: asks before the page is left, unless
	 * leaving is false, and stops the other requests of the page if requests is true.
	 *
	 * @returns ends this guard; ending it again does nothing
	 */
	begin(options?: UploadGuardOptions): UploadGuardHandle {
		const guard: Required<UploadGuardOptions> = {
			leaving: options?.leaving ?? true,
			requests: options?.requests ?? false,
			allow: (options?.allow ?? []).map(absolute)
		};
		if (guards.size === 0) {
			window.addEventListener('beforeunload', onBeforeUnload);
		}
		guards.add(guard);
		return {
			end() {
				if (guards.delete(guard) && guards.size === 0) {
					window.removeEventListener('beforeunload', onBeforeUnload);
				}
			}
		};
	},

	/**
	 * @returns whether an upload runs that keeps the user on the page
	 */
	keepsOnPage(): boolean {
		return Array.from(guards).some((guard) => guard.leaving);
	},

	/**
	 * @returns whether an upload runs that stops the other requests
	 */
	blocking(): boolean {
		return Array.from(guards).some((guard) => guard.requests);
	},

	/**
	 * Tells whether a request has to wait for the uploads, and if so marks its element for a moment
	 * and fires UPLOAD_BLOCKED_EVENT at it, or at the document.
	 *
	 * @returns whether to stop the request
	 */
	blocks(request: GuardedRequest): boolean {
		const blocking = Array.from(guards).filter((guard) => guard.requests);
		if (!blocking.length || request.bypass ||
			request.element?.closest?.(`[${UPLOAD_BYPASS_ATTRIBUTE}]`)) {
			return false;
		}
		const url = request.url ? absolute(request.url) : null;
		if (url && blocking.every((guard) => guard.allow.includes(url))) {
			return false;
		}
		const element = request.element;
		if (element?.isConnected) {
			element.setAttribute(UPLOAD_BLOCKED_ATTRIBUTE, '');
			window.setTimeout(() => element.removeAttribute(UPLOAD_BLOCKED_ATTRIBUTE),
				UploadGuard.blockedMillis);
		}
		(element?.isConnected ? element : document).dispatchEvent(
			new CustomEvent(UPLOAD_BLOCKED_EVENT, { bubbles: true, detail: { url: request.url } }));
		return true;
	},

	/**
	 * Ends every guard, as when the page goes; for tests.
	 */
	reset(): void {
		guards.clear();
		window.removeEventListener('beforeunload', onBeforeUnload);
	}
};
