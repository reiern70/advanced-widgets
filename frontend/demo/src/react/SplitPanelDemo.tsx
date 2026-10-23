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

import { useState } from 'react';

import { Scrollable, SplitPanel, type SplitPane, type SplitSize } from '@antilia/react';

import { describe, type SplitPanelConfig } from '../split-panel.js';

/**
 * The SplitPanel demo in React: the split panels of the Wicket page, in its markup, the sentence
 * below the layout following its dividers and its collapsed panes in the browser. Only the body of
 * a pane scrolls, below its title, with the scroll bars of Scrollable.
 */
export function SplitPanelDemo({ config }: { config: SplitPanelConfig }) {
	const { texts, layout } = config;
	const [files, setFiles] = useState<SplitSize>(layout.files.position);
	const [filesCollapsed, setFilesCollapsed] = useState<SplitPane | null>(
		layout.files.collapsed ?? null);
	const [editor, setEditor] = useState<SplitSize>(layout.editor.position);
	const [editorCollapsed, setEditorCollapsed] = useState<SplitPane | null>(
		layout.editor.collapsed ?? null);
	const [vertical, setVertical] = useState(config.vertical);
	const [resizable, setResizable] = useState(config.resizable);
	const [collapsible, setCollapsible] = useState(config.collapsible);

	return (
		<div className="split-demo">
			<section>
				<h3>{texts.layoutTitle}</h3>
				<div className="split-demo__layout">
					<SplitPanel position={files} min={layout.files.min} max={layout.files.max}
						collapsible collapsed={filesCollapsed} label={texts.divider}
						collapseFirstLabel={texts.collapseLeft} collapseSecondLabel={texts.collapseRight}
						restoreLabel={texts.restore} onResized={setFiles} onCollapse={setFilesCollapsed}>
						<div className="split-demo__pane">
							<div className="split-demo__title">{texts.files}</div>
							<Scrollable className="split-demo__body">
								<ul className="split-demo__files">
									{config.files.map((file) => <li key={file}>{file}</li>)}
								</ul>
							</Scrollable>
						</div>
						<SplitPanel orientation="vertical" position={editor} min={layout.editor.min}
							minSecond={layout.editor.minSecond} collapsible collapsed={editorCollapsed}
							label={texts.divider} collapseFirstLabel={texts.collapseTop}
							collapseSecondLabel={texts.collapseBottom} restoreLabel={texts.restore}
							onResized={setEditor} onCollapse={setEditorCollapsed}>
							<div className="split-demo__pane">
								<div className="split-demo__title">{texts.editor}</div>
								<Scrollable className="split-demo__body">
									<pre className="split-demo__code">{config.code}</pre>
								</Scrollable>
							</div>
							<div className="split-demo__pane">
								<div className="split-demo__title">{texts.console}</div>
								<Scrollable className="split-demo__body">
									<pre className="split-demo__console">{config.console}</pre>
								</Scrollable>
							</div>
						</SplitPanel>
					</SplitPanel>
				</div>
				<p className="split-demo__sizes">{describe(config,
					{ position: files, collapsed: filesCollapsed },
					{ position: editor, collapsed: editorCollapsed })}</p>
			</section>
			<section>
				<h3>{texts.settingsTitle}</h3>
				<div className="split-demo__checks">
					<label className="split-demo__check">
						<input type="checkbox" checked={vertical}
							onChange={(event) => setVertical(event.currentTarget.checked)} />
						{' '}{texts.vertical}
					</label>
					<label className="split-demo__check">
						<input type="checkbox" checked={resizable}
							onChange={(event) => setResizable(event.currentTarget.checked)} />
						{' '}{texts.resizable}
					</label>
					<label className="split-demo__check">
						<input type="checkbox" checked={collapsible}
							onChange={(event) => setCollapsible(event.currentTarget.checked)} />
						{' '}{texts.collapsible}
					</label>
				</div>
				<div className="split-demo__settings">
					<SplitPanel orientation={vertical ? 'vertical' : 'horizontal'}
						resizable={resizable} collapsible={collapsible} label={texts.divider}
						collapseFirstLabel={vertical ? texts.collapseTop : texts.collapseLeft}
						collapseSecondLabel={vertical ? texts.collapseBottom : texts.collapseRight}
						restoreLabel={texts.restore}>
						<div className="split-demo__pane">
							<Scrollable className="split-demo__body">
								<p className="split-demo__text">{texts.first}</p>
							</Scrollable>
						</div>
						<div className="split-demo__pane">
							<Scrollable className="split-demo__body">
								<p className="split-demo__text">{texts.second}</p>
							</Scrollable>
						</div>
					</SplitPanel>
				</div>
			</section>
		</div>
	);
}
