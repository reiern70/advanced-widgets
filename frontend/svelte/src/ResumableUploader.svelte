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
  @component
  The resumable uploader of the core: files sent in chunks to a tus server, each paused, resumed or
  canceled by the user, going on after a broken connection or a reload without sending again what
  the server holds. Svelte renders the element, the core draws the picker and a row of each file
  into it, in the look of the context. A new endpoint makes a new uploader; other props change the
  one there is, the files being sent going on as they started. bind:this gets the component, whose
  uploader() gives the core's.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { ResumableUploader, type ResumableItem, type ResumableQuestion,
		type ResumableUploaderOptions, type TusError } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = Omit<ResumableUploaderOptions, 'look' | 'onComplete' | 'onError' | 'onChange' |
		'onQuestion' | 'metadata'> & {
		/** What the server is told of a file besides its name and type. */
		metadata?: (file: File) => Record<string, string>;
		/** Called once the server holds the whole of a file: its address tells the server which. */
		oncomplete?: (item: ResumableItem) => void;
		/** Called once a file failed for good. */
		onerror?: (item: ResumableItem, error: TusError) => void;
		/** Called when a file is added, taken out or changes state, with all files. */
		onchange?: (items: ResumableItem[]) => void;
		/**
		 * Asks the user whether to go on with uploads, in a dialog of the page's own: returns true
		 * when it did, and answers by resume() or cancel(); else the uploader asks above its list.
		 */
		onquestion?: (question: ResumableQuestion) => boolean | void;
		id?: string;
		/** CSS classes of the element the uploader draws into. */
		class?: string;
	};

	let { id, class: className, metadata, oncomplete, onerror, onchange, onquestion,
		...settings }: Props = $props();

	let element = $state<HTMLDivElement>();
	let instance: ResumableUploader | null = null;

	const options = (): ResumableUploaderOptions => ({ ...settings, look,
		metadata: metadata && ((file: File) => metadata?.(file) ?? {}),
		onComplete: (item) => oncomplete?.(item),
		onError: (item, error) => onerror?.(item, error),
		onChange: (items) => onchange?.(items),
		onQuestion: (question) => onquestion?.(question) });

	// a value: the uploader is made again only when the endpoint changes, not with other props
	const endpoint = $derived(settings.endpoint);

	$effect(() => {
		if (!element || !endpoint) {
			return;
		}
		const uploader = new ResumableUploader(element, untrack(options));
		instance = uploader;
		return () => {
			uploader.destroy();
			instance = null;
		};
	});

	let configured = '';
	$effect(() => {
		const key = JSON.stringify(settings);
		untrack(() => {
			if (instance && configured && configured !== key) {
				instance.configure(options());
			}
			configured = key;
		});
	});

	/** @returns the uploader of the core, null before it is mounted */
	export function uploader(): ResumableUploader | null {
		return instance;
	}

	/** Adds files, as if chosen in the picker. */
	export function add(files: File[]): void {
		instance?.add(files);
	}

	/** Pauses every file being sent or waiting for its turn. */
	export function pauseAll(): void {
		instance?.pauseAll();
	}

	/** Starts or resumes every file paused, failed or waiting for its start button. */
	export function resumeAll(): void {
		instance?.resumeAll();
	}
</script>

<div bind:this={element} {id} class={className}></div>
