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

import { useState, type FormEvent } from 'react';

import { Button, Dropdown, Modal } from '@antilia/react';

import { format, labelOf, type DropdownTexts } from '../data.js';
import type { UnitsConfig } from '../dropdown.js';
import { Icon } from './DialogOptionsDemo.js';

/** The id of the form of the dialog, which the Choose button of the footer submits. */
const FORM_ID = 'react-unit-form';

/**
 * The unit of measure of the drop-down page in React, chosen in a Modal as the Wicket page does:
 * the drop-down of the long list of units, long options in groups, is the last thing in the
 * body of the dialog, so its open list reaches over the footer, Cancel and Choose.
 */
export function UnitDialogDemo({ config, dropdown }: { config: UnitsConfig;
	dropdown: DropdownTexts }) {
	const { texts, icons } = config;
	const [unit, setUnit] = useState(config.value);
	// the unit of the drop-down of the dialog, taken when the dialog is sent
	const [chosen, setChosen] = useState<string | null>(null);

	const choose = (event: FormEvent) => {
		event.preventDefault();
		if (chosen) {
			setUnit(chosen);
		}
		setChosen(null);
	};

	return (
		<>
			<section className="dropdown-demo__dialog">
				<h3>{texts['units.title']}</h3>
				<p className="dropdown-demo__unit">
					{format(texts['units.chosen'], labelOf(config.options, unit))}</p>
				<Button label={texts['units.choose']} icon={<Icon markup={icons.choose} />}
					onClick={() => setChosen(unit)} />
			</section>
			<Modal open={chosen !== null} onClose={() => setChosen(null)} size="medium"
				closeLabel={texts['units.close']} title={texts['units.dialog']}
				footer={<>
					<Button size="small" label={texts['units.cancel']}
						icon={<Icon markup={icons.cancel} />} data-modal-close />
					<Button type="submit" form={FORM_ID} size="small" variant="primary"
						label={texts['units.ok']} icon={<Icon markup={icons.ok} />} />
				</>}>
				<form id={FORM_ID} className="dropdown-demo__unit-form" onSubmit={choose}>
					<p>{texts['units.text']}</p>
					<label>
						{texts['units.unit']}{' '}
						<Dropdown name="unit" options={config.options} value={chosen ?? unit}
							onChange={setChosen} {...dropdown} />
					</label>
				</form>
			</Modal>
		</>
	);
}
