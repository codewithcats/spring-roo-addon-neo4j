package org.springframework.roo.addon.graph;

import java.util.List;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Interface to commands available in {@link GraphOperationsImpl}.
 *
 * @author Thomas Risberg
 */
public interface GraphOperations {

	boolean isGraphInstallationPossible();

	boolean isGraphInstalled();

	/**
	 * This method is responsible for managing all Graph related artifacts and the project pom.xml
	 *
	 * @param graphProvider the Graph provider selected (Neo4J)
     * @param dataStoreLocation
	 */
    void configureGraph(GraphProvider graphProvider, final String dataStoreLocation);

    void addRelationshipVia(JavaType from, JavaType to, JavaType via, String fieldName, String relationshipName, Cardinality cardinality, Direction direction);
    void addRelationship(JavaType from, JavaType to, String fieldName, String relationshipName, Cardinality cardinality, Direction direction);

    ClassOrInterfaceTypeDetailsBuilder createClassDetails(JavaType name, JavaType superclass, boolean createAbstract, AnnotationMetadataBuilder annotationMetadata, final List<FieldMetadataBuilder> fields);

    FieldMetadataBuilder newField(JavaType target, JavaType fieldType, boolean isCollection);
    FieldMetadataBuilder newRelationshipField(JavaType target, JavaSymbolName fieldName,JavaType fieldType, AnnotationMetadataBuilder fieldAnnotation);

	ConstructorMetadataBuilder addConstructor(JavaType target, boolean empty,boolean allFields);
}