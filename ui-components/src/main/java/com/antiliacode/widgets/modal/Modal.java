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
package com.antiliacode.widgets.modal;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.ScriptJson;
import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.scrollbar.ScrollbarBehavior;
import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookPart;
import com.antiliacode.widgets.theme.Looks;
import com.github.openjson.JSONObject;

/**
 * Wicket's {@link ModalDialog} in the markup, look and behaviors of the {@code Modal} of React and
 * Svelte, the same script of the core ({@code Wicket.Modal} from {@link #JS}) running it:
 * <ul>
 * <li>{@link #open(IModel, Component, AjaxRequestTarget, IModalButtonFactory...)} shows a title
 * in the header, a body, and in the footer the buttons the {@link IModalButtonFactory factories}
 * make, on the right;</li>
 * <li>a button in the header closes the dialog ({@link #setCloseButton(boolean)}), as Escape
 * does ({@link #setCloseOnEscape(boolean)}) and a click around the dialog may
 * ({@link #setCloseOnClick(boolean)}), and a {@link ModalCloseButton} of the footer;</li>
 * <li>before the user closes the dialog, a JavaScript function may keep it open
 * ({@link #setCanClose(String)}), {@link #confirmCloseIfModified(IModel) asking first} when a
 * field in it was modified for example;</li>
 * <li>dragging the header moves the dialog ({@link #setDraggable(boolean)}), and a double click
 * on it folds the dialog to the header and back ({@link #setFoldable(boolean)}), in the browser
 * alone;</li>
 * <li>the dialog keeps the focus while it is open ({@link #setTrapFocus(boolean)}) and gives it
 * back when it closes;</li>
 * <li>the dialog fits its content, or comes in a {@link Size} ({@link #setSize(Size)}) or a width
 * of its own ({@link #setWidth(String)});</li>
 * <li>a {@link Modal} in the body of another opens over it, nested, and takes the keys and the
 * focus until it closes.</li>
 * </ul>
 * Unlike in {@link ModalDialog}, Escape, the close button, the focus trap, dragging and folding
 * are on by default; a click around the dialog closes it only when told to. The user closing the
 * dialog calls {@link #onCloseRequest(AjaxRequestTarget)}, which closes it if the server's
 * {@link #canClose(AjaxRequestTarget)} lets it too.
 * <p>
 * The dialog has the classes of {@link ModalDialog}'s default theme without its
 * {@link org.apache.wicket.extensions.ajax.markup.html.modal.theme.DefaultTheme}: its own style
 * sheet, {@link #CSS}, shows it in the colors of the {@link com.antiliacode.widgets.theme.Theme
 * theme}. Contents of its own, given to {@link #open(Component, AjaxRequestTarget)}, use the
 * classes of {@link ModalContent}.
 *
 * @since 11.0.0
 */
public class Modal extends ModalDialog
{
	private static final long serialVersionUID = 1L;

	/** The script of the dialog, {@code Wicket.Modal}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(Modal.class,
		"antilia-modal.js");

	/** The style sheet of the dialog. */
	public static final ResourceReference CSS = new CssResourceReference(Modal.class,
		"antilia-modal.css");

	/** The class of the default theme of {@link ModalDialog}, styled by {@link #CSS}. */
	public static final String THEME_CLASS = "dialog-theme-default";

	/** The id the body given to {@link #open(IModel, Component, AjaxRequestTarget, IModalButtonFactory...)} must have. */
	public static final String BODY_ID = ModalContent.BODY_ID;

	/** The id of the overlay in the markup of {@link ModalDialog}. */
	private static final String OVERLAY_ID = "overlay";

	/**
	 * How wide a dialog is, never wider than the browser window.
	 */
	public enum Size
	{
		/** For a question or a message: 22em. */
		SMALL,
		/** For a form: 34em. */
		MEDIUM,
		/** For a table or a long text: 52em. */
		LARGE;

		/**
		 * @return the name of the size in the browser, {@code small} for example
		 */
		public String key()
		{
			return name().toLowerCase();
		}
	}

	private boolean closeOnEscape = true;

	private boolean closeOnClick;

	private boolean trapFocus = true;

	private boolean closeButton = true;

	private boolean draggable = true;

	private boolean foldable = true;

	private Size size;

	private String width;

	private String canClose;

	private IModel<String> discardQuestion;

	private final AbstractDefaultAjaxBehavior closeRequest;

	/**
	 * @param id
	 *            the component id
	 */
	public Modal(String id)
	{
		super(id);
		closeRequest = new AbstractDefaultAjaxBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				super.updateAjaxAttributes(attributes);
				postProcessCloseBehaviorAjaxAttributes(attributes);
			}

			@Override
			protected void respond(AjaxRequestTarget target)
			{
				if (isOpen())
				{
					onCloseRequest(target);
				}
			}
		};
		add(closeRequest);
	}

	@Override
	protected WebMarkupContainer newOverlay(String overlayId)
	{
		// called by the constructor of ModalDialog, before the fields of this class are set
		WebMarkupContainer overlay = super.newOverlay(overlayId);
		overlay.setOutputMarkupId(true);
		overlay.add(new LookBehavior(LookPart.DIALOG_OVERLAY));
		return overlay;
	}

	/**
	 * @return the markup id of the overlay, the element given to {@code Wicket.Modal}
	 */
	public String getOverlayMarkupId()
	{
		return get(OVERLAY_ID).getMarkupId();
	}

	@Override
	protected WebMarkupContainer newDialog(String dialogId)
	{
		WebMarkupContainer dialog = new WebMarkupContainer(dialogId)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				tag.put("role", "dialog");
				tag.put("aria-modal", "true");
			}
		};
		dialog.add(new LookBehavior(LookPart.DIALOG));
		return dialog;
	}

	@Override
	public void setContent(Component content)
	{
		super.setContent(content);
		// the element of the content, whatever component it is
		if (content.getBehaviors(ContentLook.class).isEmpty())
		{
			content.add(new ContentLook());
		}
	}

	/**
	 * Opens the dialog with a title, a body and buttons in its footer. The content is removed when
	 * the dialog closes.
	 *
	 * @param title
	 *            the title, in the header; none when {@code null}
	 * @param body
	 *            the body, whose id is {@link #BODY_ID}
	 * @param target
	 *            an optional Ajax target
	 * @param buttons
	 *            the factories of the buttons of the footer, from left to right; no footer without
	 *            any
	 * @return {@code this}
	 */
	public Modal open(IModel<String> title, Component body, AjaxRequestTarget target,
		IModalButtonFactory... buttons)
	{
		return open(title, body, Arrays.asList(buttons), target);
	}

	/**
	 * Opens the dialog with a title, a body and buttons in its footer. The content is removed when
	 * the dialog closes.
	 *
	 * @param title
	 *            the title, in the header; none when {@code null}
	 * @param body
	 *            the body, whose id is {@link #BODY_ID}
	 * @param buttons
	 *            the factories of the buttons of the footer, from left to right; no footer without
	 *            any
	 * @param target
	 *            an optional Ajax target
	 * @return {@code this}
	 */
	public Modal open(IModel<String> title, Component body,
		List<? extends IModalButtonFactory> buttons, AjaxRequestTarget target)
	{
		open(new ModalContent(CONTENT_ID, title, body, buttons), target);
		return this;
	}

	/**
	 * Called when the user asks to close the open dialog, with Escape, the close button, a click
	 * around the dialog or a {@link ModalCloseButton}, and the function of
	 * {@link #setCanClose(String)} let them. Closes the dialog if {@link #canClose(AjaxRequestTarget)}
	 * lets it.
	 *
	 * @param target
	 *            the Ajax target
	 */
	protected void onCloseRequest(AjaxRequestTarget target)
	{
		if (canClose(target))
		{
			close(target);
		}
	}

	/**
	 * Asked on the server before the dialog closes at the user's request, after the function of
	 * {@link #setCanClose(String)} in the browser; returning {@code false} keeps the dialog open,
	 * when its data cannot be left as it is for example. It may add feedback to the target.
	 *
	 * @param target
	 *            the Ajax target
	 * @return whether the dialog may close; {@code true} by default
	 */
	protected boolean canClose(AjaxRequestTarget target)
	{
		return true;
	}

	/**
	 * Escape closes the dialog; it does by default.
	 *
	 * @return {@code this}
	 */
	@Override
	public Modal closeOnEscape()
	{
		return setCloseOnEscape(true);
	}

	/**
	 * A click around the dialog closes it; it does not by default.
	 *
	 * @return {@code this}
	 */
	@Override
	public Modal closeOnClick()
	{
		return setCloseOnClick(true);
	}

	/**
	 * The dialog keeps the focus while open; it does by default.
	 *
	 * @return {@code this}
	 */
	@Override
	public Modal trapFocus()
	{
		return setTrapFocus(true);
	}

	/**
	 * @param closeOnEscape
	 *            whether Escape closes the dialog
	 * @return {@code this}
	 */
	public Modal setCloseOnEscape(boolean closeOnEscape)
	{
		this.closeOnEscape = closeOnEscape;
		return this;
	}

	/**
	 * @return whether Escape closes the dialog
	 */
	public boolean isCloseOnEscape()
	{
		return closeOnEscape;
	}

	/**
	 * @param closeOnClick
	 *            whether a click around the dialog closes it
	 * @return {@code this}
	 */
	public Modal setCloseOnClick(boolean closeOnClick)
	{
		this.closeOnClick = closeOnClick;
		return this;
	}

	/**
	 * @return whether a click around the dialog closes it
	 */
	public boolean isCloseOnClick()
	{
		return closeOnClick;
	}

	/**
	 * @param trapFocus
	 *            whether the dialog keeps the focus while open
	 * @return {@code this}
	 */
	public Modal setTrapFocus(boolean trapFocus)
	{
		this.trapFocus = trapFocus;
		return this;
	}

	/**
	 * @return whether the dialog keeps the focus while open
	 */
	public boolean isTrapFocus()
	{
		return trapFocus;
	}

	/**
	 * @param closeButton
	 *            whether the header shows a button closing the dialog
	 * @return {@code this}
	 */
	public Modal setCloseButton(boolean closeButton)
	{
		this.closeButton = closeButton;
		return this;
	}

	/**
	 * @return whether the header shows a button closing the dialog
	 */
	public boolean isCloseButton()
	{
		return closeButton;
	}

	/**
	 * @param draggable
	 *            whether dragging the header moves the dialog
	 * @return {@code this}
	 */
	public Modal setDraggable(boolean draggable)
	{
		this.draggable = draggable;
		return this;
	}

	/**
	 * @return whether dragging the header moves the dialog
	 */
	public boolean isDraggable()
	{
		return draggable;
	}

	/**
	 * @param foldable
	 *            whether a double click on the header folds the dialog to it, and back
	 * @return {@code this}
	 */
	public Modal setFoldable(boolean foldable)
	{
		this.foldable = foldable;
		return this;
	}

	/**
	 * @return whether a double click on the header folds the dialog to it, and back
	 */
	public boolean isFoldable()
	{
		return foldable;
	}

	/**
	 * @param size
	 *            how wide the dialog is; {@code null}, the default, to fit its content
	 * @return {@code this}
	 */
	public Modal setSize(Size size)
	{
		this.size = size;
		return this;
	}

	/**
	 * @return how wide the dialog is, {@code null} when it fits its content
	 */
	public Size getSize()
	{
		return size;
	}

	/**
	 * Gives the dialog a width of its own, set in the browser by the script, so it needs no inline
	 * style under a Content Security Policy.
	 *
	 * @param width
	 *            a CSS length, such as {@code 40em} or {@code 600px}, overriding the width of the
	 *            {@link #setSize(Size) size}; {@code null} for none
	 * @return {@code this}
	 */
	public Modal setWidth(String width)
	{
		this.width = width;
		return this;
	}

	/**
	 * @return the width of the dialog, if it has one of its own
	 */
	public String getWidth()
	{
		return width;
	}

	/**
	 * Sets the JavaScript function asked in the browser before the user closes the dialog;
	 * returning {@code false} keeps it open. It is given the element of the dialog, and
	 * {@code Wicket.Modal.isModified(element)} tells whether the user changed a field in it, e.g.
	 * {@code function(dialog) { return !Wicket.Modal.isModified(dialog) || confirm('Discard?'); }}.
	 *
	 * @param canClose
	 *            the function, {@code null} to close the dialog always
	 * @return {@code this}
	 * @see #confirmCloseIfModified(IModel)
	 */
	public Modal setCanClose(String canClose)
	{
		this.canClose = canClose;
		this.discardQuestion = null;
		return this;
	}

	/**
	 * @return the JavaScript function asked before the user closes the dialog, if any
	 */
	public String getCanClose()
	{
		if (discardQuestion != null)
		{
			return "function(dialog) { return !Wicket.Modal.isModified(dialog) || " +
				"window.confirm(" + ScriptJson.quote(discardQuestion.getObject()) + "); }";
		}
		return canClose;
	}

	/**
	 * Asks the user to confirm closing the dialog when a field in it was modified since it was
	 * rendered.
	 *
	 * @param question
	 *            the question asked, such as "Discard the changes?"
	 * @return {@code this}
	 */
	public Modal confirmCloseIfModified(IModel<String> question)
	{
		this.canClose = null;
		Args.notNull(question, "question");
		// a ResourceModel finds its text through this component
		this.discardQuestion = question instanceof IComponentAssignedModel<String> assigned ?
			assigned.wrapOnAssignment(this) : question;
		return this;
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		Looks.addClasses(tag, Looks.of(this), LookPart.DIALOG_THEME);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		renderHeadItems(response);
		if (isOpen())
		{
			response.render(OnDomReadyHeaderItem.forScript("Wicket.Modal.attach(" +
				ScriptJson.quote(getOverlayMarkupId()) + ", " + getOptions() + ");"));
		}
	}

	/**
	 * @return the options of {@code Wicket.Modal.attach}, as a JavaScript object
	 */
	private String getOptions()
	{
		JSONObject options = new JSONObject()
			.put("closeOnEscape", closeOnEscape)
			.put("closeOnClick", closeOnClick)
			.put("trapFocus", trapFocus)
			.put("draggable", draggable)
			.put("foldable", foldable);
		if (size != null)
		{
			options.put("size", size.key());
		}
		if (width != null)
		{
			options.put("width", width);
		}
		String json = ScriptJson.of(options);
		StringBuilder script = new StringBuilder(json.substring(0, json.length() - 1));
		script.append(",\"onClose\":function(){")
			.append(closeRequest.getCallbackScript())
			.append('}');
		String function = getCanClose();
		if (function != null)
		{
			script.append(",\"canClose\":function(){var dialog=document.getElementById(")
				.append(ScriptJson.quote(getOverlayMarkupId()))
				.append(").querySelector('.modal-dialog');return (")
				.append(function)
				.append(").call(dialog,dialog)!==false;}");
		}
		return script.append('}').toString();
	}

	@Override
	protected void onDetach()
	{
		super.onDetach();
		if (discardQuestion != null)
		{
			discardQuestion.detach();
		}
	}

	/**
	 * Renders what the dialog needs in the browser: {@link #JS} and {@link #CSS}, the scroll bars
	 * of its body, those of {@link ScrollbarBehavior}, and the buttons of its footer, those of
	 * {@link ButtonBehavior}.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contributeAjax(Application.get(), response);
		ScrollbarBehavior.renderHeadItems(response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(ButtonBehavior.CSS));
		response.render(CssHeaderItem.forReference(CSS));
	}

	/** The look of the content of the dialog. */
	private static class ContentLook extends LookBehavior
	{
		private static final long serialVersionUID = 1L;

		ContentLook()
		{
			super(LookPart.DIALOG_CONTENT);
		}
	}
}
