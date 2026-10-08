/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useMemo, useState } from 'react';

import { Tree, TreeGrid } from '@advanced-widgets/react';

import { loadFiles, type FileData, type TreeDemoConfig,
	type TreeGridDemoConfig } from '../tree.js';
import { FileDetails } from './FileDetails.js';

/**
 * The tree demo in React: the files of advanced-widgets in a Tree, loaded from the files resource
 * folder by folder; the file selected is shown next to it.
 */
export function TreeDemo({ config }: { config: TreeDemoConfig }) {
	const load = useMemo(() => loadFiles(config.filesUrl), [config.filesUrl]);
	const [file, setFile] = useState<FileData | null>(null);
	return (
		<div className="tree-demo tree-demo--single">
			<div className="tree-demo__box">
				<Tree load={load} template={config.template} label={config.texts.files}
					expanded={config.expanded} texts={config.texts.tree}
					onSelect={(_key, data) => setFile(data as FileData)} />
			</div>
			<aside className="tree-demo__details">
				<FileDetails file={file} texts={config.texts} />
			</aside>
		</div>
	);
}

/**
 * The tree grid demo in React: the files of advanced-widgets in a TreeGrid with the columns of
 * the Wicket one; the file selected is shown next to it.
 */
export function TreeGridDemo({ config }: { config: TreeGridDemoConfig }) {
	const load = useMemo(() => loadFiles(config.filesUrl), [config.filesUrl]);
	const [file, setFile] = useState<FileData | null>(null);
	return (
		<div className="tree-demo tree-demo--single">
			<div className="tree-demo__box tree-demo__box--table">
				<TreeGrid load={load} template={config.template} label={config.texts.files}
					header={config.header} columns={config.columns} expanded={config.expanded}
					texts={config.texts.tree}
					onSelect={(_key, data) => setFile(data as FileData)} />
			</div>
			<aside className="tree-demo__details">
				<FileDetails file={file} texts={config.texts} />
			</aside>
		</div>
	);
}
