(ns cljsx.core-test
  (:require [midje.sweet :refer :all]
            [cljsx.core :refer :all]
            [cljsx.encoding]))

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

(def walk (walk-factory "jsx" "jsx-fragment"))

(defn encoded [props]
  `(cljsx.encoding/encode-props*
    (meta (var ~'jsx))
    ~props))

(facts
 "Walk"
 (fact
  "Leaves non-JSX expressions intact"
  (walk '(foo bar baz))
  => '(foo bar baz))

 (fact
  "JSX Fragment"
  (walk `(~'<> "foo" "bar"))
  => `(~'jsx ~'jsx-fragment ~(encoded nil) "foo" "bar"))

 (fact
  "Simple JSX"
  (walk `(~'<foo> "bar" "baz"))
  => `(~'jsx "foo" ~(encoded nil) "bar" "baz"))

  (walk `(~'<foo/Bar> "bar" "baz"))
  => `(~'jsx ~'foo/Bar ~(encoded nil) "bar" "baz")

  (walk `(~'<foo.Bar> ~'bar ~'baz))
  => `(~'jsx ~'foo.Bar ~(encoded nil) ~'bar ~'baz))

 (fact
  "Props JSX"
  (walk
   `(~'<foo ~'> ~'bar ~'baz))
  => `(~'jsx "foo" ~(encoded nil) ~'bar ~'baz)

  (walk
   `(~'<foo :a "A" :b ~'> ~'bar ~'baz))
  => `(~'jsx "foo"
       ~(encoded {:a "A" :b true})
       ~'bar
       ~'baz)

  (walk
   `(~'<foo/Bar :a :b :c ~'>
     ~'bar ~'baz))
  => `(~'jsx ~'foo/Bar
       ~(encoded {:a true :b true :c true})
       ~'bar
       ~'baz)

  (walk
   `(~'<foo.Bar
     :a "A"
     ~'... ~'x
     :b "B"
     :a "AA" ~'>
     ~'bar
     ~'baz))
  => `(~'jsx
       ~'foo.Bar
       ~(encoded `(merge
                   {:a "A"}
                   ~'x
                   {:a "AA" :b "B"}))
       ~'bar
       ~'baz)

  (fact
   "Recursion"
   (walk
    `(~'foo
      (~'<bar> ~'baz))) =>
   `(~'foo
     (~'jsx "bar" ~(encoded nil) ~'baz))

   (walk
    `(~'foo
      (~'<bar> ~'baz)
      ~'bing)) =>
   `(~'foo
     (~'jsx "bar" ~(encoded nil) ~'baz)
     ~'bing))

  (walk
   `(~'foo
     ~'bing
     (~'<bar> ~'baz))) =>
  `(~'foo
    ~'bing
    (~'jsx "bar" ~(encoded nil) ~'baz))

  (walk
   `(~'foo
     ~'bing
     (~'<bar> ~'baz))) =>
  `(~'foo
    ~'bing
    (~'jsx "bar" ~(encoded nil) ~'baz))

  (walk
   `(~'<foo> ~'baz
           (~'<bar> ~'bing))) =>
  `(~'jsx "foo" ~(encoded nil)
          ~'baz
    (~'jsx "bar" ~(encoded nil)
                 ~'bing))

  (walk
   `(~'<foo> ~'foo
           (~'<bar> ~'bar
                  (~'<baz> ~'baz)))) =>
  `(~'jsx "foo" ~(encoded nil)
          ~'foo
    (~'jsx "bar" ~(encoded nil)
                 ~'bar
     (~'jsx "baz" ~(encoded nil)
                        ~'baz)))

  (walk
   `[111
     (~'<two> 222)
     333
     (~'<four> 444)
     555])
  =>
  `[111
    (~'jsx "two" ~(encoded nil) 222)
    333
    (~'jsx "four" ~(encoded nil) 444)
    555]

  (walk
   `#{111
      (~'<two> 222)
      333
      (~'<four> 444)
      555})
  =>
  `#{111
     (~'jsx "two" ~(encoded nil) 222)
    333
     (~'jsx "four" ~(encoded nil) 444)
    555}

  (walk
   `{:a "A"
     :b (~'<b> ~'b)
     (~'<c> ~'c) "C"
     :d (~'<d> ~'d)
     :e "E"})
  =>
  `{:a "A"
    :b (~'jsx "b" ~(encoded nil) ~'b)
    (~'jsx "c" ~(encoded nil) ~'c) "C"
    :d (~'jsx "d" ~(encoded nil) ~'d)
    :e "E"}

  (walk
   `(~'<foo
     :bar (~'<bar> ~'bar)
     :a "A"
     :baz (~'<baz :c "C"
           :bing (~'<bing> ~'bing)
           :d "D" ~'>
           ~'baz)
     :b ~'>
     ~'foo))
  =>
  `(~'jsx "foo"
    ~(encoded
      `{:bar (~'jsx "bar" ~(encoded nil) ~'bar)
        :a "A"
        :baz (~'jsx "baz"
              ~(encoded
                `{:c "C"
                  :bing (~'jsx
                         "bing"
                         ~(encoded nil)
                         ~'bing)
                  :d "D"})
              ~'baz)
        :b true})
    ~'foo)

  (walk
   `(~'<foo :a "A"
          :bar (~'<bar> ~'bar)
          :b
          ~'... ~'xxx
          :c "C"
          :baz (~'<baz> ~'baz)
          :d ~'>
          ~'foo))
  =>
  `(~'jsx "foo"
    ~(encoded
      `(merge
        {:a "A"
         :bar (~'jsx "bar" ~(encoded nil) ~'bar)
         :b true}
        ~'xxx
        {:c "C"
         :baz (~'jsx "baz" ~(encoded nil) ~'baz)
         :d true}))
    ~'foo))

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
 )


(macroexpand
 '(my> (<foo :a "A" :b "B">)))
;; => (my-jsx
;;     "foo"
;;     (if
;;      (clojure.core/let
;;       [meta__5246__auto__ (clojure.core/-> my-jsx var clojure.core/meta)]
;;       (clojure.core/or
;;        (clojure.core/= (:ns meta__5246__auto__) (clojure.core/symbol "js"))
;;        (clojure.core/= (:ns' meta__5246__auto__) (clojure.core/symbol "js"))
;;        (:js meta__5246__auto__)))
;;      (clj->js FOOOOO)
;;      FOOOOO)
;;     {:a "A", :b "B"})

(if true
  "TRUE"
  "FALSE")

('clj->js (ns-publics *ns*))
('+ (ns-publics *ns*))

(ns-refers *ns*)

(defn foo [x] [x x])

(ns-resolve *ns* 'clj->js)
(ns-resolve *ns* 'foo)

(if true
  ((or (ns-resolve *ns* 'clj->js) identity) "FOO")
  "nist")
