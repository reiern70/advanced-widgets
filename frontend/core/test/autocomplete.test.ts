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

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ACTION_EVENT, ANTILIA_LOOK, Autocomplete, CHOOSE_EVENT, markMatches, registerHandlebars,
	Scrollbar, submitForm, Templates, type AutocompleteAction, type AutocompleteSuggestion } from
	'../src/index.js';

const FRUITS: AutocompleteSuggestion[] = ['Apple', 'Apricot', 'Banana'].map((label) => ({
	id: label.toLowerCase(), label, data: { name: label }
}));

function search(term: string): Promise<AutocompleteSuggestion[]> {
	return Promise.resolve(FRUITS.filter((fruit) => fruit.label.toLowerCase()
		.startsWith(term.toLowerCase())));
}

function input(): HTMLInputElement {
	document.body.innerHTML = '<label for="f">Fruit</label><input type="hidden" id="f">';
	return document.getElementById('f') as HTMLInputElement;
}

async function type(instance: Autocomplete, text: string): Promise<void> {
	instance.text.value = text;
	instance.text.dispatchEvent(new Event('input'));
	await vi.advanceTimersByTimeAsync(250);
}

function key(target: Element, name: string): void {
	target.dispatchEvent(new KeyboardEvent('keydown', { key: name, bubbles: true }));
}

function chips(instance: Autocomplete): string[] {
	return Array.from(instance.chips.querySelectorAll('.antilia-chip-text'),
		(chip) => chip.textContent ?? '');
}

beforeEach(() => {
	vi.useFakeTimers();
});

afterEach(() => {
	vi.useRealTimers();
	document.body.innerHTML = '';
});

describe('markMatches', () => {
	const mark = (text: string) => '<mark class="antilia-autocomplete-match">' + text + '</mark>';

	it('marks the words typed, regardless of case and accents', () => {
		expect(markMatches('Published', 'pub')).toBe(mark('Pub') + 'lished');
		expect(markMatches('José Pérez', 'jose PE')).toBe(mark('José') + ' ' + mark('Pé') + 'rez');
		expect(markMatches('Jose\u0301', 'jose')).toBe(mark('Jose\u0301'));
		expect(markMatches('Ærø', 'ærø')).toBe(mark('Ærø'));
	});

	it('marks every place a word occurs, joining those that touch', () => {
		expect(markMatches('Banana', 'an')).toBe('B' + mark('anan') + 'a');
		expect(markMatches('Gran Canaria', 'can gran')).toBe(mark('Gran') + ' ' + mark('Can') +
			'aria');
	});

	it('escapes the label, the parts marked too', () => {
		expect(markMatches('<b>Tom & Jerry</b>', 'tom &')).toBe('&lt;b&gt;' + mark('Tom') + ' ' +
			mark('&amp;') + ' Jerry&lt;/b&gt;');
		expect(markMatches('<b>', 'b')).toBe('&lt;' + mark('b') + '&gt;');
	});

	it('marks nothing without a term or a match', () => {
		expect(markMatches('A & B', '')).toBe('A &amp; B');
		expect(markMatches('A & B', '  ')).toBe('A &amp; B');
		expect(markMatches('Apple', 'x')).toBe('Apple');
	});
});

describe('Autocomplete', () => {
	it('marks the letters typed in {{@labelMatch}}, the row unless a template is given',
		async () => {
			const instance = new Autocomplete(input(), { search, highlightMatch: true });

			await type(instance, 'AP');

			expect(Array.from(instance.list.children, (item) => item.innerHTML))
				.toEqual(['<mark class="antilia-autocomplete-match">Ap</mark>ple', '<mark class="antilia-autocomplete-match">Ap</mark>ricot']);

			instance.configure({ search, highlightMatch: true,
				template: '<i>{{@labelMatch}}</i> {{{@labelMatch}}} {{@label}}' });
			await type(instance, 'ba');
			expect(instance.list.children[0].innerHTML).toBe('<i><mark class="antilia-autocomplete-match">Ba</mark>nana</i> ' +
				'<mark class="antilia-autocomplete-match">Ba</mark>nana Banana');
		});

	it('writes {{@labelMatch}} as the label escaped, without highlightMatch', async () => {
		const instance = new Autocomplete(input(), {
			search: () => Promise.resolve([{ id: 1, label: 'Tom & <Jerry>' }]),
			template: '<i>{{@labelMatch}}</i>'
		});

		await type(instance, 'tom');

		expect(instance.list.children[0].innerHTML).toBe('<i>Tom &amp; &lt;Jerry&gt;</i>');
		expect(instance.list.children[0].querySelector('mark')).toBeNull();
	});

	it('writes {{@labelMatch}} as markup with Handlebars too', async () => {
		registerHandlebars({
			compile: (template) => (_data, options) => template.replace('{{@labelMatch}}',
				// Handlebars' escapeExpression writes what has a toHTML() as it is
				String(((options?.data as Record<string, { toHTML(): string }>).labelMatch)
					.toHTML()))
		}, Templates, 'fake-handlebars');
		const instance = new Autocomplete(input(), { search, highlightMatch: true,
			engine: 'fake-handlebars', template: '<i>{{@labelMatch}}</i>' });

		await type(instance, 'apr');

		expect(instance.list.children[0].innerHTML).toBe('<i><mark class="antilia-autocomplete-match">Apr</mark>icot</i>');
		delete Templates.engines['fake-handlebars'];
	});

	it('lists the suggestions of search, rendered with the template', async () => {
		const instance = new Autocomplete(input(), { search, template: '<b>{{name}}</b> {{@id}}' });

		await type(instance, 'ap');

		expect(instance.isOpen()).toBe(true);
		expect(Array.from(instance.list.children, (item) => item.innerHTML))
			.toEqual(['<b>Apple</b> apple', '<b>Apricot</b> apricot']);
		expect(instance.text.getAttribute('aria-label')).toBe('Fruit');
	});

	it('chooses with the keyboard, writes the ids and fires change', async () => {
		const element = input();
		const instance = new Autocomplete(element, { search, multiple: true });
		const changes: string[] = [];
		element.addEventListener('change', () => changes.push(element.value));

		await type(instance, 'ap');
		key(instance.text, 'ArrowDown');
		key(instance.text, 'Enter');
		await type(instance, 'b');
		key(instance.text, 'Enter');

		expect(chips(instance)).toEqual(['Apricot', 'Banana']);
		expect(changes).toEqual(['["apricot"]', '["apricot","banana"]']);
		expect(instance.isOpen()).toBe(false);
	});

	it('leaves out the suggestions chosen already and says when nothing matches', async () => {
		const instance = new Autocomplete(input(), { search, multiple: true, noMatch: 'Nothing',
			chosen: [{ id: 'apple', label: 'Apple' }] });

		await type(instance, 'ap');
		expect(Array.from(instance.list.children, (item) => item.textContent)).toEqual(['Apricot']);

		await type(instance, 'x');
		expect(instance.status.textContent).toBe('Nothing');
	});

	it('takes one value in place of the text field, until it is removed', () => {
		const element = input();
		const instance = new Autocomplete(element, { chosen: [{ id: 1, label: 'One' }],
			remove: 'Drop {0}' });
		expect(instance.text.hidden).toBe(true);
		expect(element.value).toBe('[1]');

		instance.chips.querySelector<HTMLElement>('.antilia-chip-remove')!.click();

		expect(instance.text.hidden).toBe(false);
		expect(element.value).toBe('');
	});

	it('removes the last value with Backspace in the empty field', () => {
		const instance = new Autocomplete(input(), { multiple: true,
			chosen: [{ id: 1, label: 'One' }, { id: 2, label: 'Two' }] });

		key(instance.text, 'Backspace');

		expect(chips(instance)).toEqual(['One']);
	});

	it('only reports a pick, as a find bar, and keeps no chip', async () => {
		const element = input();
		const instance = new Autocomplete(element, { search, pick: true, multiple: true,
			chosen: [{ id: 'banana', label: 'Banana' }] });
		const picked: unknown[] = [];
		element.addEventListener('change', () => picked.push(element.value));

		expect(chips(instance)).toEqual([]);
		expect(element.value).toBe('');
		await type(instance, 'ap');
		key(instance.text, 'Enter');

		expect(picked).toEqual(['["apple"]']);
		expect(instance.picked).toEqual({ id: 'apple', label: 'Apple' });
		expect(chips(instance)).toEqual([]);
		expect(instance.text.value).toBe('');
		expect(instance.text.hidden).toBe(false);
		expect(instance.wrapper.classList.contains('antilia-autocomplete-pick')).toBe(true);

		// still suggested: the page decides what it leaves out
		await type(instance, 'ap');
		expect(instance.suggestions.map((suggestion) => suggestion.id)).toEqual(['apple', 'apricot']);
		key(instance.text, 'ArrowDown');
		key(instance.text, 'Enter');
		expect(picked).toEqual(['["apple"]', '["apricot"]']);

		// nothing to remove, and nothing to show
		key(instance.text, 'Backspace');
		instance.setChosen([{ id: 'x', label: 'X' }]);
		expect(picked).toHaveLength(2);
		expect(chips(instance)).toEqual([]);
	});

	it('is a search box with freeText: it holds the text typed, which Up and Down complete',
		async () => {
			const element = input();
			element.value = 'pear';
			const instance = new Autocomplete(element, { search, freeText: true, multiple: true,
				pick: true, placeholder: 'Search' });
			const inputs: string[] = [];
			element.addEventListener('input', () => inputs.push(element.value));

			expect(instance.text.value).toBe('pear');
			expect(instance.multiple).toBe(false);
			expect(instance.pick).toBe(false);
			expect(instance.wrapper.classList.contains('antilia-autocomplete-free-text')).toBe(true);
			expect(instance.text.placeholder).toBe('Search');

			await type(instance, 'ap');
			expect(element.value).toBe('ap');
			expect(inputs).toEqual(['ap']);
			expect(instance.isOpen()).toBe(true);
			// no row active until the keys reach one: Enter sends the text typed
			expect(instance.active).toBeNull();

			const shown = () => [instance.text.value, element.value];
			key(instance.text, 'ArrowDown');
			expect(shown()).toEqual(['Apple', 'Apple']);
			key(instance.text, 'ArrowDown');
			expect(shown()).toEqual(['Apricot', 'Apricot']);
			key(instance.text, 'ArrowUp');
			expect(shown()).toEqual(['Apple', 'Apple']);
			// above the first row, the text typed
			key(instance.text, 'ArrowUp');
			expect(shown()).toEqual(['ap', 'ap']);
			expect(instance.active).toBeNull();
			key(instance.text, 'ArrowDown');
			key(instance.text, 'Escape');
			expect(shown()).toEqual(['ap', 'ap']);
			expect(instance.isOpen()).toBe(false);

			// the rows the keys reach ask for no suggestions, and are no input of the user
			expect(inputs).toEqual(['ap']);
			expect(chips(instance)).toEqual([]);
		});

	it('puts a suggestion chosen into the search box, firing change and the choose event',
		async () => {
			const element = input();
			const onChoose = vi.fn();
			const instance = new Autocomplete(element, { search, freeText: true, onChoose });
			const events: string[] = [];
			element.addEventListener('change', () => events.push('change ' + element.value));
			element.addEventListener(CHOOSE_EVENT, (event) => events.push('choose ' +
				JSON.stringify((event as CustomEvent).detail)));

			await type(instance, 'ap');
			key(instance.text, 'ArrowDown');
			key(instance.text, 'ArrowDown');
			key(instance.text, 'Enter');

			expect([instance.text.value, element.value, instance.typed])
				.toEqual(['Apricot', 'Apricot', 'Apricot']);
			expect(events).toEqual(['change Apricot', 'choose {"id":"apricot","label":"Apricot"}']);
			expect(onChoose).toHaveBeenCalledWith(FRUITS[1]);
			expect(instance.isOpen()).toBe(false);

			await type(instance, 'b');
			(instance.list.children[0] as HTMLElement).click();
			expect(element.value).toBe('Banana');
			expect(events.slice(2)).toEqual(['change Banana',
				'choose {"id":"banana","label":"Banana"}']);
		});

	it('leaves Enter to the browser in a search box, unless the keys reached a row', async () => {
		const enter = (target: Element) => {
			const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true,
				cancelable: true });
			target.dispatchEvent(event);
			return event.defaultPrevented;
		};
		const instance = new Autocomplete(input(), { search, freeText: true });

		await type(instance, 'ap');
		// the row under the mouse is active, but not the one Enter chooses
		instance.list.children[1].dispatchEvent(new MouseEvent('mousemove', { bubbles: true }));
		expect(instance.active).toBe(instance.list.children[1]);
		expect(enter(instance.text)).toBe(false);
		expect(instance.text.value).toBe('ap');
		expect(instance.isOpen()).toBe(false);

		await type(instance, 'ap');
		key(instance.text, 'ArrowDown');
		expect(enter(instance.text)).toBe(true);
		expect(instance.text.value).toBe('Apple');

		// the other autocompletes never send the form by Enter
		document.body.innerHTML = '';
		const other = new Autocomplete(input(), { search });
		expect(enter(other.text)).toBe(true);
		await type(other, 'zz');
		expect(enter(other.text)).toBe(true);
	});

	it('tells a search box\'s text the user leaves changed, and shows a text passed down',
		async () => {
			const element = input();
			const instance = new Autocomplete(element, { search, freeText: true });
			const changes: string[] = [];
			element.addEventListener('change', () => changes.push(element.value));

			await type(instance, 'kiwi');
			instance.text.dispatchEvent(new Event('change'));
			expect(changes).toEqual(['kiwi']);
			// told once: leaving the field after a choice tells nothing new
			await type(instance, 'ba');
			key(instance.text, 'ArrowDown');
			key(instance.text, 'Enter');
			instance.text.dispatchEvent(new Event('change'));
			expect(changes).toEqual(['kiwi', 'Banana']);

			instance.setText('lime');
			instance.setChosen([{ id: 'x', label: 'X' }]);
			expect([instance.text.value, element.value, instance.typed])
				.toEqual(['lime', 'lime', 'lime']);
			expect(chips(instance)).toEqual([]);
			expect(changes).toEqual(['kiwi', 'Banana']);

			// the others have no text of their own to show
			document.body.innerHTML = '';
			const other = new Autocomplete(input(), { chosen: [{ id: 1, label: 'One' }] });
			other.setText('two');
			expect(other.input.value).toBe('[1]');
		});

	it('sends a form as Enter does: by its default button, unless that one is disabled', () => {
		document.body.innerHTML = '<form id="a"><input id="t"><button type="button">-</button>' +
			'<button id="go">Go</button><input type="submit" value="Later"></form>' +
			'<form id="b"><input id="u"><input type="submit" disabled><button>Go</button></form>' +
			'<form id="c"><input id="v"></form><input id="w">';
		const clicked: string[] = [];
		document.querySelectorAll('form').forEach((form) => form.addEventListener('submit',
			(event) => {
				event.preventDefault();
				clicked.push(form.id + ' ' + ((event as SubmitEvent).submitter?.id ?? ''));
			}));
		const requestSubmit = vi.spyOn(HTMLFormElement.prototype, 'requestSubmit')
			.mockImplementation(function (this: HTMLFormElement) {
				clicked.push(this.id + ' requestSubmit');
			});

		expect(submitForm(document.getElementById('t')!)).toBe(true);
		expect(submitForm(document.getElementById('u')!)).toBe(false);
		expect(submitForm(document.getElementById('v')!)).toBe(true);
		expect(submitForm(document.getElementById('w')!)).toBe(false);

		expect(clicked).toEqual(['a go', 'c requestSubmit']);
		requestSubmit.mockRestore();
	});

	it('sends the form when a suggestion is chosen, with submitOnChoose', async () => {
		document.body.innerHTML = '<form><input type="hidden" id="f"><button>Go</button></form>';
		const element = document.getElementById('f') as HTMLInputElement;
		const sent: string[] = [];
		document.querySelector('form')!.addEventListener('submit', (event) => {
			event.preventDefault();
			sent.push(element.value);
		});
		Autocomplete.submitOnChoose('f');
		// once only, however often a page asks
		Autocomplete.submitOnChoose(element);
		Autocomplete.submitOnChoose('missing');
		const instance = new Autocomplete(element, { search, freeText: true });

		await type(instance, 'ba');
		key(instance.text, 'ArrowDown');
		key(instance.text, 'Enter');

		expect(sent).toEqual(['Banana']);
	});

	it('asks the url with the term when no search is given', async () => {
		const fetch = vi.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve(FRUITS) });
		vi.stubGlobal('fetch', fetch);
		const instance = new Autocomplete(input(), { url: '/fruits?lang=en' });

		await type(instance, 'b a');

		expect(fetch.mock.calls[0][0]).toBe('/fruits?lang=en&term=b%20a');
		vi.unstubAllGlobals();
	});

	it('shows other chosen values without firing change, and is taken away by destroy', () => {
		const element = input();
		const instance = new Autocomplete(element, { multiple: true });
		const change = vi.fn();
		element.addEventListener('change', change);

		instance.setChosen([{ id: 'x', label: 'X' }]);
		expect(chips(instance)).toEqual(['X']);
		expect(change).not.toHaveBeenCalled();
		expect(Autocomplete.of('f')).toBe(instance);

		instance.destroy();
		expect(element.parentElement).toBe(document.body);
		expect(Autocomplete.of('f')).toBeNull();
	});

	it('scrolls its list of suggestions with the slim scroll bars', () => {
		const instance = new Autocomplete(input(), { search });

		expect(instance.list.parentElement?.className).toBe('antilia-scrollbar');
		expect(Scrollbar.of(instance.list)?.options).toEqual({ axis: 'y', look: ANTILIA_LOOK });

		instance.destroy();
		expect(Scrollbar.of(instance.list)).toBeUndefined();
	});

	describe('the actions of the rows', () => {
		const ACTIONS: AutocompleteAction[] = [
			{ name: 'info', template: '<i>i</i>', label: 'About {0}', tooltip: 'All about {{name}}' },
			{ name: 'eat', template: 'Eat {{name}}' }
		];

		it('puts the buttons of the actions at the end of every row', async () => {
			const instance = new Autocomplete(input(), { search, actions: ACTIONS });

			await type(instance, 'ap');

			const item = instance.list.children[0];
			expect(item.querySelector('.antilia-autocomplete-content')?.textContent).toBe('Apple');
			const buttons = Array.from(item.querySelectorAll('button'));
			expect(buttons.map((button) => button.innerHTML)).toEqual(['<i>i</i>', 'Eat Apple']);
			expect(buttons[0].getAttribute('aria-label')).toBe('About Apple');
			expect(buttons[0].tabIndex).toBe(-1);
			expect(buttons[0].classList.contains('antilia-autocomplete-info')).toBe(true);
		});

		it('reports an action without choosing its row', async () => {
			const element = input();
			const onAction = vi.fn();
			const instance = new Autocomplete(element, { search, actions: ACTIONS, onAction });
			const events: unknown[] = [];
			element.addEventListener(ACTION_EVENT, (event) =>
				events.push((event as CustomEvent).detail));
			const change = vi.fn();
			element.addEventListener('change', change);

			await type(instance, 'ap');
			instance.list.children[1].querySelectorAll('button')[1].click();

			expect(onAction).toHaveBeenCalledWith('eat', FRUITS[1]);
			expect(events).toEqual([{ action: 'eat', id: 'apricot', label: 'Apricot' }]);
			expect(change).not.toHaveBeenCalled();
			expect(instance.isOpen()).toBe(true);
		});

		it('toggles the tooltip of the active row with F1, which follows the arrows', async () => {
			const instance = new Autocomplete(input(), { search, actions: ACTIONS });
			await type(instance, 'ap');

			const f1 = new KeyboardEvent('keydown', { key: 'F1', cancelable: true });
			instance.text.dispatchEvent(f1);
			expect(f1.defaultPrevented).toBe(true);
			expect(instance.isTooltipShown()).toBe(true);
			expect(instance.tooltip.innerHTML).toBe('All about Apple');
			expect(instance.list.children[0].getAttribute('aria-describedby'))
				.toBe(instance.tooltip.id);

			key(instance.text, 'ArrowDown');
			expect(instance.tooltip.innerHTML).toBe('All about Apricot');
			expect(instance.list.children[0].hasAttribute('aria-describedby')).toBe(false);

			key(instance.text, 'F1');
			expect(instance.isTooltipShown()).toBe(false);

			// Escape hides the tooltip first, then the list
			key(instance.text, 'F1');
			key(instance.text, 'Escape');
			expect(instance.isTooltipShown()).toBe(false);
			expect(instance.isOpen()).toBe(true);
			key(instance.text, 'Escape');
			expect(instance.isOpen()).toBe(false);
		});

		it('shows the tooltip while its button is hovered, and on a click', async () => {
			const instance = new Autocomplete(input(), { search, actions: ACTIONS });
			const change = vi.fn();
			instance.input.addEventListener('change', change);
			await type(instance, 'ap');
			const info = instance.list.children[1].querySelector('button')!;

			info.dispatchEvent(new MouseEvent('mouseover', { bubbles: true }));
			expect(instance.tooltip.innerHTML).toBe('All about Apricot');
			info.dispatchEvent(new MouseEvent('mouseout', { bubbles: true }));
			expect(instance.isTooltipShown()).toBe(false);

			info.click();
			info.dispatchEvent(new MouseEvent('mouseout', { bubbles: true }));
			expect(instance.isTooltipShown()).toBe(true);
			expect(change).not.toHaveBeenCalled();
			info.click();
			expect(instance.isTooltipShown()).toBe(false);
		});

		it('leaves F1 to the browser when no action has a tooltip', async () => {
			const instance = new Autocomplete(input(), { search, actions: [ACTIONS[1]] });
			await type(instance, 'ap');

			const f1 = new KeyboardEvent('keydown', { key: 'F1', cancelable: true });
			instance.text.dispatchEvent(f1);

			expect(f1.defaultPrevented).toBe(false);
			expect(instance.isTooltipShown()).toBe(false);
		});
	});

	it('escapes the values of the suggestions, in rows, tooltips, labels and chips', async () => {
		const evil = '<img src=x onerror="alert(1)">';
		const element = input();
		const instance = new Autocomplete(element, {
			search: () => Promise.resolve([{ id: evil, label: evil, data: { name: evil } }]),
			template: '{{name}} {{@label}} {{@id}}',
			actions: [{ name: 'info', template: '{{name}}', label: 'About {0}',
				tooltip: '{{name}}' }]
		});

		await type(instance, 'x');
		key(instance.text, 'F1');

		expect(instance.list.querySelector('img')).toBeNull();
		expect(instance.tooltip.querySelector('img')).toBeNull();
		expect(instance.list.querySelector('button')!.getAttribute('aria-label'))
			.toBe('About ' + evil);
		key(instance.text, 'Enter');
		expect(instance.chips.querySelector('img')).toBeNull();
		expect(chips(instance)).toEqual([evil]);
		expect(JSON.parse(element.value)).toEqual([evil]);
	});
});
