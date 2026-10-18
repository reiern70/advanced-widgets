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
package com.antilia.widgets.demo.contacts;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The contacts of the example: {@value #SIZE} people, made up at start, always the same ones.
 * Nobody changes them, so the pages share the list.
 */
public final class ContactsDatabase
{
	/** How many contacts there are. */
	public static final int SIZE = 2500;

	private static final List<String> FIRST_NAMES = List.of("Alba", "Ana", "Bruno", "Carla",
		"Chloé", "Dario", "Elena", "Emma", "Felix", "Greta", "Hannah", "Hugo", "Ines", "Javier",
		"Jonas", "Karin", "Kenji", "Laura", "Lucas", "Lucía", "Marta", "Mateo", "Mia", "Nils",
		"Noah", "Olga", "Oliver", "Pablo", "Paula", "Rosa", "Sara", "Sofía", "Sven", "Tania",
		"Thomas", "Valentina", "Vera", "Walter", "Yuki", "Zoe");

	private static final List<String> LAST_NAMES = List.of("Alvarez", "Becker", "Bianchi",
		"Costa", "Dubois", "Dumont", "Eriksen", "Fernández", "Fontana", "García", "Hansen",
		"Hoffmann", "Ivanova", "Jansen", "Kowalski", "Lindqvist", "López", "Martin", "Moreau",
		"Müller", "Novak", "Okafor", "Pereira", "Petrov", "Quintero", "Ricci", "Rossi",
		"Sánchez", "Schmidt", "Silva", "Smith", "Suzuki", "Tanaka", "Taylor", "Vargas", "Weber",
		"Wilson", "Yilmaz", "Zimmermann");

	/** The countries, by the ISO 3166 code, with some of their cities; the first are bigger. */
	private static final List<List<String>> CITIES = List.of(
		List.of("US", "Austin", "Boston", "Denver", "Seattle"),
		List.of("DE", "Berlin", "Hamburg", "Leipzig", "Munich"),
		List.of("ES", "Barcelona", "Bilbao", "Madrid", "Valencia"),
		List.of("GB", "Bristol", "Edinburgh", "London", "Manchester"),
		List.of("FR", "Lyon", "Nantes", "Paris", "Toulouse"),
		List.of("IT", "Bologna", "Milan", "Rome", "Turin"),
		List.of("BR", "Curitiba", "Recife", "São Paulo"),
		List.of("AR", "Buenos Aires", "Córdoba", "Rosario"),
		List.of("CA", "Halifax", "Montreal", "Toronto"),
		List.of("JP", "Osaka", "Sendai", "Tokyo"),
		List.of("NL", "Amsterdam", "Rotterdam", "Utrecht"),
		List.of("MX", "Guadalajara", "Mexico City", "Monterrey"),
		List.of("PT", "Lisbon", "Porto"),
		List.of("SE", "Gothenburg", "Stockholm"),
		List.of("NO", "Bergen", "Oslo"),
		List.of("CH", "Basel", "Geneva", "Zurich"),
		List.of("AU", "Melbourne", "Sydney"),
		List.of("UY", "Montevideo", "Salto"));

	private static final List<String> DOMAINS = List.of("example.com", "example.org",
		"example.net");

	private static final List<Contact> CONTACTS = generate();

	private ContactsDatabase()
	{
	}

	/**
	 * @return all contacts, by id; the list cannot be changed
	 */
	public static List<Contact> all()
	{
		return CONTACTS;
	}

	/**
	 * @return the contact with the id, {@code null} if there is none
	 */
	public static Contact get(long id)
	{
		return id >= 1 && id <= CONTACTS.size() ? CONTACTS.get((int)id - 1) : null;
	}

	private static List<Contact> generate()
	{
		Random random = new Random(27);
		Profession[] professions = Profession.values();
		List<Contact> contacts = new ArrayList<>(SIZE);
		for (int id = 1; id <= SIZE; id++)
		{
			String firstName = FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size()));
			String lastName = LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
			// the first countries have more people: the lower of two draws
			List<String> cities = CITIES.get(Math.min(random.nextInt(CITIES.size()),
				random.nextInt(CITIES.size())));
			String city = cities.get(1 + random.nextInt(cities.size() - 1));
			LocalDate birthDate = LocalDate.of(1955, 1, 1)
				.plusDays(random.nextInt(365 * 50));
			Profession profession = professions[random.nextInt(professions.length)];
			String email = ascii(firstName) + "." + ascii(lastName) + id + "@" +
				DOMAINS.get(random.nextInt(DOMAINS.size()));
			String phone = String.format(Locale.ROOT, "+%d %03d %04d", 1 + random.nextInt(98),
				random.nextInt(1000), random.nextInt(10000));
			contacts.add(new Contact(id, firstName, lastName, email, phone, city, cities.get(0),
				birthDate, profession));
		}
		return List.copyOf(contacts);
	}

	/**
	 * @return the name in lower case without accents, for an email address
	 */
	private static String ascii(String name)
	{
		return Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT);
	}
}
