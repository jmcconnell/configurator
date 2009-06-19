# Configurator

This library allows for the registration of config files in a running Clojure
application. The config files may consist of any valid Clojure code. The last
expression of the config file should return a map containg the config data.

For example, the following config file would result in the simple configuration
{ :my-key "My Val" }:

  (let [t (System/currentTimeMillis)]
   (if t
    (def v "My Val")
    (def v "Other Val")))
  
  { :my-key v }

The config files are executed in a seperated namespace and will not pollute the
global namespace.
