(ns analyticalengine.core
  (:import [org.geoscript.geocss Translator CssParser]
           [java.io ByteArrayInputStream])
  (:require [hiccup.core :as hiccup]
            [hiccup.page-helpers :as ph]
            [hiccup.form-helpers :as fh])
  (:use [compojure.core :only (defroutes GET ANY)]
        [compojure.route :only (not-found files)]
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
  (GET "/make-buffer" []  make-buffer)
  (files "/public")
  (not-found "<h1>Page not found</h1>"))

(def app (-> main-routes
             wrap-servlet-session
             wrap-multipart-params))

(defn run-server []
  (run-jetty #'app {:port 8080 :join? false}))
