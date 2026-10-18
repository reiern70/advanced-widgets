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
package com.antilia.widgets.demo.tree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.resource.loader.ComponentStringResourceLoader;
import org.apache.wicket.resource.loader.PackageStringResourceLoader;

/**
 * The tree of life of the tree examples, read from {@code TreeOfLife.tsv}: fungi, animals and
 * plants, a few of their groups, some long extinct, and some of their species, a few levels deep.
 * The names and the descriptions are in the bundle {@code TreeOfLife}, by the ids of the taxa;
 * the names of the ranks and the periods, and how the times are written, in the bundle of the
 * package.
 * <p>
 * The tree is the same for every visitor and never changes.
 */
public final class TreeOfLife
{
	/** The geological periods, the youngest first, by the time they began, in millions of years ago. */
	private static final List<Period> PERIODS = List.of(new Period("quaternary", 2.58),
		new Period("neogene", 23.03), new Period("paleogene", 66),
		new Period("cretaceous", 145), new Period("jurassic", 201.4),
		new Period("triassic", 251.9), new Period("permian", 298.9),
		new Period("carboniferous", 358.9), new Period("devonian", 419.2),
		new Period("silurian", 443.8), new Period("ordovician", 485.4),
		new Period("cambrian", 539), new Period("ediacaran", 635),
		new Period("cryogenian", 720), new Period("tonian", 1000));

	/** Times less than this many millions of years ago are written in years. */
	private static final double IN_YEARS = 0.1;

	private static final TreeOfLife INSTANCE = new TreeOfLife();

	private final Map<String, Taxon> taxa = new LinkedHashMap<>();

	private final List<String> roots = new ArrayList<>();

	private TreeOfLife()
	{
		Map<String, List<String>> children = new LinkedHashMap<>();
		List<String[]> rows = new ArrayList<>();
		try (InputStream in = TreeOfLife.class.getResourceAsStream("TreeOfLife.tsv");
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (!line.isBlank() && !line.startsWith("#"))
				{
					String[] parts = line.split("\t");
					String[] path = parts[0].split("/");
					String id = path[path.length - 1];
					String parent = path.length > 1 ? path[path.length - 2] : null;
					children.put(id, new ArrayList<>());
					if (parent == null)
					{
						roots.add(id);
					}
					else
					{
						children.get(parent).add(id);
					}
					rows.add(new String[] { id, parent, parts[1], parts[2], parts[3], parts[4] });
				}
			}
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		for (String[] row : rows)
		{
			taxa.put(row[0], new Taxon(row[0], "-".equals(row[2]) ? null : row[2], row[3],
				"-".equals(row[4]) ? null : Double.valueOf(row[4]), Double.parseDouble(row[5]),
				row[1], List.copyOf(children.get(row[0]))));
		}
	}

	/**
	 * @return the tree of life
	 */
	public static TreeOfLife get()
	{
		return INSTANCE;
	}

	/**
	 * @return the kingdoms
	 */
	public List<Taxon> roots()
	{
		return roots.stream().map(taxa::get).toList();
	}

	/**
	 * @return the groups and the species of a group, none for a species
	 */
	public List<Taxon> children(Taxon taxon)
	{
		return taxon.children().stream().map(taxa::get).toList();
	}

	/**
	 * @return the taxon of an id, {@code null} if there is none
	 */
	public Taxon byId(String id)
	{
		return id != null ? taxa.get(id) : null;
	}

	/**
	 * @return the group a taxon belongs to, {@code null} for a kingdom
	 */
	public Taxon parent(Taxon taxon)
	{
		return byId(taxon.parent());
	}

	/**
	 * @return the groups a taxon belongs to, the kingdom first, and the taxon itself last
	 */
	public List<Taxon> lineage(Taxon taxon)
	{
		List<Taxon> lineage = new ArrayList<>();
		for (Taxon next = taxon; next != null; next = parent(next))
		{
			lineage.add(0, next);
		}
		return lineage;
	}

	/**
	 * @return the number of groups and species
	 */
	public int size()
	{
		return taxa.size();
	}

	/**
	 * @param taxon
	 *            a group or a species
	 * @param locale
	 *            the language of the texts
	 * @return what the trees and the cards show of the taxon, in the language: its card, see
	 *         {@link #card(Taxon, Locale)}, whether it is a group, the names of its lineage, the
	 *         kingdom first and the taxon last, and the card of the group of a species
	 */
	public Map<String, Object> toJson(Taxon taxon, Locale locale)
	{
		Map<String, Object> json = card(taxon, locale);
		json.put("group", !taxon.isLeaf());
		json.put("lineage", lineage(taxon).stream().map(each -> name(each, locale)).toList());
		Taxon parent = parent(taxon);
		if (taxon.isLeaf() && parent != null)
		{
			json.put("parent", card(parent, locale));
		}
		return json;
	}

	/**
	 * @return the card of a taxon, in a language: its id, name, scientific name, rank, the periods
	 *         and the times it lived, and its description
	 */
	public static Map<String, Object> card(Taxon taxon, Locale locale)
	{
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("id", taxon.id());
		json.put("name", name(taxon, locale));
		json.put("scientific", taxon.scientific() != null ? taxon.scientific() : "");
		json.put("rank", text("rank." + taxon.rank(), locale));
		json.put("period", period(taxon, locale));
		json.put("lived", lived(taxon, locale));
		json.put("description", taxonText(taxon.id() + ".description", locale));
		return json;
	}

	/**
	 * @return the name of a taxon in a language
	 */
	public static String name(Taxon taxon, Locale locale)
	{
		return taxonText(taxon.id(), locale);
	}

	/**
	 * @return the periods a taxon lived in: the one it appeared in, and the one it died out in,
	 *         or today
	 */
	static String period(Taxon taxon, Locale locale)
	{
		String until = taxon.isLiving() ? text("period.today", locale)
			: text("period." + periodOf(taxon.to(), false), locale);
		if (taxon.from() == null)
		{
			return taxon.isLiving() ? text("period." + periodOf(0, false), locale) : until;
		}
		String since = text("period." + periodOf(taxon.from(), true), locale);
		return since.equals(until) ? since : since + " – " + until;
	}

	/**
	 * @param time
	 *            a time, in millions of years ago
	 * @param start
	 *            whether the time is when something began: a period ends at the time its next
	 *            one begins
	 * @return the key of the period of the time
	 */
	static String periodOf(double time, boolean start)
	{
		for (Period period : PERIODS)
		{
			if (start ? time <= period.began() : time < period.began())
			{
				return period.key();
			}
		}
		return PERIODS.get(PERIODS.size() - 1).key();
	}

	/**
	 * @return when a taxon lived, in millions of years ago, the most recent times in years
	 */
	static String lived(Taxon taxon, Locale locale)
	{
		NumberFormat format = NumberFormat.getNumberInstance(locale);
		format.setMaximumFractionDigits(2);
		String years = format.format(Math.round(taxon.to() * 1_000_000));
		if (taxon.from() == null)
		{
			return taxon.isLiving() ? text("lived.living", locale)
				: message("lived.until", locale, years);
		}
		String from = format.format(taxon.from());
		if (taxon.isLiving())
		{
			return message("lived.today", locale, from);
		}
		return taxon.to() < IN_YEARS ? message("lived.years", locale, from, years)
			: message("lived.range", locale, from, format.format(taxon.to()));
	}

	private static String message(String key, Locale locale, Object... arguments)
	{
		return new MessageFormat(text(key, locale), locale).format(arguments);
	}

	/**
	 * @return a text of the bundle of this package, {@code wicket-package}, in a language
	 */
	public static String text(String key, Locale locale)
	{
		String text = new PackageStringResourceLoader().loadStringResource(TreeOfLife.class, key,
			locale, null, null);
		return text != null ? text : key;
	}

	/**
	 * @return a text of the bundle {@code TreeOfLife}, the names and the descriptions of the taxa
	 */
	private static String taxonText(String key, Locale locale)
	{
		String text = new ComponentStringResourceLoader().loadStringResource(TreeOfLife.class, key,
			locale, null, null);
		return text != null ? text : key;
	}

	/**
	 * A geological period: the key of its name, and when it began, in millions of years ago.
	 */
	private record Period(String key, double began)
	{
	}
}
