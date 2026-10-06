/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.advanced.widgets.dynamicdatatable.ColumnState;
import org.junit.jupiter.api.Test;

/**
 * Saves and loads column states with {@link FileColumnStateStore}.
 */
class FileColumnStateStoreTest
{
	@Test
	void aSavedStateIsLoadedAgain()
	{
		String key = "test:" + UUID.randomUUID();
		ColumnState state = new ColumnState(List.of("0", "2", "1"), List.of("2"),
			Map.of("0", 12.5, "1", 40d));

		assertNull(FileColumnStateStore.INSTANCE.load(key));
		FileColumnStateStore.INSTANCE.save(key, state);

		assertEquals(state, new FileColumnStateStore().load(key));
	}
}
