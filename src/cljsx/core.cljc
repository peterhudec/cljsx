(ns cljsx.core
  (:require
   [cljsx.tag :as tag]
   [cljsx.props :as props]
   [cljsx.js-conversion :as js]))

(defn list->tag&props&children [[x & xs]]
  (let [str-tag (str x)]
    (if (tag/fragment? str-tag)
      (list '<> nil xs)
      (if-let [tag (tag/props? str-tag)]
        (let [[props
               children] (props/list->props&children xs)]
          (list
           tag
           (props/props->mergelist props)
           children))
        (if-let [tag (tag/simple? str-tag)]
          (list tag nil xs))))))

(defn- nil-when-empty [x]
  (if (empty? x)
    nil
    x))


(defn walk-factory [jsx-name jsx-fragment]
  (letfn
      [(walk-props [props]
         (if (map? props)
           (nil-when-empty (into {} (map walk props)))
           props))
       (walk [form]
         (cond
           (seq? form)
           (if-let [[tag
                     props-mergelist
                     children] (list->tag&props&children
                                form)]
             `(;; JSX function
               ~(symbol jsx-name)

               ;; Tag
               ~(if (= tag '<>)
                  (symbol jsx-fragment)
                  (tag/resolve-tag tag)
                  #_(js/$props->clj-when-js
                   (tag/resolve-tag tag))
                  #_(let [tag' (tag/resolve-tag tag)]
                    (js/$props->clj-when-js tag')))

               ;; Props
               #?(:clj {:clj "CLJ"}
                  :cljs {:cljs "CLJS"})
               #_(~'clj->js
                {:a "aaa"
                  :b "BBB"
                  :js ~(js/$js? (symbol jsx-name))})
               #_~(js/$clj->js-when-js
                 (symbol jsx-name)
                 {:wtfx "WTFX"})
               #_~(js/$clj->js-when-js
                 (symbol jsx-name)
                 (if (< 1 (count props-mergelist))
                   `(merge ~@(map walk-props
                                  props-mergelist))
                   (walk-props (first props-mergelist))))
               #_~(if (< 1 (count props-mergelist))
                 `(merge ~@(map walk-props
                                props-mergelist))
                 (walk-props (first props-mergelist)))

               ;; Children
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

           :else form))
       (walk-all [& forms]
         (let [results (map walk forms)]
           (if (= (count results) 1)
             (first results)
             `(do ~@results))))]
    walk-all))

(defmacro defjsx [name* jsx-name jsx-fragment]
  `(defmacro ~name* [& forms#]
     (apply
      (walk-factory ~(str jsx-name)
                    ~(str jsx-fragment))
      forms#)))
