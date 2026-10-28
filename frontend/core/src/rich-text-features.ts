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

/*
 * The features of the rich text editor a page adds, or replaces, by their names: the Tiptap
 * extensions making each, and the tools of the toolbar acting on it. A script registers a feature
 * once, before or after the editors start; an editor takes the features its options name, and one
 * that names a feature not registered yet is made anew as it registers. The built-in features
 * (RICH_TEXT_FEATURES) are in the editor's script; one registered under a built-in name replaces
 * it, its tools too.
 *
 *   registerRichTextFeature('kbd', {
 *     extensions: ({ kit }) => kit.Mark.create({
 *       name: 'kbd',
 *       parseHTML: () => [{ tag: 'kbd' }],
 *       renderHTML: ({ HTMLAttributes }) => ['kbd', HTMLAttributes, 0]
 *     }),
 *     tools: {
 *       kbd: { label: 'Keyboard key', icon: '<svg ...>', kind: 'toggle',
 *         active: (editor) => editor.isActive('kbd'),
 *         run: (editor) => editor.chain().focus().toggleMark('kbd').run() }
 *     }
 *   });
 *
 * The server keeps only the HTML its cleaning lets through: in Wicket, the feature's
 * IRichTextFeature.allowHtml().
 */

import type { AnyExtension, Editor } from '@tiptap/core';

import type { RichTextEditorKit } from './rich-text-editor.js';
import type { HeadingLevel, RichTextFormat } from './rich-text-schema.js';

/** What the extensions of a feature are made with, for an editor. */
export interface RichTextFeatureContext {
	/** The editor's classes of Tiptap and ProseMirror, so that a script needs none of its own. */
	kit: RichTextEditorKit;
	format: RichTextFormat;
	/** The features of the editor. */
	features: ReadonlySet<string>;
	headingLevels: readonly HeadingLevel[];
	/** The options of the feature, from the editor's featureOptions: in Wicket, putOptions(). */
	options: Readonly<Record<string, unknown>>;
}

/** An item of the menu of a tool of a feature. */
export interface RichTextMenuItem {
	/** Its id, unique in the menu. */
	id: string;
	label: string;
	/** Whether it is the one in force, for a menu of choices. */
	checked?: (editor: Editor) => boolean;
	enabled?: (editor: Editor) => boolean;
	run: (editor: Editor) => void;
}

/** A tool of the toolbar a feature brings. */
export interface RichTextToolDefinition {
	/**
	 * The feature the tool needs: the feature registering it when left out, none for null. It
	 * shows only while the editor has it.
	 */
	feature?: string | null;
	/**
	 * A button turning a format on and off (aria-pressed), one doing something, or one opening a
	 * menu of items; a toggle when left out.
	 */
	kind?: 'toggle' | 'action' | 'menu';
	/** What the toolbar calls it, unless the texts of the editor name it; its name otherwise. */
	label?: string;
	/** Its icon: an SVG of 24 by 24, in currentColor; its label when left out. */
	icon?: string;
	/** The keys doing it too, as Tiptap names them, shown in its title. */
	shortcut?: string;
	active?: (editor: Editor) => boolean;
	enabled?: (editor: Editor) => boolean;
	run?: (editor: Editor) => void;
	/** The items of its menu, for a menu. */
	items?: (editor: Editor) => readonly RichTextMenuItem[];
}

/** A feature: its extensions, and its tools by their names. */
export interface RichTextFeatureDefinition {
	/** The extensions making the feature, or a function making them for an editor. */
	extensions?: AnyExtension | readonly AnyExtension[] |
		((context: RichTextFeatureContext) => AnyExtension | readonly AnyExtension[]);
	/** The tools of the toolbar acting on the feature, by their names. */
	tools?: Readonly<Record<string, RichTextToolDefinition>>;
}

const features = new Map<string, RichTextFeatureDefinition>();

const listeners = new Set<(name: string) => void>();

/**
 * Makes a feature known to the editors, under a name; it replaces one of that name, a built-in
 * one too. Editors named it and missing it are made anew.
 *
 * @param name the name, which the option features and Wicket's IRichTextFeature name
 * @param definition its extensions and its tools
 */
export function registerRichTextFeature(name: string, definition: RichTextFeatureDefinition): void {
	const key = name.trim();
	if (!key) {
		throw new Error('A feature of the rich text editor needs a name');
	}
	features.set(key, Object.freeze({ ...definition }));
	listeners.forEach((listener) => listener(key));
}

/**
 * @returns the names of the features registered
 */
export function richTextFeatures(): string[] {
	return Array.from(features.keys());
}

/**
 * @returns the feature registered under a name, if any
 */
export function richTextFeature(name: string): RichTextFeatureDefinition | undefined {
	return features.get(name);
}

/**
 * Hears of the features registered, for the editors that miss them.
 *
 * @returns what stops hearing
 */
export function onRichTextFeature(listener: (name: string) => void): () => void {
	listeners.add(listener);
	return () => {
		listeners.delete(listener);
	};
}

/**
 * @returns the tool of a feature registered, and the feature it needs; none when no feature
 *          registered has a tool of that name
 */
export function registeredTool(tool: string):
	{ definition: RichTextToolDefinition; feature: string | null } | undefined {
	for (const [name, feature] of features) {
		const definition = feature.tools?.[tool];
		if (definition) {
			return { definition, feature: definition.feature === undefined ? name :
				definition.feature };
		}
	}
	return undefined;
}

/**
 * @returns the extensions of a feature registered, for an editor; none when it fails, which the
 *          log tells
 */
export function featureExtensions(definition: RichTextFeatureDefinition,
	context: RichTextFeatureContext, log: (message: string, error: unknown) => void): AnyExtension[] {
	try {
		const made = typeof definition.extensions === 'function' ?
			definition.extensions(context) : definition.extensions;
		return made === undefined ? [] : Array.isArray(made) ? [...made] : [made as AnyExtension];
	} catch (error) {
		log('A feature of the rich text editor failed', error);
		return [];
	}
}
