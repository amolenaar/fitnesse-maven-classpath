# Introduction.

FitNesse plugin that provides Maven Classpath support 
and let maven synchronize local repository with remote repo before tests starts.

Maven must be installed and executable by FitNesse.

# How to use.

 - Download the distribution.
 - Get yourself an up-to-date copy of fitnesse (>= 20150114)
 - Add the following line to plugins.properties:
 
       SymbolTypes = fitnesse.wikitext.widgets.MavenClasspathSymbolType,fitnesse.wikitext.widgets.MavenArtifactClasspathSymbolType

 - Refer to the maven artifact as follows (dependencies will be downloaded by maven):
 
       !mavenArtifact groupId:artifactId:version[:packaging[:classifier]][@scope]

 - Refer to the pom file as follows:

       !pomFile /path/to/pom.xml

 - you can define the file as `pom.xml@compile` to include a specific scope (default scope is 'test').

# How to contribute.

 - Fork the repository and send pull requests.


