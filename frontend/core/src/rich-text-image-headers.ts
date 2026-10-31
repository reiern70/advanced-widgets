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
 * The width and the height of an image, read from the header of its file without decoding the
 * picture, as Wicket's RichTextImageHeaders reads them on the server: a small file may hold a
 * picture of many millions of pixels, which decoding would take the memory of. PNG, JPEG (turned
 * as its EXIF says), GIF (its screen, or a frame going beyond it), WebP and AVIF; null for a file
 * whose header does not tell them, or tells nonsense.
 */

/** The width and the height of an image, in pixels, as the browser shows it. */
export interface RichTextImageDimensions {
	width: number;
	height: number;
}

/** The most bytes of a file read for its header. */
const MAX_READ = 16 * 1024 * 1024;

/**
 * @returns the dimensions of an image of a type, by the media type, read from its file; null when
 *          its header does not tell them, or for a type not known here
 */
export async function readImageDimensions(file: Blob,
	type: string): Promise<RichTextImageDimensions | null> {
	const reader = READERS[type.toLowerCase()];
	if (!reader) {
		return null;
	}
	try {
		const bytes = new Uint8Array(await bytesOf(file.slice(0, MAX_READ)));
		return reader(bytes);
	} catch {
		return null;
	}
}

/**
 * @returns the bytes of a blob: by arrayBuffer() where the browser has it, else by a FileReader
 */
function bytesOf(blob: Blob): Promise<ArrayBuffer> {
	if (typeof blob.arrayBuffer === 'function') {
		return blob.arrayBuffer();
	}
	return new Promise((resolve, reject) => {
		const reader = new FileReader();
		reader.onload = () => resolve(reader.result as ArrayBuffer);
		reader.onerror = () => reject(reader.error);
		reader.readAsArrayBuffer(blob);
	});
}

/**
 * @returns the dimensions of an image, by the bytes of its file, for each media type known
 */
const READERS: Readonly<Record<string, (bytes: Uint8Array) => RichTextImageDimensions | null>> = {
	'image/png': png,
	'image/jpeg': jpeg,
	'image/gif': gif,
	'image/webp': webp,
	'image/avif': avif
};

/**
 * @returns the dimensions, their width and height swapped when turned a quarter
 */
export function turned(dimensions: RichTextImageDimensions): RichTextImageDimensions {
	return { width: dimensions.height, height: dimensions.width };
}

/**
 * @returns whether an image is no wider and no higher than a limit
 */
export function fits(dimensions: RichTextImageDimensions,
	max: RichTextImageDimensions | null): boolean {
	return !max || dimensions.width <= max.width && dimensions.height <= max.height;
}

/**
 * @returns the dimensions of an image made smaller to fit a limit, its proportions kept, each at
 *          least one pixel; the same when it fits
 */
export function scaledToFit(dimensions: RichTextImageDimensions,
	max: RichTextImageDimensions): RichTextImageDimensions {
	if (fits(dimensions, max)) {
		return dimensions;
	}
	const scale = Math.min(max.width / dimensions.width, max.height / dimensions.height);
	return { width: Math.max(1, Math.floor(dimensions.width * scale)),
		height: Math.max(1, Math.floor(dimensions.height * scale)) };
}

/**
 * @returns dimensions as the messages say them: 640 × 480
 */
export function dimensionsText(dimensions: RichTextImageDimensions): string {
	return dimensions.width + ' × ' + dimensions.height;
}

function dimensions(width: number, height: number): RichTextImageDimensions | null {
	return width > 0 && height > 0 && Number.isSafeInteger(width) && Number.isSafeInteger(height) ?
		{ width, height } : null;
}

function ascii(bytes: Uint8Array, at: number, text: string): boolean {
	if (at < 0 || at + text.length > bytes.length) {
		return false;
	}
	for (let i = 0; i < text.length; i++) {
		if (bytes[at + i] !== text.charCodeAt(i)) {
			return false;
		}
	}
	return true;
}

function u8(bytes: Uint8Array, at: number): number {
	return bytes[at] ?? 0;
}

function u16be(bytes: Uint8Array, at: number): number {
	return (u8(bytes, at) << 8) | u8(bytes, at + 1);
}

function u16le(bytes: Uint8Array, at: number): number {
	return u8(bytes, at) | (u8(bytes, at + 1) << 8);
}

function u24le(bytes: Uint8Array, at: number): number {
	return u8(bytes, at) | (u8(bytes, at + 1) << 8) | (u8(bytes, at + 2) << 16);
}

function u32be(bytes: Uint8Array, at: number): number {
	return u8(bytes, at) * 0x1000000 + ((u8(bytes, at + 1) << 16) | (u8(bytes, at + 2) << 8) |
		u8(bytes, at + 3));
}

function u32le(bytes: Uint8Array, at: number): number {
	return u16le(bytes, at) + u16le(bytes, at + 2) * 0x10000;
}

/**
 * @returns the dimensions of a PNG, from its first chunk, IHDR
 */
function png(bytes: Uint8Array): RichTextImageDimensions | null {
	return bytes.length >= 24 && ascii(bytes, 12, 'IHDR') ?
		dimensions(u32be(bytes, 16), u32be(bytes, 20)) : null;
}

/**
 * @returns the dimensions of a GIF: of its screen, or of a frame going beyond it
 */
function gif(bytes: Uint8Array): RichTextImageDimensions | null {
	if (bytes.length < 13) {
		return null;
	}
	let width = u16le(bytes, 6);
	let height = u16le(bytes, 8);
	let at = 13;
	const flags = u8(bytes, 10);
	if (flags & 0x80) {
		at += 3 << ((flags & 0x07) + 1);
	}
	const skipBlocks = (): boolean => {
		while (at < bytes.length) {
			const length = u8(bytes, at++);
			if (length === 0) {
				return true;
			}
			at += length;
		}
		return false;
	};
	while (at < bytes.length) {
		const block = u8(bytes, at++);
		if (block === 0x2c) {
			if (at + 9 > bytes.length) {
				return null;
			}
			width = Math.max(width, u16le(bytes, at) + u16le(bytes, at + 4));
			height = Math.max(height, u16le(bytes, at + 2) + u16le(bytes, at + 6));
			const local = u8(bytes, at + 8);
			at += 9;
			if (local & 0x80) {
				at += 3 << ((local & 0x07) + 1);
			}
			// the size of the codes, then the picture in blocks
			at++;
			if (!skipBlocks()) {
				return dimensions(width, height);
			}
		} else if (block === 0x21) {
			at++;
			if (!skipBlocks()) {
				return dimensions(width, height);
			}
		} else if (block === 0x3b) {
			break;
		} else {
			return null;
		}
	}
	return dimensions(width, height);
}

/**
 * @returns the dimensions of a WebP: of its canvas, or of its picture, lossy or lossless
 */
function webp(bytes: Uint8Array): RichTextImageDimensions | null {
	if (bytes.length < 30 || !ascii(bytes, 0, 'RIFF') || !ascii(bytes, 8, 'WEBP')) {
		return null;
	}
	if (ascii(bytes, 12, 'VP8X')) {
		return dimensions(u24le(bytes, 24) + 1, u24le(bytes, 27) + 1);
	}
	if (ascii(bytes, 12, 'VP8L')) {
		if (u8(bytes, 20) !== 0x2f) {
			return null;
		}
		const bits = u32le(bytes, 21);
		return dimensions((bits & 0x3fff) + 1, (Math.floor(bits / 0x4000) & 0x3fff) + 1);
	}
	if (ascii(bytes, 12, 'VP8 ')) {
		if (u8(bytes, 23) !== 0x9d || u8(bytes, 24) !== 0x01 || u8(bytes, 25) !== 0x2a) {
			return null;
		}
		return dimensions(u16le(bytes, 26) & 0x3fff, u16le(bytes, 28) & 0x3fff);
	}
	return null;
}

/**
 * @returns the dimensions of a JPEG, from its frame, turned when its EXIF says the camera was
 */
function jpeg(bytes: Uint8Array): RichTextImageDimensions | null {
	if (u8(bytes, 0) !== 0xff || u8(bytes, 1) !== 0xd8) {
		return null;
	}
	let at = 2;
	let isTurned = false;
	while (at < bytes.length) {
		if (u8(bytes, at) !== 0xff) {
			return null;
		}
		while (u8(bytes, at) === 0xff && at < bytes.length) {
			at++;
		}
		const marker = u8(bytes, at++);
		if (at > bytes.length || marker === 0xd9 || marker === 0xda) {
			return null;
		}
		if (marker === 0x01 || marker === 0xd8 || marker >= 0xd0 && marker <= 0xd7) {
			continue;
		}
		const length = u16be(bytes, at);
		if (length < 2) {
			return null;
		}
		if (marker >= 0xc0 && marker <= 0xcf && marker !== 0xc4 && marker !== 0xc8 &&
			marker !== 0xcc) {
			if (at + 7 > bytes.length) {
				return null;
			}
			const found = dimensions(u16be(bytes, at + 5), u16be(bytes, at + 3));
			return found && isTurned ? turned(found) : found;
		}
		if (marker === 0xe1) {
			isTurned = isTurned || exifTurned(bytes.subarray(at + 2, at + length));
		}
		at += length;
	}
	return null;
}

/**
 * @returns whether the EXIF of a segment says the camera was turned a quarter: orientations 5 to
 *          8, which the browser shows turned
 */
function exifTurned(segment: Uint8Array): boolean {
	if (segment.length < 14 || !ascii(segment, 0, 'Exif') || segment[4] !== 0 ||
		segment[5] !== 0) {
		return false;
	}
	const tiff = segment.subarray(6);
	const little = ascii(tiff, 0, 'II');
	if (!little && !ascii(tiff, 0, 'MM')) {
		return false;
	}
	const u16 = (at: number) => little ? u16le(tiff, at) : u16be(tiff, at);
	const ifd = little ? u32le(tiff, 4) : u32be(tiff, 4);
	if (ifd < 8 || ifd + 2 > tiff.length) {
		return false;
	}
	const entries = u16(ifd);
	for (let i = 0; i < entries; i++) {
		const entry = ifd + 2 + i * 12;
		if (entry + 12 > tiff.length) {
			return false;
		}
		if (u16(entry) === 0x0112) {
			const orientation = u16(entry + 8);
			return orientation >= 5 && orientation <= 8;
		}
	}
	return false;
}

/**
 * @returns the dimensions of an AVIF: the largest its properties give a picture of it, turned
 *          when one of them says so
 */
function avif(bytes: Uint8Array): RichTextImageDimensions | null {
	const found = { width: 0, height: 0, turned: false, meta: false };
	const read = (from: number, to: number, top: boolean) => {
		let at = from;
		while (at + 8 <= to) {
			let size = u32be(bytes, at);
			let header = 8;
			if (size === 1) {
				if (at + 16 > to) {
					return;
				}
				size = u32be(bytes, at + 8) * 0x100000000 + u32be(bytes, at + 12);
				header = 16;
			} else if (size === 0) {
				size = to - at;
			}
			if (size < header || at + size > to) {
				// the metadata cut short: the boxes read so far
				if (!(top && ascii(bytes, at + 4, 'meta'))) {
					return;
				}
				size = to - at;
			}
			const end = at + size;
			const body = at + header;
			if (top && ascii(bytes, at + 4, 'meta')) {
				found.meta = true;
				// a full box: its version and flags first
				read(body + 4, end, false);
				return;
			}
			if (ascii(bytes, at + 4, 'iprp') || ascii(bytes, at + 4, 'ipco')) {
				read(body, end, false);
			} else if (ascii(bytes, at + 4, 'ispe') && body + 12 <= end) {
				found.width = Math.max(found.width, u32be(bytes, body + 4));
				found.height = Math.max(found.height, u32be(bytes, body + 8));
			} else if (ascii(bytes, at + 4, 'irot') && body < end) {
				found.turned = found.turned || (u8(bytes, body) & 0x01) !== 0;
			}
			at = end;
		}
	};
	read(0, bytes.length, true);
	const result = found.meta ? dimensions(found.width, found.height) : null;
	return result && found.turned ? turned(result) : result;
}
