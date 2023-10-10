package org.geoserver.extension.ogcfeat.datastore;

import java.io.IOException;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.data.FeatureReader;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.http.HTTPResponse;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class OGCFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature>, AutoCloseable {

	protected static final Logger LOGGER = Logging.getLogger(OGCFeatureReader.class.getName());

	private SimpleFeatureIterator iter;
	private SimpleFeatureType featureType;
	private GeoJSONReader reader;

	private HTTPResponse response;

	public OGCFeatureReader(SimpleFeatureType featureType, URL url) throws IOException {
		this.featureType = featureType;

		reader = new GeoJSONReader(url);
		reader.setSchema(featureType);
		iter = reader.getIterator();
	}

	public OGCFeatureReader(SimpleFeatureType featureType, HTTPResponse response) throws IOException {
		this.featureType = featureType;
		this.response = response;

		reader = new GeoJSONReader(response.getResponseStream());
		reader.setSchema(featureType);
		iter = reader.getIterator();

	}

	/** @see FeatureReader#getFeatureType() */
	@Override
	public SimpleFeatureType getFeatureType() {
		return featureType;
	}

	/** @see FeatureReader#hasNext() */
	@Override
	public boolean hasNext() {
		return iter.hasNext();
	}

	/**
	 * @throws IOException
	 * @see FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws NoSuchElementException, IOException {
		return iter.next();
	}

	@Override
	public void close() {
		try {
			LOGGER.info("Closing ITER");
			iter.close();
		} finally {
			try {
				LOGGER.info("Closing READER");
				// most likely closed by iter
				reader.close();
			} catch (IOException e) {

			} finally {
				if (response != null) {
					LOGGER.info("Disposing RESPONSE");
					response.dispose();
				}
			}
		}
	}
}
