(ns cljsx.core)

(defn convert-props* [metadata props]
  (if (or (= (:ns metadata) 'js)
          (:js metadata))
    (clj->js props)
    props))
