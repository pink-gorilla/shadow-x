(ns shadowx.core
  (:require-macros
   [shadowx.build.prefs :refer [get-pref]])
  (:require
   [shadow.loader :as shadow-loader]
   [shadowx.impl.url :refer [current-path entry-path entry-path-full]]
   [shadowx.module.build :as build]))



(build/add-lazy-modules)
(build/print-build-summary)

(def resolver-a (atom build/webly-resolve))

(defn set-resolver! [resolver-fn]
  (reset! resolver-a resolver-fn))

(defn get-resolver []
  @resolver-a)

(defn shadowx-resolve [s]
  (let [resolver (get-resolver)]
    (resolver s)))

; we don't want to move clojure.string to :bootstrap(:init) bundle.
; so we re-implement some features we need here.

(defn ends-with? [s e]
  (.endsWith s e))

(defn str2 [s1 s2]
  (.concat s1 s2))


;; mode

(defonce mode-a (atom :dynamic))

(defn get-mode []
  @mode-a)

;; resource path

(defonce resource-path-a (atom (get-pref :asset-path)))

(defn get-resource-path []
  @resource-path-a)

;; routing path

(defonce routing-path-a (atom "/"))

(defn get-routing-path []
  @routing-path-a)


(defn set-mode! [mode]
  (println "shadowx set-mode! " mode)
  (if (= mode "static")
    ;; static mode
    (let [cpath (current-path)
          epath (entry-path)
          epath-full (entry-path-full)
          resource-path (if (ends-with? epath "/")
                          (str2 epath "r/")
                          (str2 epath "/r/"))]
      (reset! mode-a :static)
      (println "shadowx static mode: routing-path:" cpath " resource-path:" resource-path "entry-path: " epath)
      (println "shadowx full-entry-path: " epath-full)
      (reset! routing-path-a cpath)
      (reset! resource-path-a resource-path)
      (shadow-loader/init epath-full))
    ;; dynamic mode
    (let [resource-path "/r/"]
      (reset! resource-path-a resource-path)
      (println "shadowx dynamic mode: routing-path:" (get-routing-path) " resource-path:" (get-resource-path))
      (shadow-loader/init ""))))


(defn ^:export start [start-s mode]
  (println "shadowx starting: " start-s " mode: " mode)
  (let [_ (set-mode! mode)
        start-s (symbol start-s)
        start-fn-p (build/webly-resolve start-s)] 
    (-> start-fn-p
        (.then (fn [start-fn]
                 (println "shadowx start-fn got resolved successfully. now starting!")
                 (start-fn mode)))
        (.catch (fn [err]
                  (println "shadowx start-fn resolve error: " err))))))