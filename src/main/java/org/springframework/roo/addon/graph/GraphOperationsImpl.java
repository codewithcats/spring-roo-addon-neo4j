package org.springframework.roo.addon.graph;

import static java.lang.reflect.Modifier.ABSTRACT;
import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.util.Arrays.asList;
import static org.springframework.roo.addon.graph.support.MetaDataFactory.annotationBuilder;
import static org.springframework.roo.addon.graph.support.MetaDataFactory.collectionType;
import static org.springframework.roo.addon.graph.support.MetaDataFactory.identifier;
import static org.springframework.roo.addon.graph.support.MetaDataFactory.symbol;
import static org.springframework.roo.classpath.PhysicalTypeCategory.CLASS;

import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.graph.support.AppContextUpdater;
import org.springframework.roo.addon.graph.support.PomUpdater;
import org.springframework.roo.addon.graph.support.PropertiesUpdater;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;

/**
 * Provides Graph configuration operations.
 *
 * @author Thomas Risberg
 * @author Tobias Ivarsson
 */
@Component
@Service
public class GraphOperationsImpl implements GraphOperations {
    @Reference private ProjectOperations project;
    @Reference private TypeManagementService typeManagementService;
    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;
    @Reference private PropFileOperations propFileOperations;

    public boolean isGraphInstallationPossible()
    {
        return true;
    }

    public boolean isGraphInstalled()
    {
        return true;
    }

    public void configureGraph(final GraphProvider provider, final String dataStoreLocation)
    {
        Assert.notNull( provider, "Graph provider required" );
        Assert.notNull( dataStoreLocation, "datastore location required" );

    	final PomUpdater pomUpdater = new PomUpdater(XmlUtils.getConfiguration(getClass()));
		pomUpdater.configure(project, provider.getConfigPrefix());

        final AppContextUpdater appContextUpdater=new AppContextUpdater(pathResolver,fileManager);
        appContextUpdater.updateFor(provider);

        final PropertiesUpdater propertiesUpdater=new PropertiesUpdater(pathResolver,fileManager,propFileOperations,provider.getPropertyFileName());
        propertiesUpdater.updateProperties(provider, dataStoreLocation);
	}

    public void addRelationshipVia(final JavaType from, final JavaType target, final JavaType via, final String fieldName, final String relationshipName, final Cardinality cardinality, final Direction direction) {
    	final JavaSymbolName fieldSymbol = fieldName!=null ? symbol(fieldName) : symbol(target);
        //final JavaType fieldType = isSingle(cardinality) ? via : collectionType(Iterable.class, via); // no semantics for updating single relationship entities
        final JavaType fieldType = collectionType(Iterable.class, via);

        final JavaType elementClass = via != null ? via : Type.RELATIONSHIP_BACKED; // todo optional null
        final FieldMetadataBuilder relationshipField = new FieldMetadataBuilder(identifier(from), PRIVATE|FINAL,
                asList(Type.Annotations.EntityRelationshipEntityAnnotation(relationshipName, direction, elementClass)), fieldSymbol, fieldType);
        relationshipField.setFieldInitializer("null");
        typeManagementService.addField(relationshipField.build());
    }

    public void addRelationship(final JavaType from, final JavaType target, final String fieldName, final String relationshipName, final Cardinality cardinality, final Direction direction) {
        final JavaSymbolName fieldSymbol = fieldName!=null ? symbol(fieldName) : symbol(target);

    	final JavaType fieldType = Type.isSingle(cardinality) ?  target : collectionType(Set.class, target);

        final JavaType elementClass = null; // was target
        final FieldMetadataBuilder relationshipField = new FieldMetadataBuilder(identifier(from), PRIVATE,
                asList(Type.Annotations.EntityRelationshipAnnotation(relationshipName,direction, elementClass)),
                fieldSymbol, fieldType);
        typeManagementService.addField(relationshipField.build());
    }

    public ClassOrInterfaceTypeDetailsBuilder createClassDetails(final JavaType type, final JavaType superclass, final boolean createAbstract, final AnnotationMetadataBuilder entityType, final List<FieldMetadataBuilder> fields) {
        int modifier = PUBLIC;
        if (createAbstract) {
            modifier |= ABSTRACT;
        }

        final ClassOrInterfaceTypeDetailsBuilder cid = new ClassOrInterfaceTypeDetailsBuilder(identifier(type), modifier, type, CLASS);
        if (fields != null) {
            for (final FieldMetadataBuilder field : fields) {
                cid.addField(field);
            }
        }
        if (superclass!=null) cid.setExtendsTypes(asList(superclass));
        cid.setAnnotations(asList(entityType,annotationBuilder(RooToString.class), annotationBuilder(RooJavaBean.class)));
        return cid;
    }

    public FieldMetadataBuilder newField(final JavaType target, final JavaType fieldType, final boolean isCollection) {
        final JavaType finalFieldType = isCollection ? collectionType(Set.class,fieldType) : fieldType;
        return new FieldMetadataBuilder(identifier(target), PRIVATE, fieldType.getArgName(), finalFieldType, "null");
    }

    public FieldMetadataBuilder newRelationshipField(final JavaType target, final JavaSymbolName fieldName, final JavaType fieldType, final AnnotationMetadataBuilder fieldAnnotation) {
        final FieldMetadataBuilder field = new FieldMetadataBuilder(identifier(target), PRIVATE | FINAL, asList(fieldAnnotation), fieldName, fieldType);
        field.setFieldInitializer("null");
        return field;
    }

    public ConstructorMetadataBuilder addConstructor(final JavaType target, final boolean empty, final boolean allFields) {
		//final DefaultConstructorMetadata constructorMetadata = new DefaultConstructorMetadata(declaredByMetadataId, PUBLIC, parameters, parameterNames, annotations, body);
		// classpathOperations.addConstructor(constructorMetadata);
		//return constructorMetadata;
		return null;
	}
}