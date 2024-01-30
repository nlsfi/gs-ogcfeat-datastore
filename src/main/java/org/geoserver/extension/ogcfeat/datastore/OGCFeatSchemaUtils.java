package org.geoserver.extension.ogcfeat.datastore;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.geoserver.extension.ogcfeat.datastore.model.Collection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.text.Text;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import io.swagger.v3.oas.models.media.Schema;

public class OGCFeatSchemaUtils {

	public static SimpleFeatureType buildFeatureType(Collection collection, Optional<Schema> schemaRef, URL ns)
			throws IOException {

		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		if (schemaRef.isEmpty()) {
			return builder.buildFeatureType();
		}

		builder.setName(collection.getId());
		builder.setNamespaceURI(ns.toExternalForm());

		if (collection.getDescription() != null) {
			builder.setDescription(Text.text(collection.getDescription()));
		}

		Schema<?> schema = schemaRef.get();

		Map<String, Schema> type = schema.getProperties();

		Schema idSchema = type.get("id");
		Schema geomSchema = type.get("geometry");

		if (collection.getStorageCrs() != null) {
			CoordinateReferenceSystem storageCRS;
			try {
				storageCRS = CRS.decode(collection.getStorageCrs());
			} catch (FactoryException e) {
				throw new IOException(e);
			}
			builder.setCRS(storageCRS);

		}

		if (geomSchema != null) {
			Map<String, Schema> geomTypeSchema = geomSchema.getProperties();
			List<Schema> geomOneOfSchema = geomOneOfSchema = geomSchema.getOneOf();

			String geomType = geomTypeSchema != null ? 
					geomTypeSchema.get("type").getEnum().get(0).toString() : "oneOf";
			switch (geomType) {
			case "Point":
				builder.add("geometry", Point.class);
				break;
			case "LineString":
				builder.add("geometry", LineString.class);
				break;
			case "Polygon":
				builder.add("geometry", Polygon.class);
				break;
			case "MultiPoint":
				builder.add("geometry", MultiPoint.class);
				break;
			case "MultiLineString":
				builder.add("geometry", MultiLineString.class);
				break;
			case "MultiPolygon":
				builder.add("geometry", MultiPolygon.class);
				break;
			case "oneOf":
			default:
				builder.add("geometry", Geometry.class);
				break;
			}
			builder.setDefaultGeometry("geometry");
		}

		Schema propsSchema = type.get("properties");
		Map<String, Schema> props = propsSchema.getProperties();

		for (Entry<String, Schema> prop : props.entrySet()) {

			String propName = prop.getKey();
			String propType = prop.getValue().getType();
			String propFormat = prop.getValue().getFormat();

			switch (propType) {
			case "string":
				builder.add(propName, String.class);
				break;
			case "integer": // ? Long?
				builder.add(propName, Integer.class);
				break;
			case "number":
				builder.add(propName, Double.class);
				break;
			case "boolean":
				builder.add(propName, Boolean.class);
				break;

			}

		}

		return builder.buildFeatureType();
	}
}
