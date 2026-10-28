# frontend

The browser side of the components, written once and offered to three frameworks:

| Package | What it is |
|---|---|
| `core` (`@antilia/core`) | The components in plain TypeScript, without a framework, and their style sheets. |
| `react` (`@antilia/react`) | React 18 and 19 components wrapping the core. |
| `svelte` (`@antilia/svelte`) | Svelte 5 components wrapping the core; the core's functions also work as Svelte actions. |
| `demo` | The React and Svelte pages of the demo application (not published). |

Wicket uses the core too: `core/scripts/build-wicket.mjs` builds each component into an IIFE
script under the name and package the Java classes load it from (`Wicket.Dropdown` in
`com/antiliacode/widgets/dropdown/antilia-dropdown.js`, ...), with the Wicket host set. The Java
side and the pages don't change.

Every script of `ui-components` has moved here: the dynamic data tables (`data-table.ts`, built
as `ant-data-table.js`) and Handlebars as one of their template engines (`handlebars.ts`,
built as `handlebars-adapter.js`), the drop-down and its chips, the palette, the autocomplete, the date
picker (`date-picker.ts`, built as `antilia-date-picker.js`), the infinite list
and the template engines they render with, the clipboard, the floating panel, the scroll bars, the
split panel, the tabs, the menus, the breadcrumbs, the popovers, the notifications, the desktop, the uploads (`upload.ts`, built as
`antilia-upload.js`), the resizable elements, the code editor and the preview of Markdown, the rich text editor, and the movable and resizable columns. The style sheets of the table and the progress bar have moved
too, and the buttons' (`button.css`, built as `antilia-button.css`, which Wicket's `ButtonBehavior`
and `AjaxActionButton` use; `setButtonBusy()` in `button.ts`, built as `antilia-button.js`, shows a
button busy with a spinner while it ignores clicks, for the downloads of Wicket's `AjaxDownloadButton`
and `ButtonDownloadBehavior`), and the looks of Wicket's `TabbedPanel` that `TabsStyleBehavior` gives
(in `tabs.css`, built as `antilia-tabs.css`). React and Svelte have `Dropdown`, `Palette`, `Autocomplete`, `DatePicker`,
`DateTimePicker`, `InfiniteList`, `Scrollable`, `SplitPanel`, `Tabs`, `Menu`, `Breadcrumb`, `Notifications`, `Desktop` with its
`DesktopWindow`, `ProgressBar`, `Button`, `PopoverButton`, `Resizable`, `CodeEditor`, `MarkdownPreview`,
`RichTextEditor`, `DataTable`, `Tree` and `TreeGrid`, and export the core's `Popover`; the core's `dropdown`,
`palette`, `datePicker`, `scrollbar`, `splitPanel`, `tabs`, `menu`, `breadcrumb`, `desktop`, `resizable`, `codeEditor`,
`markdownPreview` and `richTextEditor` also work as Svelte actions, and `Clipboard.install()` and
`FloatingPanel.install()` make the `data-antilia-copy` and floating panel markup work in any page.

A `DataTable` loads its rows with `loadRows`, from a mounted resource for example, or is given
them as `rows`, a local list of JSON it filters, sorts and pages in the browser with the core's
`localRows`. The JSON grid demo does both: its rows come from `/api/users`, a `JsonRowsResource`
that also creates, changes and deletes rows (POST, PUT and DELETE), or from a list the page gave.
A column's `minWidth` (`setColumnMinWidths` in Wicket, `data-min-width` on a header cell) is the
narrowest it gets when resizable columns in `'stretch'` mode are laid out for the width of the
parent: on a phone the table is then wider than its parent, rather than every column being
squeezed to a letter, and its navigation stays in view while the page, or the parent given
`overflow-x: auto`, scrolls it sideways.

The palette (`palette.ts`, built as `antilia-palette.js` for Wicket's `Palette`) shows the
available choices on the left and the chosen ones on the right, with arrows between them; a double
click, Enter or dragging moves a choice too. With `ordered`, the chosen ones keep their own order
and move up and down with buttons, Alt and the arrow keys or by dragging. With `numbered`, the rows
show their numbers; `availableTitle` and `selectedTitle` name the lists. The available list gets
a filter from `filterThreshold` choices on (8), unless `filterable` is false; it filters in the
browser, over the choices the palette has. Long lists draw `pageSize` rows at first (30) and as
many more each time they are scrolled to their end, 0 for all; the filter, the keyboard and "add
all" reach the rows not drawn yet. Over a select, as in
Wicket, it writes the choice into the select's options in that order, which a form then posts; in
React and Svelte it reports it with `onChange`, and posts it in hidden fields when given a `name`.

The split panel (`split-panel.ts`, built as `antilia-split-panel.js` for Wicket's `SplitPanel`)
shows two panes, side by side (`horizontal`) or one above the other (`vertical`), with a divider
between them the user drags, or moves as a WAI-ARIA separator with the arrow keys, Home and End; a
double click takes it back to where it started. Sizes are numbers in percent or strings such as
`'30%'` or `'250px'`. The `position` is the size of the first pane: in percent the panes keep
their shares when the split panel resizes, in pixels the first pane keeps its size. `min` and
`max` limit the first pane and `minSecond` the second, in either unit. With `collapsible`, two
buttons on the divider collapse a pane, giving the other all the space, and restore it; Enter does
so from the keyboard. `onResize` follows the divider, `onResized` reports where it was left and
`onCollapse` the collapsed pane. React and Svelte take `collapsed` too, which Svelte binds. In
Wicket the position and the collapsed pane are the panel's model, a `SplitState`, kept in the page
by default; with `setSendToServer(true)` the browser sends each change via Ajax and the panel
gives its model the new state, so a model of one's own can keep it in the session or with the
user's preferences. The core works on markup with both panes and the divider, which React, Svelte
and Wicket all render, and sets the sizes as CSS custom properties through the CSSOM, so it needs
no inline style. Split panels go in each other's panes. With `stackBelow` (`setStackBelow` in
Wicket), panes side by side go one above the other while the split panel is narrower than that
many pixels, on a phone for example: each is as high as its content, and the divider keeps its
collapse buttons; the split panel then has the class `antilia-split-stacked`.

A resizable element (`resizable.ts`, built as `antilia-resizable.js` for Wicket's
`ResizableBehavior`) is resized by the user by its bottom edge (`axis: 'vertical'`), its right edge
(`horizontal`) or its bottom right corner (`both`, the default), dragged with the mouse, a pen or a
finger. The handle, the element's last child, is a WAI-ARIA separator that takes the focus: the
arrow keys resize the element ten pixels at a time (`step`), fifty with Shift, Home and End take it
to its limits (`minWidth`, `minHeight`, 32 pixels by default, `maxWidth`, `maxHeight`), a double
click takes it back to the size it started with, and Escape, while dragging, back to where the drag
began. Sizes are in pixels, those of the border box; `null` leaves a dimension as the page's style
sheets make it, which is how the element starts unless given a `width` or a `height`. `onResize`
follows the handle and `onResized` reports the size the user left. The sizes are CSS custom
properties set through the CSSOM. React's `Resizable` takes `width` and `height`, Svelte's binds
them. In Wicket the size is the model of the behavior, a `ResizableSize`, kept in the page by default:
with `setSendToServer(true)` the browser sends each size the user gives, which the element takes
again when rendered again, also after its component was taken out of the page and put back; a model
of one's own keeps it in the session or anywhere else.

The code editor (`code-editor.ts`, built as `antilia-code-editor.js` for Wicket's `CodeEditor`) is
CodeMirror 6, in the colors of the theme and the look of the page: the grammar of a `language`
highlights the text, with Lezer's `classHighlighter`, whose classes `code-editor.css` colors from the
theme, the keywords in its primary color, the strings in that of success and so on, lighter in a
dark theme. The editor knows Java, JavaScript, TypeScript, JSON and Markdown (`CODE_EDITOR_LANGUAGES`),
whose fenced code blocks take the grammars of the others; `registerLanguage(name, loader)` plugs in
another, the loader given the CodeMirror classes of the editor (`CODE_EDITOR_KIT`), from which a page
without CodeMirror of its own, a Wicket page, builds one, a `StreamLanguage` for example; a grammar is
loaded the first time an editor shows it, or `language` is a CodeMirror extension of one's own. The
lines show their numbers (`lineNumbers`), blocks fold (`folding`), Ctrl+F searches and replaces,
Ctrl+Z undoes, brackets close and match, Alt and the mouse select a rectangle, Tab indents
(`indentWithTab`: Escape, then Tab, leaves the editor) and Ctrl+S calls `onSave`. `readOnly` lets the
user read and select but not change the text, `selectable: false` shows it as a picture, and the
editor is resizable by a bar along its bottom (`resizable`, `vertical` by default, a `Resizable`).
`extensions` take any other CodeMirror extension, and `texts.phrases` CodeMirror's phrases in the
language of the page. Over a text area, as in Wicket, the editor hides it and keeps the text in it,
which a form posts; the text area fires `input` as the user types and `change` once the editor loses
the focus, and the editor fires the events of `CODE_EDITOR_EVENTS` on it too: the focus, the blur,
the selection, Ctrl+S, a resize and changes not saved. Against the text last saved (`saved`,
`markSaved()`) the editor tells whether the text has changes as `data-modified` on its element,
which the tabs and the desktop show; closing them anyway puts the saved text back. What is selected,
how far the text is scrolled and the size the user gave the editor are its state
(`CodeEditorState`): the editor keeps it while hidden, in a tab not selected, `onStateChange` reports
it a moment after the user changed it, as the editor loses the focus and as the page goes, and the
option `state` gives it back; rendered anew in the same place with the same text, as Wicket does,
the editor goes on as it was. CodeMirror writes its style sheet into a style element, which takes
the nonce of the page's scripts, or `nonce`, under a Content Security Policy. React's `CodeEditor`
takes `value` and `state` and calls back; Svelte's binds `value`, `state` and `modified`. Wicket's
`CodeEditor` is a `TextArea` whose model is the text and whose state model, kept in the page by
default, takes the state the browser sends (`setSendToServer`, on by default), with the text the
user changed as its input; `CodeEditorEventBehavior`s hear of its events, `markSaved(target)` tells
the browser the text was saved, and `CodeEditorLanguage.of(name, script)` plugs in the grammar of a
script of the page's own.

The preview of Markdown (`markdown-preview.ts`, in `antilia-code-editor.js` for Wicket's
`MarkdownPreview`) shows a text as a page does, with GitHub's tables, task lists and strikethrough,
its fenced code blocks highlighted by the grammars of the code editor in the colors of the theme
(`code-editor.css`). It reads the text with the editor's grammar, Lezer's Markdown parser, and builds
the preview from the syntax tree an element at a time, never parsing HTML: HTML in the text shows as
text, and links and pictures take only the page's own addresses, http, https and mailto, and pictures
in `data:` addresses (`safeUrl`), so it shows safely what anyone wrote, under a strict Content
Security Policy too; links open in a new tab, but for those to places in the page. The tables get
the classes of the look, Bootstrap's `table` in the Bootstrap look. `new MarkdownPreview(element,
{ value })` renders a text and `setValue()` another; with `editor`, the preview follows a code editor,
rendering its text again a frame later each time it changes, by the user or the page, which the
editor tells with `CODE_EDITOR_EVENTS.text`; `renderMarkdown(text, element)` renders a text once.
With `scrollWithEditor` too, the preview keeps in step with the editor: as the user moves the
cursor, types or scrolls the editor, the preview scrolls to the part matching the place in the
editor. Each block it renders knows the line of the text it starts at, and the preview matches the
editor's lines to them, between the tops of the blocks around a line (`stepScrollTop`): the part
matching the cursor shows as high as the cursor, and with the cursor out of sight, the part
matching the editor's top or bottom line; nothing scrolls while either is hidden, in a collapsed
pane for example. React's and Svelte's `MarkdownPreview` take `value`, `editor` (a `CodeEditor`'s
id), `scrollWithEditor`, `label` and `placeholder`. Wicket's `MarkdownPreview` follows a
`CodeEditor`, or shows the text of its model, in the browser alone; `setScrollWithEditor(boolean)`
keeps it in step, and `setScrollWithEditor(boolean, target)` tells the browser without rendering it
again.
`SplitPanel.collapse(pane, target)` collapses a pane of a collapsible split panel from the server,
without rendering its panes again: the demo's button hiding the preview beside the editor.

The rich text editor (`rich-text-editor.ts`, built as `antilia-rich-text-editor.js` for Wicket's
`RichTextEditor`) is Tiptap, on ProseMirror, with a toolbar of the components' own
(`rich-text-toolbar.ts`), in the colors of the theme and the look of the page: Tiptap has none.
The text holds headings, bold, italic, underlined, struck and code text, the colors of the text and
of its background, links, bulleted, numbered and task lists, quotes, code blocks, rules, tables,
images and alignment, the features
(`RICH_TEXT_FEATURES`, `rich-text-schema.ts`), all of them unless the page gives fewer
(`features`); a feature left out is not in the schema, so a paste brings its text alone, and its
tools do not show. The toolbar is the page's, in groups (`toolbar`, `DEFAULT_RICH_TEXT_TOOLBAR`;
false for none), a WAI-ARIA toolbar the arrow keys move along; the text style, the colors and the
table open a menu, the link and the image a form in a popover; `bubbleMenu` adds the inline formats over a
selection. The usual keys format (Ctrl+B, Ctrl+I...), Markdown's marks do as the user types,
Ctrl+K edits the link, Ctrl+S calls `onSave`, and Escape, then Tab, leaves the editor even in a
list. The text is HTML by default, or Markdown (`format: 'markdown'`, Tiptap's Markdown extension;
underlined text, colors and alignment are then left out, Markdown has no syntax for them) or
Tiptap's JSON. The HTML has no style attribute: text aligns by classes (`RICH_TEXT_ALIGN_CLASSES`),
and takes the colors of a palette (`RICH_TEXT_COLORS`) by classes too, `<span
class="antilia-color-red">` and `<mark class="antilia-background-yellow">`, which
`rich-text-editor.css` mixes with the colors of the theme so that they read in light and dark; tables
have no widths, and Tiptap's own style sheet is in `rich-text-editor.css` instead of a style element,
so the editor needs no `'unsafe-inline'`. Links and images go only to http, https and mailto
addresses, or relative ones (`isSafeUrl`). The editor writes the text its own way, `<b>` as
`<strong>`, and tells changes against the text saved written so too. `placeholder`, `readOnly`,
`limit` and `count` (the characters and the words below the text) do as their names say;
`extensions` take Tiptap's extensions or the names of those `registerRichTextExtension(name,
loader)` registered, the loader given the classes of Tiptap of the editor (`RICH_TEXT_EDITOR_KIT`),
from which a Wicket page builds a `Mark` or a `Node`. As the code editor does, it keeps the text in a
text area a form posts, fires `RICH_TEXT_EDITOR_EVENTS`, tells of changes not saved with
`data-modified`, keeps its state (`RichTextEditorState`: the selection, the scrolling and the size)
while hidden and gives it to `onStateChange`, and goes on as it was when Wicket renders it anew.
The text area Wicket renders has the class `antilia-rich-text-editor-loading`
(`RICH_TEXT_EDITOR_CLASSES.loading`) until the editor is made of it: while scripts run, the style
sheet hides it in the room of the editor, so that a page arriving slowly shows no HTML, and without
scripts it is shown. React's `RichTextEditor` takes `value` and `state` and calls back; Svelte's binds `value`, `state`
and `modified`. The browser's HTML is never to be trusted: Wicket's `RichTextEditor` cleans it on
the server with `RichTextPolicy` (the OWASP Java HTML Sanitizer) before its model gets it, keeping
what the features write, and `RichTextView` shows it cleaned the same way; a server keeping what
the React or Svelte editor gives cleans it so.

CKEditor 5 (`ck-editor.ts`, built as `antilia-ckeditor.js` for Wicket's `CKEditor`) is a second
rich text editor, with the contract of the first: the text in a text area a form posts, HTML or
Markdown (`format`, CKEditor's Markdown plugin), the features of `RICH_TEXT_FEATURES` that CKEditor
has plugins for (`CK_EDITOR_FEATURE_PLUGINS`), `placeholder`, `readOnly`, `count`, the bar resizing
it, the state and the events of `RICH_TEXT_EDITOR_EVENTS`, `data-modified`, `markSaved()`, Ctrl+S,
and going on as it was when Wicket renders it anew. It is CKEditor's ClassicEditor, with its own
toolbar, balloons and dialogs (`toolbar` takes CKEditor's names of its tools, `plugins` and `config`
more of CKEditor), in the colors of the theme: `ck-editor.css` gives CKEditor's custom properties
the theme's values, in the editor and in CKEditor's body wrapper, where its balloons are.

None of CKEditor's code is in the core or in ui-components: CKEditor is licensed under the GPL 2 or
later, or under the commercial terms of CKSource, and the components are under the Apache License.
The page brings it: the browser build of the npm package `ckeditor5`, whose `ckeditor5.umd.js`
defines the global `CKEDITOR` the editor takes by default, or the classes of a build of its own
(`build`), with CKEditor's style sheet, `ckeditor5.css`, and its translations; and its license key
(`licenseKey`): `'GPL'` for the open-source distribution, or the key CKSource gave. CKEditor starts
asynchronously: `ready` resolves once it has, and should it not start, without CKEditor in the page
or with a key it refuses, the text area shows again and `onError` tells why. The HTML has no style
attribute: text aligns by the classes of the Tiptap editor, images have no width or height, for
which CKEditor writes a style, and the HTML a page gives or the user pastes is made quiet first
(`quietHtml`), as the Tiptap editor's. The selection counts the places between the nodes too, as
ProseMirror's does, so `RichTextEditorState` fits both editors. React's `CKEditor` and Svelte's are
our own wrappers, as for the other components, not `@ckeditor/ckeditor5-react`. Wicket's `CKEditor`
renders the CKEditor of the application's `CKEditorLibrary`, with its license key, and cleans the
HTML with `CKEditorPolicy`, which keeps what CKEditor's plugins write; `CKEditorView` shows it in
CKEditor's content style, `ckeditor5-content.css`.

A code block is in a language, `<pre><code class="language-java">`, which the menu of the tool
code block chooses while the editor has languages (`codeLanguages`, or those of the highlighter):
the block made a paragraph again, or in plain text, Java, JavaScript, TypeScript, JSON or Markdown;
without languages the tool makes a code block, or takes it away. With `highlight`, the blocks show
in the colors of their language (`rich-text-code-blocks.ts`): a ProseMirror plugin gives their text
the classes `tok-keyword` and so on of Lezer's `classHighlighter`, which `code-editor.css` colors from
the theme, as decorations, so the HTML stays plain; the name of the language shows in the corner of
the block. The grammars are not in the editor: Lezer's parsers of the five languages and
`@lezer/highlight` weigh about 217 KB minified, 80 KB gzipped, a third of the editor's 627 KB, and
most pages have no code to color. So the highlighter is the page's: `highlight: true` takes the one
registered (`registerRichTextHighlighter`), or the option is a highlighter itself. `lezerHighlighter`
(`rich-text-highlight.ts`) loads the grammar of a language the first time a block shows it, by a
dynamic `import()`, which a bundler of the page's own makes a file of its own; React's and Svelte's
`highlight` take it for `true`. A Wicket IIFE cannot load files so, esbuild inlines what it imports,
so Wicket's grammars are a script of their own, `antilia-rich-text-highlight.js`: only Lezer, no
editor, rendered after `antilia-rich-text-editor.js` only where a `RichTextEditor` highlights
(`setHighlighting(true)`), which registers them as `Wicket.RichTextHighlighter`; the editors already
there draw their blocks again. The code editor's script has the grammars too, but in CodeMirror's
classes and with CodeMirror around them: sharing them would make a page highlighting rich text load
the whole code editor.

While the page uploads images (`onUpload(file): Promise<string>`, `rich-text-upload.ts`), the form
of the image has a button choosing a file, and an image dropped into the text, or pasted without
HTML as a screenshot is, uploads too: a mark stands where it goes until the page answers its
address, which the image then takes, or a message above the text says why not. The editor checks
the type (`uploadTypes`: PNG, JPEG, GIF and WebP by default, no SVG) and the size (`uploadMaxSize`)
first, and the address the page answers goes through `isSafeUrl`: never `data:`. `uploadImageTo(url)`
posts the file to a URL and reads `{"url": ...}` or `{"error": ...}` from the answer, as Wicket's
`RichTextImageUploadBehavior` answers: a `RichTextEditor` given a `RichTextImageStore` has one, which
checks the size again, tells the type by the first bytes of the file, has the store keep it, and
checks the address the store answers; a page adds one for its React and Svelte editors.

The tabs (`tabs.ts`, built as `antilia-tabs.js` for Wicket's `TabbedPanel`) show a list of tabs,
along the top of their panels (`horizontal`) or down their left side (`vertical`), and the panel of
the tab selected, in one of nine designs (`design`, `TABS_DESIGNS`, `TabsDesign` in Wicket), all in the colors
of the theme: `tabs` joined to the panel, `pills` and `underline`, those `TabsStyle` gives Wicket's
`TabbedPanel` too; `segmented`, `folder` and `lifted`, made for tabs along the top; `sidebar`,
`steps` (numbered, those before the selected one done) and `cards` (pointing at the panel), made for
tabs down the side. Every design works in both orientations, and in the look of the page too: in the
Bootstrap look the list of `tabs`, `pills` and `underline` is Bootstrap's `nav nav-tabs`, `nav-pills`
and `nav-underline`, the other designs stay the components' own. It is a WAI-ARIA tab list: a click selects a tab, and while the focus
is on one the arrow keys select the next or the previous, Home and End the first and the last; only
the selected tab takes the focus with Tab, then its actions and its close button. A closable tab
(`data-closable="true"`) has a button closing it, after its title; Delete closes the tab with the
focus, and so does the middle button. Closing a tab whose panel has changes not saved asks first, in
the dialog of the desktop's markup (`unsaved.ts`, which the desktop uses too, so `modal.css` and
`button.css` are needed), or as `confirmClose` does; closing it anyway puts its fields back as they
were rendered and fires `antilia-tabs:discard` on the panel. A panel has changes as the desktop's
windows do, by its fields or by `data-modified`, and its tab shows a dot meanwhile, which follows the
user typing and the page changing `data-modified` (`setModified()`). A tab may have actions of the
page's own, buttons in an element of the class `antilia-tab-actions` after its title, before the
close button; a click on them does not select the tab. `onSelect` and `onClose` report what the
user did; a closed tab is hidden, with `data-closed`, until the page takes it away or opens it again
(`open()`). The core works on markup with a list of tabs and the panels, each tab and its panel
sharing a name in `data-tab`, which React, Svelte and Wicket render, and adds and takes away as
they please: the tabs find them as they come and go, set their roles and states, and hide the
panels not selected. Tabs go in each other's panels. React has `Tabs` with `Tab` children, its ref
a `TabsHandle`; Svelte's `Tabs` takes the list `tabs` and the snippets `panel`, `icon` and
`actions`, and binds `selected`. In Wicket the tabs are `Tab`s with `TabAction`s, all panels
rendered; the name of the tab selected is the model, and with `setSendToServer(true)` the browser
sends each tab the user selects. The browser always tells the server the tabs the user closes,
which `Tabs` takes away, calling `onClosed`; `addTab(tab, target)` and `removeTab(tab, target)`
change the tabs via Ajax without rendering the other panels, and `setModified(tab, modified, target)`
marks a tab whose changes the server heard of.

The menus (`menu.ts`, built as `antilia-menu.js` for Wicket's `Menu`) show a bar of items along the
top of the page (`horizontal`) or down its side (`vertical`), each a link leading somewhere, a
button doing something, or an item opening a submenu of more items, nested as deep as one likes,
with separators between them. It is a WAI-ARIA menu bar: a click opens a submenu, and while one of
the bar is open the pointer opens the others; in a submenu the pointer resting on an item opens its
own, after a moment. The arrow keys move along the bar and up and down a submenu, the arrow pointing
away from an item opening its submenu and its first item, Escape closing it; from a submenu of a bar
along the top the arrows across go on to the next or the previous item of the bar. Home and End go to
the first and the last item, a letter to the next item starting with it; only one item of the bar
takes the focus with Tab. Choosing an item, a click outside, Tab or the focus leaving the menu close
the submenus. They pop up below the items of a bar along the top and beside the others, on the side
with room and in the window, following the page as it scrolls; where they are, and how high they may
be, are custom properties set through the CSSOM, so they need no inline style and escape the
overflow of what holds the menu. A menu down the side may open them in place instead (`inline`),
under their items, as the side bar of an application does: several stay open, the arrows up and down
go through the items shown and the arrows across open and close the submenus, as in a tree. With
`openOnHover` the submenus of the bar open as the pointer rests on them too, and close as it leaves,
as in the navigation of a site. `aria-current="page"` marks the item of the page shown, which gets
`antilia-menu-current`, the items holding it `antilia-menu-current-parent`, and the submenus holding
it start open in place; `aria-disabled="true"` an item the user cannot choose. There are four designs
(`design`, `MENU_DESIGNS`, `MenuDesign` in Wicket), in the colors of the theme and both orientations:
`bar`, of the toolbar's color, `pills`, the current item filled, `underline` and `sidebar`, an accent
on the current item. In the Bootstrap look the bar is Bootstrap's `nav`, `nav-pills` and
`nav-underline`, its items `nav-link`, a submenu a `dropdown-menu` of `dropdown-item`s. The core works
on markup of lists, the bar of the class `antilia-menu-list`, each item a link or a button among the
children of its element with its submenu next to it, which React, Svelte and Wicket render, and
change as they please. React's and Svelte's `Menu` take the tree of items, `items`: a link with
`href`, a button with `onClick` (`onclick` in Svelte), a submenu with `items`, a separator
`{ separator: true }`; `onSelect` hears each item chosen. In Wicket, `Menu` takes `IMenuItem`s, or an
`IMenu` whose label names the bar: `BookmarkableMenuItem` (current on its page with the same
parameters), `AjaxMenuItem`, `LinkMenuItem`, `SubMenu` and `MenuSeparator`, an item of one's own
returning any link; `MenuLinkPanel` puts the icon and the label in the link of an item and its
tooltip on it. The ids of the items stay the same each time the menu renders, so the item with the
focus keeps it when Ajax renders the menu again.

The breadcrumbs (`breadcrumb.ts`, built as `antilia-breadcrumb.js` for Wicket's `Breadcrumb`) show
where the page is: a row of crumbs from the home page down to the page shown, the last crumb, its
label with `aria-current="page"` and no link. A `nav`, named "Breadcrumb" (`texts.label`), around an
ordered list, as the WAI-ARIA breadcrumb pattern has it; `breadcrumb.css` draws the separator between
the crumbs, `chevron`, `slash` or `arrow` (`separator`, `BREADCRUMB_SEPARATORS`,
`BreadcrumbSeparator` in Wicket), with an empty alternative text so that screen readers skip it, and
by a class, so no inline style. With `maxItems` a longer trail shows its first crumb and its last
`maxItems - 1`, the others hidden (`hidden` and `data-antilia-collapsed`) behind a button, "…",
labelled by `texts.more`, which shows them in place and gives the focus to the first of them; the
trail collapses again when its crumbs change. Wicket renders the crumbs hidden and the button
already, which the core takes over; the core makes the button when the markup has none. In the
Bootstrap look the list is Bootstrap's `breadcrumb`, its crumbs `breadcrumb-item`s and the last one
`active`, the separator through `--bs-breadcrumb-divider`. React's and Svelte's `Breadcrumb` take the
crumbs, `items`: a link with `href`, a button with `onClick` (`onclick` in Svelte), or a label alone;
`onSelect` hears each crumb chosen and `onExpand` the crumbs hidden shown. In Wicket, `Breadcrumb`
takes the `IMenuItem`s of the menus: `BookmarkableMenuItem`, `AjaxMenuItem`, `LinkMenuItem`, and an
item without a link, such as a `SubMenu`, is a crumb leading nowhere.

The popovers (`popover.ts`, built as `antilia-popover.js` for Wicket's `Popover`) are bubbles with
an arrow pointing at the element that opened them, their trigger, telling more about it: a `title`
and a `body` of text, markup (`html`) or a node, and a `content`, an element of the page that moves
into the popover while it is open and back to its place, hidden by its class
`antilia-popover-content`, when it closes; its element with `autofocus` takes the focus.
`Popover.show(trigger, options)` opens one next to the trigger where `placement` says
(`POPOVER_PLACEMENTS`): on a side, centered (`top-center`, `right-center`, `bottom-center`,
`left-center`) or along the trigger from one of its edges (`top-left`, `top-right`, `right-top`,
`right-bottom`, `bottom-left`, `bottom-right`, `left-top`, `left-bottom`), its arrow on the middle of
the trigger, or on the side with the most room (`auto`). Where it does not fit in the window it opens
where it does: on the opposite side, else on the side across with the most room; without room
anywhere it stays in the window over the trigger, without its arrow. It slides along to stay in the
window with its arrow on the trigger, following it as the page scrolls and as its content resizes;
`Popover.toggle()` closes the one open at the trigger instead. It closes with its close button,
Escape, a click outside, a click on the trigger again (which the trigger's own listeners then do not
get) or the trigger leaving the page; one is open at a time, and `Popover.closeAll()` closes it. It
is a non-modal dialog labelled by its title, with `aria-expanded` and `aria-controls` on the
trigger. Appended to the body, it copies the theme properties of its trigger, so it is in the
trigger's theme, in the color of its `variant` (`default`, `info`, `success`, `warning`, `error`)
and in one of nine designs (`design`, `POPOVER_DESIGNS`, `PopoverDesign` in Wicket): `card`, with a stripe of its color, `solid`,
`outline`, `glass` (frosted), `inverse` (a deep shade of the theme, as a tooltip), `callout` (tinted, with a badge), `aurora`
(a border flowing into the accent of the theme), `comic` (a speech bubble) and `neon`; where it is
and its arrow are custom properties set through the CSSOM. It takes the look of its trigger as well
(`look` to give another) and carries it into the body, `data-antilia-look` and `antilia-look-<name>`:
in the Bootstrap look its header and body are Bootstrap's `popover-header` and `popover-body`, its
close button `btn-close`, and the `card` takes the shape of Bootstrap's popover; `popoverClasses()`
(`Popover.getCssClasses()` in Wicket) gives the classes of a bubble drawn in the page. React and Svelte have `PopoverButton`, a
button opening a popover of `options` with a `content` they render, React through a portal, Svelte
moving it. In Wicket, `new Popover(body).setTitle(...).show(target, trigger)` shows one in the
response to an Ajax request; `new Popover(component)` shows a component of the page, a panel with a
form for example, which the server may render again via Ajax as it shows it, and
`Popover.close(target)` closes it. The action of a tab (`TabAction`, given its button) shows what
the server knows of the tab this way.

The notifications (`notifications.ts`, built as `antilia-notifications.js` for Wicket's
`Notifications`) show short messages as toasts, stacked in a corner of the window or in the middle
of its top or bottom edge (`position`, `top-right` by default, or each notification's own). Each
has a header, with the icon of its type, a `title` and a button closing it (unless `closable` is
false), and a `body`, a text, markup with `html: true`, or a node of the page's own. It stays until
the user closes it, with a `timeout` of 0, or goes after its `timeout` in milliseconds (5000 by
default; an error's is `errorTimeout`, 0 by default, so errors stay until closed), which a bar along
its bottom counts down; the countdown waits while the pointer is over the notification
(`pauseOnHover`) or the focus in it, and Escape closes the one that has the focus. The `type` is
`success`, `error`, `warning` or `info`, each with its icon and the color of the theme
(`--antilia-theme-success`, `-error`, `-warning`, `-info`, which every theme sets) on a header in
the theme's primary color, as collapsible panels have, or one of the
page's own, colored by the class `antilia-notification-<type>` setting `--antilia-notification-color` and with
the `icon` it is given, the path of an SVG. A stack shows `max` notifications (5), the oldest going
first. Errors and warnings are alerts, the others status messages. The core works on an element the
framework renders empty, adding a stack for each position into it, so the notifications are in its
theme; the timeout is a custom property set through the CSSOM. `show()` returns a handle that
closes the notification; `onClose` tells why it closed. React has `Notifications`, whose ref
shows them, and `NotificationsProvider` with `useNotifications()`; Svelte's `Notifications`,
bound with `bind:this`, has `show()` and `closeAll()`. In Wicket, `show(Notification)` shows one
via Ajax, or when the page renders (`setEscapeBody(false)` for a body of markup), and the feedback
messages of the page become notifications too, except those a `FeedbackPanel` rendering in the
request shows. Every page of the demo has them, in its base page.

The desktop (`desktop.ts`, built as `antilia-desktop.js` for Wicket's `Desktop` and
`DesktopWindow`) holds floating windows that are not modal, many at once, and a taskbar. A click
brings a window to the front, the others staying behind as they were; its title bar drags it, a
double click on it folds the window to it; its buttons minimize it to the taskbar, maximize it to
the whole desktop and close it, and its edges and corners resize it, as each window allows
(`data-resizable`, `data-maximizable`, `data-minimizable`, `data-foldable`, `data-closable`). While
the focus is in a window, F11 maximizes and restores it, and Ctrl+F6 and Ctrl+Shift+F6 go round the
windows (Ctrl+Tab and Ctrl+Shift+Tab too, where the browser lets the page have them, which Chrome
and Firefox do not). The taskbar, on any side (`taskbar`), has a button for each window that is not
closed: it brings the window to the front, restoring it when minimized, and minimizes the one
already in front; while the window in front is maximized, the one brought to the front is maximized
too, taking its place. Its last buttons tile the windows in a grid, side by side, one above the
other, or cascade them (`tiling`), and let the desktop fill the whole page and take it back into
the page (`expandable`, `expanded`; Escape on the taskbar too). The user moves the taskbar to
another side by dragging it, by its grip or where it has no button, or with the arrow keys on its
grip (`movableTaskbar`); the windows move and shrink to stay on the desktop, as they do when it
takes less of the page again (`fit()`). The menu of the taskbar, the button after the grip, lists
the windows open, to bring one to the front, and those closed, to open one again (`menu`). Closing
a window with changes not saved asks first, in a dialog of the markup of `modal.ts` (so its style
sheets, `modal.css` and `button.css`, are needed too), or as `confirmClose` does; closing it anyway
puts its fields back as they were rendered and fires `antilia-desktop:discard` on the window, for
the page to drop changes it keeps itself. A window has changes when a field in it holds another
value than the one it was rendered with, or as `data-modified` on it or on an element in it says
(`modified` of `DesktopWindow`, `setModified` in Wicket); the fields in an element marked
`data-modified="false"` do not count, unless in one marked `"auto"` in it. The windows start where `data-x`, `data-y`, `data-width` and
`data-height` say, minimized, maximized, folded or closed as `data-minimized` and the like say, and
the desktop keeps their state in those attributes and in CSS custom properties set through the
CSSOM, so it needs no inline style. `onChange` reports the states of all windows (`WindowState`)
each time the user changed any, moved the taskbar or let the desktop fill the page; `onTaskbar` and
`onExpand` report those two. React, Svelte and Wicket render the windows and may add, remove and
render them again: the desktop finds them as they come and go, and gives a window rendered again
the state it had. In Wicket the state of a window is its model, kept in the page by default; with
`setSendToServer(true)` the browser sends the states via Ajax, with the side of the taskbar and
whether the desktop fills the page, and the windows give their models the new states.

The windows are in panes, as in the workspaces of Ubuntu (`panes`, `activePane`, `DesktopPane` in
Wicket): the desktop shows the windows of one pane at a time, and the taskbar the buttons of those
windows and, after them, the panes (`paneSwitcher`). The default pane, of the id `DEFAULT_PANE`
(`"0"`), is always there and cannot be removed. The user adds a pane, asked for its name in a
dialog, renames the one shown (F2 or a double click on its button too) and removes it, its windows
going to the default pane (`editablePanes`); Ctrl+Shift and the arrow keys go from pane to pane,
and the first button on the right of each title bar moves its window to another pane, or to a new
one. A window is in the pane its `data-pane` says (`pane` of `DesktopWindow`, `setPane` in
Wicket), in the pane shown when it says none; `onPanes` reports the panes and the one shown, and
Wicket keeps them with `setSendToServer(true)`. The launcher of the taskbar, the button after its
menu, opens windows of the types the page registers (`windowTypes`, `registerWindowType()`,
`addWindowType(WindowType)` in Wicket) in the pane shown: a type with `create` makes the element,
one without asks the page through `onOpenWindow`; React and Svelte render the windows of their
types themselves, and Wicket adds a `DesktopWindow` the type creates by Ajax. Closed, those windows
are gone.

The uploads (`upload.ts`, built as `antilia-upload.js` and `antilia-upload.css` for Wicket's
`FilePickerBehavior`, `UploadProgressBar` and `UploadToResourceField`) draw a file input the same in
every browser and follow what it sends. `FilePicker` hides the input, which stays in the page and
holds the files, and puts a picker right after it, in the look of the page: a button of the theme
opening the browser's dialog (`design: 'button'`), or a zone the files are dropped on, with a button
in it (`'drop-zone'`), then what was chosen, the name and the size of a file or the list of
several, and a button clearing them. Files dropped on the picker go into the input, those its
`accept` takes and only the first unless it is `multiple`, and the input fires `input` and `change`,
so a form, an Ajax behavior or the binding of a framework gets them as if chosen in the dialog; the
picker tells the names of the others. A `multiple` input keeps the files chosen before: files chosen
in the dialog or dropped are added to them, and each has a button taking it out of the list. Given
the limits of the server (`maxSize`, `fileMaxSize`, `fileCountMax`), the picker tells at once which
the files break (`problems()`, `uploadProblems(element)`), and while its files are being sent it lets
nothing change. `UploadProgress` draws the progress of the uploads of a source,
an input or a form, into an element the framework renders empty, hidden until an upload starts: a
progress bar of the theme (`progress-bar.css`), what was sent of how much, how fast and how long it
still takes, then whether it went well, or why not: the message of the server, the HTTP status it
answered, or that the connection broke off. `uploadStarted(source, xhr)`, `uploadEnded(source,
outcome, message, status)` and `uploadRefused(source, message)`, for files not sent as they break a
limit, tell it. It asks the server how far it got reading the files, at `statusUrl` every
`pollMillis`, which is what arrived; without one it follows the progress events of the request, which
count what the browser handed to the network, whose buffers may hold megabytes the server did not
read yet. `UploadGuard.begin()` asks before the page is left until the upload ends and, with
`requests`, stops the other requests of the page meanwhile: `UploadGuard.blocks(request)` lets go
the requests of an element in one marked `data-antilia-upload-bypass`, those marked to bypass and
those to the URLs the upload allows, and marks the element of a request it stopped with
`data-antilia-upload-blocked` for a moment, which the style sheet outlines, and the progress tells.
In Wicket, `wicket/upload.ts` gives every Ajax request a precondition asking the guard (bypassed
with `UploadGuard.bypass()` in Java), follows the request of a multipart form sent by Ajax where
the engine gives it to `/ajax/call/beforeSend` (the plain JavaScript one does), and the post of
one sent the usual way, sending neither while the files break a limit, and ends the upload when its
request ends should Wicket not tell; `UploadToResourceField`, Wicket's `FileUploadToResourceField`
telling its uploads around the hooks of Wicket's field, which stay the page's, guards them, lets
the field's own callback go, sends one upload at a time and none breaking a limit, and words
its errors in the language of the page. The server tells the progress with
`UploadStatusResource`, from what Wicket keeps of an upload with
`setUploadProgressUpdatesEnabled(true)`. The demo's Upload page shows a form posted or sent by Ajax,
an upload to a resource with the guard, and zones to drop files on, and slows the server down
reading the files to watch the progress on a fast network.

The trees (`tree.ts`, built as `json-tree.js` for `JsonBasedTree` and `JsonBasedTreeGrid`) render
their nodes from JSON with a template, as a WAI-ARIA tree or, given columns, a treegrid, loading
the children of a folder the first time it is expanded. Their style sheet, `tree.css`, built as
`antilia-tree.css`, and their icons (`TREE_ICONS`, `TreeIcon` in Java) are those of the Wicket
`Tree` and `TreeGrid` too, so all of them look the same.

A theme gives the components their colors, a look the CSS classes of their parts. Every element a
component draws is a part, in the table of `look.ts` (`LookPart` in Java, which reads the same
table from `antilia-looks.properties`, written by the build): a dialog's header, a table's header,
a dropdown's option... An element keeps its own classes, those of the Antilia look, which its script
and its style sheet find it by, and a look adds its own (`partClasses(look, part)`); buttons alone
have none of their own (`lookClasses`). The Bootstrap look (`BOOTSTRAP_LOOK`, `Look.BOOTSTRAP`)
gives the parts Bootstrap's classes: `btn btn-primary btn-sm` for a button, `modal-header` for a
dialog's header, `table` for a data table, `dropdown-menu` and `dropdown-item` for a menu and its
options, `card` for a panel, `alert alert-danger` for a notification of an error, `pagination` and
`page-link` for Wicket's paging navigator, `nav nav-tabs` for tabs, `popover-header` for a popover's
header... and its corners, shadows and focus rings through the
custom properties `--antilia-look-radius`, `-radius-sm`, `-shadow`, `-shadow-lg` and `-focus-ring`.
The page brings the style sheet of Bootstrap 5.3, any build of it, such as a theme of Bootswatch,
never its script: what the components do stays their own. `antilia-look-bootstrap.css`, which the
Bootstrap look renders in Wicket, bridges the two, for example showing the menus Bootstrap hides
without its script's `show`; in the themes `Theme.BOOTSTRAP` and `BOOTSTRAP_DARK` the components
take Bootstrap's colors from its `--bs-*` properties, light or in its dark mode (`data-bs-theme`);
in any other theme Bootstrap's components and properties take the theme's colors, so the page's
own Bootstrap markup in the theme matches the components.

Each part is of an aspect: what it is, its colors, its size or a state. A look can be made of
others, an aspect from each: `composeLook(BOOTSTRAP_LOOK, { color: ANTILIA_LOOK, size: ANTILIA_LOOK
})`, `Look.BOOTSTRAP.with(LookAspect.COLOR, Look.ANTILIA)` in Java (`CompoundLook`), gives the parts
Bootstrap's classes with the colors and the sizes of Antilia's. A look is on an element and
everything inside it as `data-antilia-look`, which Wicket's `ThemeBehavior` writes, the name of the
look and the aspects taken from others (`bootstrap color:antilia size:antilia`), with a class
`antilia-look-<name>` for each look it is made of; `lookOf(element)` finds it, so markup made in the
browser is in the look of the page. The core's components take a `look` option too, which React's
`LookProvider` and `useLook()` and Svelte's `setLook()` and `getLook()` (or `LOOK_CONTEXT` given to
`mount`) give their wrappers. In Wicket `Looks.of(component)` finds the look of a component: that of
the closest `ThemeBehavior` with one, else the look of the session (`Looks.setSessionLook`, or the
model `Looks.sessionLook()` for a `ThemeBehavior` and a `LookChoice`), which every page of the
session then has, else the application's default (`Looks.setDefault`). `LookBehavior` gives a
component's tag the classes of its parts, and `LookContainer` an element of a component's markup.
A look of one's own, for another framework of CSS, implements `ILook` in Java and is registered in
the browser with `registerLook()`. The demo's Looks page shows the looks side by side. Bootstrap
writes some images as `data:` URLs, such as the tick of its check boxes, which a content security
policy such as Wicket's (`img-src 'self'`) blocks: the bridge serves the x of the close buttons
itself, and the demo allows `data:` images for Bootstrap's own markup.

Some core modules are shared state in a page: the template engines (`templates()`, which
Handlebars registers into) and, for the infinite list and the table, the scroll bars and the
resizable columns. Each Wicket script bundles its own copy of the core, so these scripts use the
page's `Wicket.Templates` through `useTemplates()`, and the list and the table the page's
`Wicket.Scrollbar`, `Wicket.ResizableColumns` and `Wicket.Veil`, which keep one registry per page.

## Building

Maven builds everything: `ui-components` installs Node and npm into `frontend/node`, runs
`npm ci`, `npm run build` and, unless `-DskipTests`, `npm test`. It then takes
`core/dist/wicket` as a resource directory and minifies its scripts and style sheets into
`*.min.js` and `*.min.css`, like those in `src/main/java`.

`demo-app` runs `npm run build:demo`, which builds `react-demo.js` and `svelte-demo.js` into
`demo/dist`, a resource directory of `demo-app`. Each holds an app of every demo, keyed by the
demo's name (`dropdown`, `autocomplete`, `palette`, `datePicker`, `jsonList`, `jsonTable`, `tree`, `treeGrid`,
`scrollbar`, `splitPanel`, `progressBar`, `notifications`, `desktop`, `buttons`, `modalDialog`). A demo page shows them in its Wicket, React and
Svelte tabs (`FrameworkTabs`): the element an app renders into carries the demo's name and its
configuration as JSON, texts in the session's language, options, initial values and the URLs of
the resources the application mounts (`/api/choices`, `/api/users`); the app is mounted the first
time its tab is shown (`demo/src/apps.ts`). An element marked `data-mount` mounts its app at once,
as the React and Svelte grids in the windows of the desktop demo do, and again when Wicket renders
it again by Ajax. `demo-app` uses the packages `ui-components` installed
and built, so build it with `-am`.

To work on the frontend alone, with the Node Maven installed:

```bash
cd frontend
export PATH=$PWD/node:$PATH
npm ci
npm run build    # core first: react and svelte import its build
npm run build:demo
npm test         # type checks, then Vitest with jsdom
```

Every test run measures the coverage of `src` with `@vitest/coverage-v8`: a summary on the
console, the report in `<package>/coverage` (`index.html`, and `lcov.info`). The run fails when
the coverage drops below the thresholds in the package's `vitest.config.ts`. The Java modules
have theirs from JaCoCo: `mvn verify` writes `target/site/jacoco` and checks the module's
`jacoco.minimum.*` properties.

## The host

The core never calls Wicket. What it needs from the page goes through a `Host`
(`core/src/host.ts`): elements added or about to be removed by an update of the page, messages
pushed by the server, and a log. In Wicket these are `Wicket.Event` topics and `Wicket.Log`
(`core/src/wicket/host.ts`). In React and Svelte the frameworks call `destroy()` when they
unmount a component, so the standalone host's node events never fire.

## Writing a wrapper

The framework renders the elements and the core adds its own next to them, never moving or
changing an element the framework owns. For the drop-down, the framework renders the wrapper
and the select:

```html
<div class="antilia-dropdown"><select class="antilia-dropdown-native">...</select></div>
```

The core adds the button and the list to the wrapper. The component creates the core's object
once it's mounted, calls `configure()` when the texts change, `refresh()` when the options
change and `update()` when the value changes, and calls `destroy()` on unmount.

The date picker goes the same way, over an input that holds its value as ISO text:

```html
<div class="antilia-datepicker"><input class="antilia-datepicker-native" value="2026-10-09"></div>
```

The core adds a text field showing the value the way the locale writes it, the button and the
calendar; it sets the input's value through the native setter and fires `input` and `change`, so
a React `onChange` and a Svelte `bind:value` see every change. The wrappers pass the locale, the
bounds and the texts as options (`configure()`) and call `update()` when the value changes.

The table goes the same way: the framework renders the table in the markup of the Wicket one, its
navigation, headers and column chooser, and leaves the body empty; the core's `DynamicDataTable`
renders the rows into it from the columns' templates, loading them through the `load` the
component gives, and reports clicks, the selection and the column chooser as actions. Another
order or set of columns starts the core's table again.
