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

(list->tag&props&children '(foo bar baz))
(list->tag&props&children '(<foo bar baz))
(list->tag&props&children '(<foo> bar baz))
(list->tag&props&children '(<foo :a 123 :b 456 > bar baz))
(list->tag&props&children '(<foo :a "A" :b "B" ... x :c "C" :d "D" > bar baz))
(list->tag&props&children '(<foo :a "A" :b "B" ... x :c "C" :d "D"))

;; TODO: The recursion is not implemented yet
(defn walk [form]
  (cond
    (list? form)
    (if-let [[tag props-mergelist children] (list->tag&mergelist&children form)]
      `(*jsx*
        ~(resolve-tag tag)
        ~(if (< 1 (count props-mergelist))
           `(merge ~@props-mergelist)
           (first props-mergelist))
        ~@children)
      form)

    (vector? form)
    "vector"

    (set? form)
    "set"

    (map? form)
    "map"

    :else
    "something else"))

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
