    package com.pandya.atman.firstsunshine;

    import android.content.Intent;
    import android.net.Uri;
    import android.os.AsyncTask;
    import android.os.Bundle;
    import android.support.v4.app.Fragment;
    import android.support.v4.widget.SwipeRefreshLayout;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.Menu;
    import android.view.MenuInflater;
    import android.view.MenuItem;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.AdapterView;
    import android.widget.ArrayAdapter;
    import android.widget.ListView;
    import android.widget.Toast;

    import org.json.JSONArray;
    import org.json.JSONException;
    import org.json.JSONObject;

    import java.io.BufferedReader;
    import java.io.IOException;
    import java.io.InputStream;
    import java.io.InputStreamReader;
    import java.net.HttpURLConnection;
    import java.net.URL;
    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.Date;
    import java.util.GregorianCalendar;
    import java.util.List;


    /**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

        private ArrayAdapter<String> mForecastAdapter;
        private SwipeRefreshLayout mSwipeRefreshLayout = null;
        private String areaCode = "524901";

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if(id == R.id.action_refresh){
            FetchWeatherTask obj = new FetchWeatherTask();
            obj.execute(areaCode);
            
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_main,container,false);

            // Create some dummy data for the ListView.  Here's a sample weekly forecast
            String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11"
            };

            final List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));

            mForecastAdapter = new ArrayAdapter<String>(
                                            getActivity(),
                                            R.layout.list_item_forecast,
                                            R.id.list_item_forecast_textview,
                                            weekForecast);

            ListView listView =  (ListView) rootView.findViewById(R.id.listview_forecast);
            listView.setAdapter(mForecastAdapter);

            //Swipe down to refresh
            mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.fragment_container);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
                @Override
                public void onRefresh(){
                    new FetchWeatherTask().execute(areaCode);
                }
            });

            //Set item click listener to navigate to detail activity
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //Display Toast for onClick
                    String forecast = mForecastAdapter.getItem(position);
                    Toast clickedToast = Toast.makeText(getActivity(),"You have clicked: "+forecast, Toast.LENGTH_SHORT);
                    clickedToast.show();

                    Intent viewDetailsIntent = new Intent(getActivity(),DetailActivity.class)
                            .putExtra(Intent.EXTRA_TEXT,forecast);

                    startActivity(viewDetailsIntent);
                }
            });

            return rootView;
        }

    class FetchWeatherTask extends AsyncTask<String, Integer, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        final String FORECAST_URL_SCHEME = "http";
        final String FORECAST_URL_AUTHORITY = "api.openweathermap.org";
        final String PARAM_QUERY = "q";
        final String PARAM_FORMAT = "mode";
        final String PARAM_UNITS = "units";
        final String PARAM_DAYS = "cnt";

        //String FORECAST_URL_RELATIVE_PATH = "data/2.5/forecast/city?";
        String format = "json";
        String units = "metric";
        int numDays = 7;

        /* The date/time conversion code is going to be moved outside the asynctask later,
        * so for convenience we're breaking it out into its own method now.
        */
        private String getReadableDateString(Date time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            String[] resultStrs = new String[numDays];

            //create a Gregorian Calendar, which is in current date
            GregorianCalendar gc = new GregorianCalendar();

            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                Date dateTime;
                /* OLD
                    // Cheating to convert this to UTC time, which is what we want anyhow
                    dateTime = dayTime.setJulianDay(julianStartDay+i);
                    day = getReadableDateString(dateTime);
                */

                //add i dates to current date of calendar
                gc.add(GregorianCalendar.DATE, i);
                //get that date, format it, and "save" it on variable day
                dateTime = gc.getTime();

                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            return resultStrs;
        }

        @Override
        protected String[] doInBackground(String... params){
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            String[] returnData = null;
            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                //Using URI Builder
                Uri.Builder builtUri = new Uri.Builder();
                
                builtUri.scheme(FORECAST_URL_SCHEME)
                        .authority(FORECAST_URL_AUTHORITY)
                        .appendPath("data").appendPath("2.5").appendPath("forecast").appendPath("daily?")
                        .appendQueryParameter(PARAM_QUERY, params[0])
                        .appendQueryParameter(PARAM_FORMAT, format)
                        .appendQueryParameter(PARAM_UNITS,units)
                        .appendQueryParameter(PARAM_DAYS,Integer.toString(numDays)).build();

                URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/city?id=524901");
                URL urlNew = new URL(builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) urlNew.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

                Log.v(LOG_TAG, forecastJsonStr);
                returnData = getWeatherDataFromJson(forecastJsonStr,numDays);

            } catch (Exception e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
            }finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return returnData;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mForecastAdapter.clear();
                for(String dayForecastStr : result) {
                    mForecastAdapter.addAll(dayForecastStr);
                }
                //To stop the swipe-to-refresh icon
                if (mSwipeRefreshLayout.isRefreshing()) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }
        }
    }
    }

