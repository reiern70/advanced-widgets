/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
