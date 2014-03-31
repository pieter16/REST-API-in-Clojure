(ns vallen.apis.hateoas
  (:use ring.util.response)
  (:require  [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(def api-context "http://localhost/api")

(defn all-apis []
  (let
    [presentations (str api-context "/presentations")]
  (response {:presentations_url presentations})))
