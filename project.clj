(defproject cljsx "0.0.1-SNAPSHOT"
  :description "Cool new project to do things and stuff"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.520"]]
  :aliases {"fig"
            ["trampoline" "run" "-m" "figwheel.main"]

            "fig:build"
            ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]}
  :profiles {:dev {:resource-paths ["target"]
                   :clean-targets ^{:protect false} ["target"]
                   ;; TODO: Move midje to a separate profile.
                   :dependencies [[midje "1.9.8"]
                                  ;; If we don't explicitly require tools.reader
                                  ;; we get the "reader-error does not exist"
                                  ;; error.
                                  ;; https://github.com/ptaoussanis/timbre/issues/263#issuecomment-402115822
                                  [org.clojure/tools.reader "1.2.2"]
                                  [com.bhauman/figwheel-main "0.2.0"]
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]
                                  [cljsjs/react "16.8.3-0"]
                                  [cljsjs/react-dom "16.8.3-0"]]}
             :midje {}})

