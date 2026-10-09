/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The license keys of the private components, checked in the browser as LicenseKey checks them on
 * the server: a key is AW1.<payload>.<signature>, both URL-safe Base64, the payload a JSON object
 * naming the licensee, the last day the key is valid on (none for a key that does not expire) and
 * the components it activates (* for all, or their names separated by commas), the signature the
 * Ed25519 signature of the payload, verified with the Web Crypto API, or by ed25519.ts in browsers
 * whose Web Crypto has no Ed25519 yet.
 *
 * As any check in the browser it keeps honest users honest.
 */

import { verifyEd25519 } from './ed25519.js';

/** The start of every key, naming its format. */
export const LICENSE_PREFIX = 'AW1.';

/** The public key checking the signatures, X.509 (SPKI) encoded in Base64, as on the server. */
export const PUBLIC_KEY = 'MCowBQYDK2VwAyEACHyUV4LF2hZ0mpwS4Z90Oi5oXDKmPVwQHxRAFu757F4=';

/** The CSS class of the placeholder of a private component, as on the server. */
export const UNLICENSED_CLASS = 'antilia-unlicensed';

/** A checked license key. */
export interface License {
	/** Whom the key was issued to. */
	licensee: string;
	/** The last day the key is valid on, as YYYY-MM-DD; null if it does not expire. */
	expires: string | null;
	/** The names of the components the key activates, * for all. */
	components: string[];
}

function decode(base64: string, url: boolean): Uint8Array<ArrayBuffer> {
	const text = url ? base64.replace(/-/g, '+').replace(/_/g, '/') : base64;
	const binary = atob(text + '='.repeat((4 - text.length % 4) % 4));
	return Uint8Array.from(binary, (char) => char.charCodeAt(0));
}

/**
 * Reads and checks a key.
 *
 * @param key the text of the key
 * @param publicKey the public key checking the signature, for tests
 * @returns the license
 * @throws Error if the text is not a key or its signature is wrong
 */
export async function verifyLicense(key: string, publicKey = PUBLIC_KEY): Promise<License> {
	const text = (key ?? '').trim();
	if (!text.startsWith(LICENSE_PREFIX)) {
		throw new Error('Not a license key of Antilia\'s Advanced Widgets');
	}
	const parts = text.substring(LICENSE_PREFIX.length).split('.');
	if (parts.length !== 2) {
		throw new Error('A license key has a payload and a signature');
	}
	let payload: Uint8Array<ArrayBuffer>;
	let signature: Uint8Array<ArrayBuffer>;
	try {
		payload = decode(parts[0], true);
		signature = decode(parts[1], true);
	} catch {
		throw new Error('The license key is not Base64');
	}
	if (!await verify(decode(publicKey, false), signature, payload)) {
		throw new Error('The signature of the license key is wrong');
	}
	try {
		const json = JSON.parse(new TextDecoder().decode(payload)) as Record<string, unknown>;
		if (typeof json.licensee !== 'string' || typeof json.components !== 'string') {
			throw new Error();
		}
		return {
			licensee: json.licensee,
			expires: typeof json.expires === 'string' ? json.expires : null,
			components: json.components.split(',').map((name) => name.trim())
				.filter((name) => name.length > 0)
		};
	} catch {
		throw new Error('The payload of the license key is wrong');
	}
}

/**
 * @param spki the public key, X.509 (SPKI) encoded
 * @returns whether the Ed25519 signature of the payload is right: checked by Web Crypto, or, where
 *          it has no Ed25519, by verifyEd25519 with the raw key, the last 32 bytes of the SPKI
 */
async function verify(spki: Uint8Array<ArrayBuffer>, signature: Uint8Array<ArrayBuffer>,
	payload: Uint8Array<ArrayBuffer>): Promise<boolean> {
	let key: CryptoKey;
	try {
		key = await crypto.subtle.importKey('spki', spki, { name: 'Ed25519' }, false, ['verify']);
	} catch {
		return verifyEd25519(spki.subarray(spki.length - 32), signature, payload);
	}
	return crypto.subtle.verify({ name: 'Ed25519' }, key, signature, payload);
}

/**
 * @param day the day, today when left out
 * @returns the day as YYYY-MM-DD, in the browser's time zone
 */
function isoDay(day: Date): string {
	const pad = (value: number) => String(value).padStart(2, '0');
	return day.getFullYear() + '-' + pad(day.getMonth() + 1) + '-' + pad(day.getDate());
}

/**
 * @param names the names of the component, any of which the key may name: the component's own and
 *            those of its Wicket counterpart and its superclasses
 * @returns whether the license activates the component on the day
 */
export function activates(license: License | null, names: string[], day = new Date()): boolean {
	if (!license || (license.expires !== null && isoDay(day) > license.expires)) {
		return false;
	}
	return license.components.includes('*') ||
		names.some((name) => license.components.includes(name));
}

let activation: Promise<License | null> = Promise.resolve(null);

/**
 * Activates the private components of the page with a key, as the license key of the application
 * does on the server.
 *
 * @param key the text of the key
 * @returns the license, or null (and logged) if the key is wrong
 */
export function activate(key: string): Promise<License | null> {
	activation = verifyLicense(key).catch((error: unknown) => {
		console.error('Antilia\'s Advanced Widgets: the license key is wrong', error);
		return null;
	});
	return activation;
}

/**
 * @param names the names of the component, see {@link activates}
 * @param key a key of the component's own; the one given to {@link activate} when left out
 * @returns whether the component is activated, once the key is checked
 */
export async function isLicensed(names: string[], key?: string | null): Promise<boolean> {
	const license = key ? await verifyLicense(key).catch(() => null) : await activation;
	return activates(license, names);
}
