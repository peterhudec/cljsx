# CLJSX A.K.A. JSX for Clojure and ClojureScript

CLJSX is just a macro which tries to bring the syntax and idioms of JSX to 
Clojure and ClojureScript.

Consider this JavaScript code:

```jsx
import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Link, Route } from 'react-router-dom'
import { Paper, Tab, Tabs } from '@material-ui/core'

const RoutedTab = props =>
  <Tab {...props} component={Link} />

const RoutedTabPanel = ({items, ...props}) =>
  <Route>
    {({location: {pathname: path}}) => {
      const activePath = path.slice(1)
      const activeContent = items[activePath]
      return (
        <Paper>
          <Tabs {...props}
              value={activePath in items && activePath}>
            {Object.keys(items).map(x =>
              <RoutedTab key={x} label={x} value={x} to={x}
                  disabled={activePath === x} />)}
          </Tabs>
          <div style={{padding: '1rem'}}>
            {activeContent}
          </div>
        </Paper>
      )}}
  </Route>

const App = () =>
  <BrowserRouter>
    <RoutedTabPanel
        centered
        indicatorColor="primary"
        items={{
          foo:
            <div style={{color: 'tomato'}}>
              <h1>Foo</h1>
              Lorem ipsum
            </div>,
          bar:
            <>
              <h2>Bar</h2>
              <ul>
                {['one', 'two', 'three'].map(x =>
                  <li key={x}>
                    {x}
                  </li>)}
              </ul>
            </>,
          baz: <h3>Baz</h3>,
        }} />
  </BrowserRouter>

ReactDOM.render(
  <App/>,
  document.getElementById('root')
)
```

```clj
(ns shadow-cljs-example.main
  (:require ["react" :as react]
            ["react-dom" :as react-dom]
            ["react-router-dom" :as rr]
            ["@material-ui/core" :as m]
            [cljsx.core :as cljsx]))

(cljsx/react>>>
 (cljsx/defcomponent RoutedTab props
   (<m/Tab ... props
           :component rr/Link
           >))

 (cljsx/defcomponent+js RoutedTabPanelX props {:keys [items]}
   (<rr/Route>
    (cljsx/fn-clj [{{path :pathname} :location}]
                  (let [active-path (subs path 1)
                        active-content (aget items active-path)]
                    (<m/Paper>
                     (<m/Tabs ... props
                              :value (if (aget items active-path)
                                       active-path
                                       false) >
                              (map #(<RoutedTab :key % :label % :value % :to %
                                                :disabled (= % active-path) >)
                                   (js/Object.keys items)))
                     (<div :style {:padding "1rem"} >
                           active-content))))))


 (cljsx/defcomponent+js RoutedTabPanel
   {clj-items :items :as clj-props}
   {js-items :items}
   (<rr/Route>
    (cljsx/fn-clj [{{path :pathname} :location}]
                  (let [active-path (subs path 1)
                        active-content (aget js-items active-path)]
                    (<m/Paper>
                     (<m/Tabs ... clj-props
                              :value (if active-content active-path false) >
                              (map #(<RoutedTab :key % :label % :value % :to %
                                                :disabled (= % active-path) >)
                                   (keys clj-items)))
                     (<div :style {:padding "1rem"} >
                           active-content))))))

 (defn App []
   (<rr/BrowserRouter>
    (<RoutedTabPanel :centered
                     :indicatorColor "primary"
                     :items {:foo (<div :style {:color "tomato"} >
                                        (<h1> "Foo")
                                        "Lorem ipsum")
                             :bar (<>
                                   (<h2> "Bar")
                                   (<ul>
                                    (map #(<li :key % > %)
                                         ["one" "two" "three"])))
                             :baz (<h3> "Baz")} >)))

 (react-dom/render
  (<App>)
  (js/document.querySelector "#mount-point"))))
```




