# CLJSX A.K.A. JSX for Clojure and ClojureScript

CLJSX tries to make it easy to use plain, unwrapped React (or any other virtual
dom) and all the related libraries in ClojureScript by mimicking the syntax of
JSX.

If you think about it, JSX is just a _reader macro_, which merely adds syntactic
sugar to JavaScript. Surprisingly, in Clojure, the language of macros, there's
no such thing as JSX. The most idiomatic way to express React DOM trees in
Clojure is the _hiccups_ format of nested vectors.

CLJSX is trying to fill this gap with the `cljsx/jsx>` macro, which is the main
workhorse it provides, apart from some additional macros all simplifying
conversion between Clojure and JavaScript.

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

It can be expressed in ClojureScript with the `cljsx/jsx>` macro so,
that it looks rather similar to the JS example above.

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




