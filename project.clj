(def slf4j-version "1.7.32")

(defproject meuse "1.3.0"
  :description "A free private Rust registry"
  :url "https://github.com/mcorbin/meuse"
  :license {:name "Eclipse Public License 2.0"}
  :maintainer {:name "Mathieu Corbin"
               :website "https://meuse.mcorbin.fr"}
  :dependencies [[amazonica "0.3.161"
                  :exclusions
                  [com.amazonaws/aws-java-sdk
                   com.amazonaws/amazon-kinesis-client]]
                 [bidi "2.1.6"]
                 [byte-streams "0.2.4"]
                 [cheshire "5.11.0"]
                 [clj-http "3.12.3"]
                 [clj-time "0.15.2"]
                 [com.amazonaws/aws-java-sdk-core "1.12.276"]
                 [com.amazonaws/aws-java-sdk-s3 "1.12.276"]
                 [com.zaxxer/HikariCP "5.0.1"]
                 [commons-codec/commons-codec "1.15"]
                 [crypto-password "0.3.0"]
                 [crypto-random "1.2.1"]
                 [digest "1.4.10"]
                 [environ "1.2.0"]
                 [exoscale/cloak "0.1.8"]
                 [exoscale/ex "0.4.0"]
                 [exoscale/interceptor "0.1.12"]
                 [exoscale/yummy "0.2.11"]
                 [hiccup "1.0.5"]
                 [honeysql "1.0.461"]
                 [io.micrometer/micrometer-registry-prometheus "1.9.2"]
                 [less-awful-ssl "1.0.6"]
                 [mount "0.1.16"]
                 [org.slf4j/slf4j-api                           ~slf4j-version]
                 [org.slf4j/log4j-over-slf4j                    ~slf4j-version]
                 [org.slf4j/jul-to-slf4j                        ~slf4j-version]
                 [org.slf4j/jcl-over-slf4j                      ~slf4j-version]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [seancorfield/next.jdbc "1.2.659"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.eclipse.jgit/org.eclipse.jgit "5.7.0.202003110725-r"]
                 [org.postgresql/postgresql "42.4.1"]
                 [ragtime "0.8.1"]
                 [ring/ring-core "1.9.5"]
                 [ring/ring-jetty-adapter "1.9.5"]
                 [spootnik/signal "0.2.4"]
                 [spootnik/unilog "0.7.30"]]
  :main ^:skip-aot meuse.core
  :target-path "target/%s"
  :source-paths ["src"]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "1.3.0"]
                                  [pjstadig/humane-test-output "0.11.0"]
                                  [tortue/spy "2.13.0"]
                                  [ring/ring-mock "0.4.0"]]
                   :global-vars    {*assert* true}
                   :cloverage {:test-ns-regex [#"^((?!meuse.integration).)*$"]
                               :ns-exclude-regex [#"meuse.core"
                                                  #"user"]}
                   :env {:meuse-configuration "dev/resources/config.yaml"}
                   :plugins [[lein-environ "1.1.0"]
                             [lein-cloverage "1.1.1"]
                             [lein-ancient "0.6.15"]
                             [lein-cljfmt "0.6.6"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]
                   :repl-options {:init-ns user}
                   :source-paths ["dev"]}
             :uberjar {:aot :all}}
  :test-selectors {:default (fn [x] (not (:integration x)))
                   :integration :integration})
