/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-upload.js: registers Wicket.Upload for FilePickerBehavior, which calls
 * Wicket.Upload.FilePicker.attach(id, options), for UploadProgressBar, which calls
 * Wicket.Upload.UploadProgress.attach(id, options), and for UploadToResourceField, which has
 * Wicket's FileUploadToResourceField tell its uploads with Wicket.Upload.field(id, messages), how
 * the server words their errors being the messages, tells what guards them with
 * Wicket.Upload.guard(id, options) and starts them with Wicket.Upload.upload(id). field() wraps the
 * hooks Wicket's field calls in the browser, which stay the page's: before the request is sent it
 * calls Wicket.Upload.started(id, xhr), once the files arrived Wicket.Upload.ended(id, 'done'),
 * once the upload failed Wicket.Upload.failed(id, error); the request itself tells of a
 * cancellation. The guard of a field's upload lets the field's own Ajax callback go, which Wicket's
 * FileUploadToResourceField calls once the upload ended, or was canceled.
 *
 * A field sends one upload at a time, and none whose files break the limits its picker knows: the
 * server hears of those as of files it refused, without receiving them. The error the server's
 * callback receives is one it has words for, whatever the request ran into, and the files it
 * received go to the callback by POST, however many they are.
 *
 * The uploads of forms are told as well: a multipart form posted the usual way, whose progress the
 * server tells, and one sent by Ajax, with its request where Wicket's Ajax gives it to
 * /ajax/call/beforeSend (the plain JavaScript engine does; jQuery's gives its own object, and the
 * progress asks the server then). Neither goes while its files break a limit. The end of a request
 * ends its upload even if Wicket never tells it is complete.
 *
 * While an upload stops the other requests, every Wicket Ajax request gets a precondition asking
 * the guard (see UploadGuard): those of elements in one marked data-antilia-upload-bypass, those
 * marked with attrs.antiliaUploadBypass (UploadGuard.bypass() in Java) and those to the URLs the
 * upload allows go on.
 */

import { useWicketHost, wicket } from './host.js';
import { FilePicker, UploadGuard, type UploadGuardHandle, type UploadGuardOptions,
	type UploadOutcome, type UploadProblemKind, uploadProblems, UploadProgress, uploadEnded,
	uploadRefused, uploadStarted } from '../upload.js';

/** What Wicket's Ajax tells of a request, as far as uploads need it. */
interface AjaxAttributes {
	/** The id of the element whose request it is. */
	c?: string;
	/** The id of the form it submits. */
	f?: string;
	/** Where it goes. */
	u?: string;
	/** Whether it is sent as multipart. */
	mp?: boolean;
	/** Its method, GET or POST. */
	m?: string;
	/** Its preconditions. */
	pre?: unknown[];
	/** Set by UploadGuard.bypass() in Java: the request goes on while uploads run. */
	antiliaUploadBypass?: boolean;
}

type Subscriber = (event: unknown, attrs: AjaxAttributes, ...rest: unknown[]) => void;

interface WicketEvents {
	subscribe(topic: string, subscriber: Subscriber): void;
}

/** What Wicket's Ajax request gives: an XMLHttpRequest, or jQuery's object. */
interface AjaxRequest {
	status?: number;
	addEventListener?: XMLHttpRequest['addEventListener'];
	always?: (callback: () => void) => unknown;
}

/** What the request of Wicket's FileUploadToResourceField tells its error callback, and sends. */
interface UploadError {
	error?: boolean;
	errorMessage?: unknown;
}

/** Wicket's FileUploadToResourceField in the browser, Wicket.Timer[id]. */
interface ResourceField {
	upload?: () => void;
	ajaxCallBackUrl?: unknown;
	/** The hooks of the page, which Wicket's field calls. */
	clientBeforeSendCallBack?: (xhr: XMLHttpRequest) => unknown;
	clientSideSuccessCallBack?: () => unknown;
	uploadErrorCallBack?: (error: UploadError) => unknown;
	/** Whether field() wrapped the hooks. */
	antiliaUpload?: boolean;
}

/** An upload of a field running: its request, if known, and the guard it began. */
interface Upload {
	xhr?: XMLHttpRequest;
	guard: UploadGuardHandle | null;
}

/** The errors the server has words for, as Wicket's FileUploadToResourceField tells them. */
const ERRORS = ['wicket.no.files.selected', 'uploadTooLarge', 'uploadSingleFileTooLarge',
	'uploadTooManyFiles', 'uploadFailed', 'uploadConnectionLost', 'uploadSameName'];

/** The error the server tells of each limit broken. */
const PROBLEM_ERRORS: Record<UploadProblemKind, string> = {
	tooLarge: 'uploadSingleFileTooLarge',
	tooMany: 'uploadTooManyFiles',
	tooMuch: 'uploadTooLarge',
	sameName: 'uploadSameName'
};

/** How long a form's upload waits for Wicket to tell its request is complete, once it ended. */
const COMPLETE_MILLIS = 3000;

/** The uploads of fields running. */
const running = new Map<string, Upload>();

/** What guards the uploads of fields, by field. */
const guards = new Map<string, UploadGuardOptions>();

/** The words of the server for the errors of the uploads of fields, by field. */
const words = new Map<string, Record<string, string>>();

/**
 * Sets what guards the next uploads of a field.
 *
 * @param source the id of the field
 * @param options what the guard does; null for no guard
 */
function guard(source: string, options: UploadGuardOptions | null): void {
	if (options) {
		guards.set(source, options);
	} else {
		guards.delete(source);
	}
}

/**
 * @returns Wicket's FileUploadToResourceField of an id, if any
 */
function fieldOf(source: string): ResourceField | undefined {
	const fields = window.Wicket?.Timer as Record<string, ResourceField> | undefined;
	return fields?.[source];
}

/**
 * Has Wicket's FileUploadToResourceField of an id tell its uploads, around the hooks of the page,
 * which it calls after: the request about to be sent, the files arrived, the upload failed. The
 * hook of a cancellation stays as it is: the request tells of it.
 *
 * @param source the id of the field
 * @param errors how the server words the errors of its uploads, which the progress tells, by
 * error, such as uploadTooLarge
 */
function field(source: string, errors?: Record<string, string>): void {
	if (errors) {
		words.set(source, { ...errors });
	}
	const instance = fieldOf(source);
	if (!instance || instance.antiliaUpload) {
		return;
	}
	instance.antiliaUpload = true;
	const before = instance.clientBeforeSendCallBack;
	instance.clientBeforeSendCallBack = function (this: unknown, xhr: XMLHttpRequest) {
		started(source, xhr);
		return before?.call(this, xhr);
	};
	const success = instance.clientSideSuccessCallBack;
	instance.clientSideSuccessCallBack = function (this: unknown) {
		ended(source, 'done');
		return success?.call(this);
	};
	const error = instance.uploadErrorCallBack;
	instance.uploadErrorCallBack = function (this: unknown, res: UploadError) {
		// the page's hook hears of the error the server is told
		failed(source, res);
		return error?.call(this, res);
	};
}

/**
 * @returns the URL of the Ajax callback of Wicket's FileUploadToResourceField of an id, if any
 */
function callbackOf(source: string): string | undefined {
	const url = fieldOf(source)?.ajaxCallBackUrl;
	return typeof url === 'string' ? url : undefined;
}

/**
 * Tells the server that an upload of a field failed, as Wicket's FileUploadToResourceField does.
 */
function tell(source: string, error: string): void {
	const url = callbackOf(source);
	const ajax = wicket().Ajax as { get?: (attrs: object) => unknown } | undefined;
	if (url && ajax?.get) {
		ajax.get({ u: url, ep: { error: true, errorMessage: error } });
	}
}

/**
 * Starts the upload of a field, unless one runs, or its files break a limit its picker knows, or
 * there are none: the server hears of those as of files it refused.
 *
 * @param source the id of the field
 * @returns whether the upload started
 */
function upload(source: string): boolean {
	const instance = fieldOf(source);
	if (!instance?.upload || running.has(source)) {
		return false;
	}
	const input = document.getElementById(source) as HTMLInputElement | null;
	const problem = uploadProblems(input)[0];
	if (problem || !input?.files?.length) {
		const error = problem ? PROBLEM_ERRORS[problem.kind] : 'wicket.no.files.selected';
		uploadRefused(source, problem?.message ?? words.get(source)?.[error]);
		tell(source, error);
		return false;
	}
	field(source);
	instance.upload();
	return true;
}

/**
 * Tells that a field started to send its files, guarding the page as guard() set, and ends the
 * upload when the request ends, if the page does not first.
 *
 * @param source the id of the field
 * @param xhr the request sending the files
 */
function started(source: string, xhr?: XMLHttpRequest): void {
	running.get(source)?.guard?.end();
	const options = guards.get(source);
	const callback = callbackOf(source);
	running.set(source, { xhr, guard: options ? UploadGuard.begin({ ...options,
		allow: [...(options.allow ?? []), ...(callback ? [callback] : [])] }) : null });
	if (xhr) {
		// the request ends its own upload only, not one started after it
		const mine = () => running.get(source)?.xhr === xhr;
		xhr.addEventListener('abort', () => mine() && ended(source, 'canceled'));
		xhr.addEventListener('error', () => mine() && ended(source, 'failed', undefined, 0));
		xhr.addEventListener('loadend', () => {
			if (!mine()) {
				return;
			}
			// Wicket's handlers ran before, and told nothing: its answer was not understood
			const error = xhr.status ? 'uploadFailed' : 'uploadConnectionLost';
			tell(source, error);
			ended(source, 'failed', words.get(source)?.[error], xhr.status);
		});
	}
	uploadStarted(source, xhr);
}

/**
 * Tells that a field's upload ended, ending its guard; only the first end of an upload counts.
 *
 * @param source the id of the field
 * @param outcome how it ended
 * @param message why it failed
 * @param status the HTTP status the server answered a failed upload with, 0 when no answer came
 */
function ended(source: string, outcome: UploadOutcome, message?: string, status?: number): void {
	if (!running.has(source)) {
		return;
	}
	running.get(source)?.guard?.end();
	running.delete(source);
	uploadEnded(source, outcome, message, status);
}

/**
 * Tells that a field's upload failed, as the error callback of Wicket's FileUploadToResourceField
 * hears: the error goes to the server next, so one the server has no words for, such as the status
 * text of the answer, becomes uploadFailed, or uploadConnectionLost when no answer came.
 *
 * @param source the id of the field
 * @param error what the request tells, and sends to the server
 */
function failed(source: string, error?: UploadError): void {
	const xhr = running.get(source)?.xhr;
	let key = typeof error?.errorMessage === 'string' ? error.errorMessage : '';
	if (xhr && xhr.status === 0) {
		key = 'uploadConnectionLost';
	} else if (!ERRORS.includes(key)) {
		key = 'uploadFailed';
	}
	if (error) {
		error.errorMessage = key;
	}
	// the server's words, unless it answered with an error, whose status tells more
	const status = xhr?.status;
	const answered = status === undefined || (status >= 200 && status < 300);
	ended(source, 'failed', answered || status === 0 ? words.get(source)?.[key] : undefined,
		answered ? undefined : status);
}

/**
 * @returns whether a URL is the Ajax callback of a field whose uploads field() follows
 */
function isFieldCallback(url: string | undefined): boolean {
	const fields = window.Wicket?.Timer as Record<string, ResourceField> | undefined;
	return !!url && !!fields && Object.values(fields)
		.some((instance) => instance?.antiliaUpload && instance.ajaxCallBackUrl === url);
}

/**
 * The precondition of every Wicket Ajax request while an upload stops the others.
 */
function precondition(attrs: AjaxAttributes): boolean {
	return !UploadGuard.blocks({
		element: attrs.c ? document.getElementById(attrs.c) : null,
		url: attrs.u,
		bypass: attrs.antiliaUploadBypass === true
	});
}

/**
 * The precondition of a multipart form sent by Ajax: none whose files break a limit their pickers
 * know goes.
 */
function formPrecondition(attrs: AjaxAttributes): boolean {
	const problems = attrs.f ? uploadProblems(document.getElementById(attrs.f)) : [];
	if (problems.length) {
		uploadRefused(attrs.f!, problems.map((problem) => problem.message).join(' · '));
		return false;
	}
	return true;
}

/**
 * @returns whether a form holds files to send
 */
function hasFiles(form: Element | null): form is Element {
	return !!form && Array.from(form.querySelectorAll('input[type="file"]'))
		.some((input) => !!(input as HTMLInputElement).files?.length);
}

/** The forms whose files Ajax is sending, with the request sending them. */
const formUploads = new Map<string, unknown>();

/**
 * Ends the upload of a form sent by Ajax, if it is the request's.
 *
 * @param form the id of the form
 * @param request the request
 * @param success whether Wicket took the answer
 */
function endFormUpload(form: string, request: unknown, success: boolean): void {
	const own = formUploads.get(form);
	if (!formUploads.has(form) || (own !== undefined && own !== request)) {
		return;
	}
	formUploads.delete(form);
	const status = (request as AjaxRequest | undefined)?.status;
	uploadEnded(form, success ? 'done' : 'failed', undefined,
		success || status === undefined || (status >= 200 && status < 300) ? undefined : status);
}

/**
 * Ends the upload of a form a while after its request ended, should Wicket not tell first that
 * the request is complete.
 */
function endFormUploadAfter(form: string, request: unknown): void {
	const later = () => window.setTimeout(() => {
		const status = (request as AjaxRequest).status ?? 0;
		endFormUpload(form, request, status >= 200 && status < 300);
	}, COMPLETE_MILLIS);
	const ajax = request as AjaxRequest | undefined;
	if (typeof ajax?.addEventListener === 'function') {
		ajax.addEventListener('loadend', later);
	} else if (typeof ajax?.always === 'function') {
		ajax.always(later);
	}
}

const Wicket = wicket();
if (Wicket.Upload === undefined) {
	useWicketHost();
	Wicket.Upload = { FilePicker, UploadProgress, UploadGuard, guard, field, upload, started, ended,
		failed, precondition };

	const events = Wicket.Event as WicketEvents | undefined;
	if (events?.subscribe) {
		events.subscribe('/ajax/call/before', (_event, attrs) => {
			if (!attrs) {
				return;
			}
			// Wicket's field tells its callback of the files received in the URL of a GET, which
			// grows too long for the server with many of them: a POST carries them in its body
			if (isFieldCallback(attrs.u)) {
				attrs.m = 'POST';
			}
			const pre = Array.isArray(attrs.pre) ? attrs.pre : [];
			const added = [
				...(UploadGuard.blocking() && !pre.includes(precondition) ? [precondition] : []),
				...(attrs.mp && attrs.f && !pre.includes(formPrecondition) ? [formPrecondition] : [])
			];
			if (added.length) {
				// a new array: Wicket shares the attributes' own between the requests of an element
				attrs.pre = [...pre, ...added];
			}
		});
		events.subscribe('/ajax/call/beforeSend', (_event, attrs, xhr) => {
			if (attrs?.mp && attrs.f && hasFiles(document.getElementById(attrs.f))) {
				formUploads.set(attrs.f, xhr);
				uploadStarted(attrs.f, xhr instanceof XMLHttpRequest ? xhr : undefined);
				endFormUploadAfter(attrs.f, xhr);
			}
		});
		events.subscribe('/ajax/call/complete', (_event, attrs, xhr, textStatus) => {
			if (attrs?.f) {
				endFormUpload(attrs.f, xhr, textStatus === 'success');
			}
		});
	}

	// a form posted the usual way: after the page's own listeners, unless one of them stopped it;
	// not at all while its files break a limit
	window.addEventListener('submit', (event) => {
		const form = event.target;
		if (event.defaultPrevented || !(form instanceof HTMLFormElement) ||
			form.enctype !== 'multipart/form-data' || !hasFiles(form)) {
			return;
		}
		const problems = uploadProblems(form);
		if (problems.length) {
			event.preventDefault();
			if (form.id) {
				uploadRefused(form.id, problems.map((problem) => problem.message).join(' · '));
			}
		} else if (form.id) {
			uploadStarted(form.id, undefined, undefined, true);
		}
	});
}
