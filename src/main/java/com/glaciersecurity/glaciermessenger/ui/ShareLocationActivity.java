package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.provider.Settings;
import android.view.View;
import android.graphics.Color;

import android.widget.Button;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.location.OnLocationClickListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.ActivityShareLocationBinding;
import com.glaciersecurity.glaciermessenger.ui.util.LocationHelper;
import com.glaciersecurity.glaciermessenger.ui.widget.Marker;
import com.glaciersecurity.glaciermessenger.ui.widget.MyLocation;
import com.glaciersecurity.glaciermessenger.utils.LocationProvider;
import com.glaciersecurity.glaciermessenger.utils.ThemeHelper;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.location.OnLocationClickListener;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.List;

import static com.glaciersecurity.glaciermessenger.ui.ActionBarActivity.configureActionBar;

public class ShareLocationActivity extends XmppActivity implements OnMapReadyCallback, OnLocationClickListener, PermissionsListener, OnCameraTrackingChangedListener {

	public static final int REQUEST_CODE_CREATE = 0;
	public static final int REQUEST_CODE_FAB_PRESSED = 1;
	public static final int REQUEST_CODE_SNACKBAR_PRESSED = 2;

	private Snackbar snackBar;
	//private ActivityShareLocationBinding binding;
	private boolean marker_fixed_to_loc = false;
	private static final String KEY_FIXED_TO_LOC = "fixed_to_loc";
	private Boolean noAskAgain = false;


	private PermissionsManager permissionsManager;
	private MapView mapView;
	private Button cancelButton;
	private Button shareButton;
	private CoordinatorLayout snackbarCoordinator;
	private MapboxMap mapboxMap;
	private LocationComponent locationComponent;
	private FloatingActionButton fab;
	private boolean isInTrackingMode;
	private Location locationListener;


	@Override
	protected void onSaveInstanceState(@NonNull final Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
		outState.putBoolean(KEY_FIXED_TO_LOC, marker_fixed_to_loc);
	}

	@Override
	protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		if (savedInstanceState.containsKey(KEY_FIXED_TO_LOC)) {
			this.marker_fixed_to_loc = savedInstanceState.getBoolean(KEY_FIXED_TO_LOC);
		}
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Mapbox.getInstance(this, getString(R.string.mapbox_token));

		// This contains the MapView in XML and needs to be called after the access token is configured.
		setContentView(R.layout.activity_share_location);

		cancelButton = findViewById(R.id.cancel_button);
		shareButton = findViewById(R.id.share_button);
		snackbarCoordinator = findViewById(R.id.snackbar_coordinator);
		fab = findViewById(R.id.fab);

		mapView = findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(this);

		setSupportActionBar(findViewById(R.id.toolbar));
		configureActionBar(getSupportActionBar());


		cancelButton.setOnClickListener(view -> {
			setResult(RESULT_CANCELED);
			finish();
		});

		this.snackBar = Snackbar.make(snackbarCoordinator, R.string.location_disabled, Snackbar.LENGTH_INDEFINITE);
		this.snackBar.setAction(R.string.enable, view -> {
			if (isLocationEnabledAndAllowed()) {
				updateUi();
			} else if (!hasLocationPermissions()) {
				permissionsManager = new PermissionsManager(this);
				permissionsManager.requestLocationPermissions(this);
			} else if (!isLocationEnabled()) {
				startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			}
		});
		ThemeHelper.fix(this.snackBar);

		shareButton.setOnClickListener(view -> {
			final Intent result = new Intent();

//			if (marker_fixed_to_loc && myLoc != null) {
//				result.putExtra("latitude", myLoc.getLatitude());
//				result.putExtra("longitude", myLoc.getLongitude());
//				result.putExtra("altitude", myLoc.getAltitude());
//				result.putExtra("accuracy", (int) myLoc.getAccuracy());
//			} else {
			if (mapboxMap.getLocationComponent() == null || !mapboxMap.getLocationComponent().isLocationComponentActivated()) {
				setResult(RESULT_CANCELED);
				finish();
			} else {
				final Location markerPoint = mapboxMap.getLocationComponent().getLastKnownLocation();
				if (markerPoint != null) {
					result.putExtra("latitude", markerPoint.getLatitude());
					result.putExtra("longitude", markerPoint.getLongitude());
				} else {
					setResult(RESULT_CANCELED);
					finish();
				}
			}
			//}

			setResult(RESULT_OK, result);
			finish();
		});

	}

	protected boolean isLocationEnabled() {
		try {
			final int locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
			return locationMode != Settings.Secure.LOCATION_MODE_OFF;
		} catch (final Settings.SettingNotFoundException e) {
			return false;
		}
	}

	@Override
	public void onMapReady(@NonNull MapboxMap mapboxMap) {
		this.mapboxMap = mapboxMap;
		mapboxMap.setStyle(Style.DARK, new Style.OnStyleLoaded() {
			@Override
			public void onStyleLoaded(@NonNull Style style) {
				enableLocationComponent(style);
			}
		});
	}

	//	@SuppressWarnings( {"MissingPermission"})
	private void enableLocationComponent(@NonNull Style loadedMapStyle) {
		// Check if permissions are enabled and if not request
		if (PermissionsManager.areLocationPermissionsGranted(this)) {

			// Create and customize the LocationComponent's options
			LocationComponentOptions customLocationComponentOptions = LocationComponentOptions.builder(this)
					.elevation(5)
					.accuracyAlpha(.6f)
					.accuracyColor(Color.RED)
					.foregroundDrawable(R.drawable.ic_attach_location)
					.build();

			// Get an instance of the component
			locationComponent = mapboxMap.getLocationComponent();

			LocationComponentActivationOptions locationComponentActivationOptions =
					LocationComponentActivationOptions.builder(this, loadedMapStyle)
							.locationComponentOptions(customLocationComponentOptions)
							.build();

			// Activate with options
			locationComponent.activateLocationComponent(locationComponentActivationOptions);

			locationComponent.setLocationComponentEnabled(true);

			// Set the component's camera mode
			locationComponent.setCameraMode(CameraMode.TRACKING);

			// Set the component's render mode
			locationComponent.setRenderMode(RenderMode.COMPASS);

			// Add the location icon click listener
			locationComponent.addOnLocationClickListener(this);

			// Add the camera tracking listener. Fires if the map camera is manually moved.
			locationComponent.addOnCameraTrackingChangedListener(this);

			findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (!isInTrackingMode) {
						isInTrackingMode = true;
						locationComponent.setCameraMode(CameraMode.TRACKING);
						locationComponent.zoomWhileTracking(16f);
						//Toast.makeText(LocationComponentOptionsActivity.this, getString(R.string.tracking_enabled),Toast.LENGTH_SHORT).show();
					} else {
						//Toast.makeText(LocationComponentOptionsActivity.this, getString(R.string.tracking_already_enabled),Toast.LENGTH_SHORT).show();
					}
				}
			});

		} else {
			permissionsManager = new PermissionsManager(this);
			permissionsManager.requestLocationPermissions(this);
		}
	}

	@SuppressWarnings( {"MissingPermission"})
	@Override
	public void onLocationComponentClick() {
		if (locationComponent.getLastKnownLocation() != null) {
//			Toast.makeText(this,
//					locationComponent.getLastKnownLocation().getLatitude() + " " +
//					locationComponent.getLastKnownLocation().getLongitude(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onCameraTrackingDismissed() {
		isInTrackingMode = false;
	}

	@Override
	public void onCameraTrackingChanged(int currentMode) {
		// Empty on purpose
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
		updateUi();
	}

	@Override
	public void onExplanationNeeded(List<String> permissionsToExplain) {
		//Toast.makeText(this, R.string.permission_need_req, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onPermissionResult(boolean granted) {
		if (granted) {
			mapboxMap.getStyle(new Style.OnStyleLoaded() {
				@Override
				public void onStyleLoaded(@NonNull Style style) {
					enableLocationComponent(style);
				}
			});
		} else {
			//Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
			finish();
		}

		updateUi();
	}

	protected boolean hasLocationPermissions() {
		return (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED );
	}

	@Override
	protected void refreshUiReal() {

	}

	@SuppressWarnings( {"MissingPermission"})
	protected void onStart() {
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
	void onBackendConnected() {

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}

	private boolean isLocationEnabledAndAllowed() {
		return (this.hasLocationPermissions());
	}


	protected void updateUi() {
		if (noAskAgain || isLocationEnabledAndAllowed()) {
			this.snackBar.dismiss();
		} else {
			this.snackBar.show();
		}

	}
}