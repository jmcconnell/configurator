(ns com.ubermenschconsulting.configurator
  (:import [org.joda.time DateTime]
           [java.util.logging Logger Level])
  (:refer-clojure :exclude [get]))

(def ^:dynamic *update-interval* (* 5 60 1000)) ; 5 minutes

(def ^:private config-version (ref 0))
(def ^:private registered-configs (ref #{}))
(def ^:private ^:dynamic *config* (ref {}))

(defn- ^:dynamic load-config-file
  "Loads f and returns the final expression. If f is found on the classpath,
  it is loaded from there. Otherwise, it is assumed to be a file."
  [f]
  (binding [*ns* (create-ns
                   (symbol (str "com.ubermensch.configurator.config"
                                (dosync (alter config-version inc)))))]
    (refer-clojure)
    (let [url (.getResource (clojure.lang.RT/baseLoader) f)]
      (load-string (slurp (or url f))))))

;(defn- update-config [c] (send *config* merge c))
(defn- update-config [c] (alter *config* merge c))

(defn register-config [f]
  (let [config (load-config-file f)]
    ;(update-config config)
    ;(await *config*)
    (dosync
      (update-config config)
      (alter registered-configs
             conj
             (with-meta [f] {:last-updated (DateTime.)})))))

(defn check-for-updates
  "Checks for config files that have not been re-read in more than
  *update-interval* seconds and reloads them."
  []
  (doseq [c (filter #(.isAfter (DateTime.) (.plus % *update-interval*))
                    @registered-configs)]
    (update-config c)))

(defn get
  "Gets the value of k from the current configuration or default or nil
  if not present."
  ([k] (get k nil))
  ([k default] (clojure.core/get @*config* k default)))

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
