/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { beforeAll, describe, expect, it, vi } from 'vitest';

import { activate, activates, enforceLicenses, isLicensed, verifyLicense } from '../src/index.js';
// the key of the demo application, signed by the private key of the server
import demoKey from '../../../demo-app/src/main/resources/META-INF/antilia-widgets.license?raw';

let keys: CryptoKeyPair;
let publicKey: string;

function base64url(bytes: Uint8Array): string {
	return btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_')
		.replace(/=+$/, '');
}

async function issue(payload: object, signer = keys.privateKey): Promise<string> {
	const bytes = new TextEncoder().encode(JSON.stringify(payload));
	const signature = new Uint8Array(await crypto.subtle.sign({ name: 'Ed25519' }, signer, bytes));
	return 'AW1.' + base64url(bytes) + '.' + base64url(signature);
}

beforeAll(async () => {
	keys = await crypto.subtle.generateKey({ name: 'Ed25519' }, true,
		['sign', 'verify']) as CryptoKeyPair;
	const spki = new Uint8Array(await crypto.subtle.exportKey('spki', keys.publicKey));
	publicKey = btoa(String.fromCharCode(...spki));
});

describe('license keys', () => {
	it('reads a key signed by the private key', async () => {
		const key = await issue({ licensee: 'ACME', expires: '2030-01-31',
			components: 'JsonBasedDynamicDataTable, Other' });

		expect(await verifyLicense(key, publicKey)).toEqual({ licensee: 'ACME',
			expires: '2030-01-31', components: ['JsonBasedDynamicDataTable', 'Other'] });
	});

	it('refuses a key signed by another key, changed, or no key at all', async () => {
		const other = await crypto.subtle.generateKey({ name: 'Ed25519' }, true,
			['sign', 'verify']) as CryptoKeyPair;
		const forged = await issue({ licensee: 'ACME', components: '*' }, other.privateKey);
		await expect(verifyLicense(forged, publicKey)).rejects.toThrow('signature');

		const key = await issue({ licensee: 'ACME', components: 'Other' });
		const [prefix, , signature] = key.split('.');
		const changed = prefix + '.' + base64url(new TextEncoder().encode(
			JSON.stringify({ licensee: 'ACME', components: '*' }))) + '.' + signature;
		await expect(verifyLicense(changed, publicKey)).rejects.toThrow('signature');

		await expect(verifyLicense('nonsense', publicKey)).rejects.toThrow('Not a license key');
		await expect(verifyLicense('AW1.abc', publicKey)).rejects.toThrow('payload and a signature');
	});

	it('activates the components it names until the day it expires', () => {
		const license = { licensee: 'ACME', expires: '2030-01-31',
			components: ['JsonBasedDynamicDataTable'] };

		expect(activates(license, ['DataTable', 'JsonBasedDynamicDataTable'],
			new Date(2030, 0, 31))).toBe(true);
		expect(activates(license, ['JsonBasedDynamicDataTable'], new Date(2030, 1, 1))).toBe(false);
		expect(activates(license, ['JsonBasedInfiniteList'], new Date(2030, 0, 1))).toBe(false);
		expect(activates({ ...license, expires: null, components: ['*'] }, ['Anything']))
			.toBe(true);
		expect(activates(null, ['JsonBasedDynamicDataTable'])).toBe(false);
	});

	it('checks the key of the demo application against the public key of the server', async () => {
		const key = demoKey;
		enforceLicenses();

		const license = await verifyLicense(key);
		expect(license.components).toContain('*');
		expect(await activate(key)).toEqual(license);
		expect(await isLicensed(['JsonBasedDynamicDataTable'])).toBe(true);
		expect(await isLicensed(['JsonBasedDynamicDataTable'], 'AW1.wrong.key')).toBe(false);
	});

	it('activates every component, with or without a key, while the keys are not enforced',
		async () => {
			enforceLicenses(false);
			await activate('AW1.wrong.key');

			expect(await isLicensed(['JsonBasedDynamicDataTable'])).toBe(true);
			expect(await isLicensed(['JsonBasedDynamicDataTable'], 'AW1.wrong.key')).toBe(true);
		});

	it('checks keys where Web Crypto has no Ed25519, as Firefox before 129', async () => {
		const importKey = vi.spyOn(crypto.subtle, 'importKey').mockRejectedValue(
			new DOMException('Ed25519 is not supported', 'NotSupportedError'));
		try {
			expect((await verifyLicense(demoKey)).components).toEqual(['*']);
			// a character in the middle of the signature: the last one holds padding bits too
			const key = demoKey.trim();
			const at = key.length - 10;
			const forged = key.substring(0, at) + (key.charAt(at) === 'A' ? 'B' : 'A') +
				key.substring(at + 1);
			await expect(verifyLicense(forged)).rejects.toThrow('signature');
			expect(importKey).toHaveBeenCalled();
		} finally {
			importKey.mockRestore();
		}
	});

	it('checks keys where the page has no Web Crypto, served over HTTP from another host', async () => {
		vi.stubGlobal('crypto', {});
		try {
			expect((await verifyLicense(demoKey)).components).toEqual(['*']);
			const key = demoKey.trim();
			const at = key.length - 10;
			const forged = key.substring(0, at) + (key.charAt(at) === 'A' ? 'B' : 'A') +
				key.substring(at + 1);
			await expect(verifyLicense(forged)).rejects.toThrow('signature');
		} finally {
			vi.unstubAllGlobals();
		}
	});
});
