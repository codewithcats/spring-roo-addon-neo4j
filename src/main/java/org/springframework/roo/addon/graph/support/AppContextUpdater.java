package org.springframework.roo.addon.graph.support;

import org.springframework.roo.addon.graph.GraphProvider;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;

import java.io.IOException;

import static org.springframework.roo.addon.graph.support.Tuple2._;


/**
 * @author Michael Hunger
 * @since 28.08.2010
 */
public class AppContextUpdater {
    public static final Tuple2<String,String> ASPECT = _("factory-method", "aspectOf");
    protected static final Tuple2<String,String> SINGLETON = _("scope","singleton");

    private final GraphAppContextConfig appContextConfig;

    public AppContextUpdater(final PathResolver pathResolver, final FileManager fileManager) {
        final String contextPath = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-graph.xml");
        addInitialGraphContext(fileManager, contextPath);
        // final String contextPath = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
        appContextConfig = new GraphAppContextConfig(fileManager, contextPath);
    }

    private void addInitialGraphContext(FileManager fileManager, final String contextPath) {
        if (!fileManager.exists(contextPath)) {
            try {
                FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "applicationContext-graph-template.xml"), fileManager.createFile(contextPath).getOutputStream());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public void updateFor(final GraphProvider provider) {
        appContextConfig.updateApplicationContext(provider);
    }


}
