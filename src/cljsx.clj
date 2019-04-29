(ns cljsx
  (:require
   [cljsx.core :as core]))

(defn jsify-props [props]
  (reduce (fn [a [k v]]
            (assoc a k [:js v]))
          {}
          props))

(defn cljify-props [props]
  (reduce (fn [a [k v]]
            (assoc a k [:clj v]))
          {}
          props))

(defn- component* [interceptor-symbol [props & decls]]
  (let [func `(fn [~props] ~@decls)]
    `(fn [props#]
       (~func (~interceptor-symbol props#)))))

(defmacro component
  "Defines a function which takes an associative as its single argument.
  The argument will always be passed to the body converted
  to a map of keywords to Clojure values.
  The first argument should be a symbol or a destructuring map,
  all the other arguments are the body of the function."
  [& more]
  (component* 'cljsx/cljify-props more))

(defmacro component-js
  "Defines a function which takes an associative as its single argument.
  The argument will always be passed to the body converted
  to a map of keywrds to JS values.
  The first argument should be a symbol or a destructuring map,
  all the other arguments are the body of the function."
  [& more]
  (component* 'cljsx/jsify-props more))

(defmacro component+js
  "Defines a function which takes an associative as its single argument.
  The argument will be allways passed to the body both as a map of keywords
  to Clojure values and as map of keywords to JS values.
  The first argument of the macro is the Clojure map,
  the second argument is the JS map.
  Both should be a symbol or a destructuring map.
  all the other arguments are the body of the function."
  [clj-props js-props & decls]
  (let [func `(fn [~clj-props ~js-props] ~@decls)]
    `(fn [props#]
       (~func
        (cljsx/cljify-props props#)
        (cljsx/jsify-props props#)))))

(defn- defcomponent* [interceptor-symbol [name & [frst & more :as decls]]]
  (let [doc (when (string? frst) frst)
        docmeta (when doc {:doc doc})
        [props & decls'] (if doc more decls)
        func `(fn [~props] ~@decls')
        name-with-meta (with-meta name (merge (meta name) docmeta))]
    `(defn ~name-with-meta [props#]
       (~func (~interceptor-symbol props#)))))

(defmacro defcomponent
  "Same as (def name (component param exprs))"
  [& more]
  (defcomponent* 'cljsx/cljify-props more))

(defmacro defcomponent-js
  "Same as (def name (component-js param exprs))"
  [& more]
  (defcomponent* 'cljsx/jsify-props more))

(defmacro defcomponent+js
  "Same as (def name (component+js param exprs))"
  [name & [frst & more :as decls]]
  (let [doc (when (string? frst) frst)
        docmeta (when doc {:doc doc})
        [clj-props js-props & decls'] (if doc more decls)
        func `(fn [~clj-props ~js-props] ~@decls')
        name-with-meta (with-meta name (merge (meta name) docmeta))]
    `(defn ~name-with-meta [props#]
       (~func
        (cljsx/cljify-props props#)
        (cljsx/jsify-props props#)))))

(core/defjsx >>> jsx jsx-fragment)
(core/defjsx react>>> react/createElement react/Fragment)

(comment
  (>>>
   (<div>
    (<h1> "Hello World!")
    (<p> "Lorem ipsum dolor sit amet.")))
  (react>>>
   (<div>
    (<h1> "Hello World!")
    (<p> "Lorem ipsum dolor sit amet."))))

