(ns demo.app
  (:require
   [shadowx.core :as shadowx]))

(defn start [mode]
  (println "DEMO APP STARTED with mode: " mode)
  (-> (shadowx/shadowx-resolve 'demo.joke/joke)
      (.then (fn [fun]
               (let [j (fun)]
                 (println "joke: " j))))))