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
package com.antilia.widgets.demo.account;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.apache.wicket.model.Model;
import org.junit.jupiter.api.Test;

/**
 * The image of the letters of the captcha: twice the size it is shown in, transparent around
 * letters of one color, dark or light after the theme, and drawn again only when the letters or
 * the theme change.
 */
class CaptchaImageTest
{
	/**
	 * @return the share of the pixels of an image that are transparent
	 */
	private static double transparent(BufferedImage image)
	{
		long count = 0;
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				if (image.getRGB(x, y) >>> 24 == 0)
				{
					count++;
				}
			}
		}
		return (double)count / (image.getWidth() * image.getHeight());
	}

	/**
	 * Asserts that the pixels that are fully opaque, those of the strokes of large letters, are of
	 * one color: the noise drawn over them changes it by a rounding at most.
	 */
	private static void assertInk(Color ink, BufferedImage image)
	{
		int opaque = 0;
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				Color color = new Color(image.getRGB(x, y), true);
				if (color.getAlpha() == 0xff)
				{
					opaque++;
					assertTrue(Math.abs(color.getRed() - ink.getRed()) <= 2 &&
						Math.abs(color.getGreen() - ink.getGreen()) <= 2 &&
						Math.abs(color.getBlue() - ink.getBlue()) <= 2,
						color + " at " + x + ", " + y);
				}
			}
		}
		assertTrue(opaque > image.getWidth() * image.getHeight() / 20, "opaque pixels " + opaque);
	}

	@Test
	void theLettersAreDrawnLargeOnATransparentBackground()
	{
		BufferedImage image = CaptchaImage.draw("HKWP7", false);

		assertEquals(CaptchaImage.WIDTH * CaptchaImage.SCALE, image.getWidth());
		assertEquals(CaptchaImage.HEIGHT * CaptchaImage.SCALE, image.getHeight());
		assertTrue(image.getColorModel().hasAlpha());
		double share = transparent(image);
		assertTrue(share > 0.5 && share < 0.95, "transparent " + share);
		assertInk(CaptchaImage.DARK_INK, image);
	}

	@Test
	void aDarkThemeDrawsLightLetters()
	{
		assertInk(CaptchaImage.LIGHT_INK, CaptchaImage.draw("M3XRT", true));
	}

	@Test
	void theImageIsDrawnAgainOnlyForOtherLettersOrAnotherTheme() throws IOException
	{
		Model<String> letters = Model.of("A9ECY");
		Model<Boolean> dark = Model.of(false);
		CaptchaImage image = new CaptchaImage(letters, dark);

		byte[] first = image.getImageData(null);
		assertSame(first, image.getImageData(null));
		BufferedImage png = ImageIO.read(new ByteArrayInputStream(first));
		assertEquals(CaptchaImage.WIDTH * CaptchaImage.SCALE, png.getWidth());
		assertInk(CaptchaImage.DARK_INK, png);

		dark.setObject(true);
		byte[] inDark = image.getImageData(null);
		assertFalse(Arrays.equals(first, inDark));
		assertInk(CaptchaImage.LIGHT_INK, ImageIO.read(new ByteArrayInputStream(inDark)));

		letters.setObject("UJ4NF");
		byte[] other = image.getImageData(null);
		assertFalse(Arrays.equals(inDark, other));
		assertArrayEquals(other, image.getImageData(null));
	}
}
