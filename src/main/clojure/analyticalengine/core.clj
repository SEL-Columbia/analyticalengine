(ns analyticalengine.core
  (:use [compojure.core :only (defroutes GET ANY)]
        [compojure.route :only (not-found)]
        [ring.middleware.multipart-params :only (wrap-multipart-params)]
        [ring.adapter.jetty-servlet :only (run-jetty)]))

(defn wrap-servlet-session [handler]
  (fn [request]
    (handler
     (if-let [servlet-request (:servlet-request request)]
       (assoc request :session (.getSession servlet-request true))
       request))))

(defn wrap-logging [handler]
  (fn [request]
    (println (keys request))
    (handler request)))

(defn index [request]
  (str request))


(defroutes main-routes
  (GET "/" [] index)
  (not-found "<h1>Page not found</h1>"))

(def app (-> main-routes
             wrap-logging
             wrap-servlet-session
             wrap-multipart-params))

(defn run-server []
  (run-jetty #'app {:port 8080 :join? false}))
