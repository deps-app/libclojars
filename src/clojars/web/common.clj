(ns clojars.web.common
  (:require [hiccup2.core :refer [html]]
            [hiccup.util :as util]))

(defn tag [s]
  (util/raw-string (html [:span.tag s])))

(defn group-is-name?
  "Is the group of the artifact the same as its name?"
  [jar]
  (= (:group_name jar) (:jar_name jar)))

(defn jar-name [jar]
  (if (group-is-name? jar)
    (:jar_name jar)
    (str (:group_name jar) "/" (:jar_name jar))))
