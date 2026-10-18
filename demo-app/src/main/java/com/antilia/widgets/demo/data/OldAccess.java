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

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An {@link Access} older than {@link Accesses#KEPT}, moved out of the table of the accesses by
 * {@link Accesses#archive(Instant)} with its id.
 */
@Entity
@Table(name = "old_accesses")
public class OldAccess extends AbstractAccess
{
	private static final long serialVersionUID = 1L;

	@Id
	private Long id;

	protected OldAccess()
	{
	}

	@Override
	public Long getId()
	{
		return id;
	}
}
