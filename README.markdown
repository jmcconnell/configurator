# Configurator

This library allows for the registration of config files in a running Clojure
application. The config files may consist of any valid Clojure code. The last
expression of the config file should return a map containing the config data.

For example, the following config file would result in the simple configuration
{ :my-key "My Val" }:

    (let [t (System/currentTimeMillis)]
     (if t
      (def v "My Val")
      (def v "Other Val")))
    
    { :my-key v }

The config files are executed in a separated namespace and will not pollute the
global namespace.

The intention is that libraries making use of Configurator can document
the config files that they expect to be available at runtime. Then, they
register those config files with Configurator like this:

     (when (not *compile-files*)
      (config/register-config "path/to/my.libs.config"))

Where "my.libs.config" can either be provided in the filesystem, relative to
where the app is launched, or in the classpath. The (when (not *compile-files*)
... ) is to prevent the registration from running during compile time. It is
not strictly necessary, but it relieves the library of the requirement that
they provide "path/to/my.libs.config" during AOT-compilation.

Once Configurator is loaded, a thread is kicked off that monitors all
registered config files. Every five minutes it reloads the files to pick up
any changes. If a thread cannot be started for any reason, for example, due to
a security policy, this feature is simply not available. However, applications
are free to call com.ubermensch.configurator/check-for-updates on their own.
