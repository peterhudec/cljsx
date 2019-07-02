# `cljsx` The Missing [JSX] Macro

[![Clojars Project](https://img.shields.io/clojars/v/cljsx.svg)](https://clojars.org/cljsx) 
[![Build Status](https://travis-ci.org/peterhudec/cljsx.svg?branch=master)](https://travis-ci.org/peterhudec/cljsx)

`cljsx` tries to make it as easy as possible to use plain, unwrapped [React]
(or any other virtual dom) and all the related JavaScript libraries in
ClojureScript by mimicking the syntax of [JSX]. It's mainly meant to be used
with the amazing [shadow-cljs] with its effortless usage of plain NPM packages,
but it works just as well with [Figwheel] and Clojure.

## TL;DR

It's just a macro which transforms occurences of _JSX expressions_ like
`(<div :className "foo" > "Hello" "World!")` into
`(createElement "div" {:className "foo"} "Hello" "World!")`.
`cljsx` doesn't really care about what `createElement` points to. It only tries
to figure out whether it's a JavaScript or Clojure function so it can convert
the arguments with [clj->js] if needed.

### Features

* Not yet another [React] wrapper, just a macro. 
* Works with all _vdom_ libraries which have the `createElement` signature.
* JSX expressions can appear anywhere and can be arbitrarily nested. That's why
  it works so well with libraries like [React Router] or [Material-UI].
  Whole namespaces can be wrapped in the macro.
* Has _props spread_ operator similar to `<div {...props} />`. 
* Supports _truthy prop_ shorthand as in `<button disabled />`.
* Automatically converts from and to JS if needed.
* Built with [spec], so you'll know early when something goes wrong.

Let's say you want to write a SPA directly in [React], not using any wrapper.
Maybe you want to use [React Router] or some other JavaScript library
and you don't want to be dealing with incompatibility issues with wrappers like [reagent].
You would directly use the `react/createElement` function which you assign to `h` for brevity:

```clj
(ns shadow-cljs-example.main
  (:require ["react" :as react]
            ["react-dom" :as react-dom]
            ["react-router-dom" :as rr]))

(def h react/createElement)

(react-dom/render
 (h rr/BrowserRouter nil
    (h rr/Route nil
       (fn [route-props]
         (h react/Fragment nil
            (h "h1" nil  (-> route-props .-location .-pathname (subs 1)))
            (h "ul" nil
               (map #(h "li" #js{:key %}
                        (h rr/Link #js{:to %} %))
                    ["foo" "bar" "baz"]))))))
 (js/document.querySelector "#mount-point"))
```

The `cljsx.core/jsx>` macro call in the following example just expands
to something very similar and equivalent to the example above,
except that the code is a bit more readable and looks more familiar
to someone with [React] background.

```clj
(ns shadow-cljs-example.main
  (:require ["react" :refer [createElement Fragment]]
            ["react-dom" :as react-dom]
            ["react-router-dom" :as rr]
            [cljsx.core :refer [jsx> fn-clj]]))

(jsx>
 (react-dom/render
  (<rr/BrowserRouter>
   (<rr/Route>
    (fn-clj [{{path :pathname} :location}]
      (<>
       (<h1> (subs path 1))
       (<ul>
        (map #(<li :key % >
                   (<rr/Link :to % > %))
             ["foo" "bar" "baz"]))))))
  (js/document.querySelector "#mount-point")))
```

And this is what it would look like written in [reagent].
Notice the `:>` in front of every [React Router] component,
the need to wrap the return value of the `rr/Route` callback
in `r/as-element` and also, that using the anonymous function
shorthand is a bit awkward with the [hiccups] vector markup,
all of which just add noise to the code.

```clj
(ns shadow-cljs-example.main
  (:require [reagent.core :as r]
            ["react-router-dom" :as rr]))

(r/render
 [:> rr/BrowserRouter
  [:> rr/Route
   (fn [route-props]
     (r/as-element
      [:<>
       [:h1 (-> route-props .-location .-pathname (subs 1))]
       [:ul (map #(identity ^{:key %} [:li
                                       [:> rr/Link {:to %} %]])
                 ["foo" "bar" "baz"])]]))]]
 (js/document.querySelector "#mount-point"))
```

## Motivation

If you think about it, [JSX] is just a _reader macro_, which merely adds
syntactic sugar to JavaScript. Oddly enough in ClojureScript, the problem of
working with [React] is mostly approached by inventing all sorts of wrappers,
which more often than not come bundled with their own state managemet.
The most idiomatic way to express [React] DOM trees in Clojure seems to be the
[hiccups] format of nested vectors, which probably stems from the obsession with
data in Clojure (list's don't seem to be data enough).

`cljsx.core/jsx>` is the missing macro. It's the main workhorse of the package
alongside a bunch of helpers for taming the JavaScript conversion.
The main goal of the package is expressiveness, readability and familiarity with
[React] idioms. `cljsx` is not tied to [React] in any way other than through the
signature of the `createElement` function and it works with all [JSX]
compatible libraries like [Inferno], [Nerv], [Preact] or [Snabbdom].

## Usage

The main thing `cljsx` provides is the `jsx>` macro and its brethren `react>`,
`snabbdom>`, `inferno>` etc. The macro will recognize _JSX expressions_ in the
code passed to it and will expand them into `createElement` calls. Anything else
will be kept unchanged: 

```clj
(macroexpand (cljs/jsx> (<div>)))
;; => (createElement "div" nil)  
```

So if you want the above example to work with [React], you should _refer_
`createElement` from the `react` namespace. Similarly, if you intend to use the
`<>` _fragment tag_, you should also refer the `Fragment` name from the same
namespace.

```clj
(ns my-react-app.main
  (:require [cljsx.core :refer [jsx>]]
            [react :refer [createElement Fragment]]
            [react-dom :as react-dom]))

(react-dom/render
  ;; The JSX macro call
  (jsx>
   ;; This s-expression is recognized as a JSX expression, because the item
   ;; at the function call position is recognized as a JSX tag by the macro.
   (<>
    ;; This is not recognized as a jsx-expression, because the first item of
    ;; the s-expression is not a JSX tag.
    (str "Hello, ")
    ;; This is a JSX expression, because the first item is a JSX tag.
    (<h1> "CLJSX!")))
  (js/document.querySelector "#mount-point"))

;; The jsx> macro call expands to
(createElement Fragment nil
 (createElement "h1" nil "Hello, CLJSX!"))
```

The `cljsx.core/jsx>` macro expands to `createElement` and `Fragment` calls, which in
JSX parlance is called _pragma_. If you will be using `cljsx` with [React], you
can save some keystrokes by using the `cljsx.core/react>` macro, which works exactly
as the `jsx>` macro, except that the _pragma_ is bound to `react/createElement`
and `react/Fragment`.

```clj
(ns my-react-app.main
  (:require [cljsx.core :refer [react>]]
            [react :as react]
            [react-dom :as react-dom]))

(react-dom/render
  (react>
   (<>
    (<h1> "Hello, CLJSX!")))
  (js/document.querySelector "#mount-point"))
```

There are other macros with _pragmas_ bound to other [JSX] compatible frameworks.

* `inferno>` bound to `inferno-create-element/createElement` and
  `inferno/Fragment`.
* `nervjs>` bound to `nervjs/createElement` and `Fragment`
* `preact>` bound to `preact/createElement` and `Fragment`
* `snabbdom>` bound to `snabbdom/createElement` and `Fragment`

Notice that the last three macros are bound to unprefixed `Fragment`. This is
because none of these frameworks seems to actually have a notion of a _fragment_.
Having it bound to `Fragment` allows you to choose how the fragment will be
interpreted. You could for example fall back to a _div_ with
`(def Fragment "div")` or make it a function which throws an exception, or just
let it fail on the undeclared `Fragment` var.

You can even create your own JSX macro with the `cljsx.core/defjsx` macro, which is
how all the aforementioned macros are defined. Note that as `defjsx` is a macro
which creates macros, it needs to be called in a `clj` or `cljc` file.

```clj
;; my_jsx_app/main.clj 
(ns my-jsx-app.main
  (:require [cljsx.core :as cljsx])) 

(cljsx/defjsx my-jsx-macro my-create-element my-fragment)

(def my-fragment :fragment)

(defn my-create-element [tag props & children]
  {:tag tag
   :props props
   :children children})

(my-jsx-macro
 (<>
  (<h1> "Hello, CLJSX!")))

;; The above statement returns
{:tag :fragment
 :props nil
 :children [{:tag "h1"
             :props nil
             :children ["Hello, CLJSX!"]}]}
```

### What is a JSX Expression?

Any list whose first element is a _JSX tag_ is recognized as a _JSX_ expression.

### What is a JSX Tag?

Any symbol in the function call position starting with the `<` character,
followed by an alphanumeric character and optionally ending with the `>`
character is a _JSX tag_. The string of characters between the opening `<` and
the optional closing `>` is the _tag name_ e.g. `<>`, `<a>`, `<div`, `<foo>`,
`<bar`, `<Baz>`, `<Bing`, `<foo.bar/Baz>`, `<foo/bar.Baz`. The following are
still _JSX tags_ even though they have invalid names: `<foo.bar>`, `<<>`,
`<..foo>`. This is to fail early on [spec] violation, as opposed to undeclared
var errors.

#### Intrinsic Tags

`JSX tags` with all lowercase, alphanumeric _names_ are _intrinsic_ and will
expand to a `createElement` call with the tag name in a string literal e.g.
`<div>`, `<a>`, `<h1>`, `<span>`, `<foobarbaz>`.

```clj
(macroexpand (cljsx/jsx> (<foo>)))
;; => (createElement "foo" nil)
```

#### Reference Tags

`JSX tags` whose names are capitalized e.g. `<Foo>` are _reference tags_ and
will be expanded with the name as symbol. Tag names can be namespaced in the
same way as symbols with the only exception, that the last part must be
capitalized e.g. `<foo.bar/Baz>`, `<foo/bar.Baz>`, in which case they are also
considered to be _reference tags_.

```clj
(macroexpand (cljsx/jsx> (<Foo>)))
;; => (createElement Foo nil)

(macroexpand (cljsx/jsx> (<foo.bar/Baz>)))
;; => (createElement foo.bar/Baz nil)

(macroexpand (cljsx/jsx> (<foo/bar.Baz>)))
;; => (createElement foo/bar.Baz nil)
```

#### Invalid Tags

Anything else is recognized as an _invalid tag_ and will be reported by the
[spec] during compile time. 

### Simple JSX Expressions Without _Props_

If a tag ends with the `>` character, it has no _props_ or _attributes_ and the
_JSX expression_ expands to a `createElement` call with `nil` as the second
argument.

```clj
(macroexpand (cljsx/jsx> (<foo>)))
;; => (createElement "foo" nil)

(macroexpand (cljsx/jsx> (<Bar>)))
;; => (createElement Bar nil)
```

### JSX Expressions With _Props_

If the tag does *not* end with the `>` character, the `jsx>` macro will
will expect a _sequence of props_ after the tag, terminated by the `>` symbol.
A _prop_ is either:

* A single keyword
* A keyword followed by a non-keyword expression
* The `...` symbol followed by a _spreadable_ expression.

Note that `cljsx` doesn't do any _prop_ name conversion e.g. hyphenization, etc,
as it is none of its business. If you wan't to write [React] props hyphenized,
or, you wan't to use `:class` instead of `:className`, you can wrap
`react/createElement` in your own `createElement` implementation
where you can intercept the props and forward them transformed to [React]:

```clj
(ns shadow-cljs-example.main
  (:require ["react" :as react]
            ["react-dom" :as react-dom]
            [camel-snake-kebab.core :as csk]
            [cljsx.core :as cljsx]))

(defn createElement [tag props & children]
  (apply react/createElement
         tag
         (->> props
              (reduce-kv #(assoc %1 (csk/->camelCase %2) %3) {})
              clj->js)
         children))

(cljsx/jsx>
 (react-dom/render
  (<div :class-name "foo" 
        :on-click #(println "clicked") >
        "Bar")
  (js/document.querySelector "#mount-point")))
```

#### Key-value _Props_

A key-value prop is a keyword followed by any non-keyword expression e.g
`:foo 123`, `:bar baz`, `:bing (+ 1 2 3)`. _JSX expressions_ with this kind of
props will be expanded to a `createElement` call with the second argument being
a map of the props. 

```clj
(cljsx/jsx>
 (<a :href "http://example.com" :target "_blank" >))

;; Expands to
(createElement "a" {:href "http://example.com" :target "_blank"})
```

```clj
(cljsx/jsx>
 (<button :onClick #(println "click")
          :disabled (not enabled) >))

;; Expands to
(createElement "a" {:onClick #(println "click")
                    :disabled (not enabled)})
```

#### Shorthand Truthy _Props_

As JSX, `cljsx` supports shorthand _truthy props_, which is any keyword
followed by either another keyword or the `>` or `...` symbols.
A _shorthand prop_ will be expanded to a value of `true`.

```clj
(cljsx/jsx>
 (<button :disabled :onClick #(println "click") >))

(cljsx/jsx>
 (<button :onClick #(println "click") :disabled >))

;; Both expand to
(createElement "button" {:disabled true :onClick #(println "click")})
```

#### The `...` _Spread_ Operator

As JSX, `cljsx` supports _spreading_ a map (or anything _spreadable_) to the
_props_. Anything followed by the `...` symbol will be merged with the rest of
the props using [merge] in the order
they appear in the _props_ sequence.

```clj
(def props {:baz 33})

(cljsx/jsx>
 (<Foo :foo 11
       :bar 22
       ... {:foo 111}
       :baz 33
       ... props
       ... [:bing 444]
       :bing >))

;; Expands to
(createElement Foo (merge {:foo 11 :bar 22}
                          {:foo 111}
                          {:baz 33}
                          props
                          [:bing 444]
                          {:bing true}))
```

### Children

All expression after a _simple tag_ or the first `>` symbol after a _props tag_
in a _JSX expression_ are treated like _children_ and are added as parameters
right after _props_ to the expanded _JSX expression_.

```clj
(cljsx/jsx>
 (<div> "Hello, World!"))

;; Expands to
(createElement "div" nil "Hello, World!")

(cljsx/jsx>
 (<span :style {:color "aqua"} > "Hello," " Aqua" " World!"))

;; Expands to
(createElement "span" {:style {:color "aqua"}} "Hello," " Aqua" " World!")
```

### Fragment

As [JSX], `cljsx` supports the `<>` _fragment_ tag and as in [JSX] it can't have
_props_. A _JSX expression_ with a fragment tag expands to a `createElement`
call with the `Fragment` symbol as the tag argument.

```clj
(cljsx/jsx>
 (<ul>
   (<>
    (<li> "Foo")
    (<li> "Bar")
    (<li> "Baz"))))

;; Expands to
(createElement "ul" nil
               (createElement Fragment nil
                              (createElement "li" nil "Foo")
                              (createElement "li" nil "Bar")
                              (createElement "li" nil "Baz")))
```

If you need to pass _props_ to the tag, e.g. the `key` prop in [React],
you can do it by using `Fragment` directly without the `<>` syntax.

```clj
(cljsx/jsx>
 (<Fragment :key 123 >
   (<div> "Foo")))

;; Expands to
(createElement Fragment {:key 123}
               (createElement "div" nil "Foo"))
```
 
### Nesting

_JSX expressions_ can appear anywhere inside of the macro and can be arbitrarily
deeply nested. They can appear as values in all the datastructure literals like
lists, vectors, maps, sets. They can even appear as keys in maps. Moreover, they
can appear as _prop_ values, which is a common pattern in [React] libraries like
[React Router] and [Material-UI]. You can even use it as values in spreads as
long as it returns a value accepted by [merge].

```clj
(let [Header (fn [{:keys [children]}]
                 (<h1> "Hello, " children "!"))
        Body (fn [{:keys [items]}]
               (<ul>
                (map #(<li :key (str (first %)) >
                           (second %))
                     items)))
        footer (<h3> "Enjoy!")]
    (<div>
     (<Header> "CLJSX")
     (<Body :items {:foo (<h1> "Foo")
                    :bar (<h2> "Bar")
                    :baz (<h3> "Baz")} >)
     footer))
```

The `jsx>` macro accepts any number of expressions. Multiple expressions
will be expanded wrapped in a [do](https://clojuredocs.org/clojure.core/do)
form. You can actually wrapp a whole namespace in the macro:

```clj
(ns my-shadow-cljs-react-app.main
  (:require ["react" :refer [createElement Fragment]]
            ["react-dom" :refer [render]]
            [cljsx.core :refer [jsx>]]))

(jsx>
 (defn Header []
   (<h1> "Hello, CLJSX!"))

 (defn Body []
   (<p> "Lorem ipsum dolor sit amet."))

 (defn Footer []
   (<h3> "Enjoy!"))

 (defn App []
   (<div>
    (<Header>)
    (<Body>)
    (<Footer>)))

 (render
  (<App>)
  (js/document.querySelector "#mount-point")))
```

### Escaping

The `jsx>` macro uses some symbols as part of its DSL like the `...` as
the _props spread_ operator, and the `>` as the end of _props tag_.
It also specially treats symbols on the function call position whose names start
with the `<` character, which it recognises as _JSX tags_.

The macro doesn't have any special escape mechanism, but it relies on what's
already available in Clojure. So if you need to pass the `>` function as a value
of a prop, or you happen to have a var assigned to a symbol starting with the
`<` character, you can dereference the vars with the `@` reader macro:

```clj
(ns my-shadow-cljs-react-app.main
  (:require ["react" :refer [createElement Fragment]]
            ["react-dom" :refer [render]]
            [cljsx.core :refer [jsx>]]
            ;; You can not use the ... symbol in def, but you can import it.
            [made-up-three-dots-exporting-namespace :refer [...]]))

(defn <div> [] "My name is <div>")
(def <a> "My name is <a>")

(defn SortableList [{:keys [comparator items]}]
  (jsx>
   (<ul>
    (map #(<li> %) (sort comparator items)))))

(jsx>
 (render
  ;; Would be interpreted as props spread operator if not escaped.
  (<div :title @#'... >
        (<SortableList :items ["a" "b" "c"]
                       ;; Would be interpreted as JSX tag end if not escaped.
                       :comparator @#'> >)
        ;; Would be interpreted as a JSX tag if not escaped.
        (@#'<div>)
        ;; No need to escape if not in a function call position.
        <a>)
  (js/document.querySelector "#mount-point")))
```

#### Passing Keywords As Prop Values

Because of the _truthy prop_ shorthand you can not pass a keyword literal as a
_prop_ value. You can however pass any expression that evaluates to a keyword.

```clj 
 (cljsx/jsx>
  (<Foo :key :val >))

;; Expands to this, which is probably not what you wanted
(createElement Foo {:key true :val true})
```

But you can pass a keyword constructed with the `keyword` function, or any other
expressions which returns a keyword. Or even better _spread_ a
keyword-to-keyword map into the _props_:

```clj
(cljsx/jsx>
 (<Foo :foo (keyword "foo")
       :bar (identity :bar)
       :baz (do :baz) >)
 (<Foo ... {:foo :foo
            :bar :bar
            :baz :baz} >))

;; Both expand to
(createElement Foo {:foo :foo
                    :bar :bar
                    :baz :baz})
```

Note that since the component calls are delegated to the `createElement`
function, having components which expect keywords or other Clojure specific
types as _prop_ values only makes sense if the `createElement` function is a
Clojure function. For example `react/createElement` is expecting all its
arguments to be JavaScript objects and `cljsx` will do this conversion under the
hood. React will then call your component with the converted JavaScript values
and since the conversion is done with [clj->js] the keywords will become
strings. Read further about the JavaScript conversion in the next section.

### JavaScript Conversion

More likely than not, you will be using `cljsx` with JavaScript libraries e.g.
[React], which expect JavaScript values as their arguments. So if you wanted to
use `react/createElement` in ClojureScript directly you would need to pass the
_props_ as a plain JavaScript object:

```clj
;; You need to pass the props as JavaScript,
;; either by using the #js reader macro,
(react/createElement "h1" (clj->js #js{:style #js{:color "blue"}})
                      "Kind of blue")
;; or converted with clj->js
(react/createElement "h1" (clj->js {:style {:color "blue"}})
                      "Kind of blue")
```

If and *only if* the `jsx>` macro is used in ClojureScript environment, its
expansion will include JavaScript conversion code: 

```clj
(cljsx/jsx>
 (<div :className "foo"
       ... parent-props >
       "Foo"))

;; In a Clojure environment, the above snippet expands to:
(createElement Component
               (merge {:foo "bar"} parent-props)
               "Baz")

;; Whereas in ClojureScript, the expansion includes JS conversion code
(if (cljsx.core/js? createElement)
  ;; If createElement is a JavaScript function,
  (apply createElement
         ;; If the tag (in this case Component) is a JavaScript function,
         (if (cljsx.core/js? Component)
           ;; no conversion is needed, because the props are a JS object already.
           Component
           ;; Otherwise (if it is a Clojure function),
           ;; the tag function needs to be intercepted,
           (fn [props__38180__auto__]
             ;; so the props passed by the JS createElement function can be
             ;; converted to a Clojure map so they don't need to be manually
             ;; converted in each component implementation.
             (Component (js->clj props__38180__auto__
                                 :keywordize-keys true))))
         ;; The props and children both need to be converted to JS before they
         ;; are passed to the JS createElement
         (clj->js (merge {:foo "bar"} parent-props))
         (map clj->js ["Baz"]))
  ;; If createElement is a Clojure function, no conversion happens.
  (createElement Component
                 (merge {:foo "bar"} parent-props)
                 "Baz"))
```

If `createElement` resolves to a JavaScript function, there are two places where
the JavaScript conversion happens:

* First all props are converted to JavaScript objects recursively with [clj->js].
* Then all _reference tags_ which resolve to Clojure functions are intercepted
  so that the props passed to them by `createElement` can be converted back to
  Clojure maps with [js->clj] before they are passed to them. 

If the Clojure _tag_ functions were not intercepted, you would need to treat the
props in your components as JavaScript objects and could not destructure them:

```clj
;; Without automatic conversion your components would be rather verbose.
(defn MyButton [js-props]
  (jsx>
    ;; cljsx is expecting the props to be a Clojure map 
    (<button ... (js->clj js-props)
             :className (str "my-button " (.-class js-props)) >)))

;; With automatic conversion, your components should be nice and readable.
(defn MyButton [{:keys [class] :as clj-props}]
  (jsx>
    (<button ... clj-props 
             :className (str "my-button " class) >)))
```

#### JavaScript Function Detection Pitfalls

In ClojureScript, testing whether whether a function is a JavaScript or Clojure
function is not trivial and not always possible. `cljsx` has it's own
`cljsx.core/clj-fn?` macro for this purpose. The function returns `true` if it
is certain that the object passed to it is a Clojure function, `false` if it is
certain that it's a JavaScript function, and `nil` if it can't be determined.
Following are cases when `cljsx.core/clj-fn?` returns `nil` and thus
*favours JavaScript conversion*:

A function is reassigned to a let binding:

```clj
(defn f [])

(let [f' f ; Metadata is lost on reassignment.
      ;; But fresh assignmet still works.
      g (fn [])
      h #(identity %)]
  [(clj-fn? f')
   (clj-fn? g)
   (clj-fn? h)])
;; => [nil true true]
```

A function is passed as an argument to another function:

```clj
(defn f [])

(defn g [x]
  (clj-fn? x))

(g f)
;; => nil

(clj-fn? (identity f))
;; => nil
```

A function is returned from another function:

```clj
(defn f [])

(clj-fn? ((fn [] f)))
;; => nil
```

A function is defined or reassigned in the same `cljsx` macro where it is used:

```clj
(defn f [])
(def g f)

(jsx>
  (defn f' [])
  (def g' f'))

(jsx>
 (defn f'' [])
 (def g'' f'')

 [(clj-fn? f) ; true
  (clj-fn? g) ; true
  (clj-fn? f') ; true
  (clj-fn? g') ; true
  ;; Only definitions inside this very jsx> block are indeterminate
  (clj-fn? f'') ; nil
  (clj-fn? g'') ; nil
  ])
;; => [true true true true nil nil]
```

#### Taking JavaScript Conversion Under Control

There are various ways how you can ensure that your functions called by [React]
will always be either Clojure data structures of JavaScript objects.

##### Setting a Var's Tag Meta

The `cljsx.core/clj-fn?` is consulting the var's metadata `:tag`
value (among other things), which is lost in all of the cases when the function
returns `nil`. You can manually set the _tag_ and thus controll the conversion
of its arguments. If the tag value is `js`, or it is not set at all, it will be
treated as a JavaScript function. If the _tag_ is set to any value other than
`js` or `nil`, it will be treated as a Clojure function.

```clj
(defn f [])
(defn ^js f-js [])

(let [^cljs f-clj]
 [(clj-fn? f)
  (clj-fn? f-js)
  (clj-fn f-clj)])
;; => [true false true]
```

This approach however is rather verbose and it doesn't work in the case when a
function is defined inside the same jsx macro call where it is used.

##### Conversion Decorators

Since the JavaScript conversion favours JavaScript when the result of the
underlying `clj-fn?` function is `nil`, you should only be concerned about what
is passed to the components you defined and which you have under control.

There are number of utilities which you can use to make sure that your
components will always receive their props as a Clojure map or a JavaScript
object.

###### The `with-js-args` and `with-clj-args` Decorators

The `with-js-args` decorator takes a function and returns the same function for
which each argument will be passed trhough [clj->js]. Conversely the
`with-clj-args` function takes a function and returns the same function for
which all arguments will be passed trhough [js->clj]. 

```clj
(ns my-react-app.main
  (:require [cljsx.core :refer [react> with-clj-args]]
            [react :as react]
            [react-dom :as react-dom]))

(react>
 (defn Component [props]
   (js/console.log "Clojure props:" (map? props))
   (<h1> "Hello!"))

 (def ComponentWithCLJProps (with-clj-args Component))

 (react-dom/render
  (<div>
   (<Component :x >) ; Clojure props: false
   (<ComponentWithCLJProps :x >) ; Clojure props: true
 (js/document.querySelector "#mount-point"))))
```

##### Function Definition Macros

Wrapping every component you write in a decorator is tedious and annoying.
But fear not, `cljsx` comes with a bunch of macros which simplify definition of
functions with predictable data format.

###### The `fn-clj` And `defn-clj` Macros

The `fn-clj` and `defn-clj` macros have the same signature and work the same as
their `fn` and `defn` counterparts, except that the functions they define will
always receive JavaScript arguments converted to Clojure data structures using
[js->clj] with `:keywordize-keys true`.

Functions defined with `fn-clj` and `defn-clj` work predictably everywhere,
even in the same `jsx>` macro where they are defined.

```clj
((fn-clj [x y & z] [x y z])
 #js{"x" 11} #js{"x" 22} #js{"z" 33})
;; => [{:x 11} {:x 22} ({:z 33})]

;; You can define multiple arities
(def multi-arity-fn (fn-clj ([] "nullary")
                            ([x] ["unary" x])
                            ([x y] ["binary" x y])
                            ([x y & z] ["variadic" x y z])))

(multi-arity-fn)
;; => "nullary"

(multi-arity-fn #js{"x" 11})
;; => ["unary" {:x 11}]

(multi-arity-fn #js{"x" 11} #js{"x" 22})
;; => ["binary" {:x 11} {:x 22}]

(multi-arity-fn #js{"x" 11} #js{"x" 22} #js{"z" 33})
;; => [{:x 11} {:x 22} ({:z 33})]
```
These macros are especially usefull for defining callbacks for JavaScript
libraries like [React Router].

```clj
(ns shadow-cljs-example.main
  (:require ["react" :as react]
            ["react-dom" :as react-dom]
            ["react-router-dom" :as rr]
            [cljsx.core :refer [defn-clj fn-clj react>]]))

(react>
 ;; This function will always receive a Clojure map, eventhough
 ;; its defined in the same react> call where it is used.
 (defn-clj Link [{:keys [href active children]}]
   (if active
     children
     (<rr/Link :to href >
               children)))

 (react-dom/render
  (<rr/BrowserRouter>
   (<rr/Route>
    ;; We can destructure the React Router route props, which are passed as a
    ;; JavaScript object because we are using fn-clj and not just fn.
    (fn-clj [{{path :pathname} :location :as x}]
            (js/console.log (subs path 1))
            (<ul>
             (map #(<li :key % >
                        ;; React will pass the props to Link as a JS object,
                        ;; but since Link is defined with def-clj,
                        ;; it will recieve the props as a Clojure map.
                        (<Link :href %
                               :active (= % (subs path 1)) >
                               %))
                  ["foo" "bar" "baz"])))))
  (js/document.querySelector "#mount-point")))
```

###### The `component` and `defcomponent` Macros

The `component` and `defcomponent` macros are just a stripped-out version of the
`fn-clj` and `defn-clj` macros. The only difference is that they define a
_unary_ function with _props_ as the only argument, which can be a map, `nil` or
a plain JavaScript object, in which case, similarly as with `def-clj` the
functions will receive the argument converted using [js->clj] with
`:keywordize-keys true`.

The `component` and `defcomponent` signatures are the same as the signatures of
`fn-clj` and `defn-clj` (or [fn] and [defn]) respectively with these rather
cosmetic differences:

* Since both define unary functions there's no point in supporting multiple
  arities.
* For the same reason, we don't need the argument vector, but only the single
  argument symbol or a destructuring map.
* In `defcomponent`, metadata can only be set with the `^` reader macro in front
  of the component name and not with the metadata map before the argument vector
  as is possible in `defn-clj` and [defn].
* Both `fn-clj` and [fn] accept an optional _name_ parameter i.e. a symbol in
  of the _argument vector_ e.g `(fn my-name [x] fn-body)`. In the `component`
  macro, the _name_ must be a _qouted_ symbol so that it can be distinguished
  from the possible _prop_ symbol e.g. `(component 'my-name props fn-body)`.

```clj
(defcomponent MyComponent props
  (map? props))

;; The above statement is equivalent to this.
;; The only difference is the missing square brackets of the argument vector.
(defn-clj MyComponent' [props]
  (map? props))

(MyComponent {:a "A"}) ; true
;; Even if you call it with a JS object, it still receives a Clojure map
(MyComponent #js{:a "A"}) ; true

(react>
 ;; Even if you define the component in the same JSX macro call as it is used,
 ;; it will still predictably receive its props as a Clojure map.
 (defcomponent ^:before-load StyledButton
   "A button which adds the .my-button class to its class name list."
   ;; Destructuring stil works on the single argument.
   {:keys [className] :or {className ""} :as props}
   ;; Pre and post conditions work the same as in defn
   {:pre [(string? className)]}
   (<button ... props
            :className (str className " my-button") >))

 (react-dom-server/renderToStaticMarkup
  ;; The component will receive a Clojure map, eve though
  ;; React passes the props to it as a JavaScript object.
  (<StyledButton :className "foo" >
                 "Click me!")))
;; => "<button class=\"foo my-button\">Click me!</button>"
```

### Example

And finally an example of a simple application which uses [Material-UI] and
[React Router] written in JSX, `cljsx` and [reagent].

#### JavaScript With JSX

```jsx
import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Link, Route } from 'react-router-dom'
import { Avatar, Card, CardContent, CardHeader, IconButton } from '@material-ui/core'
import * as icons from '@material-ui/icons'

const IconButtonLink = props =>
  <IconButton {...props} component={Link} />

const App = () =>
  <BrowserRouter>
    <Route>
      {({location: {pathname: path}}) => {
        const activeIconName = path.slice(1)
        const ActiveIcon = icons[activeIconName]
        const title = ActiveIcon ? activeIconName : "Click the icons"
        return (
          <Card>
            <CardHeader
                title={title}
                avatar={ActiveIcon && <Avatar><ActiveIcon/></Avatar>}/>
            <CardContent>
              {Object.keys(icons)
                .filter(iconName => iconName.endsWith('TwoTone'))
                .map(iconName => {
                  const Icon = icons[iconName]
                  const color = iconName === activeIconName
                    ? "secondary"
                    : "default"
                  return (
                    <IconButtonLink key={iconName} to={iconName} color={color} >
                      <Icon/>
                    </IconButtonLink>
                  )
                })}
            </CardContent>
          </Card>
        )
      }}
    </Route>
  </BrowserRouter>

ReactDOM.render(
  <App/>,
  document.querySelector('#mount-point'),
)
```

#### ClojureScript With CLJSX

```clj
(ns shadow-cljs-example.main
  (:require ["react" :as react]
            ["react-dom" :as react-dom]
            ["react-router-dom" :as rr]
            ["@material-ui/core" :as mui]
            ["@material-ui/icons" :as mui-icons]
            [cljsx.core :refer [defcomponent fn-clj react>]]))

(react>
 (defcomponent IconButtonLink props
   (<mui/IconButton ... props
                    :component rr/Link >))

 (defcomponent App _
   (<rr/BrowserRouter>
    (<rr/Route>
     (fn-clj [{{path :pathname} :location}]
             (let [active-icon-name (subs path 1)
                   ^js ActiveIcon (aget mui-icons active-icon-name)]
               (<mui/Card>
                (<mui/CardHeader :title (if ActiveIcon
                                          active-icon-name
                                          "Click the icons")
                                 :avatar (when ActiveIcon
                                           (<mui/Avatar>
                                            (<ActiveIcon>))) >)
                (<mui/CardContent>
                 (for [icon-name (js/Object.keys mui-icons)
                       :when (.endsWith icon-name "TwoTone")
                       :let [^js Icon (aget mui-icons icon-name)]]
                   (<IconButtonLink :key icon-name
                                    :to icon-name
                                    :color (if (= icon-name active-icon-name)
                                             "secondary"
                                             "default") >
                                    (<Icon>))))))))))

 (react-dom/render
  (<App>)
  (js/document.querySelector "#mount-point")))
```

#### ClojureScript With Reagent

```clj
(ns shadow-cljs-example.main
  (:require ["react-router-dom" :as rr]
            ["@material-ui/core" :as mui]
            ["@material-ui/icons" :as mui-icons]
            [reagent.core :as r]))

(defn icon-button-link [props & children]
  (into [:> mui/IconButton (merge props {:component rr/Link})]
        children))

(defn app []
  [:> rr/BrowserRouter
   [:> rr/Route
    (fn [js-route-props]
      (let [path (-> js-route-props .-location .-pathname)
            active-icon-name (subs path 1)
            ActiveIcon (aget mui-icons active-icon-name)]
        (r/as-element
         [:> mui/Card
          [:> mui/CardHeader {:title (if ActiveIcon
                                       active-icon-name
                                       "Click the icons")
                              :avatar (r/as-element
                                       [:> mui/Avatar
                                        (when ActiveIcon
                                          [:> mui/Avatar
                                           [:> ActiveIcon]])])}]
          [:> mui/CardContent
           (for [icon-name (js/Object.keys icons)
                 :when (.endsWith icon-name "TwoTone")
                 :let [Icon (aget mui-icons icon-name)]]
             ^{:key icon-name}
             [icon-button-link {:to icon-name
                                :color (if (= icon-name active-icon-name)
                                         "secondary"
                                         "default")}
              [:> Icon]])]])))]])

(r/render
 [app]
 (js/document.querySelector "#mount-point"))
```

[fn]: https://clojuredocs.org/clojure.core/fn
[defn]: https://clojuredocs.org/clojure.core/defn
[clj->js]: https://cljs.github.io/api/cljs.core/clj-GTjs
[hiccups]: https://github.com/teropa/hiccups
[js->clj]: https://cljs.github.io/api/cljs.core/js-GTclj
[Figwheel]: https://figwheel.org/
[Inferno]: https://infernojs.org/
[JSX]: https://reactjs.org/docs/introducing-jsx.html
[Material-UI]: https://material-ui.com/
[merge]: https://clojuredocs.org/clojure.core/merge
[Nerv]: https://nerv.aotu.io/
[Preact]: https://preactjs.com/
[React]: https://reactjs.org/
[React Router]: https://reacttraining.com/react-router/
[_render props_]: https://reactjs.org/docs/render-props.html 
[reagent]: http://reagent-project.github.io/
[shadow-cljs]: http://shadow-cljs.org/
[Snabbdom]: https://github.com/snabbdom/snabbdom
[spec]: https://clojure.org/about/spec 
