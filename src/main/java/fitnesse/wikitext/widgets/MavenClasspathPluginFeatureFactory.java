package fitnesse.wikitext.widgets;

import fitnesse.plugins.PluginException;
import fitnesse.plugins.PluginFeatureFactoryBase;
import fitnesse.wikitext.parser.SymbolProvider;
import fitnesse.wikitext.parser.SymbolType;
import org.codehaus.plexus.PlexusContainerException;

public class MavenClasspathPluginFeatureFactory extends PluginFeatureFactoryBase {
    @Override
    public void registerSymbolTypes(SymbolProvider symbolProvider) throws PluginException {
        super.registerSymbolTypes(symbolProvider);
        try {
            add(symbolProvider, new MavenClasspathSymbolType());
        } catch (PlexusContainerException e) {
            throw new PluginException("Unable to create symbol", e);
        }
    }

    private void add(SymbolProvider provider, SymbolType symbolType) {
        provider.add(symbolType);
        LOG.info("Added symbol " + symbolType.getClass());
    }
}
