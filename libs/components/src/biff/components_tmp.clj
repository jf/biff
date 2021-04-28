(ns biff.components-tmp
  (:require
    [nrepl.server :as nrepl]
    [reitit.ring :as reitit]
    [ring.adapter.jetty9 :as jetty]))

(defn nrepl [{:keys [biff/first-start
                     biff.nrepl/port
                     biff.nrepl/quiet]
              :or {port 7888}
              :as sys}]
  (when first-start
    (nrepl/start-server :port port)
    (spit ".nrepl-port" (str port))
    (when-not quiet
      (println "nrepl running on port" port)))
  sys)

(defn reitit [{:biff.reitit/keys [routes default-handlers on-error]
               :as sys}]
  (let [router (reitit/router routes)
        default-handlers (when on-error
                           (concat default-handlers
                                   [(reitit/create-default-handler
                                      (->> [:not-found :method-not-allowed :not-acceptable]
                                           (map (fn [k]
                                                  [k #(on-error (assoc % :reitit-error k))]))
                                           (into {})))]))
        handler (if (not-empty default-handlers)
                  (reitit/ring-handler
                    router
                    (apply reitit/routes default-handlers))
                  (reitit/ring-handler router))]
    (assoc sys
           :biff.reitit/router router
           :biff.web/handler handler)))

(defn jetty [{:biff.web/keys [host port handler]
              :biff.jetty/keys [quiet websockets]
              :or {host "0.0.0.0"
                   port 8080}
              :as sys}]
  (let [server (jetty/run-jetty handler
                                {:host host
                                 :port port
                                 :join? false
                                 :websockets websockets
                                 :allow-null-path-info true})]
    (when-not quiet
      (println "Jetty running on" (str "http://" host ":" port)))
    (update sys :biff/stop conj #(jetty/stop-server server))))
