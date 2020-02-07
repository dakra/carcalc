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

(rf/reg-sub
 ::write-off
 (fn [_ _]
   [(rf/subscribe [::db])
    (rf/subscribe [::paid])])
 (fn [[db paid] _]
   (let [{:keys [years fuel repair insurance]} db
         car (/ paid years)
         sum (+ car fuel repair insurance)]
     {:car car
      :fuel fuel  ;; TODO Calc from km
      :repair repair
      :insurance insurance
      :sum sum})))

(rf/reg-sub
 ::one%
 (fn [db]
   (* 12 (/ (:new-price db) 100))))

(rf/reg-sub
 ::pendler
 (fn [db]
   {:one-year (* (* (:km db) 0.3) 0.4)
    :total (* (:years db) (* (* (:km db) 0.3) 0.4))}))

(rf/reg-sub
 ::total-off
 (fn [_ _]
   [(rf/subscribe [::years])
    (rf/subscribe [::write-off])
    (rf/subscribe [::one%])])
 (fn [[years write-off one] _]
   (let [{:keys [car fuel repair insurance sum]} write-off
         total (- sum one)
         all-total (* 5 total)]
     {:one-year-total sum
      :one-percent one
      :all-total all-total})))

;;; Components

(defn input-slider [key opts]
  (let [defaults {:min 0 :max 100000 :step 1000 :desc (name key)}
        {:keys [min max step desc]} (merge defaults opts)
        val (get @(rf/subscribe [::db]) key)
        ;; useStyles (makeStyles {:root {:width 250} :input {:width 42}})
        ;; classes (useStyles)
        ]
    [:> Grid {:item true :xs 12}  ; :div ; {:class (.-root classes)}
     [:> Typography {:id "input-slider" :gutter-bottom true} desc]
     [:> Grid {:container true :item true :spacing 2 :align-items "center" :xs 12}
      [:> Grid {:item true :xs true}
       [:> Slider {:value val
                   :min min
                   :max max
                   :on-change #(rf/dispatch [::set-db {key %2}])}]]
      [:> Grid {:item true :xs 2}
       [:> Input {;:class (.-input classes)
                  :margin "dense"
                  :value val
                  :on-change #(rf/dispatch [::set-db {key (-> % .-target .-value)}])
                  :input-props {:type "number"
                                :step step
                                :min min
                                :max max}}]]]]))

(defn input-grid [opts]
  (let [grid-defaults {:item true :container true :spacing 2 :align-items "center" :xs 6}
        new-car? @(rf/subscribe [::new-car?])]
    [:> Grid (merge grid-defaults opts)
     [:> Grid {:item true :xs 12}
      [:> FormGroup {:row true}
       [:> FormControlLabel
        {:label "New car?"
         :control (reagent/as-element
                   [:> Switch {:checked new-car? :on-change #(rf/dispatch [::set-new-car %2])}])}]]]

     [input-slider :new-price {:min 20000 :max 100000 :name "New Car Price"}]
     (when-not new-car?
       [input-slider :paid {:min 10000 :max 100000 :name "Price Paid"}])
     [input-slider :km {:min 5000 :max 50000}]
     [input-slider :fuel {:min 0 :max 5000 :step 100}]
     [input-slider :repair {:min 0 :max 3000 :step 100}]
     [input-slider :insurance {:min 0 :max 2000 :step 100}]

     [:> Grid {:item true :xs 2}
      [:> Button {:variant "contained" :color "primary"} "foo"]]
     [:> Grid {:item true :xs 2}
      [:> Button {:variant "contained" :color "primary"} "bar"]]])
  )

(defn paper-db []
  (let [{:keys [years new-price km fuel repair insurance]} @(rf/subscribe [::db])
        paid @(rf/subscribe [::paid])]
    [:> Grid {:item true :xs 12}
     [:> Typography {:variant "h5"} "Input"]
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
      [:div (str "Insurance: " insurance)]]]))

(defn paper-write-off []
  (let [{:keys [car fuel repair insurance sum]} @(rf/subscribe [::write-off])
        {:keys [one-percent all-total]} @(rf/subscribe [::total-off])]
    [:> Grid {:item true :xs 12}
     [:> Typography {:variant "h5"} "Write offs per year"]
     [:> Paper
      [:div (str "Car: " car)]]
     [:> Paper
      [:div (str "Fuel: " fuel)]]
     [:> Paper
      [:div (str "Repair: " repair)]]
     [:> Paper
      [:div (str "Insurance: " insurance)]]
     [:> Paper
      [:div (str "Total: " sum)]]
     [:> Paper
      [:div (str "1%: " one-percent)]]
     [:> Paper
      [:div (str "All Total: " all-total)]]]))

(defn paper-pendler []
  (let [{:keys [one-year total]} @(rf/subscribe [::pendler])]
    [:> Grid {:item true :xs 12}
     [:> Typography {:variant "h5"} "Pendlerpauschale"]
     [:> Paper
      [:div (str "Pendler / Jahr: " one-year)]]
     [:> Paper
      [:div (str "Pendler Total: " total)]]]))

(defn output-grid [opts]
  (let []
    [:> Grid (into {:item true :container true :spacing 2 :align-items "center" :xs 6} opts)

     [paper-db]

     [paper-write-off]

     [paper-pendler]]))

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
