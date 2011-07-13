(ns analyticalengine.wms
  (:use [geoscript.proj :only (epsg->proj)]
        [geoscript.render :only (render)]
        [analyticalengine.db :only (get-layer)])
  (:import [org.geoscript.geocss Translator CssParser]
           [org.apache.commons.configuration HierarchicalINIConfiguration]
           [org.geotools.geometry.jts ReferencedEnvelope]
           [java.io File
            ByteArrayOutputStream
            ByteArrayInputStream
            FileInputStream]))

(defn bbox->vec [bbox]
  [(.getMinX bbox)
   (.getMinY bbox)
   (.getMaxX bbox)
   (.getMaxY bbox)])



(defn make-envelope [request]
  "Make a Refercened Envelope from a request parameter"
  (let [[minx miny maxx maxy] (map #(Double/parseDouble %)
                                  (.split (get (:query-params request) "BBOX"
                                               ) ","))
        srs   (epsg->proj (get (:query-params request) "SRS" "EPSG:4326"))]
    (ReferencedEnvelope. minx maxx miny maxy srs)))

(defn make-css-style [string]
  (.css2sld (Translator.)
            (.get (CssParser/parse
                   (ByteArrayInputStream. (.getBytes string))))))

(defn wms-handler [request]
  (let [params          (:query-params request)
        layer           (get-layer request (get params "LAYERS"))
        output          (ByteArrayOutputStream.)
        exceptions      (:EXCEPTIONS params)
        width           (Integer/parseInt (get params "WIDTH" "100"))
        height          (Integer/parseInt (get params "HEIGHT" "100"))
        format          (get params "FORMAT" "image/png")
        service         (get params "SERVICE" "WMS")
        version         (get params "VERSION" "1.1.0")
        request-type    (get params "REQUEST" "GetMap")
        extent          (make-envelope request)
        ]
    (render layer
            output
            extent
            :style nil
            :height height
            :width width)
    (ByteArrayInputStream. (.toByteArray output))))

