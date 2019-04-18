(ns pokus-2)

(defmacro m [form]
  (println (str "pokus-2/m: " (boolean &env)))
  form)
