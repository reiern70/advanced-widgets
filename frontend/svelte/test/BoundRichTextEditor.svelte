<!--
  Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)

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
<!-- A RichTextEditor whose text, state and changes not saved are bound, shown next to it. -->
<script lang="ts">
	import { RichTextEditor, type RichTextEditorState, type RichTextFormat,
		type RichTextTool } from '../src/index.js';

	let { value = $bindable('<p>Hello world</p>'), state: editorState = $bindable(), saved, name,
		format, readOnly, toolbar, count, className = 'rich', onsave, onfocus, onblur, onresize,
		onselectionchange }: {
		value?: string; state?: RichTextEditorState; saved?: string; name?: string;
		format?: RichTextFormat; readOnly?: boolean; toolbar?: RichTextTool[][] | false;
		count?: boolean; className?: string; onsave?: (value: string) => void;
		onfocus?: () => void; onblur?: (value: string) => void; onresize?: (size: unknown) => void;
		onselectionchange?: (selection: unknown) => void } = $props();
	let modified = $state(false);
</script>

<form>
	<RichTextEditor id="e" class={className} bind:value bind:state={editorState} bind:modified
		{saved} {name} {format} {readOnly} {toolbar} {count} stateDelay={0} minHeight={100}
		label="Notes" {onsave} {onfocus} {onblur} {onresize} {onselectionchange} />
</form>
<output>{value}</output>
<span class="modified">{modified}</span>
<span class="head">{editorState?.selection?.head ?? ''}</span>
<button type="button" class="reset" onclick={() => (value = '<p>Reset</p>')}>reset</button>
<input id="other" />
