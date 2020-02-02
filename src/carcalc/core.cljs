(ns carcalc.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   ;; [re-com.core :as rc]
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]))

;;; Config
(def debug?
  ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (println "dev mode")))

;;; DB
(def default-db {:blub "bla"})

;;; Events
(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   default-db))

;;; Subs
(re-frame/reg-sub
 ::db
 (fn [db]
   db))

;;; Views
(defn main-shell []
  (let [db @(re-frame/subscribe [::db])]
    [:div [:p (with-out-str (prn db))]]))

;;; Core
(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [main-shell]
                  (.getElementById js/document "app")))

(defn init! []
  (re-frame/dispatch-sync [::initialize-db])
  (dev-setup)
  (mount-root))
