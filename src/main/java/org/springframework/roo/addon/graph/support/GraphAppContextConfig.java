package org.springframework.roo.addon.graph.support;

import org.springframework.roo.addon.graph.GraphProvider;
import org.springframework.roo.process.manager.FileManager;
import org.w3c.dom.Element;

import static org.springframework.roo.addon.graph.support.Tuple2._;

/**
 * @author Michael Hunger
 * @since 02.09.2010
 */
public class GraphAppContextConfig extends AppContextConfig {
    public GraphAppContextConfig(final FileManager fileManager, final String contextPath) {
        super(fileManager, contextPath);
    }

    public void updateApplicationContext(final GraphProvider graphProvider) {
        removeBean(beans, "/beans/bean[@id = 'dataSource']");
        removeBean(beans, "/beans/jndi-lookup[@id = 'dataSource']");

        if (beanMissing("/beans/annotation-driven")) {
            beans.appendChild(createDirective("tx", "annotation-driven", _("mode", "aspectj"), _("transaction-manager", "transactionManager")));
        }
        if (beanMissing("/beans/config")) {
            beans.appendChild(createDirective("graph", "config", _("storeDirectory", "${"+graphProvider.getLocationProperty()+"}")));
        }
        /*if (beanMissing("/beans/repositories")) {
            beans.appendChild(createDirective("graph", "repositories", _("base-package", "")));
        }
        */
        /*

        removeBean(beans, "/beans/bean[@id = 'dataSource']");
        removeBean(beans, "/beans/jndi-lookup[@id = 'dataSource']");

        beans.setAttributeNS("xmlns","datagraph","http://www.springframework.org/schema/data/graph");
        beans.setAttributeNS("xsi","schemaLocation","");

        // addIfMissing(bean("transactionManager", "org.springframework.orm.jpa.JpaTransactionManager"));

        if (beanMissing("/beans/annotation-driven")) {
            beans.appendChild(createDirective("tx", "annotation-driven", _("mode", "aspectj"), _("transaction-manager", "transactionManager")));
        }
        if (beanMissing("/beans/config")) {
            beans.appendChild(createDirective("graph", "config", _("storeDirectory", "${"+graphProvider.getLocationProperty()+"}")));
        }
        if (beanMissing("/beans/repositories")) {
            beans.appendChild(createDirective("graph", "repositories", _("base-package", "")));
        }
        // neo4j config
        final Element graphDbService = addIfMissing(bean("graphDbService", "org.neo4j.kernel.EmbeddedGraphDatabase", attributes(AppContextUpdater.SINGLETON, destroy("shutdown")),
                constructorArg(0, "${" + graphProvider.getLocationProperty() + "}")));

        addIfMissing(bean("indexService", "org.neo4j.index.lucene.LuceneIndexService", attributes(destroy("shutdown")),
                constructorRef(0, graphDbService)));

        addIfMissing(bean("transactionManager", "org.springframework.transaction.jta.JtaTransactionManager",
                propertyBean("transactionManager",
                        bean("neo4jTransactionManagerService", "org.neo4j.kernel.impl.transaction.SpringTransactionManager", constructorRef(0, graphDbService))
                ),
                propertyBean("userTransaction",
                        bean("neo4jUserTransactionService", "org.neo4j.kernel.impl.transaction.UserTransactionImpl", constructorRef(0, graphDbService))
                )));


        final Element graphDatabaseContext = addIfMissing(bean("graphDatabaseContext", "org.springframework.data.graph.neo4j.support.GraphDatabaseContext",
                propertyRef("graphDatabaseService", graphDbService),
                propertyBean("relationshipEntityInstantiator",bean("relationshipEntityInstantiator","org.springframework.data.graph.neo4j.support.relationship.ConstructorBypassingGraphRelationshipInstantiator")),
                propertyBean("graphEntityInstantiator",bean("graphEntityInstantiator","org.springframework.data.graph.neo4j.support.node.Neo4jConstructorGraphEntityInstantiator")),
                propertyBean("conversionService",bean("conversionService","org.springframework.data.graph.neo4j.fieldaccess.Neo4jConversionServiceFactoryBean")),
                propertyBean("nodeTypeStrategy",bean("nodeTypeStrategy","org.springframework.data.graph.neo4j.support.SubReferenceNodeTypeStrategy",constructorRef(0,"graphDatabaseContext")))
                ));


        Element finderFactory = addIfMissing(bean("finderFactory", "org.springframework.data.graph.neo4j.finder.FinderFactory",
                constructorRef(0, graphDatabaseContext)));

        Element nodeEntityStateAccessorsFactory = addIfMissing(bean("nodeEntityStateAccessorsFactory", "org.springframework.data.graph.neo4j.fieldaccess.NodeEntityStateAccessorsFactory",
                propertyBean("nodeDelegatingFieldAccessorFactory",
                        bean("nodeDelegatingFieldAccessorFactory", "org.springframework.data.graph.neo4j.fieldaccess.NodeDelegatingFieldAccessorFactory",
                                constructorRef(0, graphDatabaseContext), constructorRef(1, finderFactory))),
                propertyRef("finderFactory",finderFactory),
                propertyRef("graphDatabaseContext",graphDatabaseContext)));

        Element relationshipEntityStateAccessorsFactory = addIfMissing(bean("relationshipEntityStateAccessorsFactory", "org.springframework.data.graph.neo4j.fieldaccess.RelationshipEntityStateAccessorsFactory",
                propertyRef("graphDatabaseContext", graphDatabaseContext), propertyRef("finderFactory", finderFactory)));


        addIfMissing(bean("neo4jNodeBacking", "org.springframework.data.graph.neo4j.support.node.Neo4jNodeBacking", attributes(AppContextUpdater.ASPECT),
                propertyRef("graphDatabaseContext",graphDatabaseContext),propertyRef("nodeEntityStateAccessorsFactory",nodeEntityStateAccessorsFactory)));

        addIfMissing(bean("neo4jRelationshipBacking", "org.springframework.data.graph.neo4j.support.relationship.Neo4jRelationshipBacking", attributes(AppContextUpdater.ASPECT),
                propertyRef("graphDatabaseContext",graphDatabaseContext),propertyRef("relationshipEntityStateAccessorsFactory",relationshipEntityStateAccessorsFactory)));

        replaceExisting(bean("entityManagerFactory",
                "org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean",
                property("persistenceUnitName", graphProvider.name()),
                property("persistenceProviderClass", graphProvider.getPersistenceProviderClass()),
                propertyBean("jpaDialect", bean(null, "org.springframework.data.graph.neo4j.jpa.Neo4jJpaDialect"))));
        */
        cleanUpAndWriteContext();
    }
}
