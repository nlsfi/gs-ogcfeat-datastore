package org.geoserver.extension.ogcfeat.datastore;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.geoserver.extension.ogcfeat.datastore.model.Collection;
import org.geotools.api.data.Query;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

public class OGCFeatExampleApp {

    public static void main(String[] args)
            throws MalformedURLException, IOException, NoSuchAuthorityCodeException, FactoryException {

        String requestCRS = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
        String responseCRS = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";

        String url = "<URL>";
        String user = "<USER>";
        String pass = "";
        double[] bbox = new double[] { 24.46526138302816, 60.2596073383277, 24.521212182443456, 60.27770378913576 };

        OGCFeatDataStoreFactory factory = new OGCFeatDataStoreFactory();

        Map<String, Object> params = new HashMap<>();
        params.put(OGCFeatDataStoreFactory.URL_PARAM.key, url);
        params.put(OGCFeatDataStoreFactory.USER_PARAM.key, user);
        params.put(OGCFeatDataStoreFactory.PASSWORD_PARAM.key, pass);
        params.put(OGCFeatDataStoreFactory.TIMEOUT_PARAM.key, "5000");
        params.put(OGCFeatDataStoreFactory.FEAT_LIMIT_PARAM.key, 10);
        params.put(OGCFeatDataStoreFactory.FEAT_PAGING_MAX_PARAM.key, 5);
        params.put(OGCFeatDataStoreFactory.NS_PARAM.key, "http://example.org/example");

        OGCFeatDataStore datastore = factory.createDataStore(params);
        try {
            for (String s : datastore.getTypeNames()) {
                System.out.println(s);
                System.out.println(datastore.getSchema(s));
            }

            FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
            BBOX filter = ff.bbox("geometry", bbox[0], bbox[1], bbox[2], bbox[3], requestCRS);

            for (Entry<String, Collection> kv : datastore.getCatalogue().collectionMap.entrySet()) {
                System.out.println(kv.getKey() + " -> " + kv.getValue());
                Query query = new Query();
                query.setMaxFeatures(5);

                query.setCoordinateSystem(CRS.decode(responseCRS));
                query.setFilter(filter);

                CoordinateReferenceSystem crs = CRS.decode(responseCRS);

                OGCFeatFeatureSource fs = (OGCFeatFeatureSource) datastore.getFeatureSource(kv.getKey());
                boolean b = Stream.of(fs.getCollection().getCrs()).filter(s -> OGCFeatFeatureSource.isEqualCrs(crs, s))
                        .findFirst().isPresent();

                ContentFeatureCollection features = fs.getFeatures(query);
                try (SimpleFeatureIterator iter = features.features()) {

                    while (iter.hasNext()) {
                        SimpleFeature f = iter.next();
                        System.out.println(f);

                    }

                }

            }
        } finally {
            datastore.dispose();
        }

    }
}
