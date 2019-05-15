# `cljsx` A.K.A. JSX for Clojure and ClojureScript

`cljsx` tries to make it easy to use plain, unwrapped [react] (or any other virtual
dom) and all the related JavaScript libraries in ClojureScript by mimicking the
syntax of JSX. It's mainly meant to be used with the amazing [shadow-cljs] with
its effortless usage of plain NPM packages, but it works just as well with
[figwheel] and Clojure. 

If you think about it, JSX is just a _reader macro_, which merely adds syntactic
sugar to JavaScript. Surprisingly, in Clojure, the language of macros, there's
no such thing as JSX. Instead there are all sorts of [react] wrappers and wrappers
of [react] libraries. The most idiomatic way to express [react] DOM trees in Clojure 
seems to be the _hiccups_ format of nested vectors.

`cljsx` is trying to fill this gap with the `cljsx/jsx>` macro, which is the 
main workhorse it provides, apart from a bunch of additional macros all related
to simplifying conversion between Clojure and JavaScript. It's main goal is
expressiveness, readability and familiarity with [react] idioms. `cljsx` is not
tied to [react] in any way and it works with any other JSX compatible virtual dom
library like [inferno], [nerv], [preact] or [snabbdom]. 

## TL;DR

Consider this simple React application which uses [material-ui] and
[react-router].

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

The same application can be expressed in ClojureScript with the `cljsx/jsx>`
macro so, that it looks rather similar to the JS example above. Note that the
example is based on the [shadow-cljs] specific syntax for requiring NPM
namespaces.

```clj
(ns shadow-cljs-example.main
  (:require ["react" :refer [createElement]]
            ["react-dom" :as react-dom]
            ["react-router-dom" :as rr]
            ["@material-ui/core" :as mui]
            ["@material-ui/icons" :as icons]
            [cljsx.core :as cljsx]))

(cljsx/jsx>
 (cljsx/defcomponent IconButtonLink props
   (<mui/IconButton ... props :component rr/Link >))

 (defn App []
   (<rr/BrowserRouter>
    (<rr/Route>
     (fn [js-route-props]
       (let [path (-> js-route-props .-location .-pathname)
             active-icon-name (subs path 1)
             ^js ActiveIcon (aget icons active-icon-name)]
         (<mui/Card>
          (<mui/CardHeader :title (if ActiveIcon
                                    active-icon-name
                                    "Click the icons")
                           :avatar (when ActiveIcon
                                     (<mui/Avatar>
                                      (<ActiveIcon>))) >)
          (<mui/CardContent>
           (for [icon-name (js/Object.keys icons)
                 :when (.endsWith icon-name "TwoTone")
                 :let [^js Icon (aget icons icon-name)]]
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

And this is how it looks like expressed with [reagent]. Notice the ubiquitous
conversion from and to [react] with `:>` and `r/as-element` respectively.
The extension of the `mui/IconButton` to `icon-button-link` must be hard to
swallow for someone comming from the JavaScript world.

```clj
(ns shadow-cljs-example.main
  (:require ["react" :refer [createElement]]
            ["react-dom" :as react-dom]
            ["react-router-dom" :as rr]
            ["@material-ui/core" :as mui]
            ["@material-ui/icons" :as icons]
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
            ActiveIcon (aget icons active-icon-name)]
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
                 :let [Icon (aget icons icon-name)]]
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