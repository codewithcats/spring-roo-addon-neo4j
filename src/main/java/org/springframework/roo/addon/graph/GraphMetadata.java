package org.springframework.roo.addon.graph;

import org.springframework.roo.addon.graph.support.BuildingItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.*;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

import javax.annotation.Resource;
import java.io.File;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.util.Arrays.asList;
import static org.springframework.roo.addon.graph.support.MetaDataFactory.*;
import static org.springframework.roo.addon.graph.support.MethodBuilder.method;
import static org.springframework.roo.addon.graph.support.Tuple2._;
import static org.springframework.roo.model.JavaType.*;

public abstract class GraphMetadata extends
        BuildingItdTypeDetailsProvidingMetadataItem
{
    private static final Logger LOG = Logger.getLogger(GraphMetadata.class.getName());
    private static final String PROVIDES_TYPE_STRING = GraphMetadata.class.getName();
    static final String PROVIDES_TYPE = MetadataIdentificationUtils.create( PROVIDES_TYPE_STRING );
    private final Type type;

    private GraphMetadata(final String identifier, final JavaType aspectName,
                          final PhysicalTypeMetadata governorPhysicalTypeMetadata,
                          final ClassOrInterfaceTypeDetails governorTypeDetails,
                          final Type type)
    {
        super( identifier, aspectName, governorTypeDetails, governorPhysicalTypeMetadata );
        this.type = type;
        buildDetails();
        this.itdTypeDetails=builder.build();
    }

    public JavaType getJavaType()
    {
        return governorTypeDetails.getName();
    }

    public File getSourceFile()
    {
        return new File(
                governorPhysicalTypeMetadata.getPhysicalLocationCanonicalPath() );
    }

    public boolean isRelationship()
    {
        return type==Type.RELATIONSHIP;
    }

    static GraphMetadata node(final GraphDatabaseProvider graphdbProvider,
                              final String identifier, final JavaType aspectName,
                              final PhysicalTypeMetadata governorPhysicalTypeMetadata,
                              final ClassOrInterfaceTypeDetails governorTypeDetails, 
                              final TypeManagementService typeManagementService, 
                              final TypeLocationService typeLocationService)
    {
        return new GraphMetadata(identifier, aspectName,
                governorPhysicalTypeMetadata, governorTypeDetails,
                Type.NODE) {
        	@Override
            public void buildDetails() {
        		final JavaType target = getJavaType();
                final String targetClass = target.getSimpleTypeName();
        		final ImportRegistrationResolver importResolver = builder.getImportRegistrationResolver();
				importResolver.addImport(Type.Types.GraphRepository);
				importResolver.addImport(Type.Annotations.GraphId);
        		importResolver.addImport(Type.Types.DirectGraphRepositoryFactory);
        		importResolver.addImport(target);
        		importResolver.addImport(Type.Types.PageRequest);
        		importResolver.addImport(Type.Types.IteratorUtil);

                // todo indexed fields
                for (final FieldMetadata fieldMetadata : governorTypeDetails.getDeclaredFields()) {
                    final List<AnnotationMetadata> annotations = fieldMetadata.getAnnotations();
                    for (final AnnotationMetadata annotation : annotations) {
                        if (annotation.getAnnotationType().equals(Type.Annotations.RelatedToVia)) {
                            addRelationshipMethods(builder, fieldMetadata,annotation,typeLocationService);
                        }
                    }
                }
        		// todo add finder as field, initialized by factory
                builder.addField(new FieldMetadataBuilder(getId(), Modifier.PRIVATE|Modifier.FINAL, asList(annotationBuilder(Type.Annotations.GraphId)),symbol("id"), LONG_OBJECT));
                final ConstructorMetadataBuilder constructor = new ConstructorMetadataBuilder(getId());
                constructor.setModifier(Modifier.PUBLIC);
                constructor.setParameterTypes(AnnotatedJavaType.convertFromJavaTypes(asList(Type.Types.Node)));
                constructor.setParameterNames(asList(symbol("n")));
                constructor.getBodyBuilder().appendFormalLine("setPersistentState(n);");
                builder.addConstructor(constructor);

                final String simpleTypeName = getJavaType().getSimpleTypeName();
                final JavaType repositoryHolderType = new JavaType(simpleTypeName + "RepositoryHolder");
                builder.addInnerType(createRepositoryType(repositoryHolderType));
                final String repositoryHolder = repositoryHolderType.getSimpleTypeName();

                // TODO REMOVE, we have to cope with the inability of roo to integrate externally introduced (AJ) methods
                // so I have to redeclare our methods here :(
                method("getId").returns(JavaType.LONG_OBJECT).line(
                    "return getNodeId();").add(getId(), builder);
                method("save").returns(getJavaType()).line(
                    "return (%s)persist();",targetClass).add(getId(), builder);
                method("delete").line(
                        "remove();", targetClass).add(getId(), builder);
                method("findPaged",_(INT_PRIMITIVE,symbol("start")),_(INT_PRIMITIVE,symbol("pageSize"))).isStatic().returns(collectionType(Collection.class, target)).line(
                    "return "+asCollection("%s.repository().findAll(new PageRequest(start/pageSize,pageSize))")+";", repositoryHolder
                ).add(getId(), builder);

                method("find"+ simpleTypeName, _(LONG_OBJECT,symbol("id"))).returns(getJavaType()).isStatic().line(
                        "return %s.repository().findOne(:param1);", repositoryHolder
                ).add(getId(), builder);
				method("count").returns(LONG_PRIMITIVE).isStatic().line(
                    "return %s.repository().count();", repositoryHolder
                ).add(getId(), builder);
                method("findAll").isStatic().returns(collectionType(Collection.class, target)).line(
                        "return " + asCollection("%s.repository().findAll()") + ";", repositoryHolder
                ).add(getId(), builder);
            }

            private String asCollection(final String inner) { return "IteratorUtil.asCollection("+inner+")"; }

            private ClassOrInterfaceTypeDetails createRepositoryType(final JavaType repositoryHolderType) {
                final String repositoryHolderTypeName = repositoryHolderType.getSimpleTypeName();
                if (MemberFindingUtils.getDeclaredInnerType(governorTypeDetails, repositoryHolderType) != null) {
                    return null;
                }

                final JavaType repositoryType = Type.graphRepository(getJavaType());

                final ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(getId(), Modifier.STATIC, repositoryHolderType, PhysicalTypeCategory.CLASS);
                classBuilder.addAnnotation(annotation(type("org.springframework.beans.factory.annotation.Configurable")));
                classBuilder.addField(new FieldMetadataBuilder(getId(), Modifier.PRIVATE, asList(annotationBuilder(Resource.class)), symbol("repositoryFactory"), Type.Types.DirectGraphRepositoryFactory));
                classBuilder.addField(new FieldMetadataBuilder(getId(), Modifier.PRIVATE|Modifier.STATIC, symbol("repository"), repositoryType,null));

                method("setRepositoryFactory", _(Type.Types.DirectGraphRepositoryFactory,symbol("repositoryFactory")))
                        .annotation(annotation(Resource.class))
                        .line(" this.repositoryFactory = repositoryFactory; ")
                        .add(getId(), classBuilder);

                method("repository")
                        .isStatic()
                        .returns(repositoryType)
                        .line("if (%1$s.repository==null) { %1$s.repository=new %1$s().repositoryFactory.createGraphRepository(%2$s.class);}", repositoryHolderTypeName, getJavaType().getSimpleTypeName())
                        .line("return repository;")
                        .line("}} ")
                        .line("declare @type : %s.%s : @org.springframework.beans.factory.annotation.Configurable;", getJavaType().getSimpleTypeName(),repositoryHolderTypeName)
                        .line("{{")
                        .add(getId(), classBuilder);

                return classBuilder.build();
            }

            private void addRelationshipMethods(final ItdTypeDetailsBuilder builder, final FieldMetadata fieldMetadata, final AnnotationMetadata annotation, final TypeLocationService typeLocationService) {
                final JavaType relationshipEntity = getRelationshipEntity(fieldMetadata, annotation, typeLocationService);
                final ClassOrInterfaceTypeDetails relationshipEntityDetails = typeLocationService.getClassOrInterface(relationshipEntity);
                final JavaType targetNode=getOtherNodeType(relationshipEntityDetails);
                final JavaSymbolName targetName = symbol(targetNode);
                final String relationshipTypeName = (String) annotation.getAttribute(symbol("type")).getValue();
                final String relationshipEntityName = relationshipEntity.getSimpleTypeName();

                method("get" + relationshipEntityName,_(targetNode, targetName)).returns(relationshipEntity).line(
                   "return (%1$s)getRelationshipTo(%2$s,%1$s.class,\"%3$s\");", relationshipEntityName, targetName, relationshipTypeName
                ).add(getId(),builder);
                method("relateTo" + targetNode.getSimpleTypeName(),_(targetNode, targetName)).returns(relationshipEntity).line(
                   "return (:return)relateTo(:param1,:return.class,\"%s\");",relationshipTypeName
                ).add(getId(), builder);
            }
            private JavaType getOtherNodeType(final ClassOrInterfaceTypeDetails relationshipType) {
                JavaType startNode = null;
                JavaType endNode = null;
                for (final FieldMetadata fieldMetadata : relationshipType.getDeclaredFields()) {
                    for (final AnnotationMetadata annotationMetadata : fieldMetadata.getAnnotations()) {
                        if (annotationMetadata.getAnnotationType().equals(Type.Annotations.StartNode) || annotationMetadata.getAnnotationType().equals(Type.Annotations.EndNode)) {
                            JavaType fieldType = fieldMetadata.getFieldType();
                            if (!fieldType.equals(getJavaType())) {
                                return fieldType;
                            } else if (annotationMetadata.getAnnotationType().equals(Type.Annotations.StartNode)) {
                                if (startNode != null) {
                                    throw new IllegalStateException("Relationship " + relationshipType.getName().getFullyQualifiedTypeName() + " shouldn't contain two fields annotated with @StartNode");
                                }
                                startNode = fieldType;
                            } else if (annotationMetadata.getAnnotationType().equals(Type.Annotations.EndNode)) {
                                if (endNode != null) {
                                    throw new IllegalStateException("Relationship " + relationshipType.getName().getFullyQualifiedTypeName() + " shouldn't contain two fields annotated with @EndNode");
                                }
                                endNode = fieldType;
                            }
                        }
                    }
                }
                if(startNode != null && endNode != null && startNode.equals(endNode)) {
                    LOG.warning("StartNode and EndNode types are same in Relationship "+relationshipType.getName().getFullyQualifiedTypeName());
                        return startNode;
                    }
                throw new IllegalStateException("Other NodeEntity to "+getJavaType()+" not found in relationship "+relationshipType);
            }
        };
    }

    private static JavaType getRelationshipEntity(final FieldMetadata fieldMetadata, AnnotationMetadata annotation, final TypeLocationService typeLocationService) {
        final AnnotationAttributeValue<?> attributeValue = annotation.getAttribute(symbol("elementClass"));
        if (attributeValue!=null) {
            final JavaType relationshipEntity = (JavaType) attributeValue.getValue();
            if (!hasType(relationshipEntity,Type.RELATIONSHIP_BACKED)) return relationshipEntity;
        }
        final JavaType fieldType = fieldMetadata.getFieldType();
        if (hasType(fieldType, type(Set.class), type(Iterable.class))) {
            final List<JavaType> typeParams = fieldType.getParameters();
            if (!typeParams.isEmpty()) {
                final JavaType relationshipEntityType = typeParams.get(0);
                final ClassOrInterfaceTypeDetails targetTypeDetails = typeLocationService.getClassOrInterface(relationshipEntityType);
                if (hasAnnotation(targetTypeDetails, Type.RELATIONSHIP_ENTITY)) return relationshipEntityType;
            }
        } else {
            final ClassOrInterfaceTypeDetails targetTypeDetails = typeLocationService.getClassOrInterface(fieldType);
            if (targetTypeDetails!=null && hasAnnotation(targetTypeDetails, Type.RELATIONSHIP_ENTITY)) return fieldType;
        }
        return null;
    }

    static GraphMetadata relationship(final GraphDatabaseProvider graphdbProvider,
                                      final String identifier, final JavaType aspectName,
                                      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
                                      final ClassOrInterfaceTypeDetails governorTypeDetails,
                                      final TypeLocationService typeLocationService)
    {
        return new GraphMetadata(identifier, aspectName,
                governorPhysicalTypeMetadata, governorTypeDetails,
                Type.RELATIONSHIP) {
        	@Override
            public void buildDetails() {
                // already done in constructor builder.addAnnotation(Type.RELATIONSHIP.getGraphAnnotation());
                final ConstructorMetadataBuilder constructor = new ConstructorMetadataBuilder(getId());
                constructor.setModifier(PUBLIC);
                constructor.addParameterType(new AnnotatedJavaType(type("org.neo4j.graphdb.Relationship"),null));
                constructor.addParameterName(symbol("r"));
                constructor.setBodyBuilder(new InvocableMemberBodyBuilder().appendFormalLine("setPersistentState(r);"));
                builder.addConstructor(constructor.build());
        	}
        };
    }


    static String createIdentifier( final JavaType javaType, final Path path )
    {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path );
    }
    
    static JavaType getJavaType( final String metadataIdentificationString )
    {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString );
    }

    static final Path getPath( final String metadataIdentificationString )
    {
        return PhysicalTypeIdentifierNamingUtils.getPath( PROVIDES_TYPE_STRING,
                metadataIdentificationString );
    }
}
