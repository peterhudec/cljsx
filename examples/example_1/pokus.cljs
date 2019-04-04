(ns example-1.pokus
  (:require-macros [example-1.pokus]))

(defn convert* [x]
  (clj->js x))

