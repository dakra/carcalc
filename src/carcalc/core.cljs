(ns carcalc.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   ;; ["@material-ui/core/colors/purple" :refer [purple]]
   ;; ["@material-ui/core/colors/green" :refer [green]]
   ["@material-ui/core" :refer [Typography Container Button Grid Icon Slider Paper ThemeProvider createMuiTheme]]
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
   :paid 40000
   :new 60000
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
 ::set-years
 (fn-traced [db [_ years]]
   (assoc db :years years)))

;;; Subs
(rf/reg-sub
 ::db
 (fn [db]
   db))

(rf/reg-sub
 ::years
 (fn [db]
   (:years db)))

(defn input-grid [opts]
  (let [years @(rf/subscribe [::years])]
    [:> Grid (into {:item true :container true :spacing 2 :align-items "center" :xs 6} opts)
     [:> Grid {:item true :xs 12}
      [:> Typography {:gutter-bottom true} "Years"]
      [:> Slider {:value years
                  :min 3
                  :max 9
                  :on-change (fn [_event value] (rf/dispatch [::set-years value]))}]]
     [:> Grid {:item true :xs 12}
      [:> Typography {:gutter-bottom true} "New Price"]
      [:> Slider {:value years
                  :min 5000
                  :max 200000
                  :on-change (fn [_event value] (rf/dispatch [::set-years value]))}]]
     [:> Grid {:item true :xs 2}
      [:> Button {:variant "contained" :color "primary"} "foo"]]
     [:> Grid {:item true :xs 2}
      [:> Button {:variant "contained" :color "primary"} "bar"]]]
    ))

(defn output-grid [opts]
  (let [years @(rf/subscribe [::years])]
    [:> Grid (into {:item true :container true :spacing 2 :align-items "center" :xs 6} opts)
     [:> Grid {:item true :xs 12}
      [:> Paper
       [:div (str "Years: " years)]]
      [:> Paper
       [:div (str "Years: " years)]]
      ]]))

;;; Views
(defn main-shell []
  (let [db @(rf/subscribe [::db])
        years @(rf/subscribe [::years])
        theme (createMuiTheme (clj->js {:palette {:type "light"}
                                        :status {:danger "orange"}}))]
    [:> ThemeProvider {:theme theme}
     [:> Container {:max-width "md"}
      [:> Grid {:container true :spacing 2 :align-items "center"}
       [input-grid {:xs 6}]
       [output-grid {:xs 6}]
       ]]]))

;;; Core
(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render [main-shell]
                  (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [::initialize-db])
  (dev-setup)
  (mount-root))
