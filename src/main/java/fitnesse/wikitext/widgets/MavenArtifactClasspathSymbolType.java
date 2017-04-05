package fitnesse.wikitext.widgets;

import fitnesse.wikitext.parser.*;
import fitnesse.wikitext.parser.Matcher;
import org.codehaus.plexus.PlexusContainerException;

import java.util.*;
import java.util.regex.*;

/**
 * FitNesse SymbolType implementation. Enables Maven classpath integration for FitNesse.
 */
public class MavenArtifactClasspathSymbolType extends SymbolType implements Rule, Translation, PathsProvider {
    /**
     * System property to disable this Symbol (if given value true).
     */
    public static final String DISABLE_KEY = "fitnesse.wikitext.widgets.MavenArtifactClasspathSymbolType.Disable";

    private MavenArtifactClasspathExtractor mavenArtifactClasspathExtractor;

    private final Map<String, List<String>> classpathCache = new HashMap<String, List<String>>();

    public MavenArtifactClasspathSymbolType() throws PlexusContainerException {
        super(MavenArtifactClasspathSymbolType.class.getSimpleName());

        if (!Boolean.getBoolean(DISABLE_KEY)) {
            this.mavenArtifactClasspathExtractor = new MavenArtifactClasspathExtractor();
        }

        wikiMatcher(new Matcher().startLineOrCell().string("!mavenArtifact"));

        wikiRule(this);
        htmlTranslation(this);
    }

    @Override
    public String toTarget(Translator translator, Symbol symbol) {
        ParsedSymbol parsedSymbol = getParsedSymbol(translator, symbol);
        StringBuilder classpathForRender = new StringBuilder("<p class='meta'>Maven classpath [mavenArtifact: ")
                .append(parsedSymbol.getSymbol())
                .append(", scope: ")
                .append(parsedSymbol.getScope())
                .append("]:</p>")
                .append("<ul class='meta'>");
        try {
            List<String> classpathElements = getClasspathElements(parsedSymbol);
            for (String element : classpathElements) {
                classpathForRender.append("<li>").append(element).append("</li>");
            }
        } catch (MavenArtifactClasspathExtractionException e) {
            classpathForRender.append("<li class='error'>Unable to parse POM file: ")
                    .append(e.getMessage()).append("</li>");
        }

        classpathForRender.append("</ul>");
        return classpathForRender.toString();

    }

    @SuppressWarnings("unchecked")
    private List<String> getClasspathElements(final ParsedSymbol parsedSymbol) throws MavenArtifactClasspathExtractionException {
        String symbol = parsedSymbol.symbol;
        if (classpathCache.containsKey(symbol)) {
            return classpathCache.get(symbol);
        } else {
            List<String> classpath = Collections.emptyList();
            if (mavenArtifactClasspathExtractor != null) {
                classpath = mavenArtifactClasspathExtractor.extractClasspathEntries(parsedSymbol);
            }
            classpathCache.put(symbol, classpath);
            return classpath;
        }
    }

    private ParsedSymbol getParsedSymbol(Translator translator, Symbol symbol) {
        return new ParsedSymbol(translator.translate(symbol.childAt(0)));
    }

    @Override
    public Maybe<Symbol> parse(Symbol current, Parser parser) {
        if (!parser.isMoveNext(SymbolType.Whitespace)) return Symbol.nothing;

        return new Maybe<Symbol>(current.add(parser.parseToEnds(0, SymbolProvider.pathRuleProvider, new SymbolType[]{SymbolType.Newline})));
    }

    @Override
    public boolean matchesFor(SymbolType symbolType) {
        return symbolType instanceof Path || super.matchesFor(symbolType);
    }

    /**
     * Exposed for testing
     */
    protected void setMavenArtifactClasspathExtractor(MavenArtifactClasspathExtractor mavenArtifactClasspathExtractor) {
        this.mavenArtifactClasspathExtractor = mavenArtifactClasspathExtractor;
    }

    @Override
    public Collection<String> providePaths(Translator translator, Symbol symbol) {
        try {
            return getClasspathElements(getParsedSymbol(translator, symbol));
        } catch (MavenArtifactClasspathExtractionException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Turn the pom+scope key into a comparable object, using the pom's last modified timestamp as
     * cache key.
     */
    static class ParsedSymbol {
        private Pattern artifactCoordinatesPattern = Pattern.compile("([^:]+):([^:]+):([^:]+)(:([^:@]+)(:([^:@]+))?)?(@([^:]+))?");

        private String symbol;

        private String groupId;
        private String artifactId;
        private String version;
        private String packaging;
        private String classifier;

        private String scope;

        public ParsedSymbol(String symbol) {
            super();
            this.symbol = symbol;
            parseSymbol();
        }

        private void parseSymbol() {
            java.util.regex.Matcher matcher = artifactCoordinatesPattern.matcher(symbol);

            if (matcher.matches()) {
                groupId = matcher.group(1);
                artifactId = matcher.group(2);
                version = matcher.group(3);
                packaging = matcher.group(5) == null ? MavenArtifactClasspathExtractor.DEFAULT_PACKAGING : matcher.group(5);
                classifier = matcher.group(7) == null ? "" : matcher.group(7);
                scope = matcher.group(9) == null ? MavenArtifactClasspathExtractor.DEFAULT_SCOPE : matcher.group(9);
            }
        }

        public String getSymbol() {
            return symbol;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getVersion() {
            return version;
        }

        public String getClassifier() {
            return classifier;
        }

        public String getPackaging() {
            return packaging;
        }

        public String getScope() {
            return scope;
        }

		/* hashCode() and equals() are optimized for used in the cache */

        @Override
        public int hashCode() {
            return symbol.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ParsedSymbol) {
                ParsedSymbol ps = (ParsedSymbol) obj;
                return symbol.equals(ps.symbol);
            }
            return false;
        }
    }
}
