(ns cljsx
  (:require
   [cljsx.core :as core]))

(defmacro fnjs
  "Same as `fn` but arguments will be passed converted by `js->clj`
  with `:keywordize-keys true`."
  [& fn-args]
  `(fn [& args#]
    (apply (fn ~@fn-args)
           (map #(cljs.core/js->clj % :keywordize-keys true)
                args#))))

(defmacro defnjs
  "Same as `defn` but arguments will be passed converted by `js->clj`
  with `:keywordize-keys true`."
  [name & [frst & more :as decls]]
  (let [doc (when (string? frst)
              frst)
        docmeta (when doc
                  {:doc doc})
        func (list* `fn (if doc
                          more
                          decls))]
    `(defn ~(with-meta name (merge (meta name) docmeta))
       [& args#]
       (apply ~func
              (map #(cljs.core/js->clj % :keywordize-keys true)
                   args#)))))

(core/defjsx jsx> jsx jsx-fragment)
(core/defjsx rsx> react/createElement react/Fragment)
