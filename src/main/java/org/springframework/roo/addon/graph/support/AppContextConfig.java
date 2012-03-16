package org.springframework.roo.addon.graph.support;

import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.roo.addon.graph.support.Tuple2._;

/**
 * @author Michael Hunger
 * @since 02.09.2010
 */
public class AppContextConfig {
    protected String contextPath;
    protected FileManager fileManager;
    protected Document appContext;
    protected Element beans;
    protected MutableFile contextMutableFile;

    public AppContextConfig(final FileManager fileManager, final String contextPath) {
        this.contextPath = contextPath;
        this.fileManager = fileManager;
        contextMutableFile = loadContextFile();
        appContext = parseXmlConfig(contextMutableFile);
        beans = appContext.getDocumentElement();
    }

    protected void cleanUpAndWriteContext() {
        cleanUpAndWriteContext(contextMutableFile, beans);
    }

    protected Tuple2<String, String> destroy(final String destroyMethodName) {
        return _("destroy-method", destroyMethodName);
    }

    protected Element addIfMissing(final Element bean) {
        if (beanMissing("/beans/bean[@id = '" + bean.getAttribute("id") + "']")) {
            beans.appendChild(bean);
        }
        return bean;
    }

    protected Element replaceExisting(final Element bean) {
        removeBean(beans, "/beans/bean[@id = '" + bean.getAttribute("id") + "']");
        beans.appendChild(bean);
        return bean;
    }

    protected boolean beanMissing(final String xPathExpression) {
        return findBean(beans, xPathExpression) == null;
    }

    protected Element constructorRef(final int idx, final String value) {
        final Element property = appContext.createElement("constructor-arg");
        property.setAttribute("index", "" + idx);
        property.setAttribute("ref", value);
        return property;
    }

    protected Element constructorArg(final int idx, final String value) {
        final Element property = appContext.createElement("constructor-arg");
        property.setAttribute("index", "" + idx);
        property.setAttribute("value", value);
        return property;
    }

    protected Element constructorBean(final int idx, final Element bean) {
        final Element property = appContext.createElement("constructor-arg");
        property.setAttribute("index", "" + idx);
        property.appendChild(bean);
        return property;
    }

    protected Element constructorRef(final int idx, final Element bean) {
        return constructorRef(idx, bean.getAttribute("id"));
    }

    protected Element propertyRef(final String name, final Element bean) {
        return propertyRef(name, bean.getAttribute("id"));
    }

    protected Element propertyRef(final String name, final String value) {
        final Element property = appContext.createElement("property");
        property.setAttribute("name", name);
        property.setAttribute("ref", value);
        return property;
    }

    protected Element property(final String name, final String value) {
        final Element property = appContext.createElement("property");
        property.setAttribute("name", name);
        property.setAttribute("value", value);
        return property;
    }

    protected Element propertyBean(final String name, final Element bean) {
        final Element property = appContext.createElement("property");
        property.setAttribute("name", name);
        property.appendChild(bean);
        return property;
    }

    protected Element createDirective(final String namespace, final String name, final Tuple2<String, String>... attributes) {
        final Element directive = appContext.createElement(namespace + ":" + name);
        for (final Tuple2<String, String> attribute : attributes) {
            directive.setAttribute(attribute._1, attribute._2);
        }
        return directive;
    }

    protected void cleanUpAndWriteContext(final MutableFile contextMutableFile, final Element parent) {
        XmlUtils.removeTextNodes(parent);

        XmlUtils.writeXml(contextMutableFile.getOutputStream(), appContext);
    }

    protected Element findBean(final Element parent, final String xPathExpression) {
        return XmlUtils.findFirstElement(xPathExpression, parent);
    }

    protected Element bean(final String id, final String beanClass, final Element... properties) {
        return bean(id, beanClass, attributes(), properties);
    }

    protected Map<String, String> attributes(final Tuple2<String, String>... attributes) {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        for (final Tuple2<String, String> attribute : attributes) {
            map.put(attribute._1, attribute._2);
        }
        return map;
    }

    protected Element bean(final String id, final String beanClass, final Map<String, String> attributes, final Element... properties) {
        final Element bean = appContext.createElement("bean");
        if (id != null) bean.setAttribute("id", id);
        bean.setAttribute("class", beanClass);
        for (final Map.Entry<String, String> entry : attributes.entrySet()) {
            bean.setAttribute(entry.getKey(), entry.getValue());
        }

        for (final Element property : properties) {
            bean.appendChild(property);
        }
        return bean;
    }


    protected Element removeBean(final Element root, final String xPathExpression) {
        final Element bean = findBean(root, xPathExpression);

        if (bean != null) {
            root.removeChild(bean);
        }
        return bean;
    }

    protected Document parseXmlConfig(final MutableFile contextMutableFile) {
        try {
            return XmlUtils.getDocumentBuilder().parse(contextMutableFile.getInputStream());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected MutableFile loadContextFile() {
        if (!fileManager.exists(contextPath))
            throw new IllegalStateException("Could not acquire applicationContext.xml in " + contextPath);

        try {
            return fileManager.updateFile(contextPath);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
