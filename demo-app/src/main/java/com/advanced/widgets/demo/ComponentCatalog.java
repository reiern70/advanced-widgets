/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import java.util.List;

import com.advanced.widgets.demo.ComponentDescriptor.Status;

/**
 * The widgets offered by the ui-components module, as listed on the landing page.
 */
public final class ComponentCatalog {

	private static final List<ComponentDescriptor> COMPONENTS = List.of(
		new ComponentDescriptor("Data Grid", "Data",
			"Sortable, pageable grid with resizable columns, inline editing and row selection.",
			Status.PLANNED),
		new ComponentDescriptor("Tree Table", "Data",
			"Hierarchical rows with lazy loading of children and expand/collapse state.",
			Status.PLANNED),
		new ComponentDescriptor("Live Feed", "Real-time",
			"List that receives new entries pushed from the server over websockets.",
			Status.PLANNED),
		new ComponentDescriptor("Notifications", "Real-time",
			"Toast notifications broadcast to one user or all connected sessions.",
			Status.PLANNED),
		new ComponentDescriptor("Autocomplete", "Forms",
			"Type-ahead input with remote suggestions, keyboard navigation and multi-select.",
			Status.PLANNED),
		new ComponentDescriptor("Date Range Picker", "Forms",
			"Pick a start and end date with presets such as last 7 days or this month.",
			Status.PLANNED),
		new ComponentDescriptor("File Upload", "Forms",
			"Drag and drop upload with per-file progress reported through websockets.",
			Status.PLANNED),
		new ComponentDescriptor("Modal Dialog", "Layout",
			"Accessible dialog with focus trapping, stacking and Ajax content.",
			Status.PLANNED),
		new ComponentDescriptor("Split Panel", "Layout",
			"Resizable horizontal or vertical panes that remember their sizes.",
			Status.PLANNED));

	private ComponentCatalog() {
	}

	public static List<ComponentDescriptor> components() {
		return COMPONENTS;
	}
}
