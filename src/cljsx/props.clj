(ns cljsx.props)

(defn- spread-at-end-error []
  (throw (Exception.
          "Extraneous symbol in props")))

(defn- spread-operand-error [operand]
  (throw (Exception.
          (format "Invalid spread operand `%s`"
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
           ;; TODO: Throwing this error is probably not necessary
           ;; as the FSM validation happens first.
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

(defn non-spread-symbol? [x]
  (and (symbol? x)
       (not= x '...)))

(def validator-fsm
  {:first
   {:next [:keyword :spread]
    :error "The first item must be a keyword or a spread!"}

   :keyword
   {:next [:spread :keyword :keyword-value]
    :validator keyword?}

   :keyword-value
   {:next [:keyword :spread]
    :validator (constantly true)
    :error "A keyword value can only be followed by a keyword or a spread!"}

   :spread
   {:next [:spread-value]
    :validator #(= % '...)
    :not-last "Spread must not be the last item!"
    :error "A spread must be followed by an associative or a symbol other than `...`!"}

   :spread-value
   {:next [:keyword :spread]
    :validator #(or (non-spread-symbol? %)
                    (associative? %))
    :error "Spread value can only be followed by a keyword or spread!"}})

(defn validate-item [state x]
  (let [{:keys [error next]} (validator-fsm state)]
    (or (some #(and ((get-in validator-fsm [% :validator]) x)
                    %)
              next)
        error)))

(defn validate
  ([props] (validate props :first nil))
  ([[item & others] state previous]
   (let [result (validate-item state item)]
     (if (string? result)
       (if previous
         (format "Wrong item `%s` after `%s`: %s" item previous result)
         result)
       (if (empty? others)
         (let [result (validate-item result ::last)]
           (if (string? result)
             (format "Wrong item after `%s`: %s" item result)
             result))
         (validate others result item))))))

(defn throw-when-invalid [props]
  (let [result (validate props)]
    (if (string? result)
      (throw (Exception. result))
      props)))

(defn props->mergelist [props]
  (->> props
       throw-when-invalid
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

