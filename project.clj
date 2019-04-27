(defproject meuse "0.1.0-SNAPSHOT"
  :description "A Rust registry"
  :url "https://github.com/mcorbin/meuse"
  :license {:name "CeCILL version 2.1"
            :url "http://www.cecill.info/licences.fr.html"}
  :maintainer {:name "Mathieu Corbin"
               :website "https://mcorbin.fr"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/java.jdbc "0.7.9"]
                 [org.clojure/tools.logging "0.4.1"]
                 [aleph "0.4.6"]
                 [bidi "2.1.5"]
                 [cheshire "5.8.1"]
                 [clj-time "0.15.1"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [crypto-password "0.2.0"]
                 [environ "1.1.0"]
                 [exoscale/yummy "0.2.6"]
                 [honeysql "0.9.4"]
                 [mount "0.1.16"]
                 [org.postgresql/postgresql "42.2.5"]
                 [ring/ring-core "1.7.1"]
                 [spootnik/signal "0.2.2"]
                 [spootnik/unilog "0.7.24"]]
  :main ^:skip-aot meuse.core
  :target-path "target/%s"
  :source-paths ["src"]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [pjstadig/humane-test-output "0.8.2"]
                                  [tortue/spy "1.6.0"]
                                  [ring/ring-mock "0.3.0"]
                                  [commons-io/commons-io 2.6]]
                   :cloverage {:ns-exclude-regex [#"meuse.core"
                                                  #"user"]}
                   :env {:meuse-configuration "dev/resources/config.yaml"}
                   :plugins [[lein-environ "1.1.0"]
                             [lein-cloverage "1.1.1"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]
                   :repl-options {:init-ns user}
                   :source-paths ["dev"]}
             :uberjar {:aot :all}})
