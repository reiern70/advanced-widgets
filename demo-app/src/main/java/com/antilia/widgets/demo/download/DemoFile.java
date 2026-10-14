/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.download;

import java.util.Locale;

import com.antilia.widgets.icon.SvgIcon;

/**
 * The kinds of files the downloads demo hands out, each a format of the {@link CountriesReport}:
 * its extension, its content type and its icon. Their names are in the bundle of the package,
 * under {@code DemoFile.<name>}.
 */
public enum DemoFile
{
	/** A text, a line per country. */
	TEXT("txt", "text/plain", SvgIcon.FILE_LINES),

	/** Comma separated values, for a spreadsheet. */
	CSV("csv", "text/csv", SvgIcon.FILE_CSV),

	/** JSON, for a program. */
	JSON("json", "application/json", SvgIcon.FILE_CODE),

	/** A chart as SVG, which a browser shows. */
	SVG("svg", "image/svg+xml", SvgIcon.FILE_IMAGE),

	/** A chart as PNG, which a browser shows. */
	PNG("png", "image/png", SvgIcon.IMAGE),

	/** A PDF, which a browser shows. */
	PDF("pdf", "application/pdf", SvgIcon.FILE_PDF),

	/** A ZIP archive of the other files, which a browser always saves. */
	ZIP("zip", "application/zip", SvgIcon.FILE_ZIPPER);

	private final String extension;

	private final String contentType;

	private final SvgIcon icon;

	DemoFile(String extension, String contentType, SvgIcon icon)
	{
		this.extension = extension;
		this.contentType = contentType;
		this.icon = icon;
	}

	/**
	 * @return the extension of the file, without the dot
	 */
	public String getExtension()
	{
		return extension;
	}

	/**
	 * @return the content type of the file, without a charset
	 */
	public String getContentType()
	{
		return contentType;
	}

	/**
	 * @return whether the file is a text, in UTF-8
	 */
	public boolean isText()
	{
		return this == TEXT || this == CSV || this == JSON || this == SVG;
	}

	/**
	 * @return the icon of the file
	 */
	public SvgIcon getIcon()
	{
		return icon;
	}

	/**
	 * @param name
	 *            the name of the file, without the extension
	 * @return the name of the file with its extension: {@code countries.csv} for example
	 */
	public String fileName(String name)
	{
		return name + "." + extension;
	}

	/**
	 * @param fileName
	 *            the name of a file with its extension
	 * @return the kind of file of the extension, {@code null} for none
	 */
	public static DemoFile ofFileName(String fileName)
	{
		int dot = fileName != null ? fileName.lastIndexOf('.') : -1;
		if (dot < 0)
		{
			return null;
		}
		String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
		for (DemoFile file : values())
		{
			if (file.extension.equals(extension))
			{
				return file;
			}
		}
		return null;
	}

	/**
	 * @param report
	 *            the report to write
	 * @param name
	 *            the name of the file, without the extension, which a ZIP archive gives the
	 *            files it holds
	 * @return the report in this format
	 */
	byte[] write(CountriesReport report, String name)
	{
		return switch (this)
		{
			case TEXT -> report.text();
			case CSV -> report.csv();
			case JSON -> report.json();
			case SVG -> report.svg();
			case PNG -> report.png();
			case PDF -> report.pdf();
			case ZIP -> report.zip(name);
		};
	}
}
