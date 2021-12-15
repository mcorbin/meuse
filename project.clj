(defproject meuse "1.2.0"
  :description "A free private Rust registry"
  :url "https://github.com/mcorbin/meuse"
  :license {:name "Eclipse Public License 2.0"}
  :maintainer {:name "Mathieu Corbin"
               :website "https://meuse.mcorbin.fr"}
  :dependencies [[amazonica "0.3.156"
                  :exclusions
                  [com.amazonaws/aws-java-sdk
                   com.amazonaws/amazon-kinesis-client]]
                 [bidi "2.1.6"]
                 [byte-streams "0.2.4"]
                 [cheshire "5.10.1"]
                 [clj-http "3.12.3"]
                 [clj-time "0.15.2"]
                 [com.amazonaws/aws-java-sdk-core "1.12.84"]
                 [com.amazonaws/aws-java-sdk-s3 "1.12.84"]
                 [com.zaxxer/HikariCP "3.4.5"]
                 [commons-codec/commons-codec "1.15"]
                 [crypto-password "0.3.0"]
                 [crypto-random "1.2.1"]
                 [digest "1.4.10"]
                 [environ "1.2.0"]
                 [exoscale/cloak "0.1.8"]
                 [exoscale/ex "0.3.18"]
                 [exoscale/interceptor "0.1.9"]
                 [exoscale/yummy "0.2.11"]
                 [hiccup "1.0.5"]
                 [honeysql "1.0.461"]
                 [io.micrometer/micrometer-registry-prometheus "1.7.4"]
                 [less-awful-ssl "1.0.6"]
                 [mount "0.1.16"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [seancorfield/next.jdbc "1.2.659"]
                 [org.clojure/tools.logging "1.2.2"]
                 [org.eclipse.jgit/org.eclipse.jgit "5.7.0.202003110725-r"]
                 [org.postgresql/postgresql "42.2.24"]
                 [ragtime "0.8.1"]
                 [ring/ring-core "1.9.4"]
                 [ring/ring-jetty-adapter "1.9.4"]
                 [spootnik/signal "0.2.4"]
                 [spootnik/unilog "0.7.29"]]
  :main ^:skip-aot meuse.core
  :target-path "target/%s"
  :source-paths ["src"]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "1.1.0"]
                                  [pjstadig/humane-test-output "0.11.0"]
                                  [tortue/spy "2.9.0"]
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
                             [lein-cljfmt "0.6.6"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]
                   :repl-options {:init-ns user}
                   :source-paths ["dev"]}
             :uberjar {:aot :all}}
  :test-selectors {:default (fn [x] (not (:integration x)))
                   :integration :integration})
