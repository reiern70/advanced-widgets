/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useMemo, useState } from 'react';

import { Dropdown, Tree, TreeGrid } from '@antilia/react';

import { iconSetOptions, loadFiles, resizeMode, resizingOptions, type FileData, type IconSet,
	type Resizing, type TreeDemoConfig, type TreeDemoTexts, type TreeGridDemoConfig }
	from '../tree.js';
import { FileDetails } from './FileDetails.js';

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
 * The tree demo in React: the files of antilia-widgets in a Tree, loaded from the files resource
 * folder by folder; the file selected is shown next to it. The drop-down above it chooses its
 * icons.
 */
export function TreeDemo({ config }: { config: TreeDemoConfig }) {
	const load = useMemo(() => loadFiles(config.filesUrl), [config.filesUrl]);
	const [file, setFile] = useState<FileData | null>(null);
	const [icons, setIcons] = useState<IconSet>(config.icons);
	return (
		<div className="tree-demo tree-demo--single">
			<div className="tree-demo__stack">
				<div className="tree-demo__options">
					<IconsChoice id="react-tree-icons" value={icons} texts={config.texts}
						onChange={setIcons} />
				</div>
				<Tree load={load} template={config.template} label={config.texts.files}
					expanded={config.expanded} texts={config.texts.tree} height={config.height}
					icons={config.iconSets[icons]} className="tree-demo__box"
					onSelect={(_key, data) => setFile(data as FileData)} />
			</div>
			<aside className="tree-demo__details">
				<FileDetails file={file} texts={config.texts} />
			</aside>
		</div>
	);
}

/**
 * The tree grid demo in React: the files of antilia-widgets in a TreeGrid with the columns of
 * the Wicket one; the file selected is shown next to it. The drop-downs above it choose how its
 * columns are resized and its icons, as those of the Wicket page do.
 */
export function TreeGridDemo({ config }: { config: TreeGridDemoConfig }) {
	const load = useMemo(() => loadFiles(config.filesUrl), [config.filesUrl]);
	const [file, setFile] = useState<FileData | null>(null);
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
				<TreeGrid load={load} template={config.template} label={texts.files}
					header={config.header} columns={config.columns} expanded={config.expanded}
					texts={texts.tree} height={config.height} icons={config.iconSets[icons]}
					resizableColumns={resizeMode(resizing)}
					className="tree-demo__box tree-demo__box--table"
					onSelect={(_key, data) => setFile(data as FileData)} />
			</div>
			<aside className="tree-demo__details">
				<FileDetails file={file} texts={config.texts} />
			</aside>
		</div>
	);
}
