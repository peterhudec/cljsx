(ns cljsx.core
  (:require-macros [cljsx.core]))

(defn with-js-args [f]
  #(apply f (map clj->js %&)))

(defn with-clj-args [f]
  (fn [& args]
    (apply f (map #(js->clj % :keywordize-keys true) args))))

(defn- js-obj?
  [x]
  (identical? (type x)
              js/Object))

(defn- js-obj->kw-map
  "Converts a JS object to a map of keywords
  to the original JS values."
  [obj]
  (reduce (fn [a k]
            (assoc a
                   (keyword k)
                   (aget obj k)))
          {}
          (js/Object.keys obj)))

(defn- map-vals->js
  "Converts all values of a map to JS."
  [m]
  (reduce (fn [a [k v]]
            (assoc a k (clj->js v)))
          {}
          m))

(defn jsify-props
  "Takes a map or a JS object and converts it to spreadable JS props,
  i.e. a CLJ map of keywords to JS converted values.

  ```
  (jsify-props {:foo {} :bar []})
  => {:foo #js {} :bar #js []}

  (jsify-props #js {:foo {} :bar []})
  => {:foo #js {} :bar #js []}
  ```"
  [p]
  (if (js-obj? p)
    (js-obj->kw-map p)
    (map-vals->js p)))

(defn cljify-props
  "The same as `js->clj` except that it won't do the conversion
  only if it is a JS object"
  [p]
  (if (js-obj? p)
    (js->clj p :keywordize-keys true)
    p))

(defn with-js-props [f]
  (fn [props]
    (f (jsify-props props))))

