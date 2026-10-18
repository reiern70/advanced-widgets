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
package com.antilia.widgets.demo.mail;

import java.io.Serializable;

/**
 * An email, in HTML and in plain text for the programs that do not show HTML.
 *
 * @param to
 *            the address it goes to
 * @param subject
 *            the subject
 * @param text
 *            the plain text
 * @param html
 *            the HTML
 */
public record Mail(String to, String subject, String text, String html) implements Serializable
{
}
