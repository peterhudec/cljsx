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

(s/def ::spread-val (s/or :reference #(and (symbol? %)
                                           (not= % '>))
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

(s/conform ::props-jsx-expression '(<foo :a "A" ... xxx :b :c "C" > child))
;; => {:tag-start [:intrinsic-tag <foo],
;;     :props
;;     [[:attr {:name :a, :value [:primitive "A"]}]
;;      [:spread {:spread-operator ..., :spread-value [:reference xxx]}]
;;      [:attr {:name :b}]
;;      [:attr {:name :c, :value [:primitive "C"]}]],
;;     :tag-end >,
;;     :children [[:primitive child]]}

(s/conform ::props '(:a "A" ... xxx :b :c "C"))
;; => [[:attr {:name :a, :value [:primitive "A"]}]
;;     [:spread {:spread-operator ..., :spread-value [:reference xxx]}]
;;     [:attr {:name :b}]
;;     [:attr {:name :c, :value [:primitive "C"]}]]

(defn props->mergelist [props]
  (let [props' (s/conform ::props props)]
    (reduce (fn [a [prop-type prop]]
              (conj a prop))
            []
            props')
    #_(split-with #(not= (first %) :spread) props')))

(props->mergelist '(:a "A" ... xxx :b :c "C"))
;; => [[:attr {:name :a, :value [:primitive "A"]}]
;;     [:spread {:spread-operator ..., :spread-value [:reference xxx]}]
;;     [:attr {:name :b}]
;;     [:attr {:name :c, :value [:primitive "C"]}]]


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mergegroup attrs first

(s/def ::prop-mergegroup (s/cat :attrs (s/* ::attr)
                                :spread ::spread))

(s/def ::prop-mergegroups (s/cat :mergegroups (s/* ::prop-mergegroup)
                                 :trailing-attrs (s/? (s/* ::attr))))

(s/conform ::prop-mergegroups '(:a :b "B" :c ... x :d :e ... y :f))


(s/conform ::prop-mergegroups '(:a :b "B" :c ... x :d :e ... y))

(s/conform ::prop-mergegroups '(... z
                                    :a :b "B" :c
                                    ... x
                                    :d :e
                                    ... y))

(s/def ::props-jsx-expression-2 (s/cat :tag-start ::props-tag
                                       :prop-mergegroups ::prop-mergegroups
                                       :tag-end ::props-tag-end
                                       :children ::forms))

(s/conform ::props-jsx-expression-2
           '(<foo :a :b "B" :c >))

(s/conform ::props-jsx-expression-2
           '(<foo ... zzz
                  :a :b "B" :c
                  ... 123
                  :a :b
                  ... yyy
                  :a > "child"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mergegroup spread first

(s/def ::prop-mergegroup-b (s/cat 
                            :spread ::spread
                            :attrs (s/* ::attr)
                            ))

(s/def ::leading-attrs
  (s/alt ;; :keyword keyword?
   :keyword keyword?
   :spread (s/cat :spread-operator ::spread-operator
                  :spread-value ::spread-val))
  ;; (s/? ::attr)
  #_(s/* ::attr)
  )


;; (s/def ::prop-mergegroups-b (s/cat :leading-attrs (s/* ::attr)
;;                                    :mergegroups (s/* ::prop-mergegroup-b)))
(s/def ::prop-mergegroups-b (s/cat
                             :head (s/alt
                                    ;; :spread (s/cat :op ::spread-operator
                                    ;;                :val ::spread-val-b)
                                    :spread ::spread
                                    :keyword keyword?
                                    :attr (s/cat :name keyword?
                                                 :value any?)
                                    ;; :spread-operator ::spread-operator
                                    )
                             ;; :tail (s/* any?)
                             :tail (s/* (s/alt
                                         :spread ::spread
                                         :keyword keyword?
                                         :attr (s/cat :name keyword?
                                                      :value (s/and any?
                                                                    #(not= % '...)))
                                         ;; :attrs (s/* ::attr)
                                         ))

                             ;; :leading-attrs ::leading-attrs
                             ;; :mergegroups (s/* ::prop-mergegroup-b)
                             ))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV

(s/def ::prop-new (s/alt
                   :spread ::spread
                   :true-attr keyword?
                   :val-attr (s/cat :name keyword?
                                :value (s/and 
                                              (complement keyword?)
                                              #(not= % '...)
                                              ::form))
                   ))

(s/def ::props-new (s/* ::prop-new))

(s/def ::props-jsx-expression-new (s/cat :tag-start ::props-tag
                                         :props ::props-new
                                         :tag-end ::props-tag-end
                                         :children ::forms))

(s/conform ::props-jsx-expression-new
           '(<foo >))

(s/conform ::props-jsx-expression-new
           '(<foo > child))

(s/conform ::props-jsx-expression-new
           '(<foo :a :b :c > child))

(s/conform ::props-jsx-expression-new
           '(<foo :a :b "B" :c
                  ... xxx
                  :d "D" :e :f "F"
                  ... yyy > child child child))

;; ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(s/conform ::prop-mergegroups-b '(... xxx))
(s/conform ::prop-mergegroups-b '(... xxx :a :b "B" :c))
(s/conform ::prop-mergegroups-b '(... xxx :a :b "B" :c ... yyy))
(s/conform ::prop-mergegroups-b '(... xxx :a :b "B" :c ... yyy :d))
(s/conform ::prop-mergegroups-b '(:a ... xxx))
(s/conform ::prop-mergegroups-b '(:a "A" ... xxx))
(s/conform ::prop-mergegroups-b '(:a "A" :aa ... xxx))

(s/conform ::prop-mergegroups-b '(:a :b "B" :c ... x :d :e ... y :f))


(s/conform ::prop-mergegroups-b '(:a :b "B" :c ... x :d :e ... y))

(s/conform ::prop-mergegroups-b '(... z
                                    :a :b "B" :c
                                    ... x
                                    :d :e
                                    ... y))

(s/def ::props-jsx-expression-2-b (s/cat :tag-start ::props-tag
                                         :prop-mergegroups ::prop-mergegroups-b
                                         :tag-end ::props-tag-end
                                         :children ::forms))

(s/conform ::props-jsx-expression-2-b
           '(<foo :a :b "B" :c >))

(s/conform ::props-jsx-expression-2-b
           '(<foo ... zzz
                  :a :b "B" :c
                  ... 123
                  :a :b
                  ... yyy
                  :a > "child"))

(s/def ::jsx-expression
  ;; We need to check for seq?, otherwise vectors would match as
  ;; expressions e.g [<foo> bar] would be a valid JSX expression.
  (s/and seq?
         (s/or :simple-jsx-expression ::simple-jsx-expression
               :props-jsx-expression ::props-jsx-expression)))
;; => :cljsx.specs/jsx-expression
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

