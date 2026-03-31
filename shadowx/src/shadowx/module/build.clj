(ns shadowx.module.build
  (:require
   [clojure.string :as str]
   [clojure.set]
   [taoensso.timbre :as timbre :refer [warn]]
   [modular.writer :refer [write-edn-private]]
   [extension :refer [get-extensions]]))

;; NAMESPACE

(defn ->keyword [s]
  (-> s str (str/replace  #"'" "") keyword))

(defn- convert-ns-def [module-name ns-def]
  (if (map? ns-def)
    {:module module-name
     :ns-vars (->> (keys ns-def) (map ->keyword) (into []))
     :loadable (->> (vals ns-def) (into []))}
    {:module module-name
     :loadable ns-def}))

(defn- convert-ns [module-name [ns-name ns-def]]
  ;(println "ns-name: " (pr-str ns-name) "keyword: " (->keyword ns-name))
  [(->keyword ns-name) (convert-ns-def module-name ns-def)])

(defn- module->ns [module]
  (let [name (:extension/name module)
        cljs-ns-bindings (:cljs/ns-bindings module)]
  ; namespaces per module is needed to find the module that needs to be loaded for a ns
    (map #(convert-ns name %) cljs-ns-bindings)))

(defn modules->ns-map [modules]
  (->> (reduce concat [] (map module->ns modules))
       (into {})))

(defn ns-map->vars [ns-map]
  (->> (map (fn [[ns-name {:keys [ns-vars]}]]
              (when ns-vars
                [ns-name ns-vars])) ns-map)
       (remove nil?)
       (into {})))

(defn ns-map->loadable [ns-map]
  (->> (map (fn [[ns-name {:keys [loadable]}]]
              (when loadable
                [ns-name loadable])) ns-map)
       (remove nil?)
       (into {})))

;; lazy namespace

(defonce lazy-modules-a (atom []))
(defonce lazy-ns-a (atom {}))
(defonce lazy-ns-vars-a (atom {}))
(defonce lazy-ns-loadable-a (atom {}))

(defmacro get-lazy-modules []
  (warn "lazy modules:" @lazy-modules-a)
  (into [] @lazy-modules-a))

(defmacro get-lazy-ns []
  (let [l (keys @lazy-ns-a)]
    (warn "lazy namespaces: " l)
    (into [] l)))

(defn- set-lazy-modules! [{:keys [spec modules]}]
  (let [ns-vars (ns-map->vars spec)
        ns-loadable (ns-map->loadable spec)]
    (write-edn-private :shadowx-lazy-namespaces spec)
    (write-edn-private :shadowx-lazy-ns-vars ns-vars)
    (write-edn-private :shadowx-lazy-ns-loadable ns-loadable)
    (reset! lazy-modules-a modules)
    (reset! lazy-ns-a spec)
    (reset! lazy-ns-vars-a ns-vars)
    (reset! lazy-ns-loadable-a ns-loadable)))

(defmacro set-ns-vars! []
  (let [ns-vars @lazy-ns-vars-a]
    ;`(reset! shadowx.module.build/lazy-ns-vars-a ~ns-vars)
    `(shadowx.module.build/set-ns-vars ~ns-vars)))

(defmacro set-ns-loadables! []
  (let [loadables @lazy-ns-loadable-a]
    ;specs
    ; lazy/loadable macro. It expects one argument which is 
    ; - a qualified symbol, 
    ; - a vector of symbols or
    ; - a map of keyword to symbol.
    ;`(reset! shadowx.module.build/lazy-ns-loadable-a
    `(shadowx.module.build/set-ns-loadables
      ~(->> (map (fn [[ns-kw l]]
                   `[~ns-kw (shadow.lazy/loadable ~l)]) loadables)
            (into {})))))

(comment
  (str 'clojure.core)
  (name 'clojure.core)
  (symbol "clojure.core")

  (map str ['a 'bingo.bongo 'ui.highcharts])
  (map name ['a 'bingo.bongo 'ui.highcharts]))

;; SERVICE

(defn set-module [{:keys [name lazy cljs-namespace cljs-ns-bindings] :as module}]
  (let [extension-name (cond
                         (:extension/name module) (:extension/name module)
                         name (if (string? name) (keyword name) name) ; old syntax
                         :else :unknown)]
    {:extension/name extension-name
     :cljs/module (or (:cljs/module module)
                      (when lazy extension-name); old syntax - lazy module get the extension name by defualt
                      :main)
     :cljs/depends-on  (or (:cljs/depends-on module)
                           (if lazy ; old syntax
                             #{:main}
                             #{:init}))
     :cljs/namespace (cond
                       (seq (:cljs/namespace module)) (:cljs/namespace module)
                       (seq cljs-namespace) cljs-namespace ; old syntax
                       :else  [])
     :cljs/ns-bindings (cond
                         (seq (:cljs/ns-bindings module)) (:cljs/ns-bindings module)
                         (seq cljs-ns-bindings) cljs-ns-bindings  ; old syntax
                         :else {})
     :cljs/define (or (:cljs/define module)
                      nil)
     :cljs/when-defined (or (:cljs/when-defined module)
                            nil)}))

(defn filter-conditional-not-defined [modules]
  (let [def2set (->> modules
                     (map :cljs/define)
                     (remove nil?)
                     (into #{}))
        modules-without-defined (->> modules
                                     (remove :cljs/when-defined))
        modules-when-defined (->> modules
                                  (filter :cljs/when-defined))
        modules-conditional-included (->> modules-when-defined
                                          (filter #(contains? def2set (:cljs/when-defined %))))
        stats {:defines def2set
               :all (into [(count modules)] (map :extension/name modules))
               :always (into [(count modules-without-defined)] (map :extension/name modules-without-defined))
               :conditional (into [(count modules-when-defined)] (map :extension/name modules-when-defined))
               :conditional-included (into [(count modules-conditional-included)] (map :extension/name modules-conditional-included))}]
    (write-edn-private :shadowx-module-conditional stats)
    (concat modules-without-defined modules-conditional-included)))

(defn- consolidate-extensions [[cljs-module extensions]]
  [cljs-module
   {:extension/list (->> (map :extension/name extensions)
                         (into []))
    :ns-bindings (->> (map :cljs/ns-bindings extensions)
                      (apply merge))
    ; shadow-cljs data
    :entries  (->> (map :cljs/namespace extensions)
                   (apply concat)
                   (into []))
    :depends-on  (->> (map :cljs/depends-on extensions)
                      (apply clojure.set/union))}])

(defn consolidate [extensions]
  (let [items (->> extensions
                   (group-by :cljs/module)
                   (map consolidate-extensions)
                   (into {}))]
    items))

(defn create-loadables [items]
  {:modules (keys items)
   :spec (->> items
              (map (fn [[module {:keys [ns-bindings]}]]
                     ;ns-bindings
                     (->> ns-bindings
                          (map (fn [[ns-s ns-def]]
                                 [(->keyword ns-s)
                                  {:module module
                                   :ns-vars (->> ns-def keys (map ->keyword) (into []))
                                   :loadable (->> ns-def vals (into []))}])))))
              (apply concat)
              (into {}))})

(defn shadow-modules [items]
  (->> items
       (map (fn [[k v]]
              [k (select-keys v [:entries :depends-on])]))
       (into {:init {:entries ['shadowx.core],
                     :depends-on #{}}})))

(defn- module? [module]
  (> (count (:cljs/namespace module)) 0))

(defn create-modules
  "processes discovered extensions
   outputs a state that contains module information
   consider it the start-fn of a service."
  [exts]
  (let [valid-modules (->> (get-extensions exts {:name nil
                                                 :extension/name nil

                                                 :cljs/define nil
                                                 :cljs/when-defined nil
                                                 :cljs/module nil
                                                 :cljs/depends-on nil
                                                 :cljs/namespace []
                                                 :cljs-namespace []
                                                 :cljs/ns-bindings {}
                                                 :cljs-ns-bindings {}
                                                 ; old syntax
                                                 :lazy false})
                           (map set-module)
                           (filter-conditional-not-defined)
                           (filter module?))
        items (consolidate valid-modules)
        loadables (create-loadables items)]
    (write-edn-private :shadowx-module-list valid-modules)
    (write-edn-private :shadowx-module-summary items)
    (set-lazy-modules! loadables)
    (shadow-modules items)))







