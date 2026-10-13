<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The Buttons demo in Svelte: the buttons of the Wicket page with the Button of
  @antilia/svelte, the drop-down above them choosing their icons.
-->
<script lang="ts">
	import { untrack } from 'svelte';

	import { Button, buttonClasses, Dropdown, getLook, Scrollable } from '@antilia/svelte';

	import { BUTTON_SIZES, BUTTON_VARIANTS, clickedText, iconSetOptions, sizeName,
		type ButtonIconSet, type ButtonsConfig } from '../buttons.js';

	let { config }: { config: ButtonsConfig } = $props();

	const texts = $derived(config.texts);

	// a link drawn as a button, in the look of the page as the buttons
	const look = getLook();
	const variants = $derived(config.variants);
	let clicked = $state<string | null>(null);
	let icons = $state<ButtonIconSet>(untrack(() => config.icons));
</script>

<div class="buttons-demo">
	<div class="buttons-demo__options">
		<label for="svelte-buttons-icons">{texts.icons}</label>
		<Dropdown id="svelte-buttons-icons" name="icons" value={icons}
			options={iconSetOptions(texts)}
			onchange={(value) => (icons = String(value) as ButtonIconSet)}
			filter={texts.filter} noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
	</div>
	<p class="buttons-demo__clicked" role="status">{clicked ?? texts['clicked.none']}</p>
	<Scrollable axis="x" class="buttons-demo__scroll">
		<table class="buttons-demo__matrix">
			<thead>
				<tr>
					<th scope="col">{texts.variant}</th>
					{#each BUTTON_SIZES as size (size)}<th scope="col">{sizeName(config, size)}</th>{/each}
				</tr>
			</thead>
			<tbody>
				{#each BUTTON_VARIANTS as variant (variant)}
					{@const sample = variants[variant]}
					<tr>
						<th scope="row">{sample.name}</th>
						{#each BUTTON_SIZES as size (size)}
							{@const click = () => (clicked = clickedText(config, variant, size))}
							<td>
								<div class="ant-button-bar">
									<Button label={sample.text} {variant} {size} onclick={click} />
									<Button label={sample.text} {variant} {size} onclick={click}>
										{#snippet icon()}{@html sample.icons[icons]}{/snippet}
									</Button>
									<Button label={sample.text} {variant} {size} iconOnly onclick={click}>
										{#snippet icon()}{@html sample.icons[icons]}{/snippet}
									</Button>
								</div>
							</td>
						{/each}
					</tr>
				{/each}
			</tbody>
		</table>
	</Scrollable>
	<h3>{texts.disabled}</h3>
	<div class="ant-button-bar">
		{#each BUTTON_VARIANTS as variant (variant)}
			<Button label={variants[variant].text} {variant} disabled>
				{#snippet icon()}{@html variants[variant].icons[icons]}{/snippet}
			</Button>
		{/each}
	</div>
	<h3>{texts.asLink}</h3>
	<p>
		<a href="#buttons-as-links"
			class={buttonClasses({ variant: 'ghost', size: 'small', look })}>{texts['asLink.text']}</a>
	</p>
</div>
