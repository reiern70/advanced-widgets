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
<!-- A CKEditor whose text, state and changes not saved are bound, shown next to it. -->
<script lang="ts">
	import { CKEditor, type CKEditorBuild, type CKEditorInstance, type CKEditorPlugin,
		type RichTextEditorState } from '../src/index.js';

	let { build, value = $bindable('<p>Hello world</p>'), state: editorState = $bindable(), saved,
		name, readOnly, toolbar, plugins, count, className = 'rich', onready, onerror, onsave,
		onfocus, onblur, onselectionchange }: {
		build?: CKEditorBuild; value?: string; state?: RichTextEditorState; saved?: string;
		name?: string; readOnly?: boolean; toolbar?: string[] | false;
		plugins?: CKEditorPlugin[]; count?: boolean; className?: string;
		onready?: (editor: CKEditorInstance) => void; onerror?: (error: unknown) => void;
		onsave?: (value: string) => void; onfocus?: () => void; onblur?: (value: string) => void;
		onselectionchange?: (selection: unknown) => void } = $props();
	let modified = $state(false);
</script>

<form>
	<CKEditor id="e" class={className} {build} licenseKey="GPL" language="de" bind:value
		bind:state={editorState} bind:modified {saved} {name} {readOnly} {toolbar} {plugins} {count}
		stateDelay={0} label="Notes" {onready} {onerror} {onsave} {onfocus} {onblur}
		{onselectionchange} />
</form>
<output>{value}</output>
<span class="modified">{modified}</span>
<span class="head">{editorState?.selection?.head ?? ''}</span>
<button type="button" class="reset" onclick={() => (value = '<p>Reset</p>')}>reset</button>
