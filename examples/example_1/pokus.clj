(ns example-1.pokus)

(defn convert* [x] x)

(defmacro makro [form]
  `(convert* ~form))

(macroexpand '(makro 123))

(makro 123)
