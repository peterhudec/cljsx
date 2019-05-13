(ns shadow-cljs-example.main
  (:require ["react" :as react]
            ["react-dom" :refer [render]]
            ["react-router-dom" :as rr]
            ["@material-ui/core" :as m]
            ["@material-ui/icons" :as i]
            [cljsx.core :refer [react>>> defcomponent]]))

(react>>>

 (defn Card []
   (<m/Card>
    (<m/CardHeader :avatar (<m/Avatar>
                            (<i/Face>))
                   :action (<m/IconButton>
                            (<i/MoreVert>))
                   :title "Andy & Lou"
                   :subheader "Have you got a bandage?"
                   >)
    (<m/CardMedia :title "Andy & Lou"
                  :style {:paddingTop "50%"}
                  :image "https://bit.ly/2JhbEFN"
                  >)
    (<m/CardContent>
     (<m/Typography :component "p" >
                    "Lorem ipsum dolor sit amet"))
    (<m/CardActions>)))

 (defcomponent RoutedTab props
   (<m/Tab ... props
           :component rr/Link
           >))

 (render
  (<rr/BrowserRouter>
   (<div>
    (<rr/Route>
     (fn [router-props]
       (<m/Tabs :centered
                :value (-> router-props
                           .-location
                           .-pathname
                           (subs 1))
                :indicatorColor "primary"
                :textColor "primary"
                >
                (map #(<RoutedTab :key %
                                  :label %
                                  :value %
                                  :to %
                                  >)
                     ["foo" "bar" "baz"]))))))
  (js/document.querySelector "#mount-point")))
