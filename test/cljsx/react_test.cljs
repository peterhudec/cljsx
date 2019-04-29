(ns cljsx.react-test
  (:require
   [smidjen.core :refer-macros [facts fact]]
   [cljsx :as cljsx]
   ["react" :as react]
   ["react-dom/server" :as react-dom]
   ["./components" :refer [JSComponent]]))

(def e react/createElement)
(def r react-dom/renderToString)

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
      (js->clj (cljsx/react>>> (<div>)))
      => (js->clj (e "div" nil))

      (js->clj (cljsx/react>>> (<h1> "child")))
      => (js->clj (e "h1" nil "child"))

      (js->clj (cljsx/react>>> (<h1> "child1"
                                 "child2")))
      => (js->clj (e "h1" nil "child1" "child2")))

(fact "Props"
      (js->clj
       (cljsx/react>>>
        (<h1 :className "foo" >
             "child1"
             "child2")))
      => (js->clj
          (e "h1"
              (clj->js {:className "foo"})
              "child1"
              "child2"))

      (js->clj
       (cljsx/react>>>
        (<h1 :className "foo"
             :style {:color "red"} >
             "child1"
             "child2")))
      => (js->clj
          (e "h1"
              (clj->js {:className "foo"
                        :style     {:color "red"}})
              "child1"
              "child2"))

      (js->clj
       (cljsx/react>>>
        (<h1 :className "foo"
             :style {:color "red"} >)))
      => (js->clj
          (e "h1"
              (clj->js {:className "foo"
                        :style     {:color "red"}}))))

(def spread-props-1 {:className "bing"})
(def spread-props-2 {:title "Have you got a bandage?"
                     :key   123})

(fact "Spread"
      (js->clj
       (cljsx/react>>>
        (<h1 :className "foo"
             ... {:className "bar"} >)))
      => (js->clj
          (e "h1"
              (clj->js {:className "bar"})))

      (js->clj
       (cljsx/react>>>
        (<h1 :className "foo"
             ... {:className "bar"}
             :className "baz" >)))
      => (js->clj
          (e "h1"
              (clj->js {:className "baz"})))

      (js->clj
       (cljsx/react>>>
        (<h1 :className "foo"
             ... {:className "bar"}
             :className "baz"
             ... spread-props-1 >)))
      => (js->clj
          (e "h1"
              (clj->js {:className "bing"})))

      (js->clj
       (cljsx/react>>>
        (<h1 :className "foo"
             ... {:className "bar"}
             :className "baz"
             ... spread-props-2 >)))
      => (js->clj
          (e "h1"
              (clj->js (merge
                        {:className "baz"}
                        spread-props-2)))))
(fact "Nesting"
      (js->clj
       (cljsx/react>>>
        (<div>
         (<h1> "Title")
         (<h2> "Subtitle")
         (<p> "Paragraph")
         "Text")))
      => (js->clj
          (e "div" nil
              (e "h1" nil "Title")
              (e "h2" nil "Subtitle")
              (e "p" nil "Paragraph")
              "Text"))

      (js->clj
       (cljsx/react>>>
        (<div> "Parent"
               (<div> "Child"
                      (<div> "Grand child")))))
      => (js->clj
          (e "div" nil "Parent"
              (e "div" nil "Child"
                  (e "div" nil "Grand child")))))

(facts "JS Detection"
       (r
        (cljsx/react>>>
         (<JSComponent>)))
       => "js"

       (r
        (cljsx/react>>>
         (<DefnComponent>)))
       => "clj"

       (r
        (cljsx/react>>>
         (<DefnComponentJS>)))
       => "js"

       (r
        (cljsx/react>>>
         (<DefnjsComponent>)))
       => "clj"

       (r
        (cljsx/react>>>
         (<DefnjsComponentJS>)))
       => "clj"

       (let [Component (fn [p] (js-or-clj p))]
         (r
          (cljsx/react>>>
           (<Component>))))
       => "clj"

       (let [Component js-or-clj]
         (r
          (cljsx/react>>>
           (<Component>))))
       => "clj"

       (let [Component #(js-or-clj %)]
         (r
          (cljsx/react>>>
           (<Component>))))
       => "clj"

       (let [^js Component js-or-clj]
         (r
          (cljsx/react>>>
           (<Component>))))
       => "js"

       (let [Component JSComponent]
         (r
          (cljsx/react>>>
           (<Component>))))
       => "js"

       ;; Functions lose their tag meta when they are passed as arguments.
       (let [Component (identity JSComponent)]
         (r
          (cljsx/react>>>
           (<Component>))))
       => "clj"

       (let [^js Component (identity JSComponent)]
         (r
          (cljsx/react>>>
           (<Component>))))
       => "js"

       (let [Component ((fn [c] c) JSComponent)]
         (r
          (cljsx/react>>>
           (<Component>))))
       => "clj"

       (let [^js Component ((fn [c] c) JSComponent)]
         (r
          (cljsx/react>>>
           (<Component>))))
       => "js"

       (let [Component ((fn [] JSComponent))]
         (r
          (cljsx/react>>>
           (<Component>))))
       => "js"

       (let [Component (identity DefnComponent)]
         (r
          (cljsx/react>>>
           (<Component>))))
       => "clj"

       (let [f (fn [Component]
                 (r
                  (cljsx/react>>>
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
         (cljsx/react>>>
          (defn DefnInsideJSX [props]
            (js-or-clj props))
          (r (<DefnInsideJSX>)))
         => "js"

         ;; Tagging has no effect here
         (cljsx/react>>>
          (defn ^function DefnInsideJSX2 [props]
            (js-or-clj props))
          (r (<DefnInsideJSX2>)))
         => "js"

         ;; For this we have defn'
         (cljsx/react>>>
          (cljsx/defn' DefnInsideJSX3 [props]
            (js-or-clj props))
          (r (<DefnInsideJSX3>)))
         => "clj"

         (cljsx/react>>>
          (let [Component (fn [props]
                            (js-or-clj props))]
            (r (<Component>))))
         => "clj"

         ;; Metadata is lost if def is inside JSX
         (cljsx/react>>>
          (def DefInsideJSX (fn [props]
                              (js-or-clj props)))
          (r (<DefInsideJSX>)))
         => "js"

         ;; For this we have fn'
         (cljsx/react>>>
          (def DefInsideJSX2 (cljsx/fn' [props]
                                        (js-or-clj props)))
          (r (<DefInsideJSX2>)))
         => "clj"))

(facts "Other forms"
       (fact "Shorthand function"
             (r
              (cljsx/react>>>
               (<ul>
                (map #(<li :key % > %)
                     ["a" "b" "c"]))))
             => (r
                 (e "ul" nil
                     (map #(e "li" (clj->js {:key %}) %)
                          ["a" "b" "c"]))))

       (fact "Escaping of >"
             (let [Component (fn [{:keys [comparator a b]}]
                               (if (comparator a b)
                                 "a comes before b"
                                 "b comes before a"))]
               (r
                (cljsx/react>>>
                 (<Component :a 5
                             :b 3
                             :comparator < >)))
               => "b comes before a"

               (r
                (cljsx/react>>>
                 (<Component :a 5
                             :b 3
                             :comparator @#'> >)))
               => "a comes before b")))

(defn clj-props? [p]
  (some vector? (vals p)))

(defn js-props? [p]
  (some array? (vals p)))

(defn js-or-clj-props [p]
  (cond
    (js-props? p) "js"
    (clj-props? p) "clj"
    :else "neither"))

(def dummy-clj-props {:a [] :b [] :c []})
(def dummy-js-props (clj->js dummy-clj-props))

(cljsx/defcomponent AlwaysCLJProps
  props
  (js-or-clj-props props))

(cljsx/defcomponent-js AlwaysJSProps
  props
  (js-or-clj-props props))

(cljsx/defcomponent+js AlwaysCLJ+JSProps
  props
  js-props
  (str (js-or-clj-props props)
       "+"
       (js-or-clj-props js-props)))

(facts "Component macros"
       (fact "Anonymous"
             (fact "Always CLJ props"
                   (let [Component (cljsx/component
                                    props
                                    (js-or-clj-props props))]
                     (Component dummy-clj-props)
                     => "clj"

                     (Component dummy-js-props)
                     => "clj"))

             (fact "Always spreadable JS props"
                   (let [Component (cljsx/component-js
                                    props
                                    (js-or-clj-props props))]
                     (Component dummy-clj-props)
                     => "js"

                     (Component dummy-js-props)
                     => "js"))

             (fact "Both CLJ and spreadable JS props"
                   (let [Component (cljsx/component+js
                                    props
                                    js-props
                                    (str (js-or-clj-props props)
                                         "+"
                                         (js-or-clj-props js-props)))]
                     (Component dummy-clj-props)
                     => "clj+js"

                     (Component dummy-js-props)
                     => "clj+js"))

             )
       (fact "Defs"
             (AlwaysCLJProps dummy-clj-props)
             => "clj"

             (AlwaysCLJProps dummy-js-props)
             => "clj"

             (AlwaysJSProps dummy-clj-props)
             => "js"

             (AlwaysJSProps dummy-js-props)
             => "js"

             (AlwaysCLJ+JSProps dummy-js-props)
             => "clj+js"

             (AlwaysCLJ+JSProps dummy-js-props)
             => "clj+js"

             ))

(fact "Element props"
      (let [Component (fn [js-props]
                        (e "ul" nil
                           (e "li" nil
                               (.-a js-props))
                           (e "li" nil
                               (.-b js-props))))]
        (r
         (e Component #js {:a (e "span" nil
                                   "AAA")
                            :b (e "span" nil
                                   "BBB")})))
      => (cljsx/react>>>
          (let [Component (cljsx/component-js
                           {:keys [a b]}
                           (<ul>
                            (<li> a)
                            (<li> b)))]
            (r
             (<Component :a (<span> "AAA")
                         :b (<span> "BBB")
                         >)))))

