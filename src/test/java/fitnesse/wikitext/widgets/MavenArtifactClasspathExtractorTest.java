package fitnesse.wikitext.widgets;

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

public class MavenArtifactClasspathExtractorTest {
    @Test
    public void extractedClasspathIncludesTestScopeDependencies() throws Exception {
        System.setProperty("MAVEN_SETTINGS_PATH","target/test-classes/user-settings.xml");
        MavenArtifactClasspathExtractor extractor = new MavenArtifactClasspathExtractor();
        List<String> classpath = extractor.extractClasspathEntries(new MavenArtifactClasspathSymbolType.ParsedSymbol("fitnesse:fitnesse-dep:1.0"));
        Assert.assertFalse(classpath.isEmpty());

        Iterator<String> classpathIter = classpath.iterator();
        while (classpathIter.hasNext()) {
            String classpathEntry = classpathIter.next();
            if(classpathEntry.contains("fitnesse-dep-1.0.jar")) continue;
            if(classpathEntry.contains("fitnesse-subdep-1.0.jar")) continue;
            classpathIter.remove();
        }
        Assert.assertEquals(2, classpath.size());
    }
}
