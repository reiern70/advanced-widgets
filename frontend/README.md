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

So far the Dropdown has moved here; the other scripts of `ui-components` follow
(see `WIP/REACT_SVELTE.md`).

## Building

Maven builds everything: `ui-components` installs Node and npm into `frontend/node`, runs
`npm ci`, `npm run build` and, unless `-DskipTests`, `npm test`. It then takes
`core/dist/wicket` as a resource directory and minifies its scripts and style sheets into
`*.min.js` and `*.min.css`, like those in `src/main/java`.

`demo-app` runs `npm run build:demo`, which builds `react-demo.js` and `svelte-demo.js` into
`demo/dist`, a resource directory of `demo-app`, for its `FrameworksPage`
(`/react-svelte`). That page gives them the URL of the `ChoicesResource` the application mounts
(`/api/choices`), which serves their data as JSON. `demo-app` uses the packages
`ui-components` installed and built, so build it with `-am`.

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
