(ns pokus
  (:require [clojure.pprint :refer [pprint]]
            [pokus-2 :as p2]
            [cljsx :as cljsx]
            [cljsx.core :as cc]))

(defn- js?* [x &env]
  (let [locals (:locals &env)
        defs (get-in &env [:ns :defs])
        all (merge locals defs)
        tag (get-in all [x :tag])]
    (or (= tag 'js)
        (nil? tag))))

(defmacro js? [x]
  (println "==================")
  (println x)
  (pprint (:js-globals &env))
  (js?* x &env))

(defmacro clj? [x]
  (not (js?* x &env)))

(defn cljs-env?* [&env]
  (println (str "cljs-env?*: " (boolean &env)))
  (let [ns (:ns &env)
        a (:js-globals &env)
        b (:js-aliases ns)
        c (:cljs.analyzer/constants ns)]
    (boolean (or a b c))))

(defmacro cljs-env? []
  (println (str "cljs-env?: " (boolean &env)))
  (p2/m 123)
  (cljs-env?* &env))

(defmacro m [name' form]
  `["name:" ~(str name') "form:" ~form])

(defmacro defm [name']
  (let [x &env]
    `(defmacro ~name' [form#]
       `[">>>" ~form# ~~x ~(boolean ~&env) ~~&env])))

(defm x)

(macroexpand '(defm x))

(macroexpand '(defmacro foo [form] ["body" form]));; => (do
;;     (clojure.core/defn foo ([&form &env form] ["body" form]))
;;     (. #'foo (setMacro))
;;     #'foo)

(do
  (defn mmm [&form &env form]
    ["body" form (count &env)])
  (. #'mmm (setMacro)))

(defn inner-macro* [env form]
  `(println (str "env: " ~(count env) " form: " ~form)))

(defmacro defmmm [name']
  `(do
    (defn ~name' [&form# &env# form#]
      (inner-macro* &env# form#))
    (. (var ~name') (setMacro))))

(defmmm xxx)

(xxx 123)

`(foo bar
      `(baz bing))
;; => (pokus/foo
;;     pokus/bar
;;     (clojure.core/seq
;;      (clojure.core/concat
;;       (clojure.core/list 'pokus/baz)
;;       (clojure.core/list 'pokus/bing))))

`(foo bar
      `(baz bing))
