(ns muon.handler
  (:use compojure.core)
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware [multipart-params :as mp]]
            [ring.util.response :as res]
            [clojure.java.io :as io]
            [honeysql.core :as hn]
            [clojure.java.jdbc :as db]))

; Modify this if you want to change the url returned by the
; webserver.
(def DOMAIN_ROOT "http://localhost:8080/")

; Change the password!!
(def PASSWORD "password")

; Change this if you want to use a different file to source
; the randomized wordlists for your system.
(def WORDSFILE "db/wordlist")

; Default Content-Type
(def MIME "application/octet-stream")
(def MIMEFILE "/etc/mime.types")
(def mimetypes nil)

(defn build-url
  [& args]
  (reduce str DOMAIN_ROOT (interpose "/" args)))

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

(defn get-words-file []
  (clojure.string/split-lines (slurp WORDSFILE)))

(def words-file (memoize get-words-file))

(defn get-from-db
  [folder filename]
  (let [query (hn/build {:select :* :from :data :where [:and [:= :folder folder] [:= :filename filename]]})]
    (first (db/query (db-connection) (hn/format query)))))

(def wrong-options
  {:status 400
   :headers {}
   :body "Bad parameters, request rejected.\n"})

(def internal-error
  {:status 500
   :headers {}
   :body "Internal server error, this should NOT happen."})

(defn generate-random-folder []
  (let [words (words-file)]
    (str (rand-nth words) (rand-nth words) (rand-nth words))))

(def base-file-data {:folder ""
                     :filename ""
                     :type ""
                     :visits 0
                     :expires_at 0
                     :max_visits 0
                     :policy ""})

(defn save-to-db
  [folder filename type opts]
  (let [duration (try (Integer/parseInt (:duration opts)) (catch Exception e nil))
        clicks (try (Integer/parseInt (:clicks opts)) (catch Exception e nil))
        data (merge base-file-data {:folder folder :filename filename :type type})
        get-id #(last (ffirst %))
        save-fn (fn [query expires?] 
                  (let [res (db/insert! (db-connection) :data query)]
                    (when expires?
                      ; Set up auto-expire entry in the table
                      (db/insert! (db-connection) :autoexpire {:data_id (get-id res) 
                                                               :expires_at (:expires_at query)}))
                    res))
        get-url #(build-url "resource" folder filename)]
    (cond
     (and (nil? duration) (nil? clicks)) wrong-options
     (not (nil? clicks))
       (let [res (save-fn (merge data {:policy "clicks" :max_visits clicks}) false)]
         (str (get-url) "\n"))
     (not (nil? duration))
       (let [res (save-fn (merge data {:policy "timed" :expires_at (+ (System/currentTimeMillis) (* duration 1000))}) true)]
         (str (get-url) "\n"))
     :else internal-error)))

(defn get-mime [filename]
  (let [dot (.indexOf filename ".")
        ext (if (> dot -1) (subs filename (inc dot)))
        mime (or (get mimetypes ext) MIME)]
    mime))

; TODO(morg): Make exit condition with exception if we recur too many times.
(defn randomize-file-location [filename]
  (loop []
    (let [folder (generate-random-folder)
          full-name (reduce str (interpose "/" ["resources" folder filename]))
          new-file (io/as-file full-name)]
      (do
        (io/make-parents full-name)
        (if (not (.exists new-file))
          [folder new-file]
          (recur))))))

(defn handle-file-upload
  [data]
  (let [[folder file] (randomize-file-location (:filename (:file data)))]
    (io/copy (:tempfile (:file data)) file)
    (save-to-db folder (:filename (:file data)) (get-mime (:filename (:file data))) data)))

(defn build-response
  [folder filename type]
  (let [full-file (reduce str (interpose "/" ["resources" folder filename]))]
    (if (seq type)
      (res/content-type (res/file-response full-file) type)
      internal-error)))

(defn check-expired
  [{:keys [id folder filename type policy expires_at max_visits visits]}]
  (if (or (and (= "timed" policy)
               (> (System/currentTimeMillis) expires_at))
          (and (= "clicks" policy)
               (>= visits max_visits)))
    {:status 404
     :headers {}
     :body "This file has expired.\n"}
    (do
      (db/execute! (db-connection) [(str "update data set visits = (visits + 1) where id =" id)])
      (build-response folder filename (or type MIME)))))

(defn return-data
  [folder filename]
   (let [data (get-from-db folder filename)]
     (if-not (empty? data)
       (check-expired data)
       nil)))

(defn load-mime 
  [file]
  (into {} (map #(let [line (.split % "\\s+")]
                   (zipmap (rest line) (repeat (first line))))
                (clojure.string/split (slurp file) #"\n"))))


(defn check-password [password]
  (not= password PASSWORD))

(defroutes app-routes
  (GET "/" [] "Welcome to Muon, the private self-destructible file host.")
  ; TODO(morg) Remove the /resource/ path and only access $HOST/<folder>/<filename> 
  (GET ["/resource/:folder/:filename"] [folder filename] (return-data folder filename))
  (mp/wrap-multipart-params
   (POST "/upload" {params :params}
         (cond
           (check-password (:password params))
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
