(ns cljsx.spec-test
  (:require  [midje.sweet :refer [fact facts]]
             [clojure.spec.alpha :as s]
             [cljsx.specs :as sut]))

(fact "Pokus"
  ::sut/spread-operator => :cljsx.specs/spread-operator)

(fact "Spec"
  (s/valid? ::sut/spread-operator '...) => true
  (s/valid? ::sut/spread-operator 'foo) => false
  )

(def dummy-var 123)

(facts "Form"
  (facts "Non JSX"
    (fact "Primitive literals"
      (s/valid? ::sut/form "foo") => true
      (s/valid? ::sut/form \newline) => true
      (s/valid? ::sut/form #"foo") => true
      (s/valid? ::sut/form nil) => true
      (s/valid? ::sut/form true) => true
      (s/valid? ::sut/form false) => true
      (s/valid? ::sut/form ##Inf) => true
      (s/valid? ::sut/form ##-Inf) => true
      (s/valid? ::sut/form ##NaN) => true
      (s/valid? ::sut/form :foo) => true
      (s/valid? ::sut/form ::foo) => true
      (s/valid? ::sut/form :foo/bar) => true
      (s/valid? ::sut/form 'foo) => true
      (s/valid? ::sut/form '...) => true
      (s/valid? ::sut/form #'dummy-var) => true
      (s/valid? ::sut/form @#'dummy-var) => true
      (s/valid? ::sut/form 123) => true
      (s/valid? ::sut/form 0xff) => true
      (s/valid? ::sut/form 017) => true
      (s/valid? ::sut/form 2r1011) => true
      (s/valid? ::sut/form 36rCRAZY) => true
      (s/valid? ::sut/form 7N) => true
      (s/valid? ::sut/form -22/7) => true
      (s/valid? ::sut/form 2.78) => true
      (s/valid? ::sut/form -1.2e-5) => true
      (s/valid? ::sut/form 4.2M) => true
      )
    (facts "Collections"
      (s/valid? ::sut/form ()) => true
      (s/valid? ::sut/form '(a b c)) => true
      (s/valid? ::sut/form []) => true
      (s/valid? ::sut/form [:a :b :c]) => true
      (s/valid? ::sut/form #{}) => true
      (s/valid? ::sut/form #{:a :b :c}) => true
      (s/valid? ::sut/form {}) => true
      (s/valid? ::sut/form {:a 1 :b 2}) => true
      ))
  (facts "JSX without props"
    (fact "No children"
      (s/valid? ::sut/form '(<>)) => true
      (s/valid? ::sut/form '(<a>)) => true
      (s/valid? ::sut/form '(<..>)) => false

      )
    (fact "With children"
      (s/valid? ::sut/form '(<> foo 123 :bar)) => true
      (s/valid? ::sut/form '(<a> foo 123 :bar)) => true
      (s/valid? ::sut/form '(<..> foo 123 :bar)) => false

      )

    )

  (facts "JSX with props"
    (fact "No children"
      (s/valid? ::sut/form '(<a :b >)) => true
      (s/valid? ::sut/form '(<.. :b >)) => false

      )
    (fact "With children"
      (s/valid? ::sut/form '(<a :b > foo 123 :bar)) => true
      (s/valid? ::sut/form '(<.. :b > foo 123 :bar)) => false

      )

    (fact "Missing tag end"
      (s/valid? ::sut/form '(<a)) => false
      (s/valid? ::sut/form '(<a :b)) => false
      (s/valid? ::sut/form '(<a :b 123)) => false
      (s/valid? ::sut/form '(<a :b 123 ... foo)) => false

      )

    (fact "Spread"
      (fact "Valid"
        (s/valid? ::sut/form '(<a ... x >)) => true
        (s/valid? ::sut/form '(<a ... {} >)) => true
        (s/valid? ::sut/form '(<a ... [] >)) => true
        (s/valid? ::sut/form '(<a ... [:x x] >)) => true
        (s/valid? ::sut/form '(<a ... [:x x :y :y] >)) => true
        (s/valid? ::sut/form '(<a :b ... x >)) => true
        (s/valid? ::sut/form '(<a :b ... {} >)) => true
        (s/valid? ::sut/form '(<a :b ... x :c >)) => true
        (s/valid? ::sut/form '(<a :b ... {} :c >)) => true
        (s/valid? ::sut/form '(<a :b ... x :c ... y >))
        => true
        (s/valid? ::sut/form '(<a :b ... {} :c ... y >))
        => true

        )
      (fact "Invalid"
        (s/valid? ::sut/form '(<a ... >)) => false
        (s/valid? ::sut/form '(<a :b ... >)) => false
        (s/valid? ::sut/form '(<a :b 123 ... >)) => false
        (s/valid? ::sut/form '(<a ... >)) => false
        (s/valid? ::sut/form '(<a ... 123 >)) => false
        (s/valid? ::sut/form '(<a ... "foo" >)) => false
        (s/valid? ::sut/form '(<a ... :bar >)) => false
        (s/valid? ::sut/form '(<a ... x ... >)) => false
        (s/valid? ::sut/form '(<a ... x ... 123 >)) => false
        (s/valid? ::sut/form '(<a ... x ... "foo" >)) => false
        (s/valid? ::sut/form '(<a ... x ... :bar >)) => false
        (s/conform ::sut/form '(<a ... [:x y :z] >)) => ::s/invalid
        (s/conform ::sut/form '(<a ... [:a :b :c :d :e] >)) => ::s/invalid

        )

      )
    )
  (facts "Nesting"
    (fact "List"
      (s/valid? ::sut/form '((<>))) => true
      (s/valid? ::sut/form '((<b>))) => true
      (s/conform ::sut/form '((<..>))) => ::s/invalid

      (s/valid? ::sut/form '((a (<>)))) => true
      (s/valid? ::sut/form '((a (<b>)))) => true
      (s/conform ::sut/form '((a (<..>)))) => ::s/invalid

      (s/valid? ::sut/form '(a (<>))) => true
      (s/valid? ::sut/form '(a (<b>))) => true
      (s/conform ::sut/form '(a (<..>))) => ::s/invalid

      (s/valid? ::sut/form '(a b (<>))) => true
      (s/valid? ::sut/form '(a b (<c>))) => true
      (s/conform ::sut/form '(a b (<..>))) => ::s/invalid
      )
    (fact "Vector"
      (s/valid? ::sut/form '[<>]) => true
      (s/valid? ::sut/form '[<b>]) => true
      (s/valid? ::sut/form '[<..>]) => true

      (s/valid? ::sut/form '[(<>)]) => true
      (s/valid? ::sut/form '[(<b>)]) => true
      (s/conform ::sut/form '[(<..>)]) => ::s/invalid

      (s/valid? ::sut/form '[(a (<>))]) => true
      (s/valid? ::sut/form '[(a (<b>))]) => true
      (s/conform ::sut/form '[(a (<..>))]) => ::s/invalid

      (s/valid? ::sut/form '[a (<>)]) => true
      (s/valid? ::sut/form '[a (<b>)]) => true
      (s/conform ::sut/form '[a (<..>)]) => ::s/invalid

      (s/valid? ::sut/form '[a b (<>)]) => true
      (s/valid? ::sut/form '[a b (<c>)]) => true
      (s/conform ::sut/form '[a b (<..>)]) => ::s/invalid
      )

    (fact "Set"
      (s/valid? ::sut/form '#{<>}) => true
      (s/valid? ::sut/form '#{<b>}) => true
      (s/valid? ::sut/form '#{<..>}) => true

      (s/valid? ::sut/form '#{(<>)}) => true
      (s/valid? ::sut/form '#{(<b>)}) => true
      (s/conform ::sut/form '#{(<..>)}) => ::s/invalid

      (s/valid? ::sut/form '#{(a (<>))}) => true
      (s/valid? ::sut/form '#{(a (<b>))}) => true
      (s/conform ::sut/form '#{(a (<..>))}) => ::s/invalid

      (s/valid? ::sut/form '#{a (<>)}) => true
      (s/valid? ::sut/form '#{a (<b>)}) => true
      (s/conform ::sut/form '#{a (<..>)}) => ::s/invalid

      (s/valid? ::sut/form '#{a b (<>)}) => true
      (s/valid? ::sut/form '#{a b (<c>)}) => true
      (s/conform ::sut/form '#{a b (<..>)}) => ::s/invalid
      )
    (fact "Map"
      (s/valid? ::sut/form '{:a <>}) => true
      (s/valid? ::sut/form '{:a <b>}) => true
      (s/valid? ::sut/form '{:a <..>}) => true

      (s/valid? ::sut/form '{:a (<>)}) => true
      (s/valid? ::sut/form '{:a (<b>)}) => true
      (s/conform ::sut/form '{:a (<..>)}) => ::s/invalid

      (s/valid? ::sut/form '{(<>) :a}) => true
      (s/valid? ::sut/form '{(<b>) :a}) => true
      (s/conform ::sut/form '{(<..>) :a}) => ::s/invalid

      (s/valid? ::sut/form '{:a {:b (<>)}}) => true
      (s/valid? ::sut/form '{:a {:b (<b>)}}) => true
      (s/conform ::sut/form '{:a {:b (<..>)}}) => ::s/invalid

      (s/valid? ::sut/form '{{:b (<>)} :a}) => true
      (s/valid? ::sut/form '{{:b (<b>)} :a}) => true
      (s/conform ::sut/form '{{:b (<..>)} :a}) => ::s/invalid
      )

    (fact "Shorthand function"
      (s/valid? ::sut/form '#(f <>)) => true
      (s/valid? ::sut/form '#(f <a>)) => true
      (s/valid? ::sut/form '#(f <..>)) => true

      (s/valid? ::sut/form '#(f (<>))) => true
      (s/valid? ::sut/form '#(f (<a>))) => true
      (s/conform ::sut/form '#(f (<..>))) => ::s/invalid

      (s/valid? ::sut/form '#(<> x y)) => true
      (s/valid? ::sut/form '#(<a> x y)) => true
      (s/conform ::sut/form '#(<..> x y)) => ::s/invalid

      (s/valid? ::sut/form '#((<>) x y)) => true
      (s/valid? ::sut/form '#((<a>) x y)) => true
      (s/conform ::sut/form '#((<..>) x y)) => ::s/invalid
      )
    
    (fact "Children"
      (s/valid? ::sut/form '(<a> (<>))) => true
      (s/valid? ::sut/form '(<a> (<b>))) => true
      (s/conform ::sut/form '(<a> (<..>))) => ::s/invalid

      (s/valid? ::sut/form '(<a> b (<>))) => true
      (s/valid? ::sut/form '(<a> b (<c>))) => true
      (s/conform ::sut/form '(<a> b (<..>))) => ::s/invalid

      (s/valid? ::sut/form '(<a> (<b> (<>)))) => true
      (s/valid? ::sut/form '(<a> (<b> (<c>)))) => true
      (s/conform ::sut/form '(<a> (<b> (<..>)))) => ::s/invalid
      )

    (facts "Props"
      (fact "Attributes"
        (s/valid? ::sut/form '(<a :p (<>) >)) => true
        (s/valid? ::sut/form '(<a :p (<b>) >)) => true
        (s/conform ::sut/form '(<a :p (<..>) >)) => ::s/invalid

        (s/valid? ::sut/form '(<a :p [(<>)] >)) => true
        (s/valid? ::sut/form '(<a :p [(<b>)] >)) => true
        (s/conform ::sut/form '(<a :p [(<..>)] >)) => ::s/invalid

        (s/valid? ::sut/form '(<a :p {:x (<>)} >)) => true
        (s/valid? ::sut/form '(<a :p {:x (<b>)} >)) => true
        (s/conform ::sut/form '(<a :p {:x (<..>)} >)) => ::s/invalid
        )
      (fact "Spread"
        (s/valid? ::sut/form '(<a ... <> >)) => true
        (s/valid? ::sut/form '(<a ... <b> >)) => true
        (s/valid? ::sut/form '(<a ... <..> >)) => true

        (s/valid? ::sut/form '(<a ... {:x (<>)} >)) => true
        (s/valid? ::sut/form '(<a ... {:x (<b>)} >)) => true
        (s/conform ::sut/form '(<a ... {:x (<..>)} >)) => ::s/invalid

        (s/valid? ::sut/form '(<a ... [:x (<>)] >)) => true
        (s/valid? ::sut/form '(<a ... [:x (<b>)] >)) => true
        (s/conform ::sut/form '(<a ... [:x (<..>)] >)) => ::s/invalid

        (s/valid? ::sut/form '(<a ... (<>) >)) => true
        (s/valid? ::sut/form '(<a ... (<b>) >)) => true
        (s/conform ::sut/form '(<a ... (<..>) >)) => ::s/invalid

        (s/valid? ::sut/form '(<a ... (f (<>)) >)) => true
        (s/valid? ::sut/form '(<a ... (f (<b>)) >)) => true
        (s/conform ::sut/form '(<a ... (f (<..>)) >)) => ::s/invalid
        )
      )

    (fact "Deep"
      (s/valid? ::sut/form
                '(<a :b "b"
                     :c
                     ... d
                     >
                     (map #(<a> "foo"
                                "bar"
                                (<a :b
                                    ... {:c (<a ... [:b (<>)]
                                                >)}
                                    >)))))
      => true

      (s/valid? ::sut/form
                '(<a :b "b"
                     :c
                     ... d
                     >
                     (map #(<a> "foo"
                                "bar"
                                (<a :b
                                    ... {:c (<a ... [:b (<x>)]
                                                >)}
                                    >)))))
      => true

      (s/conform ::sut/form
                 '(<a :b "b"
                      :c
                      ... d
                      >
                      (map #(<a> "foo"
                                 "bar"
                                 (<a :b
                                     ... {:c (<a ... [:b (<..>)]
                                                 >)}
                                     >)))))
      => ::s/invalid

      (s/conform ::sut/form
                 '(<a :b "b"
                      :c
                      ... d
                      >
                      (map #(<a> "foo"
                                 "bar"
                                 (<a :b
                                     ... {:c (<a ... [:b (<x ...
                                                             >)]
                                                 >)}
                                     >)))))
      => ::s/invalid

      (s/conform ::sut/form
                 '(<a :b "b"
                      :c
                      ... d
                      >
                      (map #(<a> "foo"
                                 "bar"
                                 (<a :b
                                     ... {:c (<a ... [:b (<x ... :k >)]
                                                 >)}
                                     >)))))
      => ::s/invalid
      )
    )
  )

(fact "::non-tag"
  (s/conform ::sut/not-tag 'foo) => 'foo
  (s/conform ::sut/not-tag 123) => 123
  (s/conform ::sut/not-tag '(a b c)) => '(a b c)
  (s/conform ::sut/not-tag '<>) => ::s/invalid
  (s/conform ::sut/not-tag '<a>) => ::s/invalid
  (s/conform ::sut/not-tag '<..>) => ::s/invalid

  )

(fact "::non-tag-form"
  (s/valid? ::sut/non-tag-form 'foo) => true
  (s/conform ::sut/non-tag-form '<a>) => ::s/invalid
  (s/valid? ::sut/non-tag-form '(a b c)) => true
  (s/valid? ::sut/non-tag-form '(<> b c)) => true
  (s/valid? ::sut/non-tag-form '(<a> b c)) => true
  (s/conform ::sut/non-tag-form '(<..> b c)) => ::s/invalid

  )

(fact "::s-expression"
  (s/valid? ::sut/s-expression '(a b c)) => true
  (s/valid? ::sut/s-expression '(a (<>) c)) => true
  (s/valid? ::sut/s-expression '(a (<a>) c)) => true
  (s/conform ::sut/s-expression '(a (<..>) c)) => ::s/invalid

  (s/conform ::sut/s-expression '(<>)) => ::s/invalid
  (s/conform ::sut/s-expression '(<a>)) => ::s/invalid
  (s/conform ::sut/s-expression '(<..>)) => ::s/invalid

  (s/valid? ::sut/s-expression '((a b) c d)) => true
  (s/valid? ::sut/s-expression '(:foo c d)) => true
  (s/valid? ::sut/s-expression '(:foo c d)) => true

  (s/valid? ::sut/s-expression '((<>) c d)) => true
  (s/valid? ::sut/s-expression '((<a>) c d)) => true
  (s/conform ::sut/s-expression '((<..>) c d)) => ::s/invalid

  (s/valid? ::sut/s-expression '(((<>)) c d)) => true
  (s/valid? ::sut/s-expression '(((<a>)) c d)) => true
  (s/conform ::sut/s-expression '(((<..>)) c d)) => ::s/invalid

  (s/valid? ::sut/s-expression '((a (<>)) c d)) => true
  (s/valid? ::sut/s-expression '((a (<b>)) c d)) => true
  (s/conform ::sut/s-expression '((a (<..>)) c d)) => ::s/invalid
  )

(facts "::coll"
  (fact "Vector"
    (s/valid? ::sut/coll []) => true
    (s/valid? ::sut/coll [:a :b :c]) => true
    (s/valid? ::sut/coll '[(<>) :b]) => true
    (s/valid? ::sut/coll '[(<a>) :b]) => true
    (s/conform ::sut/coll '[(<..>) :b]) => ::s/invalid
    (s/valid? ::sut/coll '[:a (<>)]) => true
    (s/valid? ::sut/coll '[:a (<b>)]) => true
    (s/conform ::sut/coll '[:a (<..>)]) => ::s/invalid

    )
  )

(fact "::even-vector"
  (s/valid? ::sut/even-vector []) => true
  (s/valid? ::sut/even-vector [:a :b]) => true
  (s/valid? ::sut/even-vector [:a :b :c :d]) => true

  (s/conform ::sut/even-vector [:a]) => ::s/invalid
  (s/conform ::sut/even-vector [:a :b :c]) => ::s/invalid
  (s/conform ::sut/even-vector [:a :b :c :d :e]) => ::s/invalid
  )

(fact "Forms"
  (s/valid? ::sut/forms
            '((+ 1 2 3)
              (<> b c))) => true

  (s/valid? ::sut/forms
            '((+ 1 2 3)
              (<a> b c))) => true

  (s/conform ::sut/forms
             '((+ 1 2 3)
               (<..> b c))) => ::s/invalid
  )
