(ns cljsx
  (:require
   [cljsx.core :as core]))

(def foo "FOOO")

(defmacro js-function? [form]
  `(= (-> ~form
          var
          meta
          :ns)
      (symbol "js")))

(core/defjsx jsx> jsx jsx-fragment)

(core/defjsx rsx> react/createElement react/Fragment)
