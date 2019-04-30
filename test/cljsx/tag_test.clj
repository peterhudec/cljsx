(ns cljsx.tag-test
  (:require [midje.sweet :refer [fact facts throws]]
            [cljsx.tag :as sut]))

(fact
 "Fragment tag"
 (sut/fragment? "<>") => true
 (sut/fragment? "foo") => false
 (sut/fragment? "<foo") => false
 (sut/fragment? "foo>") => false
 (sut/fragment? "<foo>") => false)

(facts
 "Simple tag"
 (fact
  "Any string wrapped in < and > is a simple tag."
  (sut/simple? "<foo>") => "foo"
  (sut/simple? "<foo.bar>") => "foo.bar"
  (sut/simple? "<foo bar baz bing>") => "foo bar baz bing")

 (fact
  "Anything else is not a simple tag"
  (sut/simple? "<foo") => nil
  (sut/simple? "foo.bar>") => nil
  (sut/simple? "foo bar baz bing") => nil))

(facts
 "Props tag"
 (fact
  (str "Any string starting with < but not ending with >"
       "is a \"props\" tag")
  (sut/props? "<foo") => "foo"
  (sut/props? "<foo.bar") => "foo.bar"
  (sut/props? "<foo bar baz/bing") => "foo bar baz/bing")
 (fact
  "Anyting else is not a props tag"
  (sut/props? "foo") => nil
  (sut/props? "foo>") => nil
  (sut/props? "<foo>") => nil))

(fact
 "Resolve tag"
 (sut/resolve-tag "foo") => "foo"
 (sut/resolve-tag "Foo") => 'Foo
 (sut/resolve-tag "foo.Bar") => 'foo.Bar
 (sut/resolve-tag "foo.bar.Baz") => 'foo.bar.Baz
 (sut/resolve-tag "foo.bar/baz.Bing") => 'foo.bar/baz.Bing
 (sut/resolve-tag "Foo.Bar/Baz.Bing") => 'Foo.Bar/Baz.Bing

 (sut/resolve-tag "Foo*") => 'Foo*
 (sut/resolve-tag "Foo*bar") => 'Foo*bar
 (sut/resolve-tag "Foo-") => 'Foo-
 (sut/resolve-tag "Foo-bar") => 'Foo-bar
 (sut/resolve-tag "Foo'") => 'Foo'
 (sut/resolve-tag "Foo'bar") => 'Foo'bar
 (sut/resolve-tag "Foo&") => 'Foo&
 (sut/resolve-tag "Foo&bar") => 'Foo&bar
 (sut/resolve-tag "Foo+") => 'Foo+
 (sut/resolve-tag "Foo+bar") => 'Foo+bar
 (sut/resolve-tag "Foo<") => 'Foo<
 (sut/resolve-tag "Foo<bar") => 'Foo<bar
 (sut/resolve-tag "Foo>") => 'Foo>
 (sut/resolve-tag "Foo>bar") => 'Foo>bar
 (sut/resolve-tag "Foo>") => 'Foo>
 (sut/resolve-tag "Foo>bar") => 'Foo>bar
 (sut/resolve-tag "foo.bar") =>
 (throws #"Invalid tag: <foo.bar>")

 (sut/resolve-tag "foo/bar") =>
 (throws #"Invalid tag: <foo/bar>")

 (sut/resolve-tag "Foo.bar") =>
 (throws #"Invalid tag: <Foo.bar>")

 (sut/resolve-tag "foo.Bar/baz") =>
 (throws #"Invalid tag: <foo.Bar/baz>"))

