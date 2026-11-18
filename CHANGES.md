# Changes

The components come in two lines, whose versions follow Wicket's:

- `10.11.0-RCn`, built from the `line-10.x` branch on Wicket 10.11.0;
- `11.0.0-RCn`, built from `master` on Wicket 11.0.0, with what needs Wicket 11 besides: uploads
  to a resource (`UploadToResourceField`, `UploadGuard`) and Wicket's Ajax without jQuery.

The newest release candidates come first. Under **Upgrading** is what an application has to
change when it moves to that release candidate from the one before: what was renamed, what is
gone and what the browser now sends under other names, which fails at run time rather than at
compile time. The pages of the demo and its deployment are left out.

## 11.0.0-RC6 (unreleased)

What 11.0.0-RC5 has, and:

- Wicket's veil, `PageVeilBehavior` and `LocalVeilBehavior`, with three gaps closed by an add-on
  to its script, `antilia-veil-gaps.js` (`VeilGaps`), which the library's `Initializer` renders
  wherever a page has Wicket's veil (#141). Enter and Space are dropped under a veil, but where
  they type, so a key pressed twice on a button no longer sends two requests. A request is veiled
  from `/ajax/call/init` on, as Wicket's Ajax queues it behind another one, not only once it is
  sent, so a second click on a button waiting its turn behind a request the veil leaves alone
  falls on the veil; the veil comes down for the requests that never run: throttled and replaced,
  dropped by their channel, stopped by their precondition. And the click of a button whose
  release a veil caught, the veil of a field's change having come up between the press and the
  release, is given back once the veil is down. `VeilGaps.setEnabled(application, false)` leaves
  Wicket's veil as it is.

**Upgrading:**

- On a page with Wicket's veil, a request waiting its turn veils the page with a transparent
  `.antilia-veil-guard` and the wait cursor, until Wicket's veil comes with its spinner as it is
  sent; a local veil goes up, spinner and all, from the click. Enter and Space under a veil are
  dropped, but in text fields, text areas and editable content.

## 11.0.0-RC5 (2026-11-17)

What 11.0.0-RC4 has, and:

- Drop-downs open their list above any box that would cut it, such as the body of a dialog that
  scrolls: in the top layer where the browser has popovers, fixed to the window otherwise,
  under the button and at least as wide as it, upwards when there is more room above, and
  shortened when there is room on neither side (#134). The list follows the button as the page
  or a box around it scrolls. Long options wrap instead of ending in «…», the list being up to
  32em wide and never wider than the window, and a single drop-down's button has its choice's
  whole text as its `title`. A select's `<optgroup>`s are drawn as labelled groups, hidden when
  the filter leaves none of their options: `Dropdown#setGroup(function)` or an overridden
  `getGroup(choice)` in Wicket, an option's `group` in React and Svelte, and the look parts
  `dropdown-group`, `dropdown-group-label` and `dropdown-group-list`. They are on `line-10.x`
  too, for the next 10.11.0 release candidate.

**Upgrading:**

- The open list of a drop-down, `.antilia-dropdown-menu`, is no longer placed by CSS inside the
  drop-down's wrapper but fixed to the window by the script, in the top layer: CSS of an
  application's own that placed it, or that put it above the button through
  `.antilia-dropdown-up > .antilia-dropdown-menu`, no longer counts.
- Long options wrap onto more lines instead of being cut with an ellipsis.

## 11.0.0-RC4 (2026-11-17)

The components of 10.11.0-RC10 on Wicket 11.0.0.

**Upgrading:** as for 10.11.0-RC10.

## 10.11.0-RC10 (2026-11-17)

- The rich text editors upload to a resource of the library, mounted once by the application
  and not tied to a page: `RichTextFiles.mount(application, path, store)`, and
  `RichTextEditor#setUploadResource(files)` (#97). It checks the uploads as the editor's own
  behavior does, has the store keep them and serves them by name, through
  `IRichTextImageStore#open(name)`: with the media type their bytes tell, `nosniff` and
  `Content-Security-Policy: sandbox`, inline only for the formats a browser may open and as
  attachments otherwise. It refuses uploads from other sites; `setReadCheck`, `setUploadCheck`
  and `setBaseUrl` let the application decide who sees and sends what, and where the files are
  served from.
- The rich text editors attach files that are no images, PDFs, Office documents, OpenDocuments,
  ZIPs and texts, each a link in the text with its name and size (#99): `setAttachmentStore`,
  `setAttachmentFormats`, `setAttachmentMaxSize`, `setAttachmentMaxCount`, the tools "Attach a
  file" and "Files of the text", and `RichTextAttachmentsRemovedEvent` for the files taken out.
  `IUploadFileFormat` and `UploadFileFormat` tell a format by the bytes of the file;
  `IUploadScanner` (`setUploadScanner`) accepts, refuses or cleans every upload, images too,
  before the store keeps it. In Wicket, React and Svelte.
- CKEditor hears of the errors escaping it after it started, tells them to the page
  (`onError(error, failure)`, the event `antilia-ck-editor:error`) and to the server
  (`CKEditorErrorEvent`), and starts again from its last text as the Watchdog did before CKEditor
  49, a limited number of times: `CKEditor#setRecovery(CKEditorRecovery)`, null for never
  (#80).
- CKEditor uploads the images the user chooses, pastes or drops, the Tiptap editor's way, with
  its checks, its messages and the progress of each upload: `CKEditor#setImageStore` and the
  limits of its images; `onUpload` in React, `onupload` in Svelte (#79).
- Data tables keep the rows per page and the sort the user chose in their `IColumnStateStore`,
  with the columns: `ColumnState#getItemsPerPage`, `getSortProperty` and `isSortAscending`
  (#127). A table reads its state once, when it is first rendered instead of when it is
  initialized, so a store set after adding the table counts; `loadColumnState()` reads it again.
- The themes color the page around the components too: each sets `--antilia-theme-page`,
  `-nav`, `-primary-soft`, `-primary-text`, `-muted`, `-accent-hover`, `-shadow` and `-focus`,
  and the red, green and blue of its colors, `--antilia-theme-*-rgb` (#122). The Bootstrap look
  gives these to Bootstrap's `--bs-*-rgb`, so that Bootstrap's links and its `.text-*` and
  `.bg-*` utilities take the theme's colors; the theme editor edits the new colors.
- The title bar of the dialogs and the wizards is a little taller in the components' own look
  (#128).
- Data tables export to Excel beside CSV: `XlsxExportToolbarAction` writes an `.xlsx` of its
  own, without Apache POI, its cells typed, the header bold, frozen and filtered, and the rows as
  they are read (#126).
- Progress bars can be still, for a bar that shows a figure rather than something under way:
  `ProgressBar#setStill`, `ProgressBarColumn#setStill`, `BeanProgressBarColumn#setStill`,
  `ProgressBar.markup(value, still)` and the prop `still` of the React and Svelte bars draw the
  filled part solid at any percentage, neither striped nor moving (#133). The default stays
  striped and moving, and an indeterminate bar moves still or not.
- A request of an editor's events no longer reaches a component the server took away, which
  answered with a `ComponentNotFoundException` and the internal error page: a tab the user
  closes leaves the page with its panel, as a desktop window disposed does, by
  `Wicket.DOM.remove`; the events of an element Wicket's Ajax removes are taken off, those
  waiting dropped; and `RemovedComponentEventListener`, which the library adds to each
  application, answers a client event of a component gone with an empty Ajax response (#125).
- This file (#109).

**Upgrading:**

- A class of an application's own extending `AbstractRichTextEditor` implements its image
  methods, now abstract there: `getImageStore`, `setImageStore`, `getImageMaxSize`,
  `setImageMaxSize`, `getImageTypes`, `setImageTypes`, `getImageMaxDimensions`,
  `setImageMaxDimensions` and `isImageUploading`. `RichTextEditor` and `CKEditor` have them.
- A file that is no image, dropped or pasted into a rich text editor taking attachments, is
  attached instead of refused.
- CKEditor's `onError` hears of the errors after the start too, not only of a failed start, with
  a second argument telling what the editor does about it.
- While CKEditor uploads an image it shows it as a `data:` address: a page uploading with it
  allows `img-src data:` in its Content Security Policy.
- A table calls `onColumnsChanged` also when the user chooses the rows per page or sorts the
  rows. A column state store of an application's own that writes a `ColumnState` field by field
  writes the three new values too, or the rows per page and the sort are lost with the table, as
  before.
- A theme of an application's own sets the new properties too, the `-rgb` ones above all, if it
  is used with the Bootstrap look: without them Bootstrap's links, its `.text-*` and `.bg-*`
  utilities and its muted text inside the theme lose their colors. The theme editor writes them
  in the CSS it gives.
- A tab the user closes is taken out of the page in the browser, its panel with it, instead of
  staying there hidden; it and a desktop window disposed go by `Wicket.DOM.remove`, so the
  scripts in them hear of it on `/dom/node/removing`.
- A `ComponentNotFoundException` of a client event's request, one carrying `antilia-event`, no
  longer reaches the application's exception mapper: the library answers it with an empty Ajax
  response.

## 11.0.0-RC2 and 11.0.0-RC3 (2026-11-13)

The components of 10.11.0-RC9 on Wicket 11.0.0. 11.0.0-RC3 is tagged on the same commit as
11.0.0-RC2.

**Upgrading:** as for 10.11.0-RC9.

## 11.0.0-RC1 (2026-11-13)

The first release candidate built from `master`: the components of 10.11.0-RC8 on Wicket 11.0.0
from Maven Central, but the footer row of the data tables (#116), which came with 11.0.0-RC2.

**Upgrading** from 10.11.0-RC8: the dependency on Wicket 11.0.0 instead of 10.11.0. The page
veil is Wicket's `org.apache.wicket.extensions.ajax.veil.PageVeilBehavior` instead of
`com.antiliacode.widgets.veil.PageVeilBehavior`, which only `line-10.x` has.

## 10.11.0-RC9 (2026-11-13)

- Data tables: a column of checkboxes bound to a boolean of the row and changed by Ajax,
  `BeanCheckBoxColumn` and `CheckBoxColumn` (#117); row actions as words, `TextRowAction`, and a
  cell's value as the link of a row action, `BeanActionLinkColumn` and `ActionLinkColumn` (#119);
  a progress bar column, `BeanProgressBarColumn` (#118); a sortable provider of rows in memory,
  `ListDynamicDataProvider` (#120); the CSV export may end with the footer,
  `CsvExportToolbarAction#setExportFooter(true)` (#116).
- The components speak French (#121).

**Upgrading:** nothing to change.

## 10.11.0-RC8 (2026-11-13)

- Each client event going to the server has a Wicket Ajax behavior of its own, with its own URL
  and Ajax attributes, so that one event can veil the page and another not, of one name too
  (#109).
- Data tables: a footer row for totals, `IFooterColumn` and `IFooterDataProvider` (#116); row
  actions only on the rows they apply to, `IAjaxActionColumnContributor#isVisible(row)`, and
  shown disabled with the reason on the others, `isEnabled(row)` and `getDisabledReason(row)`
  (#111).

**Upgrading:**

- The browser no longer sends `antilia-event-index`, and `ClientEventsBehavior.INDEX_PARAMETER`
  is gone. `antilia-event`, the name of the event, still goes, for veils and logs; the server no
  longer reads it.
- An event added to a `ClientEventsBehavior`, or to an editor, after the component was configured
  is an `IllegalStateException`: add the events while building the component. `getEvents()`
  gives the same instances each time.
- A row action left out of a row is removed from the row instead of hidden by a class: the class
  `ant-data-table-action-hidden` and `HIDDEN_ACTION_CSS_CLASS` are gone,
  `DISABLED_ACTION_CSS_CLASS` is new. `UpdateRowMessageType.newMessage` takes the disabled
  actions as a fifth argument, and `renderRow` of the core, React and Svelte tables takes the
  hidden and the disabled actions as one object, `DataTableRowActions`.

## 10.11.0-RC7 (2026-11-12)

- Pluggable editor events: the rich text editor, CKEditor and the code editor send their state
  by events the page chooses, each with its own handler, and a delay for those going to the
  server (#108). `removeEvents(INPUT, SELECT, SCROLL)` sends the state on blur and as the page
  goes only, and a state not sent yet goes with the page's next Ajax request.
- The events going to the server are round trips of Wicket's Ajax with attributes of their own
  (#108). `ClientEventsBehavior#updateAjaxAttributes`, the editors'
  `updateEventAjaxAttributes`, and the event's own `updateAjaxAttributes`, `setExtraParameter`,
  `addDynamicExtraParameter` and `addAjaxCallListener` change them; extra parameters go in `ep`,
  so that `PageVeilBehavior.noVeil(attributes)` leaves a request alone.
- The request telling the server of the images taken out of a text goes with attributes the
  server renders too, `RichTextEditor#updateImagesRemovalAjaxAttributes`, and on Wicket 11 so do
  those of the upload field to its callback, `UploadToResourceField#updateCallbackAjaxAttributes`
  (#110).

**Upgrading:** the requests of the editors carry other parameters, which a page reading them
by name, or matching them in a `/ajax/call/before` subscriber, has to follow:

| Before (RC6) | Now |
| --- | --- |
| the editor sent its state by a request of its own, as `state` | each event going to the server carries it as `antilia-state` (JSON), with `antilia-text-changed` telling whether the text changed since it was last sent |
| | the page's other Ajax requests carry a state not sent yet as `antilia-pending:<markup id of the editor>` |
| the events went by the Ajax event `antilia-client-events:ajax` | each event goes with the attributes Wicket renders for it, `antilia-client-events:ajax` is gone: change them by the hooks above instead of matching a name |
| `antilia-event`, `antilia-event-index`, `antilia-event-values` | unchanged (`antilia-event-index` went in RC8) |

- The handlers of the events in the browser take `(event, component, options, send)` instead of
  `(event, component, send, options)`: a page function of its own has to swap its last two
  parameters. `onEvent` and `onError` no longer get the name of the event, which the event
  knows.
- `setSendToServer` and `CodeEditorEventBehavior` are gone: the state goes by `SendStateEvent`s,
  on input, select, scroll and resize 400 ms after the user paused and on blur and leave at
  once, by default; take some away with `removeEvents`, or stop one with
  `setEvent(ClientEvent.nothing(BLUR))`.
- `IAjaxEvent.getExtraParameters` is gone: use `setExtraParameter` or `addDynamicExtraParameter`
  of `AbstractAjaxRoundTripEvent`. `SEND_HANDLER` and `SEND_STATE_HANDLER` moved from
  `IClientEvent` to `IAjaxRoundTripEvent`.
- React and Svelte editors take `events` instead of `stateDelay`.
- For scripts of an application's own on the editors' scripts: the rich text editor takes
  `imagesAjax`, the rendered attributes, instead of `imagesUrl`, and `sendState()` of
  `send-state.ts` is gone (`roundTrip` instead).

## 10.11.0-RC6 (2026-11-11)

- A wizard: typed steps known only as the user goes, each with its title, body and buttons,
  moved through via Ajax (#101), and a wizard in a dialog, `ModalWizard` (#103).
- The code editor may save its text as the user leaves it, shows the errors of its validators in
  its text (`JsonValidator` telling where JSON is not well formed), scrolls with the scroll bars
  of the components and takes a model telling whether it is read only.
- The rich text editors may store uploaded images permanently and insert them from the store,
  show the progress of an upload and cancel it, and scroll with the scroll bars of the
  components, in Wicket, React and Svelte (#98, #97).

**Upgrading:** nothing to change. The text of the editors scrolls with the library's scroll bar
instead of the browser's.

## 10.11.0-RC5 (2026-10-30)

- The rich text editors upload several images at once, may limit their pixels and scale them
  down, list the images of the text and delete those taken out of it (#96); may keep the images
  of the site alone (#86); tell what their cleaning removed, of a paste and of the text on the
  server, and can reject such a text (#88).
- The throttle makes an Ajax request wait before Wicket locks its page, and slows down at most
  two requests of a session at once (#86).
- A security review (#86) hardened the components and the demo.

**Upgrading:**

- Click-to-copy copies only for the elements a `ClipboardCopyBehavior` attached:
  `antilia-clipboard.js` no longer installs itself on the document, so `data-antilia-copy` on an
  element alone does nothing. Attach the element, or a container with
  `ClipboardCopyBehavior.within()`; React and Svelte pages whose markup is all their own may call
  `Clipboard.install()`.
- Svelte's `Menu` and `Breadcrumb` write the icon of an item as text or render it as a snippet;
  markup goes in `iconMarkup`, which React's `MenuItem` and `BreadcrumbItem` take too.
- `JsonRowsResource` takes rows to create or change only as `application/json` (415 otherwise),
  and asks `isAllowed(attributes, method)` first (403 when false).
- The CSV export puts an apostrophe before a field a spreadsheet could read as a formula, unless
  told not to.
- The rich text editors clean Markdown and Tiptap's JSON too, by `RichTextMarkdownSanitizer` and
  `RichTextJsonSanitizer` (`setMarkdownSanitizer`, `setJsonSanitizer`): links and images to
  addresses `RichTextPolicy.isSafeUrl` refuses no longer reach the model.

## 10.11.0-RC4 (2026-10-29)

- The rich text editors raise and lower text, superscript and subscript, in the Tiptap editor and
  CKEditor (#85).

**Upgrading:** nothing to change.

## 10.11.0-RC3 (2026-10-28)

10.11.0-RC2 was released the same day without a tag in the repository; what it holds is listed
here with RC3.

- CKEditor 5 is a second rich text editor, with the Wicket contract of the Tiptap one, while the
  application brings CKEditor and its license (`CKEditorLibrary`).
- The rich text editors listen to any number of events of any names, in the browser or on the
  server, with the form or without (`IClientEvent`, `IAjaxEvent`, `IAjaxSubmitEvent`,
  `ClientEventsBehavior`); fire types of events a page or a library registers (#82); check the
  spelling, have a proofreader underline mistakes and offer mentions and completions (#83); take
  palettes, features and tools a page plugs in (#81); pick colors from a grid of squares (#84).
- Breadcrumbs (#76); filters above the grids wait 150 ms for the typing to pause (#77); on a
  phone the tables scroll sideways in their own box (#55).

**Upgrading:**

- `Tabs` is `TabbedPanel`, its markup and texts too: an application overriding `Tabs.close`
  renames its keys to `TabbedPanel.close`, `TabbedPanel.modified` and the others. `TabsDesign`
  and `TabsOrientation` keep their names.
- `RichTextImageStore` is `IRichTextImageStore`, and `RichTextSanitizer` is
  `IRichTextSanitizer`.
- `RichTextEditorEvent` and `RichTextEditorEventBehavior` are gone: the editors take events by
  `addEvent`, their names in `RichTextEditorEvents`; `RichTextEditorResizeEvent` and
  `RichTextEditorChangeEvent` hear of the size and of the text.
- The editors are attached to a `<textarea>` only.

## 10.11.0-RC1 (2026-10-27)

The first release candidate of the components on Wicket 10.11.0 (#74).
