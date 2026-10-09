/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Makes up users with random names, phones, birthdays and addresses, for the demos.
 */
public final class UserGenerator
{
	private static final UserGenerator INSTANCE = new UserGenerator();

	private static final List<String> FIRST_NAMES = List.of("Alba", "Bruno", "Carla", "Dario",
		"Elena", "Felix", "Greta", "Hugo", "Ines", "Jonas", "Karin", "Lucas", "Marta", "Nils",
		"Olga", "Pablo", "Quinn", "Rosa", "Sven", "Tania", "Ugo", "Vera", "Walter", "Ximena",
		"Yusuf", "Zoe");

	private static final List<String> LAST_NAMES = List.of("Alvarez", "Becker", "Costa",
		"Dumont", "Eriksen", "Fontana", "Garcia", "Hoffmann", "Ivanova", "Jansen", "Kowalski",
		"Lindqvist", "Moreau", "Novak", "Okafor", "Petrov", "Quintero", "Rossi", "Schmidt",
		"Tanaka", "Ueda", "Vargas", "Weber", "Yilmaz", "Zimmermann");

	private static final List<String> STREETS = List.of("Harbour Road", "Elm Street",
		"Mill Lane", "Station Square", "Garden Walk", "Bridge Street", "Orchard Way",
		"Castle Hill");

	/** Pairs of the ISO 3166 code of a country and one of its cities. */
	private static final List<List<String>> CITIES = List.of(List.of("AR", "Rosario"),
		List.of("BR", "Curitiba"), List.of("CA", "Halifax"), List.of("DE", "Leipzig"),
		List.of("ES", "Bilbao"), List.of("FR", "Nantes"), List.of("GB", "Bristol"),
		List.of("IT", "Bologna"), List.of("JP", "Sendai"), List.of("NO", "Bergen"),
		List.of("US", "Austin"), List.of("UY", "Salto"));

	private UserGenerator()
	{
	}

	/**
	 * @return the generator
	 */
	public static UserGenerator getInstance()
	{
		return INSTANCE;
	}

	/**
	 * @return a new user, not saved yet
	 */
	public User generate()
	{
		User user = new User(pick(FIRST_NAMES), pick(LAST_NAMES));
		user.setHomePhone(phone());
		user.setCellPhone(phone());
		user.setBornDate(birthday());
		List<String> city = pick(CITIES);
		user.setCountry(city.get(0));
		user.setCity(city.get(1));
		user.setAddress(random().nextInt(1, 300) + " " + pick(STREETS));
		return user;
	}

	private static String phone()
	{
		return String.format("%03d-555-%04d", random().nextInt(200, 1000),
			random().nextInt(0, 10000));
	}

	private static Date birthday()
	{
		LocalDate first = LocalDate.of(1950, 1, 1);
		LocalDate day = first.plusDays(random().nextLong(0, 365L * 50));
		return Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private static <T> T pick(List<T> choices)
	{
		return choices.get(random().nextInt(choices.size()));
	}

	private static ThreadLocalRandom random()
	{
		return ThreadLocalRandom.current();
	}
}
