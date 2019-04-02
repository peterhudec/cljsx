(ns example-1.main
  (:require
   [cljsx :as cljsx :refer-macros [jsx> rsx> js-function?]]
   ["react" :as react]
   ["react-dom" :as react-dom]))

(defn cljs-func {:foo "bar"} [x]
  [x x])

(def ^{:foo "bar"} wtf ["WTF"])
(def wtf-meta (meta wtf))

(js/console.log "wtf" wtf)
(js/console.log "META wtf" wtf-meta)
(js/console.log "cljs-func" cljs-func)
(js/console.log "react-dom/render" react-dom/render)
(js/console.log "META cljs-func" (meta #'cljs-func))
(js/console.log "META react-dom/render" (meta #'react-dom/render))

(js/console.log "(js-function? cljs-func)"
                (js-function? cljs-func))
(js/console.log "(js-function? react-dom/render)"
                (js-function? react-dom/render))

(= 'js
   (js-function? react-dom/render))

(rsx>
 (defn MyComponent [props]
   (js/console.log "MyComponent props" props)
   (<h2> "My component"))

 (react-dom/render
  (<div> (<h1 :className "foo"
              :style {:background "gold"} >
              "Hello CLJSX")
         (<p> "Lorem ipsum dolor sit amet")
         (<ul> (<li> "Foo")
               (<> (<li> "Bar")
                   (<li> "Baz"))
               (<li> "Bing"))
         (<MyComponent :foo "bar" >
                       "my child"
                       "my other child"))
  (js/document.querySelector "#mount")))
