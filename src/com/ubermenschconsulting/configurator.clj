(ns com.ubermenschconsulting.configurator
  (:use [clojure.test :only [testing with-test is]])
  (:import [org.joda.time DateTime]
           [java.util.logging Logger Level])
  (:refer-clojure :exclude [get]))

(def ^:private *config-version* (ref 0))
(def ^:private *registered-configs* (ref #{}))
(def ^:private *config* (ref {}))
(def ^:private *update-interval* (* 5 60 1000)) ; 5 minutes

(with-test
  (defn- load-config-file
    "Loads f and returns the final expression. If f is found on the classpath,
    it is loaded from there. Otherwise, it is assumed to be a file."
    [f]
    (binding [*ns* (create-ns
                     (symbol (str "com.ubermensch.configurator.config"
                                  (dosync (alter *config-version* inc)))))]
      (refer-clojure)
      (let [url (.getResource (clojure.lang.RT/baseLoader) f)]
        (load-string (slurp (or url f))))))

  (testing "loading a file"
    (is (= '(1 2 3 4 5) (load-config-file "src/test/test.config"))))

  (testing "loading a resource"
    (is (= '(1 2 3 4 5) (load-config-file "test.config"))))

  (testing "vars do not pollute namespace"
    (is (= nil (resolve 'configurator-test-var)))))

;(defn- update-config [c] (send *config* merge c))
(defn- update-config [c] (alter *config* merge c))

(with-test
  (defn register-config [f]
    (let [config (load-config-file f)]
      ;(update-config config)
      ;(await *config*)
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
          (is (= {:a 0 :b 1} @*config*))))

      (binding [load-config-file (fn [_] {:a 0 :b 3 :c 4})]
        (let [c (register-config "test-merge2")]
          (is (= {:a 0 :b 3 :c 4} @*config*)))))))

(defn check-for-updates
  "Checks for config files that have not been re-read in more than
  *update-interval* seconds and reloads them."
  []
  (doseq [c (filter #(.isAfter (DateTime.) (.plus % *update-interval*))
                    @*registered-configs*)]
    (update-config c)))

(with-test
  (defn get
    "Gets the value of k from the current configuration or default or nil
    if not present."
    ([k] (get k nil))
    ([k default] (clojure.core/get @*config* k default)))

  (binding [load-config-file (fn [_] {:a 0 :b 1})]
    (register-config "test")

    (testing "existing config values are returned"
      (is (= 0 (get :a)))
      (is (= 1 (get :b))))

    (testing "missing values result in nil"
      (is (nil? (get :c))))

    (testing "missing values with a default result in the default"
      (is (= 2 (get :c 2))))))

(when (not *compile-files*)
  (try
    (.scheduleAtFixedRate (java.util.concurrent.ScheduledThreadPoolExecutor. 1)
                          check-for-updates
                          0
                          *update-interval*
                          java.util.concurrent.TimeUnit/MILLISECONDS)
    (catch Exception e
      (.log (Logger/getLogger (str (ns-name *ns*)))
            Level/WARNING
"Could not kick off Thread to monitor config files. Configuration will remain
as initially read until check-for-updates is called by application code."))))
