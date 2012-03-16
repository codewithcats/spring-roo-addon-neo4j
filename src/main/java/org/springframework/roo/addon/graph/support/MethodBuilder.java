package org.springframework.roo.addon.graph.support;

import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.springframework.roo.addon.graph.support.Tuple2._;

/**
 * @author Michael Hunger
 * @since 10.09.2010
 */
public class MethodBuilder {

    private final JavaSymbolName name;
    private JavaType returnType = JavaType.VOID_PRIMITIVE;
    private int modifiers=Modifier.PUBLIC;
    private final List<Tuple2<AnnotatedJavaType,JavaSymbolName>> params=new ArrayList<Tuple2<AnnotatedJavaType, JavaSymbolName>>();
    private final List<AnnotationMetadata> annotations=new ArrayList<AnnotationMetadata>();
    private final StringBuilder body=new StringBuilder();
    private final List<JavaType> throwTypes=new ArrayList<JavaType>();

    public MethodBuilder(final String name, final Tuple2<JavaType,JavaSymbolName>...params) {
        Assert.notNull(name);
        this.name = MetaDataFactory.symbol(name);
        params(params);
    }

    public MethodBuilder annotatedParams(final Tuple2<AnnotatedJavaType,JavaSymbolName>...params) {
        Assert.notNull(params);
        this.params.addAll(asList(params));
        return this;
    }

    public MethodBuilder isPublic() {
        this.modifiers |= Modifier.PUBLIC;
        return this;
    }
    public MethodBuilder isPrivate() {
        this.modifiers |= Modifier.PRIVATE;
        return this;
    }
    public MethodBuilder isProtected() {
        this.modifiers |= Modifier.PROTECTED;
        return this;
    }
    public MethodBuilder isStatic() {
        this.modifiers |= Modifier.STATIC;
        return this;
    }
    public MethodBuilder isFinal() {
        this.modifiers |= Modifier.FINAL;
        return this;
    }

    public MethodBuilder params(final Tuple2<JavaType,JavaSymbolName>...params) {
        for (final Tuple2<JavaType, JavaSymbolName> param : params) {
            Assert.notNull(param._1);
            Assert.notNull(param._2);
            this.params.add(_(new AnnotatedJavaType(param._1,null),param._2));
        }
        return this;
    }
    public static MethodBuilder method(final String name, final Tuple2<JavaType,JavaSymbolName>...params) {
        return new MethodBuilder(name,params);
    }

    public void add(final String id, final ItdTypeDetailsBuilder builder) {
        Assert.notNull(builder);
        builder.addMethod(toMethod(id));
    }

    public void add(String id, AbstractMemberHoldingTypeDetailsBuilder<ClassOrInterfaceTypeDetails> builder) {
        Assert.notNull(builder);
        builder.addMethod(toMethod(id));
    }

    public void add(final String id, final MutableClassOrInterfaceTypeDetails cid) {
        Assert.notNull(cid);
        cid.addMethod(toMethod(id));
    }

    public MethodBuilder annotation(final AnnotationMetadata...annotations) {
        Assert.notNull(annotations);
        this.annotations.addAll(asList(annotations));
        return this;
    }

    public MethodMetadata toMethod(final String id) {
        Assert.notNull(id);
        final MethodMetadataBuilder method = new MethodMetadataBuilder(id, modifiers, name, returnType, paramTypes(), paramNames(), new InvocableMemberBodyBuilder());
        for (final AnnotationMetadata annotation : annotations) {
            method.addAnnotation(annotation);
        }
        for (final JavaType throwType : throwTypes) {
            method.addThrowsType(throwType);
        }
        method.getBodyBuilder().appendFormalLine(this.body.toString());
        return method.build();
    }

    private List<JavaSymbolName> paramNames() {
        final List<JavaSymbolName> result=new ArrayList<JavaSymbolName>();
        for (final Tuple2<AnnotatedJavaType, JavaSymbolName> param : params) {
            result.add(param._2);
        }
        return result;
    }

    private List<AnnotatedJavaType> paramTypes() {
        final List<AnnotatedJavaType> result=new ArrayList<AnnotatedJavaType>();
        for (final Tuple2<AnnotatedJavaType, JavaSymbolName> param : params) {
            result.add(param._1);
        }
        return result;
    }

    public MethodBuilder returns(final JavaType returnType) {
        Assert.notNull(returnType);
        this.returnType = returnType;
        return this;
    }

    public MethodBuilder body(final String...lines) {
        Assert.notNull(lines);
        for (final String line : lines) {
            body.append(line).append("\n");
        }
        return this;
    }
    public MethodBuilder exceptions(final JavaType...exceptions) {
        Assert.notNull(exceptions);
        throwTypes.addAll(asList(exceptions));
        return this;
    }

    public MethodBuilder line(final String line, final Object...args) {
        Assert.notNull(line);
        return body(String.format(replaceSymbols(line),args));
    }

    private String replaceSymbols(String line) {
        int pos=1;
        for (final Tuple2<AnnotatedJavaType, JavaSymbolName> param : params) {
            line=line.replace(":param"+pos,param._2.getSymbolName())
                     .replace(":type"+pos,param._1.getJavaType().getSimpleTypeName());
            pos++;
        }
        return line.replace(":return",returnType.getSimpleTypeName());
    }
}
