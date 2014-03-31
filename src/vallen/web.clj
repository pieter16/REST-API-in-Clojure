(ns vallen.web
  (:require [compojure.core :refer [defroutes context GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [hiccup.page :as page]
            [clojure.java.io :as io]
            [ring.middleware.stacktrace :as trace]
            [ring.middleware.session :as session]
            [ring.middleware.session.cookie :as cookie]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.basic-authentication :as basic]
            [ring.middleware.json :as middleware]
            [cemerick.drawbridge :as drawbridge]
            [environ.core :refer [env]]
            [liberator.core :refer [resource]]
            [vallen.presentations.cassandra :as cs]
            [vallen.apis.hateoas :as apis]))

;; DEFAULT HEROKU FUNCTIONS
(defn- authenticated? [user pass]
  ;; DO: heroku config:add REPL_USER=uid REPL_PASSWORD=password
  (= [user pass] [(env :repl-user false) (env :repl-password false)]))

(def ^:private drawbridge
  (-> (drawbridge/ring-handler)
    (session/wrap-session)
    (basic/wrap-basic-authentication authenticated?)))

;; The API starts here

;; We’re going to work with JSON, so let’s include some Ring middlewares to setup response headers
;; (wrap-json-response) and parse request bodies (wrap-json-body) for us. A middleware is just a
;; wrapper around a handler, thus it can pre- and post-process the whole request/response cycle.

(defroutes app-routes
  (ANY "/repl" {:as req}
    (drawbridge req))
  (GET "/" [] (apis/all-apis))
  (context "/presentations" [] (defroutes presentations-routes
                           (GET "/" [] (cs/read-all-presentations))
                           (POST "/" {body :body} (cs/create-presentation body))
                           (context "/:id" [id] (defroutes presentation-routes
                                                  (GET "/" [] (cs/read-presentation id))
                                                  (PUT "/" {body :body} (cs/update-presentation id body))
                                                  (DELETE "/" [] (cs/delete-presentation id))
                                                  ))))
  (GET "/test" [] (resource :available-media-types ["text/html"]
                    :handle-ok (fn [ctx]
                                 (format "<html>It's %d milliseconds since the beginning of the epoch."
                                   (System/currentTimeMillis)))))
  (ANY "*" []
    (route/not-found (slurp (io/resource "404.html")))))

(def app
  (-> (site app-routes)
    (middleware/wrap-json-body)
    (middleware/wrap-json-response)))

(defn wrap-error-page [handler]
  (fn [req]
    (try (handler req)
      (catch Exception e
        {:status 500
         :headers {"Content-Type" "text/html"}
         :body (slurp (io/resource "500.html"))}))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port ) 5000))
        ;; DONE: heroku config:add SESSION_SECRET=161514131211109
        store (cookie/cookie-store {:key (env :session-secret )})
        cassandra-init (cs/init)
        cassandra-session (cs/set-session)
        ]
    (jetty/run-jetty (-> #'app
                       ((if (env :production )
                          wrap-error-page
                          trace/wrap-stacktrace))
                       (site {:session {:store store}}))
      {:port port :join? false})))

;; For interactive development:
(comment
  (ns vallen.web)
  (.stop server)
  (def server (-main))
  )
