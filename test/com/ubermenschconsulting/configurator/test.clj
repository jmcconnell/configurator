(ns com.ubermenschconsulting.configurator.test
  (:use [clojure.test :only [deftest testing is use-fixtures]])
  (:require [com.ubermenschconsulting.configurator :as config])
  (:import [org.joda.time DateTime]))

(defn reset-config [tests]
  (binding [config/*config* (ref {})]
    (tests)))

(use-fixtures :each reset-config)

(deftest load-config-file-test
  (testing "loading a file"
    (is (= '(1 2 3 4 5) (#'config/load-config-file "test/test.config"))))

  (testing "loading a resource"
    (is (= '(1 2 3 4 5) (#'config/load-config-file "test.config"))))

  (testing "vars do not pollute namespace"
    (is (= nil (resolve 'configurator-test-var)))))

(deftest register-config-test
  (testing "registered config files"
    (binding [config/load-config-file (fn [_] nil)]
      (testing "stored in set"
        (is (= #{["test"]} (config/register-config "test")))
        (is (= #{["test"]} (config/register-config "test")))
        (is (= #{["test"] ["test2"]} (config/register-config "test2"))))

      (testing "contain proper metadata"
        (let [c (config/register-config "test")]
          (Thread/sleep 1) ; to ensure our new date is later
          (is (true? (.isBefore (:last-updated (meta (c ["test"])))
                                (DateTime.)))))))
           
    (testing "merge config into *config*"
      (binding [config/load-config-file (fn [_] {:a 0 :b 1})]
        (let [c (config/register-config "test-merge")]
          (is (= {:a 0 :b 1} @config/*config*))))

      (binding [config/load-config-file (fn [_] {:a 0 :b 3 :c 4})]
        (let [c (config/register-config "test-merge2")]
          (is (= {:a 0 :b 3 :c 4} @config/*config*)))))))

(deftest get-test
  (binding [config/load-config-file (fn [_] {:a 0 :b 1})]
    (config/register-config "test")

    (testing "existing config values are returned"
      (is (= 0 (config/get :a)))
      (is (= 1 (config/get :b))))

    (testing "missing values result in nil"
      (is (nil? (config/get :c))))

    (testing "missing values with a default result in the default"
      (is (= 2 (config/get :c 2))))))
