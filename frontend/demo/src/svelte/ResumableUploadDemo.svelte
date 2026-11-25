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
  The Resumable upload demo in Svelte: the uploader of the core sending to the tus resource of the
  page, two files at once, and the files the server received whole, as it tells them.
-->
<script lang="ts">
	import { ResumableUploader, type ResumableItem } from '@antilia/svelte';

	import { receive, RECEIVED_MAX, sizeOf, type Receipt,
		type ResumableUploadConfig } from '../resumable-upload.js';

	let { config }: { config: ResumableUploadConfig } = $props();

	const texts = $derived(config.texts);
	let received = $state<Receipt[]>([]);
	let failed = $state<string | null>(null);

	async function completed(item: ResumableItem) {
		const receipt = item.storedName ? await receive(config, item.storedName, item.name) : null;
		if (receipt) {
			failed = null;
			received = [receipt, ...received].slice(0, RECEIVED_MAX);
		} else {
			failed = `${texts['received.failed']}: ${item.name}`;
		}
	}
</script>

<div class="resumable-demo">
	<ResumableUploader class="resumable-demo__uploads" endpoint={config.endpoint}
		query={config.query} parallel={2} pollMillis={1000}
		chunkSize={config.chunkSize} fileMaxSize={config.fileMaxSize} locale={config.locale}
		texts={config.uploaderTexts} pickerTexts={config.pickerTexts} oncomplete={completed} />
	<section class="resumable-demo__received">
		<h3>{texts.received}</h3>
		{#if failed}
			<p class="resumable-demo__hint" role="alert">{failed}</p>
		{/if}
		{#if received.length}
			<table class="resumable-demo__table">
				<thead>
					<tr>
						<th>{texts['received.name']}</th>
						<th>{texts['received.size']}</th>
						<th>{texts['received.sha256']}</th>
					</tr>
				</thead>
				<tbody>
					{#each received as receipt, index (receipt.sha256 + index)}
						<tr>
							<td>{receipt.name}</td>
							<td class="resumable-demo__number">{sizeOf(receipt.size, config.locale)}</td>
							<td class="resumable-demo__digest">{receipt.sha256}</td>
						</tr>
					{/each}
				</tbody>
			</table>
		{:else}
			<p class="resumable-demo__hint">{texts['received.none']}</p>
		{/if}
	</section>
</div>
