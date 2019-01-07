(ns clojars.web.jar
  (:require [clojars.web.common :refer [tag jar-name]]
            [clojure.spec.alpha :as s]))

(defn leiningen-coordinates [attrs jar]
  (list
    [:h4 "Leiningen/Boot"]
    [:div#leiningen-coordinates.package-config-example
     attrs
     [:pre
      (tag "[")
      (jar-name jar)
      [:span.string " \""
       (:version jar) "\""] (tag "]")]]))

(defn leiningen-coordinates-string [jar]
  (format "[%s \"%s\"]" (jar-name jar) (:version jar)))

(s/def ::version string?)
(s/def ::group_name string?)
(s/def ::jar_name string?)

(s/def ::jar (s/keys :req-un [::version ::group_name ::jar_name]))

(s/fdef leiningen-coordinates-string
        :args (s/cat :jar ::jar))

(defn clojure-cli-coordinates [attrs jar]
  (list
    [:h4 "Clojure CLI/deps.edn"]
    [:div#deps-coordinates.package-config-example
     attrs
     [:pre
      (jar-name jar)
      \space
      (tag "{")
      ":mvn/version "
      [:span.string \" (:version jar) \"]
      (tag "}")]]))

(defn gradle-coordinates [attrs {:keys [group_name jar_name version]}]
  (list
    [:h4 "Gradle"]
    [:div#gradle-coordinates.package-config-example
     attrs
     [:pre
      "compile "
      [:span.string \' group_name ":" jar_name ":" version \']]]))

(defn maven-coordinates [attrs {:keys [group_name jar_name version]}]
  (list
    [:h4 "Maven"]
    [:div#maven-coordinates.package-config-example
     attrs
     [:pre
      (tag "<dependency>\n")
      (tag "  <groupId>") group_name (tag "</groupId>\n")
      (tag "  <artifactId>") jar_name (tag "</artifactId>\n")
      (tag "  <version>") version (tag "</version>\n")
      (tag "</dependency>")]]))

(defn coordinates [attrs jar]
  (for [f [maven-coordinates
           gradle-coordinates
           leiningen-coordinates
           clojure-cli-coordinates]]
    (f attrs jar)))
