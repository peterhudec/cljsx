(ns cljsx.props-test
  (:require [cljsx.props :refer :all]
            [midje.sweet :refer :all]))

(facts
 (str "Spread is only valid if it is followed by a symbol "
      "other than `'...` or an associative data structure")
 (fact
  "Valid spread"
  (valid-spread-operand? 'foo) => truthy
  ;(valid-spread-operand? '(... nil)) => truthy
  (valid-spread-operand? {}) => truthy
  (valid-spread-operand? []) => truthy
  (valid-spread-operand? '(foo)) => truthy
  (valid-spread-operand? '#(+ 1 2)) => truthy)
 
 (fact
  "Invalid spread"
  (valid-spread-operand? "foo") => falsey
  (valid-spread-operand? #{}) => falsey
  (valid-spread-operand? ()) => falsey
  (valid-spread-operand? 123) => falsey
  (valid-spread-operand? :foo) => falsey
  (valid-spread-operand? '...) => falsey))

(def spread-at-end-error
  #"Invalid spread at the end of props")

(fact
 "Split props on spread operator"
 (split-spread '()) => '()
 (split-spread '(a b c)) => '((a b c))

 (split-spread '(... xxx a b))
 => '((... xxx) (a b))

 (split-spread '(a b ... xxx c d))
 => '((a b) (... xxx) (c d))

 (split-spread '(a b c ... xxx d e f))
 => '((a b c) (... xxx) (d e f))

 (split-spread '(a b ... xxx c d ... yyy))
 => '((a b) (... xxx) (c d) (... yyy))

 (split-spread '(a b ... xxx c d ... yyy e f))
 => '((a b) (... xxx) (c d) (... yyy) (e f))

 (split-spread '(a b ...))
 => (throws #"Extraneous symbol in props")

 (split-spread '(...))
 => (throws #"Extraneous symbol in props")

 (split-spread '(a b ... ... c d))
 => (throws #"Invalid spread operand `...`")

 (split-spread '(a b ... 123 c d))
 => (throws #"Invalid spread operand `123`")

 (split-spread '(a b ... "foo" c d))
 => (throws #"Invalid spread operand `\"foo\"`")

 (split-spread '(a b ... ... c d))
 => (throws #"Invalid spread operand `...`")

 (split-spread '(a b ... :c c d ... :e))
 => (throws #"Invalid spread operand `:c`"))

(facts
 "props->map converts a list of props to a map "
 (fact
  "Last keyword wins"
  (props->map '(:a "A"))
  => {:a "A"}

  (props->map '(:a "A" :b "B" :a "AA"))
  => {:a "AA" :b "B"}

  (props->map '(:a "A" :b "B" :a "AA" :b "BB" :a "AAA"))
  => {:a "AAA" :b "BB"})
 (fact
  "Keyword without value is associated with `true`"
  (props->map '(:a))
  => {:a true}

  (props->map '(:a "A" :b))
  => {:a "A" :b true}

  (props->map '(:a "A" :b :c "C"))
  => {:a "A" :b true :c "C"}

  (props->map '(:a "A" :b "B" :a :b "BB"))
  => {:a true :b "BB"}

  (props->map '(:a "A" :b "B" :a :b))
  => {:a true :b true}

  (props->map '(:a :b :a "A" :b "B"))
  => {:a "A" :b "B"}))

(fact
 (str "props->mergelist takes a list of props and "
      "returns a list of expressions to be merged")
 (props->mergelist ())
 => (throws #"The first item must be a keyword or a spread!")

 (props->mergelist '(:a "A" :b "B"))
 => '({:a "A" :b "B"})

 (props->mergelist '(:a "A" :b ... x))
 => '({:a "A" :b true}
      x)

 (props->mergelist '(:a "A" :b ... x :c "C" :d))
 => '({:a "A" :b true}
      x
      {:c "C" :d true})

 (props->mergelist '(:a "A" :b ... x :c "C" :d ... y))
 => '({:a "A" :b true}
      x
      {:c "C" :d true}
      y)

 (props->mergelist '(... x ... y ... z))
 => '(x y z))

(fact
 (str "list->props&children splits a list on the first `'>`"
      "occurence but won't keep the actual `'>` separator")
 (list->props&children '(a b c))
 => '[(a b c) nil]

 (list->props&children '(a b c >))
 => '[(a b c) nil]

 (list->props&children '(a b c > d e f))
 => '[(a b c) (d e f)]

 (list->props&children '(a b c > d e f > i j k))
 => '[(a b c) (d e f > i j k)])
