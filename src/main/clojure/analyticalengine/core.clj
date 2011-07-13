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
   (ph/include-js "/public/js/jquery-1.5.1.min.js")
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

;; (defn find-layers [name]
;;   (let [[ds-name layer-name] (.split name ":")
;;         datastore (get @catalog ds-name)]
;;    (.getFeatureSource datastore layer-name)))


;; (defn find-style [name]
;;   (if (= name "") nil
;;       (make-css-style (slurp (get @styles name))))

(defn find-layers [names])
(defn find-styles [styles])

(defn wms-handler [request]
  ""
  (let [params          (:query-params request)
        layer           (find-layers (get params "LAYERS"))
        output          (ByteArrayOutputStream.)
        exceptions      (:EXCEPTIONS params)
        width           (Integer/parseInt (get params "WIDTH" "100"))
        height          (Integer/parseInt (get params "HEIGHT" "100"))
        format          (get params "FORMAT" "image/png")
        service         (get params "SERVICE" "WMS")
        style           (find-styles (get params "STYLES"))
        version         (get params "VERSION" "1.1.0")
        request-type    (get params "REQUEST" "GetMap")
        extent          (make-envelope request)
        ]
    (render layer
            output
            extent
            :style style
            :height height
            :width width)
     (ByteArrayInputStream. (.toByteArray output))))


(defn index [request]
  (let [datastore (:datastore @(.state (:servlet request)))]
    (page 
     [:div [:ul (map (fn [name] [:li  name])
                     (seq (.getTypeNames datastore)))]])))

(defroutes main-routes
  (GET "/" [] index)
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
