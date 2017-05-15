package fitnesse.wikitext.widgets;

import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.project.*;
import org.codehaus.plexus.*;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLoggerManager;
import org.codehaus.plexus.util.Os;
import org.sonatype.aether.RepositorySystemSession;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class to extract classpath elements from Maven projects. Heavily based on code copied from Jenkin's Maven
 * support.
 */
public class MavenClasspathExtractor {

	public final static String DEFAULT_SCOPE = "test";

	private final Logger logger = new ConsoleLoggerManager().getLoggerForComponent("maven-classpath-plugin");
	
	private PlexusContainer plexusContainer;

    // Ensure M2_HOME variable is handled in a way similar to the mvn executable (script). To the extend possible.
    static {
        final String m2Home = System.getenv().get("M2_HOME");
        if (m2Home != null && System.getProperty("maven.home") == null) {
            System.setProperty("maven.home", m2Home);
        }
    }

    public MavenClasspathExtractor() throws PlexusContainerException {
    	plexusContainer = buildPlexusContainer(getClass().getClassLoader(), null);
    }
    
    public List<String> extractClasspathEntries(final File pomFile) throws MavenClasspathExtractionException {
		return extractClasspathEntries(pomFile, DEFAULT_SCOPE);
	}

    public List<String> extractClasspathEntries(final File pomFile, final String scope) throws MavenClasspathExtractionException {
        try {
            final MavenExecutionRequest mavenExecutionRequest = mavenConfiguration();
            mavenExecutionRequest.setBaseDirectory(pomFile.getParentFile());
            mavenExecutionRequest.setPom(pomFile);
            
            return getClasspathForScope(buildProject(pomFile, mavenExecutionRequest), scope);

        } catch (ComponentLookupException e) {
            throw new MavenClasspathExtractionException(e);
        } catch (DependencyResolutionRequiredException e) {
            throw new MavenClasspathExtractionException(e);
        } catch (ProjectBuildingException e) {
            throw new MavenClasspathExtractionException(e);
		}
    }

    private List<String> getClasspathForScope(ProjectBuildingResult projectBuildingResult, String scope) throws DependencyResolutionRequiredException {
        final MavenProject project = projectBuildingResult.getProject();

        if ("compile".equalsIgnoreCase(scope)) {
            return project.getCompileClasspathElements();
        } else if ("runtime".equalsIgnoreCase(scope)) {
            return project.getRuntimeClasspathElements();
        }
        return project.getTestClasspathElements();
    }

    // protected for test purposes
    protected MavenExecutionRequest mavenConfiguration() throws MavenClasspathExtractionException {
        final MavenExecutionRequest mavenExecutionRequest = new DefaultMavenExecutionRequest();

    	try {
			final MavenExecutionRequestPopulator executionRequestPopulator = lookup(MavenExecutionRequestPopulator.class);
	        final MavenExecutionRequestPopulator populator = lookup(MavenExecutionRequestPopulator.class);
	
	    	mavenExecutionRequest.setInteractiveMode(false);
	    	
	    	mavenExecutionRequest.setSystemProperties(System.getProperties());
	    	mavenExecutionRequest.getSystemProperties().putAll(getEnvVars());
	
	        executionRequestPopulator.populateDefaults(mavenExecutionRequest);
	        populator.populateDefaults(mavenExecutionRequest);
	        
	        logger.debug("Local repository " + mavenExecutionRequest.getLocalRepository());
		} catch (ComponentLookupException e) {
            throw new MavenClasspathExtractionException(e);
		} catch (MavenExecutionRequestPopulationException e) {
            throw new MavenClasspathExtractionException(e);
		}
        return mavenExecutionRequest;
    }

    private Properties getEnvVars() {
        final Properties envVars = new Properties();
        final boolean caseSensitive = !Os.isFamily(Os.FAMILY_WINDOWS);
        for (final Map.Entry<String, String> entry : System.getenv().entrySet()) {
            final String key = "env." + (caseSensitive ? entry.getKey() : entry.getKey().toUpperCase(Locale.ENGLISH));
            envVars.setProperty(key, entry.getValue());
        }
        return envVars;
    }


    public ProjectBuildingResult buildProject(File mavenProject, MavenExecutionRequest mavenExecutionRequest) throws ProjectBuildingException, ComponentLookupException {
        final ProjectBuildingRequest projectBuildingRequest = mavenExecutionRequest.getProjectBuildingRequest();
        projectBuildingRequest.setRepositorySession(buildRepositorySystemSession(mavenExecutionRequest));
        projectBuildingRequest.setProcessPlugins(false);
        projectBuildingRequest.setResolveDependencies(true);

        return lookup(ProjectBuilder.class).build(mavenProject, projectBuildingRequest);
    }
    
    public <T> T lookup(final Class<T> clazz) throws ComponentLookupException {
        return plexusContainer.lookup(clazz);
    }

    private RepositorySystemSession buildRepositorySystemSession(final MavenExecutionRequest mavenExecutionRequest) throws ComponentLookupException {
        return ((DefaultMaven) lookup(Maven.class)).newRepositorySession(mavenExecutionRequest);
    }

    public static PlexusContainer buildPlexusContainer(final ClassLoader mavenClassLoader, final ClassLoader parent) throws PlexusContainerException {
        final DefaultContainerConfiguration conf = new DefaultContainerConfiguration();

        final ClassWorld classWorld = new ClassWorld();

        final ClassRealm classRealm = new ClassRealm(classWorld, "maven", mavenClassLoader);
        final ClassLoader classLoader = parent == null ? Thread.currentThread().getContextClassLoader() : parent;
        classRealm.setParentRealm(new ClassRealm(classWorld, "maven-parent", classLoader));
        conf.setRealm(classRealm);

        return buildPlexusContainer(conf);
    }

    private static PlexusContainer buildPlexusContainer(final ContainerConfiguration containerConfiguration) throws PlexusContainerException {
        return new DefaultPlexusContainer(containerConfiguration);
    }
}
