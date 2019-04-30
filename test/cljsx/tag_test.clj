(ns cljsx.tag-test
  (:require [midje.sweet :refer :all]
            [clojure.test :refer :all]
            [cljsx.tag :refer :all :as tag]))

(fact
 "Fragment tag"
 (fragment? "<>") => true
 (fragment? "foo") => false
 (fragment? "<foo") => false
 (fragment? "foo>") => false
 (fragment? "<foo>") => false)

(facts
 "Simple tag"
 (fact
  "Any string wrapped in < and > is a simple tag."
  (simple? "<foo>") => "foo"
  (simple? "<foo.bar>") => "foo.bar"
  (simple? "<foo bar baz bing>") => "foo bar baz bing")

 (fact
  "Anything else is not a simple tag"
  (simple? "<foo") => nil
  (simple? "foo.bar>") => nil
  (simple? "foo bar baz bing") => nil))

(facts
 "Props tag"
 (fact
  (str "Any string starting with < but not ending with >"
       "is a \"props\" tag")
  (props? "<foo") => "foo"
  (props? "<foo.bar") => "foo.bar"
  (props? "<foo bar baz/bing") => "foo bar baz/bing")
 (fact
  "Anyting else is not a props tag"
  (props? "foo") => nil
  (props? "foo>") => nil
  (props? "<foo>") => nil))

(fact
 "Resolve tag"
 (resolve-tag "foo") => "foo"
 (resolve-tag "Foo") => 'Foo
 (resolve-tag "foo.Bar") => 'foo.Bar
 (resolve-tag "foo.bar.Baz") => 'foo.bar.Baz
 (resolve-tag "foo.bar/baz.Bing") => 'foo.bar/baz.Bing
 (resolve-tag "Foo.Bar/Baz.Bing") => 'Foo.Bar/Baz.Bing

 (resolve-tag "Foo*") => 'Foo*
 (resolve-tag "Foo*bar") => 'Foo*bar
 (resolve-tag "Foo-") => 'Foo-
 (resolve-tag "Foo-bar") => 'Foo-bar
 (resolve-tag "Foo'") => 'Foo'
 (resolve-tag "Foo'bar") => 'Foo'bar
 (resolve-tag "Foo&") => 'Foo&
 (resolve-tag "Foo&bar") => 'Foo&bar
 (resolve-tag "Foo+") => 'Foo+
 (resolve-tag "Foo+bar") => 'Foo+bar
 (resolve-tag "Foo<") => 'Foo<
 (resolve-tag "Foo<bar") => 'Foo<bar
 (resolve-tag "Foo>") => 'Foo>
 (resolve-tag "Foo>bar") => 'Foo>bar
 (resolve-tag "Foo>") => 'Foo>
 (resolve-tag "Foo>bar") => 'Foo>bar
 (resolve-tag "foo.bar") =>
 (throws #"Invalid tag: <foo.bar>")

 (resolve-tag "foo/bar") =>
 (throws #"Invalid tag: <foo/bar>")

 (resolve-tag "Foo.bar") =>
 (throws #"Invalid tag: <Foo.bar>")

 (resolve-tag "foo.Bar/baz") =>
 (throws #"Invalid tag: <foo.Bar/baz>"))

