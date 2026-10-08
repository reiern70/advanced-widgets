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
		new ComponentDescriptor("dataGrid", "data", Status.PLANNED),
		new ComponentDescriptor("treeTable", "data", Status.PLANNED),
		new ComponentDescriptor("liveFeed", "realTime", Status.PLANNED),
		new ComponentDescriptor("notifications", "realTime", Status.PLANNED),
		new ComponentDescriptor("autocomplete", "forms", Status.AVAILABLE),
		new ComponentDescriptor("dateRangePicker", "forms", Status.PLANNED),
		new ComponentDescriptor("fileUpload", "forms", Status.PLANNED),
		new ComponentDescriptor("modalDialog", "layout", Status.PLANNED),
		new ComponentDescriptor("splitPanel", "layout", Status.PLANNED));

	private ComponentCatalog() {
	}

	public static List<ComponentDescriptor> components() {
		return COMPONENTS;
	}
}
