(ns incanter.charts-3d
  (:use [incanter core charts])
  (:import [org.jfree.chart ChartFactory]
           [org.jfree.chart.plot PiePlot3D]
           [org.jfree.data.general PieDataset DefaultPieDataset]))

(defn- data-as-list
  "
  data-as-list [x data]

  If x is a collection, return it
  If x is a single value, and data is undefined, return x in vector
  If x is a single value, and data is defined, return ($ x data)
  "
  [x data]
  (if (coll? x)
    (to-list x)
    (if data
      (let [selected ($ x data)]
        (if (coll? selected)
          selected
          [selected]))
      [x])))

(defn- pie-slider [v]
  (let [chart (.. v getChartPanel getChart)
        w (.. v getChartPanel getWidth)
        h (.. v getChartPanel getHeight)
        slider-values (range 0.0 0.9 0.025)
        updater-fn (fn [df]
                     (.. chart getPlot (setDepthFactor df))
                     (.. v  getChartPanel (setSize w (- h (* df 300)))))
        max-idx (dec (count slider-values))
        slider (doto (javax.swing.JSlider. javax.swing.JSlider/VERTICAL 0 max-idx 0)
                 (.addChangeListener (proxy [javax.swing.event.ChangeListener] []
                                       (stateChanged [^javax.swing.event.ChangeEvent event]
                                                     (let [source (.getSource event)
                                                           value (nth slider-values (.getValue source))]
                                                       (updater-fn value))))))
        panel (javax.swing.JPanel. (java.awt.BorderLayout.))]
    (doto panel
      (.add (. v getChartPanel))
      (.add slider java.awt.BorderLayout/EAST))
    (. v setContentPane panel)
    (. v setSize (+ (. v getWidth) (. slider getWidth)) (. v getHeight))
    (.revalidate v)
    slider))

(defn pie-chart-3d*
  ([categories values & options]
    (let [opts (when options (apply assoc {} options))
          data (or (:data opts) $data)
          _values (data-as-list values data)
          _categories (data-as-list categories data)
          title (or (:title opts) "")
          theme (or (:theme opts) :default)
          legend? (true? (:legend opts))
          dataset (DefaultPieDataset.)
          chart (ChartFactory/createPieChart3D
                  title
                  dataset
                  legend?
                  false
                  false)]
      (do
        (doseq [i (range 0 (count _values))]
          (.setValue dataset (nth _categories i) (nth _values i)))
        (set-theme chart theme)
        (.. chart getPlot (setDepthFactor 0.0))
        chart))))

(defmacro pie-chart-3d
  ([categories values & options]
    `(let [opts# ~(when options (apply assoc {} options))
           title# (or (:title opts#) "")
           args# (concat [~categories ~values]
                         (apply concat (seq (apply assoc opts#
                                                   [:title title#]))))]
       (apply pie-chart-3d* args#))))

(when-let [original-view-fn (get-method view org.jfree.chart.JFreeChart)]
  (defmethod view org.jfree.chart.JFreeChart [chart & options]
    (let [view (apply original-view-fn chart options)]
      (when (= (type (.getPlot chart)) org.jfree.chart.plot.PiePlot3D)
        (pie-slider view))
      view)))


