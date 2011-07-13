(ns analyticalengine.pages
  (:use
   [clojure.contrib.json :only (json-str)]
   [geoscript.proj :only (find-feature-epsg)]
   [analyticalengine.wms :only (bbox->vec)]
   [analyticalengine.db :only (get-datastore get-layer)])
  (:require [hiccup.core :as hiccup]
            [hiccup.page-helpers :as ph]
            [hiccup.form-helpers :as fh]))


(defn page [body & {:keys [header title]}]
  (hiccup/html
   (ph/doctype :html4)
   [:head
    [:title (or title "")]]
   (ph/include-css "/public/css/screen.css")
   (ph/include-js "/public/js/jquery.min.js")
   header
   [:body
    [:div {:id "container"}
     [:div {:id "content"}
      body]]]))

(defn index [request]
  (let [datastore (get-datastore request)]
    (page 
     [:div
      [:h3 "Layers"]
      [:ul
       (map (fn [name]
              [:li [:a {:href (str "/viewer/" name)} name]])
            (.getTypeNames datastore))]])))

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
                      epsg: \""(find-feature-epsg layer)"\",
                      bounds:" (json-str (bbox->vec (.getBounds layer)))",
                    });})"))))))
