/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antiliacode.widgets.form;

import java.util.UUID;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;

import com.antiliacode.widgets.ScriptJson;

/**
 * Refuses to process a form twice from the same version: a double click, Enter pressed twice, a
 * submit queued behind another one or a page veil missed send the form once, however many times
 * the browser sends it.
 * <p>
 * The form has a version, kept in the session ({@link FormVersions}) and written on its tag,
 * {@value #ATTRIBUTE}. A submit carries the version the form had when the user acted, as the
 * parameter {@value #PARAMETER_PREFIX} followed by the form's markup id: the script
 * ({@code Wicket.FormVersion}) reads it at {@code /ajax/call/init}, within the click, for an Ajax
 * submit, and writes it into a hidden input for a normal one. The version is taken at the click
 * because Wicket's Ajax serializes a queued submit's form only when it sends it, after the
 * previous answer has been applied, so a second click queued behind the first would otherwise
 * send the new version and pass.
 * <p>
 * Wicket's {@link Form} takes every behavior that is an {@link IFormValidator} for a form
 * validator, so this behavior is one: a submit carrying the form's version raises it, and an Ajax
 * answer tells the browser the new one; a submit carrying another version fails the validation,
 * so {@code onSubmit} is not called and no model changes, and the form gets the error
 * {@value #ALREADY_SENT_KEY} ({@link #onRefused(Form)}). {@link DoubleSubmitBulletsProofForm} is a
 * form with this behavior.
 * <p>
 * What it does not cover:
 * <ul>
 * <li>A submit without a version is taken, so that a script that did not load blocks no form; so
 * is every submit of a form whose session is temporary, on a stateless page.</li>
 * <li>Only the submits of a form count, not the updates of one of its fields: a button whose
 * {@code defaultFormProcessing} is off skips the validation.</li>
 * <li>A click on a component that the first click's answer removed ends in Wicket's
 * {@code ComponentNotFoundException} before any form is processed: that is for a page veil to
 * prevent.</li>
 * <li>A user who goes back and sends a page shown again sends its new version: a business rule that
 * must not run twice, such as issuing an invoice, still checks it in the domain.</li>
 * </ul>
 * With nested forms, each has its own version, and a submit carries that of the nearest form
 * around the component submitting it, the one Wicket processes.
 */
public class FormVersionBehavior extends Behavior implements IFormValidator
{
	private static final long serialVersionUID = 1L;

	/** The attribute of the form's tag holding its version. */
	public static final String ATTRIBUTE = "data-antilia-form-version";

	/** Followed by the markup id of the form, the parameter of a submit carrying its version. */
	public static final String PARAMETER_PREFIX = "antilia-form-version:";

	/** The key of the error a form gets when a submit carries another version than its own. */
	public static final String ALREADY_SENT_KEY = "FormVersionBehavior.alreadySent";

	/** The script stamping the submits with the version. */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		FormVersionBehavior.class, "antilia-form-version.js");

	/** The error in English, when no bundle has {@value #ALREADY_SENT_KEY}. */
	static final String ALREADY_SENT = "This form was already sent, or changed while you " +
		"were sending it. Check it and send it again.";

	/** The form's key in the session: the same in every stored version of its page. */
	private final String key = UUID.randomUUID().toString();

	private Form<?> form;

	@Override
	public void bind(Component component)
	{
		super.bind(component);
		if (!(component instanceof Form))
		{
			throw new IllegalArgumentException(FormVersionBehavior.class.getSimpleName() +
				" can only be added to a form, not to " + component.getClass().getName());
		}
		if (form != null && form != component)
		{
			throw new IllegalStateException(FormVersionBehavior.class.getSimpleName() +
				" is already bound to " + form.getPageRelativePath() +
				" and cannot be bound to another form");
		}
		form = (Form<?>)component;
		component.setOutputMarkupId(true);
	}

	@Override
	public void unbind(Component component)
	{
		form = null;
		super.unbind(component);
	}

	/**
	 * @return the form's version: 0 until a submit of it was taken
	 */
	public int getVersion()
	{
		FormVersions versions = Session.exists() ? FormVersions.of(Session.get()) : null;
		return versions == null ? 0 : versions.current(key);
	}

	@Override
	public void onComponentTag(Component component, ComponentTag tag)
	{
		super.onComponentTag(component, tag);
		tag.put(ATTRIBUTE, getVersion());
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		CoreLibrariesContributor.contribute(component.getApplication(), response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(OnDomReadyHeaderItem.forScript("Wicket.FormVersion.watch();"));
	}

	/**
	 * @return none: the form's version is checked whatever fields it has
	 */
	@Override
	public FormComponent<?>[] getDependentFormComponents()
	{
		return null;
	}

	/**
	 * Takes the submit when it carries the form's version, raising it, and refuses it when it
	 * carries another one.
	 */
	@Override
	public void validate(Form<?> form)
	{
		String sent = form.getRequest()
			.getRequestParameters()
			.getParameterValue(PARAMETER_PREFIX + form.getMarkupId())
			.toOptionalString();
		Session session = Session.get();
		if (sent == null || session.isTemporary())
		{
			return;
		}
		int next = FormVersions.take(session, key, sent);
		if (next < 0)
		{
			onRefused(form);
			return;
		}
		form.getRequestCycle()
			.find(IPartialPageRequestHandler.class)
			.ifPresent(target -> target.appendJavaScript("Wicket.FormVersion.set(" +
				ScriptJson.quote(form.getMarkupId()) + ", " + next + ");"));
	}

	/**
	 * Called when a submit carried another version than the form's: the form was already sent from
	 * that version, or changed while it was being sent. By default the form gets the error
	 * {@value #ALREADY_SENT_KEY}, which fails the validation; an override that reports nothing must
	 * still give the form an error, or the submit is processed.
	 *
	 * @param form
	 *            the form
	 */
	protected void onRefused(Form<?> form)
	{
		form.error(form.getString(ALREADY_SENT_KEY, null, ALREADY_SENT));
	}
}
