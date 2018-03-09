(defn what [x]
      (inc x))

(def y 5)

(what y)

(defn which [x y]
      (let [z (+ x y)
            (inc z)]))
