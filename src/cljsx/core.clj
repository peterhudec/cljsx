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

(defn nil-when-empty [x]
  (if (empty? x)
    nil
    x))

(defn walk-factory [jsx-name*]
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
             `(~(symbol jsx-name*)
               ~(resolve-tag tag)
               ~(if (< 1 (count props-mergelist))
                  `(merge ~@(map walk-props
                                 props-mergelist))
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

           :else form))
       (walk-all [& forms]
         (let [results (map walk forms)]
           (if (= (count results) 1)
             (first results)
             `(do ~@results))))]
    walk-all))

(defmacro defjsx [name* jsx-name]
  `(defmacro ~name* [& forms#]
     (apply (walk-factory ~(str jsx-name)) forms#)))

(defjsx jsx> *jsx*)
(macroexpand-1 '(jsx> (a
                       (<b> b b)
                       c)))

(macroexpand-1
 '(jsx>
   (<a> a a)
   (<b> b b)
   (<c> c c)))

(defjsx rsx> react/createElement)
(macroexpand-1 '(rsx> (a
                       (<b> b b)
                       c)))

(defn *jsx* [tag props & children]
  {:tag tag
   :props props
   :children (into [] children)})

(def bar "BAR")
(def baz (jsx> (<baz> "BAZ")))
(defn Foo [] "Foooo")

(def spread-me {:spread "me"})

(jsx>
 (<foo> bar
        (<aaa :a "AAA" :aa >
              "Ahoj")
        baz
        (<bbb :b (<ccc>)
              :spread "not me"
              ... spread-me
              :foo (<Foo>)>
              "BBB")));; => {:tag "foo",
