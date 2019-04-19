(ns cljsx.core-test
  (:require [midje.sweet :refer :all]
            [cljsx.core :refer :all]
            [cljsx.conversion]))

(fact
 "list->tag&props&children"
 (list->tag&props&children
  '(<> foo bar))
 => '(<> nil (foo bar))

 (list->tag&props&children
  '(foo bar baz))
 => nil

 (list->tag&props&children
  '(<foo> bar baz))
 => '("foo" nil (bar baz))

 (list->tag&props&children
  '(<foo > bar baz))
 => (throws #"The first item must be a keyword or a spread!")

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

(defjsx my> my-jsx my-fragment)

(defn my-jsx [tag props & children]
  {:tag tag
   :props props
   :children (into [] children)})

(def my-fragment "MY FRAGMENT")

(fact
 "Custom JSX"
 (my>
  (+ 100 10 1))
 =>
 111

 (my>
  (<foo> "child-1"
         "child-2"))
 =>
 {:tag "foo"
  :props nil
  :children ["child-1" "child-2"]}

 (my>
  (<> "child-1"
      "child-2"))
 =>
 {:tag my-fragment
  :props nil
  :children ["child-1" "child-2"]}

 (my>
  (<foo> "foo-child-1"
         (<bar> "bar-child-1"
                "bar-child-2")
         "foo-child-3"))
 =>
 {:tag "foo"
  :props nil
  :children ["foo-child-1"
             {:tag "bar"
              :props nil
              :children ["bar-child-1"
                         "bar-child-2"]}
             "foo-child-3"]}

 (my>
  (<foo :a "A" :b "B" :c "C"
        ... {:a "AA"}
        :c "CC" >
        "foo-child"))
 =>
 {:tag "foo"
  :props {:a "AA"
          :b "B"
          :c "CC"}
  :children ["foo-child"]}

 (my>
  (let [bar (<bar>)
        Baz "BAZ"]
    (<foo> "foo"
           bar
           (<Baz> "baz")
           "foo")))
 =>
 {:tag "foo"
  :props nil
  :children ["foo"
             {:tag "bar"
              :props nil
              :children []}
             {:tag "BAZ"
              :props nil
              :children ["baz"]}
             "foo"]}

 (my>
  (let [bar (<bar>)
        Baz "BAZ"]
    (<> "foo"
           bar
           (<Baz> "baz")
           "foo")))
 =>
 {:tag my-fragment
  :props nil
  :children ["foo"
             {:tag "bar"
              :props nil
              :children []}
             {:tag "BAZ"
              :props nil
              :children ["baz"]}
             "foo"]}

 (my>
  (map (fn [x] (<foo> x))
       ["A" "B" "C"]))
 =>
 '({:tag "foo", :props nil, :children ["A"]}
   {:tag "foo", :props nil, :children ["B"]}
   {:tag "foo", :props nil, :children ["C"]})

 (my>
  (map (fn [x] (<> x))
       ["A" "B" "C"]))
 =>
 `({:tag ~my-fragment, :props nil, :children ["A"]}
   {:tag ~my-fragment, :props nil, :children ["B"]}
   {:tag ~my-fragment, :props nil, :children ["C"]})

 (my>
  (map (fn [Tag] (<Tag> "child"))
       ["a" "b" "c"]))
 =>
 '({:tag "a", :props nil, :children ["child"]}
   {:tag "b", :props nil, :children ["child"]}
   {:tag "c", :props nil, :children ["child"]})

 (my>
  (map #(<foo :prop % > %)
       ["a" "b" "c"]))
 =>
 '({:tag "foo", :props {:prop "a"}, :children ["a"]}
   {:tag "foo", :props {:prop "b"}, :children ["b"]}
   {:tag "foo", :props {:prop "c"}, :children ["c"]})

 (my>
  (map #(<> %)
       ["a" "b" "c"]))
 =>
 `({:tag ~my-fragment, :props nil, :children ["a"]}
   {:tag ~my-fragment, :props nil, :children ["b"]}
   {:tag ~my-fragment, :props nil, :children ["c"]})

 (my>
  (let [Foo "FOO"]
    (map #(<Foo :prop % > %)
         ["a" "b" "c"])))
 => '({:tag "FOO", :props {:prop "a"}, :children ["a"]}
      {:tag "FOO", :props {:prop "b"}, :children ["b"]}
      {:tag "FOO", :props {:prop "c"}, :children ["c"]})

 (macroexpand
  '(my>
    (<foo> "foo")
    (<bar> "bar")
    (<baz> "baz")))
 =>
 '(do (my-jsx "foo" nil "foo")
      (my-jsx "bar" nil "bar")
      (my-jsx "baz" nil "baz")))
