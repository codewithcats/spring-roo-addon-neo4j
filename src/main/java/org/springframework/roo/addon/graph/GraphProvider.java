package org.springframework.roo.addon.graph;

import org.springframework.roo.support.style.ToStringCreator;

/**
 * Graph DB providers known to the Graph add-on.
 *
 */
public enum GraphProvider {
    NEO4J;


	@Override
    public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("provider", name());
		return tsc.toString();
	}

    public String getConfigPrefix( )
    {
        return "/configuration/graphstores/graphstore[@id='" + name() + "']";
    }

    public String getLocationProperty() {
        return name().toLowerCase()+".location";
    }

    public String getPropertyFileName() {
        return "neo4j.properties";
    }

    public String getPersistenceProviderClass() {
        return "org.springframework.data.graph.neo4j.jpa.Neo4jPersistenceProvider";
    }
}