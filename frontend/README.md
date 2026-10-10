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
`com/antilia/widgets/dropdown/antilia-dropdown.js`, ...), with the Wicket host set. The Java
side and the pages don't change.

Every script of `ui-components` has moved here: the dynamic data tables (`data-table.ts`, built
as `ant-data-table.js`) and Handlebars as one of their template engines (`handlebars.ts`,
built as `handlebars-adapter.js`), the drop-down and its chips, the palette, the autocomplete, the date
picker (`date-picker.ts`, built as `antilia-date-picker.js`), the infinite list
and the template engines they render with, the clipboard, the floating panel, the scroll bars, and
the movable and resizable columns. The style sheets of the table and the progress bar have moved
too, and the buttons' (`button.css`, built as `antilia-button.css`, which Wicket's `ButtonBehavior`
and `AjaxActionButton` use). React and Svelte have `Dropdown`, `Palette`, `Autocomplete`, `DatePicker`,
`DateTimePicker`, `InfiniteList`, `Scrollable`, `ProgressBar`, `Button`, `DataTable`, `Tree` and
`TreeGrid`; the core's `dropdown`, `palette`, `datePicker` and `scrollbar` also work as Svelte actions, and `Clipboard.install()` and
`FloatingPanel.install()` make the `data-antilia-copy` and floating panel markup work in any page.

A `DataTable` loads its rows with `loadRows`, from a mounted resource for example, or is given
them as `rows`, a local list of JSON it filters, sorts and pages in the browser with the core's
`localRows`. The JSON grid demo does both: its rows come from `/api/users`, a `JsonRowsResource`
that also creates, changes and deletes rows (POST, PUT and DELETE), or from a list the page gave.

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

The trees (`tree.ts`, built as `json-tree.js` for `JsonBasedTree` and `JsonBasedTreeGrid`) render
their nodes from JSON with a template, as a WAI-ARIA tree or, given columns, a treegrid, loading
the children of a folder the first time it is expanded. Their style sheet, `tree.css`, built as
`antilia-tree.css`, and their icons (`TREE_ICONS`, `TreeIcon` in Java) are those of the Wicket
`Tree` and `TreeGrid` too, so all of them look the same.

`DataTable`, `Tree` and `TreeGrid` are private, as the JSON table and the trees are in Wicket:
they check a license key in the browser (`license.ts`), the same `AW1.` keys with the same Ed25519
signature the server checks, and show the same placeholder without one. Give the key with
`activate(key)` once, or to a component as `licenseKey`. Web Crypto verifies the signature; where
it has no Ed25519 yet (Firefox before 129), `ed25519.ts` does, and where the page has no Web
Crypto at all (served over plain HTTP from another host than `localhost`, such as
`http://192.168.0.152:9180`), `ed25519.ts` hashes with a SHA-512 of its own too.

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
`scrollbar`, `progressBar`, `buttons`, `modalDialog`). A demo page shows them in its Wicket, React and
Svelte tabs (`FrameworkTabs`): the element an app renders into carries the demo's name and its
configuration as JSON, texts in the session's language, options, initial values and the URLs of
the resources the application mounts (`/api/choices`, `/api/users`); the app is mounted the first
time its tab is shown (`demo/src/apps.ts`). `demo-app` uses the packages `ui-components` installed
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
