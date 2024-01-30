package org.geoserver.extension.ogcfeat.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.geoserver.extension.ogcfeat.datastore.model.Collection;
import org.geoserver.extension.ogcfeat.datastore.model.Collections;
import org.geoserver.extension.ogcfeat.datastore.model.Conformance;
import org.geoserver.extension.ogcfeat.datastore.model.LandingPage;
import org.geoserver.extension.ogcfeat.datastore.model.Link;
import org.geotools.http.HTTPResponse;
import org.geotools.util.logging.Logging;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.media.Schema;

public class OGCFeatCatalogue {

    protected static final Logger LOGGER = Logging.getLogger(OGCFeatCatalogue.class.getName());

    public static final String CRS84 = OGCFeatCrsUtils.DEFAULT_CRS;
    // "http://www.opengis.net/def/crs/OGC/1.3/CRS84";

    // landing page
    // - conformance
    public static final String CONFORMANCE = "conformance";

    // - collections
    public static final String DATA = "data";
    public static final String APPLICATION_JSON = "application/json";

    //
    // collections - schema
    // https://github.com/opengeospatial/ogcapi-features/issues/338
    public static final String DESCRIBED_BY = "describedBy";
    public static final String DESCRIBEDBY = "describedby";
    public static final String APPLICATION_SCHEMA_JSON = "application/schema+json";

    // collections - items
    public static final String ITEMS = "items";
    public static final String APPLICATION_GEO_JSON = "application/geo+json";

    //
    public static final String ACCEPTS = APPLICATION_GEO_JSON + ", " +
    //
            APPLICATION_SCHEMA_JSON + ", " +
            //
            APPLICATION_JSON;

    // MUST support geo+json schema
    public static final Set<String> requirements = Set.of(
            //
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core",
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/oas30",
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson");

    //
    protected ObjectMapper OM = new ObjectMapper();

    Optional<Link> landingPageUrl = Optional.empty();

    protected Optional<LandingPage> landingPage = Optional.empty();
    protected Optional<Collections> collections = Optional.empty();
    protected Optional<Conformance> conformance = Optional.empty();
    protected OGCFeatBackendClient client;

    protected Map<String, Collection> collectionMap = new HashMap<>();

    public Set<String> keySet() {
        return collectionMap.keySet();
    }

    public Collection get(String id) {
        return collectionMap.get(id);
    }

    public OGCFeatCatalogue(String url, OGCFeatBackendClient client) {
        this.client = client;

        Link link = new Link();
        link.setHref(url);
        landingPageUrl = Optional.of(link);
    }

    public synchronized boolean catalogue(OGCFeatDataStore ogcFeatDataStore) throws IOException {

        if (landingPage.isPresent()) {
            LOGGER.info("catalogue set up");
            return false;
        }

        landingPage = fetch(landingPageUrl, LandingPage.class);

        conformance = fetch(landingPage.get().getLinks().stream().filter(OGCFeatCatalogue::conformanceLink).findFirst(),
                Conformance.class);

        if (!conformance.isPresent()) {
            return false;
        }

        if (!requirements()) {
            return false;
        }

        collections = fetch(landingPage.get().getLinks().stream().filter(OGCFeatCatalogue::collectionsLink).findFirst(),
                Collections.class);

        if (!collections.isPresent()) {
            return false;
        }

        collectionMap.putAll(
                collections.get().getCollections().stream().collect(Collectors.toMap(Collection::getId, c -> c)));

        return true;

    }

    public Optional<Schema> fetchSchema(Collection collection) throws IOException {

        return fetch(collection.getLinks().stream().filter(OGCFeatCatalogue::describedByLink).findFirst(),
                Schema.class);
    }

    public Optional<Link> itemsUrl(Collection collection) {
        return collection.getLinks().stream().filter(OGCFeatCatalogue::itemsGeoJSONLink).findFirst();
    }

    boolean requirements() {
        if (conformance.isEmpty()) {
            return false;
        }

        Integer req = requirements.size();
        return req.equals(conformance.get().conformsTo.stream().filter(s -> requirements.contains(s))
                .collect(Collectors.counting()).intValue());

    }

    static boolean conformanceLink(Link l) {
        return CONFORMANCE.equals(l.getRel()) && APPLICATION_JSON.equals(l.getType());
    }

    static boolean collectionsLink(Link l) {
        return DATA.equals(l.getRel()) && APPLICATION_JSON.equals(l.getType());
    }

    static boolean describedByLink(Link l) {
        return DESCRIBEDBY.equals(l.getRel()) && APPLICATION_SCHEMA_JSON.equals(l.getType())
                || DESCRIBED_BY.equals(l.getRel()) && APPLICATION_SCHEMA_JSON.equals(l.getType());
    }

    static boolean itemsGeoJSONLink(Link l) {
        return ITEMS.equals(l.getRel()) && APPLICATION_GEO_JSON.equals(l.getType());
    }

    protected <T> Optional<T> fetch(Optional<Link> link, Class<T> clazz) throws IOException {

        LOGGER.info("FETCH " + link + " " + clazz + " CLIENT " + client);

        if (link.isEmpty()) {
            return null;
        }
        URL url = new URL(link.get().getHref());

        HTTPResponse response = client.get(url);
        T t;
        try {
            try (InputStream inp = response.getResponseStream()) {
                t = OM.readValue(inp, clazz);
            }
        } finally {
            response.dispose();
        }
        return Optional.ofNullable(t);
    }

    public OGCFeatBackendClient getClient() {
        return client;
    }

}
