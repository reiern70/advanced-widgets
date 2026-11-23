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
package com.antiliacode.widgets.demo.account;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.apache.wicket.WicketRuntimeException;

/**
 * The letters of a captcha, carried by the page and the URL of their image instead of a session:
 * a token holding them, with when they expire and an id of their own, encrypted and signed with
 * AES-GCM under a key made anew at every start of the server, so that only the server reads it
 * and nobody makes one. A token answers once, right or wrong, within {@link #VALID}: the ids of
 * the tokens answered are kept in memory until they expire, so that a robot cannot try the same
 * letters again and again, nor send a form solved once again.
 */
public final class CaptchaToken
{
	/** The letters: none that look like another, as 0 and O, 1 and I, or 5 and S. */
	static final String LETTERS = "ACDEFHJKLMNPRTUWXY346789";

	/** How many letters a captcha has. */
	static final int LENGTH = 5;

	/** How long a token may be answered. */
	public static final Duration VALID = Duration.ofMinutes(10);

	/** The most ids of tokens answered kept: beyond, the oldest go, and they expire anyway. */
	static final int MAXIMUM_ANSWERED = 100_000;

	private static final int ID_BYTES = 8;

	private static final int IV_BYTES = 12;

	private static final int TAG_BITS = 128;

	private static final SecureRandom RANDOM = new SecureRandom();

	private static final SecretKey KEY = newKey();

	/** The ids of the tokens answered, and when each expires. */
	private static final Map<String, Instant> ANSWERED = new LinkedHashMap<>();

	private CaptchaToken()
	{
	}

	/**
	 * @param now
	 *            the time on the server's clock
	 * @return a token of new letters, which expires {@link #VALID} later
	 */
	public static String issue(Instant now)
	{
		StringBuilder letters = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++)
		{
			letters.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
		}
		return issue(letters.toString(), now);
	}

	/**
	 * @param letters
	 *            the letters, for tests
	 * @param now
	 *            the time on the server's clock
	 * @return a token of the letters, which expires {@link #VALID} later
	 */
	static String issue(String letters, Instant now)
	{
		byte[] id = new byte[ID_BYTES];
		RANDOM.nextBytes(id);
		byte[] text = letters.getBytes(StandardCharsets.US_ASCII);
		ByteBuffer plain = ByteBuffer.allocate(ID_BYTES + Long.BYTES + text.length)
			.put(id)
			.putLong(now.plus(VALID).getEpochSecond())
			.put(text);
		byte[] iv = new byte[IV_BYTES];
		RANDOM.nextBytes(iv);
		try
		{
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, KEY, new GCMParameterSpec(TAG_BITS, iv));
			byte[] sealed = cipher.doFinal(plain.array());
			return Base64.getUrlEncoder().withoutPadding().encodeToString(
				ByteBuffer.allocate(IV_BYTES + sealed.length).put(iv).put(sealed).array());
		}
		catch (GeneralSecurityException e)
		{
			throw new WicketRuntimeException(e);
		}
	}

	/**
	 * @param token
	 *            a token, as a page or a URL sent it; may be {@code null}
	 * @param now
	 *            the time on the server's clock
	 * @return the letters of the token, if it is one the server made and it has not expired; it
	 *         may have been answered
	 */
	public static Optional<String> letters(String token, Instant now)
	{
		return open(token, now).map(Opened::letters);
	}

	/**
	 * Answers a token, which then answers no more.
	 *
	 * @param token
	 *            the token the form sent, may be {@code null}
	 * @param typed
	 *            the letters typed, in capitals or not, with spaces around them or not
	 * @param now
	 *            the time on the server's clock
	 * @return whether the letters typed are those of the token, which the server made, has not
	 *         expired and was not answered before
	 */
	public static boolean answer(String token, String typed, Instant now)
	{
		Optional<Opened> opened = open(token, now);
		if (opened.isEmpty() || !firstAnswer(opened.get(), now))
		{
			return false;
		}
		return typed != null && opened.get().letters().equals(typed.strip().toUpperCase(Locale.ROOT));
	}

	/**
	 * Notes that a token was answered.
	 *
	 * @return whether it was not answered before
	 */
	private static boolean firstAnswer(Opened opened, Instant now)
	{
		synchronized (ANSWERED)
		{
			for (Iterator<Instant> expires = ANSWERED.values().iterator(); expires.hasNext();)
			{
				// in the order answered, nearly that of expiring: stop at the first still valid
				if (expires.next().isAfter(now))
				{
					break;
				}
				expires.remove();
			}
			if (ANSWERED.containsKey(opened.id()))
			{
				return false;
			}
			if (ANSWERED.size() >= MAXIMUM_ANSWERED)
			{
				Iterator<String> oldest = ANSWERED.keySet().iterator();
				oldest.next();
				oldest.remove();
			}
			ANSWERED.put(opened.id(), opened.expires());
			return true;
		}
	}

	/**
	 * What a token holds.
	 */
	private record Opened(String id, Instant expires, String letters)
	{
	}

	private static Optional<Opened> open(String token, Instant now)
	{
		if (token == null || token.length() > 200)
		{
			return Optional.empty();
		}
		try
		{
			ByteBuffer sealed = ByteBuffer.wrap(Base64.getUrlDecoder().decode(token));
			if (sealed.remaining() <= IV_BYTES)
			{
				return Optional.empty();
			}
			byte[] iv = new byte[IV_BYTES];
			sealed.get(iv);
			byte[] rest = new byte[sealed.remaining()];
			sealed.get(rest);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, KEY, new GCMParameterSpec(TAG_BITS, iv));
			ByteBuffer plain = ByteBuffer.wrap(cipher.doFinal(rest));
			byte[] id = new byte[ID_BYTES];
			plain.get(id);
			Instant expires = Instant.ofEpochSecond(plain.getLong());
			byte[] letters = new byte[plain.remaining()];
			plain.get(letters);
			if (!expires.isAfter(now))
			{
				return Optional.empty();
			}
			return Optional.of(new Opened(HexFormat.of().formatHex(id), expires,
				new String(letters, StandardCharsets.US_ASCII)));
		}
		catch (IllegalArgumentException | GeneralSecurityException | BufferUnderflowException e)
		{
			// not base 64, not made by this server, or changed
			return Optional.empty();
		}
	}

	private static SecretKey newKey()
	{
		try
		{
			KeyGenerator generator = KeyGenerator.getInstance("AES");
			generator.init(256, RANDOM);
			return generator.generateKey();
		}
		catch (GeneralSecurityException e)
		{
			throw new WicketRuntimeException(e);
		}
	}
}
