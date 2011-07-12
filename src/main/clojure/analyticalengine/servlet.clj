(ns analyticalengine.servlet
  (:use [analyticalengine.core :only (app build-catalog)]
        [ring.util.servlet :only (defservice)])
  (:gen-class :extends javax.servlet.http.HttpServlet))

(build-catalog)
(defservice app)
