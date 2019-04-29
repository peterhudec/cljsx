(ns example
  (:require-macros
   [pokus]
   [cljsx.core])
  (:require
   [cljsx.core :as cc]
   ;[cljsx.props :as props]
   ;[cljsx.tag :as tag]
   ;[cljsx.conversion :as conversion]
   [pokus :as pokus]
   [cljsx :as cljsx]
   ["react" :as react]
   ["react-dom" :as react-dom]))

(defn CljComponent* [props]
  (js/console.log "----------------------")
  (js/console.log "CljComponent props" props)
  "Clj Component")

(defn ForwardProps* [props]
  (cljsx/react>>>
   (<pre ... props >
    (str props))))

(defn MultiArity
  ([] "Multi arity")
  ([x] "Multi arity x")
  ([x y] "Multi arity x y"))

(cljsx/defnjs JSMultiArity
  ([] "JS Multi arity")
  ([x] "JS Multi arity x")
  ([x y] "JS Multi arity x y"))


#_(react-dom/render
 (react/createElement "h1" nil
                      "Pokus"
                      (react/createElement CljComponent* (clj->js {:aaa "AAA"})
                                           "CHILDREN"))
 (js/document.querySelector "#mount"))

(cljsx/react>>>
 (defn YetAnotherComponent [{:keys [foo] :as props}]
   (js/console.log "===================")
   (let [props (js->clj props :keywordize-keys true)]
     (js/console.log "YetAnotherComponent props" props)
     (js/console.log "YetAnotherComponent :foo" foo))
   "Yet another component")

 (defn MyOtherComponent [props]
   (js/console.log "MyOtherComponent props" props)
   "My other component"
   ;; If a component is used inside the same JSX macro
   ;; as it is declared, it will have no record in the &env
   ;; and will be detected as JS function
   (<YetAnotherComponent :foo "Bar" >)))

(cljsx/react>>>
 ;; This is detected as a JS function,
 ;; because it's not in the &env
 (defn MyComponent [props]
   ;; Here props are a JS object
   (js/console.log "MyComponent props" props)
   (<div :style {:border "4px dashed green"} >
         "My component"))

 (cljsx/defnjs JSPropsCompoonent [props]
   (js/console.log "JSPropsComponent" props)
   (<pre> "JS Props"))


 (cljsx/defnjs JSMultiArityInside
   ([] "JS Multi arity inside")
   ([x]
    (js/console.log "JSMultiArityInside x" x)
    "JS Multi arity inside x")
   ([x y]
    (js/console.log "JSMultiArityInside x y" x y)
    "JS Multi arity inside x y")
   ([x y z]
    (js/console.log "JSMultiArityInside x y z" x y z)
    "JS Multi arity inside x y z"))

 (react-dom/render
  [
   (<h1 :className "foo"
        :key 1 >
        "CHILDREN"
        "Child 2")
   (<MultiArity :foo "bar"> "Child")
   (<JSMultiArity :foo "bar"> "Child")
   (<JSMultiArityInside :foo "bar" > "Child")
   (<JSMultiArityInside> "Child")
   (<MyComponent :foo "bar"
                 >)
   (<MyOtherComponent :foo "bar"
                      >)
   (<YetAnotherComponent :foo "bar"
                         >)
   (<JSPropsCompoonent :aaa "AAA"
                       :bbb "BBB" >
                       "Child"
                       "Child")
   (<js/JSComponent1 :key 2
                     :foo "FOO"
                     :bar "BAR" >
                     "CHILDREN")
   (<CljComponent* :key 3
                   :foo "FOO"
                   :bar "BAR" >
                   "CHILDREN")]
  (js/document.querySelector "#mount")))

(defn jsx [tag props & children]
  (println "===================")
  ;(println tag)
  (js/console.log "JSX:::" props)
  ;; (js/console.log ">>>>>" (js->clj props))
  {:tag tag
   :props props
   :children children})

(cljsx/>>>
 (<intrinsic :iiii "iiiii" >)
 (<CljComponent* :cccc "cccc" >)
 (<react/Fragment :rrrr "rrrrr" >)
 (<js/JSComponent1 :jjjj "JJJJ" >))

(def ReassignedFragment react/Fragment)

(def Fnc (fn [props] 123))

(def Identity identity)

#_(cljsx/>>>
 ;; (<foo :a "A" :b "B" > "Child")
 (<intrinsic>)
 (<react/Fragment>) ;; No &env entry
 (<CljComponent*>) ;; Has &env entry, but without tag
 (<Fnc>)
 (<Identity>)
 (<ReassignedFragment>)
 (<js/JSComponent1>)

 (let [LetReactFragment react/Fragment
       LetClj CljComponent*
       LetFn (fn [props] "heh")
       LetJSIdentity (identity react/Fragment)
       LetCljIdentity (identity CljComponent*)]
   ;; Has &env entry and tag is `js`
   (<LetReactFragment>)

   ;; Has &env entry and tag is `nil`
   (<LetClj>)

   ;; Has &env entry and tag is `function`
   (<LetFn>)

   ;; Have &env entry and tag is `any`
   (<LetJSIdentity>)
   (<LetCljIdentity>)
   ))

(defn f [Arg]
  (cljsx/>>>
   ;; Has &env, but tag is `nil` (so it thinks it's CLJ)
   (<Arg>)))

;(f (fn []))
;(f react/Fragment)

#_((cljsx.core/component [x y & more]
                       (js/console.log "WTF")
                       #_(js/console.log "x" x)
                       #_(js/console.log "y" y)
                       #_(js/console.log "more" more)
                       #_[x y])
 "XXX" "YYY" "ZZZ" "AAA")

(js/console.log ">>>>"
                ((cljsx/fnjs [x y z]
                              [x y z])
                 #js {:x "X"} "Y" "Z"))

(cljsx/defnjs foo [x y]
  (js/console.log "FOO" x y)
  [x y])

(js/console.log "foo" (foo #js {:a "A"} {:b "B"}))

(js/console.log "===============")
(js/console.log ">" > (> 2 1) (> 1 2))
(js/console.log "@#'>" @#'> (@#'> 2 1) (@#'> 1 2))

(js/console.log "###################")
(defn pokus [jsx]
  (js/console.log "jsx is:" jsx)
  (cljsx/>>>
   (<div> "child")))

(js/console.log "jsx" (pokus jsx))
(js/console.log "react/createElement" (pokus (identity react/createElement)))

(defn docstring-fn
  "I'm docstring"
  [x]
  nil)

(js/console.log "docstring-fn" (meta #'docstring-fn))


(cljsx/defnjs docstring-fn-js
  "I'm docstring JS"
  [x]
  nil)

(js/console.log "docstring-fn-js" (meta #'docstring-fn-js))
