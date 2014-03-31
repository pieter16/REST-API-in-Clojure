(defproject vallen-api "1.0.0-SNAPSHOT"
  :description "Several code exploration APIs"
  :url "http://vallen-api.herokuapp.com"
  :license {:name "(c) vallen.net"
            :url "http://vallen.net/licence"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [ring/ring-devel "1.2.2"]
                 [ring-basic-authentication "1.0.5"]
                 [environ "0.4.0"]
                 [com.cemerick/drawbridge "0.0.6"]
                 [hiccup "1.0.5"]
                 [liberator "0.11.0"]
                 [clojurewerkz/cassaforte "1.2.0"]
                 [ring/ring-json "0.3.0"]
                 [com.taoensso/timbre "3.1.6"]
                 ]
  :min-lein-version "2.0.0"
  :plugins [[environ/environ.lein "0.3.1"]
            [lein-clean-m2 "0.1.2"]
            [lein-ring "0.8.10"]]
  :ring {:handler vallen.web/app}
  :hooks [environ.leiningen.hooks]
  :profiles {:production {:env {:production true}}}
  :main ^{:skip-aot true} vallen.web)
