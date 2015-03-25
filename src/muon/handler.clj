(ns muon.handler
  (:use compojure.core)
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware [multipart-params :as mp]]
            [ring.util.response :as res]
            [clojure.java.io :as io]
            [honeysql.core :as hn]
            [honeysql.helpers :refer :all]
            [clojure.java.jdbc :as db]))

; Modify this if you want to change the url returned by the
; webserver
(def DOMAIN_ROOT "http://localhost:8080/")

; Change the password!!
(def PASSWORD "password")

; Default Content-Type
(def MIME "application/octet-stream")
(def MIMEFILE "/etc/mime.types")
(def mimetypes nil)

(defn build-url
  [& args]
  (reduce str DOMAIN_ROOT args))

(defn pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setMaxIdleTimeExcessConnections (* 5 60))
               (.setMaxIdleTime (* 1 60 60)))]
    {:datasource cpds}))

(def sqldb {:subprotocol "sqlite"
            :classname "org.sqlite.JDBC"
            ; Modify this if you want to change the path to your DB
            :subname "db/database.db"})

(def pooled-db (delay (pool sqldb)))
(defn db-connection [] @pooled-db)


(defn get-from-db
  [id]
  (first (db/query (db-connection) (hn/format (hn/build {:select :*
                                                         :from [:data]
                                                         :where [:= :id id]})))))

(def wrong-options
  {:status 400
   :headers {}
   :body "Bad parameters, request rejected.\n"})

(def internal-error
  {:status 500
   :headers {}
   :body "Internal server error, this should NOT happen."})

(defn save-to-db
  [text type opts]
  (let [duration (try (Integer/parseInt (:duration opts)) (catch Exception e nil))
        clicks (try (Integer/parseInt (:clicks opts)) (catch Exception e nil))]
    (cond
     (and (nil? duration) (nil? clicks)) wrong-options
     (not (nil? clicks))
       (let [res (db/insert! (db-connection) :data {:text text :type type :policy "clicks" :expires_at 0 :max_visits clicks :visits 0})]
         (build-url
          "resource/"
          (str (last (ffirst res)) "\n")))
     (not (nil? duration))
       (let [res (db/insert! (db-connection) :data {:text text :type type :policy "timed" :expires_at (+ (System/currentTimeMillis) (* duration 1000)) :max_visits 0 :visits 0})]
         (build-url
          "resource/"
          (str (last (ffirst res)) "\n")))
     :else internal-error)))

(defn get-mime [filename]
  (let [dot (.indexOf filename ".")
        ext (if (> dot -1) (subs filename (inc dot)))
        mime (or (get mimetypes ext) MIME)]
    mime))

(defn handle-file-upload
  [data]
  (let [filename (str "resources/" (System/currentTimeMillis))]
    (io/copy (:tempfile (:file data)) (io/as-file filename))
    (save-to-db filename (get-mime (:filename (:file data))) data)))

(defn build-response
  [text type]
  (if (seq type)
    (res/content-type (res/file-response text) type)
    internal-error))

(defn check-expired
  [{:keys [id text type policy expires_at max_visits visits]}]
  (if (or (and (= "timed" policy)
               (> (System/currentTimeMillis) expires_at))
          (and (= "clicks" policy)
               (>= visits max_visits)))
    {:status 404
     :headers {}
     :body "This file has expired.\n"}
    (do
      (db/execute! (db-connection) [(str "update data set visits = (visits + 1) where id =" id)])
      (build-response text (or type MIME)))))

(defn return-data
  [id]
   (let [data (get-from-db id)]
     (if-not (empty? data)
       (check-expired data)
       nil)))

(defn load-mime [file]
  (loop [mimes (line-seq (io/reader file))
         mimetypes {}]
    (if (empty? mimes)
      mimetypes
      (let [line (.split (first mimes) "\\s+")
            type (first line)
            exts (rest line)]
        (recur (rest mimes)
          (reduce (fn [mm ext]
                    (assoc mm ext type))
            mimetypes exts))))))

(defroutes app-routes
  (GET "/" [] "Welcome to Muon, the private self-destructible file host.")
  (GET ["/resource/:id" :id #"[0-9]+"] [id] (return-data (Integer/parseInt id)))
  (mp/wrap-multipart-params
   (POST "/upload" {params :params}
         (cond
           (not= (:password params) PASSWORD)
             {:status 401
              :header {}
              :body "Unauthorized access. This incident will be reported to your parents.\n"}
           (not (nil? (:file params)))
             (handle-file-upload params)
           :else
             {:status 418
              :headers {}
              :body "Incorrect request, either give me a file or a piece of text.\n"})))
  (route/resources "/")
  (route/not-found "Not Found\n"))


(def mimetypes (load-mime MIMEFILE))
(def app
  (handler/site app-routes))
