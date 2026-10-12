/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

/**
 * A page with the carousel of all demos, whose texts are in the bundle of {@link BasePage}.
 */
public class CarouselTestPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	public CarouselTestPage()
	{
		add(new DemoCarousel("carousel", Demos.all()));
	}
}
