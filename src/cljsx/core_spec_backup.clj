(ns cljsx.core-spec-backup
  (:require
   [clojure.spec.alpha :as s]
   [expound.alpha :refer [expound]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (s/def ::spread-operator #{'...})

  (s/def ::spreadable (s/or :reference symbol?
                            :literal map?))

  (s/def ::value (s/and (complement #{'...})
                        (complement keyword?)))

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

  (s/def ::nodes (s/* (s/spec ::node)))

  (s/conform ::nodes '(<foo>))
  (s/conform ::nodes '((<foo>) (<bar>)))

  (s/def ::simple-jsx-expression (s/cat :tag ::simple-tag
                                        :children ::nodes
                                        ))

  (s/conform ::simple-jsx-expression '(<foo>))
  (s/conform ::simple-jsx-expression '(<foo> child))
  (s/conform ::simple-jsx-expression '(<foo> (foo bar)))
  (s/conform ::simple-jsx-expression '(<foo> (<bar>)))

  (s/def ::props-tag (s/alt :intrinsic-tag ::props-intrinsic-tag
                            :reference-tag ::props-reference-tag))

  (s/def ::props-tag-end #{'>})

  (s/def ::props-jsx-expression (s/cat :tag-start ::props-tag
                                       :props (s/* ::prop)
                                       :tag-end ::props-tag-end
                                       :children ::nodes))

  (s/conform ::props-jsx-expression '(<foo ... props :a "AAA" > foo bar baz))
  (s/conform ::props-jsx-expression '(<foo :a "AAA" > foo bar baz))

  (s/def ::jsx-expression (s/or :simple-jsx-expression ::simple-jsx-expression
                                :props-jsx-expression ::props-jsx-expression))

  (s/def ::not-tag #(->> %
                         str
                         (re-matches #"<.+>?")
                         not))

  (s/def ::s-xpression (s/cat :first ::not-tag
                              ;; TODO: Don't forget to switch this back to ::nodes
                              :rest (s/* (constantly true)) #_::nodes))

  (s/conform ::s-xpression '(foo bar))
  (s/conform ::s-xpression '(<foo> bar))

  (s/def ::not-jsx-expression (s/or :not-list (complement list?)
                                    :s-expression ::s-xpression))

  (s/conform ::not-jsx-expression '(foo bar))
  (s/conform ::not-jsx-expression '(<foo> bar))

  (defn jsx? [c]
    (when (list? c)
      (when-let [[x] c]
        (when (symbol? x)
          (->> x
               str
               (re-matches #"<.+>?"))))))

  (jsx? '())
  (jsx? '(foo bar))
  (jsx? '(<foo> bar))
  (jsx? '(<> bar))
  (jsx? '(< bar))
  (jsx? '(<< bar))
  (jsx? '(:foo bar))
  (jsx? [])

  (s/def ::collection (s/coll-of ::node
                                 :kind (complement jsx?)))

  (s/conform ::collection '(foo bar))
  (s/conform ::collection '(<foo> bar))
  (s/conform ::collection '[])
  (s/conform ::collection '[:foo])
  (s/conform ::collection '[:foo (<bar>)])
  (s/conform ::collection '(:foo (<bar>)))
  (s/conform ::collection '#{foo bar baz})
  (s/conform ::collection '#{foo (<bar>) baz})

  (s/def ::map (s/map-of ::node ::node))

  (s/conform ::map '{:foo 123})
  (s/conform ::map '{:foo (<foo>)})
  (s/conform ::map '{(<foo :bar ... >) :foo})

  ;; (s/def ::not-jsx-expression (s/or ;; :collection ::collection
  ;;                                   :map ::map
  ;;                                   :not-list (complement list?)
  ;;                                   :s-expression ::s-xpression))

  (s/def ::node (s/or :collection ::collection
                      :map ::map
                      :symbol symbol?
                      ;; :not-jsx-expression ::not-jsx-expression
                      :jsx-expression ::jsx-expression))

  (s/conform ::node '[:foo (<bar>) :baz])
  (s/conform ::node '[:foo (<bar>) {:baz (<bing>)}])

  (s/conform ::node 'foo)
  (s/conform ::node '(foo bar baz))
  (s/conform ::node '(<foo> bar))
  (s/conform ::node '(<> foo bar baz))
  (s/conform ::node '(<simple> foo bar baz))
  (s/conform ::node '(<---> foo bar baz))
  (s/conform ::node '(<foo foo bar baz))
  (s/conform ::node '(<foo > foo bar baz))
  (s/conform ::node '(<foo ... props :a "AAA" > foo bar baz))
  (s/conform ::node '(<foo> foo (<bar>) baz))
  (s/conform ::node '(foo (<bar>) baz)))


