<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The Popovers demo in Svelte: the popovers of the Wicket page, opened by PopoverButton, whose
  content Svelte renders and moves into them: the facts of the browser as the popover opens, and a
  form renaming the project.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { buttonClasses, partClasses, popoverClasses, type ButtonVariant } from '@antilia/core';
	import { getLook, Popover, PopoverButton, Tabs, type PopoverDesign, type PopoverOptions,
		type PopoverPlacement, type PopoverVariant, type TabItem } from '@antilia/svelte';

	import { format } from '../data.js';
	import { facts, nameOf, placementText, sideOf, type FieldConfig,
		type PopoversConfig } from '../popovers.js';

	let { config }: { config: PopoversConfig } = $props();

	const look = getLook();

	const texts = $derived(config.texts);
	const icons = $derived(config.icons);
	// the cases, one at a time
	const cases = $derived<TabItem[]>(['placementsTitle', 'galleryTitle', 'richTitle', 'fieldsTitle',
		'tableTitle'].map((id) => ({ id, title: texts[id as keyof typeof texts],
		modified: false })));
	// the choices and the project start as the page has them, then are the app's
	const initial = untrack(() => config);
	let design = $state<PopoverDesign>(initial.design);
	let variant = $state<PopoverVariant>(initial.variant);
	let position = $state<PopoverPlacement>(initial.position);
	const shown = $derived(sideOf(position));
	let project = $state(initial.project);
	let opened = $state(0);
	let draft = $state('');
	let error = $state<string | null>(null);
	let field: HTMLInputElement | undefined = $state();

	function iconOnly(label: string, variant: ButtonVariant) {
		return { class: buttonClasses({ size: 'small', variant, iconOnly: true, look }), title: label,
			'aria-label': label };
	}

	function helpOf(field: FieldConfig): PopoverOptions {
		return { title: field.label, body: field.help, placement: 'right-center', design: 'inverse',
			closeLabel: texts.close };
	}

	function save(event: SubmitEvent) {
		event.preventDefault();
		if (draft.trim() === '') {
			error = texts.required;
			return;
		}
		project = draft.trim();
		Popover.closeAll();
	}
</script>

{#snippet body(label: string, icon?: string, only = false)}
	{#if icon}<span class="ant-button__icon" aria-hidden="true">{@html icon}</span>{/if}
	{#if !only}<span class="ant-button__label">{label}</span>{/if}
{/snippet}

{#snippet status()}
	<dl class="popovers-demo__facts">
		{#each facts(opened) as [key, value] (key)}<dt>{texts['fact.' + key]}</dt><dd>{value}</dd>{/each}
	</dl>
{/snippet}

{#snippet rename()}
	<form class="popovers-demo__rename" onsubmit={save}>
		<label class="popovers-demo__rename-field">
			{texts.name}
			<input type="text" bind:value={draft} bind:this={field} />
		</label>
		<p class="popovers-demo__error" role="alert" hidden={error === null}>{error}</p>
		<div class="popovers-demo__actions">
			<button type="submit" class={buttonClasses({ look, size: 'small', variant: 'primary' })}>
				<span class="ant-button__label">{texts.save}</span>
			</button>
			<button type="button" class={buttonClasses({ look, size: 'small', variant: 'ghost' })}
				onclick={() => Popover.closeAll()}>
				<span class="ant-button__label">{texts.cancel}</span>
			</button>
		</div>
	</form>
{/snippet}

<div class="popovers-demo">
	<Tabs design="pills" label={texts.cases} tabs={cases}>
		{#snippet panel(tab)}
			{#if tab.id === 'placementsTitle'}
				<section>
					<p class="popovers-demo__text">{texts.placementsText}</p>
					<div class="popovers-demo__controls">
						<label class="popovers-demo__control">
							{texts.design}
							<select bind:value={design}>
								{#each config.designs as each (each)}
									<option value={each}>{nameOf(config, 'design', each)}</option>
								{/each}
							</select>
						</label>
						<label class="popovers-demo__control">
							{texts.variant}
							<select bind:value={variant}>
								{#each config.variants as each (each)}
									<option value={each}>{nameOf(config, 'variant', each)}</option>
								{/each}
							</select>
						</label>
					</div>
					<div class="popovers-demo__compass">
						<div class="popovers-demo__target" aria-hidden="true"></div>
						{#each config.placements as placement (placement)}
							{@const name = nameOf(config, 'placement', placement)}
							<PopoverButton class={buttonClasses({ look, size: 'small',
								variant: placement === 'auto' ? 'primary' : 'secondary',
								className: 'popovers-demo__place popovers-demo__place--' + placement })}
								options={{ title: name, body: placementText(config, placement), placement, design, variant,
									maxWidth: config.placementWidth, closeLabel: texts.close }}>
								{@render body(name, placement === 'auto' ? icons.auto : undefined)}
							</PopoverButton>
						{/each}
					</div>
				</section>
			{:else if tab.id === 'galleryTitle'}
				<section>
					<div class="popovers-demo__controls">
						<label class="popovers-demo__control">
							{texts.position}
							<select bind:value={position}>
								{#each config.positions as each (each)}
									<option value={each}>{nameOf(config, 'placement', each)}</option>
								{/each}
							</select>
						</label>
					</div>
					<div class="popovers-demo__gallery">
						{#each config.designs as each (each)}
							{@const name = nameOf(config, 'design', each)}
							{@const text = texts['design.' + each.toUpperCase() + '.text']}
							<figure class="popovers-demo__example">
								<div class="popovers-demo__stage">
									<div class={popoverClasses(look, each, variant)} data-side={shown.side}
										data-align={shown.align}>
										<div class={partClasses(look, 'popover-arrow')}></div>
										<div class={partClasses(look, 'popover-header')}>
											<div class={partClasses(look, 'popover-title')}>{name}</div>
											<span class={partClasses(look, 'popover-close')} aria-hidden="true">
												<svg viewBox="0 0 24 24" width="12" height="12" focusable="false">
													<path d="M6 6l12 12 M18 6L6 18" fill="none" stroke="currentColor"
														stroke-width="2.5" stroke-linecap="round" />
												</svg>
											</span>
										</div>
										<div class={partClasses(look, 'popover-body')}>{text}</div>
									</div>
								</div>
								<PopoverButton class={buttonClasses({ look, size: 'small' })}
									options={{ title: name, body: text, design: each, variant, placement: position,
										maxWidth: config.galleryWidth, closeLabel: texts.close }}>
									{@render body(texts.try)}
								</PopoverButton>
							</figure>
						{/each}
					</div>
				</section>
			{:else if tab.id === 'richTitle'}
				<section>
					<div class="popovers-demo__rich">
						<PopoverButton class={buttonClasses({ look })} onopen={() => opened++} content={status}
							options={{ title: texts.statusTitle, body: texts.statusText,
								placement: 'bottom-left', maxWidth: '24rem', closeLabel: texts.close }}>
							{@render body(texts['status.app'], icons.status)}
						</PopoverButton>
						<p class="popovers-demo__project">
							{texts.project} <strong>{project}</strong>
							<PopoverButton {...iconOnly(texts.rename, 'ghost')} content={rename}
								onclick={() => {
									// the form as it starts, with the name of the project
									draft = project;
									error = null;
								}}
								onopen={() => field?.focus()}
								options={{ title: texts.renameTitle, placement: 'right-top', design: 'aurora',
									closeLabel: texts.close }}>
								{@render body(texts.rename, icons.rename, true)}
							</PopoverButton>
						</p>
					</div>
				</section>
			{:else if tab.id === 'fieldsTitle'}
				<section>
					<form class="popovers-demo__fields" onsubmit={(event) => event.preventDefault()}>
						{#each config.fields as field (field.key)}
							{@const help = format(texts.help, field.label)}
							<div class="popovers-demo__field">
								<div class="popovers-demo__label">
									<label for={'svelte-' + field.key}>{field.label}</label>
									<PopoverButton {...iconOnly(help, 'ghost')} options={helpOf(field)}>
										{@render body(help, icons.help, true)}
									</PopoverButton>
								</div>
								<input id={'svelte-' + field.key} class="popovers-demo__input"
									type={field.key === 'password' ? 'password' : 'text'} />
							</div>
						{/each}
					</form>
				</section>
			{:else if tab.id === 'tableTitle'}
				<section>
					<div class="popovers-demo__scroll">
						<table class="popovers-demo__table">
							<thead>
								<tr><th scope="col">{texts.country}</th><th scope="col">{texts.language}</th>
									<th scope="col">{texts.currency}</th><th scope="col"></th></tr>
							</thead>
							<tbody>
								{#each config.countries as country (country.code)}
									{@const about = format(texts.aboutCountry, country.name)}
									<tr>
										<td>{country.name}</td><td>{country.language}</td><td>{country.currency}</td>
										<td class="popovers-demo__action">
											<PopoverButton {...iconOnly(about, 'ghost')}
												options={{ title: country.name, body: country.about, placement: 'left-center',
													design: 'callout', closeLabel: texts.close }}>
												{@render body(about, icons.info, true)}
											</PopoverButton>
										</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				</section>
			{/if}
		{/snippet}
	</Tabs>
</div>
