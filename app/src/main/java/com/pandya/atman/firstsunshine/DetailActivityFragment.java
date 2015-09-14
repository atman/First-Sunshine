package com.pandya.atman.firstsunshine;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    public DetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        Intent receivedIntent = getActivity().getIntent();
        String forecastData = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);

        TextView detailData = (TextView) rootView.findViewById(R.id.forecast_detail);
        detailData.setText(forecastData);

        return rootView;
    }
}
