# Configurator

by J. McConnell

## Overview

This library allows for the registration of config files in a running Clojure
application. The config files may consist of any valid Clojure code. The last
expression of the config file should return a map containing the config data.

For example, the following config file would result in the simple configuration
`{ :my-key "My Val" }`:

    (let [t (System/currentTimeMillis)]
     (if t
      (def v "My Val")
      (def v "Other Val")))
    
    { :my-key v }

The config files are executed in a separated namespace and will not pollute the
global namespace.

Once Configurator is loaded, a thread is kicked off that monitors all
registered config files. Every five minutes it reloads the files to pick up
any changes. If a thread cannot be started for any reason, for example, due to
a security policy, this feature is simply not available. However, applications
are free to call `com.ubermensch.configurator/check-for-updates` on their own.

## Use

The intention is that libraries making use of Configurator can document
the config files that they expect to be available at runtime. Then, they
register those config files with Configurator like this:

    (ns my.ns
     (:require [com.ubermensch.configurator :as config]))

     (when (not *compile-files*)
      (config/register-config "path/to/my.ns.config"))

Where "my.ns.config" can either be provided in the filesystem, relative to
where the app is launched, or in the classpath. The `(when (not *compile-files*)
... )` is to prevent the registration from running during compile time. It is
not strictly necessary, but it relieves the library of the requirement that
they provide "path/to/my.ns.config" during AOT-compilation.

Then, to access configured values, you use the `get` function:

    (config/get ::my-key)

Here the key, `::my-key`, is namespace-qualified and would expand to
`::my.ns/my-key` when run within the `my.ns` namespace declared above. It
is a good idea for libraries to use namespace-qualified keys like this to
avoid pollution.

## License

Copyright (c) J. McConnell. All rights reserved.

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file epl-v10.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by the
terms of this license.  You must not remove this notice, or any other, from
this software.
