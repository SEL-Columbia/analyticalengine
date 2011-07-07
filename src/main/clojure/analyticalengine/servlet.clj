(ns analyticalengine.servlet
  (:use [analyticalengine.core :only (app)]
        [ring.util.servlet :only (defservice)])
  (:gen-class :extends javax.servlet.http.HttpServlet))

(defservice app)
