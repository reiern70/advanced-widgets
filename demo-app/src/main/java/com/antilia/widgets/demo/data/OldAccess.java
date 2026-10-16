/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
