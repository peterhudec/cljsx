(ns example
  (:require
   [cljsx :as cljsx]
   ["react" :as react]
   ["react-dom" :as react-dom]))

(defn CljComponent* [props]
  (js/console.log "CljComponent props" props)
  "Clj Component")

(defn ForwardProps* [props]
  (cljsx/rsx>
   (<pre ... props >
    (str props))))

(cljsx/rsx>
 (react-dom/render
  (<div>
   (<h1 :className "foo">
        "Hello CLJSX!")
   (<ForwardProps* :className "foo"
                   :style {:background "yellow"}
                   :onClick #(js/console.log "clicked")>)
   (<ul>
    (<li> "Foo")
    (<>
     (<li> "Bar")
     (<li> "Baz"))
    (<li> "Bing"))
   (<js/JSComponent1 :a "A"
                     :b "B"
                     :map {:x "X" :y "Y"}>
                     "JS Child 1"
                     "JS Child 2")
   (<CljComponent* :a "A"
                   :b "B"
                   :map {:x "X" :y "Y"}
                   :list '(foo bar baz)
                   :set #{11 22 33}>
                   "JS Child 1"
                   "JS Child 2"))
  (js/document.querySelector "#mount")))

(defn jsx* [tag props & children]
  (js/console.log "jsx*" props)
  {:tag tag
   :props props
   :children children})

(cljsx/jsx*>
 (<foo :a "A" :b "B" > "Child"))

