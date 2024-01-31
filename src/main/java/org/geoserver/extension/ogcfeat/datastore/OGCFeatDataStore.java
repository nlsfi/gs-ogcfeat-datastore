package org.geoserver.extension.ogcfeat.datastore;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.geoserver.extension.ogcfeat.datastore.model.Collection;
import org.geotools.api.data.Query;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.geotools.http.HTTPClient;
import org.geotools.util.logging.Logging;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.referencing.FactoryException;

import io.swagger.v3.oas.models.media.Schema;

public class OGCFeatDataStore extends ContentDataStore {

    protected static final Logger LOGGER = Logging.getLogger(OGCFeatDataStore.class.getName());

    //
    protected URL namespace;
    protected String username;
    protected String password;
    protected int limit;
    protected int pages;

    protected OGCFeatCatalogue catalogue;

    public OGCFeatCatalogue getCatalogue() {
        return catalogue;
    }

    protected Map<Name, OGCFeatFeatureSource> featureSources = new ConcurrentHashMap<>();

    private OGCFeatBackendClient client;

    public OGCFeatDataStore(String ns, String url, HTTPClient client) throws MalformedURLException {

    }

    public OGCFeatDataStore(String ns, String url, String user, String pass, Integer poolMax, Integer timeoutMillis,
            Integer limitMax, Integer pagingMax) throws MalformedURLException, URISyntaxException {
        client = new OGCFeatBackendClient(user, pass, poolMax, timeoutMillis, OGCFeatCatalogue.ACCEPTS);
        limit = limitMax;
        pages = pagingMax;
        LOGGER.info("CREATING DS " + ns + " Url " + url + " CLIENT " + client);
        namespace = new URI(ns).toURL();
        catalogue = new OGCFeatCatalogue(url, client);

        LOGGER.info("CREATED DS " + ns + " Url " + url);

    }

    @Override
    protected List<Name> createTypeNames() throws IOException {
        LOGGER.info("CREATING TYPENAMES");
        catalogue.catalogue(this);
        return catalogue.keySet().stream().map(s -> new NameImpl(namespace.toExternalForm(), s))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        LOGGER.info("CREATING FeatureSource");
        catalogue.catalogue(this);

        if (featureSources.containsKey(entry.getName())) {
            return featureSources.get(entry.getName());
        }

        Collection collection = catalogue.get(entry.getName().getLocalPart());

        Optional<Schema> schemaRef = catalogue.fetchSchema(collection);

        SimpleFeatureType featureType = OGCFeatSchemaUtils.buildFeatureType(collection, schemaRef, namespace);
        OGCFeatFeatureSource featureSource;
        try {
            featureSource = new OGCFeatFeatureSource(catalogue, entry, new Query(), collection, featureType, limit,
                    pages);
        } catch (FactoryException e) {
            LOGGER.warning(e.toString());
            throw new IOException(e);
        }

        featureSources.put(entry.getName(), featureSource);

        LOGGER.info(featureType.toString());

        return featureSource;
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
        super.dispose();
        LOGGER.info("CLOSING CLIENT");

        client.close();
        LOGGER.info("CLIENT CLOSED");
    }

}
