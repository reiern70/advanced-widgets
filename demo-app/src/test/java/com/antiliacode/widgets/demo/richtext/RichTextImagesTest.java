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
package com.antiliacode.widgets.demo.richtext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.antiliacode.widgets.richtext.RichTextImageType;

/**
 * The images of the demo: found by the names the store gives only, and no more of them, nor
 * older, than allowed.
 */
class RichTextImagesTest
{
	@TempDir
	Path folder;

	private Path image(String name, Duration age) throws IOException
	{
		Path file = Files.write(folder.resolve(name), new byte[] { 1 });
		Files.setLastModifiedTime(file, FileTime.from(Instant.now().minus(age)));
		return file;
	}

	private static String name(int index, String extension)
	{
		return String.format("%032x", index) + "." + extension;
	}

	private long count() throws IOException
	{
		try (Stream<Path> files = Files.list(folder))
		{
			return files.count();
		}
	}

	@Test
	void findsOnlyTheImagesItNamed() throws IOException
	{
		RichTextImages images = new RichTextImages(folder);
		Path bird = image(name(1, "png"), Duration.ZERO);
		image("notes.txt", Duration.ZERO);
		image(name(2, "gif"), Duration.ofDays(2));

		assertEquals(bird, images.find(name(1, "png")));
		assertNull(images.find("notes.txt"));
		assertNull(images.find("../" + name(1, "png")));
		assertNull(images.find(name(1, "svg")));
		assertNull(images.find(name(3, "png")), "gone");
		assertNull(images.find(name(2, "gif")), "too old");
		assertNull(images.find(null));
		assertEquals(RichTextImageType.PNG, RichTextImages.typeOf(bird));
		assertEquals(RichTextImageType.JPEG, RichTextImages.typeOf(Path.of(name(4, "jpg"))));
		assertNull(RichTextImages.typeOf(Path.of("notes.txt")));
	}

	@Test
	void keepsNoMoreImagesNorOlderOnesThanAllowed() throws IOException
	{
		RichTextImages images = new RichTextImages(folder);
		for (int i = 0; i < 5; i++)
		{
			image(name(i, "png"), Duration.ofMinutes(10 - i));
		}
		image(name(9, "webp"), Duration.ofDays(3));
		image("other.txt", Duration.ofDays(3));

		images.prune(3);
		assertEquals(4, count(), "the three newest images, and the file that is none");
		assertFalse(Files.exists(folder.resolve(name(0, "png"))));
		assertFalse(Files.exists(folder.resolve(name(1, "png"))));
		assertTrue(Files.exists(folder.resolve(name(4, "png"))));
		assertFalse(Files.exists(folder.resolve(name(9, "webp"))));
		assertTrue(Files.exists(folder.resolve("other.txt")));

		new RichTextImages(folder.resolve("missing")).prune(0);
	}
}
