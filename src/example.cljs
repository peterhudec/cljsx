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
  (js/console.log "CljComponent props" props)
  "Clj Component")

(defn ForwardProps* [props]
  (cljsx/rsx>
   (<pre ... props >
    (str props))))

#_(cljsx/rsx>
 (react-dom/render
  [
   (<h1 :className "foo"
        :key 1 >
        "Fooo")
   (<js/JSComponent1 :key 2
                     :foo "FOO"
                     :bar "BAR" >)
   (<CljComponent* :key 3
                   :foo "FOO"
                   :bar "BAR" >)]
  #_(<div>
   (<h1 :className "foo">
        "Hello CLJSX!")
   #_(<ForwardProps* :className "foo"
                   :style {:background "yellow"}
                   :onClick #(js/console.log "clicked")>)
   (<ul>
    (<li> "Foo")
    (<>
     (<li> "Bar")
     (<li> "Baz"))
    (<li> "Bing"))
   (<pre>
    #_(if (pokus/clj? react/createElement)
        "JO"
        "Nein")
    )
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
  ;(js/console.log "jsx*" props)
  {:tag tag
   :props props
   :children children})

(js/console.clear)

(cljsx/jsx*>
 #_(<foo :a "A" :b "B" > "Child")
 #_(<react/Fragment>) ;; No &env entry
 #_(<CljComponent*>) ;; Has &env entry, but without tag
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

(f (fn []))

(comment
  (js/console.log "WTF???" (pokus/cljs-env?))
  (js/console.log "======================")
  (js/console.log "defm x" (pokus/x 12345))
  (js/console.log "mmm" (pokus/mmm 12345))
  (js/console.log "xxx" (pokus/xxx (+ 1000 1)))
  (js/console.log "aaa" (cc/aaa (+ 1000 1)))
  (js/console.log "bbb" (cljsx/bbb (+ 1000 1)))

  (js/console.log "POKUS"
                  (cc/pokus> (+ 2000 2)
                             (str "foo" "+" "bar"))))
