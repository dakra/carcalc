(ns carcalc.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   ;; ["@material-ui/core/colors/purple" :refer [purple]]
   ;; ["@material-ui/core/colors/green" :refer [green]]
   ["@material-ui/core" :refer [Typography Container Button Grid Icon Slider Paper ThemeProvider createMuiTheme
                                makeStyles Input Switch FormGroup FormControlLabel]]
   ;; [re-com.core :as rc]
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]))

;;; Config
(def debug?
  ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (println "dev mode")))

;;; DB
(def default-db
  {:years 6
   :new-price 60000
   :paid 40000
   :km  20000
   :fuel 2500
   :repair 1500
   :insurance 1000})

(defn write-off [amount years]
  (/ amount years))
(write-off 40000 5)

;;; Events
(rf/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   default-db))

(rf/reg-event-db
 ::set-db
 (fn-traced [db [_ kw]]
   (into db kw)))

(rf/reg-event-db
 ::set-years
 (fn-traced [db [_ years]]
   (assoc db :years years)))

(rf/reg-event-db
 ::set-new-car
 (fn-traced [db [_ new-car?]]
   (assoc db :years (if new-car? 6 5))))

(rf/reg-event-db
 ::set-new-price
 (fn-traced [db [_ new-price]]
   (assoc db :new-price new-price)))

(rf/reg-event-db
 ::set-paid
 (fn-traced [db [_ paid]]
   (assoc db :paid paid)))

;;; Subs
(rf/reg-sub
 ::db
 (fn [db]
   db))

(rf/reg-sub
 ::years
 (fn [db]
   (:years db)))

(rf/reg-sub
 ::new-car?
 (fn [db]
   (= (:years db) 6)))

(rf/reg-sub
 ::paid
 (fn [_ _]
   [(rf/subscribe [::new-car?])
    (rf/subscribe [::db])])
 (fn [[new-car? db] _]
   (if new-car? (:new-price db) (:paid db))))

;;; Components

(defn input-slider [key max]
  (let [val (get @(rf/subscribe [::db]) key)
        ;; useStyles (makeStyles {:root {:width 250} :input {:width 42}})
        ;; classes (useStyles)
        ]
    [:> Grid {:item true :xs 12}  ; :div ; {:class (.-root classes)}
     [:> Typography {:id "input-slider" :gutter-bottom true} (name key)]
     [:> Grid {:container true :item true :spacing 2 :align-items "center" :xs 12}
      [:> Grid {:item true :xs true}
       [:> Slider {:value val
                   :min 0
                   :max max
                   :on-change #(rf/dispatch [::set-db {key %2}])}]]
      [:> Grid {:item true :xs 2}
       [:> Input {;:class (.-input classes)
                  :margin "dense"
                  :value val
                  :on-change #(rf/dispatch [::set-db {key (-> % .-target .-value)}])
                  :input-props {:type "number"
                                :step 1000
                                :min 0
                                :max max}}]]]]))

(defn input-grid [opts]
  (let [new-car? @(rf/subscribe [::new-car?])]
    [:> Grid (into {:item true :container true :spacing 2 :align-items "center" :xs 6} opts)
     [:> Grid {:item true :xs 12}
      [:> FormGroup {:row true}
       [:> FormControlLabel
        {:label "New car?"
         :control (reagent/as-element
                   [:> Switch {:checked new-car? :on-change #(rf/dispatch [::set-new-car %2])}])}]]]

     [input-slider :new-price 200000]
     (when-not new-car?
       [input-slider :paid 200000])
     [input-slider :km 50000]
     [input-slider :fuel 10000]
     [input-slider :repair 3000]
     [input-slider :insurance 2000]

     [:> Grid {:item true :xs 2}
      [:> Button {:variant "contained" :color "primary"} "foo"]]
     [:> Grid {:item true :xs 2}
      [:> Button {:variant "contained" :color "primary"} "bar"]]])
  )

(defn output-grid [opts]
  (let [{:keys [years new-price km fuel repair insurance]} @(rf/subscribe [::db])
        paid @(rf/subscribe [::paid])]
    [:> Grid (into {:item true :container true :spacing 2 :align-items "center" :xs 6} opts)
     [:> Grid {:item true :xs 12}
      [:> Paper
       [:div (str "Years: " years)]]
      [:> Paper
       [:div (str "New Price: " new-price)]]
      [:> Paper
       [:div (str "Paid: " paid)]]
      [:> Paper
       [:div (str "KM: " km)]]
      [:> Paper
       [:div (str "Fuel: " fuel)]]
      [:> Paper
       [:div (str "Repair: " repair)]]
      [:> Paper
       [:div (str "Incurance: " insurance)]]]]))

;;; Views
(defn main-shell []
  (let [theme (createMuiTheme (clj->js {:palette {:type "light"}
                                        :status {:danger "orange"}}))]
    [:> ThemeProvider {:theme theme}
     [:> Container {:max-width "md"}
      [:> Grid {:container true :spacing 2 :align-items "center"}
       [input-grid {:xs 6}]
       [output-grid {:xs 6}]]]]))

;;; Core
(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render [main-shell]
                  (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [::initialize-db])
  (dev-setup)
  (mount-root))
