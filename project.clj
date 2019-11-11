(defproject meuse "0.2.0"
  :description "A free private Rust registry"
  :url "https://github.com/mcorbin/meuse"
  :license {:name "Eclipse Public License 1.0"}
  :maintainer {:name "Mathieu Corbin"
               :website "https://meuse.mcorbin.fr"}
  :dependencies [[aleph "0.4.7-alpha5"]
                 [amazonica "0.3.139"
                  :exclusions
                  [com.amazonaws/aws-java-sdk
                   com.amazonaws/amazon-kinesis-client]]

                 [com.amazonaws/aws-java-sdk-core "1.11.495"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.495"]
                 [bidi "2.1.6"]
                 [cc.qbits/ex "0.1.3"]
                 [cheshire "5.9.0"]
                 [clj-time "0.15.2"]
                 [digest "1.4.9"]
                 [com.zaxxer/HikariCP "3.4.1"]
                 [crypto-password "0.2.1"]
                 [crypto-random "1.2.0"]
                 [environ "1.1.0"]
                 [exoscale/yummy "0.2.8"]
                 [honeysql "0.9.8"]
                 [io.micrometer/micrometer-registry-prometheus "1.3.1"]
                 [less-awful-ssl "1.0.4"]
                 [metosin/spec-tools "0.10.0"]
                 [mount "0.1.16"]
                 [commons-codec/commons-codec "1.13"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.clojure/tools.logging "0.5.0"]
                 [org.postgresql/postgresql "42.2.8"]
                 [ring/ring-core "1.7.1"]
                 [spootnik/signal "0.2.4"]
                 [spootnik/unilog "0.7.25"]]
  :main ^:skip-aot meuse.core
  :target-path "target/%s"
  :source-paths ["src"]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.3.1"]
                                  [pjstadig/humane-test-output "0.10.0"]
                                  [tortue/spy "2.0.0"]
                                  [clj-http "3.10.0"]
                                  [ring/ring-mock "0.4.0"]
                                  [commons-io/commons-io 2.6]]
                   :global-vars    {*assert* true}
                   :cloverage {:test-ns-regex [#"^((?!meuse.integration).)*$"]
                               :ns-exclude-regex [#"meuse.core"
                                                  #"user"]}
                   :env {:meuse-configuration "dev/resources/config.yaml"}
                   :plugins [[lein-environ "1.1.0"]
                             [lein-cloverage "1.1.1"]
                             [lein-ancient "0.6.15"]
                             [lein-cljfmt "0.6.5"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]
                   :repl-options {:init-ns user}
                   :source-paths ["dev"]}
             :uberjar {:aot :all}}
  :test-selectors {:default (fn [x] (not (:integration x)))
                   :integration :integration})
