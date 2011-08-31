package uk.co.gidley.clockRadio;

import android.app.Activity;
import android.app.ListFragment;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class RadioStationsList extends ListFragment {

	private static final String TAG = "RadioStationsList";

	public interface OnSelectStationListener {
		public void onSelectStationListener(String stationUri);
	}

	private OnSelectStationListener stationSelectListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		Cursor cursor = getActivity().managedQuery(RadioStations.CONTENT_URI,
				null, null, null, null);
		RadioStationsList radioStationsList = (RadioStationsList) getFragmentManager()
				.findFragmentById(R.id.stationList);
		radioStationsList.setListAdapter(new SimpleCursorAdapter(getActivity(),
				R.layout.list_item, cursor,
				new String[] { RadioStations.TITLE }, new int[] { R.id.text },
				SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER));

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			stationSelectListener = (OnSelectStationListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnSelectStationListener");
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				Uri selectedItem = ContentUris.withAppendedId(
						RadioStations.CONTENT_ID_URI_BASE, id);
				Cursor cursor = getActivity().managedQuery(selectedItem, null,
						null, null, null);
			
				if (cursor.moveToFirst()) {
					String url = cursor.getString(cursor
							.getColumnIndex(RadioStations.URL));
					stationSelectListener.onSelectStationListener(url);
					Log.d(TAG, "URL clicked:" + url);
				} else {
					Log.d(TAG, "Move to first was false");
				}
			}
		});
		super.onActivityCreated(savedInstanceState);
	}

}
