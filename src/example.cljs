(ns example
  (:require
   [cljsx :as cljsx]
   ["react" :as react]
   ["react-dom" :as react-dom]))

(defn foo [] "foo")

(cljsx/rsx>
 (react-dom/render
  (<div :className "foo"
        :style {:background "yellow"}>
   (<js/JSComponent1 :a "A"
                     :b "B"
                     :map {:x "X" :y "Y"}>
                     "JS Child 1"
                     "JS Child 2"))
  (js/document.querySelector "#mount")))

(defn jsx [tag props & children]
  (js/console.log "jsx" props)
  {:tag tag
   :props props
   :children children})

(cljsx/jsx>
 (<foo :a "A" :b "B" > "Child"))
