(ns analyticalengine.servlet
  (:import [javax.naming InitialContext]
           [org.geotools.data DataStoreFinder]
           [org.geotools.jdbc JDBCJNDIDataStoreFactory]
           [javax.sql DataSource])

  (:use [analyticalengine.core :only (app build-catalog)]
        [ring.util.servlet :only (defservice)])
  (:gen-class :extends javax.servlet.http.HttpServlet
              :exposes-methods {init superInit}
              :init create-state
              :state state))


(defn -create-state []
  [[] (atom {})])

(defn -init
  ([this servlet-config]
     (.superInit this servlet-config))
  ([this]
     (swap! (.state this) assoc
            :datasource (DataStoreFinder/getDataStore
                         {(.key JDBCJNDIDataStoreFactory/JNDI_REFNAME) "java:/comp/env/jdbc/analyticalengine"
                          (.key JDBCJNDIDataStoreFactory/DBTYPE) "postgis"}))))


(defservice app)
