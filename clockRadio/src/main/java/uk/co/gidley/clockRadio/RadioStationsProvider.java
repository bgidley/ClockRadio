/*
 * Copyright 2011 Ben Gidley
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package uk.co.gidley.clockRadio;

import android.content.*;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import de.akquinet.android.androlog.Log;

public class RadioStationsProvider extends ContentProvider {

    private static String TAG = "RadioStationsProvider";

    /**
     * The database that the provider uses as its underlying data store
     */
    private static final String DATABASE_NAME = "radio_stations.db";

    // Handle to a new DatabaseHelper.
    private DatabaseHelper mOpenHelper;

    /**
     * The database version
     */
    private static final int DATABASE_VERSION = 2;

    private static final UriMatcher sUriMatcher;

    private static final int STATIONS = 1;
    private static final int STATION_ID = 2;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(RadioStations.AUTHORITY, "Stations", STATIONS);
        sUriMatcher.addURI(RadioStations.AUTHORITY, "Stations/#", STATION_ID);

    }

    static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {

            // calls the super constructor, requesting the default cursor
            // factory.
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * Creates the underlying database with table name and column names
         * taken from the RadioStations class.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + RadioStations.TABLE_NAME + " ("
                    + RadioStations.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + RadioStations.TITLE + " TEXT,"
                    + RadioStations.UNIQUE_NAME + " TEXT," + RadioStations.URL
                    + " TEXT" + ");");
        }

        /**
         * Demonstrates that the provider must consider what happens when the
         * underlying datastore is changed. In this sample, the database is
         * upgraded the database by destroying the existing data. A real
         * application should upgrade the database in place.
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            // Logs that the database is being upgraded
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");

            // Kills the table and existing data
            db.execSQL("DROP TABLE IF EXISTS " + RadioStations.TABLE_NAME);

            // Recreates the database with a new version
            onCreate(db);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;

        int count;

        // Does the delete based on the incoming URI pattern.
        switch (sUriMatcher.match(uri)) {

            // If the incoming pattern matches the general pattern for notes, does a
            // delete
            // based on the incoming "where" columns and arguments.
            case STATIONS:
                count = db.delete(RadioStations.TABLE_NAME, // The database table
                        // name
                        where, // The incoming where clause column names
                        whereArgs // The incoming where clause values
                );
                break;

            // If the incoming URI matches a single note ID, does the delete based
            // on the
            // incoming data, but modifies the where clause to restrict it to the
            // particular note ID.
            case STATION_ID:
                /*
                 * Starts a final WHERE clause by restricting it to the desired note
                 * ID.
                 */
                finalWhere = RadioStations.ID + // The ID column name
                        " = " + // test for equality
                        uri.getPathSegments(). // the incoming note ID
                                get(RadioStations.STATION_ID_OFFSET);

                // If there were additional selection criteria, append them to the
                // final
                // WHERE clause
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                // Performs the delete.
                count = db.delete(RadioStations.TABLE_NAME, // The database table
                        // name.
                        finalWhere, // The final WHERE clause
                        whereArgs // The incoming where clause values.
                );
                break;

            // If the incoming pattern is invalid, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }/*
		 * Gets a handle to the content resolver object for the current context,
		 * and notifies it that the incoming URI changed. The object passes this
		 * along to the resolver framework, and observers that have registered
		 * themselves for the provider are notified.
		 */
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows deleted.
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case STATIONS:
                return RadioStations.CONTENT_TYPE;

            case STATION_ID:
                return RadioStations.CONTENT_ITEM_TYPE;

            default:
                Log.e(TAG, "Unmatched URI :" + uri.toString());
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (sUriMatcher.match(uri) != STATIONS) {
            throw new IllegalArgumentException("Invalid URI " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(RadioStations.TABLE_NAME, RadioStations.TITLE,
                values);

        if (rowId > 0) {
            // Creates a URI with the note ID pattern and the new row ID
            // appended to it.
            Uri noteUri = ContentUris.withAppendedId(
                    RadioStations.CONTENT_ID_URI_BASE, rowId);

            // Notifies observers registered against this provider that the data
            // changed.
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(RadioStations.TABLE_NAME);

        switch (sUriMatcher.match(uri)) {
            case STATIONS:
                break;

            case STATION_ID:
                qb.appendWhere(RadioStations.ID
                        + "="
                        + uri.getPathSegments()
                        .get(RadioStations.STATION_ID_OFFSET));
                break;
        }

        String orderBy;
        // If no sort order is specified, uses the default
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = RadioStations.DEFAULT_SORT_ORDER;
        } else {
            // otherwise, uses the incoming sort order
            orderBy = sortOrder;
        }

        Cursor c = qb.query(mOpenHelper.getReadableDatabase(), projection,
                selection, selectionArgs, null, null, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;

    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;

        // Does the update based on the incoming URI pattern
        switch (sUriMatcher.match(uri)) {

            // If the incoming URI matches the general notes pattern, does the
            // update based on
            // the incoming data.
            case STATIONS:

                // Does the update and returns the number of rows updated.
                count = db.update(RadioStations.TABLE_NAME, // The database table
                        // name.
                        values, // A map of column names and new values to use.
                        selection, // The where clause column names.
                        selectionArgs // The where clause column values to select
                        // on.
                );
                break;

            // If the incoming URI matches a single note ID, does the update based
            // on the incoming
            // data, but modifies the where clause to restrict it to the particular
            // ID.
            case STATION_ID:
                /*
                 * Starts creating the final WHERE clause by restricting it to the
                 * incoming note ID.
                 */
                finalWhere = RadioStations.ID + // The ID column name
                        " = " + // test for equality
                        uri.getPathSegments(). // the incoming note ID
                                get(RadioStations.STATION_ID_OFFSET);

                // If there were additional selection criteria, append them to the
                // final WHERE
                // clause
                if (selection != null) {
                    finalWhere = finalWhere + " AND " + selection;
                }

                // Does the update and returns the number of rows updated.
                count = db.update(RadioStations.TABLE_NAME, // The database table
                        // name.
                        values, // A map of column names and new values to use.
                        finalWhere, // The final WHERE clause to use
                        // placeholders for whereArgs
                        selectionArgs // The where clause column values to select
                        // on, or
                        // null if the values are in the where argument.
                );
                break;
            // If the incoming pattern is invalid, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /*
           * Gets a handle to the content resolver object for the current context,
           * and notifies it that the incoming URI changed. The object passes this
           * along to the resolver framework, and observers that have registered
           * themselves for the provider are notified.
           */
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows updated.
        return count;
    }

}
