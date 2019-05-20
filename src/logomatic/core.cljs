(ns logomatic.core
  (:require [clojure.math.combinatorics :as combo]
            [quil.core :as q :include-macros true]
            [quil.middleware :as m]))

(def fore-color [24])
(def back-color [230])

(defn setup []
  (q/frame-rate 30))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; generate logos

(defn connection-subsets [connections number]
  (->> (combo/combinations (range connections) 2)
       shuffle
       combo/subsets
       (filter #(= (count %) number))))

(def limit 36)
    
(defonce logos (atom []))

(defn ^:export generate-logos []
  (let [conns (int (.-value (.getElementById js/document "connections")))
        left-conns (int (.-value (.getElementById js/document "left-connections")))
        right-conns (int (.-value (.getElementById js/document "right-connections")))]
    (reset! logos 
            (->> (map vector
                      (take (* 20 limit) (cycle (connection-subsets conns left-conns)))
                      (take (* 20 limit) (cycle (connection-subsets conns right-conns))))
                 (take limit)
                 distinct
                 (into [])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; display params

(defn point-angle-distance [x y angle distance]
  [(+ x (* (Math/cos angle) distance))
   (- y (* (Math/sin angle) distance))])

(defn draw-triangle [center size left?]
  (q/begin-shape)
  (q/vertex 0 (- center (* 0.5 size)))
  (apply q/vertex
         (point-angle-distance 0 (- center (* 0.5 size))
                               (if left? (q/radians 220) (q/radians -40))
                               (* 0.77 size)))
  (apply q/vertex
         (point-angle-distance 0 (+ center (* 0.5 size))
                               (if left? (q/radians 140) (q/radians 40))
                               (* 0.77 size)))
  (q/vertex 0 (+ center (* 0.5 size)))
  (q/end-shape)
  (q/line 0 (- center (* 0.5 size)) 0 (+ center (* 0.5 size))))

(defn draw-side [conns scaled-ys left? triangle?]
  (doseq [[a b] conns]
    (let [size (- (nth scaled-ys b) (nth scaled-ys a))
          center (+ (* size 0.5) (nth scaled-ys a))]
      (if left?
        (if triangle?
          (draw-triangle center size true)
          (q/arc 0 center size size (q/radians 90) (q/radians 270)))
        (if triangle?
          (draw-triangle center size false)
          (q/arc 0 center size size (q/radians -90) (q/radians 90)))))))

(def phi-spacing
  {3 [0 1.8541019662610232 3]
   4 [0 1.5278640449853027 2.4721359550146973 4]
   5 [0 1.9098300562316282 2.5 3.090169943768372 5]
   6 [0 1.4163894750191373 2.2917181705809644 3.7082818294190356 5.124671304438173 6]
   7 [0 1.6524758424922823 2.6737620787242795 3.5 4.3262379212757205 5.978713763768003 7]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw []
  (apply q/background back-color)
  (let [invert? (.-checked (.getElementById js/document "invert"))
        left-triangles? (.-checked (.getElementById js/document "left-triangles"))
        right-triangles? (.-checked (.getElementById js/document "right-triangles"))
        rot (q/radians (int (.-value (.getElementById js/document "rotation"))))
        thickness (js/parseFloat (.-value (.getElementById js/document "line-width")))]
    (q/stroke-weight thickness)
    (doseq [[i j logo] (map vector
                            (cycle (range 6))
                            (mapcat (partial repeat 6) (range 6))
                            @logos)]
      (let [max-y (inc (apply max (flatten (concat (first logo) (second logo)))))
            raw-ys (range max-y) #_(phi-spacing max-y)
            ys (->> raw-ys
                    (map #(q/map-range % (first raw-ys) (last raw-ys) -50 50))
                    (into []))]
        (q/with-translation [(+ 75 (* 125 i)) (+ 75 (* 125 j))]
          (q/with-rotation [rot]
            (q/no-stroke)
            (apply q/fill (if invert? fore-color back-color))
            (draw-side (first logo) ys true left-triangles?)
            (draw-side (second logo) ys false right-triangles?)
            (q/no-fill)
            (apply q/stroke (if invert? back-color fore-color))
            (draw-side (first logo) ys true left-triangles?)
            (draw-side (second logo) ys false right-triangles?)))))))

; this function is called in index.html
(defn ^:export run-sketch []
  (generate-logos)
  (q/defsketch logomatic
    :host "logomatic"
    :size [800 800]
    :setup setup
    :draw draw))

; uncomment this line to reset the sketch:
;;(run-sketch)
