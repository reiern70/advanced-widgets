<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- A CodeEditor whose text, state and changes not saved are bound, shown next to it. -->
<script lang="ts">
	import { CodeEditor, type CodeEditorState } from '../src/index.js';

	let { value = $bindable('let a = 1;'), state: editorState = $bindable(), saved, name, language,
		lineNumbers, readOnly, className = 'code', onsave, onfocus, onblur, onresize,
		onselectionchange }: {
		value?: string; state?: CodeEditorState; saved?: string; name?: string; language?: string;
		lineNumbers?: boolean; readOnly?: boolean; className?: string;
		onsave?: (value: string) => void; onfocus?: () => void; onblur?: (value: string) => void;
		onresize?: (size: unknown) => void; onselectionchange?: (selection: unknown) => void }
		= $props();
	let modified = $state(false);
</script>

<form>
	<CodeEditor id="e" class={className} bind:value bind:state={editorState} bind:modified {saved}
		{name} {language} {lineNumbers} {readOnly} stateDelay={0} minHeight={100} label="Script"
		{onsave} {onfocus} {onblur} {onresize} {onselectionchange} />
</form>
<output>{value}</output>
<span class="modified">{modified}</span>
<span class="head">{editorState?.selection?.ranges[0]?.head ?? ''}</span>
<button type="button" class="reset" onclick={() => (value = 'const b = 2;')}>reset</button>
<input id="other" />
