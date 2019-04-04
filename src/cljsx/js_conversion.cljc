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
  #?(:clj {:fuuck "FUUUUCK"}
     :cljs {:jjj "SSS"}))

#_(defn $clj->js-when-js [jsx-symbol form]
  #?(:clj {:is-fucking-clj "IS FUCKING CLJ!!!"}
     :cljs `(if ~($js? jsx-symbol)
              {:is-js "IS JS"}
              {:not-js "NOT JS"})))

#_(defn $clj->js-when-js_XXX [jsx-symbol form]
  #?(:clj form
     :cljs `(if ~($js? jsx-symbol)
              (~'clj->js ~form)
              ~form)))

(defn $props->clj-when-js [component]
  #?(:clj component
     :cljs `(if ~($js? component)
              ~component
              ~component
              #_(fn [props#]
                (~component (~'js->clj props#))))))
