/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

/**
 * The default template engine, shipped with the table's script.
 * <p>
 * It knows two placeholders and nothing else: {@code {{a.b}}} writes the value at the dotted
 * path, HTML-escaped, and {@code {{{a.b}}}} writes it unescaped. A missing value writes nothing.
 * It does not evaluate code, so it works under a Content Security Policy without
 * {@code unsafe-eval}.
 *
 * @since 11.0.0
 */
public class BuiltInTemplateEngine implements ITemplateEngine
{
	private static final long serialVersionUID = 1L;

	/** The shared instance. */
	public static final BuiltInTemplateEngine INSTANCE = new BuiltInTemplateEngine();

	@Override
	public String getName()
	{
		return "builtin";
	}
}
