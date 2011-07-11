(ns analyticalengine.core
  (:import [org.geoscript.geocss Translator CssParser]
           [java.io ByteArrayInputStream])

  (:use [compojure.core :only (defroutes GET ANY)]
        [compojure.route :only (not-found)]
        [geoscript io render]
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


(defn index [request]
  (str request))


(defroutes main-routes
  (GET "/" [] index)
  (not-found "<h1>Page not found</h1>"))

(def app (-> main-routes
             wrap-servlet-session
             wrap-multipart-params))

(defn run-server []
  (run-jetty #'app {:port 8080 :join? false}))
