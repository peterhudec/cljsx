(ns cljsx.conversion)

(defn intercept-jsx* [jsx jsx-needs-conversion? tag-needs-conversion?]
  (if jsx-needs-conversion?
    ;; If `jsx` is for example react/createElement,
    ;; we intercept it and...
    (fn [tag props & children]
      (apply jsx
             (if tag-needs-conversion?
               ;; ...if the tag is a JS function,
               ;; or an intrinsic tag e.g "div",
               ;; we keep it as it is,...
               tag
               ;; ...but if the tag is a Clojure function,
               ;; we return a function which calls the
               ;; Clojure tag with Clojure props.
               #(tag props))
             ;; If the tag is a JS function,
             ;; we need to convert the props to JS.
             (clj->js props)
             children))
    ;; If `jsx` is a Clojure function, we leave it unchanged.
    jsx))
