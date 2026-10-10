/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.data;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Keeps a {@link Language} by its ISO 639 code.
 */
@Converter
public class LanguageConverter implements AttributeConverter<Language, String>
{
	@Override
	public String convertToDatabaseColumn(Language language)
	{
		return language == null ? null : language.getCode();
	}

	@Override
	public Language convertToEntityAttribute(String code)
	{
		return code == null ? null : Language.ofCode(code);
	}
}
