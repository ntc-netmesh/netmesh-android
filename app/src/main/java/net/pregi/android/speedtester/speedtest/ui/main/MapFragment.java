package net.pregi.android.speedtester.speedtest.ui.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.pregi.android.speedtester.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;

/**
 * A placeholder fragment containing a simple view.
 */
@Deprecated
public class MapFragment extends BaseFragment {
    // For some reason the lifecycle of the MapView has to be called manually.

    // Initializing the map:
    //      https://stackoverflow.com/questions/26174527/android-mapview-in-fragment
    // TODO: do a check on whether Google Play services is installed.
    //      https://stackoverflow.com/questions/13778965/google-maps-android-api-v2-check-if-googlemaps-are-installed-on-device
    // Check if location access is permitted:
    //      https://stackoverflow.com/questions/36091197/how-to-solve-setmylocationenabled-permission-requirement
    //      https://developer.android.com/training/permissions/requesting#java

    private MapView mapView;
    private GoogleMap map;

    private int PERMISSIONREQUEST_LOCATION;

    private boolean isMapAsyncRun = false;
    private synchronized void getMapAsync() {
        // This might be run before the fragment is even ready,
        //      usually because the user tapped on this fragment's tab directly.
        if (!isMapAsyncRun && mapView != null) {
            // Run this such that this is only done when the fragment comes into view.

            // Gets to GoogleMap from the MapView and does initialization stuff
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {
                    map.getUiSettings().setMyLocationButtonEnabled(false);

                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        map.setMyLocationEnabled(true);

                        // Updates the location and zoom of the MapView
                        // CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(43.1, -87.9), 10);
                        // map.animateCamera(cameraUpdate);
                    } else {
                        /*
                        ActivityCompat.requestPermissions(
                                getActivity(),
                                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                                PERMISSIONREQUEST_LOCATION
                        );*/
                    }
                }
            });
        }

        isMapAsyncRun = true;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        // This is how we know whether the user is about to view this fragment.
        if (isVisibleToUser) {
            getMapAsync();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PERMISSIONREQUEST_LOCATION = getResources().getInteger(R.integer.permissionrequest_location);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONREQUEST_LOCATION) {
            getMapAsync();
        }
    }

    @Override
    public void onCreateView(View root,
                             @NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mapView = root.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);

        // This might be run before the fragment even becomes visible,
        //    usually because they swiped to a fragment that comes before this one.
        if (getUserVisibleHint()) {
            getMapAsync();
        }

        // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
        MapsInitializer.initialize(this.getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    public MapFragment() {
        super(R.layout.fragment_speedtest_map);
    }
}