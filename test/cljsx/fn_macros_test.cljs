(ns cljsx.fn-macros-test
  (:require
   [smidjen.core :refer-macros [facts fact]]
   [cljsx.core :as sut]))

(fact "Single arity fn-clj"
      ((sut/fn-clj [x] x) 11) => 11
      ((sut/fn-clj [x y] [x y]) 11 22) => [11 22]
      ((sut/fn-clj [x y] [x y]) {:a "A"} {:b "B"})
      => [{:a "A"} {:b "B"}]

      ((sut/fn-clj [x y] [x y]) #js {:a "A"} #js {:b "B"})
      => [{:a "A"} {:b "B"}]

      ((sut/fn-clj [& more] more) 11 22 33) => [11 22 33]
      ((sut/fn-clj [x y & more] [x y more]) 11 22 33 44 55)
      => [11 22 '(33 44 55)]

      ((sut/fn-clj [& more] more) {:a "A"} {:b "B"} {:c "C"})
      => '({:a "A"} {:b "B"} {:c "C"})

      ((sut/fn-clj [& more] more) #js{:a "A"} #js{:b "B"} #js{:c "C"})
      => '({:a "A"} {:b "B"} {:c "C"})

      ((sut/fn-clj [x & more] [x more]) {:a "A"} {:b "B"} {:c "C"})
      => '[{:a "A"} ({:b "B"} {:c "C"})]

      ((sut/fn-clj [x & more] [x more]) #js{:a "A"} #js{:b "B"} #js{:c "C"})
      => '[{:a "A"} ({:b "B"} {:c "C"})]

      (clojure.string/includes? (str (sut/fn-clj foobarbaz [])) "foobarbaz")
      => true

      ((sut/fn-clj foo [x] x) 11) => 11
      ((sut/fn-clj foo [x y] [x y]) 11 22) => [11 22]
      ((sut/fn-clj foo [x y] [x y]) {:a "A"} {:b "B"})
      => [{:a "A"} {:b "B"}]

      ((sut/fn-clj foo [x y] [x y]) #js {:a "A"} #js {:b "B"})
      => [{:a "A"} {:b "B"}]

      ((sut/fn-clj foo [& more] more) 11 22 33) => [11 22 33]
      ((sut/fn-clj foo [x y & more] [x y more]) 11 22 33 44 55)
      => [11 22 '(33 44 55)]

      ((sut/fn-clj foo [& more] more) {:a "A"} {:b "B"} {:c "C"})
      => '({:a "A"} {:b "B"} {:c "C"})

      ((sut/fn-clj foo [& more] more) #js{:a "A"} #js{:b "B"} #js{:c "C"})
      => '({:a "A"} {:b "B"} {:c "C"})

      ((sut/fn-clj foo [x & more] [x more]) {:a "A"} {:b "B"} {:c "C"})
      => '[{:a "A"} ({:b "B"} {:c "C"})]

      ((sut/fn-clj foo [x & more] [x more]) #js{:a "A"} #js{:b "B"} #js{:c "C"})
      => '[{:a "A"} ({:b "B"} {:c "C"})])

(def multi-arity-fn-clj
  (sut/fn-clj ([] "nullary")
              ([x] ["unary" x])
              ([x y] ["binary" x y])
              ([x y & z] ["variadic" x y z])))

(def multi-arity-fn-clj-named
  (sut/fn-clj ([] "nullary")
              ([x] ["unary" x])
              ([x y] ["binary" x y])
              ([x y & z] ["variadic" x y z])))

(fact "Multi-arity fn-clj"
      (multi-arity-fn-clj) => "nullary"
      (multi-arity-fn-clj-named) => "nullary"

      (multi-arity-fn-clj {"a" 11}) => ["unary" {"a" 11}]
      (multi-arity-fn-clj #js{"a" 11}) => ["unary" {:a 11}]
      (multi-arity-fn-clj-named {"a" 11}) => ["unary" {"a" 11}]
      (multi-arity-fn-clj-named #js{"a" 11}) => ["unary" {:a 11}]

      (multi-arity-fn-clj {"a" 11} {"b" 22}) => ["binary" {"a" 11} {"b" 22}]
      (multi-arity-fn-clj #js{"a" 11} #js{"b" 22}) => ["binary" {:a 11} {:b 22}]
      (multi-arity-fn-clj-named {"a" 11} {"b" 22})
      => ["binary" {"a" 11} {"b" 22}]
      (multi-arity-fn-clj-named #js{"a" 11} #js{"b" 22})
      => ["binary" {:a 11} {:b 22}]

      (multi-arity-fn-clj {"a" 11} {"b" 22} {"c" 33} {"d" 44})
      => ["variadic" {"a" 11} {"b" 22} '({"c" 33} {"d" 44})]
      (multi-arity-fn-clj #js{"a" 11} #js{"b" 22} #js{"c" 33} #js{"d" 44})
      => ["variadic" {:a 11} {:b 22} '({:c 33} {:d 44})]
      (multi-arity-fn-clj-named {"a" 11} {"b" 22} {"c" 33} {"d" 44})
      => ["variadic" {"a" 11} {"b" 22} '({"c" 33} {"d" 44})]
      (multi-arity-fn-clj-named #js{"a" 11} #js{"b" 22} #js{"c" 33} #js{"d" 44})
      => ["variadic" {:a 11} {:b 22} '({:c 33} {:d 44})])

(sut/defn-clj single-arity-defn-clj [x & y]
  [x y])

(sut/defn-clj single-arity-defn-clj-docstring
  "Lorem ipsum"
  [x & y]
  [x y])

(sut/defn-clj single-arity-defn-clj-meta
  {:foo "bar"}
  [x & y]
  [x y])

(sut/defn-clj single-arity-defn-clj-docstring+meta
  "Lorem ipsum"
  {:foo "bar"}
  [x & y]
  [x y])

(sut/defn-clj single-arity-defn-prepost
  [x y]
  {:pre [(number? y)]
   :post [(map? %)]}
  x)

(fact "Single arity defn-clj"
      (single-arity-defn-clj {"a" 11} {"b" 22} {"c" 33})
      => '[{"a" 11} ({"b" 22} {"c" 33})]
      (single-arity-defn-clj #js{"a" 11} #js{"b" 22} #js{"c" 33})
      => '[{:a 11} ({:b 22} {:c 33})]
      (single-arity-defn-clj-docstring {"a" 11} {"b" 22} {"c" 33})
      => '[{"a" 11} ({"b" 22} {"c" 33})]
      (single-arity-defn-clj-docstring #js{"a" 11} #js{"b" 22} #js{"c" 33})
      => '[{:a 11} ({:b 22} {:c 33})]
      (single-arity-defn-clj-meta {"a" 11} {"b" 22} {"c" 33})
      => '[{"a" 11} ({"b" 22} {"c" 33})]
      (single-arity-defn-clj-meta #js{"a" 11} #js{"b" 22} #js{"c" 33})
      => '[{:a 11} ({:b 22} {:c 33})]
      (single-arity-defn-clj-docstring+meta {"a" 11} {"b" 22} {"c" 33})
      => '[{"a" 11} ({"b" 22} {"c" 33})]
      (single-arity-defn-clj-docstring+meta #js{"a" 11} #js{"b" 22} #js{"c" 33})
      => '[{:a 11} ({:b 22} {:c 33})]

      (-> #'single-arity-defn-clj-docstring meta :doc) => "Lorem ipsum"
      (-> #'single-arity-defn-clj-meta meta :foo) => "bar"
      (-> #'single-arity-defn-clj-docstring+meta meta :doc) => "Lorem ipsum"
      (-> #'single-arity-defn-clj-docstring+meta meta :foo) => "bar"

      (single-arity-defn-prepost {"a" 11} 123) => {"a" 11}

      (try (single-arity-defn-prepost {"a" 11} "foo")
           (catch js/Object e
             (.-message e)))
      => "Assert failed: (number? y)"

      (try (single-arity-defn-prepost "foo" 123)
           (catch js/Object e
             (.-message e)))
      => "Assert failed: (map? %)")

(sut/defn-clj multi-arity-defn-clj
  ([] "nullary")
  ([x] ["unary" x])
  ([x y] ["binary" x y])
  ([x y & z] ["variadic" x y z]))

(sut/defn-clj multi-arity-defn-clj-meta
  {:foo "bar"}
  ([] "nullary")
  ([x] ["unary" x])
  ([x y] ["binary" x y])
  ([x y & z] ["variadic" x y z]))

(sut/defn-clj multi-arity-defn-clj-docstring
  "Lorem ipsum"
  ([] "nullary")
  ([x] ["unary" x])
  ([x y] ["binary" x y])
  ([x y & z] ["variadic" x y z]))

(sut/defn-clj multi-arity-defn-clj-docstring+meta
  "Lorem ipsum"
  {:foo "bar"}
  ([] "nullary")
  ([x] ["unary" x])
  ([x y] ["binary" x y])
  ([x y & z] ["variadic" x y z]))

(fact "Multi-arity defn-clj"
      (-> #'multi-arity-defn-clj-meta meta :foo) => "bar"
      (-> #'multi-arity-defn-clj-docstring meta :doc) => "Lorem ipsum"
      (-> #'multi-arity-defn-clj-docstring+meta meta :foo) => "bar"
      (-> #'multi-arity-defn-clj-docstring+meta meta :doc) => "Lorem ipsum"

      (multi-arity-defn-clj) => "nullary"
      (multi-arity-defn-clj-meta) => "nullary"
      (multi-arity-defn-clj-docstring) => "nullary"
      (multi-arity-defn-clj-docstring+meta) => "nullary"

      (multi-arity-defn-clj {"a" 11}) => ["unary" {"a" 11}]
      (multi-arity-defn-clj #js{"a" 11}) => ["unary" {:a 11}]
      (multi-arity-defn-clj-meta {"a" 11}) => ["unary" {"a" 11}]
      (multi-arity-defn-clj-meta #js{"a" 11}) => ["unary" {:a 11}]
      (multi-arity-defn-clj-docstring {"a" 11}) => ["unary" {"a" 11}]
      (multi-arity-defn-clj-docstring #js{"a" 11}) => ["unary" {:a 11}]
      (multi-arity-defn-clj-docstring+meta {"a" 11}) => ["unary" {"a" 11}]
      (multi-arity-defn-clj-docstring+meta #js{"a" 11}) => ["unary" {:a 11}]

      (multi-arity-defn-clj {"a" 11} {"b" 22}) => ["binary" {"a" 11} {"b" 22}]
      (multi-arity-defn-clj #js{"a" 11} #js{"b" 22})
      => ["binary" {:a 11} {:b 22}]
      (multi-arity-defn-clj-meta {"a" 11} {"b" 22})
      => ["binary" {"a" 11} {"b" 22}]
      (multi-arity-defn-clj-meta #js{"a" 11} #js{"b" 22})
      => ["binary" {:a 11} {:b 22}]
      (multi-arity-defn-clj-docstring {"a" 11} {"b" 22})
      => ["binary" {"a" 11} {"b" 22}]
      (multi-arity-defn-clj-docstring #js{"a" 11} #js{"b" 22})
      => ["binary" {:a 11} {:b 22}]
      (multi-arity-defn-clj-docstring+meta {"a" 11} {"b" 22})
      => ["binary" {"a" 11} {"b" 22}]
      (multi-arity-defn-clj-docstring+meta #js{"a" 11} #js{"b" 22})
      => ["binary" {:a 11} {:b 22}]

      (multi-arity-defn-clj {"a" 11} {"b" 22} {"c" 33} {"d" 44})
      => ["variadic" {"a" 11} {"b" 22} '({"c" 33} {"d" 44})]
      (multi-arity-defn-clj #js{"a" 11} #js{"b" 22} #js{"c" 33} #js{"d" 44})
      => ["variadic" {:a 11} {:b 22} '({:c 33} {:d 44})]
      (multi-arity-defn-clj-meta {"a" 11} {"b" 22} {"c" 33} {"d" 44})
      => ["variadic" {"a" 11} {"b" 22} '({"c" 33} {"d" 44})]
      (multi-arity-defn-clj-meta #js{"a" 11}
                                 #js{"b" 22}
                                 #js{"c" 33}
                                 #js{"d" 44})
      => ["variadic" {:a 11} {:b 22} '({:c 33} {:d 44})]
      (multi-arity-defn-clj-docstring {"a" 11} {"b" 22} {"c" 33} {"d" 44})
      => ["variadic" {"a" 11} {"b" 22} '({"c" 33} {"d" 44})]
      (multi-arity-defn-clj-docstring #js{"a" 11}
                                      #js{"b" 22}
                                      #js{"c" 33}
                                      #js{"d" 44})
      => ["variadic" {:a 11} {:b 22} '({:c 33} {:d 44})]
      (multi-arity-defn-clj-docstring+meta {"a" 11} {"b" 22} {"c" 33} {"d" 44})
      => ["variadic" {"a" 11} {"b" 22} '({"c" 33} {"d" 44})]
      (multi-arity-defn-clj-docstring+meta #js{"a" 11}
                                           #js{"b" 22}
                                           #js{"c" 33}
                                           #js{"d" 44})
      => ["variadic" {:a 11} {:b 22} '({:c 33} {:d 44})])
