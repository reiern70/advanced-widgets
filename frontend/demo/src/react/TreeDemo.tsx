/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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

import { useMemo, useState } from 'react';

import { Dropdown, Tree, TreeGrid } from '@antilia/react';

import { iconSetOptions, loadTaxa, resizeMode, resizingOptions, type IconSet, type Resizing,
	type TaxonData, type TreeDemoConfig, type TreeDemoTexts, type TreeGridDemoConfig }
	from '../tree.js';
import { TaxonDetails } from './TaxonDetails.js';

/** The drop-down choosing the icons of the trees, as that of the Wicket page does. */
function IconsChoice({ id, value, texts, onChange }: { id: string; value: IconSet;
	texts: TreeDemoTexts; onChange: (value: IconSet) => void }) {
	return (
		<>
			<label htmlFor={id}>{texts.icons}</label>
			<Dropdown id={id} name="icons" value={value} options={iconSetOptions(texts)}
				onChange={(chosen) => onChange(chosen as IconSet)} filter={texts.filter}
				noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
		</>
	);
}

/**
 * The tree demo in React: the tree of life in a Tree, loaded from the tree of life resource group
 * by group, groups selected as well as species; the cards of what is selected are shown next to
 * it. The drop-down above it chooses its icons.
 */
export function TreeDemo({ config }: { config: TreeDemoConfig }) {
	const load = useMemo(() => loadTaxa(config.dataUrl), [config.dataUrl]);
	const [taxon, setTaxon] = useState<TaxonData | null>(null);
	const [icons, setIcons] = useState<IconSet>(config.icons);
	return (
		<div className="tree-demo tree-demo--single">
			<div className="tree-demo__stack">
				<div className="tree-demo__options">
					<IconsChoice id="react-tree-icons" value={icons} texts={config.texts}
						onChange={setIcons} />
				</div>
				<Tree load={load} template={config.template} label={config.texts.treeOfLife} selectBranches
					expanded={config.expanded} texts={config.texts.tree} height={config.height}
					icons={config.iconSets[icons]} className="tree-demo__box"
					onSelect={(_key, data) => setTaxon(data as TaxonData)} />
			</div>
			<aside className="tree-demo__details">
				<TaxonDetails taxon={taxon} texts={config.texts} />
			</aside>
		</div>
	);
}

/**
 * The tree grid demo in React: the tree of life in a TreeGrid with the columns of the Wicket
 * one, groups selected as well as species; the cards of what is selected are shown next to it. The drop-downs above it choose how its
 * columns are resized and its icons, as those of the Wicket page do.
 */
export function TreeGridDemo({ config }: { config: TreeGridDemoConfig }) {
	const load = useMemo(() => loadTaxa(config.dataUrl), [config.dataUrl]);
	const [taxon, setTaxon] = useState<TaxonData | null>(null);
	const [resizing, setResizing] = useState<Resizing>(config.resizableColumns);
	const [icons, setIcons] = useState<IconSet>(config.icons);
	const texts = config.texts;
	return (
		<div className="tree-demo tree-demo--single">
			<div className="tree-demo__stack">
				<div className="tree-demo__options">
					<label htmlFor="react-tree-grid-resize">{texts.resize}</label>
					<Dropdown id="react-tree-grid-resize" name="resize" value={resizing}
						options={resizingOptions(texts)}
						onChange={(value) => setResizing(value as Resizing)}
						filter={texts.filter} noMatch={texts.noMatch} more={texts.more}
						remove={texts.remove} />
					<IconsChoice id="react-tree-grid-icons" value={icons} texts={texts}
						onChange={setIcons} />
				</div>
				<TreeGrid load={load} template={config.template} label={texts.treeOfLife} selectBranches
					header={config.header} columns={config.columns} expanded={config.expanded}
					texts={texts.tree} height={config.height} icons={config.iconSets[icons]}
					resizableColumns={resizeMode(resizing)}
					className="tree-demo__box tree-demo__box--table"
					onSelect={(_key, data) => setTaxon(data as TaxonData)} />
			</div>
			<aside className="tree-demo__details">
				<TaxonDetails taxon={taxon} texts={config.texts} />
			</aside>
		</div>
	);
}
