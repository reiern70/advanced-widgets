/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * The {@link OldAccess}es in the table {@code old_accesses}, also filtered, see
 * {@link AccessFilter}.
 */
public interface IOldAccessRepository
	extends
		JpaRepository<OldAccess, Long>,
		JpaSpecificationExecutor<OldAccess>
{
}
