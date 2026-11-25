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
 * The Resumable upload demo: the uploader of the core sends files in chunks over tus to the
 * resource the Wicket page mounted; each file the server holds whole is handed over to the
 * receipts resource of the demo, which answers its name, its size and its SHA-256, then deletes it.
 */

import type { FilePickerTexts, ResumableUploaderTexts } from '@antilia/core';

/** The texts of the demo, by the keys of the page's bundle. */
export interface ResumableUploadTexts {
	received: string;
	'received.name': string;
	'received.size': string;
	'received.sha256': string;
	'received.none': string;
	'received.failed': string;
}

/** What the page gives the demo. */
export interface ResumableUploadConfig {
	/** Where uploads are created: the tus resource. */
	endpoint: string;
	/** The signed query the resource wants on every request, for the files of this page. */
	query: string;
	/** Where a file the server holds whole is handed over. */
	receiptsUrl: string;
	fileMaxSize: number;
	chunkSize: number;
	locale: string;
	texts: ResumableUploadTexts;
	uploaderTexts: Partial<ResumableUploaderTexts>;
	pickerTexts: Partial<FilePickerTexts>;
}

/** What the server tells of a file it received whole, before deleting it. */
export interface Receipt {
	name: string;
	size: number;
	sha256: string;
}

/** The receipts the apps list, the latest first. */
export const RECEIVED_MAX = 12;

/**
 * Hands a file the server stored whole over to the receipts resource.
 *
 * @param config what the page gave the demo
 * @param storedName the name the server stored the file under, as it told it
 * @param clientFileName the name of the file
 * @returns what the server tells of it, null when it did not take it
 */
export async function receive(config: ResumableUploadConfig, storedName: string,
	clientFileName: string): Promise<Receipt | null> {
	try {
		const response = await fetch(config.receiptsUrl, { method: 'POST',
			credentials: 'same-origin', body: new URLSearchParams({ name: storedName,
				clientFileName }) });
		return response.ok ? await response.json() as Receipt : null;
	} catch {
		return null;
	}
}

/**
 * @returns a size as the page writes it: bytes, KB, MB or GB, with one decimal
 */
export function sizeOf(bytes: number, locale: string): string {
	const units = ['bytes', 'KB', 'MB', 'GB'];
	let value = bytes;
	let unit = 0;
	while (value >= 1024 && unit < units.length - 1) {
		value /= 1024;
		unit++;
	}
	return new Intl.NumberFormat(locale, { maximumFractionDigits: unit ? 1 : 0 }).format(value) +
		' ' + units[unit];
}
