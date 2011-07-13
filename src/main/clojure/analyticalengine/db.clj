(ns analyticalengine.db)

(defn get-datastore [request]
  (:datastore @(.state (:servlet request))))

(defn get-layer [request layer]
  (.getFeatureSource (get-datastore request) layer))


