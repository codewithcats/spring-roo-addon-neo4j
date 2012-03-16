package org.springframework.roo.addon.graph;

import org.springframework.roo.addon.graph.support.Tuple2;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaType;

import java.util.ArrayList;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.springframework.roo.addon.graph.support.MetaDataFactory.*;
import static org.springframework.roo.addon.graph.support.Tuple2._;


/**
 * @author Michael Hunger
 * @since 27.08.2010
 */
public enum Type {
    NODE() {
        @Override
        public AnnotationMetadataBuilder getGraphAnnotation() {
            //return annotationBuilder(type(ANNOTATION_API + "NodeEntity"),_("fullIndex",index));
            return annotationBuilder(NODE_ENTITY);
        }
    }, RELATIONSHIP() {
        @Override
        public AnnotationMetadataBuilder getGraphAnnotation() {
            return annotationBuilder(RELATIONSHIP_ENTITY);
        }

    };
    private static final String ANNOTATION_API = "org.springframework.data.neo4j.annotation.";
    private static final String CORE_API = "org.springframework.data.neo4j.core.";
    static final JavaType NODE_ENTITY = type(ANNOTATION_API + "NodeEntity");
    static final JavaType NODE_BACKED = type(CORE_API + "NodeBacked");
    static final JavaType RELATIONSHIP_ENTITY = type(ANNOTATION_API + "RelationshipEntity");
    static final JavaType RELATIONSHIP_BACKED = type(CORE_API + "RelationshipBacked");

    static JavaType graphRepository(JavaType entityType) {
        return new JavaType(Types.GraphRepository.getFullyQualifiedTypeName(),0, DataType.TYPE,null,asList(entityType));
    }

    public static boolean isSingle(final Cardinality cardinality) {
        return cardinality == Cardinality.ONE_TO_ONE || cardinality == Cardinality.MANY_TO_ONE;
    }

    public abstract AnnotationMetadataBuilder getGraphAnnotation();

    public interface Types {
        String PACKAGE_FINDER = "org.springframework.data.neo4j.repository.";
        JavaType GraphRepository = type(PACKAGE_FINDER + "GraphRepository");
        JavaType DirectGraphRepositoryFactory = type(PACKAGE_FINDER + "DirectGraphRepositoryFactory");
        JavaType Node = type("org.neo4j.graphdb.Node");
        JavaType PageRequest = type("org.springframework.data.domain.PageRequest");
        JavaType IteratorUtil = type("org.neo4j.helpers.collection.IteratorUtil");
    }

    public static class Annotations {
        public static final JavaType GraphId = type(ANNOTATION_API + "GraphId");
        public static final JavaType RelatedToVia = type(ANNOTATION_API + "RelatedToVia");
        public static final JavaType RelatedTo = type(ANNOTATION_API + "RelatedTo");
        public static final JavaType GraphTraversal = type(ANNOTATION_API + "GraphTraversal");
        public static final JavaType GraphQuery = type(ANNOTATION_API + "GraphQuery");
        public static final JavaType EndNode = type(ANNOTATION_API + "EndNode");
        public static final JavaType StartNode = type(ANNOTATION_API + "StartNode");
        public static final JavaType Transient = type("javax.persistence.Transient");

        private Annotations() {
        }

        public static AnnotationMetadataBuilder StartNodeAnnotation() {
            return annotationBuilder(StartNode);
        }

        public static AnnotationMetadataBuilder EndNodeAnnotation() {
            return annotationBuilder(EndNode);
        }

        public static AnnotationMetadataBuilder EntityRelationshipAnnotation(final String relationshipName, final Direction direction, final JavaType elementClass) {
            return createRelationshipAnnotation(RelatedTo, relationshipName, direction, elementClass);
        }

        private static AnnotationMetadataBuilder createRelationshipAnnotation(JavaType annotationType, String relationshipName, Direction direction, JavaType elementClass) {
            Collection<Tuple2<String,?>> attributes=new ArrayList<Tuple2<String,?>>();
            if (relationshipName!=null) attributes.add(_("type", relationshipName));
            if (direction!=null && direction != Direction.OUTGOING) attributes.add(_("direction",  Enums.direction(direction)));
            if (elementClass!=null) attributes.add(_("elementClass", elementClass));
            return annotationBuilder(annotationType, attributes);
        }

        public static AnnotationMetadataBuilder EntityRelationshipEntityAnnotation(final String relationshipName, final Direction direction, final JavaType elementClass) {
            return createRelationshipAnnotation(RelatedToVia, relationshipName, direction, elementClass);
        }

    }
    public static class Enums {
        public static final JavaType DIRECTION = type("org.springframework.data.neo4j.core.Direction");

        public static EnumDetails direction(Direction direction) {
            return enumDetails(DIRECTION, symbol(direction));
        }
    }
}
