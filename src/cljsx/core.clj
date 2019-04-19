(ns cljsx.core
  (:require
   [cljsx.tag :as tag]
   [cljsx.props :as props]
   [cljsx.conversion]))

(defn list->tag&props&children [[x & xs]]
  (let [str-tag (str x)]
    (if (tag/fragment? str-tag)
      (list '<> nil xs)
      (if-let [tag (tag/props? str-tag)]
        (let [[props
               children] (props/list->props&children xs)]
          (list
           tag
           (props/props->mergelist props)
           children))
        (if-let [tag (tag/simple? str-tag)]
          (list tag nil xs))))))

(defn- nil-when-empty [x]
  (if (empty? x)
    nil
    x))

(defn walk-factory [cljs-env jsx-name jsx-fragment]
  (letfn
   [(walk-props [props]
      (if (map? props)
        (nil-when-empty (into {} (map walk props)))
        props))
    (walk [form]
      (cond
        (seq? form)
        (if-let [[tag
                  props-mergelist
                  children] (list->tag&props&children
                             form)]
          (let [jsx-symbol (symbol jsx-name)
                resolved-tag (if (= tag '<>)
                               (symbol jsx-fragment)
                               (tag/resolve-tag tag))
                props-mergelist (if (< 1 (count props-mergelist))
                                  `(merge ~@(map walk-props
                                                 props-mergelist))
                                  (walk-props (first props-mergelist)))]
            `(;; JSX
              ~jsx-symbol

              ;; Tag
              ~(if cljs-env
                 `(let [resolved-tag# ~resolved-tag]
                   (if (cljsx.conversion/js? ~resolved-tag)
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
                 `(cljs.core/clj->js ~props-mergelist)
                 props-mergelist)

              ;; Children
              ~@(map walk children)))
          (map walk form))

        (vector? form)
        (into [] (map walk form))

        (set? form)
        (into #{} (map walk form))

        (map? form)
        (into {} (map (fn [[k v]]
                        [(walk k) (walk v)])
                      form))

        :else form))
    (walk-all [& forms]
      (let [results (map walk forms)]
        (if (= (count results) 1)
          (first results)
          `(do ~@results))))]
    walk-all))

(defn cljs-env? [&env]
  (let [ns (:ns &env)
        a (:js-globals &env)
        b (:js-aliases ns)
        c (:cljs.analyzer/constants ns)]
    (boolean (or a b c))))

(defmacro defjsx [macro-name jsx-name jsx-fragment-name]
  `(do
     (defn ~macro-name [&form# &env# & forms#]
       (apply
        (walk-factory (cljs-env? &env#)
                      ~(str jsx-name)
                      ~(str jsx-fragment-name))
        forms#))
     (. (var ~macro-name) (setMacro))
     (var ~macro-name)))

