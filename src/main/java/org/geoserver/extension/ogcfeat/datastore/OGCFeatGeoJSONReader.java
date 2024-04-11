package org.geoserver.extension.ogcfeat.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.extension.ogcfeat.datastore.model.Link;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.Geometries;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;

import com.bedatadriven.jackson.datatype.jts.parsers.GenericGeometryParser;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * https://github.com/geotools/geotools/blob/29.2/modules/unsupported/geojson-core/src/main/java/org/geotools/data/geojson/GeoJSONReader.java
 * Utility class to provide a reader for GeoJSON streams
 *
 * @author ian
 * @author nls-jajuko modifications
 */
public class OGCFeatGeoJSONReader implements AutoCloseable {
    /** GEOMETRY_NAME */
    public static final String GEOMETRY_NAME = "geometry";

    private static final Logger LOGGER = Logging.getLogger(OGCFeatGeoJSONReader.class);
    /** Top Level Attributes Not Included in Properties */
    public static final Object TOP_LEVEL_ATTRIBUTES = "topLevelAttributes";

    private JsonParser parser;

    // private static JsonFactory factory = new JsonFactory();

    private SimpleFeatureType schema;

    private SimpleFeatureBuilder builder;

    private int nextID = 0;

    private String baseName = "features";

    private boolean schemaChanged = false;

    private static GeometryFactory GEOM_FACTORY = new GeometryFactory();

    private static GenericGeometryParser GEOM_PARSER = new GenericGeometryParser(GEOM_FACTORY);

    private URL url;

    private String idPrefix;

    private String idFieldName = "id";

    private InputStream is;

    private boolean guessingDates = true;

    /** For reading be a bit more lenient regarding what we parse */
    private DateParser dateParser = new DateParser();

    ObjectMapper mapper = new ObjectMapper();

    /**
     * ID Strategy. AUTO is autogenerated using basename and incremented integer.
     * PREFIX uses a provided ID and prefix string. PROVIDED uses a provided ID
     * without a prefix. *
     */
    public enum IdStrategy {
        AUTO, PREFIX, PROVIDED
    }

    /** Default IdStrategy is Provided */
    private IdStrategy idStrategy = IdStrategy.PROVIDED;

    /**
     * Builds a GeoJSON parser from a GeoJSON document, provided as an
     * {@link InputStream}
     */
    public OGCFeatGeoJSONReader(InputStream is) throws IOException {
        parser = mapper.getFactory().createParser(is);
    }

    /**
     * Returns the Strategy Used to Generate IDs
     *
     * @return the idStrategy
     */
    public IdStrategy getIdStrategy() {
        return idStrategy;
    }

    /**
     * Sets the Strategy Used to Generate IDs
     *
     * @param idStrategy the idStrategy to set
     */
    public void setIdStrategy(IdStrategy idStrategy) {
        this.idStrategy = idStrategy;
    }

    /**
     * Get the Prefix to use for IDs
     *
     * @return the idPrefix
     */
    public String getIdPrefix() {
        return idPrefix;
    }

    /**
     * Set the Prefix to use for IDs
     *
     * @param idPrefix the idPrefix to set
     */
    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    /**
     * Get the Field Name to use for IDs
     *
     * @return the idFieldName
     */
    public String getIdFieldName() {
        return idFieldName;
    }

    /**
     * Set the Field Name to use for IDs
     *
     * @param idFieldName the idFieldName to set
     */
    public void setIdFieldName(String idFieldName) {
        this.idFieldName = idFieldName;
    }

    /**
     * Returns true if the parser is trying to convert string formatted as dates
     * into java.util.Date, false otherwise. Defaults to true.
     */
    public boolean isGuessingDates() {
        return guessingDates;
    }

    /** Enables/Disables guessing strings formatted as dates into java.util.Date. */
    public void setGuessingDates(boolean guessingDates) {
        this.guessingDates = guessingDates;
    }

    /**
     * Returns true if the source is still connected, false otherwise.
     *
     * @return
     */
    public boolean isConnected() {
        if (url != null) {
            try (InputStream inputStream = url.openStream()) {
                if (inputStream != null && inputStream.available() > 0) {
                    return true;
                }
                url = new URL(url.toExternalForm());
                try (InputStream inputStream2 = url.openStream()) {
                    return inputStream2 != null && inputStream2.available() > 0;
                }

            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Failure trying to determine if connected", e);
                return false;
            }
        }
        return true;
    }

    /** Parses and returns a single feature from the source */
    public SimpleFeature getFeature() throws IOException {
        ObjectMapper mapper = ObjectMapperFactory.getDefaultMapper();
        ObjectNode node = mapper.readTree(parser);
        return getNextFeature(node);
    }

    /** */
    private SimpleFeature getNextFeature(ObjectNode node) throws IOException {
        JsonNode type = node.get("type");
        if (type == null) {
            throw new RuntimeException("Missing object type in GeoJSON Parsing, expected type=Feature here");
        }
        if (!"Feature".equalsIgnoreCase(type.asText())) {
            throw new RuntimeException(
                    "Unexpected object type in GeoJSON Parsing, expected Feature got '" + type.asText() + "'");
        }
        JsonNode geom = node.get("geometry");

        // the geometry might have been selected away by a property selection
        Geometry g = null;
        if (geom != null)
            g = GEOM_PARSER.geometryFromJson(geom);

        JsonNode props = node.get("properties");
        // accommodate for STAC servers that remove the properties object altogether,
        // when
        // no property is selected using the STAC API Search Fields extension
        if (props == null)
            props = mapper.createObjectNode();

        if (builder == null || (builder.getFeatureType().getGeometryDescriptor() == null && g != null)
                || (builder.getFeatureType().getGeometryDescriptor() != null
                        && !builder.getFeatureType().getGeometryDescriptor().getType().getBinding().isInstance(g))) {
            builder = getBuilder(props, g);
        }
        boolean restart = true;
        SimpleFeature feature = null;
        while (restart) {
            restart = false;

            Iterator<Entry<String, JsonNode>> fields = props.fields();
            while (fields.hasNext()) {
                Entry<String, JsonNode> n = fields.next();
                AttributeDescriptor descriptor = schema.getDescriptor(n.getKey());
                if (descriptor == null) {
                    // we haven't seen this attribute before
                    restart = true;
                    builder = null;
                    // rebuild the schema
                    builder = getBuilder(props, g);
                    setSchemaChanged(true);
                    descriptor = schema.getDescriptor(n.getKey());
                }
                Class<?> binding = descriptor.getType().getBinding();
                if (binding == Integer.class) {
                    builder.set(n.getKey(), n.getValue().asInt());
                } else if (binding == Double.class) {
                    builder.set(n.getKey(), n.getValue().asDouble());
                } else if (binding == String.class) {
                    builder.set(n.getKey(), n.getValue().textValue());
                } else if (binding == Boolean.class) {
                    builder.set(n.getKey(), n.getValue().booleanValue());
                } else if (binding == Object.class) {
                    builder.set(n.getKey(), n.getValue());
                } else if (binding == List.class) {
                    ArrayNode array = (ArrayNode) n.getValue();
                    List<Object> list = new ArrayList<>();
                    for (int i = 0; i < array.size(); i++) {
                        JsonNode item = array.get(i);
                        Object vc;
                        switch (item.getNodeType()) {
                        case BOOLEAN:
                            vc = item.asBoolean();
                            break;
                        case NUMBER:
                            vc = item.asDouble();
                            break;
                        case STRING:
                            vc = item.asText();
                            break;
                        case OBJECT:
                            vc = item;
                            break;
                        case ARRAY:
                            vc = item;
                            break;
                        case NULL:
                            vc = null;
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Cannot handle arrays with values of type " + item.getNodeType());
                        }
                        list.add(vc);
                    }
                    builder.set(n.getKey(), list);
                } else if (Geometry.class.isAssignableFrom(binding)) {
                    Geometry geomAtt = GEOM_PARSER.geometryFromJson(n.getValue());
                    builder.set(n.getKey(), geomAtt);
                } else if (Date.class.isAssignableFrom(binding)) {
                    String text = n.getValue().asText();
                    Date date = dateParser.parse(text);
                    if (date != null) {
                        builder.set(n.getKey(), date);
                    } else {
                        // will go through the Converter machinery which, depending on the
                        // classpath, might try out a larger set of conversions, or end up
                        // with a null value
                        builder.set(n.getKey(), n.getValue().asText());
                    }

                } else {
                    LOGGER.warning("Unable to parse object of type " + binding);
                    builder.set(n.getKey(), n.getValue().asText());
                }
            }
            if (g != null)
                builder.set(GEOMETRY_NAME, g);
            String newId = getOrGenerateId(node);
            feature = builder.buildFeature(newId);
            if (node.fields().hasNext()) {
                Map<String, Object> topLevelAttributes = new HashMap<>();
                node.fields().forEachRemaining(e -> {
                    String k = e.getKey();
                    if (!"geometry".equals(k) && !"type".equals(k) && !"properties".equals(k) && !"bbox".equals(k)) {
                        topLevelAttributes.put(k, e.getValue());
                    }
                });
                if (!topLevelAttributes.isEmpty()) {
                    feature.getUserData().put(TOP_LEVEL_ATTRIBUTES, topLevelAttributes);
                }
            }
        }
        return feature;
    }

    private String getOrGenerateId(ObjectNode node) {
        switch (idStrategy) {
        case AUTO:
            return autogenerateId(node);
        case PREFIX:
            return prefixId(node);
        case PROVIDED:
            return providedId(node);
        default:
            throw new IllegalArgumentException("Unknown id strategy");
        }
    }

    private String providedId(ObjectNode node) {
        if (idFieldName != null && node.has(idFieldName)) {
            return node.get(idFieldName).asText();
        } else {
            return null; // Passing null to the builder will generate a new id
        }
    }

    private String prefixId(ObjectNode node) {
        String id = null;
        if (idFieldName != null && node.has(idFieldName)) {
            id = node.get(idFieldName).asText();
        } else {
            id = UUID.randomUUID().toString();
        }
        if (idPrefix != null) {
            return idPrefix + "." + id;
        } else {
            return baseName + "." + id;
        }
    }

    private String autogenerateId(ObjectNode node) {
        return baseName + "." + nextID++;
    }

    /**
     * Create a simpleFeatureBuilder for the current schema + these new properties.
     */
    private SimpleFeatureBuilder getBuilder(JsonNode props, Geometry g) {

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        // GeoJSON is always WGS84
        typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
        typeBuilder.setName(baseName);
        HashSet<String> existing = new HashSet<>();
        if (schema != null) {
            // copy the existing types to the new schema
            for (AttributeDescriptor att : schema.getAttributeDescriptors()) {
                // in case of Geometry, see if we have mixed geometry types
                if (att instanceof GeometryDescriptor && schema.getGeometryDescriptor() == att && g != null) {
                    GeometryDescriptor gd = (GeometryDescriptor) att;
                    Class<?> currClass = g.getClass();
                    Class<?> prevClass = gd.getType().getBinding();
                    if (!prevClass.isAssignableFrom(currClass)) {
                        typeBuilder.add(GEOMETRY_NAME, Geometry.class, DefaultGeographicCRS.WGS84);
                    } else {
                        typeBuilder.add(att);
                    }
                } else {
                    typeBuilder.add(att);
                }
                existing.add(att.getLocalName());
            }
        }

        if (typeBuilder.getDefaultGeometry() == null && g != null) {
            typeBuilder.setDefaultGeometry(GEOMETRY_NAME);
            if (!existing.contains(GEOMETRY_NAME)) {
                Class<?> geomType = g.getClass();
                typeBuilder.add(GEOMETRY_NAME, geomType, DefaultGeographicCRS.WGS84);
            }
        }

        Iterator<Entry<String, JsonNode>> fields = props.fields();
        while (fields.hasNext()) {
            Entry<String, JsonNode> n = fields.next();
            if (existing.contains(n.getKey())) {
                continue;
            } else {
                existing.add(n.getKey());
            }
            typeBuilder.nillable(true);
            JsonNode value = n.getValue();

            if (value instanceof IntNode) {
                typeBuilder.add(n.getKey(), Integer.class);
            } else if (value instanceof DoubleNode) {
                typeBuilder.add(n.getKey(), Double.class);
            } else if (value instanceof BooleanNode) {
                typeBuilder.add(n.getKey(), Boolean.class);
            } else if (value instanceof ObjectNode) {
                if (Optional.ofNullable(value.get("type")).map(t -> t.asText()).map(t -> Geometries.getForName(t))
                        .isPresent()) {
                    typeBuilder.add(n.getKey(), Geometry.class, DefaultGeographicCRS.WGS84);
                } else {
                    // a complex object, we don't know what it is going to be
                    typeBuilder.add(n.getKey(), Object.class);
                }
            } else if (value instanceof ArrayNode) {
                typeBuilder.add(n.getKey(), List.class);
            } else if (value instanceof TextNode && guessingDates) {
                // it could be a date too
                Date date = dateParser.parse(value.asText());
                if (date != null) {
                    typeBuilder.add(n.getKey(), Date.class);
                } else {
                    typeBuilder.defaultValue("");
                    typeBuilder.add(n.getKey(), String.class);
                }
            } else {
                typeBuilder.defaultValue("");
                typeBuilder.add(n.getKey(), String.class);
            }
        }

        schema = typeBuilder.buildFeatureType();

        return new SimpleFeatureBuilder(schema);
    }

    /**
     * Returns a {@link FeatureIterator} streaming over the provided source. The
     * feature type may evolve feature by feature, discovering new attributes that
     * were not previosly encountered.
     *
     * @return
     * @throws IOException
     */
    public GeoJsonIterator getIterator() throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected");
        }
        return new GeoJsonIterator(parser);
    }

    /**
     * Returns the current feature type, with the structure discovered so far while
     * parsing features (parse them all in order to get a final, stable feature
     * type):
     *
     * @return
     * @throws IOException
     */
    public FeatureType getSchema() throws IOException {
        if (!isConnected()) {
            throw new IOException("not connected to " + url.toExternalForm());
        }
        return schema;
    }

    /** @param schema the schema to set */
    public void setSchema(SimpleFeatureType schema) {
        this.schema = schema;
    }

    /** @return the schemaChanged */
    public boolean isSchemaChanged() {
        return schemaChanged;
    }

    /** @param schemaChanged the schemaChanged to set */
    public void setSchemaChanged(boolean schemaChanged) {
        this.schemaChanged = schemaChanged;
    }

    @Override
    public void close() throws IOException {
        if (parser != null) {
            parser.close();
            parser = null;
        }
        if (is != null) {
            is.close();
        }
    }

    protected class GeoJsonIterator implements SimpleFeatureIterator, AutoCloseable {
        JsonParser parser;

        private SimpleFeature feature;

        private JsonNode next;

        private Instant timeStamp;

        private Map meta;

        private List<Link> links = new ArrayList<>();

        public Instant getTimeStamp() {
            return timeStamp;
        }

        public Map getMeta() {
            return meta;
        }

        public List<Link> getLinks() {
            return links;
        }

        public GeoJsonIterator(JsonParser parser) throws IOException {
            if (!isConnected()) {
                throw new IOException("not connected to " + url.toExternalForm());
            }
            this.parser = parser;
            builder = null;
            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == null) {
                    break;
                }
                if (JsonToken.FIELD_NAME.equals(token) && "features".equalsIgnoreCase(parser.currentName())) {
                    token = parser.nextToken();

                    if (!JsonToken.START_ARRAY.equals(token) || token == null) {
                        throw new IOException("No Features found");
                    }
                    break;
                }
            }
        }

        @Override
        public boolean hasNext() {
            // make sure not to read too far if they call hasNext() multiple times
            if (feature != null) {
                return true;
            }
            try {

                JsonToken token = parser.nextToken();
                if (token == JsonToken.START_OBJECT) {
                    ObjectNode node = mapper.readTree(parser);
                    feature = getNextFeature(node);
                    if (feature != null)
                        return true;
                } else if (token == JsonToken.END_ARRAY) {
                    LOGGER.info("END OF FEATURES");
                    // EOF features
                    // read possible meta
                    // TODO: this has slight hakunapi assumptions
                    token = parser.nextToken();

                    for (int n = 0; n < 32; n++) {
                        if (token == null) {
                            return false;
                        }
                        if (token == JsonToken.END_OBJECT) {
                            return false;

                        }
                        if (!JsonToken.FIELD_NAME.equals(token)) {
                            return false;
                        }
                        //
                        String name = parser.currentName();
                        if ("links".equals(name)) {
                            List<Link> parsedLinks = parseLinks(parser);
                            if (parsedLinks != null) {
                                links.addAll(parsedLinks);
                            }
                            token = parser.nextToken();
                        } else if ("timeStamp".equals(name)) {
                            timeStamp = Instant.parse(parser.nextTextValue());
                            token = parser.nextToken();
                        } else if ("meta".equals(name)) {
                            meta = parser.readValueAs(Map.class);
                            token = parser.nextToken();
                        } else {
                            token = parser.nextToken();

                        }

                    }

                    return false;
                }
            } catch (IOException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
            return false;
        }

        private List<Link> parseLinks(JsonParser p) {
            try {
                // Last token was "links" field
                if (p.nextToken() != JsonToken.START_ARRAY) {
                    throw new IllegalStateException("Expected links to be an array");
                }
                List<Link> links = new ArrayList<>();
                while (p.nextToken() == JsonToken.START_OBJECT) {
                    links.add(p.readValueAs(Link.class));
                }
                return links;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public SimpleFeature next() throws NoSuchElementException {

            if (feature != null) {
                SimpleFeature ret = feature;
                feature = null;
                return ret;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        @SuppressWarnings("PMD.UseTryWithResources")
        public void close() {
            try {
                try {
                    if (parser != null)
                        parser.close();
                } finally {
                    if (is != null)
                        is.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("Unexpected failure closing iterator", e);
            }
            parser = null;
        }
    }
}
