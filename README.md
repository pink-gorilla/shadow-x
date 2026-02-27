# shadow-x [![GitHub Actions status |pink-gorilla/shadow-x](https://github.com/pink-gorilla/shadow-x/workflows/CI/badge.svg)](https://github.com/pink-gorilla/shadow-x/actions?workflow=CI)[![Clojars Project](https://img.shields.io/clojars/v/org.pinkgorilla/shadow-x.svg)](https://clojars.org/org.pinkgorilla/shadow-x)

**End Users** this project is not for you.

- With shadow-x you can build clj/cljs web apps (server: clj, frontend: reagent/reframe)
- shadow-x uses great tools such as: reagent, reframe, tailwind-css, shadow-cljs
- shadow-x brings a things that can be hard to configure (or repetitive) when you develop a web app such as:
  - unit test runner
  - routing (via edn config), extensible both in frontend  and backend
  - keybindings
  - loading animation
  - notifications and dialogs

- shadow-x is used in [Goldly](https://github.com/pink-gorilla/goldly).

# features

## shadow-x build 
  - this feature is available for apps that use shadow-x
  - compiles/watches via shadow-cljs 
  - does not require shadow-cljs.edn
  - bundle-size report at compile time


# shadow-x - how to use:

To start shadow-x you have to pass it two parameter: **profile** and **config**

# shadow-x Compile Profiles

**shadow-x profile** can be one of the following strings:
- watch: builds and runs shadow dev server (shadow-cljs watch)
- compile: builds bundle and output bundle stats
- release: builds release bundle (for production)  no tenx. no source-maps bundle stats
- jetty: runs app, with bundle compiled via compile or release 
- ci: builds bundle for unit tests
- npm-install: just installs npm dependencies (based on deps.cljs)

**shadow-x config**:
- is a clojure datastructure 
- is used in backend (clj) and frontend (cljs)
- can be passed as a string (link to a edn file or resource) - mandatory if shadow-x is used in a leiningen project
- can be passed as a clojure datastructure - for quicker configuration in deps.edn projects.

## npm dependencies
- keep your npm dependencies *only* in deps.cljs
- do NOT create a package.json file!
- package.json will be auto generated


Add npm dependencies that you want to use into a clojure deps.cljs

```
{:npm-deps
 {; font awesome
  "@fortawesome/fontawesome-free" "^5.13.0"
```

Sometimes github repo and npm module do not match. 
Check this to see what goes on:  https://unpkg.com/@ricokahler/oauth2-popup-flow@2.0.0-alpha.1/index.js

## unit tests
- clj `bb test-clj`
- cljs: `bb test-cljs`

## static build

```
  cd demo-shadow-x
  clj -X:shadow-x:compile  or clojure -X:shadow-x:release-adv
  clj -X:shadow-x:static
  cp node_modules/@icon .gorilla/static/index_files -r
 ./http-server.sh

```

  
