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
package com.antilia.widgets.demo.download;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.wicket.util.string.Strings;

import com.antilia.widgets.demo.Countries;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The report the downloads demo hands out, made when it is asked for: the countries of the world,
 * by their ISO code and their name in a language, sorted by name, under a title. It is written in
 * the format of each {@link DemoFile}: a text, CSV, JSON, a chart of the countries by the initial
 * of their names as SVG and as PNG, a PDF, and a ZIP archive of all of them. The texts of the
 * files come from the bundle of the package, in the language of the report.
 *
 * @param title
 *            the title of the report
 * @param rows
 *            how many countries the report lists, the first ones by name
 * @param locale
 *            the language of the names and of the texts
 */
record CountriesReport(String title, int rows, Locale locale)
{
	/** How many countries there are, all a report can list. */
	static final int ALL = Locale.getISOCountries().length;

	/** The color of the bars of the chart, the primary color of the default theme. */
	private static final Color BAR = new Color(0xc2410c);

	/** The color of the texts of the chart. */
	private static final Color INK = new Color(0x2b1d12);

	/** The width of a bar of the chart, and of the room between two. */
	private static final int BAR_WIDTH = 22;

	private static final int GAP = 8;

	/** The height of the tallest bar. */
	private static final int CHART_HEIGHT = 220;

	/** The lines of a page of the PDF, below its title. */
	private static final int LINES_PER_PAGE = 48;

	/**
	 * @return the report of all countries under the default title, in a language
	 */
	static CountriesReport of(Locale locale)
	{
		return new CountriesReport(DownloadTexts.text("file.title", locale), ALL, locale);
	}

	/**
	 * @return the countries the report lists, each its code and its name, sorted by name
	 */
	List<String[]> countries()
	{
		Collator collator = Collator.getInstance(locale);
		return Arrays.stream(Locale.getISOCountries())
			.map(code -> new String[] { code, Countries.name(code, locale) })
			.sorted(Comparator.comparing((String[] country) -> country[1], collator))
			.limit(Math.max(0, rows))
			.toList();
	}

	private String text(String key)
	{
		return DownloadTexts.text(key, locale);
	}

	/**
	 * @return the report as a text: the title, underlined, then a line per country, its code and
	 *         its name
	 */
	byte[] text()
	{
		String code = text("file.code");
		String line = "%-" + (Math.max(code.length(), 2) + 2) + "s%s\n";
		StringBuilder text = new StringBuilder();
		text.append(title).append('\n').append("=".repeat(title.length())).append("\n\n");
		text.append(String.format(line, code, text("file.name")));
		for (String[] country : countries())
		{
			text.append(String.format(line, country[0], country[1]));
		}
		return text.toString().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * @return the report as CSV: a header, then a line per country, every field quoted
	 */
	byte[] csv()
	{
		StringBuilder csv = new StringBuilder();
		csv.append(quote(text("file.code"))).append(',').append(quote(text("file.name")))
			.append("\r\n");
		for (String[] country : countries())
		{
			csv.append(quote(country[0])).append(',').append(quote(country[1])).append("\r\n");
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static String quote(String value)
	{
		return '"' + value.replace("\"", "\"\"") + '"';
	}

	/**
	 * @return the report as JSON: its title and its countries, each its code and its name
	 */
	byte[] json()
	{
		JSONArray countries = new JSONArray();
		for (String[] country : countries())
		{
			countries.put(new JSONObject().put("code", country[0]).put("name", country[1]));
		}
		return new JSONObject().put("title", title).put("countries", countries).toString(2)
			.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * @return how many countries the report has per initial of their names, without accents, by
	 *         initial
	 */
	Map<String, Integer> initials()
	{
		Map<String, Integer> initials = new TreeMap<>(Collator.getInstance(locale));
		for (String[] country : countries())
		{
			String initial = Normalizer.normalize(country[1].substring(0, 1), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "").toUpperCase(locale);
			initials.merge(initial, 1, Integer::sum);
		}
		return initials;
	}

	private static int max(Map<String, Integer> initials)
	{
		return initials.values().stream().mapToInt(Integer::intValue).max().orElse(1);
	}

	/**
	 * @return the chart of the countries by the initial of their names as SVG: the title, then a
	 *         bar per initial, its count above and the initial below
	 */
	byte[] svg()
	{
		Map<String, Integer> initials = initials();
		int max = max(initials);
		int width = Math.max(360, 48 + initials.size() * (BAR_WIDTH + GAP));
		int top = 84;
		int height = top + CHART_HEIGHT + 40;
		StringBuilder svg = new StringBuilder();
		svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
			.append("\" height=\"").append(height).append("\" viewBox=\"0 0 ").append(width)
			.append(' ').append(height).append("\" font-family=\"system-ui, sans-serif\">\n");
		svg.append("<title>").append(escape(title)).append("</title>\n");
		svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");
		svg.append("<text x=\"24\" y=\"36\" font-size=\"20\" font-weight=\"600\" fill=\"#2b1d12\">")
			.append(escape(title)).append("</text>\n");
		svg.append("<text x=\"24\" y=\"58\" font-size=\"13\" fill=\"#6b5e55\">")
			.append(escape(text("file.chart"))).append("</text>\n");
		int x = 24;
		for (Map.Entry<String, Integer> initial : initials.entrySet())
		{
			int bar = Math.max(2, initial.getValue() * CHART_HEIGHT / max);
			int y = top + CHART_HEIGHT - bar;
			int middle = x + BAR_WIDTH / 2;
			svg.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"")
				.append(BAR_WIDTH).append("\" height=\"").append(bar)
				.append("\" rx=\"3\" fill=\"#c2410c\"/>\n");
			svg.append("<text x=\"").append(middle).append("\" y=\"").append(y - 6)
				.append("\" font-size=\"11\" text-anchor=\"middle\" fill=\"#6b5e55\">")
				.append(initial.getValue()).append("</text>\n");
			svg.append("<text x=\"").append(middle).append("\" y=\"")
				.append(top + CHART_HEIGHT + 20)
				.append("\" font-size=\"13\" font-weight=\"600\" text-anchor=\"middle\" fill=\"#2b1d12\">")
				.append(escape(initial.getKey())).append("</text>\n");
			x += BAR_WIDTH + GAP;
		}
		svg.append("</svg>\n");
		return svg.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static String escape(String text)
	{
		return Strings.escapeMarkup(text).toString();
	}

	/**
	 * @return the chart of the countries by the initial of their names as PNG: its bars alone,
	 *         without texts, which would need the fonts of a desktop on the server
	 */
	byte[] png()
	{
		Map<String, Integer> initials = initials();
		int max = max(initials);
		int width = Math.max(360, 48 + initials.size() * (BAR_WIDTH + GAP));
		int height = CHART_HEIGHT + 48;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, width, height);
			graphics.setColor(INK);
			graphics.fillRect(20, 24 + CHART_HEIGHT, width - 40, 2);
			graphics.setColor(BAR);
			int x = 24;
			for (int count : initials.values())
			{
				int bar = Math.max(2, count * CHART_HEIGHT / max);
				graphics.fill(new RoundRectangle2D.Float(x, 24 + CHART_HEIGHT - bar, BAR_WIDTH,
					bar, 6, 6));
				x += BAR_WIDTH + GAP;
			}
		}
		finally
		{
			graphics.dispose();
		}
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		try
		{
			ImageIO.write(image, "png", png);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return png.toByteArray();
	}

	/**
	 * @return the report as a PDF of A4 pages: the title, then the countries in two columns, their
	 *         codes and their names, and the number of each page at its foot; in Helvetica, whose
	 *         letters are those of Windows-1252, a question mark standing for any other
	 */
	byte[] pdf()
	{
		List<String[]> countries = countries();
		int pages = Math.max(1, (countries.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
		List<String> contents = new ArrayList<>();
		for (int page = 0; page < pages; page++)
		{
			StringBuilder content = new StringBuilder("BT\n");
			content.append("/F2 16 Tf 1 0 0 1 56 780 Tm (").append(pdfString(title))
				.append(") Tj\n");
			content.append("/F2 10 Tf 1 0 0 1 56 750 Tm (").append(pdfString(text("file.code")))
				.append(") Tj 1 0 0 1 100 750 Tm (").append(pdfString(text("file.name")))
				.append(") Tj\n/F1 10 Tf\n");
			int y = 732;
			for (String[] country : countries.subList(page * LINES_PER_PAGE,
				Math.min(countries.size(), (page + 1) * LINES_PER_PAGE)))
			{
				content.append("1 0 0 1 56 ").append(y).append(" Tm (").append(pdfString(country[0]))
					.append(") Tj 1 0 0 1 100 ").append(y).append(" Tm (")
					.append(pdfString(country[1])).append(") Tj\n");
				y -= 14;
			}
			content.append("/F1 9 Tf 1 0 0 1 56 40 Tm (").append(pdfString(text("file.page")
				.replace("${page}", String.valueOf(page + 1))
				.replace("${pages}", String.valueOf(pages)))).append(") Tj\nET\n");
			contents.add(content.toString());
		}
		return new PdfWriter().write(contents);
	}

	/**
	 * @return the text as a string of a PDF, its parentheses and backslashes escaped
	 */
	private static String pdfString(String text)
	{
		return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
	}

	/**
	 * @return the report as a ZIP archive of all its other files
	 */
	byte[] zip(String name)
	{
		ByteArrayOutputStream zip = new ByteArrayOutputStream();
		try (ZipOutputStream out = new ZipOutputStream(zip))
		{
			for (DemoFile file : DemoFile.values())
			{
				if (file != DemoFile.ZIP)
				{
					out.putNextEntry(new ZipEntry(file.fileName(name)));
					out.write(file.write(this, name));
					out.closeEntry();
				}
			}
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return zip.toByteArray();
	}

	/**
	 * Writes a PDF of pages whose contents are given, in Helvetica and Helvetica Bold, two of the
	 * standard fonts every reader of PDF has, as /F1 and /F2.
	 */
	private static class PdfWriter
	{
		private static final Charset WIN_ANSI = Charset.forName("windows-1252");

		private final ByteArrayOutputStream pdf = new ByteArrayOutputStream();

		private final List<Integer> offsets = new ArrayList<>();

		byte[] write(List<String> contents)
		{
			append("%PDF-1.4\n%âãÏÓ\n");
			int pages = contents.size();
			// 1 the catalog, 2 the pages, 3 and 4 the fonts, then each page and its content
			object("<< /Type /Catalog /Pages 2 0 R >>");
			StringBuilder kids = new StringBuilder();
			for (int page = 0; page < pages; page++)
			{
				kids.append(5 + 2 * page).append(" 0 R ");
			}
			object("<< /Type /Pages /Kids [" + kids + "] /Count " + pages + " >>");
			object("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica " +
				"/Encoding /WinAnsiEncoding >>");
			object("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold " +
				"/Encoding /WinAnsiEncoding >>");
			for (int page = 0; page < pages; page++)
			{
				object("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources " +
					"<< /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + (6 + 2 * page) +
					" 0 R >>");
				byte[] stream = encode(contents.get(page));
				offsets.add(pdf.size());
				append((offsets.size()) + " 0 obj\n<< /Length " + stream.length + " >>\nstream\n");
				pdf.writeBytes(stream);
				append("\nendstream\nendobj\n");
			}
			int xref = pdf.size();
			append("xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
			for (int offset : offsets)
			{
				append(String.format("%010d 00000 n \n", offset));
			}
			append("trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\nstartxref\n" +
				xref + "\n%%EOF\n");
			return pdf.toByteArray();
		}

		private void object(String dictionary)
		{
			offsets.add(pdf.size());
			append(offsets.size() + " 0 obj\n" + dictionary + "\nendobj\n");
		}

		private void append(String text)
		{
			pdf.writeBytes(text.getBytes(StandardCharsets.ISO_8859_1));
		}

		/**
		 * @return the text in Windows-1252, a question mark for any letter it has not
		 */
		private static byte[] encode(String text)
		{
			CharsetEncoder encoder = WIN_ANSI.newEncoder()
				.onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE)
				.replaceWith(new byte[] { '?' });
			try
			{
				ByteBuffer bytes = encoder.encode(CharBuffer.wrap(text));
				byte[] encoded = new byte[bytes.remaining()];
				bytes.get(encoded);
				return encoded;
			}
			catch (CharacterCodingException e)
			{
				throw new IllegalStateException(e);
			}
		}
	}
}
