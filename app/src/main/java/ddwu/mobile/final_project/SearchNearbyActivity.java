package ddwu.mobile.final_project;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.List;

import noman.googleplaces.NRPlaces;
import noman.googleplaces.PlaceType;
import noman.googleplaces.PlacesException;
import noman.googleplaces.PlacesListener;

public class SearchNearbyActivity extends AppCompatActivity implements OnMapReadyCallback {

    final static String TAG = "SearchNearbyActivity";
    final static int PERMISSION_REQ_CODE = 100;

    private LocationManager locManager;

    private GoogleMap mGoogleMap;
    private MarkerOptions markerOptions;

    RadioGroup rGroup;
    String select;

    double myLocLat;
    double myLocLng;

    private PlacesClient placesClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_nearby);

        rGroup = findViewById(R.id.rgNearby);

        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // ?????? ?????? ??? ????????? ?????? ?????? ??? ?????? ??????
        if (checkPermission()) {
            mapLoad();
        }

        // Places ????????? ??? ??????????????? ??????
        Places.initialize(getApplicationContext(), getString(R.string.google));
        placesClient = Places.createClient(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        Log.d(TAG, "Map ready");

        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setOnMyLocationButtonClickListener(locationButtonClickListener);

        // TODO: ??? ?????? ??? ????????? ?????? ??? ?????? ??????
        markerOptions = new MarkerOptions();
//        mGeoDataClient = Places.getGeoDataClient(MainActivity.this);

        mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(final Marker marker) {
                String placeId = marker.getTag().toString();    // ????????? setTag() ??? ????????? Place ID ??????

                List<Place.Field> placeFields       // ??????????????? ????????? ????????? ?????? ??????
                        = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.PHONE_NUMBER, Place.Field.ADDRESS);

                FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();    // ?????? ??????

                // ?????? ?????? ??? ?????? ??????/?????? ????????? ??????
                placesClient.fetchPlace(request).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                    @Override                    // ?????? ?????? ??? ?????? ????????? ??????
                    public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {  // ?????? ?????? ???
                        Place place = fetchPlaceResponse.getPlace();
                        Log.i(TAG, "Place found: " + place.getName());  // ?????? ??? ?????? ???
                        Log.i(TAG, "Phone: " + place.getPhoneNumber());
                        Log.i(TAG, "Address: " + place.getAddress());

                        Intent intent = new Intent(SearchNearbyActivity.this, NearbyDetailActivity.class);
                        intent.putExtra("name", place.getName());
                        intent.putExtra("phone", place.getPhoneNumber());
                        intent.putExtra("address", place.getAddress());
                        intent.putExtra("select", select);
                        startActivity(intent);
                    }
                }).addOnFailureListener(new OnFailureListener() {   // ?????? ?????? ??? ?????? ????????? ??????
                    @Override
                    public void onFailure(@NonNull Exception exception) {   // ?????? ?????? ???
                        if (exception instanceof ApiException) {
                            ApiException apiException = (ApiException) exception;
                            int statusCode = apiException.getStatusCode();  // ?????? ??? ??????
                            Log.e(TAG, "Place not found: " + exception.getMessage());
                        }
                    }
                });
            }
        });
    }

    GoogleMap.OnMyLocationButtonClickListener locationButtonClickListener = new GoogleMap.OnMyLocationButtonClickListener() {
        @Override
        public boolean onMyLocationButtonClick() {
            if(checkPermission()) {
                locManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 5000, 0, locListener);
            }
            return false;
        }
    };

    LocationListener locListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            myLocLat = location.getLatitude();
            myLocLng = location.getLongitude();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btnSearchNearby:
                mGoogleMap.clear();     // ????????? ?????? ??????

                switch (rGroup.getCheckedRadioButtonId()) {
                    case R.id.rbtnGallery:
                        select = "gallery";
                        searchStart(PlaceType.ART_GALLERY);
                        break;
                    case R.id.rbtnLibrary:
                        select = "library";
                        searchStart(PlaceType.LIBRARY);
                        break;
                    case R.id.rbtnMuseum:
                        select = "museum";
                        searchStart(PlaceType.MUSEUM);
                        break;
                }
                break;
        }
    }

    private void searchStart(String type) {
        new NRPlaces.Builder().listener(placesListener)
                .key(getString(R.string.api_key))
                .latlng(myLocLat, myLocLng)
                .radius(500)
                .type(type)
                .build()
                .execute();
    }

    PlacesListener placesListener = new PlacesListener() {
        @Override
        public void onPlacesSuccess(final List<noman.googleplaces.Place> places) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (noman.googleplaces.Place place : places) {
                        markerOptions.title(place.getName());
                        markerOptions.position(new LatLng(place.getLatitude(), place.getLongitude()));

                        Marker newMarker = mGoogleMap.addMarker(markerOptions);
                        newMarker.setTag(place.getPlaceId());
                        Log.d(TAG, "ID: " + place.getPlaceId());
                    }
                }
            });
        }
        @Override
        public void onPlacesFailure(PlacesException e) {}
        @Override
        public void onPlacesStart() {}
        @Override
        public void onPlacesFinished() {}
    };

    /*???????????? ??????????????? ??????*/
    private void mapLoad() {
        MapFragment mapFragment =
                (MapFragment) getFragmentManager().findFragmentById(R.id.map2);

        mapFragment.getMapAsync(this);      // ???????????? this: MainActivity ??? OnMapReadyCallback ??? ???????????????
    }

    /* ?????? permission ?????? */
    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                        PERMISSION_REQ_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_REQ_CODE) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // ???????????? ??????????????? ?????? ??? ?????? ??????
                mapLoad();
            } else {
                // ????????? ????????? ??? ???????????? ??????
                Toast.makeText(this, "??? ????????? ?????? ?????? ????????? ?????????", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

}