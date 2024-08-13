(ns humble-outliner.views
  (:require
   [humble-outliner.events :as events]
   [humble-outliner.model :as model]
   [io.github.humbleui.cursor :as cursor]
   [io.github.humbleui.ui :as ui]
   [humble-outliner.state :refer [dispatch!]]
   [io.github.humbleui.paint :as paint]
   ))

(defn text-field [{:keys [id focused *demo *state]}]
  (let [opts   {:focused   focused
                :on-focus  (fn [] ; no parameters for on-focus
                            (dispatch! *demo (events/item-input-focused id)))
                :on-change (fn [text]
                             (dispatch! *demo (events/item-input-changed id text)))}
        keymap {:enter #(dispatch! *demo (events/item-enter-pressed id (:from @*state)))
                :up #(dispatch! *demo (events/focus-before id))
                :down #(dispatch! *demo (events/focus-after id))}]
    [ui/with-context {:hui.text-field/cursor-blink-interval 500
                      :hui.text-field/cursor-width          1
                      :hui.text-field/padding-top           (float 8)
                      :hui.text-field/padding-bottom        (float 8)
                      :hui.text-field/padding-left          (float 0)
                      :hui.text-field/padding-right         (float 0)}
     [ui/focusable opts
      [ui/event-listener {:capture? true
                          :event    :key
                          :on-event (fn [e ctx]
                                      (when (and (:hui/focused? ctx) (:pressed? e))
                                        (cond
                                          (and (= :backspace (:key e))
                                               (zero? (:from @*state))
                                               (zero? (:to @*state)))
                                          (dispatch! *demo (events/item-beginning-backspace-pressed id))

                                          (and (= :tab (:key e))
                                               (:shift (:modifiers e)))
                                          (dispatch! *demo (events/item-outdented id))

                                          (= :tab (:key e))
                                          (dispatch! *demo (events/item-indented id))

                                          (and (= :up (:key e))
                                               (= #{:shift :alt} (:modifiers e)))
                                          (dispatch! *demo (events/item-move-up id))

                                          (and (= :down (:key e))
                                               (= #{:shift :alt} (:modifiers e)))
                                          (dispatch! *demo (events/item-move-down id)))))}
       [ui/on-key-focused {:keymap keymap}
        [ui/with-cursor {:cursor :pointing-hand}
         [ui/text-input
          ;opts :*state *state
          (merge  opts {:*state *state})
          ]]]]
      ]
     ]))

(ui/defcomp dot-spacer []
  [ui/gap {:width 6 :height 6}])

(ui/defcomp dot []
  [ui/align {:y :center}
   [ui/clip {:radii [3]}
    [ui/rect {:paint (paint/fill 0xFFCDCCCA)}
     [dot-spacer]]]])

(ui/defcomp outline-item [*demo id]
  (let [{:keys [focused-id]} @*demo
        {:keys [text]} (get-in @*demo [:entities id])
        focused (= id focused-id)
        *state (cursor/cursor-in *demo [:input-states id])
        _ (swap! *state assoc :text text)]
    [ui/row
     (if (or focused (seq text))
       [dot]
       [dot-spacer])
     [ui/gap {:width 12 :height 0}]
     [ui/size  {:width 500}
      [text-field {:id id
                   :focused focused
                   :*demo *demo
                   :*state *state}]]]))

(ui/defcomp indentline []
  [ui/row
   ;; dot-size is 6px, with 1px line and 2px left gap the line is technically off center.
   ;; But if the dot size is an odd number and line centered, then it looks optically off.
   [ui/gap {:width 2 :height 0}]
   [ui/rect {:paint (paint/fill 0xFFEFEDEB)}
    [ui/gap {:width 1 :height 0}]]])

(ui/defcomp outline-tree [*demo items]
  [ui/row
   [ui/gap {:width 24 :height 0}]
   [ui/column
    (for [{:keys [id children]} items]
      [ui/column
       [outline-item *demo id]
       (when (seq children)
         [ui/row
          [indentline]
          [outline-tree *demo children]])])]])


(ui/defcomp app [*demo]
  [ui/padding {:vertical 15 :horizontal 15}
   [ui/rect {:paint (paint/fill 0xFFEFEDEB)}
    [ui/column
     [ui/vscroll
      [ui/padding {:padding 3}
       [ui/column {:gap 5}
        (let [items (->> (:entities @*demo)
                         (model/stratify))]
          [outline-tree *demo items])]]
      ]]]])














