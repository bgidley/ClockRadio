package uk.co.gidley.clockRadio;

import android.app.ListFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class RadioStationsList extends ListFragment {

	static final String[] COUNTRIES = new String[] { "Bob", "Jack", "Sally" };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

//		setListAdapter(new ArrayAdapter<String>(getActivity(),
//				R.layout.list_item, COUNTRIES));

		Cursor cursor = getActivity().managedQuery(RadioStations.CONTENT_URI, null, null,
				null, null);
		RadioStationsList radioStationsList = (RadioStationsList) getFragmentManager()
				.findFragmentById(R.id.stationList);
		radioStationsList.setListAdapter(new SimpleCursorAdapter(
				getActivity(), R.layout.list_item, cursor,
				new String[] { RadioStations.TITLE }, new int[] { R.id.text },
				SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER));

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// When clicked, show a toast with the TextView text
				Toast.makeText(getActivity().getApplicationContext(),
						((TextView) view).getText(), Toast.LENGTH_SHORT).show();
			}
		});
		super.onActivityCreated(savedInstanceState);
	}

}
