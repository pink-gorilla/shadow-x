(ns demo.handler
  (:require
   [hiccup.page :as page]
   [webserver.router :as router]))

(defn handler-app [_req]
  {:body
   (page/html5
    {:mode :html}
    [:body
     [:div#app]
     [:div
      [:script {:src  "/r/prod1/init.js"
                :type "text/javascript"
                :onload "shadowx.core.start (\"demo.app/start\", \"dynamic\");"}]]])})

(def handler (router/create-handler {:ctx {}
                                     :exts []}
                                    [["/" {:get demo.handler/handler-app}]]))