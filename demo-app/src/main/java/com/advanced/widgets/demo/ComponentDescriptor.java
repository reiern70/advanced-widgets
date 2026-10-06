/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import java.io.Serializable;

/**
 * Describes one entry of the component catalog shown on the {@link HomePage}.
 */
public record ComponentDescriptor(String name, String category, String description, Status status)
	implements Serializable {

	public enum Status {
		AVAILABLE("Available"),
		IN_PROGRESS("In progress"),
		PLANNED("Planned");

		private final String label;

		Status(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}

		/** CSS modifier used to color the status badge. */
		public String cssClass() {
			return "badge--" + name().toLowerCase().replace('_', '-');
		}
	}
}
