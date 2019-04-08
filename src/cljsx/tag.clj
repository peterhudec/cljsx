(ns cljsx.tag)

(defn fragment? [x]
  (= x "<>"))

(def simple?
  (comp
   second
   (partial re-matches #"<([^<>]+)>")))

(def props?
  (comp
   second
   (partial re-matches #"<([^<>]+)")))

(def intrinsic?
  (partial re-matches #"[a-z0-9]+"))

(def reference?
  (partial re-matches
           #"(?:[\w.-]+/)?(?:[\w]+\.)*[A-Z][><:+&'*\-\w]*"))

(defn resolve-tag [s]
  (if-let [intrinsic-tag (intrinsic? s)]
    intrinsic-tag
    (if-let [reference-tag (reference? s)]
      (symbol reference-tag)
      (throw (Exception.
              (format "Invalid tag: <%s>" s))))))

(defn needs-conversion? [x]
  (->> x
       (re-matches #"(.*[/])?[\w$.]+$")
       boolean))

(needs-conversion? "foo")
(needs-conversion? "foo/Bar")
(needs-conversion? "foo/foo.Bar")
(needs-conversion? "foo/foo.Bar_baz")
(needs-conversion? "foo/foo.Bar$baz")

(needs-conversion? "foo/foo.Bar*baz")
(needs-conversion? "foo/foo.Bar-baz")

(def p #"(?:[\w.-]+/)?(?:[\w]+\.)*[A-Z]\w*")

(re-matches p "foo")
(re-matches p "foo/bar")
(re-matches p "foo.bar/bar")
(re-matches p "foo/bar.bar")
(re-matches p "foo/bar.Bar")
(re-matches p "foo/bar.Bar*")
(re-matches p "foo/bar.Bar'")
(re-matches p "foo/bar.Bar&")
(re-matches p "foo/bar.Bar-")


