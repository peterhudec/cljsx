(ns cljsx.core-test
  (:require [midje.sweet :refer :all]
            [cljsx.core :refer :all]))

(fact
 "list->tag&props&children"
 (list->tag&props&children
  '(foo bar baz))
 => nil

 (list->tag&props&children
  '(<foo> bar baz))
 => '("foo" nil (bar baz))

 (list->tag&props&children
  '(<foo > bar baz))
 => '("foo" nil (bar baz))

 (list->tag&props&children
  '(<foo :a "A" :b > bar baz))
 => '("foo" ({:a "A" :b true}) (bar baz))

 (list->tag&props&children
  '(<foo :a "A" :b ... x :d :e "E" >
         bar baz))
 => '("foo"
      ({:a "A" :b true} x {:d true :e "E"})
      (bar baz))

 (list->tag&props&children
  '(<foo :a "A" :b
         ... x
         :d :e "E"
         ... y >
         bar baz))
 => '("foo"
      ({:a "A" :b true} x {:d true :e "E"} y)
      (bar baz))

 (list->tag&props&children
  '(<foo :a "A" :b
         ... x
         :d :e "E"
         ... y
         :f >
         bar baz))
 => '("foo"
      ({:a "A" :b true} x {:d true :e "E"} y {:f true})
      (bar baz))
 )

(facts
 "Walk"
 (fact
  "Leaves non-JSX expressions intact"
  (walk '(foo bar baz))
  => '(foo bar baz))
 (fact
  "Simple JSX"
  (walk '(<foo> bar baz))
  => '(*jsx* "foo" nil bar baz)

  (walk '(<foo/Bar> bar baz))
  => '(*jsx* foo/Bar nil bar baz)

  (walk '(<foo.Bar> bar baz))
  => '(*jsx* foo.Bar nil bar baz)
  )

 (fact
  "Props JSX"
  (walk
   '(<foo > bar baz))
  => '(*jsx* "foo" nil bar baz)

  (walk
   '(<foo :a "A" :b > bar baz))
  => '(*jsx* "foo" {:a "A" :b true} bar baz)

  (walk
   '(<foo/Bar :a :b :c > bar baz))
  => '(*jsx* foo/Bar {:a true :b true :c true} bar baz)

  (walk
   '(<foo.Bar :a "A" ... x :b "B" :a "AA" > bar baz))
  => '(*jsx*
       foo.Bar
       (clojure.core/merge
        {:a "A"}
        x
        {:a "AA" :b "B"})
       bar
       baz)

  (fact
   "Recursion"
   (walk
    '(foo
      (<bar> baz))) =>
   '(foo
     (*jsx* "bar" nil baz))

   (walk
    '(foo
      (<bar> baz)
      bing)) =>
   '(foo
     (*jsx* "bar" nil baz)
     bing))

  (walk
   '(foo
     bing
     (<bar> baz))) =>
  '(foo
    bing
    (*jsx* "bar" nil baz))

  (walk
   '(foo
     bing
     (<bar> baz))) =>
  '(foo
    bing
    (*jsx* "bar" nil baz))

  (walk
   '(<foo> baz
           (<bar> bing))) =>
  '(*jsx* "foo" nil
          baz
          (*jsx* "bar" nil
                 bing))

  (walk
   '(<foo> foo
           (<bar> bar
                  (<baz> baz)))) =>
  '(*jsx* "foo" nil
          foo
          (*jsx* "bar" nil
                 bar
                 (*jsx* "baz" nil
                        baz)))

  (walk
   '[111
     (<two> 222)
     333
     (<four> 444)
     555])
  =>
  '[111
    (*jsx* "two" nil 222)
    333
    (*jsx* "four" nil 444)
    555]

  (walk
   '#{111
      (<two> 222)
      333
      (<four> 444)
      555})
  =>
  '#{111
    (*jsx* "two" nil 222)
    333
    (*jsx* "four" nil 444)
    555}

  (walk
   '{:a "A"
     :b (<b> b)
     (<c> c) "C"
     :d (<d> d)
     :e "E"})
  =>
  '{:a "A"
    :b (*jsx* "b" nil b)
    (*jsx* "c" nil c) "C"
    :d (*jsx* "d" nil d)
    :e "E"}

  (walk
   '(<foo :bar (<bar> bar)
          :a "A"
          :baz (<baz :c "C"
                     :bing (<bing> bing)
                     :d "D" >
                     baz)
          :b >
          foo))
  =>
  '(*jsx* "foo"
          {:bar (*jsx* "bar" nil bar)
           :a "A"
           :baz (*jsx* "baz"
                       {:c "C"
                        :bing (*jsx* "bing" nil bing)
                        :d "D"}
                       baz)
           :b true}
          foo)

  (walk
   '(<foo :a "A"
          :bar (<bar> bar)
          :b
          ... xxx
          :c "C"
          :baz (<baz> baz)
          :d >
          foo))
  =>
  '(*jsx* "foo"
          (clojure.core/merge
           {:a "A"
            :bar (*jsx* "bar" nil bar)
            :b true}
           xxx
           {:c "C"
            :baz (*jsx* "baz" nil baz)
            :d true})
          foo)
  ))

