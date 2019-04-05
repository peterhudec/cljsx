(ns cljsx.conversion)

(defn js? [var']
  (let [metadata (meta var')]
    (or (= (:ns metadata) 'js)
        (:js metadata))))

(def not-js? (complement js?))

(defn clj? [var']
  (and (meta var')
       (not-js? var')))

(defn resolve-jsx* [jsx jsx-var tag-var]
  (if (js? jsx-var)
    (fn [tag props & children]
      (apply jsx
             (if (clj? tag-var)
               #(tag props)
               tag)
             (clj->js props)
             children))
    jsx))

