(defproject muon "0.2.0-indev"
  :description "Muon - Build your own self-destructible file host."
  :url "https://github.com/Morgawr/Muon.git"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [compojure "1.6.1"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [com.mchange/c3p0 "0.9.2.1"]
                 [honeysql "0.9.4"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler muon.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]
                        [ring-serve "0.1.2"]]}})
