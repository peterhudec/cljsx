(ns figwheel-example.main
  (:require [react :as react]
            [react-dom :as react-dom]
            [cljsx.core :refer [react> defcomponent]]))

(def common-props {:style {:color "purple"
                           :background "lavenderblush"
                           :borderRadius "0.2em"
                           :padding "0.4em"}})

(react>
 (defcomponent Button {:keys [title children] :as props}
   (<button ... common-props
            ... props
            :onClick #(js/alert title) >
            children))

 (defn Header []
   (<h1 ... common-props >
        "Hello CLJSX!"))

 (defcomponent ButtonList {:keys [children]}
   (<ul>
    (map #(<li :key % >
               (<Button :title %
                        :disabled (= % "Bar") > %))
         children)))

 (defn Content []
   (<>
    (<p ... common-props >
        "Lorem ipsum dolor sit amet")
    (<ButtonList> "Foo"
                  "Bar"
                  "Baz")))

 (defn Footer []
   (<h2 ... common-props >
        "Enjoy!"))

 (defcomponent App _
   (<div :style {:border "3px dashed purple"
                 :padding "2rem"
                 :borderRadius "1rem"
                 :maxWidth "400px"
                 :margin "2rem auto"} >
         (<Header>)
         (<Content>)
         (<Footer>)))

 (react-dom/render
  (<App>)
  (js/document.querySelector "#mount-point")))
