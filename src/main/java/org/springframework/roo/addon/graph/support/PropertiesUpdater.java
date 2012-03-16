package org.springframework.roo.addon.graph.support;

import org.springframework.roo.addon.graph.GraphProvider;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.TemplateUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Michael Hunger
 * @since 28.08.2010
 */
public class PropertiesUpdater {
    private final PathResolver pathResolver;
    private final FileManager fileManager;
    private final PropFileOperations propFileOperations;
    private final String propertyFileName;
    private static final Logger log = Logger.getLogger(PropertiesUpdater.class.getName());

    public PropertiesUpdater(final PathResolver pathResolver, final FileManager fileManager, final PropFileOperations propFileOperations, final String propertyFileName) {
        this.pathResolver = pathResolver;
        this.fileManager = fileManager;
        this.propFileOperations = propFileOperations;
        this.propertyFileName = propertyFileName;
    }

    public boolean hasProperties() {
        return fileManager.exists(getPropertiesPath());
    }

    public SortedSet<String> getPropertyKeys() {
        if (fileManager.exists(getPropertiesPath())) {
            return propFileOperations.getPropertyKeys(Path.SPRING_CONFIG_ROOT, propertyFileName, true);
        }
        return new TreeSet<String>();
    }

    private String getPropertiesPath() {
        return pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, propertyFileName);
    }


    public void updateProperties(final GraphProvider graphProvider, final String dataStoreLocation) {
        final String databasePath = getPropertiesPath();
        final boolean propertiesExist = fileManager.exists(databasePath);

        final MutableFile databaseMutableFile = obtainPropertiesFile(databasePath, propertiesExist);

        final Properties props = obtainProperties(databaseMutableFile, propertiesExist);

        props.put(graphProvider.getLocationProperty(), dataStoreLocation);


        storeProperties(databaseMutableFile, props);
    }

    private void storeProperties(final MutableFile databaseMutableFile, final Properties props) {
        OutputStream os = databaseMutableFile.getOutputStream();
        try {
            props.store(os, null);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        } finally {
            if (os!=null) {
                try {
                    os.close();
                } catch (IOException e) {
                    log.warning("Error closing file: "+databaseMutableFile.getCanonicalPath());
                }
            }
        }
    }

    private Properties obtainProperties(final MutableFile databaseMutableFile, final boolean propertiesExist) {
        try {
            final Properties props = new Properties();
            if (propertiesExist) {
                props.load(databaseMutableFile.getInputStream());
            } else {
                final InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "graph-template.properties");
                Assert.notNull(templateInputStream, "Could not acquire database properties template");
                props.load(templateInputStream);
            }
            return props;
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    private MutableFile obtainPropertiesFile(final String databasePath, final boolean propertiesExist) {
        if (!propertiesExist) {
            return fileManager.createFile(databasePath);
        } else {
            return fileManager.updateFile(databasePath);
        }

    }


}
