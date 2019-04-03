(ns example-1.main
  (:require
   [cljsx :as cljsx :refer-macros [jsx> rsx> js-function?]]
   ["react" :as react]
   ["react-dom" :as react-dom]))

(js/console.log "CCCCCC" js/JSComponent1)

(rsx>
 (defn MyComponent [props]
   (js/console.log "MyComponent props" props)
   (<h2> "My component"))

 (defn ForwardProps [props]
   (<h3 ...(js->clj props) >
        "FW Props"))

 (react-dom/render
  (<div> (<h1 :className "foo"
              :style {:background "gold"} >
              "Hello CLJSX")
         (<p> "Lorem ipsum dolor sit amet")
         (<ul> (<li> "Foo")
               (<> (<li> "Bar")
                   (<li> "Baz"))
               (<li> "Bing"))
         (<ForwardProps :style {:background "yellow"}>
                        "Forward Props")
         (<js/JSComponent1 :a "A"
                           :b "B"
                           :map {:x "X" :y "Y"}>
                           "JS Child 1"
                           "JS Child 2")
         (<MyComponent :foo "bar" >
                       "my child"
                       "my other child"))
  (js/document.querySelector "#mount")))
