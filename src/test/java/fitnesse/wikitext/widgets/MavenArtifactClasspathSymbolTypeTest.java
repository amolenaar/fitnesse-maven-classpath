package fitnesse.wikitext.widgets;

import fitnesse.wiki.PageData;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.fs.InMemoryPage;
import fitnesse.wikitext.parser.Symbol;
import fitnesse.wikitext.parser.SymbolProvider;
import fitnesse.wikitext.parser.Translator;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MavenArtifactClasspathSymbolTypeTest {

    private MavenArtifactClasspathSymbolType mavenArtifactClasspathSymbolType;
    private MavenArtifactClasspathExtractor mavenArtifactClasspathExtractor;
    private Symbol symbol;
    private WikiPage wikiPage;

    @Before
    public void setUp() throws Exception {
        System.clearProperty(MavenArtifactClasspathSymbolType.DISABLE_KEY);
        symbol = mock(Symbol.class);
        wikiPage = InMemoryPage.makeRoot("RooT");
        mavenArtifactClasspathExtractor = mock(MavenArtifactClasspathExtractor.class);

        mavenArtifactClasspathSymbolType = new MavenArtifactClasspathSymbolType();
        mavenArtifactClasspathSymbolType.setMavenArtifactClasspathExtractor(mavenArtifactClasspathExtractor);

        System.setProperty("MAVEN_SETTINGS_PATH","target/test-classes/user-settings.xml");
    }

    @Test
    public void testParsedSymbol() {
        String symbol = "groupId:artifactId:version:packaging:classifier@runtime";
        MavenArtifactClasspathSymbolType.ParsedSymbol parsedSymbol = new MavenArtifactClasspathSymbolType.ParsedSymbol(symbol);
        assertEquals("groupId", parsedSymbol.getGroupId());
        assertEquals("artifactId", parsedSymbol.getArtifactId());
        assertEquals("version", parsedSymbol.getVersion());
        assertEquals("packaging", parsedSymbol.getPackaging());
        assertEquals("classifier", parsedSymbol.getClassifier());
        assertEquals("runtime", parsedSymbol.getScope());
    }

    @Test
    public void testParsedSymbol_withoutClassifier() {
        String symbol = "groupId:artifactId:version:packaging@runtime";
        MavenArtifactClasspathSymbolType.ParsedSymbol parsedSymbol = new MavenArtifactClasspathSymbolType.ParsedSymbol(symbol);
        assertEquals("groupId", parsedSymbol.getGroupId());
        assertEquals("artifactId", parsedSymbol.getArtifactId());
        assertEquals("version", parsedSymbol.getVersion());
        assertEquals("packaging", parsedSymbol.getPackaging());
        assertEquals("", parsedSymbol.getClassifier());
        assertEquals("runtime", parsedSymbol.getScope());
    }

    @Test
    public void testParsedSymbol_withoutScope() {
        String symbol = "groupId:artifactId:version:packaging";
        MavenArtifactClasspathSymbolType.ParsedSymbol parsedSymbol = new MavenArtifactClasspathSymbolType.ParsedSymbol(symbol);
        assertEquals("groupId", parsedSymbol.getGroupId());
        assertEquals("artifactId", parsedSymbol.getArtifactId());
        assertEquals("version", parsedSymbol.getVersion());
        assertEquals("packaging", parsedSymbol.getPackaging());
        assertEquals("", parsedSymbol.getClassifier());
        assertEquals("test", parsedSymbol.getScope());
    }

    @Test
    public void testParsedSymbol_withoutPackaging() {
        String symbol = "groupId:artifactId:version";
        MavenArtifactClasspathSymbolType.ParsedSymbol parsedSymbol = new MavenArtifactClasspathSymbolType.ParsedSymbol(symbol);
        assertEquals("groupId", parsedSymbol.getGroupId());
        assertEquals("artifactId", parsedSymbol.getArtifactId());
        assertEquals("version", parsedSymbol.getVersion());
        assertEquals("jar", parsedSymbol.getPackaging());
        assertEquals("", parsedSymbol.getClassifier());
        assertEquals("test", parsedSymbol.getScope());
    }

    @Test
    public void testParsedSymbol_withoutVersion() {
        String symbol = "groupId:artifactId";
        MavenArtifactClasspathSymbolType.ParsedSymbol parsedSymbol = new MavenArtifactClasspathSymbolType.ParsedSymbol(symbol);
        assertEquals(null, parsedSymbol.getGroupId());
        assertEquals(null, parsedSymbol.getArtifactId());
        assertEquals(null, parsedSymbol.getVersion());
        assertEquals(null, parsedSymbol.getPackaging());
        assertEquals(null, parsedSymbol.getClassifier());
        assertEquals(null, parsedSymbol.getScope());
    }

    @Test
    public void translatesToClasspathEntries() throws MavenArtifactClasspathExtractionException {
        Symbol child = mock(Symbol.class);
      Translator translator = mock(Translator.class);

        when(symbol.childAt(0)).thenReturn(child);
        when(translator.translate(child)).thenReturn("g:a:v");

        when(mavenArtifactClasspathExtractor.extractClasspathEntries(any(MavenArtifactClasspathSymbolType.ParsedSymbol.class)))
                .thenReturn(Arrays.asList("test1", "test2"));

        assertEquals("<p class='meta'>Maven classpath [mavenArtifact: g:a:v, scope: test]:</p><ul class='meta'><li>test1</li><li>test2</li></ul>"
                , mavenArtifactClasspathSymbolType.toTarget(translator, symbol));
    }

    @Test
    public void translatesToJavaClasspath() throws MavenArtifactClasspathExtractionException {
        Symbol child = mock(Symbol.class);
        Translator translator = mock(Translator.class);

        when(symbol.childAt(0)).thenReturn(child);
        when(translator.translate(child)).thenReturn("g:a:v");

        when(mavenArtifactClasspathExtractor.extractClasspathEntries(any(MavenArtifactClasspathSymbolType.ParsedSymbol.class)))
                .thenReturn(Arrays.asList("test1", "test2"));

        assertArrayEquals(new Object[]{"test1", "test2"}, mavenArtifactClasspathSymbolType.providePaths(translator, symbol).toArray());
    }

    @Test
    public void loadPomXml() throws Exception {
        configureMavenArtifactClasspathSymbolType();
        PageData pageData = wikiPage.getData();
        pageData.setContent("!mavenArtifact commons-lang:commons-lang:2.6:jar\n");
        wikiPage.commit(pageData);
        String html = wikiPage.getHtml();
        assertTrue(html, html.startsWith("<p class='meta'>Maven classpath [mavenArtifact: commons-lang:commons-lang:2.6:jar, scope: test]:</p><ul class='meta'><li>"));
    }

    @Test
    public void loadPomXmlFromVariable() throws Exception {
        configureMavenArtifactClasspathSymbolType();
        PageData pageData = wikiPage.getData();
        pageData.setContent("!define MAVEN_ARTIFACT {commons-lang:commons-lang:2.6:jar}\n" +
                "!mavenArtifact ${MAVEN_ARTIFACT}\n");
        wikiPage.commit(pageData);
        String html = wikiPage.getHtml();
        assertTrue(html, html.contains("<p class='meta'>Maven classpath [mavenArtifact: commons-lang:commons-lang:2.6:jar, scope: test]:</p><ul class='meta'><li>"));
    }

    @Test
    public void canBeDisabled() throws Exception {
        System.setProperty(MavenArtifactClasspathSymbolType.DISABLE_KEY, "TRUE");
        mavenArtifactClasspathSymbolType = new MavenArtifactClasspathSymbolType();

        Symbol child = mock(Symbol.class);
        Translator translator = mock(Translator.class);

        when(symbol.childAt(0)).thenReturn(child);
        when(translator.translate(child)).thenReturn("g:a:v");

        assertEquals("<p class='meta'>Maven classpath [mavenArtifact: g:a:v, scope: test]:</p><ul class='meta'></ul>"
                , mavenArtifactClasspathSymbolType.toTarget(translator, symbol));
    }

    private void configureMavenArtifactClasspathSymbolType() throws Exception {
        SymbolProvider.wikiParsingProvider.add(new MavenArtifactClasspathSymbolType());
    }
}
