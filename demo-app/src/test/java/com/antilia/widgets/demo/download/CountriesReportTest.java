/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.download;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import com.antilia.widgets.demo.WicketApplication;
import com.github.openjson.JSONObject;

/**
 * Writes the report of the countries in every format of {@link DemoFile}, in the language asked
 * for.
 */
class CountriesReportTest
{
	private WicketTester tester;

	@BeforeEach
	void start()
	{
		// the texts of the files are in the bundles of the application
		tester = new WicketTester(new WicketApplication());
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private static String string(byte[] bytes)
	{
		return new String(bytes, StandardCharsets.UTF_8);
	}

	@Test
	void listsTheCountriesByNameInTheLanguageUpToTheRowsAskedFor()
	{
		CountriesReport report = new CountriesReport("Länder", 3, Locale.GERMAN);

		List<String[]> countries = report.countries();
		assertEquals(3, countries.size());
		assertArrayEquals(new String[] { "AF", "Afghanistan" }, countries.get(0));
		assertArrayEquals(new String[] { "EG", "Ägypten" }, countries.get(1));
		assertEquals(CountriesReport.ALL, CountriesReport.of(Locale.GERMAN).countries().size());
		assertEquals("Länder der Welt", CountriesReport.of(Locale.GERMAN).title());
	}

	@Test
	void writesTextCsvAndJsonWithTheHeadersOfTheLanguage()
	{
		CountriesReport report = new CountriesReport("Países", 2, Locale.forLanguageTag("es"));

		assertEquals("Países\n======\n\nCódigo  Nombre\nAF      Afganistán\nAL      Albania\n",
			string(report.text()).replace("\r\n", "\n"));
		assertEquals("\"Código\",\"Nombre\"\r\n\"AF\",\"Afganistán\"\r\n\"AL\",\"Albania\"\r\n",
			string(report.csv()));
		JSONObject json = new JSONObject(string(report.json()));
		assertEquals("Países", json.getString("title"));
		assertEquals("Afganistán", json.getJSONArray("countries").getJSONObject(0)
			.getString("name"));
	}

	@Test
	void chartsTheCountriesByTheInitialOfTheirNamesWithoutAccents() throws Exception
	{
		CountriesReport report = new CountriesReport("Länder <alle>", CountriesReport.ALL,
			Locale.GERMAN);

		assertEquals(report.initials().values().stream().mapToInt(Integer::intValue).sum(),
			CountriesReport.ALL);
		assertTrue(report.initials().containsKey("A"));
		assertTrue(report.initials().keySet().stream().noneMatch(initial -> initial.equals("Ä")));

		Document svg = DocumentBuilderFactory.newInstance().newDocumentBuilder()
			.parse(new ByteArrayInputStream(report.svg()));
		assertEquals("svg", svg.getDocumentElement().getTagName());
		assertEquals("Länder <alle>", svg.getElementsByTagName("title").item(0).getTextContent());
		assertEquals(report.initials().size(), svg.getElementsByTagName("rect").getLength() - 1);

		BufferedImage png = ImageIO.read(new ByteArrayInputStream(report.png()));
		assertTrue(png.getWidth() >= 360 && png.getHeight() > 200);
	}

	@Test
	void writesAPdfWhoseCrossReferencesPointAtItsObjects()
	{
		CountriesReport report = new CountriesReport("Länder (alle)", CountriesReport.ALL,
			Locale.GERMAN);

		byte[] pdf = report.pdf();
		String text = new String(pdf, StandardCharsets.ISO_8859_1);
		assertTrue(text.startsWith("%PDF-1.4\n"));
		assertTrue(text.endsWith("%%EOF\n"));
		assertTrue(text.contains("(Länder \\(alle\\)) Tj"), "in Windows-1252, escaped");
		int pages = (CountriesReport.ALL + 47) / 48;
		assertTrue(text.contains("/Count " + pages + " "), text.substring(0, 300));
		assertTrue(text.contains("(Seite " + pages + " von " + pages + ") Tj"));

		Matcher startxref = Pattern.compile("startxref\n(\\d+)\n").matcher(text);
		assertTrue(startxref.find());
		int xref = Integer.parseInt(startxref.group(1));
		assertTrue(text.startsWith("xref\n", xref));
		Matcher offsets = Pattern.compile("(\\d{10}) 00000 n \n").matcher(text.substring(xref));
		int number = 1;
		while (offsets.find())
		{
			assertTrue(text.startsWith(number + " 0 obj\n", Integer.parseInt(offsets.group(1))),
				"object " + number);
			number++;
		}
		assertEquals(4 + 2 * pages + 1, number);
	}

	@Test
	void zipsAllTheOtherFiles() throws IOException
	{
		CountriesReport report = new CountriesReport("World", 5, Locale.ENGLISH);

		List<String> names = new ArrayList<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(
			DemoFile.ZIP.write(report, "world"))))
		{
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
			{
				names.add(entry.getName());
				if (entry.getName().endsWith(".csv"))
				{
					assertEquals(string(report.csv()), string(zip.readAllBytes()));
				}
			}
		}
		assertEquals(List.of("world.txt", "world.csv", "world.json", "world.svg", "world.png",
			"world.pdf"), names);
	}

	@Test
	void knowsTheKindOfAFileByItsExtension()
	{
		assertEquals(DemoFile.PDF, DemoFile.ofFileName("countries.PDF"));
		assertEquals(null, DemoFile.ofFileName("countries"));
		assertEquals(null, DemoFile.ofFileName("countries.exe"));
		assertTrue(Arrays.stream(DemoFile.values()).allMatch(file -> file.getIcon() != null));
	}
}
