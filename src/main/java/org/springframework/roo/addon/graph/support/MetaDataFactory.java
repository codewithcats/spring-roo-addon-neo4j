package org.springframework.roo.addon.graph.support;

import static java.util.Arrays.asList;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.IdentifiableAnnotatedJavaStructure;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.*;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.StringUtils;

public abstract class MetaDataFactory {
    public static JavaType type(String type) {
    	return new JavaType(type);
    }
    public static JavaType type(Class type) {
    	return new JavaType(type.getName());
    }

    public static JavaType collectionType(Class<? extends Iterable> collectionType,JavaType...params) {
    	return new JavaType(collectionType.getName(), 0, DataType.TYPE,null, asList(params));
    }
    
    public static JavaSymbolName symbol(String name) {
    	return new JavaSymbolName(name);
    }
    public static JavaSymbolName symbol(JavaType type) {
    	return type.getArgName()!=null ? type.getArgName() : propertySymbol(type.getSimpleTypeName());
    }

    public static JavaSymbolName propertySymbol(String name) {
    	return new JavaSymbolName(StringUtils.uncapitalize(name));
    }
    
    public static JavaSymbolName symbol(Enum anEnum) {
    	return new JavaSymbolName(anEnum.name());
    }
	public static String identifier(JavaType target) {
		return PhysicalTypeIdentifier.createIdentifier(target, Path.SRC_MAIN_JAVA);
	}
	public static AnnotationMetadata annotation(Class<? extends Annotation> annotationType, Tuple2<String,?>...params) {
        return annotation(type(annotationType.getName()), params);
	}
	public static AnnotationMetadataBuilder annotationBuilder(AnnotationMetadata annotationMetadata) {
        return new AnnotationMetadataBuilder(annotationMetadata);
    }
	public static AnnotationMetadataBuilder annotationBuilder(Class<? extends Annotation> annotationType, Tuple2<String,?>...params) {
        return annotationBuilder(type(annotationType.getName()),asList(params));
	}
	public static AnnotationMetadataBuilder annotationBuilder(Class<? extends Annotation> annotationType, Iterable<Tuple2<String,?>> params) {
        return annotationBuilder(type(annotationType.getName()), params);
	}
	public static AnnotationMetadata annotation(String annotationType, Tuple2<String,?>...params) {
        return annotation(type(annotationType), asList(params));
	}
	public static AnnotationMetadataBuilder annotationBuilder(String annotationType, Tuple2<String,?>...params) {
        return annotationBuilder(type(annotationType), asList(params));
	}

    public static AnnotationMetadata annotation(JavaType type, Tuple2<String, ?>... params) {
        return annotation(type, asList(params));
    }
    public static AnnotationMetadata annotation(JavaType type, Iterable<Tuple2<String, ?>> params) {
        return annotationBuilder(type, params).build();
    }
    public static AnnotationMetadataBuilder annotationBuilder(JavaType type, Tuple2<String, ?>... params) {
        return annotationBuilder(type, asList(params));
    }

    public static AnnotationMetadataBuilder annotationBuilder(JavaType type, Iterable<Tuple2<String, ?>> params) {
        return new AnnotationMetadataBuilder(type, annotationAttributes(params));
    }

    public static EnumDetails enumDetails(JavaType type, JavaSymbolName symbol) {
        return new EnumDetails(type, symbol);
    }
    public static List<AnnotationAttributeValue<?>> annotationAttributes(
			Iterable<Tuple2<String, ?>> params) {
		List<AnnotationAttributeValue<?>> attributes=new ArrayList<AnnotationAttributeValue<?>>();
		for (Tuple2<String, ?> entry : params) {
			final JavaSymbolName symbol = symbol(entry._1);
			final Object value = entry._2;
            if (value instanceof AnnotationAttributeValue) {
                attributes.add((AnnotationAttributeValue<?>) value);
            }
			if (value instanceof String) {
				attributes.add(new StringAttributeValue(symbol, (String)value));
			}
            if (value instanceof EnumDetails) {
                attributes.add(new EnumAttributeValue(symbol, (EnumDetails) value));
            }
			if (value instanceof Enum) {
				attributes.add(new EnumAttributeValue(symbol, enumDetails(type(value.getClass()), symbol((Enum)value))));
			}
			if (value instanceof JavaType) {
				attributes.add(new ClassAttributeValue(symbol, (JavaType)value));
			}
		}
		return attributes;
	}
	public static JavaSymbolName createGetterName(FieldMetadata field) {
        final JavaType fieldType = field.getFieldType();
        if (fieldType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
            return symbol("is" + StringUtils.capitalize(field.getFieldName().getSymbolName()));
        } else {
            return symbol("get" + StringUtils.capitalize(field.getFieldName().getSymbolName()));
        }
	}
	public static JavaSymbolName createSetterName(FieldMetadata field) {
		return symbol("set" + StringUtils.capitalize(field.getFieldName().getSymbolName()));
	}

    public static boolean hasAnnotation(final IdentifiableAnnotatedJavaStructure metadata, final JavaType...annotationTypes) {
        return getAnnotation(metadata, annotationTypes)!=null;
    }

    public static AnnotationMetadata getAnnotation(final IdentifiableAnnotatedJavaStructure metadata, final JavaType... annotationTypes) {
        for (final AnnotationMetadata annotationMetadata : metadata.getAnnotations()) {
            for (final JavaType annotationType : annotationTypes) {
                if (annotationMetadata.getAnnotationType().equals(annotationType)) return annotationMetadata;
            }
        }
        return null;
    }

    public static boolean hasName(final MethodMetadata metadata, final String...names) {
      return hasName(metadata.getMethodName(), names);
    }

    public static boolean hasName(final FieldMetadata metadata, final String...names) {
      return hasName(metadata.getFieldName(), names);
    }

    public static boolean hasType(final FieldMetadata metadata, final JavaType...types) {
      return hasType(metadata.getFieldType(), types);
    }

    public static boolean hasType(final MethodMetadata metadata, final JavaType...types) {
      return hasType(metadata.getReturnType(), types);
    }

    public static boolean hasName(final JavaSymbolName metaDataName, final String[] names) {
        for (final String name : names) {
            if (metaDataName.getSymbolName().equals(name)) return true;
        }
        return false;
    }

    public static boolean hasType(final JavaType metaDataType, final JavaType... types) {
        for (final JavaType type : types) {
            if (metaDataType.getFullyQualifiedTypeName().equals(type.getFullyQualifiedTypeName())) return true;
        }
        return false;
    }
}
