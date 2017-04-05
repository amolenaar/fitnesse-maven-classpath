package fitnesse.wikitext.widgets;

import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLoggerManager;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to extract classpath elements from Maven projects. Heavily based on code copied from Jenkin's Maven
 * support.
 */
public class MavenArtifactClasspathExtractor {

    public final static String DEFAULT_PACKAGING = "jar";
	public final static String DEFAULT_SCOPE = "test";

	private final Logger logger = new ConsoleLoggerManager().getLoggerForComponent("maven-classpath-plugin");

    // Ensure M2_HOME variable is handled in a way similar to the mvn executable (script). To the extend possible.
    static {
        String m2Home = System.getenv().get("M2_HOME");
        if (m2Home != null && System.getProperty("maven.home") == null) {
            System.setProperty("maven.home", m2Home);
        }
    }

    public List<String> extractClasspathEntries(MavenArtifactClasspathSymbolType.ParsedSymbol parsedSymbol) throws MavenArtifactClasspathExtractionException {
        try {
            File pomFile = createPomFile(parsedSymbol);

            File dependencyList = invokeDependencyList(pomFile);

            pomFile.delete();

            List<String> classpath = parseClasspath(dependencyList);

            dependencyList.delete();

            return classpath;
        } catch (IOException e) {
            throw new MavenArtifactClasspathExtractionException("Could not extract classpath", e);
        }
    }

    private List<String> parseClasspath(File dependencyList) throws MavenArtifactClasspathExtractionException {
        Pattern pattern = Pattern.compile("(.*):([^:]+)");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(dependencyList));
            while (!"The following files have been resolved:".equals(reader.readLine())) ;
            List<String> classpath = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    // group 1 contains the path to the file
                    classpath.add(matcher.group(2));
                }
            }
            return classpath;
        } catch (IOException e) {
            throw new MavenArtifactClasspathExtractionException("Could not extract classpath", e);
        } finally {
            if(reader!=null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new MavenArtifactClasspathExtractionException("Could not extract classpath", e);
                }
            }
        }
    }

    private File invokeDependencyList(File pomFile) throws IOException, MavenArtifactClasspathExtractionException {
        DefaultInvocationRequest request = new DefaultInvocationRequest();
        request.setMavenOpts(System.getProperty("MAVEN_OPTS"));
        request.setUserSettingsFile(new File(System.getProperty("MAVEN_SETTINGS_PATH")));
        request.setBatchMode(true);
        request.setUpdateSnapshots(true);
        request.setGoals(Arrays.asList("-f " + pomFile.getPath(), "dependency:list"));

        File dependencyList = File.createTempFile("fitnesse-maven-classpath-", ".deps");
        Properties properties = new Properties();
        properties.setProperty("outputFile", dependencyList.getPath()); // redirect output to a file
        properties.setProperty("outputAbsoluteArtifactFilename", "true"); // with paths
        request.setProperties(properties);

        DefaultInvoker defaultInvoker = new DefaultInvoker();
//        defaultInvoker.setOutputHandler(null); // not interested in Maven output itself
        try {
            InvocationResult result = defaultInvoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("Build failed.");
            }
        } catch (MavenInvocationException e) {
            throw new MavenArtifactClasspathExtractionException("Could not extract classpath", e);
        }
        return dependencyList;
    }

    private File createPomFile(MavenArtifactClasspathSymbolType.ParsedSymbol parsedSymbol) throws IOException {
        File pomFile = File.createTempFile("fitnesse-maven-classpath-", ".pom");

        PrintWriter writer = new PrintWriter(pomFile);
        writer.println("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        writer.println("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">");
        writer.println("    <modelVersion>4.0.0</modelVersion>");
        writer.println("");
        writer.println("    <groupId>org.fitnesse.plugins</groupId>");
        writer.println("    <artifactId>maven-classpath-plugin</artifactId>");
        writer.println("    <version>1.10-SNAPSHOT</version>");
        writer.println("");
        writer.println("    <dependencies>");
        writer.println("        <dependency>");
        writer.println("            <groupId>"+parsedSymbol.getGroupId()+"</groupId>");
        writer.println("            <artifactId>"+parsedSymbol.getArtifactId()+"</artifactId>");
        writer.println("            <version>"+parsedSymbol.getVersion()+"</version>");
        writer.println("            <scope>"+parsedSymbol.getScope()+"</scope>");
        writer.println("            <classifier>"+parsedSymbol.getClassifier()+"</classifier>");
        writer.println("            <type>"+parsedSymbol.getPackaging()+"</type>");
        writer.println("        </dependency>");
        writer.println("    </dependencies>");
        writer.println("</project>");
        writer.close();
        return pomFile;
    }
}
