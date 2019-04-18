(ns cljsx.conversion)

(defn intercept-jsx* [jsx & _] jsx)

(defn- js?* [x &env]
  (let [locals (:locals &env)
        defs (get-in &env [:ns :defs])
        all (merge locals defs)
        tag (get-in all [x :tag])]
    (boolean (or (= tag 'js)
                 (nil? tag)))))

(defmacro js?
  "
  If `(get all x)` is nil, this is a JS function,
  else if `tag` (:tag (get all x)) is `js`, this is a JS function,
  else if `tag` is `any`, this CAN BE a JS function.
  "
  [x]
  #_(js?* x &env)
  (let [locals (:locals &env)
        defs (get-in &env [:ns :defs])
        all (merge locals defs)
        entry (get all x)
        tag (:tag entry)]
    (if entry
      (if (contains? entry :tag)
        (cond
          (= tag 'js) "SURELY JS (tag is 'js)"
          (= tag 'any) "NOT SURE (tag is 'any)"
          (= tag nil) (str "NOT SURE (tag is " (pr-str tag) ")")
          :else (str "SURELY CLJ (tag is " (pr-str tag) ")"))
        "SURELY CLJ (no tag)")
      "SURELY JS (no entry)")
    #_(with-out-str (clojure.pprint/pprint (get all x)))))

(defmacro clj? [x]
  (not (js?* x &env)))

(contains? {:a nil} :b)

(pr-str 'foo)
