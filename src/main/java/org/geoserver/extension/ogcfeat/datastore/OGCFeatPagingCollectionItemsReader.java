package org.geoserver.extension.ogcfeat.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import org.geotools.api.data.FeatureReader;
import org.geotools.http.HTTPResponse;
import org.geotools.util.logging.Logging;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

/*
 * WIP
 * paging feature reader that should close responses
 */
public class OGCFeatPagingCollectionItemsReader
		implements FeatureReader<SimpleFeatureType, SimpleFeature>, AutoCloseable {

	protected static final Logger LOGGER = Logging.getLogger(OGCFeatPagingCollectionItemsReader.class.getName());

	private OGCFeatGeoJSONReader.GeoJsonIterator iter;
	private SimpleFeatureType featureType;
	private OGCFeatGeoJSONReader reader;

	private HTTPResponse response;

	private int pages;
	private int page = 0;

	private OGCFeatCatalogue catalogue;
	private URL url;
	private Throwable err;

	public Throwable getErr() {
		return err;
	}

	public OGCFeatPagingCollectionItemsReader(OGCFeatCatalogue catalogue, SimpleFeatureType buildFeatureType, URL url,
			int pages) throws IOException {
		this.url = url;
		this.pages = pages;
		this.catalogue = catalogue;
		this.featureType = buildFeatureType;
	}

	protected void page(URL url) throws IOException {
		++page;
		info("PAGE " + url);
		response = catalogue.getClient().get(url);
		InputStream inp = response.getResponseStream();
		if (inp == null) {
			throw new IOException("No response for PAGE");
		}
		reader = new OGCFeatGeoJSONReader(inp);
		reader.setSchema(featureType);
		iter = reader.getIterator();
	}

	/** @see FeatureReader#getFeatureType() */
	@Override
	public SimpleFeatureType getFeatureType() {
		return featureType;
	}

	private void info(String msg) {
		LOGGER.info("#" + page + ": " + msg);
	}

	/** @see FeatureReader#hasNext() */
	@Override
	public boolean hasNext() {
		if (page == 0) {
			try {
				page(url);
			} catch (IOException e) {
				err = e;
				LOGGER.severe(e.toString());
				return false;
			}
		}

		boolean iterHasNext = iter.hasNext();

		if (iterHasNext) {
			return true;
		}

		if (page >= pages) {
			info("hasNext PAGE>=PAGES " + page + ">=" + pages + " FALSE");
			return false;
		}

		Optional<String> urlRef = iter.getLinks().stream()
				.filter(l -> "next".equalsIgnoreCase(l.getRel()) && l.getType().indexOf("json") != -1)
				.map(l -> l.getHref()).filter(Objects::nonNull).findFirst();
		if (!urlRef.isPresent()) {
			return false;
		}

		try {
			url = new URL(urlRef.get());
		} catch (MalformedURLException e) {
			err = e;
			LOGGER.severe(e.toString());
			return false;
		}

		// TODO - most likely error - should be this.close() instead of iter.close()
		iter.close();

		try {
			page(url);
		} catch (IOException e) {
			err = e;
			LOGGER.severe(e.toString());
			return false;
		}

		iterHasNext = iter.hasNext();

		if (iterHasNext) {
			return true;
		}

		return false;
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
			info("Closing ITER");
			iter.close();
		} finally {
			iter = null;
			try {
				info("Closing READER");
				// most likely closed by iter
				reader.close();
			} catch (IOException e) {
				info("CLOSE " + e.toString());
			} finally {
				reader = null;
				if (response != null) {
					info("Disposing RESPONSE");
					response.dispose();
					response = null;
				}
			}
		}
	}
}
