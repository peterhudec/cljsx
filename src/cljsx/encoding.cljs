(ns cljsx.encoding)

(defn encode-props* [metadata props]
  (if (or (= (:ns metadata) 'js)
          (:js metadata))
    (clj->js props)
    props))
