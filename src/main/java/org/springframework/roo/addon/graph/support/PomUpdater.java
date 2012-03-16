package org.springframework.roo.addon.graph.support;

import static org.springframework.roo.support.util.XmlUtils.findElements;

import java.util.List;

import org.springframework.roo.addon.graph.support.Converter;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.Repository;
import org.w3c.dom.Element;

public class PomUpdater {

	private final Element configuration;

	public PomUpdater(Element configuration) {
		this.configuration = configuration;
	}

	public void configure(ProjectOperations project, String providerPrefix) {
        // Add the general configuration for graph persistence
        for ( Dependency dependency : dependencies( "/configuration" ) )
        {
            project.addDependency( dependency );
        }
        for ( Property property : properties( "/configuration" ) )
        {
            project.addProperty( property );
        }
        for ( Plugin plugin : plugins( "/configuration" ) )
        {
        	project.removeBuildPlugin(plugin); // remove first, wouldn't be added otherwise
        	project.addBuildPlugin(plugin);
        }

        // Add the specific configuration for the specified graph provider
        for ( Dependency dependency : dependencies(providerPrefix ) )
        {
            project.addDependency( dependency );
        }
        for ( Repository repository : repositories(providerPrefix ) )
        {
            project.removeRepository(repository);
            project.addRepository( repository );
        }
        for ( Property property : properties(providerPrefix ) )
        {
            project.addProperty( property );
        }
	}
	
	
    private Iterable<Dependency> dependencies(String prefixPath )
    {
        return new Converter<Dependency, Element>( elements(prefixPath, "/dependencies/dependency") )
        {
            @Override
            Dependency convert( Element dependency )
            {
                return new Dependency( dependency );
            }
        };
    }

    private Iterable<Plugin> plugins(String prefixPath )
    {
        return new Converter<Plugin, Element>( elements(prefixPath, "/plugins/plugin") )
        {
            @Override
            Plugin convert( Element dependency )
            {
                return new Plugin( dependency );
            }
        };
    }

	private List<Element> elements(String prefixPath, String suffix) {
		return findElements(prefixPath+suffix, configuration );
	}

    private Iterable<Property> properties( String prefixPath )
    {
        return new Converter<Property, Element>( elements(
                prefixPath,"/properties/*")  )
        {
            @Override
            Property convert( Element property )
            {
                return new Property( property );
            }
        };
    }
    
    Iterable<Repository> repositories( String prefixPath )
    {
        return new Converter<Repository, Element>( elements(
                prefixPath,"/repositories/repository" ) )
        {
            @Override
            Repository convert( Element repository )
            {
                return new Repository( repository );
            }
        };
    }

}
