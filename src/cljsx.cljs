(ns cljsx
  (:require-macros [cljsx])
  (:require [cljsx.conversion]))

(defn with-js-args [f]
  #(apply f (map clj->js %&)))

(defn with-clj-args [f]
  (fn [& args]
    (apply f (map #(js->clj % :keywordize-keys true) args))))

