(ns shadow-cljs-example.main
  (:require ["react" :refer [createElement]]
            ["react-dom" :as react-dom]
            ["react-router-dom" :as rr]
            ["@material-ui/core" :as mui]
            ["@material-ui/icons" :as icons]
            [cljsx.core :as cljsx]))

(cljsx/jsx>
 (cljsx/defcomponent IconButtonLink props
   (<mui/IconButton ... props :component rr/Link >))

 (defn App []
   (<rr/BrowserRouter>
    (<rr/Route>
     (fn [js-route-props]
       (let [path (-> js-route-props .-location .-pathname)
             active-icon-name (subs path 1)
             ^js ActiveIcon (aget icons active-icon-name)]
         (<mui/Card>
          (<mui/CardHeader :title (if ActiveIcon
                                    active-icon-name
                                    "Click the icons")
                           :avatar (when ActiveIcon
                                     (<mui/Avatar>
                                      (<ActiveIcon>))) >)
          (<mui/CardContent>
           (for [icon-name (js/Object.keys icons)
                 :when (.endsWith icon-name "TwoTone")
                 :let [^js Icon (aget icons icon-name)]]
             (<IconButtonLink :key icon-name
                              :to icon-name
                              :color (if (= icon-name active-icon-name)
                                       "secondary"
                                       "default") >
                              (<Icon>))))))))))

 (react-dom/render
  (<App>)
  (js/document.querySelector "#mount-point")))
