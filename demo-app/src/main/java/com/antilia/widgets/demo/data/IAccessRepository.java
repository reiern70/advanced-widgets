/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.data;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The {@link Access}es in the table {@code accesses}.
 */
public interface IAccessRepository extends JpaRepository<Access, Long>
{
}
