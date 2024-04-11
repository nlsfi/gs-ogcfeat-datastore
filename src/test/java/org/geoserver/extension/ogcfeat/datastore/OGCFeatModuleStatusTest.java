package org.geoserver.extension.ogcfeat.datastore;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.geoserver.data.DataStoreFactoryInitializer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.api.data.DataAccessFinder;
import org.junit.Test;
import org.springframework.web.context.WebApplicationContext;
import org.vfny.geoserver.util.DataStoreUtils;

public class OGCFeatModuleStatusTest {
    @Test
    public void testInitializer() {
        GeoServerResourceLoader resourceLoader = createMock(GeoServerResourceLoader.class);
        expect(resourceLoader.getBaseDirectory()).andReturn(new File("target")).once();
        replay(resourceLoader);

        DataAccessFinder.getAvailableDataStores();

        OGCFeatDataStoreFactoryInitializer initializer = new OGCFeatDataStoreFactoryInitializer();
        initializer.setResourceLoader(resourceLoader);

        WebApplicationContext appContext = createNiceMock(WebApplicationContext.class);
        expect(appContext.getBeanNamesForType(DataStoreFactoryInitializer.class))
                .andReturn(new String[] { "geopkgDataStoreFactoryInitializer" }).anyTimes();
        expect(appContext.getBean("geopkgDataStoreFactoryInitializer")).andReturn(initializer).anyTimes();
        replay(appContext);

        new GeoServerExtensions().setApplicationContext(appContext);

        DataAccessFactory fac = DataStoreUtils.aquireFactory(new OGCFeatDataStoreFactory().getDisplayName());

        assertNotNull(fac);

        verify(resourceLoader);
    }
}