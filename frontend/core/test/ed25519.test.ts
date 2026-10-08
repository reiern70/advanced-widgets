/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { describe, expect, it } from 'vitest';

import { sha512Of, verifyEd25519 } from '../src/ed25519.js';
import { PUBLIC_KEY } from '../src/index.js';
// the key of the demo application, signed by the private key of the server
import demoKey from '../../../demo-app/src/main/resources/META-INF/advanced-widgets.license?raw';

function fromBase64(text: string): Uint8Array {
	const base64 = text.replace(/-/g, '+').replace(/_/g, '/');
	return Uint8Array.from(atob(base64 + '='.repeat((4 - base64.length % 4) % 4)),
		(char) => char.charCodeAt(0));
}

describe('sha512Of, for pages without Web Crypto', () => {
	it('hashes as Web Crypto does, over one block and more', async () => {
		for (const length of [0, 3, 111, 112, 127, 128, 129, 300]) {
			const data = Uint8Array.from({ length }, (_, index) => (index * 31 + 7) & 0xff);
			const expected = new Uint8Array(await crypto.subtle.digest('SHA-512', data));

			expect(sha512Of(data)).toEqual(expected);
		}
	});

	it('gives the digest of the standard for abc', () => {
		const hex = Array.from(sha512Of(new TextEncoder().encode('abc')),
			(byte) => byte.toString(16).padStart(2, '0')).join('');

		expect(hex).toBe('ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a' +
			'2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f');
	});
});

describe('verifyEd25519, for browsers without Ed25519 in Web Crypto', () => {
	it('verifies what Web Crypto signs, and refuses what was changed', async () => {
		const keys = await crypto.subtle.generateKey({ name: 'Ed25519' }, true,
			['sign', 'verify']) as CryptoKeyPair;
		const raw = new Uint8Array(await crypto.subtle.exportKey('raw', keys.publicKey));
		for (const text of ['', 'a', 'The quick brown fox', 'x'.repeat(1000)]) {
			const message = new TextEncoder().encode(text);
			const signature = new Uint8Array(await crypto.subtle.sign({ name: 'Ed25519' },
				keys.privateKey, message));

			expect(await verifyEd25519(raw, signature, message)).toBe(true);
			const changed = Uint8Array.from(message.length ? message : [0]);
			changed[0] ^= 1;
			expect(await verifyEd25519(raw, signature, changed)).toBe(false);
			const forged = Uint8Array.from(signature);
			forged[40] ^= 1;
			expect(await verifyEd25519(raw, forged, message)).toBe(false);
		}
	});

	it('verifies the key of the demo application against the public key of the server', async () => {
		const [, payload, signature] = demoKey.trim().split('.');
		const raw = fromBase64(PUBLIC_KEY).subarray(12);

		expect(await verifyEd25519(raw, fromBase64(signature), fromBase64(payload))).toBe(true);
		expect(await verifyEd25519(raw, fromBase64(signature),
			new TextEncoder().encode('{"licensee":"me","components":"*"}'))).toBe(false);
	});

	it('refuses signatures and keys that are no points of the curve', async () => {
		const message = new Uint8Array([1, 2, 3]);
		expect(await verifyEd25519(new Uint8Array(32).fill(0xff), new Uint8Array(64), message))
			.toBe(false);
		expect(await verifyEd25519(new Uint8Array(31), new Uint8Array(64), message)).toBe(false);
		expect(await verifyEd25519(fromBase64(PUBLIC_KEY).subarray(12), new Uint8Array(63),
			message)).toBe(false);
	});
});
