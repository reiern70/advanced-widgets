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
