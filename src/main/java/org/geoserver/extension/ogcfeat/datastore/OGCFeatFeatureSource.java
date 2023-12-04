package org.geoserver.extension.ogcfeat.datastore;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.http.client.utils.URIBuilder;
import org.geoserver.extension.ogcfeat.datastore.model.Collection;
import org.geoserver.extension.ogcfeat.datastore.model.Link;
import org.geotools.data.DefaultResourceInfo;
import org.geotools.data.FeatureReader;
import org.geotools.data.FilteringFeatureReader;
import org.geotools.data.Query;
import org.geotools.data.ResourceInfo;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.filter.visitor.FilterVisitorSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.http.HTTPResponse;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.AnyInteracts;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.Meets;
import org.opengis.filter.temporal.MetBy;
import org.opengis.filter.temporal.OverlappedBy;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.filter.temporal.TOverlaps;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BinaryLogicOperator;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;

public class OGCFeatFeatureSource extends ContentFeatureSource {

	protected static final Logger LOGGER = Logging.getLogger(OGCFeatFeatureSource.class.getName());

	private OGCFeatCatalogue catalogue;
	private Collection collection;
	private DefaultResourceInfo resourceInfo;

	private int limit;
	private int pages;

	public Collection getCollection() {
		return collection;
	}

	public OGCFeatFeatureSource(OGCFeatCatalogue catalogue, ContentEntry entry, Query query, Collection collection,
			SimpleFeatureType schema, int limit, int pages) throws NoSuchAuthorityCodeException, FactoryException {
		super(entry, query);
		this.limit = limit;
		this.pages = pages;
		this.schema = schema;
		this.catalogue = catalogue;
		this.collection = collection;
		resourceInfo = new DefaultResourceInfo();
		resourceInfo.setCRS(CRS.decode(collection.getStorageCrs()));
		resourceInfo.setName(collection.getId());
		if (collection.getTitle() != null) {
			resourceInfo.setTitle(collection.getTitle());
		} else {
			resourceInfo.setTitle(collection.getId());
		}
	}

	public boolean isCrsCollectionSupported(CoordinateReferenceSystem crs) {

		if (isEqualCrs(crs, collection.getStorageCrs())) {
			return true;
		}

		return Stream.of(collection.getCrs()).filter(s -> isEqualCrs(crs, s)).findFirst().isPresent();

	}

	public static boolean isEqualCrs(CoordinateReferenceSystem crs, String serviceCrs) {
		String crsURI;
		try {
			crsURI = OGCFeatCrsUtils.getCRSURI(crs);
		} catch (FactoryException e) {
			return false;
		}

		return serviceCrs.equals(crsURI);
	}

	@Override
	protected boolean canReproject() {
		return true;
	}

	@Override
	protected boolean canLimit() {
		return true;
	}

	@Override
	protected boolean canFilter() {
		return true;
	}

	class BBOXFilterInfo extends DefaultFilterVisitor {
		public BBOX bbox;
		public boolean isSimple = false;

		@Override
		public Object visit(BBOX filter, Object data) {
			bbox = filter;
			return super.visit(filter, data);
		}
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		// LET's not support empty queries ATM
		// LET's support only BBOX ATM

		LOGGER.info(query.getFilter().toString());

		BBOXFilterInfo bboxInfo = new BBOXFilterInfo();

		bboxInfo.isSimple = query.getFilter() instanceof BBOX;

		LOGGER.info("OGCFeat " + query.toString());

		if (bboxInfo.isSimple) {
			bboxInfo.bbox = (BBOX) query.getFilter();
		} else {
			query.getFilter().accept(bboxInfo, bboxInfo);
		}

		if (bboxInfo.bbox == null) {
			throw new IOException("OGCFeat requires minimum a BBOX Filter");
		}

		CoordinateReferenceSystem bboxCrs = bboxInfo.bbox.getBounds().getCoordinateReferenceSystem();
		if (bboxCrs != null && !isCrsCollectionSupported(bboxCrs)) {
			throw new IOException("Invalid Filter (BBOX) CRS");
		}

		if (bboxCrs == null) {
			throw new IOException("Missing any CRS");
		}

		// QUERY CRS
		CoordinateReferenceSystem queryCrs = query.getCoordinateSystemReproject() != null
				? query.getCoordinateSystemReproject()
				: query.getCoordinateSystem();

		// gs renderer does not provide any QUERY CRS
		if (queryCrs == null) {
			queryCrs = bboxCrs;
		}

		if (queryCrs != null && !isCrsCollectionSupported(queryCrs)) {
			throw new IOException("Invalid CRS");
		}
		String crsParamValue;
		try {
			crsParamValue = OGCFeatCrsUtils.getCRSURI(queryCrs);
		} catch (FactoryException e) {
			throw new IOException(e);
		}

		final NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setGroupingUsed(false);

		final BoundingBox bounds = bboxInfo.bbox.getBounds();

		// AXIS ORDER ISSUES FOR BBOX

		final String bbox = String.format("%s,%s,%s,%s", nf.format(bounds.getMinX()), nf.format(bounds.getMinY()),
				nf.format(bounds.getMaxX()), nf.format(bounds.getMaxY()));
		String bboxCrsParamValue;
		try {
			bboxCrsParamValue = OGCFeatCrsUtils.getCRSURI(bboxCrs);
		} catch (FactoryException e) {
			throw new IOException(e);
		}

		final Optional<Link> link = catalogue.itemsUrl(collection);
		if (link.isEmpty()) {
			throw new IOException("Not found");
		}

		String href = link.get().getHref();

		// String accepts = link.get().getType();

		URL url = null;
		try {
			URIBuilder uriBuilder = new URIBuilder(href);
			uriBuilder.addParameter("bbox", bbox);
			uriBuilder.addParameter("bbox-crs", bboxCrsParamValue);
			uriBuilder.addParameter("crs", crsParamValue);

			if (!query.isMaxFeaturesUnlimited()) {
				int limitValue = Math.min(query.getMaxFeatures(), limit);
				String limitParamValue = Integer.toString(limitValue, 10);

				uriBuilder.addParameter("limit", limitParamValue);
			} else {
				uriBuilder.addParameter("limit", Integer.toString(limit, 10));

			}

			URI uri = uriBuilder.build();
			url = uri.toURL();
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}

		if (bboxInfo.isSimple) {
			LOGGER.info(collection.getId() + " items URL\n" + url);
			return new OGCFeatPagingCollectionItemsReader(catalogue, buildFeatureType(), url, pages);
		} else {
			OGCFeatPagingCollectionItemsReader delegate = new OGCFeatPagingCollectionItemsReader(catalogue,
					buildFeatureType(), url, pages);
			return new FilteringFeatureReader<>(delegate, query.getFilter());
		}
	}

	@Override
	public ResourceInfo getInfo() {
		return resourceInfo;
	}

	/**
	 * Calculates the bounds of a specified query. Subclasses must implement this
	 * method. If the computation is not fast, subclasses can return
	 * <code>null</code>.
	 */
	@Override
	protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {

		return null;
	}

	/**
	 * Calculates the number of features of a specified query. Subclasses must
	 * implement this method. If the computation is not fast, it's possible to
	 * return -1.
	 */
	@Override
	protected int getCountInternal(Query query) throws IOException {
		// HEAD items
		// "OGC-NumberMatched" header

		return -1;
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		return schema;
	}

}