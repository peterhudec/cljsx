(ns shadow-cljs-example.main
  (:require ["react" :as react]
            ["react-dom" :as react-dom]
            ["react-router-dom" :as rr]
            ["@material-ui/core" :as mui]
            ["@material-ui/icons" :as icons]
            [cljsx.core :as cljsx]))

(cljsx/react>>>
 (cljsx/defcomponent IconButtonLink props
   (<mui/IconButton ... props :component rr/Link >))

 (defn App []
   (<rr/BrowserRouter>
    (<rr/Route>
     (fn [js-route-props]
       (let [path (-> js-route-props .-location .-pathname)
             active-icon-name (subs path 1)
             ^js ActiveIcon (aget icons active-icon-name)
             names (->> icons js/Object.keys (filter #(.endsWith % "Tone")))]
         (<mui/Card>
          (<mui/CardHeader :title (when ActiveIcon active-icon-name)
                           :avatar (when ActiveIcon
                                     (<mui/Avatar> (<ActiveIcon>))) >)
          (<mui/CardContent>
           (map #(let [^js Icon (aget icons %)
                       color (if (= % active-icon-name) "secondary" "default")]
                   (<IconButtonLink :key % :to % :color color >
                                    (<Icon>)))
                names))))))))

 (react-dom/render
  (<App>)
  (js/document.querySelector "#mount-point")))
