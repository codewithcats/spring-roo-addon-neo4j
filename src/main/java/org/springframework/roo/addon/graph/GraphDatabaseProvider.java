package org.springframework.roo.addon.graph;

import org.springframework.roo.model.JavaType;

public enum GraphDatabaseProvider
{
    NEO4J
    {
        @Override
        JavaType nodeType()
        {
            return new JavaType( "org.neo4j.graphdb.Node" );
        }

        @Override
        JavaType relationshipType()
        {
            return new JavaType( "org.neo4j.graphdb.Relationship" );
        }
    };

    abstract JavaType nodeType();

    abstract JavaType relationshipType();
}
