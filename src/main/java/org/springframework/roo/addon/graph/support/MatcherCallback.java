package org.springframework.roo.addon.graph.support;

import org.springframework.roo.classpath.customdata.taggers.FieldMatcher;
import org.springframework.roo.classpath.customdata.taggers.Matcher;
import org.springframework.roo.classpath.customdata.taggers.MethodMatcher;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomDataAccessor;
import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
* @author mh
* @since 06.07.11
*/
public class MatcherCallback<T extends CustomDataAccessor> implements Matcher<T> {

    public MethodMatcher asMethodMatcher() {
        return new MethodMatcher(Collections.<FieldMatcher>emptyList(), (CustomDataKey<MethodMetadata>) key, false) {
            public List<MethodMetadata> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
                return (List<MethodMetadata>) MatcherCallback.this.matches(memberHoldingTypeDetailsList);
            }

            @Override
            public List<MethodMetadata> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList, HashMap<String, String> pluralMap) {
                return matches(memberHoldingTypeDetailsList);
            }
        };
    }
    public FieldMatcher asFieldMatcher() {
        return new FieldMatcher((CustomDataKey<FieldMetadata>) key, Collections.<AnnotationMetadata>emptyList()) {
            public List<FieldMetadata> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
                return (List<FieldMetadata>) MatcherCallback.this.matches(memberHoldingTypeDetailsList);
            }
        };
    }
    private final CustomDataKey<T> key;

    public MatcherCallback(CustomDataKey<T> key) {
        this.key = key;
    }

    @SuppressWarnings({"unchecked"})
    public List<T> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
        List result=new ArrayList();
        for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
            for (ConstructorMetadata constructorMetadata : memberHoldingTypeDetails.getDeclaredConstructors()) {
                if (matchesConstructor(constructorMetadata)) result.add(constructorMetadata);
            }
            for (MethodMetadata methodMetadata : memberHoldingTypeDetails.getDeclaredMethods()) {
                if (matchesMethod(methodMetadata)) result.add(methodMetadata);
            }
            for (FieldMetadata fieldMetadata : memberHoldingTypeDetails.getDeclaredFields()) {
                if (matchesField(fieldMetadata)) result.add(fieldMetadata);
            }
        }
        return result;
    }

    protected boolean matchesField(FieldMetadata fieldMetadata) {
        return false;
    }

    protected boolean matchesMethod(MethodMetadata methodMetadata) {
        return false;
    }

    protected boolean matchesConstructor(ConstructorMetadata constructorMetadata) {
        return false;
    }

    public CustomDataKey<T> getCustomDataKey() {
        return key;
    }

    public MatcherCallback<T> forKey(final CustomDataKey<T> newKey) {
        return new MatcherCallback<T>(newKey) {
            public List<T> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
                return MatcherCallback.this.matches(memberHoldingTypeDetailsList);
            }
        };
    }
}
