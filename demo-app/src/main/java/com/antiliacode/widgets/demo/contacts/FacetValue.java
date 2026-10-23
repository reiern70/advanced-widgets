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
package com.antiliacode.widgets.demo.contacts;

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
