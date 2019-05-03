(ns cljsx.specs
  (:require
   [clojure.spec.alpha :as s]
   [expound.alpha :refer [expound]]))

(s/def ::spread-operator #{'...})

(defn even-vector? [x]
  (and (vector? x)
       (= (mod (count x) 2) 0)))

(s/def ::even-vector (s/coll-of ::form
                                :kind even-vector?))

(s/def ::spreadable (s/or :reference symbol?
                          :map ::map
                          :even-vector ::even-vector
                          :jsx ::jsx-expression
                          :s-expression ::s-expression))

(s/def ::value (s/and (complement #{'...})
                      (complement keyword?)
                      ::form))

(s/def ::attr (s/cat :name keyword?
                     :value (s/? ::value)))

(s/def ::spread (s/cat :spread-operator ::spread-operator
                       :spread-value ::spreadable))

(s/def ::prop (s/alt :attribute ::attr
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

;; (s/def ::form (s/or :map ::map
;;                     :coll ::coll
;;                     :not-jsx ::not-jsx
;;                     :jsx ::jsx-expression))
(s/def ::form (s/or
               :primitive (complement coll?)
               ;; :symbol symbol?
               ;; :keyword keyword?
               :s-expression ::s-expression
               ;; :not-jsx-2 ::not-jsx-2
               :map ::map
               :coll ::coll
               :jsx ::jsx-expression
               ))

(s/def ::forms (s/* (s/spec ::form)))

(s/def ::simple-jsx-expression (s/cat :tag ::simple-tag
                                      :children ::forms))

(s/def ::props-jsx-expression (s/cat :tag-start ::props-tag
                                     :props (s/* ::prop)
                                     :tag-end ::props-tag-end
                                     :children ::forms))


(s/def ::jsx-expression (s/or :simple-jsx-expression ::simple-jsx-expression
                              :props-jsx-expression ::props-jsx-expression))

(defn jsx? [x]
  (and (seq? x)
       ;; TODO: Use tag? here
       (-> x first symbol?)
       (->> x
            first
            str
            (re-matches #"<.+>?"))))

(jsx? '(+ a b))
(jsx? '(:foo bar))

(s/def ::not-jsx (complement jsx?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tag? [x]
  (and (symbol? x)
       (->> x
            str
            (re-matches #"<.+>?"))))

(s/def ::not-tag (complement tag?))

(s/def ::non-tag-form (s/and ::not-tag
                             ::form))

(s/def ::s-expression (s/and seq?
                             (s/cat :first ::non-tag-form
                                    :rest (s/* ::form))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::map (s/map-of ::form ::form))

(defn non-jsx-coll? [x]
  (and (coll? x)
       (not (map? x))
       (not (jsx? x))))

(s/def ::coll (s/coll-of ::form
                         :kind non-jsx-coll?))

(s/def ::not-jsx-2 (s/and (complement map?)
                          (complement vector?)
                          (complement set?)
                          (complement jsx?)))

(comment
  (s/conform ::form '(<foo> bar (baz (<bing> {:a (<..>)}))))
  (s/conform ::form '(<foo> bar (baz (<bing> {:a (<..>)}))))
  (s/conform ::form '(baz (<bing> {:a (<..>)})))
  (s/conform ::form '(baz))
  (s/conform ::form 'foo)
  (s/conform ::form :foo)
  (s/conform ::form "foo")
  (s/conform ::form '(foo bar))
  (s/conform ::form '(foo (<bar>)))
  (s/conform ::form '[foo bar])
  (s/conform ::form '[foo (<bar>)])
  (s/conform ::form '{:foo "bar"})
  (s/conform ::form '{:foo (<bar>)}))
