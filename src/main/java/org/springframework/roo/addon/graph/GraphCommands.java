package org.springframework.roo.addon.graph;

import static org.springframework.roo.addon.graph.support.MetaDataFactory.symbol;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.test.IntegrationTestOperations;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the 'graph' add-on to be used by the ROO shell.
 *
 * @author Thomas Risberg
 * @author Tobias Ivarsson
 */
@Component
@Service
public class GraphCommands implements CommandMarker {

	@Reference private GraphOperations graphOperations;
    @Reference private TypeManagementService typeManagementService;
    @Reference private IntegrationTestOperations integrationTestOperations;

	@CliAvailabilityIndicator("graph setup")
	public boolean isInstallGraphAvailable() {
		return graphOperations.isGraphInstallationPossible(); // && !graphOperations.isGraphInstalled();
	}

	@CliCommand(value = "graph setup", help = "Install or updates a Graph database provider in your project")
    public void installGraphPersistence(
			@CliOption(key = { "provider" }, mandatory = true, help = "The graph provider to support") final GraphProvider graphProvider,
			@CliOption(key = { "databaseLocation" }, mandatory = true, help = "The database location to use") final String databaseLocation) {

        graphOperations.configureGraph( graphProvider, databaseLocation);

	}

    @CliAvailabilityIndicator({"graph entity","graph relationship"})
    public boolean isGraphEntityAvailable() {
      return graphOperations.isGraphInstalled();
    }

	@CliCommand(value = "graph entity", help = "Creates a new graph entity in SRC_MAIN_JAVA")
	public void newGraphEntity(
			@CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "Name of the entity to create") final JavaType name,
			@CliOption(key = "extends", mandatory = false, unspecifiedDefaultValue = "java.lang.Object", help = "The superclass (defaults to java.lang.Object)") final JavaType superclass,
			@CliOption(key = "abstract", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Whether the generated class should be marked as abstract") final boolean createAbstract,
			@CliOption(key = "testAutomatically", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Create automatic integration tests for this entity") final boolean testAutomatically) {

		if (testAutomatically && createAbstract) {
			// We can't test an abstract class
			throw new IllegalArgumentException("Automatic tests cannot be created for an abstract entity; remove the --testAutomatically or --abstract option");
		}
        final ClassOrInterfaceTypeDetailsBuilder details = graphOperations.createClassDetails(name, superclass, createAbstract, Type.NODE.getGraphAnnotation(), null);
        typeManagementService.generateClassFile(details.build());

		if (testAutomatically) {
			integrationTestOperations.newIntegrationTest(name);
		}
	}

    @CliCommand(value = "constructor", help = "Creates an empty constructor in the class")
	public void emptyConstructor(@CliOption(key = "class", optionContext = "update,project", mandatory = false, unspecifiedDefaultValue="*", help = "The name of the class to receive this constructor") final JavaType target,
			@CliOption(key = "empty", mandatory = false, specifiedDefaultValue = "false", unspecifiedDefaultValue = "false", help = "Whether the generated constructor should be empty") final boolean empty,
			@CliOption(key = "allFields", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Whether the generated constructor should set all possible fields.") final boolean allFields) {
    	if (empty && allFields) 
    		throw new IllegalArgumentException("Constructor can either be empty or use all fields.");

    	graphOperations.addConstructor(target,empty,allFields);
    }

    // todo add the field on the other side too (optionally)
    @CliCommand(value = "graph relationship", help = "Creates a new relationship between two graph entities")
	public void newGraphRelationship(
            @CliOption(key = "via", mandatory = false, help = "Name of explicit relationship class") final JavaType via, // todo correct relationship-type handling
			@CliOption(key = "fieldName", mandatory = true, help = "Name of the field name") final String fieldName,
			@CliOption(key = "type", mandatory = false, help = "Name of relationship") final String relationshipName,
			@CliOption(key = "from", optionContext = "update,project", mandatory = true, help = "Name of the start graph entity") final JavaType from,
			@CliOption(key = "to", optionContext = "update,project", mandatory = true, help = "Name of the end graph entity") final JavaType to,
			@CliOption(key = "direction", mandatory = false, unspecifiedDefaultValue = "OUTGOING", specifiedDefaultValue="OUTGOIONG", help = "INCOMING or OUTGOING") final Direction direction,
            @CliOption(key = "cardinality", mandatory = false, unspecifiedDefaultValue = "ONE_TO_ONE", specifiedDefaultValue = "ONE_TO_MANY", help = "The relationship cardinarily") final Cardinality cardinality) {

        if (via==null) {
            graphOperations.addRelationship(from,to,fieldName, relationshipName,cardinality,direction);
        } else {
            if (Type.isSingle(cardinality)) throw new IllegalArgumentException("No single cardinality for relationship fields of relationship entities supported.");
        	final JavaSymbolName symbolTo = symbol(to).equals(symbol(from)) ? symbol(symbol(to).getSymbolName()+"2") : symbol(to);
        
            final List<FieldMetadataBuilder> fields = Arrays.<FieldMetadataBuilder>asList(
                    graphOperations.newRelationshipField(via, symbol(from), from, Type.Annotations.StartNodeAnnotation()),
                    graphOperations.newRelationshipField(via, symbolTo, to, Type.Annotations.EndNodeAnnotation()));
            
            final ClassOrInterfaceTypeDetailsBuilder details = graphOperations.createClassDetails(via, null, false, Type.RELATIONSHIP.getGraphAnnotation(), fields);
            typeManagementService.generateClassFile(details.build());

            graphOperations.addRelationshipVia(from, to, via, fieldName, relationshipName, cardinality, direction);
        }
	}
}