(ns analyticalengine.core
  (:import [org.geoscript.geocss Translator CssParser]
           [org.apache.commons.configuration HierarchicalINIConfiguration]
           [org.geotools.geometry.jts ReferencedEnvelope]
           [java.io File
            ByteArrayOutputStream
            ByteArrayInputStream
            FileInputStream])
  (:require [hiccup.core :as hiccup]
            [hiccup.page-helpers :as ph]
            [hiccup.form-helpers :as fh])
  (:use [compojure.core :only (defroutes GET ANY)]
        [compojure.route :only (not-found files)]
        [clojure.contrib.json :only (json-str)]
        [geoscript io render proj]
        [clojure.contrib.sql :as sql]
        [ring.middleware.multipart-params :only (wrap-multipart-params)]
        [ring.adapter.jetty-servlet :only (run-jetty)]))

(defn wrap-servlet-session [handler]
  (fn [request]
    (handler
     (if-let [servlet-request (:servlet-request request)]
       (assoc request :session (.getSession servlet-request true))
       request))))

(defn make-css-style [string]
  (.css2sld (Translator.)
            (.get (CssParser/parse
                   (ByteArrayInputStream. (.getBytes string))))))

(defn page [body & {:keys [header title]}]
  (hiccup/html
   (ph/doctype :html4)
   [:head
    [:title (or title "")]]
   (ph/include-css "/public/css/screen.css")
   (ph/include-js "/public/js/jquery.min.js")
   header
   [:body
    [:div {:id :container}
     [:div {:id "content" }
      body]]]))

(defn make-envelope [request]
  "Make a Refercened Envelope from a request parameter"
  (let [[minx miny maxx maxy] (map #(Double/parseDouble %)
                                  (.split (get (:query-params request) "BBOX"
                                               ) ","))
        srs   (epsg->proj (get (:query-params request) "SRS" "EPSG:4326"))]
    (ReferencedEnvelope. minx maxx miny maxy srs)))

(defn get-datastore [request]
  (:datastore @(.state (:servlet request))))

(defn get-layer [request layer]
  (println layer)
  (.getFeatureSource (get-datastore request) layer))

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


(defn index [request]
  (let [datastore (get-datastore request)]
    (page 
     [:div
      [:ul
       (map (fn [name]
              [:li [:a {:href (str "/viewer/" name)} name]])
            (.getTypeNames datastore))]])))

(defn bbox->vec [bbox]
  [(.getMinX bbox)
   (.getMinY bbox)
   (.getMaxX bbox)
   (.getMaxY bbox)])

(defn web-viewer [request]
  (let [layer (get-layer request (:layer (:params request)))]
    (page
     [:div [:h3 "Layer viewer"] layer [:div {:id "maps"}]]
     :header (list
              (ph/include-js "/public/js/openlayers/OpenLayers.js")
              (ph/include-js "/public/js/viewer.js")
              (ph/javascript-tag (str "$(function(){
                    loadPage({
                      name:\""(.. layer (getName) (getLocalPart))"\",
                      bounds:" (json-str (bbox->vec (.getBounds layer)))",
                    });
             })"))))))

(defroutes main-routes
  (GET "/" [] index)
  (GET "/viewer/:layer" [layer] web-viewer)
  (GET "/wms" [] wms-handler)
  (files "/public")
  (not-found "<h1>Page not found</h1>"))

(defmacro with-datastore [store & body]
  `(try
    ~@body
    (finally (.dispose ~store))))

(defn wrap-db-connection [handler]
  (fn [request]
    (if (-> request :uri (.startsWith "/public/"))
      (handler request)
      (with-datastore (:datastore @(.state (:servlet request)))
        (handler request)))))



(def app (-> main-routes
             wrap-servlet-session
             wrap-multipart-params))

(defn run-server []
  (run-jetty #'app {:port 8080 :join? false}))
