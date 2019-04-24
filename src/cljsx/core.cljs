(ns cljsx.core)

(defn as-js [f]
  #(apply f (map clj->js %&)))

