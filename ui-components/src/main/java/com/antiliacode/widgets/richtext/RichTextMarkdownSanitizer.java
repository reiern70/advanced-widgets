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
package com.antiliacode.widgets.richtext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cleans the Markdown of a rich text editor of the addresses of its links and images: the
 * Markdown an editor keeps by default ({@link AbstractRichTextEditor#getMarkdownSanitizer()}).
 * <p>
 * A link or an image may go only to an http, https or mailto address, or to a relative one, as in
 * the HTML ({@link RichTextPolicy#isSafeUrl(String)}). Another address, {@code javascript:} or
 * {@code data:} for example, written with entities and backslash escapes or not, gives way to
 * {@code #}: in an inline link or image, {@code [text](address)}, and in a reference definition,
 * {@code [label]: address}. An autolink to such an address, {@code <javascript:...>}, becomes
 * text.
 * <p>
 * {@link #IMAGES_FROM_SITE_ONLY} keeps the images of the site alone: the address of an image,
 * {@code ![text](address)}, must be {@link RichTextPolicy#isSiteUrl(String) the site's own}, else
 * gives way to {@code #} too. So must that of a reference definition, which an image may use as
 * well as a link, {@code ![text][label]}; the editor writes none. Links go to other sites as
 * before.
 * <p>
 * HTML written in the Markdown stays as it is: telling it from the text of code, where
 * {@code <b>} is no tag, takes a parser of Markdown. The editor writes the {@code <} of the text
 * as {@code &lt;}, so only code holds it as it is, but a form sent by other means than the editor
 * may hold HTML anywhere. So render the Markdown with a renderer that shows HTML as text or leaves
 * it out, as {@link com.antiliacode.widgets.codeeditor.MarkdownPreview} does, or
 * {@link AbstractRichTextEditor#setMarkdownSanitizer(IRichTextSanitizer) set a sanitizer} that
 * parses the Markdown and cleans it. The same goes for a value that only looks like a link, in a
 * code span for example: its address gives way too, the text being changed but no harm done.
 * <p>
 * It {@link #sanitizeAndReport(String) tells} of each address that gave way: as a link,
 * {@code a}, or as an image, {@code img}, by the {@code !} before the brackets; a reference
 * definition and an autolink as a link. HTML left in the Markdown is not told of.
 *
 * @since 11.0.0
 */
public class RichTextMarkdownSanitizer implements IRichTextSanitizer
{
	private static final long serialVersionUID = 1L;

	/** The sanitizer. */
	public static final RichTextMarkdownSanitizer INSTANCE = new RichTextMarkdownSanitizer();

	/**
	 * The sanitizer keeping the images of the site alone.
	 *
	 * @since 11.0.0
	 */
	public static final RichTextMarkdownSanitizer IMAGES_FROM_SITE_ONLY =
		new RichTextMarkdownSanitizer(true);

	/** Where an inline link or an image, or a reference definition, writes its address. */
	private static final Pattern DESTINATION_START = Pattern.compile("\\]\\(|\\]:");

	/** An autolink: a scheme, then anything but spaces and angle brackets. */
	private static final Pattern AUTOLINK = Pattern
		.compile("<([A-Za-z][A-Za-z0-9+.-]{1,31}:[^\\s<>]*)>");

	/** A character entity: numeric, decimal or hexadecimal, or named. */
	private static final Pattern ENTITY = Pattern
		.compile("&(?:#([0-9]{1,7})|#[xX]([0-9A-Fa-f]{1,6})|([A-Za-z][A-Za-z0-9]{1,31}));");

	/** A backslash escape: a backslash before a punctuation character of ASCII. */
	private static final Pattern BACKSLASH_ESCAPE = Pattern
		.compile("\\\\([!\"#$%&'()*+,\\-./:;<=>?@\\[\\\\\\]^_`{|}~])");

	/** Whether images go only to the site's own addresses. */
	private final boolean imagesFromSiteOnly;

	/**
	 * A sanitizer letting images go to other sites, as {@link #INSTANCE} does.
	 */
	public RichTextMarkdownSanitizer()
	{
		this(false);
	}

	/**
	 * @param imagesFromSiteOnly
	 *            whether images, and reference definitions, go only to the site's own addresses
	 * @since 11.0.0
	 */
	public RichTextMarkdownSanitizer(boolean imagesFromSiteOnly)
	{
		this.imagesFromSiteOnly = imagesFromSiteOnly;
	}

	/**
	 * @return whether images, and reference definitions, go only to the site's own addresses
	 * @since 11.0.0
	 */
	public boolean isImagesFromSiteOnly()
	{
		return imagesFromSiteOnly;
	}

	@Override
	public String sanitize(String markdown)
	{
		return sanitizeAndReport(markdown).text();
	}

	/**
	 * Cleans the Markdown, telling of each link, {@code a}, and each image, {@code img}, whose
	 * address gave way.
	 */
	@Override
	public RichTextSanitized sanitizeAndReport(String markdown)
	{
		if (markdown == null)
		{
			return new RichTextSanitized(null, RichTextCleaningReport.EMPTY);
		}
		RichTextCleaningReport.Builder report = RichTextCleaningReport.builder();
		String clean = cleanAutolinks(cleanDestinations(markdown, report), report);
		return new RichTextSanitized(clean, report.build());
	}

	/**
	 * @return the Markdown with the addresses of its links, images and reference definitions that
	 *         are not safe replaced by {@code #}
	 */
	private String cleanDestinations(String markdown, RichTextCleaningReport.Builder report)
	{
		StringBuilder clean = null;
		int copied = 0;
		Matcher start = DESTINATION_START.matcher(markdown);
		int from = 0;
		while (from < markdown.length() && start.find(from))
		{
			// the next one may start inside this address, should a renderer end it sooner
			from = start.end();
			int begin = skipSpace(markdown, start.end());
			int end = destinationEnd(markdown, begin);
			if (end <= begin)
			{
				continue;
			}
			boolean pointed = markdown.charAt(begin) == '<';
			String destination = pointed ? markdown.substring(begin + 1, end - 1) :
				markdown.substring(begin, end);
			boolean image = start.group().equals("](") && isImage(markdown, start.start());
			boolean loaded = imagesFromSiteOnly && (image || start.group().equals("]:"));
			if (loaded ? isSite(destination) : isSafe(destination))
			{
				continue;
			}
			from = end;
			report.addElement(image ? "img" : "a");
			if (clean == null)
			{
				clean = new StringBuilder(markdown.length());
			}
			clean.append(markdown, copied, begin).append(pointed ? "<#>" : "#");
			copied = end;
		}
		if (clean == null)
		{
			return markdown;
		}
		return clean.append(markdown, copied, markdown.length()).toString();
	}

	/**
	 * @return whether the brackets closing at an index are those of an image, {@code ![...]}: the
	 *         bracket opening them, nested brackets and backslash escapes counted, comes after a
	 *         {@code !} that is not escaped
	 */
	private static boolean isImage(String markdown, int closing)
	{
		int depth = 0;
		for (int i = closing - 1; i >= 0; i--)
		{
			char c = markdown.charAt(i);
			if ((c != '[' && c != ']') || (i > 0 && markdown.charAt(i - 1) == '\\'))
			{
				continue;
			}
			if (c == ']')
			{
				depth++;
			}
			else if (depth > 0)
			{
				depth--;
			}
			else
			{
				return i > 0 && markdown.charAt(i - 1) == '!' &&
					(i < 2 || markdown.charAt(i - 2) != '\\');
			}
		}
		return false;
	}

	/**
	 * @return where the address starting at an index begins: after the spaces and tabs, and at most
	 *         one line ending, Markdown allows before it, with the marks of the quotes the next
	 *         line goes on in
	 */
	private static int skipSpace(String markdown, int index)
	{
		boolean lineEnding = false;
		int i = index;
		while (i < markdown.length())
		{
			char c = markdown.charAt(i);
			if (c == ' ' || c == '\t' || (c == '>' && lineEnding))
			{
				i++;
			}
			else if ((c == '\n' || c == '\r') && !lineEnding)
			{
				lineEnding = true;
				i += c == '\r' && i + 1 < markdown.length() && markdown.charAt(i + 1) == '\n' ? 2
					: 1;
			}
			else
			{
				break;
			}
		}
		return i;
	}

	/**
	 * @return the end of the address starting at an index: after the {@code >} of one in angle
	 *         brackets, else before the first space, control character or parenthesis that does not
	 *         belong to it; the index itself when there is none
	 */
	private static int destinationEnd(String markdown, int begin)
	{
		if (begin >= markdown.length())
		{
			return begin;
		}
		if (markdown.charAt(begin) == '<')
		{
			for (int i = begin + 1; i < markdown.length(); i++)
			{
				char c = markdown.charAt(i);
				if (c == '\\' && i + 1 < markdown.length())
				{
					i++;
				}
				else if (c == '>')
				{
					return i + 1;
				}
				else if (c == '<' || c == '\n' || c == '\r')
				{
					return begin;
				}
			}
			return begin;
		}
		int depth = 0;
		int i = begin;
		while (i < markdown.length())
		{
			char c = markdown.charAt(i);
			if (c <= ' ' || c == 0x7f)
			{
				break;
			}
			if (c == '\\' && i + 1 < markdown.length())
			{
				i += 2;
				continue;
			}
			if (c == '(')
			{
				depth++;
			}
			else if (c == ')')
			{
				if (depth == 0)
				{
					break;
				}
				depth--;
			}
			i++;
		}
		return i;
	}

	/**
	 * @return the Markdown with the autolinks to addresses that are not safe turned into text
	 */
	private static String cleanAutolinks(String markdown, RichTextCleaningReport.Builder report)
	{
		Matcher autolink = AUTOLINK.matcher(markdown);
		StringBuilder clean = null;
		int copied = 0;
		while (autolink.find())
		{
			if (isSafe(autolink.group(1)))
			{
				continue;
			}
			report.addElement("a");
			if (clean == null)
			{
				clean = new StringBuilder(markdown.length());
			}
			clean.append(markdown, copied, autolink.start()).append("&lt;");
			copied = autolink.start() + 1;
		}
		if (clean == null)
		{
			return markdown;
		}
		return clean.append(markdown, copied, markdown.length()).toString();
	}

	/**
	 * @return whether an address of the Markdown is safe once its backslash escapes and entities
	 *         are read, as a renderer reads them; an empty one is
	 */
	static boolean isSafe(String destination)
	{
		String address = read(destination);
		return address.isBlank() || RichTextPolicy.isSafeUrl(address);
	}

	/**
	 * @return whether an address of the Markdown is the site's own once its backslash escapes and
	 *         entities are read; an empty one is
	 */
	static boolean isSite(String destination)
	{
		String address = read(destination);
		return address.isBlank() || RichTextPolicy.isSiteUrl(address);
	}

	/**
	 * @return an address of the Markdown as a renderer reads it: its backslash escapes and its
	 *         entities read
	 */
	private static String read(String destination)
	{
		return decodeEntities(BACKSLASH_ESCAPE.matcher(destination).replaceAll("$1"));
	}

	/**
	 * @return the text with its numeric entities read, and {@code &colon;}, {@code &Tab;} and
	 *         {@code &NewLine;}, the named ones that may hide a scheme
	 */
	private static String decodeEntities(String text)
	{
		if (text.indexOf('&') < 0)
		{
			return text;
		}
		Matcher entity = ENTITY.matcher(text);
		StringBuilder decoded = new StringBuilder(text.length());
		while (entity.find())
		{
			String replacement;
			if (entity.group(1) != null || entity.group(2) != null)
			{
				int code = entity.group(1) != null ? Integer.parseInt(entity.group(1)) :
					Integer.parseInt(entity.group(2), 16);
				replacement = Character.isValidCodePoint(code) && code != 0 ?
					new String(Character.toChars(code)) : "?";
			}
			else
			{
				replacement = switch (entity.group(3))
				{
					case "colon" -> ":";
					case "Tab" -> "\t";
					case "NewLine" -> "\n";
					default -> entity.group();
				};
			}
			entity.appendReplacement(decoded, Matcher.quoteReplacement(replacement));
		}
		entity.appendTail(decoded);
		return decoded.toString();
	}
}
