package com.example.zooseeker_jj_zaaz_team_52.location;

import android.annotation.SuppressLint;
import android.app.Application;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.zooseeker_jj_zaaz_team_52.ZooData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class LocationModel extends AndroidViewModel {
    private final String TAG = "LocationModel";
    private final MediatorLiveData<ZooLocation> lastKnownCoords;

    private LiveData<ZooLocation> locationProviderSource = null;
    private final MutableLiveData<ZooLocation> mockSource;

    private Map<String, ZooData.VertexInfo> zooData;

    public LocationModel(@NonNull Application application) {
        super(application);
        lastKnownCoords = new MediatorLiveData<>();
        // Create and add the mock source.
        mockSource = new MutableLiveData<>();
        lastKnownCoords.addSource(mockSource, lastKnownCoords::setValue);
    }

    public LiveData<ZooLocation> getLastKnownCoords() {
        return lastKnownCoords;
    }
        
    /**
     * @param locationManager the location manager to request updates from.
     * @param provider        the provider to use for location updates (usually GPS).
     * @apiNote This method should only be called after location permissions have been obtained.
     * @implNote If a location provider source already exists, it is removed.
     */
    @SuppressLint("MissingPermission")
    public void addLocationProviderSource(LocationManager locationManager, String provider) {
        // If a location provider source is already added, remove it.
        if (locationProviderSource != null) {
            removeLocationProviderSource();
        }

        // Create a new GPS source.
        var providerSource = new MutableLiveData<ZooLocation>();
        var locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                var coord = Coord.fromLocation(location);
                var zooLocation = new ZooLocation(coord, findNearestLandmark(coord));
                Log.i(TAG, String.format("Model received GPS location update: %s", coord));
                providerSource.postValue(zooLocation);
            }
        };
        // Register for updates.
        locationManager.requestLocationUpdates(provider, 0, 0f, locationListener);

        locationProviderSource = providerSource;
        lastKnownCoords.addSource(locationProviderSource, lastKnownCoords::setValue);
    }

    void removeLocationProviderSource() {
        if (locationProviderSource == null) return;
        lastKnownCoords.removeSource(locationProviderSource);
    }

    public ZooData.VertexInfo findNearestLandmark(Coord coord) {
        double minDistance = Double.MAX_VALUE;
        ZooData.VertexInfo nearestLandmark = null;
        for (ZooData.VertexInfo landmark : zooData.values()) {
            float[] results = new float[1];
            Location.distanceBetween(coord.lat, coord.lng, landmark.lat, landmark.lng, results);
            if (results[0] < minDistance) {
                minDistance = results[0];
                nearestLandmark = landmark;
            }
        }

        return nearestLandmark;
    }

    @VisibleForTesting
    public void mockLocation(Coord coords) {
        mockSource.postValue(new ZooLocation(coords, findNearestLandmark(coords)));
    }

    @SuppressLint("DefaultLocale")
    @VisibleForTesting
    public Future<?> mockRoute(List<Coord> route, long delay, TimeUnit unit) {
        return Executors.newSingleThreadExecutor().submit(() -> {
            int i = 1;
            int n = route.size();
            for (var coord : route) {
                // Mock the location...
                Log.i(TAG, String.format("Model mocking route (%d / %d): %s", i++, n, coord));
                mockLocation(coord);

                // Sleep for a while...
                try {
                    Thread.sleep(unit.toMillis(delay));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setZooData(Map<String, ZooData.VertexInfo> zooData) {
        this.zooData = zooData;
    }
}
