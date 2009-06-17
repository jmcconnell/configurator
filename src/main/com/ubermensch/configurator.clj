(ns com.ubermensch.configurator
  (:use
     [clojure.contrib.duck-streams :only [with-in-reader slurp*]]
     [clojure.contrib.test-is :only [testing with-test is]])
  (:import [org.joda.time DateTime]))

(def *config-version* (ref 0))
(def *registered-configs* (ref #{}))
(def *config* (agent {}))
(def *update-interval* (* 5 60 1000)) ; 5 minutes

(with-test
  (defn load-config-file
    "Loads f and returns the final expression. If f is found on the classpath,
    it is loaded from there. Otherwise, it is assumed to be a file."
    [f]
    (binding [*ns* (create-ns
                     (symbol (str "com.ubermensch.configurator.config"
                                  (dosync (alter *config-version* inc)))))]
      (refer-clojure)
      (let [url (.getResource (clojure.lang.RT/baseLoader) f)]
        (load-string (slurp* (or url f))))))

  (testing "loading a file"
    (is (= '(1 2 3 4 5) (load-config-file "src/test/test.config"))))

  (testing "loading a resource"
    (is (= '(1 2 3 4 5) (load-config-file "test.config"))))

  (testing "vars do not pollute namespace"
    (is (= nil (resolve 'configurator-test-var)))))

(defn- update-config [c] (send *config* merge c))

(with-test
  (defn register-config [f]
    (let [config (load-config-file f)]
      (dosync
        (update-config config)
        (alter *registered-configs*
               conj
               (with-meta [f] {:last-updated (DateTime.)})))))

  (testing "registered config files"
    (binding [load-config-file (fn [_] nil)]
      (testing "stored in set"
        (is (= #{["test"]} (register-config "test")))
        (is (= #{["test"]} (register-config "test")))
        (is (= #{["test"] ["test2"]} (register-config "test2"))))

      (testing "contain proper metadata"
        (let [c (register-config "test")]
          (Thread/sleep 1) ; to ensure our new date is later
          (is (true? (.isBefore (:last-updated (meta (c ["test"])))
                                (DateTime.)))))))
           
    (testing "merge config into *config*"
      (binding [load-config-file (fn [_] {:a 0 :b 1})]
        (let [c (register-config "test-merge")]
          (await *config*)
          (is (= {:a 0 :b 1} @*config*))))

      (binding [load-config-file (fn [_] {:a 0 :b 3 :c 4})]
        (let [c (register-config "test-merge2")]
          (await *config*)
          (is (= {:a 0 :b 3 :c 4} @*config*)))))))
