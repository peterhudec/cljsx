(ns cljsx.encoding)

(defn js? [var']
  (let [metadata (meta var')]
    (or (= (:ns metadata) 'js)
        (:js metadata))))

(defn encode-props* [var' props]
  (if (js? var')
    (clj->js props)
    props))

