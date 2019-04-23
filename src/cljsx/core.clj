(ns cljsx.core
  (:require
   [clojure.walk :as w]
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
  (if-let [[tag proplist children] (list->tag&props&children l)]
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



