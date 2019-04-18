(ns cljsx.conversion
  (:require-macros [cljsx.core :as core]
                   [cljsx.conversion :refer [js?]]))

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

#_(defn intercept-jsx-2* [jsx]
  (fn [tag props & children]
    (js/console.log "==============")
    (js/console.log "INTERCEPTED")
    (js/console.log "tag:" tag)
    ;(js/console.log "js?" (core/js? tag))
    (js/console.log "wtf" (c/js? tag))
    (js/console.log "--------------")
    (apply jsx tag props children)))

(defn needs-conversion? [tag]
  (or #_(string? tag)
      (js? tag)))
