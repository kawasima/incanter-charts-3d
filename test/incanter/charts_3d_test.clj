(ns incanter.charts-3d-test
  (:use [clojure.test]
        [incanter core charts-3d charts]))

(def ds (dataset [:教科書 :選定校数]
                 [["東書" 15]
                  ["実教" 46]
                  ["開隆堂" 6]
                  ["教出" 3]
                  ["清水" 0]
                  ["啓林館" 11]
                  ["数研" 8]
                  ["一橋" 14]
                  ["日文" 22]
                  ["暁" 1]
                  ["オーム" 0]
                  ["第一" 26]]))

(def chart (pie-chart-3d (sel ds :cols :教科書)
             (sel ds :cols :選定校数)))

(deftest save-pie-chart-3d
  (save chart "target/kyokasho.gif"))

(deftest view-pie-chart-3d
  (let [pc3d (view chart)]
    (Thread/sleep 10000)
    (.dispose pc3d)))

(deftest pie-chart-normal-test
  (let [pc (view (pie-chart
                   (sel ds :cols :教科書)
                   (sel ds :cols :選定校数)))]
    (Thread/sleep 10000)
    (.dispose pc)))