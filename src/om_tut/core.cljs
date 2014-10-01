(ns om-tut.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [clojure.data :as data]
            [clojure.string :as string]))

(enable-console-print!)

(def app-state
  (atom
    {:beers
      [ {:name "Road 2 Ruin" :ibu 99 :abv 4.0 :brewery "Two Roads"}
        {:name "Roadsmary's Baby" :ibu 69 :abv 5.0 :brewery "Two Roads"}]}))

(defn display-name [{:keys [:name :brewery] :as beer}]
  (str name " (" brewery ")"))

(defn parse-beer [beer-str]
  (let [[name brewery abv ibu :as parts] (string/split beer-str #":")]
        {:name name :brewery brewery :abv abv :ibu ibu}))

(defn add-beer [app owner]
  (let [new-beer (-> (om/get-node owner "new-beer") .-value parse-beer)]
    (when new-beer
      (om/transact! app :beers #(conj % new-beer))
      (om/set-state! owner :text ""))))

(defn beer-view [beer owner]
  (reify
    om/IRenderState
      (render-state [this {:keys [delete]}]
        (dom/li nil
          (dom/span nil (display-name beer))
            (dom/button #js {:onClick (fn [e] (put! delete @beer))} "Delete")))))

(defn handle-change [e owner {:keys [text]}]
  (om/set-state! owner :text (.. e -target -value)))

(defn beers-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:delete (chan)
       :text ""})
    om/IWillMount
    (will-mount [_]
      (let [delete (om/get-state owner :delete)]
        (go (loop []
          (let [beer (<! delete)]
            (om/transact! app :beers
              (fn [xs] (vec (remove #(= beer %) xs))))
            (recur))))))
    om/IRenderState
    (render-state [this state]
      (dom/div nil
        (dom/h2 nil "beer list")
        (apply dom/ul nil
          (om/build-all beer-view (:beers app)
            {:init-state state}))
        (dom/div nil
          (dom/input #js {:js "text" :ref "new-beer" :value (:text state)
                          :onChange #(handle-change % owner state)})
          (dom/button #js {:onClick #(add-beer app owner)} "Add Beer"))))))

(om/root
  beers-view
  app-state
  {:target (. js/document (getElementById "beers"))})
