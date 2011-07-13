(ns analyticalengine.servlet
  (:import [javax.naming InitialContext]
           [org.geotools.data DataStoreFinder]
           [org.geotools.factory GeoTools]
           [org.geotools.jdbc JDBCJNDIDataStoreFactory]
           [javax.sql DataSource])
  (:use [analyticalengine.core :only (app)]
        [ring.util.servlet :only (defservice)])
  (:gen-class :extends javax.servlet.http.HttpServlet
              :exposes-methods {init superInit destory superDestory}
              :init create-state
              :state state))


(defn -create-state []
  [[] (atom {})])

(defn get-datastore []
  (DataStoreFinder/getDataStore
   {(.key JDBCJNDIDataStoreFactory/JNDI_REFNAME) "java:/comp/env/jdbc/analyticalengine"
    (.key JDBCJNDIDataStoreFactory/DBTYPE) "postgis"}))

(defn -init
  ([this servlet-config]
     (.superInit this servlet-config))
  ([this]
     (let [context  (InitialContext.)]
       (GeoTools/init context)
       (swap! (.state this) assoc
              :datastore (get-datastore)))))

(defn -destory []
  (println "calling destory method"))

(defservice app)
