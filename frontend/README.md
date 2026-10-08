# frontend

The browser side of the components, written once and offered to three frameworks:

| Package | What it is |
|---|---|
| `core` (`@advanced-widgets/core`) | The components in plain TypeScript, without a framework, and their style sheets. |
| `react` (`@advanced-widgets/react`) | React 18 and 19 components wrapping the core. |
| `svelte` (`@advanced-widgets/svelte`) | Svelte 5 components wrapping the core; the core's functions also work as Svelte actions. |
| `demo` | The React and Svelte pages of the demo application (not published). |

Wicket uses the core too: `core/scripts/build-wicket.mjs` builds each component into an IIFE
script under the name and package the Java classes load it from (`Wicket.Dropdown` in
`com/advanced/widgets/dropdown/wicket-dropdown.js`, ...), with the Wicket host set. The Java
side and the pages don't change.

Every script of `ui-components` has moved here: the dynamic data tables (`data-table.ts`, built
as `dynamic-data-table.js`) and Handlebars as one of their template engines (`handlebars.ts`,
built as `handlebars-adapter.js`), the drop-down and its chips, the autocomplete, the infinite list
and the template engines they render with, the clipboard, the floating panel, the scroll bars, and
the movable and resizable columns. The style sheets of the table and the progress bar have moved
too. React and Svelte have `Dropdown`, `Autocomplete`, `InfiniteList`, `Scrollable`,
`ProgressBar`, `DataTable`, `Tree` and `TreeGrid`; the core's `dropdown` and `scrollbar` also
work as Svelte actions, and `Clipboard.install()` and `FloatingPanel.install()` make the
`data-wicket-copy` and floating panel markup work in any page.

The trees (`tree.ts`, built as `json-tree.js` for `JsonBasedTree` and `JsonBasedTreeGrid`) render
their nodes from JSON with a template, as a WAI-ARIA tree or, given columns, a treegrid, loading
the children of a folder the first time it is expanded. Their style sheet, `tree.css`, built as
`wicket-tree.css`, and their icons (`TREE_ICONS`, `TreeIcon` in Java) are those of the Wicket
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
demo's name (`dropdown`, `autocomplete`, `jsonList`, `jsonTable`, `tree`, `treeGrid`,
`scrollbar`, `progressBar`, and
`overview` for the combined page `/react-svelte`). A demo page shows them in its Wicket, React and
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
<div class="wicket-dropdown"><select class="wicket-dropdown-native">...</select></div>
```

The core adds the button and the list to the wrapper. The component creates the core's object
once it's mounted, calls `configure()` when the texts change, `refresh()` when the options
change and `update()` when the value changes, and calls `destroy()` on unmount.

The table goes the same way: the framework renders the table in the markup of the Wicket one, its
navigation, headers and column chooser, and leaves the body empty; the core's `DynamicDataTable`
renders the rows into it from the columns' templates, loading them through the `load` the
component gives, and reports clicks, the selection and the column chooser as actions. Another
order or set of columns starts the core's table again.
