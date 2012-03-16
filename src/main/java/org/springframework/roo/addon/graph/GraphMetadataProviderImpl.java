package org.springframework.roo.addon.graph;

import static java.util.Arrays.asList;
import static org.springframework.roo.addon.graph.support.MetaDataFactory.*;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.*;
import static org.springframework.roo.classpath.details.MemberFindingUtils.getDeclaredTypeAnnotation;
import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JavaType.LONG_PRIMITIVE;
import static org.springframework.roo.model.JavaType.STRING_OBJECT;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.entity.AbstractIdentifierServiceAwareMetadataProvider;
import org.springframework.roo.addon.entity.EntityMetadataProvider;
import org.springframework.roo.addon.graph.support.MatcherCallback;
import org.springframework.roo.addon.graph.support.MetaDataFactory;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.plural.PluralMetadataProvider;
import org.springframework.roo.classpath.*;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.customdata.taggers.*;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component( immediate = true )
@Service
public final class GraphMetadataProviderImpl extends
        AbstractIdentifierServiceAwareMetadataProvider implements GraphMetadataProvider, EntityMetadataProvider
{
    private static final JavaType GRAPH_ENTITY_TYPE = Type.NODE.getGraphAnnotation().getAnnotationType();
    private static final JavaType GRAPH_RELATIONSHIP_TYPE = Type.RELATIONSHIP.getGraphAnnotation().getAnnotationType();
	@Reference
    private ConfigurableMetadataProvider configurableMetadataProvider;
    @Reference
    private PluralMetadataProvider pluralMetadataProvider;
    @Reference
    private TypeLocationService typeLocationService;
    @Reference
    private TypeManagementService typeManagementService;
    @Reference
    private CustomDataKeyDecorator customDataKeyDecorator;

    // FIXME: get the GraphDatabaseProvider from the configuration
    private final GraphDatabaseProvider graphdbProvider = GraphDatabaseProvider.NEO4J;
    private boolean visualize = true;

    protected void activate( final ComponentContext context )
    {
        metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType() );

        registerForTypes(GRAPH_ENTITY_TYPE, GRAPH_RELATIONSHIP_TYPE);
        registerNodeEntityMatchers();
        registerRelationshipEntityMatchers();
    }

    private void registerMatcher(final Matcher<?>... matchers) {
        registerMatcher(getClass().getName(), matchers);
    }

    private void registerMatcher(final String registeringName, final Matcher<?>... matchers) {
        final String className = getClass().getName();
        for (final Matcher<?> matcher : matchers) {
            customDataKeyDecorator.registerMatcher(registeringName,matcher);
        }
    }

    private void registerNodeEntityMatcher(final Matcher<?>...matchers) {
        registerMatcher(getClass().getName()+"Node",matchers);
    }
    private void registerRelationshipEntityMatcher(final Matcher<?>...matchers) {
        registerMatcher(getClass().getName()+"Relationship",matchers);
    }

    private void registerNodeEntityMatchers() {
        registerNodeEntityMatcher(new TypeMatcher(PersistenceCustomDataKeys.PERSISTENT_TYPE, GraphMetadata.class.getName()));
        registerMatcher(new ConstructorMatcher(PersistenceCustomDataKeys.NO_ARG_CONSTRUCTOR, Collections.<JavaType>emptyList()));
        registerMatchers();

        registerNodeEntityMatcher(new MatcherCallback<FieldMetadata>(MANY_TO_MANY_FIELD) {
            @Override
            protected boolean matchesField(final FieldMetadata fieldMetadata) {
                final JavaType type = fieldMetadata.getFieldType();
                if (type.isPrimitive() || hasType(type, STRING_OBJECT)) return false;
                if (hasType(fieldMetadata, type(Set.class), type(Iterable.class))) {
                    final List<JavaType> typeParams = fieldMetadata.getFieldType().getParameters();
                    if (!typeParams.isEmpty()) {
                        final ClassOrInterfaceTypeDetails targetTypeDetails = typeLocationService.getClassOrInterface(typeParams.get(0));
                        if (hasAnnotation(targetTypeDetails, GRAPH_ENTITY_TYPE)) return true;
                    }
                    final AnnotationMetadata annotation = getAnnotation(fieldMetadata, Type.Annotations.RelatedTo, Type.Annotations.RelatedToVia);
                    // todo inner type
                    return (annotation != null);
                    /*
                    final AnnotationAttributeValue<?> direction = annotation.getAttribute(symbol("direction"));
                    if (direction == null || direction.getValue().toString().contains("OUTGOING")) {
                        return true;
                    }
                    */
                }
                return false;
            }
        }.asFieldMatcher());

        registerNodeEntityMatcher(new MatcherCallback<FieldMetadata>(ONE_TO_ONE_FIELD) {
            @Override
            protected boolean matchesField(final FieldMetadata fieldMetadata) {
                final JavaType type = fieldMetadata.getFieldType();
                if (type.isPrimitive() || type.isCommonCollectionType() || hasType(type, STRING_OBJECT, type(Set.class), type(Iterable.class)))
                    return false;
                if (hasAnnotation(fieldMetadata, Type.Annotations.RelatedTo)) return true;
                final ClassOrInterfaceTypeDetails targetTypeDetails = typeLocationService.findClassOrInterface(type);
                if (targetTypeDetails==null) return false;
                if (hasAnnotation(targetTypeDetails, GRAPH_ENTITY_TYPE)) return true;
                return false;
            }
        }.asFieldMatcher());

        registerNodeEntityMatcher(new MatcherCallback<FieldMetadata>(TRANSIENT_FIELD) {
            protected boolean matchesField(final FieldMetadata metadata) {
                return Modifier.isTransient(metadata.getModifier()) || hasAnnotation(metadata, Type.Annotations.Transient);
            }
        }.asFieldMatcher());


        final MatcherCallback<MethodMetadata> persistMatcher = new MatcherCallback<MethodMetadata>(PERSIST_METHOD) {
            protected boolean matchesMethod(final MethodMetadata metadata) {
                return hasName(metadata, "save");
            }
        };
/*
        final MatcherCallback<MethodMetadata> persistMatcher = new MatcherCallback<MethodMetadata>(PERSIST_METHOD) {
            public List<MethodMetadata> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
                if (memberHoldingTypeDetailsList.isEmpty()) return Collections.emptyList();
                final MemberHoldingTypeDetails firstDetail = memberHoldingTypeDetailsList.get(0);
                return asList(method("persist").returns(firstDetail.getName()).toMethod(firstDetail.getDeclaredByMetadataId()));
            }
        };
*/
        registerNodeEntityMatcher(persistMatcher.asMethodMatcher());
        registerNodeEntityMatcher(persistMatcher.forKey(FLUSH_METHOD).asMethodMatcher());
        registerNodeEntityMatcher(persistMatcher.forKey(MERGE_METHOD).asMethodMatcher());
    }
    private void registerRelationshipEntityMatchers() {
        registerRelationshipEntityMatcher(new TypeMatcher(PersistenceCustomDataKeys.PERSISTENT_TYPE, Type.RELATIONSHIP_ENTITY.getFullyQualifiedTypeName()));
        registerMatchers();
        registerRelationshipEntityMatcher(new FieldMatcher(ONE_TO_ONE_FIELD, asList(annotation(Type.Annotations.StartNode), annotation(Type.Annotations.EndNode))));
    }

    private void registerMatchers() {
        //customDataKeyDecorator.registerMatcher(myName, new TypeMatcher(PersistenceCustomDataKeys.IDENTIFIER_TYPE, "org.springframework.roo.addon.entity.IdentifierMetadata"));

        registerMatcher(new MatcherCallback<FieldMetadata>(IDENTIFIER_FIELD) {
            protected boolean matchesField(final FieldMetadata metadata) {
                return hasName(metadata, "id") && hasType(metadata, LONG_OBJECT, LONG_PRIMITIVE) || hasAnnotation(metadata, Type.Annotations.GraphId);
            }
        }.asFieldMatcher());

        registerMatcher(new MatcherCallback<MethodMetadata>(IDENTIFIER_ACCESSOR_METHOD) {
            protected boolean matchesMethod(final MethodMetadata metadata) {
                return hasName(metadata, "getId") && hasType(metadata, LONG_OBJECT, LONG_PRIMITIVE);
            }
        }.asMethodMatcher());


        registerMatcher(new MatcherCallback<MethodMetadata>(REMOVE_METHOD){
            protected boolean matchesMethod(final MethodMetadata metadata) {
                return hasName(metadata, "delete");
            }
        }.asMethodMatcher());

        registerMatcher(new MatcherCallback<MethodMetadata>(COUNT_ALL_METHOD){
            protected boolean matchesMethod(final MethodMetadata metadata) {
                return hasName(metadata, "count");
            }
        }.asMethodMatcher());

        registerMatcher(new MatcherCallback<MethodMetadata>(FIND_METHOD){
            protected boolean matchesMethod(final MethodMetadata metadata) {
                return hasName(metadata, "findById");
            }
        }.asMethodMatcher());

        registerMatcher(new MatcherCallback<MethodMetadata>(FIND_ALL_METHOD){
            protected boolean matchesMethod(final MethodMetadata metadata) {
                return hasName(metadata, "findAll");
            }
        }.asMethodMatcher());

        registerMatcher(new MatcherCallback<MethodMetadata>(FIND_ENTRIES_METHOD){
            protected boolean matchesMethod(final MethodMetadata metadata) {
                return hasName(metadata, "findPaged");
            }
        }.asMethodMatcher());

        //registerMatcher(new MethodMatcher(FIND_ENTRIES_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("findEntriesMethod"), "find", false, true, "Entries"));

        // TODO registerMatcher(new FieldMatcher(PersistenceCustomDataKeys.MANY_TO_MANY_FIELD, getAnnotationMetadataList("javax.persistence.ManyToMany")));
        // TODO registerMatcher(new FieldMatcher(PersistenceCustomDataKeys.MANY_TO_ONE_FIELD, getAnnotationMetadataList("javax.persistence.ManyToOne")));
    }

    private void unregisterMatchers() {
        final String registeringType = getClass().getName();
        customDataKeyDecorator.unregisterMatchers(registeringType);
        customDataKeyDecorator.unregisterMatchers(registeringType+"Node");
        customDataKeyDecorator.unregisterMatchers(registeringType+"Relationship");
    }

    private void registerForTypes(final JavaType...registeredTypes) {
		for (final JavaType registeredType : registeredTypes) {
			configurableMetadataProvider.addMetadataTrigger( registeredType );
	        pluralMetadataProvider.addMetadataTrigger( registeredType );
	        addMetadataTrigger( registeredType );
		}
	}

    protected void deactivate( final ComponentContext context )
    {
        unregisterForTypes(GRAPH_ENTITY_TYPE,GRAPH_RELATIONSHIP_TYPE);

        metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType() );
        unregisterMatchers();
    }

    private void unregisterForTypes(final JavaType...unregisterTypes) {
		for (final JavaType unregisterType : unregisterTypes) {
			configurableMetadataProvider.removeMetadataTrigger( unregisterType );
	        pluralMetadataProvider.removeMetadataTrigger( unregisterType );
	        removeMetadataTrigger(unregisterType);
		}
	}

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString, final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename )
    {
        final PhysicalTypeDetails physicalTypeDetails = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();

        final ClassOrInterfaceTypeDetails governorTypeDetails;
        if ( physicalTypeDetails instanceof ClassOrInterfaceTypeDetails )
        {
            governorTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeDetails;
        }
        else
        {
            return null;
        }



        final String pluralIdentifier = PluralMetadata.createIdentifier(GraphMetadata.getJavaType(metadataIdentificationString), GraphMetadata.getPath(metadataIdentificationString));
        final PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralIdentifier);
        if (pluralMetadata==null) {
            throw new IllegalStateException("Can access plural metadata");
        }
        metadataDependencyRegistry.registerDependency(pluralIdentifier, metadataIdentificationString);
        metadataDependencyRegistry.registerDependency(ProjectMetadata.getProjectIdentifier(), metadataIdentificationString);

        final AnnotationMetadata rooGraphEntity = getDeclaredTypeAnnotation(governorTypeDetails, GRAPH_ENTITY_TYPE);
        final AnnotationMetadata rooGraphRelationship = getDeclaredTypeAnnotation(governorTypeDetails, GRAPH_RELATIONSHIP_TYPE);
        final GraphMetadata metadata;

        if ( rooGraphEntity != null )
        {
            metadata = GraphMetadata.node( graphdbProvider,
                    metadataIdentificationString, aspectName,
                    governorPhysicalTypeMetadata, governorTypeDetails, typeManagementService, typeLocationService);
        } else
        if ( rooGraphRelationship != null)
        {
            metadata = GraphMetadata.relationship( graphdbProvider, 
                    metadataIdentificationString, aspectName,
                    governorPhysicalTypeMetadata, governorTypeDetails,typeLocationService);
        } else {
        	throw new IllegalStateException("not a graph entity or relationship: "+governorPhysicalTypeMetadata.getPhysicalLocationCanonicalPath());
        }
        return metadata;
    }

    @Override
    protected String createLocalIdentifier( final JavaType javaType, final Path path )
    {
        return GraphMetadata.createIdentifier( javaType, path );
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString )
    {
        final JavaType javaType = GraphMetadata.getJavaType( metadataIdentificationString );
        final Path path = GraphMetadata.getPath( metadataIdentificationString );
        return PhysicalTypeIdentifier.createIdentifier( javaType, path );
    }

    public String getItdUniquenessFilenameSuffix()
    {
        return "GraphEntity";
    }

    public String getProvidesType()
    {
        return GraphMetadata.PROVIDES_TYPE;
    }
}
