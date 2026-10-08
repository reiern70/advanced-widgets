/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Verifies Ed25519 signatures (RFC 8032, section 5.1.7) for browsers whose Web Crypto has no
 * Ed25519 yet, such as Firefox before 129, or no Web Crypto at all, as in a page served over
 * plain HTTP from another host than localhost: the curve arithmetic in BigInt, the hash with the
 * SHA-512 of Web Crypto, or of its own (FIPS 180-4) where there is none. It only verifies, with
 * public data, so it needs no constant time.
 */

const MASK_64 = (1n << 64n) - 1n;

/** The round constants of SHA-512. */
const K_512 = [
	'428a2f98d728ae22', '7137449123ef65cd', 'b5c0fbcfec4d3b2f', 'e9b5dba58189dbbc',
	'3956c25bf348b538', '59f111f1b605d019', '923f82a4af194f9b', 'ab1c5ed5da6d8118',
	'd807aa98a3030242', '12835b0145706fbe', '243185be4ee4b28c', '550c7dc3d5ffb4e2',
	'72be5d74f27b896f', '80deb1fe3b1696b1', '9bdc06a725c71235', 'c19bf174cf692694',
	'e49b69c19ef14ad2', 'efbe4786384f25e3', '0fc19dc68b8cd5b5', '240ca1cc77ac9c65',
	'2de92c6f592b0275', '4a7484aa6ea6e483', '5cb0a9dcbd41fbd4', '76f988da831153b5',
	'983e5152ee66dfab', 'a831c66d2db43210', 'b00327c898fb213f', 'bf597fc7beef0ee4',
	'c6e00bf33da88fc2', 'd5a79147930aa725', '06ca6351e003826f', '142929670a0e6e70',
	'27b70a8546d22ffc', '2e1b21385c26c926', '4d2c6dfc5ac42aed', '53380d139d95b3df',
	'650a73548baf63de', '766a0abb3c77b2a8', '81c2c92e47edaee6', '92722c851482353b',
	'a2bfe8a14cf10364', 'a81a664bbc423001', 'c24b8b70d0f89791', 'c76c51a30654be30',
	'd192e819d6ef5218', 'd69906245565a910', 'f40e35855771202a', '106aa07032bbd1b8',
	'19a4c116b8d2d0c8', '1e376c085141ab53', '2748774cdf8eeb99', '34b0bcb5e19b48a8',
	'391c0cb3c5c95a63', '4ed8aa4ae3418acb', '5b9cca4f7763e373', '682e6ff3d6b2b8a3',
	'748f82ee5defb2fc', '78a5636f43172f60', '84c87814a1f0ab72', '8cc702081a6439ec',
	'90befffa23631e28', 'a4506cebde82bde9', 'bef9a3f7b2c67915', 'c67178f2e372532b',
	'ca273eceea26619c', 'd186b8c721c0c207', 'eada7dd6cde0eb1e', 'f57d4f7fee6ed178',
	'06f067aa72176fba', '0a637dc5a2c898a6', '113f9804bef90dae', '1b710b35131c471b',
	'28db77f523047d84', '32caab7b40c72493', '3c9ebe0a15c9bebc', '431d67c49c100d4c',
	'4cc5d4becb3e42b6', '597f299cfc657e2a', '5fcb6fab3ad6faec', '6c44198c4a475817'
].map((hex) => BigInt('0x' + hex));

/** The initial hash value of SHA-512. */
const H_512 = ['6a09e667f3bcc908', 'bb67ae8584caa73b', '3c6ef372fe94f82b', 'a54ff53a5f1d36f1',
	'510e527fade682d1', '9b05688c2b3e6c1f', '1f83d9abfb41bd6b', '5be0cd19137e2179']
	.map((hex) => BigInt('0x' + hex));

function rotateRight(value: bigint, bits: bigint): bigint {
	return ((value >> bits) | (value << (64n - bits))) & MASK_64;
}

/**
 * @returns the SHA-512 of the data, in plain BigInt: slow, but the data of a license key is short
 */
export function sha512Of(data: Uint8Array): Uint8Array {
	const length = data.length;
	const padded = new Uint8Array(Math.ceil((length + 17) / 128) * 128);
	padded.set(data);
	padded[length] = 0x80;
	const bits = BigInt(length) * 8n;
	for (let i = 0; i < 16; i++) {
		padded[padded.length - 1 - i] = Number((bits >> BigInt(8 * i)) & 0xffn);
	}
	const h = [...H_512];
	const w = new Array<bigint>(80);
	for (let block = 0; block < padded.length; block += 128) {
		for (let t = 0; t < 16; t++) {
			let word = 0n;
			for (let b = 0; b < 8; b++) {
				word = (word << 8n) | BigInt(padded[block + t * 8 + b]);
			}
			w[t] = word;
		}
		for (let t = 16; t < 80; t++) {
			const s0 = rotateRight(w[t - 15], 1n) ^ rotateRight(w[t - 15], 8n) ^ (w[t - 15] >> 7n);
			const s1 = rotateRight(w[t - 2], 19n) ^ rotateRight(w[t - 2], 61n) ^ (w[t - 2] >> 6n);
			w[t] = (w[t - 16] + s0 + w[t - 7] + s1) & MASK_64;
		}
		let [a, b, c, d, e, f, g, hh] = h;
		for (let t = 0; t < 80; t++) {
			const sum1 = rotateRight(e, 14n) ^ rotateRight(e, 18n) ^ rotateRight(e, 41n);
			const choice = (e & f) ^ (~e & MASK_64 & g);
			const t1 = (hh + sum1 + choice + K_512[t] + w[t]) & MASK_64;
			const sum0 = rotateRight(a, 28n) ^ rotateRight(a, 34n) ^ rotateRight(a, 39n);
			const majority = (a & b) ^ (a & c) ^ (b & c);
			const t2 = (sum0 + majority) & MASK_64;
			hh = g;
			g = f;
			f = e;
			e = (d + t1) & MASK_64;
			d = c;
			c = b;
			b = a;
			a = (t1 + t2) & MASK_64;
		}
		[a, b, c, d, e, f, g, hh].forEach((value, i) => {
			h[i] = (h[i] + value) & MASK_64;
		});
	}
	const digest = new Uint8Array(64);
	h.forEach((value, i) => {
		for (let b = 0; b < 8; b++) {
			digest[i * 8 + b] = Number((value >> BigInt(8 * (7 - b))) & 0xffn);
		}
	});
	return digest;
}

/**
 * @returns the SHA-512 of the data: Web Crypto's, or our own where the page has no Web Crypto
 */
async function sha512(data: Uint8Array<ArrayBuffer>): Promise<Uint8Array> {
	if (typeof crypto !== 'undefined' && crypto.subtle) {
		return new Uint8Array(await crypto.subtle.digest('SHA-512', data));
	}
	return sha512Of(data);
}

/** The prime of the field. */
const P = 2n ** 255n - 19n;

/** The order of the base point. */
const L = 2n ** 252n + 27742317777372353535851937790883648493n;

/** A point in extended coordinates: x = X/Z, y = Y/Z, x*y = T/Z. */
type Point = [bigint, bigint, bigint, bigint];

function mod(value: bigint, modulus = P): bigint {
	const rest = value % modulus;
	return rest >= 0n ? rest : rest + modulus;
}

function pow(base: bigint, exponent: bigint): bigint {
	let result = 1n;
	let square = mod(base);
	for (let e = exponent; e > 0n; e >>= 1n) {
		if (e & 1n) {
			result = mod(result * square);
		}
		square = mod(square * square);
	}
	return result;
}

function inverse(value: bigint): bigint {
	return pow(value, P - 2n);
}

/** The constant of the curve, -121665/121666. */
const D = mod(-121665n * inverse(121666n));

/** The square root of -1. */
const SQRT_M1 = pow(2n, (P - 1n) / 4n);

const BASE: Point = (() => {
	const y = mod(4n * inverse(5n));
	const x = 15112221349535400772501151409588531511454012693041857206046113283949847762202n;
	return [x, y, 1n, mod(x * y)];
})();

const ZERO: Point = [0n, 1n, 1n, 0n];

function add([x1, y1, z1, t1]: Point, [x2, y2, z2, t2]: Point): Point {
	const a = mod((y1 - x1) * (y2 - x2));
	const b = mod((y1 + x1) * (y2 + x2));
	const c = mod(t1 * 2n * D * t2);
	const d = mod(z1 * 2n * z2);
	const e = b - a;
	const f = d - c;
	const g = d + c;
	const h = b + a;
	return [mod(e * f), mod(g * h), mod(f * g), mod(e * h)];
}

function multiply(point: Point, scalar: bigint): Point {
	let result = ZERO;
	let addend = point;
	for (let s = scalar; s > 0n; s >>= 1n) {
		if (s & 1n) {
			result = add(result, addend);
		}
		addend = add(addend, addend);
	}
	return result;
}

function littleEndian(bytes: Uint8Array): bigint {
	let value = 0n;
	for (let i = bytes.length - 1; i >= 0; i--) {
		value = (value << 8n) | BigInt(bytes[i]);
	}
	return value;
}

/**
 * @returns the point the 32 bytes encode, or null if they encode none
 */
function decode(bytes: Uint8Array): Point | null {
	if (bytes.length !== 32) {
		return null;
	}
	const sign = bytes[31] >> 7;
	const copy = Uint8Array.from(bytes);
	copy[31] &= 0x7f;
	const y = littleEndian(copy);
	if (y >= P) {
		return null;
	}
	const y2 = mod(y * y);
	const u = mod(y2 - 1n);
	const v = mod(D * y2 + 1n);
	let x = mod(u * pow(v, 3n) * pow(u * pow(v, 7n), (P - 5n) / 8n));
	const check = mod(v * x * x);
	if (check === mod(-u)) {
		x = mod(x * SQRT_M1);
	} else if (check !== u) {
		return null;
	}
	if (x === 0n && sign === 1) {
		return null;
	}
	if (Number(x & 1n) !== sign) {
		x = P - x;
	}
	return [x, y, 1n, mod(x * y)];
}

function encode([x, y, z]: Point): Uint8Array {
	const zi = inverse(z);
	const ax = mod(x * zi);
	let ay = mod(y * zi);
	const bytes = new Uint8Array(32);
	for (let i = 0; i < 32; i++) {
		bytes[i] = Number(ay & 0xffn);
		ay >>= 8n;
	}
	bytes[31] |= Number(ax & 1n) << 7;
	return bytes;
}

/**
 * @param publicKey the 32 bytes of the public key
 * @param signature the 64 bytes of the signature
 * @param message the message signed
 * @returns whether the signature of the message is right
 */
export async function verifyEd25519(publicKey: Uint8Array, signature: Uint8Array,
	message: Uint8Array): Promise<boolean> {
	if (signature.length !== 64) {
		return false;
	}
	const a = decode(publicKey);
	const r = signature.subarray(0, 32);
	const s = littleEndian(signature.subarray(32));
	if (!a || !decode(r) || s >= L) {
		return false;
	}
	const hashed = new Uint8Array(r.length + publicKey.length + message.length);
	hashed.set(r, 0);
	hashed.set(publicKey, r.length);
	hashed.set(message, r.length + publicKey.length);
	const k = mod(littleEndian(await sha512(hashed)), L);
	// [S]B = R + [k]A, that is [S]B - [k]A = R
	const left = add(multiply(BASE, s), multiply([mod(-a[0]), a[1], a[2], mod(-a[3])], k));
	const expected = encode(left);
	return expected.every((byte, index) => byte === r[index]);
}
