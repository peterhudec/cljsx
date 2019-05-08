(ns cljsx.core-test
  (:require [midje.sweet :refer [fact facts throws]]
            [cljsx.core :as sut]))

(sut/defjsx dummy>>> dummy-jsx dummy-fragment)

(defn dummy-jsx [tag props & children]
  {:tag tag
   :props props
   :children (into [] children)})

(def dummy-fragment "MY FRAGMENT")

(fact
 "Custom JSX"
 (dummy>>>
  (+ 100 10 1))
 =>
 111

 (dummy>>>
  (<foo> "child-1"
         "child-2"))
 =>
 {:tag "foo"
  :props nil
  :children ["child-1" "child-2"]}

 (dummy>>>
  (<> "child-1"
      "child-2"))
 =>
 {:tag dummy-fragment
  :props nil
  :children ["child-1" "child-2"]}

 (dummy>>>
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

 (dummy>>>
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

 (dummy>>>
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

 (dummy>>>
  (let [bar (<bar>)
        Baz "BAZ"]
    (<> "foo"
           bar
           (<Baz> "baz")
           "foo")))
 =>
 {:tag dummy-fragment
  :props nil
  :children ["foo"
             {:tag "bar"
              :props nil
              :children []}
             {:tag "BAZ"
              :props nil
              :children ["baz"]}
             "foo"]}

 (dummy>>>
  (map (fn [x] (<foo> x))
       ["A" "B" "C"]))
 =>
 '({:tag "foo", :props nil, :children ["A"]}
   {:tag "foo", :props nil, :children ["B"]}
   {:tag "foo", :props nil, :children ["C"]})

 (dummy>>>
  (map (fn [x] (<> x))
       ["A" "B" "C"]))
 =>
 `({:tag ~dummy-fragment, :props nil, :children ["A"]}
   {:tag ~dummy-fragment, :props nil, :children ["B"]}
   {:tag ~dummy-fragment, :props nil, :children ["C"]})

 (dummy>>>
  (map (fn [Tag] (<Tag> "child"))
       ["a" "b" "c"]))
 =>
 '({:tag "a", :props nil, :children ["child"]}
   {:tag "b", :props nil, :children ["child"]}
   {:tag "c", :props nil, :children ["child"]})

 (dummy>>>
  (map #(<foo :prop % > %)
       ["a" "b" "c"]))
 =>
 '({:tag "foo", :props {:prop "a"}, :children ["a"]}
   {:tag "foo", :props {:prop "b"}, :children ["b"]}
   {:tag "foo", :props {:prop "c"}, :children ["c"]})

 (dummy>>>
  (map #(<> %)
       ["a" "b" "c"]))
 =>
 `({:tag ~dummy-fragment, :props nil, :children ["a"]}
   {:tag ~dummy-fragment, :props nil, :children ["b"]}
   {:tag ~dummy-fragment, :props nil, :children ["c"]})

 (dummy>>>
  (let [Foo "FOO"]
    (map #(<Foo :prop % > %)
         ["a" "b" "c"])))
 => '({:tag "FOO", :props {:prop "a"}, :children ["a"]}
      {:tag "FOO", :props {:prop "b"}, :children ["b"]}
      {:tag "FOO", :props {:prop "c"}, :children ["c"]})

 (macroexpand
  '(dummy>>>
    (<foo> "foo")
    (<bar> "bar")
    (<baz> "baz")))
 =>
 '(do (dummy-jsx "foo" nil "foo")
      (dummy-jsx "bar" nil "bar")
      (dummy-jsx "baz" nil "baz")))

