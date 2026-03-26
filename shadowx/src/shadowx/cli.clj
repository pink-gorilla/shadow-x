(ns shadowx.cli
  (:require
   [modular.config :refer [load-config! get-in-config]]
   [extension :refer [discover]]
   [modular.writer :refer [write-edn-private]]
   [shadowx.build.core :refer [build]]
   [shadowx.build.profile :refer [setup-profile]])
  (:gen-class))

(defn build-cli [{:keys [config profile version]
                  :or {version "default"}}]
  (load-config! config)
  (let [config (get-in-config [])
        ext-config {:disabled (or (get-in config [:extension :disabled]) #{})}
        exts (discover ext-config)]
    (write-edn-private :shadowx-build-cli-config config)
    (println "profile: " profile)
    (let [full-profile (setup-profile profile)]
      (build exts config full-profile version))))