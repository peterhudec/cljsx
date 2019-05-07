(ns cljsx.specs
  (:require
   [clojure.reflect :as r]
   [clojure.spec.alpha :as s]
   [clojure.core.specs.alpha :as cs]
   [expound.alpha :as expound]))

(s/def ::attr-val (s/and (complement #{'...})
                         (complement keyword?)
                         ::form))

(s/def ::attr (s/cat :name keyword?
                     :value (s/? ::attr-val)))

(s/def ::spread (s/cat :spread-operator ::spread-operator
                       :spread-value ::spread-val))

(s/def ::spread-operator #{'...})

(defn- even-vector? [x]
  (and (vector? x)
       (= (mod (count x) 2) 0)))

(s/def ::even-vector (s/coll-of ::form
                                :kind even-vector?))

(s/def ::spread-val (s/or :reference symbol?
                          :map ::map
                          :jsx ::jsx-expression
                          :s-expression ::s-expression
                          :even-vector ::even-vector))

(s/def ::prop (s/alt :attr ::attr
                     :spread ::spread))

(s/def ::fragment #{'<>})

(s/def ::simple-intrinsic-tag (s/and symbol?
                                     #(re-matches #"<[a-z0-9]+>" (str %))))

(s/def ::props-intrinsic-tag (s/and symbol?
                                    #(re-matches #"<[a-z0-9]+" (str %))))

(s/def ::simple-reference-tag
  (s/and symbol?
         #(->> %
               str
               (re-matches #"<([\w.-]+/)?([\w]+\.)*[A-Z][><:+&'*\-\w]*>"))))

(s/def ::props-reference-tag
  (s/and symbol?
         #(->> %
               str
               (re-matches #"<([\w.-]+/)?([\w]+\.)*[A-Z][><:+&'*\-\w]*"))))

(s/def ::simple-tag (s/alt :fragment-tag ::fragment
                           :intrinsic-tag ::simple-intrinsic-tag
                           :reference-tag ::simple-reference-tag))

(s/def ::props-tag (s/alt :intrinsic-tag ::props-intrinsic-tag
                          :reference-tag ::props-reference-tag))

(s/def ::props-tag-end #{'>})

(s/def ::form (s/or
               :primitive (complement coll?)
               :s-expression ::s-expression
               :map ::map
               :coll ::coll
               :jsx ::jsx-expression))

(s/def ::forms (s/* (s/spec ::form)))

(s/def ::simple-jsx-expression (s/cat :tag ::simple-tag
                                      :children ::forms))

(s/def ::invalid-first-prop (s/and (complement keyword?)
                                   (complement #{'>})
                                   (complement #{'...})))

(s/def ::props (s/* ::prop))

(s/def ::props-jsx-expression (s/cat :tag-start ::props-tag
                                     :props ::props
                                     :tag-end ::props-tag-end
                                     :children ::forms))

(s/def ::jsx-expression
  ;; We need to check for seq?, otherwise vectors would match as
  ;; expressions e.g [<foo> bar] would be a valid JSX expression.
  (s/and seq?
         (s/or :simple-jsx-expression ::simple-jsx-expression
               :props-jsx-expression ::props-jsx-expression)))

(defn- tag? [x]
  (and (symbol? x)
       (->> x
            str
            (re-matches #"<.+>?"))))

(defn jsx? [x]
  (and (seq? x)
       (tag? (first x))))

(s/def ::not-tag (complement tag?))

(s/def ::non-tag-form (s/and ::not-tag
                             ::form))

(s/def ::s-expression (s/and seq?
                             (s/cat :first ::non-tag-form
                                    :rest (s/* ::form))))

(s/def ::map (s/map-of ::form ::form))

(defn- non-jsx-coll? [x]
  (and (coll? x)
       (not (map? x))
       (not (jsx? x))))

(s/def ::coll (s/coll-of ::form
                         :kind non-jsx-coll?))

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

