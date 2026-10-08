/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import org.apache.wicket.Session;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

import com.advanced.widgets.dynamicdatatable.IDynamicDataProvider;
import com.advanced.widgets.dynamicdatatable.JsonRowsResource;

/**
 * Serves the users of the JSON table demo from a mounted resource, {@value #PATH}, filtered by
 * the parameter {@value #FILTER} as the table's filter does.
 */
public class UsersRowsResource extends JsonRowsResource<User, Long>
{
	private static final long serialVersionUID = 1L;

	/** Where the resource is mounted. */
	public static final String PATH = "api/users";

	/** The parameter holding the filter by first or last name. */
	public static final String FILTER = "filter";

	/** The reference the application mounts and the table fetches from. */
	public static final ResourceReference REFERENCE = new ResourceReference(
		UsersRowsResource.class, "users")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new UsersRowsResource();
		}
	};

	@Override
	protected IDynamicDataProvider<User, Long> newProvider(PageParameters parameters)
	{
		// the session, if the browser has one, so that the countries are named in its language
		Session.get();
		UsersProvider provider = new UsersProvider();
		provider.setFilter(parameters.get(FILTER).toOptionalString());
		return provider;
	}
}
