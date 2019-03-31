(ns cljsx.core
  (:require
   [cljsx.tag :refer [props-tag?
                      resolve-tag
                      simple-tag?]]
   [cljsx.props :refer [list->props&children
                        props->mergelist]]))

(defn list->tag&props&children [[x & xs]]
  (if-let [tag (-> x str props-tag?)]
    (let [[props children] (list->props&children xs)]
      (list tag (props->mergelist props) children))
    (if-let [tag (-> x str simple-tag?)]
      (list tag nil xs))))

(into {} (map (fn [x] x) {:a "A" :b "B"}))

(defn nil-when-empty [x]
  (if (empty? x)
    nil
    x))

(defn walk-props [props]
  (if (map? props)
    (nil-when-empty
     (into {}
           (map (fn [x] (walk x))
                props)))
    props))

(map identity [])

(defn walk [form]
  (cond
    (list? form)
    (if-let [[tag
              props-mergelist
              children] (list->tag&props&children form)]
      `(~(symbol "*jsx*")
        ~(resolve-tag tag)
        ~(if (< 1 (count props-mergelist))
           `(merge ~@(map walk-props props-mergelist))
           (walk-props (first props-mergelist)))
        ~@(map walk children))
      (map walk form))

    (vector? form)
    (into [] (map walk form))

    (set? form)
    (into #{} (map walk form))

    (map? form)
    (into {} (map (fn [[k v]]
                    [(walk k) (walk v)])
                  form))

    :else
    form))

(walk '(foo bar))

(walk '(<foo> bar))

(walk
 '(<foo :a "A" :b "B" >
        bar
        baz))

(walk
 '(<foo :a "A" :b "B"
        ... xxx
        :c
        :d "D" >
        bar
        baz))

(walk
 '(<foo.Bar :a "A" :b "B"
        ... xxx
        :c
        :d "D" >
        bar
        baz))

(walk
 '(<foo.Bar ... xxx
            ... yyy >
            bar
            baz))
