(ns shadowx.core)

(defn ring-handler [req]
  {:body "This is the default shadowx ring handler.
          you should set :shadow {:ring-handler \"your.ns/ring-handler\""})