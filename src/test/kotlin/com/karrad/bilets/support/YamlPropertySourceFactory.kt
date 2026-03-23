package com.karrad.bilets.support

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PropertySourceFactory

class YamlPropertySourceFactory : PropertySourceFactory {
    override fun createPropertySource(name: String?, resource: EncodedResource): PropertySource<*> {
        val factory = YamlPropertiesFactoryBean()
        factory.setResources(resource.resource)
        val properties = requireNotNull(factory.`object`) {
            "Unable to load yaml property source: ${resource.resource.filename}"
        }
        val sourceName = name ?: resource.resource.filename ?: "yamlPropertySource"
        return PropertiesPropertySource(sourceName, properties)
    }
}
