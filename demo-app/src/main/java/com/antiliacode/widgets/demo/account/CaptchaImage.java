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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Random;

import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.DynamicImageResource;

/**
 * The image of the letters of a captcha, drawn to be read at the first try: large bold letters,
 * each a little turned, moved and slanted, the whole row bent by a soft wave, with two thin
 * curves through it and a few dots, enough to keep out robots that read the text of a page but
 * not a person. Its background is transparent, so the image takes the surface of the page, and
 * the letters are dark, or light in a dark theme. Drawn at twice the size it is shown in
 * ({@value #WIDTH} by {@value #HEIGHT} pixels), so that it stays sharp on screens of a high
 * density.
 */
public class CaptchaImage extends DynamicImageResource
{
	private static final long serialVersionUID = 1L;

	/** The width the image is shown in, in CSS pixels. */
	public static final int WIDTH = 240;

	/** The height the image is shown in, in CSS pixels. */
	public static final int HEIGHT = 80;

	/** How much larger than shown the image is drawn. */
	static final int SCALE = 2;

	/** The letters, in the color of the text of the light themes. */
	static final Color DARK_INK = new Color(0x1c, 0x23, 0x33);

	/** The letters in a dark theme. */
	static final Color LIGHT_INK = new Color(0xe6, 0xe9, 0xf0);

	private static final Random RANDOM = new SecureRandom();

	private final IModel<String> letters;

	private final IModel<Boolean> dark;

	/** The image last drawn, kept until the letters or the theme change. */
	private transient SoftReference<byte[]> drawn;

	/** The letters and the theme the image last drawn is for. */
	private String drawnFor;

	/**
	 * @param letters
	 *            the letters to draw
	 * @param dark
	 *            whether the page is in a dark theme
	 */
	public CaptchaImage(IModel<String> letters, IModel<Boolean> dark)
	{
		this.letters = letters;
		this.dark = dark;
	}

	@Override
	protected byte[] getImageData(Attributes attributes)
	{
		boolean inDark = Boolean.TRUE.equals(dark.getObject());
		String key = letters.getObject() + (inDark ? "/dark" : "/light");
		byte[] data = drawn == null || !key.equals(drawnFor) ? null : drawn.get();
		if (data == null)
		{
			data = toImageData(draw(letters.getObject(), inDark));
			drawn = new SoftReference<>(data);
			drawnFor = key;
			setLastModifiedTime(Instant.now());
		}
		return data;
	}

	/**
	 * Draws letters.
	 *
	 * @param text
	 *            the letters
	 * @param inDark
	 *            whether the page is in a dark theme, which draws them light
	 * @return the image, {@value #SCALE} times the size it is shown in
	 */
	static BufferedImage draw(String text, boolean inDark)
	{
		int width = WIDTH * SCALE;
		int height = HEIGHT * SCALE;
		Color ink = inDark ? LIGHT_INK : DARK_INK;

		BufferedImage row = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = row.createGraphics();
		smooth(g);
		g.setColor(ink);
		drawLetters(g, text, width, height);
		g.dispose();

		BufferedImage image = bend(row, ink);
		g = image.createGraphics();
		smooth(g);
		drawNoise(g, ink, width, height);
		g.dispose();
		return image;
	}

	private static void smooth(Graphics2D g)
	{
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}

	/**
	 * Draws each letter in a cell of its own, centered in it and a little turned, moved, slanted
	 * and sized, never so much that two letters touch.
	 */
	private static void drawLetters(Graphics2D g, String text, int width, int height)
	{
		int margin = 14 * SCALE;
		double cell = (width - 2.0 * margin) / Math.max(text.length(), 1);
		Font font = new Font(Font.SANS_SERIF, Font.BOLD, 40 * SCALE);
		FontRenderContext context = g.getFontRenderContext();
		for (int i = 0; i < text.length(); i++)
		{
			GlyphVector glyph = font.createGlyphVector(context, text.substring(i, i + 1));
			Shape outline = glyph.getOutline();
			Rectangle2D bounds = outline.getBounds2D();

			AffineTransform at = new AffineTransform();
			at.translate(margin + cell * (i + 0.5) + between(-4, 4) * SCALE,
				height / 2.0 + between(-6, 6) * SCALE);
			at.rotate(Math.toRadians(between(-14, 14)));
			at.shear(between(-0.15, 0.15), 0);
			double size = between(0.9, 1.08);
			at.scale(size, size);
			at.translate(-bounds.getCenterX(), -bounds.getCenterY());
			g.fill(at.createTransformedShape(outline));
		}
	}

	/**
	 * Bends the row of letters with a soft wave, moving each column of pixels up or down by a
	 * fraction of a pixel too, so that the edges of the letters stay smooth. The letters are all
	 * of one color: only how opaque each pixel is moves.
	 */
	private static BufferedImage bend(BufferedImage row, Color ink)
	{
		int width = row.getWidth();
		int height = row.getHeight();
		double amplitude = between(3, 5) * SCALE;
		double length = between(150, 210) * SCALE;
		double phase = between(0, 2 * Math.PI);
		int color = ink.getRGB() & 0xffffff;
		BufferedImage bent = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < width; x++)
		{
			double dy = amplitude * Math.sin(2 * Math.PI * x / length + phase);
			for (int y = 0; y < height; y++)
			{
				double from = y - dy;
				int above = (int)Math.floor(from);
				double share = from - above;
				double alpha = (1 - share) * alpha(row, x, above) +
					share * alpha(row, x, above + 1);
				if (alpha > 0)
				{
					bent.setRGB(x, y, (int)Math.round(alpha) << 24 | color);
				}
			}
		}
		return bent;
	}

	private static int alpha(BufferedImage image, int x, int y)
	{
		return y < 0 || y >= image.getHeight() ? 0 : image.getRGB(x, y) >>> 24;
	}

	/**
	 * Draws two thin curves across the letters and a few dots around them, in the color of the
	 * letters but fainter.
	 */
	private static void drawNoise(Graphics2D g, Color ink, int width, int height)
	{
		g.setStroke(new BasicStroke(1.6f * SCALE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 150));
		for (int i = 0; i < 2; i++)
		{
			g.draw(new CubicCurve2D.Double(0, height * between(0.3, 0.7),
				width * between(0.2, 0.4), height * between(0.1, 0.9),
				width * between(0.6, 0.8), height * between(0.1, 0.9),
				width, height * between(0.3, 0.7)));
		}
		g.setColor(new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 90));
		for (int i = 0; i < 40; i++)
		{
			double r = between(1, 2.2) * SCALE;
			g.fill(new Ellipse2D.Double(between(0, width), between(0, height), r, r));
		}
	}

	private static double between(double min, double max)
	{
		return min + RANDOM.nextDouble() * (max - min);
	}
}
