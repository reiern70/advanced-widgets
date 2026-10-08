/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Verifies Ed25519 signatures (RFC 8032, section 5.1.7) for browsers whose Web Crypto has no
 * Ed25519 yet, such as Firefox before 129: the curve arithmetic in BigInt, the hash with the
 * SHA-512 of Web Crypto. It only verifies, with public data, so it needs no constant time.
 */

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
	const k = mod(littleEndian(new Uint8Array(await crypto.subtle.digest('SHA-512', hashed))), L);
	// [S]B = R + [k]A, that is [S]B - [k]A = R
	const left = add(multiply(BASE, s), multiply([mod(-a[0]), a[1], a[2], mod(-a[3])], k));
	const expected = encode(left);
	return expected.every((byte, index) => byte === r[index]);
}
