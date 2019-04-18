(ns cljsx.conversion)

(defn intercept-jsx* [jsx & _] jsx)

(defmacro js?
  "Checks whether `x` is a JavaScript value.
  Returns `nil` if it can't be determined if it's
  a JavaScript or Clojure value."
  [x]
  (let [locals (:locals &env)
        defs (get-in &env [:ns :defs])
        all (merge locals defs)
        entry (all x)]
    (if-not entry
      ;; If there's no entry, it's a JS object
      true
      ;; Otherwise...
      (case (:tag entry)
        ;; If tag is 'js we can be sure it's a JS value.
        js true
        ;; If tag is 'any, we can't tell,
        ;; This is probably a value returned from a function.
        any nil
        ;; If tag is nil, CLJ values have some additional keys.
        ;; We can for example check on the presence of :meta
        nil (if (:meta entry)
              ;; If there is the :meta key, it is a CLJ value
              false
              ;; Otherwise we can't tell
              nil)
        ;; If tag is anything except for the above,
        ;; it's a CLJ value.
        false))))

