(ns analyticalengine.core
  (:use [compojure.core :only (defroutes GET ANY)]
        [compojure.route :only (not-found files)]
        [analyticalengine.pages :as pages]
        [analyticalengine.wms   :as wms]
        [clojure.contrib.json :only (json-str)]
        [ring.middleware.params :only (wrap-params)]
        [ring.middleware.multipart-params :only (wrap-multipart-params)]
        [ring.adapter.jetty-servlet :only (run-jetty)]))

(defn wrap-servlet-session [handler]
  (fn [request]
    (handler
     (if-let [servlet-request (:servlet-request request)]
       (assoc request :session (.getSession servlet-request true))
       request))))

(defroutes main-routes
  (GET "/" [] pages/index)
  (GET "/viewer/:layer" [layer] pages/web-viewer)
  (GET "/wms" [] wms/wms-handler)
  (files "/public")
  (not-found "<h1>Page not found</h1>"))



(def app (-> main-routes
             wrap-params
             wrap-servlet-session
             wrap-multipart-params))

(defn run-server []
  (run-jetty #'app {:port 8080 :join? false}))
