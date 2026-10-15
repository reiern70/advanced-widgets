/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks {@link DemoUploadsFileManager}: it stores the files of a field, deletes them and the
 * folder of the field, and the folders left behind for too long.
 */
class DemoUploadsFileManagerTest
{
	@TempDir
	Path folder;

	private static FileUpload upload(String name, String text) throws IOException
	{
		DiskFileItem item = new DiskFileItem.Builder().setFieldName("files")
			.setContentType("text/plain").setFileName(name).setFormField(false).get();
		try (OutputStream out = item.getOutputStream())
		{
			out.write(text.getBytes());
		}
		return new FileUpload(item);
	}

	@Test
	void storesTheFilesOfAFieldAndDeletesThem() throws IOException
	{
		DemoUploadsFileManager files = new DemoUploadsFileManager(folder.toFile());
		files.save(upload("a.txt", "hello"), "field1");
		files.save(upload("b.txt", "bye"), "field1");

		Path a = folder.resolve("field1").resolve("a.txt");
		assertEquals("hello", Files.readString(a));
		assertEquals(a.toFile().getCanonicalFile(), files.getFile("field1", "a.txt")
			.getCanonicalFile());

		files.delete(a.toFile());
		assertFalse(Files.exists(a));
		assertTrue(Files.isDirectory(a.getParent()), "b.txt is still there");
		files.delete(files.getFile("field1", "b.txt"));
		assertFalse(Files.exists(a.getParent()), "the folder of the field went with its last file");

		files.delete(null);
		files.delete(a.toFile());
	}

	@Test
	void deletesTheFoldersLeftBehind() throws IOException
	{
		DemoUploadsFileManager files = new DemoUploadsFileManager(folder.toFile());
		Path old = Files.createDirectories(folder.resolve("old"));
		Files.writeString(old.resolve("c.txt"), "left");
		Files.setLastModifiedTime(old, FileTime.from(Instant.now()
			.minus(DemoUploadsFileManager.MAX_AGE).minusSeconds(60)));

		files.save(upload("d.txt", "new"), "fresh");

		assertFalse(Files.exists(old));
		assertTrue(Files.exists(folder.resolve("fresh").resolve("d.txt")));
	}

	@Test
	void keepsItsFolderInTheTemporaryFolder()
	{
		assertEquals(Path.of(System.getProperty("java.io.tmpdir"), "antilia-widgets-uploads"),
			DemoUploadsFileManager.defaultFolder().toPath());
	}
}
