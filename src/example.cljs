(ns example
  (:require
   [cljsx :as cljsx]
   ["react" :as react]
   ["react-dom" :as react-dom]))

(defn CljComponent [props]
  (js/console.log "CljComponent props" props)
  "Clj Component")

(cljsx/rsx>
 (defn ForwardProps [props]
   (js/console.log "wtfx" props)
   (<pre ... props >
         "Forward props"))

 (react-dom/render
  (<div>
   (<h1> "Hello CLJSX")
   (<ForwardProps :className "foo"
                  :style {:background "gold"}>)
   (<js/JSComponent1 :a "A"
                     :b "B"
                     :map {:x "X" :y "Y"}>
                     "JS Child 1"
                     "JS Child 2")
   (<CljComponent :a "A"
                  :b "B"
                  :map {:x "X" :y "Y"}
                  :list '(foo bar baz)
                  :set #{11 22 33}>
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
