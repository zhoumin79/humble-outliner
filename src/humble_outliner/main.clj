(set! *warn-on-reflection* true)
(ns humble-outliner.main
  "The main app namespace.
  Responsible for initializing the window and app state when the app starts."
  (:gen-class)
  (:require
   [humble-outliner.state :as state]
   [humble-outliner.views :as views]
   [humble-outliner.shared :as shared]
   [io.github.humbleui.util :as util]
   [io.github.humbleui.window :as window]
   [io.github.humbleui.app :as app]
   [io.github.humbleui.ui :as ui])
  (:import
   [io.github.humbleui.jwm.skija LayerMetalSkija]
   [io.github.humbleui.skija ColorSpace]))

(defonce *app
  (atom nil))

(reset! *app
  (ui/default-theme {}
    (ui/make [views/app state/*db])))

(defn maybe-save-window-rect [window event]
  (when (#{:window-move :window-resize} (:event event))
    (let [rect (window/window-rect window)
          {:keys [id scale work-area]} (window/screen window)
          x (-> rect :x (- (:x work-area)) (/ scale) int)
          y (-> rect :y (- (:y work-area)) (/ scale) int)
          w (-> rect :width (/ scale) int)
          h (-> rect :height (/ scale) int)]
      (shared/save-state {:screen-id id, :x x, :y y, :width w, :height h}))))

(defn restore-window-rect []
  (util/when-some+ [{:keys [screen-id x y width height]} (shared/load-state)]
                   (when-some [screen (util/find-by :id screen-id (app/screens))]
                     (let [{:keys [scale work-area]} screen
                           right  (-> (:right work-area) (/ scale) int)
                           bottom (-> (:bottom work-area) (/ scale) int)
                           x      (min (- right 500) x)
                           y      (min (- bottom 500) y)
                           width  (min (- right x) width)
                           height (min (- bottom y) height)]
                       {:screen screen-id, :x x, :y y, :width width, :height height}))))

(defn -main [& args]
  ;; setup window
  (ui/start-app!
   (let [opts   (merge
                 {:title    "test"
                  :screen   (:id (first (app/screens)))
                  :width    800
                  :height   800
                  :x        :center
                  :y        :center
                  :full-screen? true
                  :on-event #'maybe-save-window-rect}
                 (restore-window-rect))
         window (ui/window opts *app)]
      ;; TODO load real monitor profile
     (when (= :macos app/platform)
       (set! (.-_colorSpace ^LayerMetalSkija (.getLayer window)) (ColorSpace/getDisplayP3)))
     (shared/set-floating! window @shared/*floating?)
     (deliver shared/*window window)))
  @shared/*window)




