package com.glaciersecurity.glaciermessenger.ui;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.osmdroid.util.GeoPoint;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.ActivityShowLocationBinding;
import com.glaciersecurity.glaciermessenger.ui.util.LocationHelper;
import com.glaciersecurity.glaciermessenger.ui.util.Log;
import com.glaciersecurity.glaciermessenger.ui.util.UriHelper;
import com.glaciersecurity.glaciermessenger.ui.widget.Marker;
import com.glaciersecurity.glaciermessenger.ui.widget.MyLocation;
import com.glaciersecurity.glaciermessenger.utils.LocationProvider;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.location.OnLocationClickListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import static android.content.Intent.ACTION_VIEW;
import static com.glaciersecurity.glaciermessenger.ui.ActionBarActivity.configureActionBar;


public class ShowLocationActivity extends XmppActivity implements OnMapReadyCallback {

	// Variables needed to initialize a map
	private MapboxMap mapboxMap;
	private MapView mapView;
	// Variables needed to handle location permissions
	private PermissionsManager permissionsManager;
	// Variables needed to add the location engine
	private LocationEngine locationEngine;
	private long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
	private long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
	private Location loc;
	// Variables needed to listen to location updates

	public static final String ACTION_SHOW_LOCATION = "show_location";

	//private ShowLocationCallback callback = new ShowLocationCallback(this);


	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Mapbox.getInstance(this, getString(R.string.mapbox_token));

		setContentView(R.layout.activity_show_location);



		mapView = findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(this);

		setSupportActionBar(findViewById(R.id.toolbars));
		configureActionBar(getSupportActionBar());




//		this.binding = DataBindingUtil.setContentView(this,R.layout.activity_show_location);

//
//		setSupportActionBar((Toolbar) binding.toolbar);
//
//		configureActionBar(getSupportActionBar());
//		//setupMapView(this.binding.map, this.loc);
//
//		this.binding.fab.setOnClickListener(view -> startNavigation());
//

//					final Uri geoUri = intent.getData();
//
//					// Attempt to set zoom level if the geo URI specifies it
//					if (geoUri != null) {
//						final HashMap<String, String> query = UriHelper.parseQueryString(geoUri.getQuery());
//
//						// Check for zoom level.
//						final String z = query.get("z");
//						if (z != null) {
//							try {
//								mapController.setZoom(Double.valueOf(z));
//							} catch (final Exception ignored) {
//							}
//						}
//
//						// Check for the actual geo query.
//						boolean posInQuery = false;
//						final String q = query.get("q");
//						if (q != null) {
//							final Pattern latlng = Pattern.compile("/^([-+]?[0-9]+(\\.[0-9]+)?),([-+]?[0-9]+(\\.[0-9]+)?)(\\(.*\\))?/");
//							final Matcher m = latlng.matcher(q);
//							if (m.matches()) {
//								try {
//									this.loc = new GeoPoint(Double.valueOf(m.group(1)), Double.valueOf(m.group(3)));
//									posInQuery = true;
//								} catch (final Exception ignored) {
//								}
//							}
//						}
//
//						final String schemeSpecificPart = geoUri.getSchemeSpecificPart();
//						if (schemeSpecificPart != null && !schemeSpecificPart.isEmpty()) {
//							try {
//								final GeoPoint latlong = LocationHelper.parseLatLong(schemeSpecificPart);
//								if (latlong != null && !posInQuery) {
//									this.loc = latlong;
//								}
//							} catch (final NumberFormatException ignored) {
//							}
//						}
//					}

//			}
//			updateLocationMarkers();

//
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//
//// Mapbox access token is configured here. This needs to be called either in your application
//// object or in the same activity which contains the mapview.
//		Mapbox.getInstance(this, getString(R.string.access_token));
//
//// This contains the MapView in XML and needs to be called after the access token is configured.
//		setContentView(R.layout.activity_main);

		mapView = findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(this);
	}

	@Override
	public void onMapReady(@NonNull final MapboxMap mapboxMap) {
		this.mapboxMap = mapboxMap;

		mapboxMap.setStyle(Style.TRAFFIC_NIGHT,
				new Style.OnStyleLoaded() {
					@Override
					public void onStyleLoaded(@NonNull Style style) {
						enableLocationComponent(style);
						processViewIntent(getIntent());
					}
				});
	}

	protected void processViewIntent(@NonNull Intent intent) {
		if (intent != null) {
			final String action = intent.getAction();
			if (action == null) {
				return;
			}
			switch (action) {
				case "com.glaciersecurity.glaciermessenger.location.show":
					if (intent.hasExtra("longitude") && intent.hasExtra("latitude")) {
						final double longitude = intent.getDoubleExtra("longitude", 0);
						final double latitude = intent.getDoubleExtra("latitude", 0);
						this.loc = new Location("");
						loc.setLatitude(latitude);
						loc.setLongitude(longitude);
					}
					break;
				case ACTION_SHOW_LOCATION:
					if (intent.hasExtra("longitude") && intent.hasExtra("latitude")) {
						final double longitude = intent.getDoubleExtra("longitude", 0);
						final double latitude = intent.getDoubleExtra("latitude", 0);
						this.loc = new Location("");
						loc.setLatitude(latitude);
						loc.setLongitude(longitude);
						mapboxMap.getLocationComponent().forceLocationUpdate(loc);

					}
					break;
			}
		}
	}

	/**
	 * Initialize the Maps SDK's LocationComponent
	 */
	@SuppressWarnings( {"MissingPermission"})
	private void enableLocationComponent(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request

// Get an instance of the component
			LocationComponent locationComponent = mapboxMap.getLocationComponent();

// Set the LocationComponent activation options
			LocationComponentActivationOptions locationComponentActivationOptions =
					LocationComponentActivationOptions.builder(this, loadedMapStyle)
							.useDefaultLocationEngine(false)
							.build();

// Activate with the LocationComponentActivationOptions object
			locationComponent.activateLocationComponent(locationComponentActivationOptions);

// Enable to make component visible
			locationComponent.setLocationComponentEnabled(true);

// Set the component's camera mode
			locationComponent.setCameraMode(CameraMode.TRACKING);

// Set the component's render mode
			locationComponent.setRenderMode(RenderMode.COMPASS);

		//	initLocationEngine();

	}

	/**
	 * Set up the LocationEngine and the parame
	 * ters for querying the device's location
	 */

//	@SuppressLint("MissingPermission")
//	private void initLocationEngine() {
//		locationEngine = LocationEngineProvider.getBestLocationEngine(this);
//
//		LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
//				.setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
//				.setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();
//
//		locationEngine.requestLocationUpdates(request, callback, getMainLooper());
//		locationEngine.getLastLocation(callback);
//	}

//	@Override
//	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//		permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
//	}
//
//	@Override
//	public void onExplanationNeeded(List<String> permissionsToExplain) {
//		//Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
//	}
//
//	@Override
//	public void onPermissionResult(boolean granted) {
//		if (granted) {
//			if (mapboxMap.getStyle() != null) {
//				enableLocationComponent(mapboxMap.getStyle());
//			}
//		} else {
//			//Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
//			finish();
//		}
//	}

//	private static class ShowLocationCallback
//			implements LocationEngineCallback<LocationEngineResult> {
//
//		private final WeakReference<ShowLocationActivity> activityWeakReference;
//
//		ShowLocationCallback(ShowLocationActivity activity) {
//			this.activityWeakReference = new WeakReference<>(activity);
//		}
//
//		/**
//		 * The LocationEngineCallback interface's method which fires when the device's location has changed.
//		 *
//		 * @param result the LocationEngineResult object which has the last known location within it.
//		 */
//		@Override
//		public void onSuccess(LocationEngineResult result) {
//			ShowLocationActivity activity = activityWeakReference.get();
//
//			if (activity != null) {
//				Location location = result.getLastLocation();
//
//				if (location == null) {
//					return;
//				}
//
//// Create a Toast which displays the new location's coordinates
////				Toast.makeText(activity, String.format(activity.getString(R.string.new_location),
////						String.valueOf(result.getLastLocation().getLatitude()), String.valueOf(result.getLastLocation().getLongitude())),
////						Toast.LENGTH_SHORT).show();
//
//// Pass the new location to the Maps SDK's LocationComponent
//				if (activity.mapboxMap != null && result.getLastLocation() != null) {
//					activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
//				}
//			}
//		}
//
//		/**
//		 * The LocationEngineCallback interface's method which fires when the device's location can not be captured
//		 *
//		 * @param exception the exception message
//		 */
//		@Override
//		public void onFailure(@NonNull Exception exception) {
//			Log.d("LocationChangeActivity", exception.getLocalizedMessage());
//			ShowLocationActivity activity = activityWeakReference.get();
//			if (activity != null) {
//				Toast.makeText(activity, exception.getLocalizedMessage(),
//						Toast.LENGTH_SHORT).show();
//			}
//		}
//	}

	@Override
	public void onStart() {
		super.onStart();
		mapView.onStart();
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
	protected void onStop() {
		super.onStop();
		mapView.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
// Prevent leaks
//		if (locationEngine != null) {
//			locationEngine.removeLocationUpdates(callback);
//		}
		mapView.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}

	@Override
	void onBackendConnected() {

	}

	@Override
	protected void refreshUiReal() {

	}

}
