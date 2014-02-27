(ns incanter.charts-3d
  (:require [clojure.java.io :as io])
  (:use [incanter core charts])
  (:import [org.jfree.chart ChartFactory]
           [org.jfree.chart.plot PiePlot3D]
           [org.jfree.data.general PieDataset DefaultPieDataset]
           [javax.imageio ImageIO ImageTypeSpecifier IIOImage]
           [javax.imageio.metadata IIOMetadataNode]
           [javax.imageio.stream FileImageOutputStream]
           [java.awt.image BufferedImage]))

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

(defn- get-writer []
  (let [writers (iterator-seq (ImageIO/getImageWritersBySuffix "gif"))]
    (first writers)))

(defn- get-node [root-node node-name]
  (let [nodes-cnt (.getLength root-node)]
    (if-let [item-idx (seq (filter #(= (.compareToIgnoreCase (.. root-node (item %) getNodeName) node-name) 0) (range nodes-cnt)))]
      (.item root-node (first item-idx))
      (let [node (IIOMetadataNode. node-name)]
        (.appendChild root-node node)
        node))))

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


(defn- save-gif [chart filename & options]
  (with-open [os (FileImageOutputStream. (io/file filename))]
    (let [width (or (:width options) 500)
          height (or (:height options) 400)
          gif-writer (get-writer)
          image-write-param (.getDefaultWriteParam gif-writer)
          type-specifier (ImageTypeSpecifier/createFromBufferedImageType BufferedImage/TYPE_INT_RGB)
          image-meta (.getDefaultImageMetadata gif-writer type-specifier image-write-param)
          meta-format-name (.getNativeMetadataFormatName image-meta)
          root (.getAsTree image-meta meta-format-name)
          child (IIOMetadataNode. "ApplicationExtension")
          depth-factors (vec (range 0.0 0.9 0.025))]
      (doto (get-node root "GraphicControlExtension")
        (.setAttribute "disposalMethod" "none")
        (.setAttribute "userInputFlag" "FALSE")
        (.setAttribute "transparentColorFlag" "FALSE")
        (.setAttribute "delayTime" "1")
        (.setAttribute "transparentColorIndex" "0"))
      (doto child
        (.setAttribute "applicationID" "NETSCAPE")
        (.setAttribute "authenticationCode" "2.0")
        (.setUserObject (into-array Byte/TYPE [0x1 0x0 0x0])))
      (.appendChild (get-node root "ApplicationExtensions") child)

      (.setFromTree image-meta meta-format-name root)

      (doto gif-writer
        (.setOutput os)
        (.prepareWriteSequence nil))
      (doseq [df (into depth-factors (reverse depth-factors))]
        (.. chart getPlot (setDepthFactor df))
        (.writeToSequence gif-writer
                          (IIOImage.
                           (.createBufferedImage chart width (- height (* df 300))) nil image-meta)
                          image-write-param))
      (.endWriteSequence gif-writer))))

(when-let [original-view-fn (get-method view org.jfree.chart.JFreeChart)]
  (defmethod view org.jfree.chart.JFreeChart [chart & options]
    (let [view (apply original-view-fn chart options)]
      (when (= (type (.getPlot chart)) org.jfree.chart.plot.PiePlot3D)
        (pie-slider view))
      view)))

(when-let [original-save-fn (get-method save org.jfree.chart.JFreeChart)]
  (defmethod save org.jfree.chart.JFreeChart [chart filename & options]
    (if (and (= (type (.getPlot chart)) org.jfree.chart.plot.PiePlot3D)
             (.endsWith filename ".gif"))
      (apply save-gif chart filename options)
      (apply original-save-fn chart filename options))))

