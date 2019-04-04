(ns example-1.main
  (:require
   [example-1.pokus :as pokus]
   ["react" :as react]
   ["react-dom" :as react-dom]))

(js/console.clear)
(js/console.log (pokus/makro {:foo "bar"}))
(js/console.log {:foo "bar"})
