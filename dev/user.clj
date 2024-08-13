(ns user
  (:require
   [clj-reload.core :as reload]
   [duti.core :as duti]
   [humble-outliner.state :as state]
   [humble-outliner.main :as main]
   [io.github.humbleui.app :as app]
   [io.github.humbleui.window :as window]
   ))

(reload/init
  {:dirs ["src" "dev" "test"]
   :no-reload '#{user
                 io.github.humbleui.protocols
                 io.github.humbleui.signal}})

(def ^:dynamic *t0*)

(def monitor)

(defn log [& args]
  (let [dt    (- (System/currentTimeMillis) *t0*)
        mins  (quot dt 60000)
        secs  (mod (quot dt 1000) 60)
        msecs (mod dt 1000)]
    (apply println (format "%02d:%02d.%03d" mins secs msecs) args))
  (flush))

(defn reload []
  (binding [*t0*                     (System/currentTimeMillis)
            clj-reload.util/*log-fn* log]
    ;; do not reload in the middle of the frame
    (locking monitor
      (duti/reload))))

(defn -main [& args]
  (let [args (apply array-map args)
        ;; starting app
        _    (set! *warn-on-reflection* true)
        _    (@(requiring-resolve 'main/-main))
        ;; starting socket repl
        port (some-> (get args "--port") parse-long)
        _    (duti/start-socket-repl {:port port})]))


(comment
  ;; Anything we do to the app UI, we need to eval it wrapped in `doui` so that
  ;; it runs on the UI thread.
  (reload)
  ;; (reset-window)

  ;; keep window on top even when not focused
  (app/doui
   (window/set-z-order @state/*window :floating))

  ;; set window to hide normally when not focused
  (app/doui
   (window/set-z-order @state/*window :normal)))








