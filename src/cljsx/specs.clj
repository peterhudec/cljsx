(ns cljsx.specs
  (:require
   [clojure.reflect :as r]
   [clojure.spec.alpha :as s]
   [clojure.core.specs.alpha :as cs]
   [expound.alpha :as expound]))

(s/def ::spread (s/cat :operator ::spread-operator
                       :val ::spread-val))

(s/def ::spread-operator #{'...})
(expound/defmsg ::spread-operator
  "The spread operator `...`.")

(defn- even-vector? [x]
  (and (vector? x)
       (= (mod (count x) 2) 0)))
(s/def ::even-vector (s/coll-of ::form
                                :kind even-vector?))
(expound/defmsg ::even-vector
  "Vector with even number of items e.g. [a b], [a b c d] or [].")

(defn- spread-reference? [x]
  (and (symbol? x)
       (not= x '>)
       (not= x '...)))

(s/def ::spread-reference spread-reference?)

(expound/defmsg ::spread-reference
  "Any symbol other than `>` (closing tag) or `...` (spread operator).")

(s/def ::spread-val (s/or :reference ::spread-reference
                          :map ::map
                          :jsx ::jsx-expression
                          :s-expression ::s-expression
                          :even-vector ::even-vector))

(s/def ::true-attr keyword?)

(s/def ::attr-val (s/and (complement keyword?)
                         #(not= % '...)
                         ::form))

(s/def ::val-attr (s/cat :name keyword?
                         :value ::attr-val))

(s/def ::prop (s/alt :spread ::spread
                     :true-attr ::true-attr
                     :val-attr ::val-attr))

(s/def ::props (s/* ::prop))

(def fragment-tag? #{'<>})

(s/def ::fragment fragment-tag?)

(expound/defmsg ::fragment
  "JSX fragment tag `<>`.")

(defn- intrinsic-tag-without-props? [x]
  (re-matches #"<[a-z0-9]+>" (str x)))

(s/def ::simple-intrinsic-tag (s/and symbol?
                                     intrinsic-tag-without-props?))

(expound/defmsg ::simple-intrinsic-tag
  "JSX intrinsic tag without props e.g. <div>, <h1> or <foo>.")

(defn- intrinsic-tag-with-props? [x]
  (re-matches #"<[a-z0-9]+" (str x)))

(s/def ::props-intrinsic-tag (s/and symbol?
                                    intrinsic-tag-with-props?))

(defn- reference-tag-without-props? [x]
  (->> x
       str
       (re-matches #"<([\w.-]+/)?([\w]+\.)*[A-Z][><:+&'*\-\w]*>")))

(s/def ::simple-reference-tag
  (s/and symbol?
         reference-tag-without-props?))

(expound/defmsg ::simple-reference-tag
  "JSX reference tag without props e.g. <Foo>, <foo.Bar> or <foo.bar/Baz>.")

(defn- reference-tag-with-props? [x]
  (->> x
       str
       (re-matches #"<([\w.-]+/)?([\w]+\.)*[A-Z][><:+&'*\-\w]*")))

(s/def ::props-reference-tag
  (s/and symbol?
         reference-tag-with-props?
         #_#(->> %
                 str
                 (re-matches #"<([\w.-]+/)?([\w]+\.)*[A-Z][><:+&'*\-\w]*"))))

(s/def ::simple-tag (s/alt :fragment-tag ::fragment
                           :intrinsic-tag ::simple-intrinsic-tag
                           :reference-tag ::simple-reference-tag))

(s/def ::props-tag (s/alt :intrinsic-tag ::props-intrinsic-tag
                          :reference-tag ::props-reference-tag))

(def props-tag-end? #{'>})
(s/def ::props-tag-end props-tag-end?)
(expound/defmsg ::props-tag-end
  (str
   "End of JSX props tag "
   "(either the closing tag `>` is missing, "
   "or the first prop is neither a keyword, "
   "nor the `...` spread operator)."))

(def primitive? (complement coll?))
(s/def ::primitive primitive?)
(expound/defmsg ::primitive
  "Any primitive value e.g. 123 or \"foo\".")

(s/def ::form (s/or
               :jsx ::jsx-expression
               :primitive ::primitive
               :s-expression ::s-expression
               :map ::map
               :coll ::coll))

(s/def ::forms (s/* (s/spec ::form)))

(s/def ::simple-jsx-expression (s/cat :tag ::simple-tag
                                      :children ::forms))

(s/def ::props-jsx-expression (s/cat :tag-start ::props-tag
                                     :props ::props
                                     :tag-end ::props-tag-end
                                     :children ::forms))

(s/def ::jsx-expression
  ;; We need to check for seq?, otherwise vectors would match as
  ;; expressions e.g [<foo> bar] would be a valid JSX expression.
  (s/and seq?
         ;; Props expression is first so that its errors show up first
         (s/or :props-jsx-expression ::props-jsx-expression
               :simple-jsx-expression ::simple-jsx-expression)))
(expound/defmsg ::jsx-expression
  "JSX expression e.g. (<foo>), (<Bar> child) or (<baz.Bing :x y > child)")

(defn- tag? [x]
  (and (symbol? x)
       (->> x
            str
            (re-matches #"<.+>?"))))

(defn jsx? [x]
  (and (seq? x)
       (tag? (first x))))

(def anything-which-is-not-a-tag? (complement tag?))
(s/def ::not-tag anything-which-is-not-a-tag?)
(expound/defmsg ::not-tag "Anythig other than a JSX tag.")

(s/def ::non-tag-form (s/and ::not-tag
                             ::form))

(s/def ::s-expression (s/and seq?
                             (s/cat :first ::non-tag-form
                                    :rest (s/* ::form))))
(expound/defmsg ::s-expression
  "S-expresion e.g. (+ 1 2), (def foo 123) or (foo bar baz).")

(s/def ::map (s/map-of ::form ::form))

(defn- any-collection-which-is-not-a-jsx-expression? [x]
  (and (coll? x)
       (not (map? x))
       (not (jsx? x))))
(s/def ::coll (s/coll-of ::form
                         :kind any-collection-which-is-not-a-jsx-expression?))
(expound/defmsg ::coll
  "Any collection which is not a JSX expression e.g. (a b c) or #{a b c}")

(s/def ::fn-args (:args (s/get-spec 'clojure.core/fn)))

;; https://github.com/clojure/core.specs.alpha/blob/master/src/main/clojure/clojure/core/specs/alpha.clj
(s/def ::fn-body (s/alt :prepost+body (s/cat :prepost map?
                                             :body (s/+ any?))
                        :body (s/* any?)))

(s/def ::component-name (s/spec (s/cat :quote #{'quote}
                                       :symbol simple-symbol?)))

(s/def ::component-props (s/or :local-name ::cs/local-name
                               :map-binding ::cs/map-binding-form))

(s/def ::component-args (s/cat :quoted-name (s/? ::component-name)
                               :props ::component-props
                               :body ::fn-body))

(s/def ::component+js-args (s/cat :quoted-name (s/? ::component-name)
                                  :clj-props ::component-props
                                  :js-props ::component-props
                                  :body ::fn-body))

(s/def ::defcomponent-args (s/cat :fn-name simple-symbol?
                                  :docstring (s/? string?)
                                  :props ::component-props
                                  :body ::fn-body))

(s/def ::defcomponent+js-args (s/cat :fn-name simple-symbol?
                                     :docstring (s/? string?)
                                     :clj-props ::component-props
                                     :js-props ::component-props
                                     :body ::fn-body))

(s/fdef cljsx.core/component
  :args ::component-args
  :ret any?)

(s/fdef cljsx.core/component-js
  :args ::component-args
  :ret any?)

(s/fdef cljsx.core/component+js
  :args ::component+js-args
  :ret any?)

(s/fdef cljsx.core/defcomponent
  :args ::defcomponent-args
  :ret any?)

(s/fdef cljsx.core/defcomponent-js
  :args ::defcomponent-args
  :ret any?)

(s/fdef cljsx.core/defcomponent+js
  :args ::defcomponent+js-args
  :ret any?)

