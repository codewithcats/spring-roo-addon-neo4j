package org.springframework.roo.addon.graph.support;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultItdTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.model.JavaType;

/**
 * creates class definition for graph entities not based on itd type details
 */
public abstract class BuildingItdTypeDetailsProvidingMetadataItem extends
        AbstractItdTypeDetailsProvidingMetadataItem
{
    protected BuildingItdTypeDetailsProvidingMetadataItem(String identifier,
                                                          JavaType aspectName,
                                                          ClassOrInterfaceTypeDetails governorTypeDetails,
                                                          PhysicalTypeMetadata governorPhysicalTypeMetadata)
    {
        super( identifier,aspectName, governorPhysicalTypeMetadata);
    }

    public abstract void buildDetails();
}
