(defproject cljsx "1.0.0"
  :description "JSX for Clojure and ClojureScript"
  :url "https://github.com/peterhudec/cljsx"
  :license {:author "Peter Hudec"
            :email "peterhudec@peterhudec.com"
            :year 2019
            :key "mit"
            :name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}
  :source-paths ["src" "test" "examples"]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [expound "0.7.2"]]
  :aliases {"fig"
            ["trampoline" "run" "-m" "figwheel.main"]

            "fig:build"
            ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]

            "lint"
            ["do" "cljfmt" "check," "bikeshed," "eastwood"]

            "test:clj"
            ["midje"]

            "test:cljs"
            ["shell" "npm" "run" "test:cljs"]

            "test:watch"
            ["shell" "npm" "run" "test:watch"]

            "example:watch"
            ["shell" "npm" "start"]

            "test"
            ["do" "test:clj," "test:cljs"]

            "check"
            ["do" "test," "lint"]

            "travis"
            ["do" "test:clj," "lint"]}
  :profiles {:dev {:resource-paths ["target"]
                   :clean-targets ^{:protect false} ["target"]
                   :plugins [[lein-cljfmt "0.6.4"]
                             [lein-bikeshed "0.5.2"]
                             [lein-midje "3.2.1"]
                             [lein-shell "0.5.0"]
                             [jonase/eastwood "0.3.5"]]
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
