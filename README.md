# Introduction.

FitNesse plugin that provides Maven Classpath support.

# How to use.

 - Download the distribution (jar-with-dependencies.jar from https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.fitnesse.plugins%22%20AND%20a%3A%22maven-classpath-plugin%22).
 - Get yourself an up-to-date copy of fitnesse (>= 20150226), and place the jar in fitnesse's plugins directory

 - Refer to the pom file as follows:
 
       !pomFile /path/to/pom.xml
        
 - you can define the file as `pom.xml@compile` to include a specific scope.

 - if you want to disable this plugin (for instance during a jUnit run with all plugins on classpath): set the system property `fitnesse.wikitext.widgets.MavenClasspathSymbolType.Disable` to `true`


# How to contribute.

 - Fork the repository and send pull requests.


