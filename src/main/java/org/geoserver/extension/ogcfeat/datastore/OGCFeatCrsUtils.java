package org.geoserver.extension.ogcfeat.datastore;

import java.util.logging.Logger;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class OGCFeatCrsUtils {

	protected static final Logger LOGGER = Logging.getLogger(OGCFeatCrsUtils.class.getName());

	public static final String DEFAULT_CRS = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";

	// based on
	// https://github.com/geoserver/geoserver/blob/main/src/community/ogcapi/ogcapi-features/src/main/java/org/geoserver/ogcapi/v1/features/FeatureService.java#L165

	/** Maps authority and code to a CRS URI */
	static String mapCRSCode(String authority, String code) {
		return "http://www.opengis.net/def/crs/" + authority + "/0/" + code;
	}

	/** Returns the CRS-URI for a given CRS. */
	public static String getCRSURI(CoordinateReferenceSystem crs) throws FactoryException {
		
		//LOGGER.info("getCRSURI for"+crs);
		
		if (CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84)) {
			return DEFAULT_CRS;
		}
		String identifier = CRS.lookupIdentifier(crs, false);
		return mapResponseSRS(identifier);
	}

	private static String mapResponseSRS(String srs) {
		int idx = srs.indexOf(":");
		if (idx == -1)
			return mapCRSCode("EPSG", srs);
		String authority = srs.substring(0, idx);
		String code = srs.substring(idx + 1);
		return mapCRSCode(authority, code);
	}
}
