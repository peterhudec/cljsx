(ns cljsx.props)

(defn- spread-at-end-error []
  (throw (#?(:clj Exception.
             :cljs js/Error.)
          "Invalid spread at the end of props")))

(defn- spread-operand-error [operand]
  (throw (#?(:clj Exception.
             :cljs js/Error.)
          (str "Invalid spread operand "
               (pr-str operand))
          #_(format "Invalid spread operand `%s`"
                  (pr-str operand)))))

(defn valid-spread-operand? [x]
  (or (and (symbol? x)
           (not= x '...))
      (and (list? x)
           (not= x ()))
      (nil? x)
      (associative? x)))

(defn split-spread
  "Splits a list of props on occurences on the spread
  operator `'...` so that the next item after `'...`
  is grouped with the operator."
  [x]
  (remove
   empty?
   (let [[left right] (split-with #(not= % '...) x)]
     (if (empty? right)
       (list left)
       (let [[spread the-rest] (split-at 2 right)
             [_ operand] spread]
         (case (count spread)
           1 (spread-at-end-error)
           2 (when-not (valid-spread-operand? operand)
               (spread-operand-error operand)))
         (concat
          [left]
          [spread]
          (split-spread the-rest)))))))

(defn props->map [props]
  (->> props
       (partition-all 2 1)
       (reduce (fn [a [k v :as pair]]
                 (if (keyword? k)
                   (assoc a k
                          (if (or (keyword? v)
                                  (= (count pair) 1))
                            true
                            v))
                   a))
               {})))

(defn props->mergelist [props]
  (->> props
       split-spread
       (map #(if (= (first %) '...)
               (second %)
               (props->map %)))
       ;; Remove all empty maps
       (filter #(not= % {}))
       ;; Return nil rather than empty list
       (#(if (empty? %) nil %))))

(defn list->props&children [l]
  (let [[x [_ & xs]] (split-with #(not= % '>) l)]
    [x xs]))

