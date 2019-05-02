(ns cljsx.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.walk :as w]
   [cljsx.tag :as tag]
   [cljsx.props :as props]))

(defn list->tag+props+children [[x & xs]]
  (let [str-tag (str x)]
    (if (tag/fragment? str-tag)
      (list '<> nil xs)
      (if-let [tag (tag/props? str-tag)]
        (let [[props
               children] (props/list->props+children xs)]
          (list
           tag
           (props/props->mergelist props)
           children))
        (if-let [tag (tag/simple? str-tag)]
          (list tag nil xs))))))

;; TODO: Shouldn't this should be used somewhere?
(defn- nil-when-empty [x]
  (if (empty? x)
    nil
    x))

(defn cljs-env? [&env]
  (let [ns (:ns &env)
        a (:js-globals &env)
        b (:js-aliases ns)
        c (:cljs.analyzer/constants ns)]
    (boolean (or a b c))))

(defn function-call? [l]
  (and (seq? l)
       (symbol? (first l))))

(defn visit-function-call [cljs-env jsx-name fragment-name l]
  (if-let [[tag proplist children] (list->tag+props+children l)]
    (let [jsx-symbol (symbol jsx-name)
          resolved-tag (if (= tag '<>)
                         (symbol fragment-name)
                         (tag/resolve-tag tag))
          proplist (if (< 1 (count proplist))
                            `(merge ~@proplist)
                            (first proplist))]
      ;; This is the (jsx tag props children) call:
      `(~jsx-symbol

        ;; Tag
        ~(if cljs-env
           `(let [resolved-tag# ~resolved-tag]
              (if (cljsx.core/js? ~resolved-tag)
                resolved-tag#
                (fn [props#]
                  ;; If we try to do (~resolved-tag ...),
                  ;; compilation fails, but it works with let binding.
                  (resolved-tag# (cljs.core/js->clj
                                  props#
                                  :keywordize-keys true)))))
           resolved-tag)

        ;; Props
        ~(if cljs-env
           `(cljs.core/clj->js ~proplist)
           proplist)

        ;; Children
        ~@children))
    l))

(defn visit-node [cljs-env jsx-name fragment-name node]
  (if (function-call? node)
    (visit-function-call cljs-env
                         jsx-name
                         fragment-name
                         node)
    node))

(defn wrap-in-do [[x & more :as l]]
  (if (empty? more)
    x
    `(do ~@l)))

(defmacro defjsx [macro-name jsx-name fragment-name]
  `(do
     (defn ~macro-name [&form# &env# & forms#]
       (->> forms#
            (w/postwalk #(visit-node (cljs-env? &env#)
                                     ~(str jsx-name)
                                     ~(str fragment-name)
                                     %))
            wrap-in-do))
     (. (var ~macro-name) (setMacro))
     (var ~macro-name)))

(defn fnjs* [fn-args]
  `(fn [& more#]
     (apply (fn ~@fn-args)
            (map #(cljs.core/js->clj % :keywordize-keys true)
                 more#))))

(defmacro fnjs [& fn-args]
  (fnjs* fn-args))

(defmacro defnjs [name' & defn-args]
  `(def ~name' ~(fnjs* defn-args)))

(defmacro js?
  "Checks whether `x` is a JavaScript value.
  Returns `nil` if it can't be determined if it's
  a JavaScript or Clojure value."
  [x]
  (let [locals (:locals &env)
        defs (get-in &env [:ns :defs])
        all (merge locals defs)
        entry (all x)]
    (if-not entry
      ;; If there's no entry, it's a JS object
      #_true
      "NO ENTRY"
      ;; Otherwise...
      (case (:tag entry)
        ;; If tag is 'js we can be sure it's a JS value.
        js true
        ;; If tag is 'any, we can't tell,
        ;; This is probably a value returned from a function.
        any nil
        ;; If tag is nil, CLJ values have some additional keys.
        ;; We can for example check on the presence of :meta
        nil (if (:meta entry)
              ;; If there is the :meta key, it is a CLJ value
              false
              ;; Otherwise we can't tell
              nil)
        ;; If tag is anything except for the above,
        ;; it's a CLJ value.
        false))))

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
  (component* 'cljsx.core/cljify-props more))

(defmacro component-js
  "Defines a function which takes an associative as its single argument.
  The argument will always be passed to the body converted
  to a map of keywrds to JS values.
  The first argument should be a symbol or a destructuring map,
  all the other arguments are the body of the function."
  [& more]
  (component* 'cljsx.core/jsify-props more))

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
        (cljsx.core/cljify-props props#)
        (cljsx.core/jsify-props props#)))))

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
  (defcomponent* 'cljsx.core/cljify-props more))

(defmacro defcomponent-js
  "Same as (def name (component-js param exprs))"
  [& more]
  (defcomponent* 'cljsx.core/jsify-props more))

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
        (cljsx.core/cljify-props props#)
        (cljsx.core/jsify-props props#)))))

(defjsx >>> jsx jsx-fragment)
(defjsx react>>> react/createElement react/Fragment)

