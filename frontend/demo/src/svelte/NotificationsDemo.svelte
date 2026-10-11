<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The Notifications demo in Svelte: the forms of the Wicket page, showing their notifications with
  the Notifications bound to the demo.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Button, Notifications, type NotificationPosition } from '@antilia/svelte';
	import { NOTIFICATION_POSITIONS } from '@antilia/core';

	import { DEMO_TIMEOUTS, DEMO_TYPES, notification, saved, type DemoType,
		type NotificationsConfig } from '../notifications.js';

	let { config }: { config: NotificationsConfig } = $props();

	const texts = $derived(config.texts);
	// the form starts as the page has it, then is the app's
	const initial = untrack(() => config);
	let type = $state<DemoType>(initial.type);
	let title = $state(initial.title);
	let message = $state(initial.message);
	let html = $state(initial.html);
	let position = $state<NotificationPosition>(initial.position);
	let timeout = $state(initial.timeout);
	let name = $state('');
	let notifications = $state<Notifications>();

	function show(event: SubmitEvent) {
		event.preventDefault();
		notifications?.show(notification({ type, title, message, html, position, timeout }));
	}

	function save(event: SubmitEvent) {
		event.preventDefault();
		notifications?.show(saved(texts, name));
	}
</script>

<div class="notifications-demo">
	<section>
		<h3>{texts.showTitle}</h3>
		<form class="notifications-demo__form" onsubmit={show}>
			<label>{texts.type}
				<select name="type" bind:value={type}>
					{#each DEMO_TYPES as value (value)}<option {value}>{texts.types[value]}</option>{/each}
				</select>
			</label>
			<label>{texts.title}
				<input type="text" name="title" bind:value={title} />
			</label>
			<label class="notifications-demo__wide">{texts.message}
				<textarea name="message" rows="2" bind:value={message}></textarea>
			</label>
			<label class="notifications-demo__check">
				<input type="checkbox" name="html" bind:checked={html} />
				{texts.html}
			</label>
			<label>{texts.position}
				<select name="position" bind:value={position}>
					{#each NOTIFICATION_POSITIONS as value (value)}
						<option {value}>{texts.positions[value]}</option>
					{/each}
				</select>
			</label>
			<label>{texts.timeout}
				<select name="timeout" bind:value={timeout}>
					{#each DEMO_TIMEOUTS as value (value)}<option {value}>{texts.timeouts[value]}</option>{/each}
				</select>
			</label>
			<div class="notifications-demo__buttons">
				<Button type="submit" variant="primary" label={texts.show}
					class="notifications-demo__show" />
				<Button label={texts.closeAll} class="notifications-demo__close-all"
					onclick={() => notifications?.closeAll()} />
			</div>
		</form>
	</section>
	<section>
		<h3>{texts.feedbackTitle}</h3>
		<form class="notifications-demo__form" onsubmit={save}>
			<label>{texts.name}
				<input type="text" name="name" bind:value={name} />
			</label>
			<div class="notifications-demo__buttons">
				<Button type="submit" label={texts.save} class="notifications-demo__save" />
			</div>
		</form>
	</section>
	<Notifications bind:this={notifications} closeLabel={texts.close} label={texts.label} />
</div>
