/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.contacts;

import java.io.Serializable;

/**
 * A value of a {@link Facet} with how many contacts have it. The suggestions of the autocomplete
 * of a facet show it as JSON: {@code {"id": "ES", "label": "Spain", "count": 210}}.
 *
 * @param id
 *            the value: an ISO 3166 code, a year or the name of a {@link Profession}
 * @param label
 *            how the value reads in the language of the session
 * @param count
 *            how many of the contacts the other filters leave have the value
 */
public record FacetValue(String id, String label, int count) implements Serializable
{
}
