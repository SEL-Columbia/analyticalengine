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
        [ring.middleware.multipart-params :only (wrap-multipart-params)]
        [ring.adapter.jetty-servlet :only (run-jetty)]))

(defn wrap-servlet-session [handler]
  (fn [request]
    (handler
     (if-let [servlet-request (:servlet-request request)]
       (assoc request :session (.getSession servlet-request true))
       request))))

(def catalog (ref {}))
(def styles (ref {}))


(defn load-datastore [ds-name ds-config]
  (println (str "Loading datastore" ds-name " datastore"))
  (dosync (alter catalog assoc
                 (name ds-name)
                 (data-store
                  (.getString ds-config "connection")))))

(defn load-style [style-name style-config]
  (println (str "Loading style:" style-name ))
  (dosync (alter styles assoc
                 (name style-name) (.getString style-config "path"))))

(defn load-section [section-name section]
  (let [type (.getString section "type")]
    (condp = type
        "style" (load-style section-name section)
        "datastore" (load-datastore section-name section))))

(defn iter-sections [config]  
  (let [sections (filter #(not= "application" %) (.getSections config))]
    (doseq [section sections]
      (load-section section (.getSection config section)))))

(defn load-ini-file [path]
  "Loads a ini file from the file system"
  (let [config (HierarchicalINIConfiguration. path)]
    config))

(defn build-catalog []
  (let [config (load-ini-file "catalog.ini")]
    (println "Starting server")
    (println "--------------------")
    (iter-sections config)))

(defn build-catalog []
  (println "Loading catalog"))

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

(defn find-layers [name]
  (let [[ds-name layer-name] (.split name ":")
        datastore (get @catalog ds-name)]
    (.getFeatureSource datastore layer-name)))

(defn find-style [name]
  (if (= name "") nil
      (make-css-style (slurp (get @styles name)))))

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
        style           (find-style (get params "STYLES"))
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
  (page
   [:div [:h1 "Home page"]
    [:ul 
     [:li [:a {:href "/make-buffer"} "Buffer CSV file"]]
     [:li [:a {:href "/make-distance"} "Distance"]]]]))

(defn make-buffer [request]
  (page [:div [:h2 "Buffer CSV file"]]))

(defroutes main-routes
  (GET "/" [] index)
  (GET "/wms" [] wms-handler)    
  (GET "/make-buffer" []  make-buffer)
  (files "/public")
  (not-found "<h1>Page not found</h1>"))

(def app (-> main-routes
             wrap-servlet-session
             wrap-multipart-params))

(defn run-server []
  (run-jetty #'app {:port 8080 :join? false}))
