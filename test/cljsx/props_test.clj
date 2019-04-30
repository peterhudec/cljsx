(ns cljsx.props-test
  (:require [cljsx.props :as sut]
            [midje.sweet :refer [fact facts falsey throws truthy]]))

(facts
 (str "Spread is only valid if it is followed by a symbol "
      "other than `'...` or an associative data structure")
 (fact
  "Valid spread"
  (sut/valid-spread-operand? 'foo) => truthy
  (sut/valid-spread-operand? {}) => truthy
  (sut/valid-spread-operand? []) => truthy
  (sut/valid-spread-operand? '(foo)) => truthy
  (sut/valid-spread-operand? '#(+ 1 2)) => truthy)
 
 (fact
  "Invalid spread"
  (sut/valid-spread-operand? "foo") => falsey
  (sut/valid-spread-operand? #{}) => falsey
  (sut/valid-spread-operand? ()) => falsey
  (sut/valid-spread-operand? 123) => falsey
  (sut/valid-spread-operand? :foo) => falsey
  (sut/valid-spread-operand? '...) => falsey))

(def spread-at-end-error
  #"Invalid spread at the end of props")

(fact
 "Split props on spread operator"
 (sut/split-spread '()) => '()
 (sut/split-spread '(a b c)) => '((a b c))

 (sut/split-spread '(... xxx a b))
 => '((... xxx) (a b))

 (sut/split-spread '(a b ... xxx c d))
 => '((a b) (... xxx) (c d))

 (sut/split-spread '(a b c ... xxx d e f))
 => '((a b c) (... xxx) (d e f))

 (sut/split-spread '(a b ... xxx c d ... yyy))
 => '((a b) (... xxx) (c d) (... yyy))

 (sut/split-spread '(a b ... xxx c d ... yyy e f))
 => '((a b) (... xxx) (c d) (... yyy) (e f))

 (sut/split-spread '(a b ...))
 => (throws #"Extraneous symbol in props")

 (sut/split-spread '(...))
 => (throws #"Extraneous symbol in props")

 (sut/split-spread '(a b ... ... c d))
 => (throws #"Invalid spread operand `...`")

 (sut/split-spread '(a b ... 123 c d))
 => (throws #"Invalid spread operand `123`")

 (sut/split-spread '(a b ... "foo" c d))
 => (throws #"Invalid spread operand `\"foo\"`")

 (sut/split-spread '(a b ... ... c d))
 => (throws #"Invalid spread operand `...`")

 (sut/split-spread '(a b ... :c c d ... :e))
 => (throws #"Invalid spread operand `:c`"))

(facts
 "sut/props->map converts a list of props to a map "
 (fact
  "Last keyword wins"
  (sut/props->map '(:a "A"))
  => {:a "A"}

  (sut/props->map '(:a "A" :b "B" :a "AA"))
  => {:a "AA" :b "B"}

  (sut/props->map '(:a "A" :b "B" :a "AA" :b "BB" :a "AAA"))
  => {:a "AAA" :b "BB"})
 (fact
  "Keyword without value is associated with `true`"
  (sut/props->map '(:a))
  => {:a true}

  (sut/props->map '(:a "A" :b))
  => {:a "A" :b true}

  (sut/props->map '(:a "A" :b :c "C"))
  => {:a "A" :b true :c "C"}

  (sut/props->map '(:a "A" :b "B" :a :b "BB"))
  => {:a true :b "BB"}

  (sut/props->map '(:a "A" :b "B" :a :b))
  => {:a true :b true}

  (sut/props->map '(:a :b :a "A" :b "B"))
  => {:a "A" :b "B"}))

(fact
 (str "sut/props->mergelist takes a list of props and "
      "returns a list of expressions to be merged")
 (sut/props->mergelist ())
 => (throws #"The first item must be a keyword or a spread!")

 (sut/props->mergelist '(:a "A" :b "B"))
 => '({:a "A" :b "B"})

 (sut/props->mergelist '(:a "A" :b ... x))
 => '({:a "A" :b true}
      x)

 (sut/props->mergelist '(:a "A" :b ... x :c "C" :d))
 => '({:a "A" :b true}
      x
      {:c "C" :d true})

 (sut/props->mergelist '(:a "A" :b ... x :c "C" :d ... y))
 => '({:a "A" :b true}
      x
      {:c "C" :d true}
      y)

 (sut/props->mergelist '(... x ... y ... z))
 => '(x y z))

(fact
 (str "sut/list->props&children splits a list on the first `'>`"
      "occurence but won't keep the actual `'>` separator")
 (sut/list->props&children '(a b c))
 => '[(a b c) nil]

 (sut/list->props&children '(a b c >))
 => '[(a b c) nil]

 (sut/list->props&children '(a b c > d e f))
 => '[(a b c) (d e f)]

 (sut/list->props&children '(a b c > d e f > i j k))
 => '[(a b c) (d e f > i j k)])
