/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.license;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

/**
 * A license key activating the private components of ui-components.
 * <p>
 * A key is {@code AW1.<payload>.<signature>}, both parts in URL-safe Base64: the payload is a JSON
 * object naming the licensee, the day the key expires (none for a key that does not) and the
 * components it activates ({@code *} for all, or the simple names of their classes separated by
 * commas); the signature is the Ed25519 signature of the payload. The key is checked offline
 * against {@link #PUBLIC_KEY}; only the holder of the private key can issue keys.
 *
 * @since 11.0.0
 */
public final class LicenseKey implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The start of every key, naming its format. */
	public static final String PREFIX = "AW1.";

	/** The public key checking the signatures, X.509 encoded in Base64. */
	static final String PUBLIC_KEY = "MCowBQYDK2VwAyEACHyUV4LF2hZ0mpwS4Z90Oi5oXDKmPVwQHxRAFu757F4=";

	private static final String ALL = "*";

	private final String licensee;

	private final LocalDate expires;

	private final Set<String> components;

	private LicenseKey(String licensee, LocalDate expires, Set<String> components)
	{
		this.licensee = licensee;
		this.expires = expires;
		this.components = components;
	}

	/**
	 * Reads and checks a key.
	 *
	 * @param key
	 *            the text of the key
	 * @return the key
	 * @throws InvalidLicenseKeyException
	 *             if the text is not a key or its signature is wrong
	 */
	public static LicenseKey parse(String key)
	{
		return parse(key, PUBLIC_KEY);
	}

	/**
	 * Reads and checks a key against a public key, for tests.
	 */
	static LicenseKey parse(String key, String publicKey)
	{
		if (key == null)
		{
			throw new InvalidLicenseKeyException("No license key");
		}
		String text = key.strip();
		if (!text.startsWith(PREFIX))
		{
			throw new InvalidLicenseKeyException("Not a license key of ui-components");
		}
		String[] parts = text.substring(PREFIX.length()).split("\\.");
		if (parts.length != 2)
		{
			throw new InvalidLicenseKeyException("A license key has a payload and a signature");
		}
		byte[] payload;
		byte[] signature;
		try
		{
			payload = Base64.getUrlDecoder().decode(parts[0]);
			signature = Base64.getUrlDecoder().decode(parts[1]);
		}
		catch (IllegalArgumentException e)
		{
			throw new InvalidLicenseKeyException("The license key is not Base64", e);
		}
		if (!verify(payload, signature, publicKey))
		{
			throw new InvalidLicenseKeyException("The signature of the license key is wrong");
		}
		try
		{
			JSONObject json = new JSONObject(new String(payload, StandardCharsets.UTF_8));
			String expires = json.optString("expires", null);
			return new LicenseKey(json.getString("licensee"),
				expires != null ? LocalDate.parse(expires) : null,
				Arrays.stream(json.getString("components").split(","))
					.map(String::strip)
					.filter(name -> !name.isEmpty())
					.collect(Collectors.toUnmodifiableSet()));
		}
		catch (JSONException | DateTimeParseException e)
		{
			throw new InvalidLicenseKeyException("The payload of the license key is wrong", e);
		}
	}

	private static boolean verify(byte[] payload, byte[] signature, String publicKey)
	{
		try
		{
			PublicKey key = KeyFactory.getInstance("Ed25519")
				.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
			Signature verifier = Signature.getInstance("Ed25519");
			verifier.initVerify(key);
			verifier.update(payload);
			return verifier.verify(signature);
		}
		catch (GeneralSecurityException | IllegalArgumentException e)
		{
			return false;
		}
	}

	/**
	 * @return whom the key was issued to
	 */
	public String getLicensee()
	{
		return licensee;
	}

	/**
	 * @return the last day the key is valid on, {@code null} if it does not expire
	 */
	public LocalDate getExpires()
	{
		return expires;
	}

	/**
	 * @return whether the key is valid on the day
	 */
	public boolean isValidOn(LocalDate day)
	{
		return expires == null || !day.isAfter(expires);
	}

	/**
	 * @return whether the key activates the component class: all of them, or one of its classes
	 *         by simple name
	 */
	public boolean activates(Class<?> componentClass)
	{
		if (components.contains(ALL))
		{
			return true;
		}
		for (Class<?> type = componentClass; type != null; type = type.getSuperclass())
		{
			if (components.contains(type.getSimpleName()))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString()
	{
		return "LicenseKey[" + licensee + (expires != null ? ", until " + expires : "") + "]";
	}
}
