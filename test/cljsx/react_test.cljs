(ns cljsx.react-test
  (:require
   [smidjen.core :refer-macros [facts fact]]
   [cljsx :as cljsx]
   ["react" :as react]
   ["react-dom/server" :as react-dom]
   ["./components" :refer [JSComponent]]))

(def ce react/createElement)
(def rts react-dom/renderToString)

(defn js-or-clj [props]
  (if (map? props)
    "clj"
    "js"))

(defn DefnComponent [props]
  (js-or-clj props))

(defn ^js DefnComponentJS [props]
  (js-or-clj props))

(cljsx/defn' DefnjsComponent [props]
  (js-or-clj props))

(cljsx/defn' ^js DefnjsComponentJS [props]
  (js-or-clj props))

(fact "No props"
      (js->clj (cljsx/rsx> (<div>)))
      => (js->clj (ce "div" nil))

      (js->clj (cljsx/rsx> (<h1> "child")))
      => (js->clj (ce "h1" nil "child"))

      (js->clj (cljsx/rsx> (<h1> "child1"
                                 "child2")))
      => (js->clj (ce "h1" nil "child1" "child2")))

(fact "Props"
      (js->clj
       (cljsx/rsx>
        (<h1 :className "foo" >
             "child1"
             "child2")))
      => (js->clj
          (ce "h1"
              (clj->js {:className "foo"})
              "child1"
              "child2"))

      (js->clj
       (cljsx/rsx>
        (<h1 :className "foo"
             :style {:color "red"} >
             "child1"
             "child2")))
      => (js->clj
          (ce "h1"
              (clj->js {:className "foo"
                        :style     {:color "red"}})
              "child1"
              "child2"))

      (js->clj
       (cljsx/rsx>
        (<h1 :className "foo"
             :style {:color "red"} >)))
      => (js->clj
          (ce "h1"
              (clj->js {:className "foo"
                        :style     {:color "red"}}))))

(def spread-props-1 {:className "bing"})
(def spread-props-2 {:title "Have you got a bandage?"
                     :key   123})

(fact "Spread"
      (js->clj
       (cljsx/rsx>
        (<h1 :className "foo"
             ... {:className "bar"} >)))
      => (js->clj
          (ce "h1"
              (clj->js {:className "bar"})))

      (js->clj
       (cljsx/rsx>
        (<h1 :className "foo"
             ... {:className "bar"}
             :className "baz" >)))
      => (js->clj
          (ce "h1"
              (clj->js {:className "baz"})))

      (js->clj
       (cljsx/rsx>
        (<h1 :className "foo"
             ... {:className "bar"}
             :className "baz"
             ... spread-props-1 >)))
      => (js->clj
          (ce "h1"
              (clj->js {:className "bing"})))

      (js->clj
       (cljsx/rsx>
        (<h1 :className "foo"
             ... {:className "bar"}
             :className "baz"
             ... spread-props-2 >)))
      => (js->clj
          (ce "h1"
              (clj->js (merge
                        {:className "baz"}
                        spread-props-2)))))
(fact "Nesting"
      (js->clj
       (cljsx/rsx>
        (<div>
         (<h1> "Title")
         (<h2> "Subtitle")
         (<p> "Paragraph")
         "Text")))
      => (js->clj
          (ce "div" nil
              (ce "h1" nil "Title")
              (ce "h2" nil "Subtitle")
              (ce "p" nil "Paragraph")
              "Text"))

      (js->clj
       (cljsx/rsx>
        (<div> "Parent"
               (<div> "Child"
                      (<div> "Grand child")))))
      => (js->clj
          (ce "div" nil "Parent"
              (ce "div" nil "Child"
                  (ce "div" nil "Grand child")))))

(facts "JS Detection"
       (rts
        (cljsx/rsx>
         (<JSComponent>)))
       => "js"

       (rts
        (cljsx/rsx>
         (<DefnComponent>)))
       => "clj"

       (rts
        (cljsx/rsx>
         (<DefnComponentJS>)))
       => "js"

       (rts
        (cljsx/rsx>
         (<DefnjsComponent>)))
       => "clj"

       (rts
        (cljsx/rsx>
         (<DefnjsComponentJS>)))
       => "clj"

       (let [Component (fn [p] (js-or-clj p))]
         (rts
          (cljsx/rsx>
           (<Component>))))
       => "clj"

       (let [Component js-or-clj]
         (rts
          (cljsx/rsx>
           (<Component>))))
       => "clj"

       (let [Component #(js-or-clj %)]
         (rts
          (cljsx/rsx>
           (<Component>))))
       => "clj"

       (let [^js Component js-or-clj]
         (rts
          (cljsx/rsx>
           (<Component>))))
       => "js"

       (let [Component JSComponent]
         (rts
          (cljsx/rsx>
           (<Component>))))
       => "js"

       ;; Functions lose their tag meta when they are passed as arguments.
       (let [Component (identity JSComponent)]
         (rts
          (cljsx/rsx>
           (<Component>))))
       => "clj"

       (let [^js Component (identity JSComponent)]
         (rts
          (cljsx/rsx>
           (<Component>))))
       => "js"

       (let [Component ((fn [c] c) JSComponent)]
         (rts
          (cljsx/rsx>
           (<Component>))))
       => "clj"

       (let [^js Component ((fn [c] c) JSComponent)]
         (rts
          (cljsx/rsx>
           (<Component>))))
       => "js"

       (let [Component ((fn [] JSComponent))]
         (rts
          (cljsx/rsx>
           (<Component>))))
       => "js"

       (let [Component (identity DefnComponent)]
         (rts
          (cljsx/rsx>
           (<Component>))))
       => "clj"

       (let [f (fn [Component]
                 (rts
                  (cljsx/rsx>
                   (<Component>))))]
         (f DefnComponent)
         => "clj"

         ;; JS info lost
         (f JSComponent)
         => "clj"

         ;; The only thing we can do is force argument conversion to JS.
         (f (cljsx/with-js-args JSComponent))
         => "js"

         ;; Metadata is lost on functions defined in the same JSX block where it is used.
         (cljsx/rsx>
          (defn DefnInsideJSX [props]
            (js-or-clj props))
          (rts (<DefnInsideJSX>)))
         => "js"

         ;; Tagging has no effect here
         (cljsx/rsx>
          (defn ^function DefnInsideJSX2 [props]
            (js-or-clj props))
          (rts (<DefnInsideJSX2>)))
         => "js"

         ;; For this we have defn'
         (cljsx/rsx>
          (cljsx/defn' DefnInsideJSX3 [props]
            (js-or-clj props))
          (rts (<DefnInsideJSX3>)))
         => "clj"

         (cljsx/rsx>
          (let [Component (fn [props]
                            (js-or-clj props))]
            (rts (<Component>))))
         => "clj"

         ;; Metadata is lost if def is inside JSX
         (cljsx/rsx>
          (def DefInsideJSX (fn [props]
                              (js-or-clj props)))
          (rts (<DefInsideJSX>)))
         => "js"

         ;; For this we have fn'
         (cljsx/rsx>
          (def DefInsideJSX2 (cljsx/fn' [props]
                                        (js-or-clj props)))
          (rts (<DefInsideJSX2>)))
         => "clj"))

(facts "Other forms"
       (fact "Shorthand function"
             (rts
              (cljsx/rsx>
               (<ul>
                (map #(<li :key % > %)
                     ["a" "b" "c"]))))
             => (rts
                 (ce "ul" nil
                     (map #(ce "li" (clj->js {:key %}) %)
                          ["a" "b" "c"]))))

       (fact "Escaping of >"
             (let [Component (fn [{:keys [comparator a b]}]
                               (if (comparator a b)
                                 "a comes before b"
                                 "b comes before a"))]
               (rts
                (cljsx/rsx>
                 (<Component :a 5
                             :b 3
                             :comparator < >)))
               => "b comes before a"

               (rts
                (cljsx/rsx>
                 (<Component :a 5
                             :b 3
                             :comparator @#'> >)))
               => "a comes before b")))


