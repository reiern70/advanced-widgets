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
package com.antiliacode.widgets.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxUtils;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes.Method;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.ScriptJson;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antiliacode.widgets.dynamicdatatable.BuiltInTemplateEngine;
import com.antiliacode.widgets.dynamicdatatable.IJsonSerializer;
import com.antiliacode.widgets.dynamicdatatable.ITemplateEngine;
import com.antiliacode.widgets.dynamicdatatable.JacksonJsonSerializer;
import com.antiliacode.widgets.dynamicdatatable.ResourceTemplate;
import com.antiliacode.widgets.icon.IIcon;
import com.antiliacode.widgets.jsonlist.IItemAction;
import com.antiliacode.widgets.scrollbar.ScrollbarBehavior;
import com.antiliacode.widgets.template.Templates;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.antiliacode.widgets.theme.LookPart;
import com.antiliacode.widgets.theme.Looks;

/**
 * A tree whose nodes are rendered in the browser from JSON, the way the rows of a JSON table are:
 * the Wicket side of the core's {@code JsonTree}, which {@code @antilia/react} and
 * {@code @antilia/svelte} offer as {@code Tree}.
 * <p>
 * The tree is a single component without children: Wicket renders only its {@code <div>}, with
 * the roots, and the children of the folders {@link #expand(Object) expanded}, as JSON in its
 * script. The children of another folder come as JSON from the tree's own endpoint the first time
 * it is expanded. Each node is the {@link #setNodeTemplate(String) node template} evaluated
 * against the node's JSON, its {@link #getNodeData(Object) data}, by the
 * {@link #setTemplateEngine template engine}; templates refer to the node's key as
 * {@code {{@key}}} and to its level, counting from 1, as {@code {{@level}}}.
 * <p>
 * The tree is a WAI-ARIA tree: Up and Down move to the node before and after, Right expands a
 * folder or moves to its first child, Left collapses it or moves to its parent, Home and End move
 * to the first and the last node, Enter and Space select a leaf or expand and collapse a folder.
 * A click does the same. Only leaves are selected, unless {@link #setSelectBranches(boolean)} has
 * folders selected too: then a click, Enter or Space selects a folder, and expands it if it is
 * collapsed, its arrow still expanding and collapsing it. The browser marks the node selected
 * right away and tells the tree, which calls {@link #onSelect(AjaxRequestTarget, Object)}, where
 * the application updates what shows the node. Expanding and collapsing is done in the browser; the
 * tree is told, so it renders the same folders expanded when it is rendered again.
 * <p>
 * The {@link IJsonTreeProvider provider} knows the key of every node, which ties a node in the
 * browser to one on the server. Links and buttons in the nodes are plain markup, not components:
 * a click on an element with a {@code data-dt-action} attribute sends the action and the node's
 * key to the tree, which looks the node up and hands it to the {@link IItemAction} registered for
 * the action with {@link #addAction(IItemAction)}; a click on a node that has gone since it was
 * rendered ends up in {@link #onNotFoundAction(AjaxRequestTarget, String, Serializable, Object)}.
 * <p>
 * The application can have the browser load the nodes again, keeping the folders expanded and
 * the leaf selected, with {@link #refresh(IPartialPageRequestHandler)}, and repaint one node with
 * {@link #updateNode(IPartialPageRequestHandler, Object)}.
 * <p>
 * The nodes have the icons of {@link TreeIcon}, or those the application gives, for all the
 * nodes with {@link #setIcon(TreeIcon, IIcon)} and for one with {@link #getIcon(Object)}. While
 * the roots load and when there are none the tree says so, in the texts {@code Tree.loading} and
 * {@code Tree.empty} of the bundle of ui-components.
 * <p>
 * Given a {@link #setHeight(int, BodyHeightUnits) height}, the tree scrolls itself, with slim
 * scroll bars in the colors of the theme; else the page scrolls.
 * <p>
 * The tree carries the CSS classes {@value #CSS_CLASS} and {@value Tree#CSS_CLASS}, and takes its
 * colors from the {@link com.antiliacode.widgets.theme.Theme theme} it is rendered in, if any.
 * <p>
 * The node template is written into the page as markup and has to be authored by the developer.
 * Every property the {@link #setJsonSerializer serializer} produces for the data of a node is sent
 * to the browser.
 *
 * @param <T>
 *            the type of the nodes
 * @param <K>
 *            the type of the keys of the nodes
 * @see JsonBasedTreeGrid
 * @since 11.0.0
 */
public class JsonBasedTree<T, K extends Serializable> extends WebComponent
{
	private static final long serialVersionUID = 1L;

	/** The CSS class of the trees rendered from JSON. */
	public static final String CSS_CLASS = "json-tree";

	/** The action the browser sends when the user selects a node. */
	public static final String SELECT_ACTION = "_antilia-select";

	/** The action the browser sends when the user expands a folder. */
	public static final String EXPAND_ACTION = "_antilia-expand";

	/** The action the browser sends when the user collapses a folder. */
	public static final String COLLAPSE_ACTION = "_antilia-collapse";

	private static final ResourceReference JAVASCRIPT = new JavaScriptResourceReference(
		JsonBasedTree.class, "json-tree.js");

	/** The parameter asking the data endpoint for the children of a node. */
	private static final String PARENT_PARAMETER = "parent";

	private static final String ACTION_PARAMETER = "action";

	private static final String KEY_PARAMETER = "key";

	private final IJsonTreeProvider<T, K> provider;

	private final Map<String, IItemAction<T>> actions = new LinkedHashMap<>();

	private final Map<TreeIcon, IIcon> icons = new EnumMap<>(TreeIcon.class);

	private final Set<K> expanded = new LinkedHashSet<>();

	private final ActionBehavior actionBehavior;

	private final DataBehavior dataBehavior;

	private K selected;

	private boolean selectBranches;

	private String nodeTemplate;

	private ResourceTemplate resourceTemplate;

	private ITemplateEngine templateEngine = BuiltInTemplateEngine.INSTANCE;

	private IJsonSerializer jsonSerializer = JacksonJsonSerializer.INSTANCE;

	private IModel<String> label;

	private int height = -1;

	private BodyHeightUnits heightUnits = BodyHeightUnits.PIXELS;

	private transient Map<String, String> templates;

	/**
	 * @param id
	 *            component id
	 * @param provider
	 *            supplies the nodes and resolves them back from their keys
	 */
	public JsonBasedTree(String id, IJsonTreeProvider<T, K> provider)
	{
		super(id);
		this.provider = Args.notNull(provider, "provider");
		setOutputMarkupId(true);

		add(actionBehavior = new ActionBehavior());
		add(dataBehavior = new DataBehavior());
	}

	/**
	 * Registers the action handling the clicks on the elements of the nodes whose
	 * {@code data-dt-action} attribute is the action's id.
	 *
	 * @param action
	 *            the action
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             if another action is registered with the same id
	 */
	public JsonBasedTree<T, K> addAction(IItemAction<T> action)
	{
		Args.notNull(action, "action");
		String actionId = Args.notEmpty(action.getActionId(), "actionId");
		IItemAction<T> registered = actions.putIfAbsent(actionId, action);
		if (registered != null && registered != action)
		{
			throw new IllegalArgumentException(
				"Another action is already registered as '" + actionId + "'");
		}
		return this;
	}

	/**
	 * Called when the user selects a leaf, or a folder when they are selected too, which the
	 * browser shows selected already. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param node
	 *            the node
	 */
	protected void onSelect(AjaxRequestTarget target, T node)
	{
	}

	/**
	 * Called when a click cannot be dispatched: the node no longer exists, for example because it
	 * was deleted after it was rendered, or no action is registered for it. Does nothing by
	 * default.
	 * <p>
	 * Both the action and the key come from the browser.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param action
	 *            the action, {@value #SELECT_ACTION} for the selection of a leaf
	 * @param key
	 *            the key the node was looked up by, or {@code null} if the browser sent no valid
	 *            key
	 * @param node
	 *            the node, or {@code null} if it no longer exists
	 */
	protected void onNotFoundAction(AjaxRequestTarget target, String action, K key, T node)
	{
	}

	/**
	 * Expands a folder when the tree is rendered next, for example before it is rendered the
	 * first time: its children are sent along with the tree.
	 *
	 * @param node
	 *            the folder
	 * @return {@code this}
	 */
	public JsonBasedTree<T, K> expand(T node)
	{
		expanded.add(provider.keyOf(node));
		return this;
	}

	/**
	 * Collapses a folder when the tree is rendered next.
	 *
	 * @param node
	 *            the folder
	 * @return {@code this}
	 */
	public JsonBasedTree<T, K> collapse(T node)
	{
		expanded.remove(provider.keyOf(node));
		return this;
	}

	/**
	 * @return whether a folder is expanded, as the tree was last told
	 */
	public boolean isExpanded(T node)
	{
		return expanded.contains(provider.keyOf(node));
	}

	/**
	 * @return whether folders are selected too, not only leaves
	 */
	public boolean isSelectBranches()
	{
		return selectBranches;
	}

	/**
	 * Has folders selected too, not only leaves: a click on a folder, Enter or Space selects it,
	 * and expands it if it is collapsed; a click on its arrow still expands or collapses it.
	 *
	 * @param selectBranches
	 *            whether folders are selected too
	 * @return {@code this}
	 */
	public JsonBasedTree<T, K> setSelectBranches(boolean selectBranches)
	{
		this.selectBranches = selectBranches;
		return this;
	}

	/**
	 * Selects a node when the tree is rendered next, without telling
	 * {@link #onSelect(AjaxRequestTarget, Object)}.
	 *
	 * @param node
	 *            the leaf, or a folder when they are selected too, {@code null} for none
	 * @return {@code this}
	 */
	public JsonBasedTree<T, K> setSelected(T node)
	{
		selected = node != null ? provider.keyOf(node) : null;
		return this;
	}

	/**
	 * @return the node selected, {@code null} if none is, or it no longer exists
	 */
	public T getSelected()
	{
		return selected != null ? provider.findByKey(selected) : null;
	}

	/**
	 * Has the browser load the nodes it shows again, keeping the folders expanded and the leaf
	 * selected, for example after nodes were added or deleted.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 */
	public void refresh(IPartialPageRequestHandler target)
	{
		target.appendJavaScript("if (" + getVarName() + ") { " + getVarName() + ".reload(); }");
	}

	/**
	 * Has the browser repaint one node, if it knows it, from its current state, leaving the others
	 * alone, for example after it was edited.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 * @param node
	 *            the node in its current state
	 */
	public void updateNode(IPartialPageRequestHandler target, T node)
	{
		Args.notNull(node, "node");
		target.appendJavaScript("if (" + getVarName() + ") { " + getVarName() + ".renderNode(" +
			ScriptJson.quote(getNodeKey(node)) + ", " +
			ScriptJson.escape(jsonSerializer.toJson(getNodeData(node))) + "); }");
	}

	/**
	 * @return the key of a node as the browser shows it
	 */
	public String getNodeKey(T node)
	{
		return provider.formatKey(provider.keyOf(node));
	}

	/**
	 * Resolves a node from a key as the browser shows it, for example one sent with a request.
	 *
	 * @param key
	 *            the key as the browser shows it, possibly not a key at all
	 * @return the node, or {@code null} if there is none for the key
	 */
	public T findNode(String key)
	{
		K parsed = provider.parseKey(key);
		return parsed != null ? provider.findByKey(parsed) : null;
	}

	/**
	 * @return the provider of the nodes
	 */
	public IJsonTreeProvider<T, K> getProvider()
	{
		return provider;
	}

	/**
	 * @param node
	 *            a node
	 * @return what the template of the node is rendered from, serialized as JSON: by default the
	 *         node itself
	 */
	protected Object getNodeData(T node)
	{
		return node;
	}

	/**
	 * @param node
	 *            a node
	 * @return an icon of the node's own, or {@code null}, as by default, for the folder or the
	 *         leaf icon of the tree
	 */
	protected IIcon getIcon(T node)
	{
		return null;
	}

	/**
	 * Gives the tree an icon of the application's own.
	 *
	 * @param slot
	 *            the icon replaced
	 * @param icon
	 *            the icon, {@code null} for the tree's own again
	 * @return {@code this}
	 */
	public JsonBasedTree<T, K> setIcon(TreeIcon slot, IIcon icon)
	{
		if (icon == null)
		{
			icons.remove(slot);
		}
		else
		{
			icons.put(slot, icon);
		}
		return this;
	}

	/**
	 * @param label
	 *            the name of the tree for screen readers
	 * @return {@code this}
	 */
	public JsonBasedTree<T, K> setLabel(IModel<String> label)
	{
		this.label = wrap(label);
		return this;
	}

	/**
	 * @return the height of the tree in {@link #getHeightUnits()}, or a negative number,
	 *         {@code -1} by default, when the tree is as high as its nodes
	 */
	public int getHeight()
	{
		return height;
	}

	/**
	 * @return the unit of {@link #getHeight()}, {@link BodyHeightUnits#PIXELS} by default
	 */
	public BodyHeightUnits getHeightUnits()
	{
		return heightUnits;
	}

	/**
	 * Gives the tree a fixed height, within which it scrolls, with slim scroll bars in the colors
	 * of the theme, drawn by a {@link ScrollbarBehavior} script; the head of a tree grid stays in
	 * place above the rows. Without a height, the tree is as high as its nodes.
	 *
	 * @param height
	 *            the height of the tree in the given units; any negative number, such as
	 *            {@code -1}, to let the tree take the height of its nodes, as by default
	 * @param units
	 *            the units of the height
	 * @return {@code this}
	 */
	public JsonBasedTree<T, K> setHeight(int height, BodyHeightUnits units)
	{
		Args.isTrue(height != 0, "the height has to be positive, or negative for none");
		heightUnits = Args.notNull(units, "units");
		this.height = height;
		return this;
	}

	/**
	 * Sets the template of the nodes, trusted markup written into the page as is. Without one,
	 * the template is read from an {@code .html} file named after the class of the tree, or one of
	 * its superclasses, as the markup of a panel is.
	 *
	 * @param template
	 *            the template, evaluated by the {@link #setTemplateEngine(ITemplateEngine) template
	 *            engine} against the JSON of each node
	 * @return {@code this}
	 */
	public JsonBasedTree<T, K> setNodeTemplate(String template)
	{
		nodeTemplate = Args.notEmpty(template, "template");
		resourceTemplate = null;
		return this;
	}

	/**
	 * Reads the template of the nodes from {@code name.html} in the package of {@code scope}, for
	 * the style, variation and locale of the tree, as the markup of a panel is.
	 *
	 * @param scope
	 *            the class whose package holds the template
	 * @param name
	 *            the name of the template, without the extension
	 * @return {@code this}
	 */
	public JsonBasedTree<T, K> setNodeTemplate(Class<?> scope, String name)
	{
		resourceTemplate = ResourceTemplate.named(scope, name);
		nodeTemplate = null;
		return this;
	}

	/**
	 * @return the template of the nodes as rendered in this tree, for its style, variation and
	 *         locale
	 */
	public String getNodeTemplate()
	{
		if (nodeTemplate != null)
		{
			return nodeTemplate;
		}
		ResourceTemplate template = resourceTemplate != null ? resourceTemplate
			: ResourceTemplate.forClass(getClass(), JsonBasedTree.class);
		String style = getStyle();
		String variation = getVariation();
		Locale locale = getLocale();
		if (getApplication().usesDevelopmentConfig())
		{
			return template.load(style, variation, locale);
		}
		if (templates == null)
		{
			templates = new HashMap<>();
		}
		return templates.computeIfAbsent(template.getCacheKey(style, variation, locale),
			key -> template.load(style, variation, locale));
	}

	/**
	 * @param templateEngine
	 *            the engine evaluating the templates in the browser
	 * @return {@code this}
	 */
	public JsonBasedTree<T, K> setTemplateEngine(ITemplateEngine templateEngine)
	{
		this.templateEngine = Args.notNull(templateEngine, "templateEngine");
		return this;
	}

	/**
	 * @return the serializer turning the data of the nodes into JSON
	 */
	public IJsonSerializer getJsonSerializer()
	{
		return jsonSerializer;
	}

	/**
	 * @param jsonSerializer
	 *            the serializer turning the data of the nodes into JSON
	 * @return {@code this}
	 */
	public JsonBasedTree<T, K> setJsonSerializer(IJsonSerializer jsonSerializer)
	{
		this.jsonSerializer = Args.notNull(jsonSerializer, "jsonSerializer");
		return this;
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		checkComponentTag(tag, "div");
		super.onComponentTag(tag);
		Looks.addClasses(tag, Looks.of(this), LookPart.TREE_JSON, getTreePart());
	}

	/**
	 * @return the part of the kind of tree, whose class the style sheet of the trees styles
	 */
	LookPart getTreePart()
	{
		return LookPart.TREE;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		CoreLibrariesContributor.contributeAjax(getApplication(), response);
		if (height > 0)
		{
			ScrollbarBehavior.renderHeadItems(response);
		}
		response.render(JavaScriptHeaderItem.forReference(Templates.JS));
		response.render(JavaScriptHeaderItem.forReference(JAVASCRIPT));
		response.render(CssHeaderItem.forReference(Tree.CSS));
		templateEngine.renderHead(response);

		String send = actionBehavior.getCallbackFunction(
			CallbackParameter.explicit(ACTION_PARAMETER), CallbackParameter.explicit(KEY_PARAMETER))
			.toString();
		String callbacks = "(function (send) { return { " +
			"select: function (key) { send(" + ScriptJson.quote(SELECT_ACTION) + ", key); }, " +
			"action: function (action, key) { send(action, key); }, " +
			"toggle: function (key, expanded) { send(expanded ? " +
			ScriptJson.quote(EXPAND_ACTION) + " : " + ScriptJson.quote(COLLAPSE_ACTION) +
			", key); } }; })(" + send + ")";
		response.render(OnDomReadyHeaderItem.forScript("new Wicket.JsonTree(" +
			ScriptJson.of(buildConfig()) + ", " + callbacks + ", " +
			ScriptJson.escape(jsonSerializer.toJson(toNodes(provider.getRoots()))) + ");"));
	}

	/**
	 * @return the configuration of the tree in the browser
	 */
	protected JSONObject buildConfig()
	{
		JSONObject config = new JSONObject();
		config.put("id", getMarkupId());
		config.put("dataUrl", dataBehavior.getCallbackUrl().toString());
		config.put("engine", templateEngine.getName());
		config.put("template", getNodeTemplate());
		if (label != null && label.getObject() != null)
		{
			config.put("label", label.getObject());
		}
		JSONArray expandedKeys = new JSONArray();
		for (K key : expanded)
		{
			expandedKeys.put(provider.formatKey(key));
		}
		config.put("expanded", expandedKeys);
		if (selected != null)
		{
			config.put("selected", provider.formatKey(selected));
		}
		if (selectBranches)
		{
			config.put("selectBranches", true);
		}
		if (height > 0)
		{
			config.put("height", new JSONObject().put("value", height)
				.put("unit", heightUnits.getCssUnit()));
		}
		if (!icons.isEmpty())
		{
			JSONObject markup = new JSONObject();
			markup.put("toggle", iconMarkup(TreeIcon.TOGGLE));
			markup.put("folder", iconMarkup(TreeIcon.FOLDER));
			markup.put("folderOpen", iconMarkup(TreeIcon.FOLDER_OPEN));
			markup.put("leaf", iconMarkup(TreeIcon.LEAF));
			config.put("icons", markup);
		}
		JSONObject texts = new JSONObject();
		texts.put("loading", getString("Tree.loading", null, "Loading…"));
		texts.put("empty", getString("Tree.empty", null, "Nothing to show"));
		config.put("texts", texts);
		return config;
	}

	private String iconMarkup(TreeIcon slot)
	{
		return icons.getOrDefault(slot, slot).getMarkup();
	}

	/**
	 * @return the nodes as the browser reads them, with the children of those expanded
	 */
	private List<Map<String, Object>> toNodes(Iterator<? extends T> nodes)
	{
		List<Map<String, Object>> json = new ArrayList<>();
		while (nodes.hasNext())
		{
			T node = nodes.next();
			boolean leaf = !provider.hasChildren(node);
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("key", getNodeKey(node));
			entry.put("leaf", leaf);
			entry.put("data", getNodeData(node));
			IIcon icon = getIcon(node);
			if (icon != null)
			{
				entry.put("icon", icon.getMarkup());
			}
			if (!leaf && expanded.contains(provider.keyOf(node)))
			{
				entry.put("children", toNodes(provider.getChildren(node)));
			}
			json.add(entry);
		}
		return json;
	}

	/**
	 * @return the expression evaluating to the tree's client-side instance
	 */
	private String getVarName()
	{
		return "Wicket.JsonTree.instances[" + ScriptJson.quote(getMarkupId()) + "]";
	}

	@Override
	protected void onRemove()
	{
		super.onRemove();
		AjaxUtils.executeIfAjaxOrWebSockets(target -> target.appendJavaScript(
			"if (" + getVarName() + ") { " + getVarName() + ".destroy(); }"));
	}

	@Override
	protected void onDetach()
	{
		provider.detach();
		for (IItemAction<T> action : actions.values())
		{
			action.detach();
		}
		if (label != null)
		{
			label.detach();
		}
		super.onDetach();
	}

	private void onAction(AjaxRequestTarget target, String action, K key, T node)
	{
		if (EXPAND_ACTION.equals(action) || COLLAPSE_ACTION.equals(action))
		{
			if (key != null && EXPAND_ACTION.equals(action))
			{
				expanded.add(key);
			}
			else if (key != null)
			{
				expanded.remove(key);
			}
			return;
		}
		if (node == null)
		{
			onNotFoundAction(target, action, key, null);
		}
		else if (SELECT_ACTION.equals(action))
		{
			if (!selectBranches && provider.hasChildren(node))
			{
				onNotFoundAction(target, action, key, node);
			}
			else
			{
				selected = key;
				onSelect(target, node);
			}
		}
		else
		{
			IItemAction<T> handler = actions.get(action);
			if (handler == null)
			{
				onNotFoundAction(target, action, key, node);
			}
			else
			{
				handler.onAction(node, target);
			}
		}
	}

	private class DataBehavior extends AbstractAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void onRequest()
		{
			String parentKey = getComponent().getRequest().getRequestParameters()
				.getParameterValue(PARENT_PARAMETER).toOptionalString();
			Iterator<? extends T> nodes;
			if (parentKey == null)
			{
				nodes = provider.getRoots();
			}
			else
			{
				T parent = findNode(parentKey);
				nodes = parent != null ? provider.getChildren(parent) : List.<T> of().iterator();
			}
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("nodes", toNodes(nodes));
			getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(
				new TextRequestHandler("application/json", "UTF-8", jsonSerializer.toJson(data)));
		}
	}

	private class ActionBehavior extends AbstractDefaultAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			attributes.setMethod(Method.POST);
		}

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			IRequestParameters parameters = getComponent().getRequest().getRequestParameters();
			String action = parameters.getParameterValue(ACTION_PARAMETER).toOptionalString();
			if (action == null)
			{
				return;
			}
			K key = provider.parseKey(parameters.getParameterValue(KEY_PARAMETER)
				.toOptionalString());
			boolean toggle = EXPAND_ACTION.equals(action) || COLLAPSE_ACTION.equals(action);
			T node = key != null && !toggle ? provider.findByKey(key) : null;
			onAction(target, action, key, node);
		}
	}
}
