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
  (cljsx/rsx>
   (<pre ... props >
    (str props))))

#_(react-dom/render
 (react/createElement "h1" nil
                      "Pokus"
                      (react/createElement CljComponent* (clj->js {:aaa "AAA"})
                                           "CHILDREN"))
 (js/document.querySelector "#mount"))

(cljsx/rsx>
 (defn YetAnotherComponent [props]
   (js/console.log "YetAnotherComponent props" props)
   "Yet another component")

 (defn MyOtherComponent [props]
   (js/console.log "MyOtherComponent props" props)
   "My other component"
   ;; If a component is used inside the same JSX macro
   ;; as it is declared, it will have no record in the &env
   ;; and will be detected as JS function
   (<YetAnotherComponent :foo "Bar" >)))

(cljsx/rsx>
 ;; This is detected as a JS function,
 ;; because it's not in the &env
 (defn MyComponent [props]
   ;; Here props are a JS object
   (js/console.log "MyComponent props" props)
   (<div :style {:border "4px dashed green"} >
         "My component"))

 (react-dom/render
  [
   (<h1 :className "foo"
        :key 1 >
        "CHILDREN"
        "Child 2")
   (<MyComponent :foo "bar"
                 >)
   (<MyOtherComponent :foo "bar"
                      >)
   (<YetAnotherComponent :foo "bar"
                         >)
   (<js/JSComponent1 :key 2
                     :foo "FOO"
                     :bar "BAR" >
                     "CHILDREN")
   (<CljComponent* :key 3
                   :foo "FOO"
                   :bar "BAR" >
                   "CHILDREN")]
  (js/document.querySelector "#mount")))

(defn jsx* [tag props & children]
  (println "===================")
  ;(println tag)
  (js/console.log "JSX:::" props)
  ;; (js/console.log "JSX>>>" (js->clj props))
  {:tag tag
   :props props
   :children children})

(cljsx/jsx*>
 (<intrinsic :iiii "iiiii" >)
 (<CljComponent* :cccc "cccc" >)
 (<react/Fragment :rrrr "rrrrr" >)
 (<js/JSComponent1 :jjjj "JJJJ" >))

(def ReassignedFragment react/Fragment)

(def Fnc (fn [props] 123))

(def Identity identity)

#_(cljsx/jsx*>
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
  (cljsx/jsx*>
   ;; Has &env, but tag is `nil` (so it thinks it's CLJ)
   (<Arg>)))

;(f (fn []))
;(f react/Fragment)


