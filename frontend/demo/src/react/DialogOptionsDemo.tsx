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

import { useId, useState } from 'react';

import { Button, Modal } from '@antilia/react';

import { format } from '../data.js';
import { DEFAULT_DIALOG_OPTIONS, DIALOG_FLAGS, DIALOG_SIZES, isValidWidth, modalOptions,
	type DialogOptions } from '../dialog-options.js';
import type { ModalDemoConfig } from '../team.js';

/** An icon the server wrote, an inline SVG. */
export function Icon({ markup }: { markup: string }) {
	return <span style={{ display: 'contents' }} dangerouslySetInnerHTML={{ __html: markup }} />;
}

/**
 * A dialog of the options example: Close, a button opening a dialog nested in it while it may,
 * and OK in its footer.
 */
function NestedDialog({ config, options, level, onClose }: { config: ModalDemoConfig;
	options: DialogOptions; level: number; onClose(): void }) {
	const { texts, icons } = config;
	const [nested, setNested] = useState(false);
	const deepest = level >= config.maxLevel;
	return (
		<Modal open onClose={onClose} {...modalOptions(options)} closeLabel={texts.close}
			title={format(texts['dialog.title'], level)}
			footer={<>
				<Button size="small" label={texts.close} icon={<Icon markup={icons.cancel} />}
					data-modal-close />
				{deepest ? null : (
					<Button size="small" label={texts['dialog.nested']}
						icon={<Icon markup={icons.nested} />} onClick={() => setNested(true)} />
				)}
				<Button size="small" variant="primary" label={texts['dialog.ok']}
					icon={<Icon markup={icons.ok} />} onClick={onClose} />
			</>}>
			<p className="dialog-options__text">
				{deepest ? texts['dialog.deepest'] : texts['dialog.text']}</p>
			{nested ? (
				<NestedDialog config={config} options={options} level={level + 1}
					onClose={() => setNested(false)} />
			) : null}
		</Modal>
	);
}

/**
 * The options example in React: the form setting the options of a dialog, as the Wicket one, and
 * the button opening it.
 */
export function DialogOptionsDemo({ config }: { config: ModalDemoConfig }) {
	const { texts, icons } = config;
	const id = useId();
	const [options, setOptions] = useState<DialogOptions>(DEFAULT_DIALOG_OPTIONS);
	const [open, setOpen] = useState(false);
	const set = <K extends keyof DialogOptions>(key: K, value: DialogOptions[K]) =>
		setOptions((current) => ({ ...current, [key]: value }));
	return (
		<form className="dialog-options" onSubmit={(event) => event.preventDefault()}>
			<h2 className="team-demo__title">{texts.options}</h2>
			<div className="dialog-options__groups">
				<fieldset>
					<legend>{texts['options.behavior']}</legend>
					{DIALOG_FLAGS.map((flag) => (
						<label key={flag}>
							<input type="checkbox" checked={options[flag]}
								onChange={(event) => set(flag, event.target.checked)} />
							{' '}{texts[`option.${flag}`]}
						</label>
					))}
				</fieldset>
				<fieldset>
					<legend>{texts.size}</legend>
					<div className="dialog-options__sizes">
						{DIALOG_SIZES.map((size) => (
							<label key={size}>
								<input type="radio" name={id + '-size'} value={size}
									checked={options.size === size} onChange={() => set('size', size)} />
								{' '}{texts[`size.${size}`]}
							</label>
						))}
					</div>
					<label className="dialog-options__width">
						{texts.width}{' '}
						<input type="text" value={options.width} placeholder="40em" size={8}
							aria-invalid={isValidWidth(options.width) ? undefined : true}
							onChange={(event) => set('width', event.target.value.trim())} />
					</label>
				</fieldset>
			</div>
			<Button variant="primary" label={texts.open} icon={<Icon markup={icons.open} />}
				onClick={() => setOpen(true)} />
			{open ? (
				<NestedDialog config={config} options={options} level={1}
					onClose={() => setOpen(false)} />
			) : null}
		</form>
	);
}
