(ns cljsx.core
  (:require
   [cljsx.tag :as tag]
   [cljsx.props :as props]
   [cljsx.conversion]))

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

#_(defmacro clj? [x]
  (let [locals (:locals &env)
        defs (get-in &env [:ns :defs])
        all (merge locals defs)]
    (boolean (all x))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- js?* [x &env]
  (let [locals (:locals &env)
        defs (get-in &env [:ns :defs])
        all (merge locals defs)
        tag (get-in all [x :tag])]
    (or (= tag 'js)
        (nil? tag))
    (with-out-str (clojure.pprint/pprint (keys all )))))

(defmacro js? [x]
  #_(js?* x &env)
  "WTF")

(defmacro clj? [x]
  (not (js?* x &env)))

(defn cljs-env?* [&env]
  (let [ns (:ns &env)
        a (:js-globals &env)
        b (:js-aliases ns)
        c (:cljs.analyzer/constants ns)]
    (boolean (or a b c))))

(defmacro cljs-env? []
  (cljs-env?* &env))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn walk-factory [&env' jsx-name jsx-fragment]
  (letfn
      [(walk-props [props]
         (if (map? props)
           (nil-when-empty (into {} (map walk props)))
           props))
       (walk [form]
         (cond
           (seq? form)
           (if-let [
                    [tag
                     props-mergelist
                     children] (list->tag&props&children
                                form)]
             `(;; JSX
               ~(let [is-cljs (cljs-env?* &env')]
                  (if is-cljs
                    `(fn [tag# props# & children#]
                       (do
                         (js/console.log "=============")
                         (js/console.log "tag:" ~tag)
                         (js/console.log "::::")
                         (js/console.log
                          (if (string? ~(tag/resolve-tag tag))
                            "INTRINSIC"
                            (cljsx.conversion/js?
                             ~(tag/resolve-tag tag)))))
                       (apply ~(symbol jsx-name)
                              tag#
                              (if (cljsx.core/js? ~(symbol jsx-name))
                                (cljs.core/clj->js props#)
                                props#)
                              children#))
                    (symbol jsx-name)))

               ;; Tag
               ~(if (= tag '<>)
                  (symbol jsx-fragment)
                  (tag/resolve-tag tag))

               ;; Props
               #_~(let [tag' (if (= tag '<>)
                             (symbol jsx-fragment)
                             (tag/resolve-tag tag))]
                  `{:tag ~tag'
                    :clj? (cljsx.core/clj? ~tag')})
               ~(if (< 1 (count props-mergelist))
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

(defmacro defjsx-backup [name* jsx-name jsx-fragment]
  (println (str "defjsx OUT: " (boolean &env)))
  (clojure.pprint/pprint &env)
  `(defmacro ~name* [& forms#]
     (println (str "defjsx IN: " (boolean ~&env)))
     (apply
      (walk-factory ~&env
                    ~(str jsx-name)
                    ~(str jsx-fragment))
      forms#)))

(defmacro js? [form]
  `(let [meta# (-> ~form
                   var
                   meta)]
     (or (= (:ns meta#) (symbol "js"))
         ;; This is only for testing and convenience
         (:js meta#))))

(defn inner-macro* [name' env form]
  `(println (str "name: " ~name' " env: " ~(count env) " form: " ~form)))

(defmacro defm [name']
  `(do
     (defn ~name' [&form# &env# form#]
       (inner-macro* ~(str name') &env# form#))
     (. (var ~name') (setMacro))))

(defm aaa)

(defn defjsx* [macro-name is-cljs jsx-name jsx-fragment-name forms]
  `[~macro-name
    ~is-cljs
    ~jsx-name
    ~jsx-fragment-name
    ~(str forms)])

(defmacro defjsx [macro-name jsx-name jsx-fragment-name]
  `(do
     (defn ~macro-name [&form# &env# & forms#]
       (apply
        (walk-factory &env#
                      ~(str jsx-name)
                      ~(str jsx-fragment-name))
        forms#)
       #_(defjsx*
         ~(str macro-name)
         (cljs-env?* &env#)
         ~(str jsx-name)
         ~(str jsx-fragment-name)
         forms#))
     (. (var ~macro-name) (setMacro))
     (var ~macro-name)))

(defjsx pokus> pokus-jsx pokus-fragment)
