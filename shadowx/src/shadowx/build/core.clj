(ns shadowx.build.core
  (:require
   [taoensso.timbre  :refer [debug info warn]]
   [modular.writer :refer [write write-edn-private]]
   [shadowx.build.shadow-config :refer [shadow-config]]
   [shadowx.build.shadow :refer [shadow-build]] ; shadow via generated config file
   [shadowx.build.prefs :refer [write-build-prefs]]))

(defn write-shadow-config [config]
  (write "shadow-cljs.edn" config))

(defn build
  ([exts opts profile]
   (build exts opts profile "default"))
  ([exts opts profile version]
   (info "build profile: " profile " version: " version)
   (let [bundle (get profile :bundle)
         shadow-config (shadow-config exts opts profile version)]
     (if bundle
       (do (info "building bundle: " bundle)
           (write-shadow-config shadow-config) ; this outputs the shadow-config that shadow-cljs reads
           (write-edn-private :shadowx-shadow-config shadow-config) ; same as the shadow config, but in .gorilla path
           (write-build-prefs)
           (shadow-build profile shadow-config)
          ;(write-target "lazy" ()[name data])
           )
       (warn "profile has no bundle")))))

;(comment
  ;(get-shadow-server-config)
  ;(get-config :demo)
;  (build :watch "+dev" (symbol "demo.app/handler") (symbol "demo.app"))
;  (build :compile "+dev" (symbol "demo.app/handler") (symbol "demo.app"))
 ; 
;  )
