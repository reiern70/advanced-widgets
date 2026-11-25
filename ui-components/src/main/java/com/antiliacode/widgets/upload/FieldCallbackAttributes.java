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
package com.antiliacode.widgets.upload;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxAttributeName;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.danekja.java.util.function.serializable.SerializableConsumer;

import com.github.openjson.JSONObject;

/**
 * Renders the attributes of the requests to the callback of Wicket's FileUploadToResourceField,
 * whose URL its script knows: the behavior hears of no request of its own.
 */
final class FieldCallbackAttributes extends AbstractDefaultAjaxBehavior
{
	private static final long serialVersionUID = 1L;

	private final SerializableConsumer<AjaxRequestAttributes> update;

	/**
	 * @param update
	 *            changes the attributes, as the field lets its subclasses do
	 */
	FieldCallbackAttributes(SerializableConsumer<AjaxRequestAttributes> update)
	{
		this.update = update;
	}

	@Override
	protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
	{
		super.updateAjaxAttributes(attributes);
		attributes.setMethod(AjaxRequestAttributes.Method.POST);
		update.accept(attributes);
	}

	@Override
	protected void postprocessConfiguration(JSONObject attributesJson, Component component)
	{
		super.postprocessConfiguration(attributesJson, component);
		// the URL is the callback's, and Wicket's field sends no field with the requests
		attributesJson.remove(AjaxAttributeName.URL.jsonName());
		attributesJson.remove(AjaxAttributeName.MARKUP_ID.jsonName());
	}

	/**
	 * @return the attributes, as the script of the field takes them
	 */
	CharSequence renderAttributes()
	{
		return renderAjaxAttributes(getComponent());
	}

	@Override
	public boolean canCallListener(Component component)
	{
		// the requests go to the callback of Wicket's field
		return false;
	}

	@Override
	protected void respond(AjaxRequestTarget target)
	{
	}
}
