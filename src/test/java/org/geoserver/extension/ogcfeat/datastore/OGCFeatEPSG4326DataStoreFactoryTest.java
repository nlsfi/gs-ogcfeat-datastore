package org.geoserver.extension.ogcfeat.datastore;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

public class OGCFeatEPSG4326DataStoreFactoryTest {

	public static void main(String[] args)
			throws MalformedURLException, IOException, NoSuchAuthorityCodeException, FactoryException {

		String requestCRS = "http://www.opengis.net/def/crs/EPSG/0/4326";
		String responseCRS = "http://www.opengis.net/def/crs/EPSG/0/4326";

		String url = "https://avoin-paikkatieto.maanmittauslaitos.fi/maastotiedot/features/v1";
		String user = "7cd2ddae-9f2e-481c-99d0-404e7bc7a0b2";

		OGCFeatDataStoreFactory factory = new OGCFeatDataStoreFactory();

		Map<String, Object> params = new HashMap<>();
		params.put(OGCFeatDataStoreFactory.URL_PARAM.key, url);
		params.put(OGCFeatDataStoreFactory.USER_PARAM.key, user);
		params.put(OGCFeatDataStoreFactory.PASSWORD_PARAM.key, "");
		params.put(OGCFeatDataStoreFactory.TIMEOUT_PARAM.key, "5000");
		params.put(OGCFeatDataStoreFactory.NS_PARAM.key, "https://xml.nls.fi/example");

		OGCFeatDataStore datastore = factory.createDataStore(params);
		try {
			/*
			 * for (String s : datastore.getTypeNames()) { System.out.println(s); }
			 */

			double[] bbox = new double[] { 24.46526138302816, 60.2596073383277, 24.521212182443456, 60.27770378913576 };

			FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
			BBOX filter = ff.bbox("geometry", bbox[0], bbox[1], bbox[2], bbox[3], requestCRS);
			Query query = new Query();

			query.setCoordinateSystem(CRS.decode(responseCRS));
			query.setFilter(filter);

			ContentFeatureCollection features = datastore.getFeatureSource("soistuma").getFeatures(query);
			try (SimpleFeatureIterator iter = features.features()) {

				while (iter.hasNext()) {
					SimpleFeature f = iter.next();
					System.out.println(f);

				}

			}
		} finally {
			datastore.dispose();
		}

	}
}