/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { FACTS, type FileData, type TreeDemoTexts } from '../tree.js';

/**
 * The file selected in a tree of the demos, as the Wicket page shows it, or an invitation to
 * select one.
 */
export function FileDetails({ file, texts }: { file: FileData | null; texts: TreeDemoTexts }) {
	return (
		<section className="tree-demo-file" aria-live="polite">
			<h2 className="tree-demo-file__title">{texts.selectedFile}</h2>
			{file ? (
				<dl className="tree-demo-file__facts">
					{FACTS.map((fact) => [
						<dt key={fact + '-label'}>{texts[fact]}</dt>,
						<dd key={fact} className={fact === 'name' || fact === 'path' ?
							'tree-demo-file__' + fact : undefined}>{file[fact]}</dd>
					])}
				</dl>
			) : <p className="tree-demo-file__none">{texts.noneSelected}</p>}
		</section>
	);
}
