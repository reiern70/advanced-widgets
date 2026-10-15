/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, describe, expect, it, vi } from 'vitest';

import { ANTILIA_LOOK, BOOTSTRAP_LOOK, Tabs, tabs, TABS_DISCARD_EVENT,
	TABS_DESIGNS } from '../src/index.js';

function key(target: Element, name: string, init: KeyboardEventInit = {}): KeyboardEvent {
	const event = new KeyboardEvent('keydown', { key: name, bubbles: true, cancelable: true,
		...init });
	target.dispatchEvent(event);
	return event;
}

function tabMarkup(name: string, attributes = '', actions = ''): string {
	return '<div class="antilia-tab" data-tab="' + name + '" ' + attributes + '>' +
		'<button class="antilia-tab-button"><span class="antilia-tab-title">Title ' + name +
		'</span></button>' + actions + '</div>';
}

function panelMarkup(name: string, attributes = ''): string {
	return '<div class="antilia-tabs-panel" data-tab="' + name + '" ' + attributes + '>' +
		'<input id="' + name + '-field"></div>';
}

/**
 * @returns the element of tabs one, two and three, with their panels
 */
function markup(extra: Record<string, string> = {}, actions = ''): HTMLElement {
	document.body.innerHTML = '<div id="t"><div class="antilia-tabs-list">' +
		tabMarkup('one', extra.one ?? '', actions) + tabMarkup('two', extra.two ?? '') +
		tabMarkup('three', extra.three ?? '') + '</div><div class="antilia-tabs-panels">' +
		panelMarkup('one', extra.panelOne ?? '') + panelMarkup('two', extra.panelTwo ?? '') +
		panelMarkup('three') + '</div></div>';
	return document.getElementById('t')!;
}

function tab(name: string, root: ParentNode = document): HTMLElement {
	return root.querySelector('.antilia-tab[data-tab="' + name + '"]')!;
}

function button(name: string): HTMLButtonElement {
	return tab(name).querySelector('.antilia-tab-button')!;
}

function panel(name: string, root: ParentNode = document): HTMLElement {
	return root.querySelector('.antilia-tabs-panel[data-tab="' + name + '"]')!;
}

function close(name: string): HTMLButtonElement {
	return tab(name).querySelector('.antilia-tab-close')!;
}

function field(name: string): HTMLInputElement {
	return document.getElementById(name + '-field') as HTMLInputElement;
}

/** Lets the mutation observers run. */
function settle(): Promise<void> {
	return new Promise((done) => setTimeout(done, 0));
}

afterEach(() => {
	document.querySelectorAll('.antilia-tabs-confirm').forEach((confirm) => confirm.remove());
	document.body.innerHTML = '';
});

describe('Tabs', () => {
	it('makes a tab list of the tabs, showing the panel of the first', () => {
		const element = markup();
		const tabs = new Tabs(element, { label: 'Documents' });

		expect(Tabs.of('t')).toBe(tabs);
		expect(element.className).toBe('antilia-tabs antilia-tabs-horizontal antilia-tabs-tabs');
		const list = element.querySelector('.antilia-tabs-list')!;
		expect(list.getAttribute('role')).toBe('tablist');
		expect(list.getAttribute('aria-orientation')).toBe('horizontal');
		expect(list.getAttribute('aria-label')).toBe('Documents');
		expect(tabs.selected()).toBe('one');
		expect(tabs.names()).toEqual(['one', 'two', 'three']);

		expect(tab('one').getAttribute('role')).toBe('presentation');
		expect(tab('one').hasAttribute('data-selected')).toBe(true);
		expect(button('one').getAttribute('role')).toBe('tab');
		expect(button('one').type).toBe('button');
		expect(button('one').id).toBe('t-tab-one');
		expect(button('one').getAttribute('aria-selected')).toBe('true');
		expect(button('one').tabIndex).toBe(0);
		expect(button('one').getAttribute('aria-controls')).toBe('t-panel-one');
		expect(button('two').getAttribute('aria-selected')).toBe('false');
		expect(button('two').tabIndex).toBe(-1);
		expect(panel('one').getAttribute('role')).toBe('tabpanel');
		expect(panel('one').getAttribute('aria-labelledby')).toBe('t-tab-one');
		expect(panel('one').hidden).toBe(false);
		expect(panel('two').hidden).toBe(true);
		expect(tab('one').querySelector('.antilia-tab-close')).toBeNull();
		expect(element.getAttribute('style')).toBeNull();
	});

	it('starts on the tab the options name, or the one marked data-selected', () => {
		expect(new Tabs(markup({ two: 'data-selected' })).selected()).toBe('two');
		expect(new Tabs(markup({ two: 'data-selected' }), { selected: 'three' }).selected())
			.toBe('three');
		// a closed tab is not selected
		expect(new Tabs(markup({ one: 'data-closed' })).selected()).toBe('two');
		expect(new Tabs(markup({ two: 'data-selected data-closed' }), { selected: 'nothing' })
			.selected()).toBe('one');
	});

	it('lays the tabs down the side when vertical, in the design it is given', () => {
		const element = markup();
		const tabs = new Tabs(element, { orientation: 'vertical', design: 'pills' });

		expect(element.classList.contains('antilia-tabs-vertical')).toBe(true);
		expect(element.classList.contains('antilia-tabs-pills')).toBe(true);
		expect(element.querySelector('.antilia-tabs-list')!.getAttribute('aria-orientation'))
			.toBe('vertical');

		tabs.configure({ orientation: 'horizontal', design: 'underline' });
		expect(element.className).toBe('antilia-tabs antilia-tabs-horizontal antilia-tabs-underline');
		// an unknown design is the default one
		tabs.configure({ design: 'other' as never });
		expect(tabs.design()).toBe('tabs');
		// every design has its class, the one before it taken away
		for (const design of TABS_DESIGNS) {
			tabs.configure({ design });
			expect(element.classList.contains('antilia-tabs-' + design)).toBe(true);
			expect(Array.from(element.classList).filter((name) => /^antilia-tabs-(?!horizontal|vertical)/
				.test(name))).toEqual(['antilia-tabs-' + design]);
		}
		expect(TABS_DESIGNS).toEqual(['tabs', 'pills', 'underline', 'segmented', 'folder', 'lifted',
			'sidebar', 'steps', 'cards']);
	});

	it('selects a tab the user clicks, telling the page', () => {
		const onSelect = vi.fn();
		const tabs = new Tabs(markup(), { onSelect });

		button('two').click();

		expect(tabs.selected()).toBe('two');
		expect(onSelect).toHaveBeenCalledWith('two');
		expect(panel('one').hidden).toBe(true);
		expect(panel('two').hidden).toBe(false);
		expect(button('two').tabIndex).toBe(0);
		expect(button('one').tabIndex).toBe(-1);

		button('two').click();
		expect(onSelect).toHaveBeenCalledTimes(1);

		tabs.select('three');
		expect(tabs.selected()).toBe('three');
		expect(onSelect).toHaveBeenCalledTimes(1);
		tabs.select('nothing');
		expect(tabs.selected()).toBe('three');
	});

	it('goes round the tabs with the arrow keys, Home and End', () => {
		const onSelect = vi.fn();
		const tabs = new Tabs(markup({ two: 'data-closed' }), { onSelect });
		button('one').focus();

		key(button('one'), 'ArrowRight');
		expect(tabs.selected()).toBe('three');
		expect(document.activeElement).toBe(button('three'));
		key(button('three'), 'ArrowRight');
		expect(tabs.selected()).toBe('one');
		key(button('one'), 'ArrowLeft');
		expect(tabs.selected()).toBe('three');
		key(button('three'), 'Home');
		expect(tabs.selected()).toBe('one');
		key(button('one'), 'End');
		expect(tabs.selected()).toBe('three');
		expect(onSelect.mock.calls.map((call) => call[0])).toEqual(['three', 'one', 'three', 'one',
			'three']);

		// the other arrows are for vertical tabs, and modifiers are the page's
		expect(key(button('three'), 'ArrowDown').defaultPrevented).toBe(false);
		expect(key(button('three'), 'ArrowLeft', { ctrlKey: true }).defaultPrevented).toBe(false);
		tabs.configure({ orientation: 'vertical' });
		key(button('three'), 'ArrowDown');
		expect(tabs.selected()).toBe('one');
		key(button('one'), 'ArrowUp');
		expect(tabs.selected()).toBe('three');
	});

	it('closes a closable tab without changes at once, selecting the next', () => {
		const onClose = vi.fn();
		const onSelect = vi.fn();
		const tabs = new Tabs(markup({ one: 'data-closable="true"' }), { onClose, onSelect });
		expect(close('one').title).toBe('Close');
		expect(close('one').getAttribute('aria-label')).toBe('Close Title one');
		expect(close('one').tabIndex).toBe(0);
		expect(tab('two').querySelector('.antilia-tab-close')).toBeNull();
		button('one').focus();

		close('one').click();

		expect(tabs.isClosed('one')).toBe(true);
		expect(tab('one').hidden).toBe(true);
		expect(tab('one').hasAttribute('data-closed')).toBe(true);
		expect(panel('one').hidden).toBe(true);
		expect(tabs.selected()).toBe('two');
		expect(tabs.openNames()).toEqual(['two', 'three']);
		expect(document.activeElement).toBe(button('two'));
		expect(onSelect).toHaveBeenCalledWith('two');
		expect(onClose).toHaveBeenCalledWith('one');
		expect(document.querySelector('.antilia-tabs-confirm')).toBeNull();

		tabs.open('one');
		expect(tabs.isClosed('one')).toBe(false);
		expect(tabs.selected()).toBe('one');
		expect(tab('one').hidden).toBe(false);
	});

	it('selects the previous tab when the last one closes, and none when all have', () => {
		const tabs = new Tabs(markup({ one: 'data-closable="true"',
			three: 'data-closable="true" data-selected' }));

		tabs.close('three');
		expect(tabs.selected()).toBe('two');
		// one that is not selected closes without changing the selection
		tabs.close('one');
		expect(tabs.selected()).toBe('two');
		tabs.close('two');
		expect(tabs.selected()).toBeNull();
		expect(tabs.openNames()).toEqual([]);
	});

	it('closes the tab with the focus on Delete, and with the middle button', () => {
		const tabs = new Tabs(markup({ one: 'data-closable="true"', two: 'data-closable="true"' }));

		key(button('one'), 'Delete');
		expect(tabs.isClosed('one')).toBe(true);

		const down = new MouseEvent('mousedown', { bubbles: true, cancelable: true, button: 1 });
		button('two').dispatchEvent(down);
		expect(down.defaultPrevented).toBe(true);
		button('two').dispatchEvent(new MouseEvent('auxclick', { bubbles: true, cancelable: true,
			button: 1 }));
		expect(tabs.isClosed('two')).toBe(true);

		// a tab that is not closable stays
		key(button('three'), 'Delete');
		button('three').dispatchEvent(new MouseEvent('auxclick', { bubbles: true, button: 1 }));
		expect(tabs.isClosed('three')).toBe(false);
	});

	it('marks a tab whose panel has changes, as the user types', async () => {
		new Tabs(markup());
		expect(tab('one').classList.contains('antilia-tab-modified')).toBe(false);

		field('one').value = 'typed';
		field('one').dispatchEvent(new Event('input', { bubbles: true }));

		const mark = tab('one').querySelector<HTMLElement>('.antilia-tab-mark')!;
		expect(tab('one').classList.contains('antilia-tab-modified')).toBe(true);
		expect(mark.parentElement).toBe(button('one'));
		expect(mark.hidden).toBe(false);
		expect(mark.getAttribute('role')).toBe('img');
		expect(mark.getAttribute('aria-label')).toBe('Changes not saved');

		field('one').value = '';
		field('one').dispatchEvent(new Event('change', { bubbles: true }));
		expect(mark.hidden).toBe(true);
		expect(tab('one').classList.contains('antilia-tab-modified')).toBe(false);

		// and as the page says it with data-modified
		panel('two').setAttribute('data-modified', 'true');
		await settle();
		expect(tab('two').classList.contains('antilia-tab-modified')).toBe(true);
	});

	it('asks before closing a tab with changes, and puts its fields back', async () => {
		const onClose = vi.fn();
		const tabs = new Tabs(markup({ one: 'data-closable="true"' }), { onClose });
		const changed = vi.fn();
		field('one').addEventListener('change', changed);
		const discarded = vi.fn();
		panel('one').addEventListener(TABS_DISCARD_EVENT, discarded);
		field('one').value = 'typed';
		expect(tabs.isModified('one')).toBe(true);
		expect(tabs.isModified('two')).toBe(false);

		close('one').click();

		const confirm = document.querySelector('.antilia-tabs-confirm')!;
		expect(confirm.classList.contains('dialog-theme-default')).toBe(true);
		const dialog = confirm.querySelector('.modal-dialog')!;
		expect(dialog.getAttribute('role')).toBe('alertdialog');
		expect(dialog.querySelector('.modal-dialog-title')?.textContent).toBe('Close Title one?');
		expect(dialog.querySelector('.modal-dialog-body')?.textContent)
			.toBe('The changes in Title one are not saved. Close the tab and lose them?');
		const keep = dialog.querySelector<HTMLButtonElement>('[data-confirm="keep"]')!;
		expect(keep.textContent).toBe('Keep it open');
		expect(tabs.isClosed('one')).toBe(false);

		keep.click();
		await settle();

		expect(document.querySelector('.antilia-tabs-confirm')).toBeNull();
		expect(tabs.isClosed('one')).toBe(false);
		expect(field('one').value).toBe('typed');
		expect(onClose).not.toHaveBeenCalled();

		close('one').click();
		document.querySelector<HTMLButtonElement>('[data-confirm="discard"]')!.click();
		await settle();

		expect(document.querySelector('.antilia-tabs-confirm')).toBeNull();
		expect(tabs.isClosed('one')).toBe(true);
		expect(onClose).toHaveBeenCalledWith('one');
		expect(field('one').value).toBe('');
		expect(changed).toHaveBeenCalledTimes(1);
		expect(discarded).toHaveBeenCalledTimes(1);
		expect((discarded.mock.calls[0][0] as CustomEvent).detail).toEqual({ tab: 'one' });
	});

	it('says the dialog in the texts it is given', async () => {
		const tabs = new Tabs(markup({ one: 'data-closable="true"', panelOne: 'data-modified' }), {
			texts: { closeModifiedTitle: '¿Cerrar {0}?', keep: 'Dejarla abierta', close: 'Cerrar' }
		});
		expect(close('one').title).toBe('Cerrar');

		const closing = tabs.requestClose('one');
		expect(document.querySelector('.modal-dialog-title')?.textContent).toBe('¿Cerrar Title one?');
		expect(document.querySelector('[data-confirm="keep"]')?.textContent).toBe('Dejarla abierta');
		key(document.querySelector('.antilia-tabs-confirm .modal-dialog')!, 'Escape');

		await expect(closing).resolves.toBe(false);
		expect(tabs.isClosed('one')).toBe(false);
	});

	it('asks the page instead, when told so', async () => {
		const confirmClose = vi.fn(() => Promise.resolve(true));
		const tabs = new Tabs(markup({ one: 'data-closable="true"', panelOne: 'data-modified' }),
			{ confirmClose });

		await expect(tabs.requestClose('one')).resolves.toBe(true);

		expect(confirmClose).toHaveBeenCalledWith('one', 'Title one');
		expect(document.querySelector('.antilia-tabs-confirm')).toBeNull();
		expect(tabs.isClosed('one')).toBe(true);
		await expect(tabs.requestClose('one')).resolves.toBe(false);
		await expect(tabs.requestClose('nothing')).resolves.toBe(false);
	});

	it('takes whether a panel has changes from the page', () => {
		const tabs = new Tabs(markup({ panelTwo: 'data-modified="false"' }));
		field('two').value = 'typed';
		expect(tabs.isModified('two')).toBe(false);

		tabs.setModified('one', true);
		expect(panel('one').getAttribute('data-modified')).toBe('true');
		expect(tabs.isModified('one')).toBe(true);
		expect(tab('one').classList.contains('antilia-tab-modified')).toBe(true);
		tabs.setModified('one', false);
		expect(tabs.isModified('one')).toBe(false);
		expect(tab('one').classList.contains('antilia-tab-modified')).toBe(false);
		tabs.setModified('two', null);
		expect(panel('two').hasAttribute('data-modified')).toBe(false);
		expect(tabs.isModified('two')).toBe(true);
		tabs.setModified('nothing', true);
	});

	it('keeps the actions of a tab apart: they do not select it, and follow it with Tab', () => {
		const action = vi.fn();
		const tabs = new Tabs(markup({ two: 'data-selected' },
			'<span class="antilia-tab-actions"><button class="info">i</button></span>'));
		const info = tab('one').querySelector<HTMLButtonElement>('.info')!;
		info.addEventListener('click', action);
		expect(info.tabIndex).toBe(-1);

		info.click();

		expect(action).toHaveBeenCalled();
		expect(tabs.selected()).toBe('two');
		tabs.select('one');
		expect(info.tabIndex).toBe(0);
		// keys on an action are its own
		expect(key(info, 'ArrowRight').defaultPrevented).toBe(false);
	});

	it('finds the tabs the page adds and takes away', async () => {
		const tabs = new Tabs(markup({ three: 'data-selected' }));

		document.querySelector('.antilia-tabs-list')!.insertAdjacentHTML('beforeend',
			tabMarkup('four', 'data-closable="true"'));
		document.querySelector('.antilia-tabs-panels')!.insertAdjacentHTML('beforeend',
			panelMarkup('four'));
		await settle();

		expect(tabs.names()).toEqual(['one', 'two', 'three', 'four']);
		expect(button('four').getAttribute('role')).toBe('tab');
		expect(panel('four').hidden).toBe(true);
		expect(close('four')).not.toBeNull();

		tab('three').remove();
		panel('three').remove();
		await settle();

		expect(tabs.names()).toEqual(['one', 'two', 'four']);
		expect(tabs.selected()).toBe('one');
		expect(panel('one').hidden).toBe(false);
	});

	it('gives a tab rendered again its parts back', async () => {
		const tabs = new Tabs(markup({ one: 'data-closable="true"', panelOne: 'data-modified' }));
		const old = tab('one');
		old.insertAdjacentHTML('afterend', tabMarkup('one', 'data-closable="true"'));
		old.remove();
		await settle();

		expect(tab('one')).not.toBe(old);
		expect(close('one')).not.toBeNull();
		expect(button('one').getAttribute('aria-selected')).toBe('true');
		expect(tab('one').querySelector('.antilia-tab-mark')).not.toBeNull();

		// the page making a tab closable no longer
		tab('one').setAttribute('data-closable', 'false');
		await settle();
		expect(tab('one').querySelector('.antilia-tab-close')).toBeNull();
		expect(tabs.selected()).toBe('one');
	});

	it('leaves tabs in a panel to themselves', () => {
		markup();
		panel('one').innerHTML = '<div id="inner"><div class="antilia-tabs-list">' +
			tabMarkup('a', 'data-closable="true"') + tabMarkup('b') +
			'</div><div class="antilia-tabs-panels">' + panelMarkup('a') + panelMarkup('b') +
			'</div></div>';
		const outer = new Tabs(document.getElementById('t')!);
		const inner = new Tabs(document.getElementById('inner')!);
		const innerTab = (name: string) => tab(name, document.getElementById('inner')!);

		expect(outer.names()).toEqual(['one', 'two', 'three']);
		expect(inner.names()).toEqual(['a', 'b']);
		innerTab('b').querySelector<HTMLElement>('.antilia-tab-button')!.click();
		expect(inner.selected()).toBe('b');
		expect(outer.selected()).toBe('one');
		key(innerTab('b').querySelector('.antilia-tab-button')!, 'End');
		expect(outer.selected()).toBe('one');

		// a change in the inner panels marks the outer tab
		field('a').value = 'typed';
		field('a').dispatchEvent(new Event('input', { bubbles: true }));
		expect(tab('one').classList.contains('antilia-tab-modified')).toBe(true);
		expect(innerTab('a').classList.contains('antilia-tab-modified')).toBe(true);
		expect(outer.isModified('one')).toBe(true);
	});

	it('keeps the changes of tabs in a panel without any to the inner tabs', async () => {
		// as the cases of a demo: their panels have no changes, whatever the panels of tabs in them
		markup({ panelOne: 'data-modified="false"' });
		panel('one').innerHTML = '<div id="inner"><div class="antilia-tabs-list">' +
			tabMarkup('a') + tabMarkup('b') + '</div><div class="antilia-tabs-panels">' +
			panelMarkup('a') + panelMarkup('b') + '</div></div>';
		const outer = new Tabs(document.getElementById('t')!);
		const inner = new Tabs(document.getElementById('inner')!);
		const innerTab = (name: string) => tab(name, document.getElementById('inner')!);

		field('a').value = 'typed';
		field('a').dispatchEvent(new Event('input', { bubbles: true }));
		expect(inner.isModified('a')).toBe(true);
		expect(innerTab('a').classList.contains('antilia-tab-modified')).toBe(true);
		expect(outer.isModified('one')).toBe(false);

		// and an editor in it, saying so with data-modified as the user types
		field('a').value = '';
		panel('b', document.getElementById('inner')!).insertAdjacentHTML('beforeend',
			'<div class="editor" data-modified="false"><textarea id="code">saved</textarea></div>');
		await settle();
		// its text area holds the text, and the text saved as the one it was rendered with
		(document.getElementById('code') as HTMLTextAreaElement).value = 'changed';
		document.querySelector('.editor')!.setAttribute('data-modified', 'true');
		await settle();
		expect(inner.isModified('b')).toBe(true);
		expect(innerTab('b').classList.contains('antilia-tab-modified')).toBe(true);
		expect(outer.isModified('one')).toBe(false);
		expect(tab('one').classList.contains('antilia-tab-modified')).toBe(false);
	});

	it('gives its parts the classes of its look, the look of the page by default', async () => {
		const element = markup({ one: 'data-closable="true"', panelOne: 'data-modified' });
		element.setAttribute('data-antilia-look', 'bootstrap');
		const tabs = new Tabs(element, { design: 'pills' });
		const list = element.querySelector('.antilia-tabs-list')!;

		expect(element.className).toBe('antilia-tabs antilia-tabs-horizontal antilia-tabs-pills');
		expect(list.className).toBe('antilia-tabs-list nav nav-pills');
		expect(close('one').className).toBe('antilia-tab-close btn-close');
		expect(tab('one').classList.contains('antilia-tab-modified')).toBe(true);

		// a design of the components' own is no nav
		tabs.configure({ design: 'folder' });
		expect(element.classList.contains('antilia-tabs-pills')).toBe(false);
		expect(element.classList.contains('antilia-tabs-folder')).toBe(true);
		expect(list.className).toBe('antilia-tabs-list');
		tabs.configure({ design: 'underline' });
		expect(list.className).toBe('antilia-tabs-list nav nav-underline');

		// the dialog asking before a tab with changes closes, in the look
		const closing = tabs.requestClose('one');
		const confirm = document.querySelector('.antilia-tabs-confirm')!;
		expect(confirm.querySelector('.modal-dialog-header')!.classList.contains('modal-header'))
			.toBe(true);
		expect(confirm.querySelector('[data-confirm="discard"]')!.className)
			.toBe('btn btn-danger btn-sm');
		key(confirm.querySelector('.modal-dialog')!, 'Escape');
		await expect(closing).resolves.toBe(false);

		// another look: those of the one before go
		tabs.configure({ look: ANTILIA_LOOK });
		expect(list.className).toBe('antilia-tabs-list');
		expect(close('one').className).toBe('antilia-tab-close');
		tabs.configure({ look: BOOTSTRAP_LOOK });
		tabs.destroy();
		expect(list.className).toBe('antilia-tabs-list');
		expect(element.className).toBe('antilia-tabs');
	});

	it('takes itself away, leaving the panels shown', () => {
		const element = markup();
		const tabs = new Tabs(element, { orientation: 'vertical' });
		tabs.setModified('two', true);
		tab('one').setAttribute('data-closable', 'true');
		tabs.refresh();
		expect(close('one')).not.toBeNull();

		tabs.destroy();

		expect(Tabs.of(element)).toBeNull();
		expect(element.classList.contains('antilia-tabs-vertical')).toBe(false);
		expect(tab('one').querySelector('.antilia-tab-close')).toBeNull();
		expect(tab('two').querySelector('.antilia-tab-mark')).toBeNull();
		expect(panel('two').hidden).toBe(false);
		button('two').click();
		expect(panel('one').hidden).toBe(false);
	});

	it('works as a Svelte action', () => {
		const onSelect = vi.fn();
		const action = tabs(markup(), { onSelect });
		action.update({ selected: 'two' });
		expect(Tabs.of('t')!.selected()).toBe('two');
		expect(onSelect).not.toHaveBeenCalled();
		action.destroy();
		expect(Tabs.of('t')).toBeNull();
		expect(Tabs.attach('missing')).toBeNull();
		expect(Tabs.attach('t')).not.toBeNull();
	});
});
