(ns com.ubermensch.configurator
  (:use
     [clojure.contrib.duck-streams :only [with-in-reader slurp*]]
     [clojure.contrib.test-is :only [testing with-test is]]))

(def *config-version* (ref 0))
(def *registered-configs* (ref {}))

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

(with-test
  (defn register-config [f]
    (let [config (load-config-file f)]
      (dosync (alter *registered-configs* conj [f config]))))

  (binding [load-config-file (fn [_] nil)]
    (is (= {"test" nil} (register-config "test")))
    (is (= {"test" nil "test2" nil} (register-config "test2")))))
