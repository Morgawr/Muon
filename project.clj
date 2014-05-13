(defproject muon "0.1.0"
  :description "Muon - Build your own self-destructible file/text host."
  :url ""
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [lein-light-nrepl "0.0.6"]
                 [compojure "1.1.6"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [com.mchange/c3p0 "0.9.2.1"]
                 [honeysql "0.4.3"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler muon.handler/app}
  :repl-options {:nrepl-middleware [lighttable.nrepl.handler/lighttable-ops]}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]
                        [ring-serve "0.1.2"]]}})
