(ns vallen.presentations.cassandra
  (:use ring.util.response)
  (:require [clojurewerkz.cassaforte.client :as client]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.multi.cql :as mcql]
            [taoensso.timbre :as timbre])
  (:use clojurewerkz.cassaforte.query))

(timbre/refer-timbre)

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def cassandra-server "pv-cass1.cloudapp.net")

;; Connecting
;; Do not forget to Enable CQL Support On the Server
;; In order to use CQL and Cassaforte, you need to enable CQL support. Make sure start_native_transport is set to true in cassandra.yaml:
;; start_native_transport: true

;; Will connect to my cassandra cluster in Azure

(defn init []
  (do
    (def cluster (client/build-cluster {:contact-points [cassandra-server]}))
    (info "Cassandra cluster initialized:" cluster)))

;; Creates the keyspaces
(defn set-session []
  (do
    (def session (client/connect cluster :presentations_keyspace ))
    (info "Cassandra session created:" session)))

(defn create-test-presentations []
  "Function to setup presentations_keyspace"
  (do
    (mcql/create-table session "presentations"
      (column-definitions {:presentationid :varchar
                           :topic :varchar
                           :title :varchar
                           :presenter :varchar
                           :url :varchar
                           :status :varchar
                           :primary-key :presentationid}))

    (mcql/insert session "presentations" {:presentationid (uuid)
                                          :topic "Clojure"
                                          :title "Are we there yet"
                                          :presenter "Rich Hickey"
                                          :url "http://www.infoq.com/presentations/Are-We-There-Yet-Rich-Hickey"
                                          :status "seen once"
                                          })
    (mcql/insert session "presentations" {:presentationid (uuid)
                                          :topic "Clojure"
                                          :title "Components Just Enough Structure"
                                          :presenter "Stuart Sierra"
                                          :url "https://www.youtube.com/watch?v=13cmHf_kt-Q"
                                          :status "not seen"
                                          })))

(defn create-presentation-keyspace []
  "Function to initialize the keyspaces"
  (do
    (client/connect! [cassandra-server])
   ;; (cql/drop-keyspace :presentations_keyspace )
    (cql/create-keyspace "presentations_keyspace"
      (with {:replication {:class "SimpleStrategy"
                           :replication_factor 1}}))
    (init)
    (set-session)
    (create-test-presentations)
    ))

(defn read-all-presentations []
  (let
    [result (response (mcql/select session "presentations"))]
    (do
      (info "Delivered all records:" result)
      result)))

(defn read-presentation [id]
  (do
    (info "Delivered record:" id)
    (response (mcql/select session "presentations" (where :presentationid id)))))

(defn create-presentation [entry]
  (let
    [partyid (uuid)
     record (assoc entry :presentationid partyid)
     result (mcql/insert session "presentations" record)]
    (do
      (info "Created record: " result)
      (response (read-presentation partyid)))))

(defn update-presentation [id entry]
  (do
    (info "Updated record: " id "with entry:" entry)
    (mcql/update session "presentations" entry (where :presentationid id))))

(defn delete-presentation [id]
  (do
    (info "Deleted record:" id)
    (response (mcql/delete session "presentations" (where :presentationid id)))))