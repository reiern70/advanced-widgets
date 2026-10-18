/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.license;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import com.github.openjson.JSONObject;

/**
 * Issues license keys; kept with the tests so that it never ships in the jar. The private key
 * signing the keys must stay out of the repository.
 * <p>
 * Create the key pair once, then put the printed public key into {@link LicenseKey#PUBLIC_KEY}:
 *
 * <pre>
 * java LicenseKeyTool.java keypair &lt;private key file&gt;
 * </pre>
 *
 * Issue a key, valid until a day ({@code -} for no end) for all components ({@code *}) or a
 * comma separated list of class names:
 *
 * <pre>
 * java LicenseKeyTool.java sign &lt;private key file&gt; &lt;licensee&gt; &lt;expires|-&gt; &lt;components&gt;
 * </pre>
 *
 * Both run with the test classpath of ui-components.
 */
public final class LicenseKeyTool
{
	private LicenseKeyTool()
	{
	}

	public static void main(String[] args) throws Exception
	{
		if (args.length == 2 && "keypair".equals(args[0]))
		{
			KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
			Path file = Path.of(args[1]);
			if (Files.exists(file))
			{
				throw new IllegalStateException(file + " exists already; not overwriting it");
			}
			Files.writeString(file,
				Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()) + "\n");
			System.out.println("Private key written to " + file + "; keep it secret.");
			System.out.println("Public key for LicenseKey.PUBLIC_KEY:");
			System.out.println(Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
		}
		else if (args.length == 5 && "sign".equals(args[0]))
		{
			System.out.println(sign(readPrivateKey(Path.of(args[1])), args[2],
				"-".equals(args[3]) ? null : args[3], args[4]));
		}
		else
		{
			System.err.println("usage: keypair <private key file> | " +
				"sign <private key file> <licensee> <expires|-> <components>");
			System.exit(2);
		}
	}

	/**
	 * @return a license key signed with the private key
	 */
	static String sign(PrivateKey key, String licensee, String expires, String components)
		throws Exception
	{
		JSONObject payload = new JSONObject();
		payload.put("licensee", licensee);
		if (expires != null)
		{
			payload.put("expires", expires);
		}
		payload.put("components", components);
		byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
		Signature signature = Signature.getInstance("Ed25519");
		signature.initSign(key);
		signature.update(bytes);
		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		return LicenseKey.PREFIX + encoder.encodeToString(bytes) + "." +
			encoder.encodeToString(signature.sign());
	}

	static PrivateKey readPrivateKey(Path file) throws Exception
	{
		byte[] encoded = Base64.getDecoder().decode(Files.readString(file).trim());
		return KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(encoded));
	}
}
