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
<!-- A CodeEditor whose text, state and changes not saved are bound, shown next to it. -->
<script lang="ts">
	import { CodeEditor, type CodeEditorError, type CodeEditorState } from '../src/index.js';

	let { value = $bindable('let a = 1;'), state: editorState = $bindable(), saved, name, language,
		lineNumbers, readOnly, saveOnBlur = $bindable(), errors = $bindable(), className = 'code',
		onsave, onfocus, onblur, onresize, onselectionchange }: {
		value?: string; state?: CodeEditorState; saved?: string; name?: string; language?: string;
		lineNumbers?: boolean; readOnly?: boolean; saveOnBlur?: boolean; errors?: CodeEditorError[];
		className?: string;
		onsave?: (value: string) => void; onfocus?: () => void; onblur?: (value: string) => void;
		onresize?: (size: unknown) => void; onselectionchange?: (selection: unknown) => void }
		= $props();
	let modified = $state(false);
</script>

<form>
	<CodeEditor id="e" class={className} bind:value bind:state={editorState} bind:modified {saved}
		{name} {language} {lineNumbers} {readOnly} {saveOnBlur} {errors} stateDelay={0}
		minHeight={100} label="Script"
		{onsave} {onfocus} {onblur} {onresize} {onselectionchange} />
</form>
<output>{value}</output>
<span class="modified">{modified}</span>
<span class="head">{editorState?.selection?.ranges[0]?.head ?? ''}</span>
<button type="button" class="reset" onclick={() => (value = 'const b = 2;')}>reset</button>
<button type="button" class="same-errors" onclick={() => (errors = errors?.map((error) =>
	({ ...error })))}>same errors</button>
<button type="button" class="other-errors" onclick={() => (errors = [{ message: 'Not saved' }])}>
	other errors</button>
<button type="button" class="no-errors" onclick={() => (errors = undefined)}>no errors</button>
<button type="button" class="save-on-blur" onclick={() => (saveOnBlur = true)}>save on blur</button>
<input id="other" />
