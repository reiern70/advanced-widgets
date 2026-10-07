/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import java.io.Serializable;

/**
 * Describes one entry of the component catalog shown on the {@link HomePage}. Its texts are in the
 * bundle of the home page: the name under {@code catalog.<id>}, the description under
 * {@code catalog.<id>.description}; the category is a key under {@code category.} in the bundle of
 * {@link BasePage}.
 */
public record ComponentDescriptor(String id, String category, Status status)
	implements Serializable {

	/**
	 * @return the key of the name
	 */
	public String nameKey() {
		return "catalog." + id;
	}

	/**
	 * @return the key of the description
	 */
	public String descriptionKey() {
		return "catalog." + id + ".description";
	}

	/**
	 * @return the key of the category
	 */
	public String categoryKey() {
		return "category." + category;
	}

	public enum Status {
		AVAILABLE,
		IN_PROGRESS,
		PLANNED;

		/**
		 * @return the key of the label, in the bundle of the home page
		 */
		public String labelKey() {
			return "status." + name();
		}

		/** CSS modifier used to color the status badge. */
		public String cssClass() {
			return "badge--" + name().toLowerCase().replace('_', '-');
		}
	}
}
