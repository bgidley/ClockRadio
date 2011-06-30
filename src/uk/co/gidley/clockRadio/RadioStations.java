package uk.co.gidley.clockRadio;

import android.content.SyncAdapterType;
import android.net.Uri;

public class RadioStations {

	public static final String AUTHORITY = "uk.co.gidley.clockradio.radiostationsprovider";

	public static final String STATIONS_PATH = "/Stations";

	public static final String STATIONS_PATH_ID = STATIONS_PATH + "/";

	public static final String SCHEME = "content://";

	public static final Uri CONTENT_URI = Uri
	.parse(SCHEME + AUTHORITY + STATIONS_PATH);
	
	public static final int STATION_ID_OFFSET=1;

	public static final Uri STATIONS_ID_URI = Uri.parse(SCHEME + AUTHORITY + STATIONS_PATH_ID + "#");

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.uk.co.gidley.clockRadio.Station";

	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.uk.co.gidley.clockRadio.Station";

	private RadioStations() {
	}

	public static final String TABLE_NAME = "notes";

	public static final String ID = "_id";

	public static final String TITLE = "title";

	public static final String URL = "url";

	public static final String DEFAULT_SORT_ORDER = "_id DESC";

	public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME + AUTHORITY + STATIONS_PATH_ID);
}
