/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.tree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.resource.loader.PackageStringResourceLoader;

/**
 * The files of the tree examples: those of advanced-widgets itself, read from
 * {@code ProjectFiles.tsv}, their sizes the real ones and the days they were changed made up. The
 * folders come from the paths; they list their folders first, then their files, by name.
 * <p>
 * The files are the same for every visitor and never change.
 */
public final class ProjectFiles
{
	/** The day the files were changed last, at the latest. */
	private static final LocalDateTime LATEST = LocalDateTime.of(2026, 10, 7, 18, 30);

	/** How far back the files were changed, in minutes: about eight months. */
	private static final int SPAN = 60 * 24 * 240;

	/** The kinds of files, by the extension of their names. */
	private static final Map<String, String> KINDS = Map.ofEntries(Map.entry("java", "java"),
		Map.entry("ts", "typescript"), Map.entry("tsx", "typescript"), Map.entry("mjs", "javascript"),
		Map.entry("js", "javascript"), Map.entry("svelte", "svelte"), Map.entry("html", "html"),
		Map.entry("css", "css"), Map.entry("xml", "xml"), Map.entry("json", "json"),
		Map.entry("md", "markdown"), Map.entry("tsv", "text"), Map.entry("license", "text"));

	private static final ProjectFiles INSTANCE = new ProjectFiles();

	private final Map<Integer, ProjectFile> files = new LinkedHashMap<>();

	private final List<Integer> roots;

	private ProjectFiles()
	{
		Folder root = new Folder("", "");
		try (InputStream in = ProjectFiles.class.getResourceAsStream("ProjectFiles.tsv");
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (!line.isBlank() && !line.startsWith("#"))
				{
					String[] parts = line.split("\t");
					root.add(parts[0], parts[0].split("/"), 0, Long.parseLong(parts[1].strip()));
				}
			}
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		ProjectFile project = root.build(files);
		files.remove(project.id());
		roots = project.children();
	}

	/**
	 * @return the files
	 */
	public static ProjectFiles get()
	{
		return INSTANCE;
	}

	/**
	 * @return the files and folders at the root of the project
	 */
	public List<ProjectFile> roots()
	{
		return roots.stream().map(files::get).toList();
	}

	/**
	 * @return the children of a folder, none for a file
	 */
	public List<ProjectFile> children(ProjectFile folder)
	{
		return folder.children().stream().map(files::get).toList();
	}

	/**
	 * @return the file or folder of an id, {@code null} if there is none
	 */
	public ProjectFile byId(int id)
	{
		return files.get(id);
	}

	/**
	 * @return the file or folder of a path, {@code null} if there is none
	 */
	public ProjectFile byPath(String path)
	{
		return files.values().stream().filter(file -> file.path().equals(path)).findFirst()
			.orElse(null);
	}

	/**
	 * @return the number of files and folders
	 */
	public int size()
	{
		return files.size();
	}

	/**
	 * @param file
	 *            a file or a folder
	 * @param locale
	 *            the language of the texts
	 * @return what the trees and the details show of the file, in the language: its id, name and
	 *         path, the name of its kind, its size, for a folder with the number of its items, and
	 *         when it was changed
	 */
	public static Map<String, Object> toJson(ProjectFile file, Locale locale)
	{
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("id", file.id());
		json.put("name", file.name());
		json.put("path", file.path());
		json.put("folder", file.folder());
		json.put("kind", text("kind." + file.kind(), locale));
		json.put("size", formatSize(file.bytes(), locale));
		json.put("bytes", file.bytes());
		json.put("items", file.folder() ? new MessageFormat(text("items", locale), locale)
			.format(new Object[] { file.children().size() }) : "");
		json.put("modified", DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM,
			FormatStyle.SHORT).withLocale(locale).format(file.modified()));
		return json;
	}

	/**
	 * @return a text of the bundle of this package, {@code wicket-package}, in a language
	 */
	public static String text(String key, Locale locale)
	{
		String text = new PackageStringResourceLoader().loadStringResource(ProjectFiles.class, key,
			locale, null, null);
		return text != null ? text : key;
	}

	/**
	 * @return the size in bytes, kilobytes or megabytes, as the language writes numbers
	 */
	static String formatSize(long bytes, Locale locale)
	{
		NumberFormat format = NumberFormat.getNumberInstance(locale);
		format.setMaximumFractionDigits(1);
		if (bytes < 1024)
		{
			return format.format(bytes) + " B";
		}
		double kilobytes = bytes / 1024.0;
		return kilobytes < 1024 ? format.format(kilobytes) + " KB"
			: format.format(kilobytes / 1024) + " MB";
	}

	/**
	 * A folder as the files are read: its folders and files by name.
	 */
	private static final class Folder
	{
		private final String name;

		private final String path;

		private final Map<String, Folder> folders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		private final Map<String, Long> files = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		Folder(String name, String path)
		{
			this.name = name;
			this.path = path;
		}

		void add(String filePath, String[] parts, int index, long bytes)
		{
			if (index == parts.length - 1)
			{
				files.put(parts[index], bytes);
			}
			else
			{
				String folderPath = path.isEmpty() ? parts[index] : path + "/" + parts[index];
				folders.computeIfAbsent(parts[index], key -> new Folder(key, folderPath))
					.add(filePath, parts, index + 1, bytes);
			}
		}

		/**
		 * Gives this folder and everything in it their ids, in the order the trees show them.
		 *
		 * @return the folder
		 */
		ProjectFile build(Map<Integer, ProjectFile> all)
		{
			int id = all.size() + 1;
			all.put(id, null);
			List<ProjectFile> children = new ArrayList<>();
			folders.values().forEach(folder -> children.add(folder.build(all)));
			files.forEach((fileName, bytes) -> {
				String filePath = path.isEmpty() ? fileName : path + "/" + fileName;
				int fileId = all.size() + 1;
				ProjectFile file = new ProjectFile(fileId, fileName, filePath, false, bytes,
					LATEST.minusMinutes(Math.floorMod(filePath.hashCode(), SPAN)),
					kindOf(fileName), List.of());
				all.put(fileId, file);
				children.add(file);
			});
			ProjectFile folder = new ProjectFile(id, name, path, true,
				children.stream().mapToLong(ProjectFile::bytes).sum(),
				children.stream().map(ProjectFile::modified).max(Comparator.naturalOrder())
					.orElse(LATEST.minusMinutes(SPAN)),
				"folder", children.stream().map(ProjectFile::id).toList());
			all.put(id, folder);
			return folder;
		}

		private static String kindOf(String fileName)
		{
			int dot = fileName.lastIndexOf('.');
			return dot < 0 ? "text" : KINDS.getOrDefault(
				fileName.substring(dot + 1).toLowerCase(Locale.ROOT), "other");
		}
	}
}
