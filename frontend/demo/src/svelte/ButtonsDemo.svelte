<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The Buttons demo in Svelte: the buttons of the Wicket page with the Button of
  @advanced-widgets/svelte.
-->
<script lang="ts">
	import { Button } from '@advanced-widgets/svelte';

	import { BUTTON_SIZES, BUTTON_VARIANTS, clickedText, sizeName,
		type ButtonsConfig } from '../buttons.js';

	let { config }: { config: ButtonsConfig } = $props();

	const texts = $derived(config.texts);
	const variants = $derived(config.variants);
	let clicked = $state<string | null>(null);
</script>

<div class="buttons-demo">
	<p class="buttons-demo__clicked" role="status">{clicked ?? texts['clicked.none']}</p>
	<div class="buttons-demo__scroll">
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
								<div class="aw-button-bar">
									<Button label={sample.text} {variant} {size} onclick={click} />
									<Button label={sample.text} {variant} {size} onclick={click}>
										{#snippet icon()}{@html sample.icon}{/snippet}
									</Button>
									<Button label={sample.text} {variant} {size} iconOnly onclick={click}>
										{#snippet icon()}{@html sample.icon}{/snippet}
									</Button>
								</div>
							</td>
						{/each}
					</tr>
				{/each}
			</tbody>
		</table>
	</div>
	<h3>{texts.disabled}</h3>
	<div class="aw-button-bar">
		{#each BUTTON_VARIANTS as variant (variant)}
			<Button label={variants[variant].text} {variant} disabled>
				{#snippet icon()}{@html variants[variant].icon}{/snippet}
			</Button>
		{/each}
	</div>
	<h3>{texts.asLink}</h3>
	<p>
		<a href="#buttons-as-links" class="aw-button aw-button--ghost aw-button--small">{texts['asLink.text']}</a>
	</p>
</div>
