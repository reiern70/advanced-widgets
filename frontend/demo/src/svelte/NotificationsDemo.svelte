<!--
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!--
  The Notifications demo in Svelte: the forms of the Wicket page, showing their notifications with
  the Notifications bound to the demo.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Button, Dropdown, Notifications, type NotificationPosition } from '@antilia/svelte';
	import { NOTIFICATION_POSITIONS } from '@antilia/core';

	import { dropdownTexts } from '../data.js';
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
				<Dropdown name="type" value={type} options={DEMO_TYPES.map((value) =>
					({ value, label: texts.types[value] }))}
					onchange={(value) => type = value as DemoType} {...dropdownTexts(texts)} />
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
				<Dropdown name="position" value={position} options={NOTIFICATION_POSITIONS.map((value) =>
					({ value, label: texts.positions[value] }))}
					onchange={(value) => position = value as NotificationPosition}
					{...dropdownTexts(texts)} />
			</label>
			<label>{texts.timeout}
				<Dropdown name="timeout" value={String(timeout)} options={DEMO_TIMEOUTS.map((value) =>
					({ value: String(value), label: texts.timeouts[value] }))}
					onchange={(value) => timeout = Number(value)} {...dropdownTexts(texts)} />
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
