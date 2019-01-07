(ns clojars.web.jar-test
  (:require [clojure.test :refer :all]
            [clojars.web.jar :as jar]))

(deftest leiningen-coordinates-string-test
  (is (= (jar/leiningen-coordinates-string {:group_name "com.acme"
                                            :jar_name "boom"
                                            :version "1"})
         "[com.acme/boom \"1\"]"))
  (is (= (jar/leiningen-coordinates-string {:group_name "boom"
                                            :jar_name "boom"
                                            :version "1"})
         "[boom \"1\"]")))
