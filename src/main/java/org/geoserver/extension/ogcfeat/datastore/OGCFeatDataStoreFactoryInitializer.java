package org.geoserver.extension.ogcfeat.datastore;

import org.geoserver.data.DataStoreFactoryInitializer;
import org.geoserver.platform.GeoServerResourceLoader;

public class OGCFeatDataStoreFactoryInitializer extends DataStoreFactoryInitializer<OGCFeatDataStoreFactory> {

	GeoServerResourceLoader resourceLoader;

	public OGCFeatDataStoreFactoryInitializer() {
		super(OGCFeatDataStoreFactory.class);
	}

	public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void initialize(OGCFeatDataStoreFactory factory) {
		factory.setBaseDirectory(resourceLoader.getBaseDirectory());
	}
}