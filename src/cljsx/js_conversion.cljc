(ns cljsx.js-conversion)

(defn $js? [form]
  (when (symbol? form)
    `(let [meta# (-> ~form
                     var
                     meta)]
       (or (= (:ns meta#) (symbol "js"))
           ;; This is only for testing and convenience
           (:js meta#)))))

(defn $clj->js-when-js [jsx-symbol form]
  #?(:clj form
     :cljs `(if ~($js? jsx-symbol)
              (~'clj->js ~form)
              ~form)))

