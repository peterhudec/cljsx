(ns cljsx.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.core.specs.alpha :as cs]
   [clojure.walk :as w]
   [cljsx.specs :as specs]))

(defn- normalize-tag [sym]
  (->> sym
       str
       (re-matches #"<(.*?)>?")
       second))

(defn- conformed-props->mergelist [props]
  (->> props
       (reduce (fn [[mergelist pmap] prop']
                 (let [[k v :as prop] (s/unform ::specs/prop prop')
                       is-spread (= k '...)
                       is-true-attr (= (count prop) 1)]
                   (if is-spread
                     [(if (empty? pmap)
                        (conj mergelist v)
                        (conj mergelist pmap v)) {}]
                     [mergelist (assoc pmap k (if is-true-attr
                                                true
                                                v))])))
               [[] {}])
       ;; Flatten the reduced structure
       ((fn [[mergelist pmap]]
          (if (empty? pmap)
            mergelist
            (conj mergelist pmap))))
       ;; Remove empty maps
       (filter #(not (and (map? %) (empty? %))))
       ;; Return nil, if the mergelist is empty
       (#(when-not (empty? %) %))))

(defn visit-node [cljs-env jsx-name fragment-name node]
  (let [node' (s/conform ::specs/jsx node)]
    (if (= node' ::s/invalid)
      node
      (let [jsx-symbol (symbol jsx-name)
            fragment-symbol (symbol fragment-name)
            [jsx-type {:keys [tag props children]}] node'
            [tag-type dirty-tag] tag
            prop-mergelist (conformed-props->mergelist props)
            props (if (< 1 (count prop-mergelist))
                    `(merge ~@prop-mergelist)
                    (first prop-mergelist))
            unformed-children (s/unform ::specs/forms children)
            resolved-tag (case tag-type
                           :fragment-tag (symbol fragment-name)
                           :intrinsic-tag (normalize-tag dirty-tag)
                           :reference-tag (-> dirty-tag
                                              normalize-tag
                                              symbol))
            intercepted-tag `(let [resolved-tag# ~resolved-tag]
                               (if (cljsx.core/clj-fn? ~resolved-tag)
                                 (fn [props#]
                                   ;; If we try to do (~resolved-tag ...),
                                   ;; compilation fails,
                                   ;; but it works with let binding.
                                   (resolved-tag# (cljs.core/js->clj
                                                   props#
                                                   :keywordize-keys true)))
                                 resolved-tag#))]
        (if cljs-env
          `(if (cljsx.core/clj-fn? ~jsx-symbol)
             (~jsx-symbol ~resolved-tag ~props ~@unformed-children)
             (apply ~jsx-symbol
                    ~intercepted-tag
                    (cljs.core/clj->js ~props)
                    (map cljs.core/clj->js ~(into [] unformed-children))))
          ;; We don't wanna pollute the expansion with JS related stuff
          ;; if not in CLJS environment
          `(~jsx-symbol ~resolved-tag ~props ~@unformed-children))))))

(defn wrap-in-do [[x & more :as args]]
  (if (empty? more)
    x
    `(do ~@args)))

(defmacro defjsx [macro-name jsx-name fragment-name]
  `(do
     (defn ~macro-name [&form# &env# & forms#]
       (->> forms#
            (w/postwalk #(visit-node (cljs-env? &env#)
                                     ~(str jsx-name)
                                     ~(str fragment-name)
                                     %))
            wrap-in-do))
     (. (var ~macro-name) (setMacro))
     (var ~macro-name)
     (s/fdef ~macro-name
       :args :cljsx.specs/forms
       :ret any?)))

(defn cljs-env? [&env]
  (let [ns (:ns &env)
        a (:js-globals &env)
        b (:js-aliases ns)
        c (:cljs.analyzer/constants ns)]
    (boolean (or a b c))))

(defmacro clj-fn?
  "Checks whether `x` is a Clojure function.
  Returns `true` if it is certain that the argument is a Clojure function,
  `false` if it is certain that it's JavaScript function and `nil` if
  it can't be determined."
  [x]
  (let [locals (:locals &env)
        defs (get-in &env [:ns :defs])
        all (merge locals defs)
        entry (all x)]
    (if-not entry
      ;; If there's no entry, it's a JS object, except for a case
      ;; when a CLJ function was passed through identity
      ;; (with-out-str (clojure.pprint/pprint all))
      nil
      ;; Otherwise...
      (case (:tag entry)
        ;; If tag is 'js we can be sure it's a JS value.
        js false
        ;; If tag is 'any, we can't tell,
        ;; This is probably a value returned from a function.
        any nil
        ;; If tag is nil, CLJ values have some additional keys.
        ;; We can for example check on the presence of :meta
        nil (if (:meta entry)
              ;; If there is the :meta key, it is a CLJ value
              true
              ;; Otherwise we can't tell
              nil)
        ;; If tag is anything except for the above,
        ;; it's a CLJ value.
        true))))

(def jsify-props identity)
(def cljify-props identity)
(def js-obj-or-map? map?)

(defn- component-impl [interceptor-sym args]
  (let [{:keys [props body] :as args'} (s/conform ::specs/component-args args)
        fn-name (get-in args' [:quoted-name :symbol])
        possible-fn-name (if fn-name [fn-name] [])
        props' (s/unform ::specs/component-props props)
        body' (s/unform ::specs/fn-body body)
        func `(fn [~props'] ~@body')]
    `(fn ~@possible-fn-name [props#]
       (clojure.core/assert (cljsx.core/js-obj-or-map? props#))
       (~func (~interceptor-sym props#)))))

(defmacro component
  "Defines a function which takes an associative as its single argument.
  The argument will always be passed to the body converted
  to a map of keywords to Clojure values.
  The first argument should be a symbol or a destructuring map,
  all the other arguments are the body of the function."
  [& more]
  (component-impl 'cljsx.core/cljify-props more))

(defmacro component-js
  "Defines a function which takes an associative as its single argument.
  The argument will always be passed to the body converted
  to a map of keywrds to JS values.
  The first argument should be a symbol or a destructuring map,
  all the other arguments are the body of the function."
  [& more]
  (component-impl 'cljsx.core/jsify-props more))

(defmacro component+js
  "Defines a function which takes an associative as its single argument.
  The argument will be allways passed to the body both as a map of keywords
  to Clojure values and as map of keywords to JS values.
  The first argument of the macro is the Clojure map,
  the second argument is the JS map.
  Both should be a symbol or a destructuring map.
  all the other arguments are the body of the function."
  [& args]
  (let [args' (s/conform ::specs/component+js-args args)
        {:keys [clj-props js-props body] :as args'} args'
        fn-name (get-in args' [:quoted-name :symbol])
        possible-fn-name (if fn-name [fn-name] [])
        clj-props' (s/unform ::specs/component-props clj-props)
        js-props' (s/unform ::specs/component-props js-props)
        body' (s/unform ::specs/fn-body body)
        func `(fn [~clj-props' ~js-props'] ~@body')]
    `(fn ~@possible-fn-name [props#]
       (clojure.core/assert (cljsx.core/js-obj-or-map? props#))
       (~func
        (cljsx.core/cljify-props props#)
        (cljsx.core/jsify-props props#)))))

(defn- defcomponent-impl [interceptor-sym args]
  (let [{:keys [fn-name
                docstring
                props
                body]} (s/conform ::specs/defcomponent-args args)
        possible-docstring (if docstring [docstring] [])
        props' (s/unform ::specs/component-props props)
        body' (s/unform ::specs/fn-body body)
        func `(fn [~props'] ~@body')]
    `(defn ~fn-name ~@possible-docstring [props#]
       (clojure.core/assert (cljsx.core/js-obj-or-map? props#))
       (~func (~interceptor-sym props#)))))

(defmacro defcomponent
  "Same as (def name (component param exprs))"
  [& args]
  (defcomponent-impl 'cljsx.core/cljify-props args))

;; TODO: There doesn't seem to be a usecase for this macro.
(defmacro defcomponent-js
  "Same as (def name (component-js param exprs))"
  [& args]
  (defcomponent-impl 'cljsx.core/jsify-props args))

;; TODO: There doesn't seem to be a usecase for this macro
(defmacro defcomponent+js
  "Same as (def name (component+js param exprs))"
  [& args]
  (let [{:keys [fn-name
                docstring
                clj-props
                js-props
                body]} (s/conform ::specs/defcomponent+js-args args)
        possible-docstring (if docstring [docstring] [])
        clj-props' (s/unform ::specs/component-props clj-props)
        js-props' (s/unform ::specs/component-props js-props)
        body' (s/unform ::specs/fn-body body)
        func `(fn [~clj-props' ~js-props'] ~@body')]
    `(defn ~fn-name ~@possible-docstring [props#]
       (clojure.core/assert (cljsx.core/js-obj-or-map? props#))
       (~func
        (cljsx.core/cljify-props props#)
        (cljsx.core/jsify-props props#)))))

(defn- intercept-fn-tail [[params & body]]
  (let [[pos-args [_ rest-arg]] (split-with #(not= % '&) params)
        pos-arg-aliases (map #(if (symbol? %) % (gensym)) pos-args)
        possible-rest-arg (if rest-arg ['& rest-arg] [])
        rest-arg-conversion (if rest-arg
                              `(map #(cljs.core/js->clj % :keywordize-keys true)
                                    ~rest-arg)
                              [])]
    `([~@pos-arg-aliases ~@possible-rest-arg]
      (apply (fn [~@params]
               ~@body)
             ~@(map (fn [x]
                      `(cljs.core/js->clj ~x :keywordize-keys true))
                    pos-arg-aliases)
             ~rest-arg-conversion))))

(defmacro fn-clj [& fn-args]
  (let [{fn-name :fn-name [arity] :fn-tail} (s/conform ::specs/fn-args fn-args)
        possible-fn-name (if fn-name [fn-name] [])
        fn-tail (if fn-name (rest fn-args) fn-args)
        multi-arity-fn-tail (if (= arity :arity-1)
                              (intercept-fn-tail fn-tail)
                              (map intercept-fn-tail fn-tail))]
    `(fn ~@possible-fn-name ~@multi-arity-fn-tail)))

(defmacro defn-clj [& defn-args]
  (let [{[arity] :fn-tail
         :keys [fn-name
                docstring
                meta]} (s/conform ::cs/defn-args defn-args)
        possible-docstring (if docstring [docstring] [])
        possible-meta (if meta [meta] [])
        ;; Drop that many items how many defn-arguments there are
        ;; in front of the function tail.
        fn-tail (drop (->> [docstring meta]
                           (filter some?)
                           count
                           inc)
                      defn-args)
        multi-arity-fn-tail (if (= arity :arity-1)
                              (intercept-fn-tail fn-tail)
                              (map intercept-fn-tail fn-tail))]
    `(defn ~fn-name
       ~@possible-docstring
       ~@possible-meta
       ~@multi-arity-fn-tail)))

(defjsx jsx> createElement Fragment)
(defjsx inferno> inferno-create-element/createElement inferno/Fragment)
(defjsx react> react/createElement react/Fragment)

;; These don't have fragment support
(defjsx snabbdom> snabbdom-pragma/createElement Fragment)
(defjsx nervjs> nervjs/createElement Fragment)
(defjsx preact> preact/h Fragment)
