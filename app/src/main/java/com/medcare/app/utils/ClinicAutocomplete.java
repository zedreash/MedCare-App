package com.medcare.app.utils;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClinicAutocomplete {

    public interface Listener {
        void onClinicPicked(ClinicResult clinic);
        void onClinicBlur();
    }

    public static class ClinicResult {
        public final String name;
        public final Double lat;
        public final Double lng;
        final String placeId;

        public ClinicResult(String name, Double lat, Double lng) {
            this(name, lat, lng, null);
        }

        ClinicResult(String name, Double lat, Double lng, String placeId) {
            this.name = name;
            this.lat = lat;
            this.lng = lng;
            this.placeId = placeId;
        }
    }

    public static class PopularClinic {
        public final String en;
        public final String ar;
        public final String he;
        public final double lat;
        public final double lng;

        PopularClinic(String en, String ar, String he, double lat, double lng) {
            this.en = en;
            this.ar = ar;
            this.he = he;
            this.lat = lat;
            this.lng = lng;
        }
    }

    public static final LatLngBounds REGION_BOUNDS =
            new LatLngBounds(new LatLng(29.5, 34.2), new LatLng(33.3, 35.9));

    public static final PopularClinic[] POPULAR_CLINICS = {
            new PopularClinic("Palestine Medical Complex \u2014 Ramallah",
                    "\u0627\u0644\u0645\u062c\u0645\u0639 \u0627\u0644\u0637\u0628\u064a \u0627\u0644\u0641\u0644\u0633\u0637\u064a\u0646\u064a \u2014 \u0631\u0627\u0645 \u0627\u0644\u0644\u0647",
                    "\u05d4\u05de\u05ea\u05d7\u05dd \u05d4\u05e8\u05e4\u05d5\u05d0\u05d9 \u05d4\u05e4\u05dc\u05e1\u05d8\u05d9\u05e0\u05d9 \u2014 \u05e8\u05de\u05d0\u05dc\u05dc\u05d4",
                    31.8994, 35.2057),
            new PopularClinic("Al-Ahli Hospital \u2014 Hebron",
                    "\u0645\u0633\u062a\u0634\u0641\u0649 \u0627\u0644\u0623\u0647\u0644\u064a \u2014 \u0627\u0644\u062e\u0644\u064a\u0644",
                    "\u05d1\u05d9\u05ea \u05d4\u05d7\u05d5\u05dc\u05d9\u05dd \u05d0\u05dc-\u05d0\u05d4\u05dc\u05d9 \u2014 \u05d7\u05d1\u05e8\u05d5\u05df",
                    31.5567, 35.0834),
            new PopularClinic("Arab Specialized Hospital \u2014 Nablus",
                    "\u0627\u0644\u0645\u0633\u062a\u0634\u0641\u0649 \u0627\u0644\u0639\u0631\u0628\u064a \u0627\u0644\u062a\u062e\u0635\u0635\u064a \u2014 \u0646\u0627\u0628\u0644\u0633",
                    "\u05d1\u05d9\u05ea \u05d4\u05d7\u05d5\u05dc\u05d9\u05dd \u05d4\u05e2\u05e8\u05d1\u05d9 \u05d4\u05de\u05d9\u05d5\u05d7\u05d3 \u2014 \u05e9\u05db\u05dd",
                    32.2247, 35.2400),
            new PopularClinic("An-Najah National University Hospital \u2014 Nablus",
                    "\u0645\u0633\u062a\u0634\u0641\u0649 \u062c\u0627\u0645\u0639\u0629 \u0627\u0644\u0646\u062c\u0627\u062d \u0627\u0644\u0648\u0637\u0646\u064a\u0629 \u2014 \u0646\u0627\u0628\u0644\u0633",
                    "\u05d1\u05d9\u05ea \u05d4\u05d7\u05d5\u05dc\u05d9\u05dd \u05e9\u05dc \u05d0\u05d5\u05e0\u05d9\u05d1\u05e8\u05e1\u05d9\u05d8\u05ea \u05d0-\u05e0\u05d2'\u05d0\u05d7 \u2014 \u05e9\u05db\u05dd",
                    32.2404, 35.2423),
            new PopularClinic("Beit Jala Hospital \u2014 Bethlehem",
                    "\u0645\u0633\u062a\u0634\u0641\u0649 \u0628\u064a\u062a \u062c\u0627\u0644\u0627 \u2014 \u0628\u064a\u062a \u0644\u062d\u0645",
                    "\u05d1\u05d9\u05ea \u05d4\u05d7\u05d5\u05dc\u05d9\u05dd \u05d1\u05d9\u05ea \u05d2'\u05d0\u05dc\u05d0 \u2014 \u05d1\u05d9\u05ea \u05dc\u05d7\u05dd",
                    31.7113, 35.1975),
            new PopularClinic("Khalil Suleiman Hospital \u2014 Jenin",
                    "\u0645\u0633\u062a\u0634\u0641\u0649 \u062e\u0644\u064a\u0644 \u0633\u0644\u064a\u0645\u0627\u0646 \u2014 \u062c\u0646\u064a\u0646",
                    "\u05d1\u05d9\u05ea \u05d4\u05d7\u05d5\u05dc\u05d9\u05dd \u05d7'\u05dc\u05d9\u05dc \u05e1\u05d5\u05dc\u05d9\u05de\u05d0\u05df \u2014 \u05d2'\u05e0\u05d9\u05df",
                    32.4616, 35.2924),
            new PopularClinic("Istishari Arab Hospital \u2014 Ramallah",
                    "\u0645\u0633\u062a\u0634\u0641\u0649 \u0627\u0644\u0627\u0633\u062a\u0634\u0627\u0631\u064a \u0627\u0644\u0639\u0631\u0628\u064a \u2014 \u0631\u0627\u0645 \u0627\u0644\u0644\u0647",
                    "\u05d1\u05d9\u05ea \u05d4\u05d7\u05d5\u05dc\u05d9\u05dd \u05d0\u05d9\u05e1\u05ea\u05d9\u05e9\u05d0\u05e8\u05d9 \u05d4\u05e2\u05e8\u05d1\u05d9 \u2014 \u05e8\u05de\u05d0\u05dc\u05dc\u05d4",
                    31.9337, 35.1635),
            new PopularClinic("Al-Makassed Islamic Charitable Hospital \u2014 Jerusalem",
                    "\u0645\u0633\u062a\u0634\u0641\u0649 \u0627\u0644\u0645\u0642\u0627\u0635\u062f \u0627\u0644\u062e\u064a\u0631\u064a\u0629 \u0627\u0644\u0625\u0633\u0644\u0627\u0645\u064a\u0629 \u2014 \u0627\u0644\u0642\u062f\u0633",
                    "\u05d1\u05d9\u05ea \u05d4\u05d7\u05d5\u05dc\u05d9\u05dd \u05d0\u05dc-\u05de\u05e7\u05d0\u05e1\u05d3 \u2014 \u05d9\u05e8\u05d5\u05e9\u05dc\u05d9\u05dd",
                    31.7809, 35.2459),
            new PopularClinic("Rafidia Surgical Hospital \u2014 Nablus",
                    "\u0645\u0633\u062a\u0634\u0641\u0649 \u0631\u0641\u064a\u062f\u064a\u0627 \u0627\u0644\u062c\u0631\u0627\u062d\u064a \u2014 \u0646\u0627\u0628\u0644\u0633",
                    "\u05d1\u05d9\u05ea \u05d4\u05d7\u05d5\u05dc\u05d9\u05dd \u05e8\u05e4\u05d9\u05d3\u05d9\u05d4 \u2014 \u05e9\u05db\u05dd",
                    32.2252, 35.2437),
            new PopularClinic("Al-Shifa Medical Complex \u2014 Gaza",
                    "\u0645\u062c\u0645\u0639 \u0627\u0644\u0634\u0641\u0627\u0621 \u0627\u0644\u0637\u0628\u064a \u2014 \u063a\u0632\u0629",
                    "\u05d4\u05de\u05ea\u05d7\u05dd \u05d4\u05e8\u05e4\u05d5\u05d0\u05d9 \u05d0-\u05e9\u05d9\u05e4\u05d0 \u2014 \u05e2\u05d6\u05d4",
                    31.5244, 34.4429)
    };

    private final Fragment fragment;
    private final TextInputLayout layout;
    private final TextInputEditText input;
    private final View anchor;
    private final Listener listener;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private final int locationRequestCode;

    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private ListPopupWindow popup;
    private ArrayAdapter<String> adapter;
    private List<ClinicResult> itemResults = new ArrayList<>();
    private boolean ignoreTextChanges = false;
    private Runnable debounceRunnable;
    private Location lastLocation;
    private boolean locationRequested = false;
    private ClinicResult lastSelection;

    public ClinicAutocomplete(Fragment fragment, TextInputLayout layout,
                              int locationRequestCode, Listener listener) {
        this.fragment = fragment;
        this.layout = layout;
        this.input = (TextInputEditText) layout.getEditText();
        this.anchor = layout;
        this.listener = listener;
        this.locationRequestCode = locationRequestCode;
    }

    public void attach() {
        initPlaces();
        sessionToken = AutocompleteSessionToken.newInstance();
        adapter = new ArrayAdapter<>(fragment.requireContext(), android.R.layout.simple_list_item_1);
        popup = new ListPopupWindow(fragment.requireContext());
        popup.setAnchorView(anchor);
        popup.setAdapter(adapter);
        popup.setModal(false);
        popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        popup.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= itemResults.size()) return;
            ClinicResult picked = itemResults.get(position);
            ignoreTextChanges = true;
            input.setText(picked.name);
            input.setSelection(picked.name.length());
            ignoreTextChanges = false;
            popup.dismiss();
            resolveSelection(picked);
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (ignoreTextChanges) return;
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> fetchSuggestions(s.toString());
                debounceHandler.postDelayed(debounceRunnable, 400);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (input.getText() == null || input.getText().toString().trim().isEmpty()) {
                    suggestOnFocus();
                }
            } else {
                popup.dismiss();
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                if (listener != null) listener.onClinicBlur();
            }
        });
    }

    public void clear() {
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
        if (popup != null) popup.dismiss();
    }

    public void setText(String value) {
        if (input == null) return;
        ignoreTextChanges = true;
        input.setText(value == null ? "" : value);
        ignoreTextChanges = false;
    }

    public void setPreselected(String name, Double lat, Double lng) {
        lastSelection = (name == null || name.isEmpty()) ? null
                : new ClinicResult(name, lat, lng);
        setText(name);
    }

    public void onLocationPermissionResult(boolean granted) {
        if (granted) {
            resolveLocation(this::suggestOnFocus);
        } else {
            showPopular();
        }
    }

    @Nullable
    public ClinicResult selectionForText(String text) {
        if (text == null) return null;
        if (lastSelection != null && lastSelection.name != null
                && text.trim().equalsIgnoreCase(lastSelection.name)) {
            return lastSelection;
        }
        double[] coords = popularCoordsFor(text);
        if (coords != null) {
            return new ClinicResult(text.trim(), coords[0], coords[1]);
        }
        return null;
    }

    @Nullable
    public static double[] popularCoordsFor(String name) {
        if (name == null) return null;
        String lower = name.trim().toLowerCase();
        if (lower.isEmpty()) return null;
        for (PopularClinic c : POPULAR_CLINICS) {
            String enLower = c.en.toLowerCase();
            String ar = c.ar;
            String he = c.he;
            if (lower.equals(enLower) || lower.equals(ar) || lower.equals(he)) {
                return new double[]{c.lat, c.lng};
            }
            if ((enLower.startsWith(lower) || lower.startsWith(enLower)) && lower.length() >= 3) {
                return new double[]{c.lat, c.lng};
            }
            if (lower.startsWith(ar) || lower.startsWith(he)) {
                return new double[]{c.lat, c.lng};
            }
        }
        return null;
    }

    private void resolveSelection(ClinicResult picked) {
        if (picked.placeId == null || placesClient == null) {
            lastSelection = picked;
            if (listener != null) listener.onClinicPicked(picked);
            return;
        }
        List<Place.Field> fields = Arrays.asList(Place.Field.LAT_LNG);
        FetchPlaceRequest request = FetchPlaceRequest.builder(picked.placeId, fields).build();
        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    if (!fragment.isAdded()) return;
                    Place place = response.getPlace();
                    ClinicResult resolved = new ClinicResult(picked.name,
                            place.getLatLng() != null ? place.getLatLng().latitude : null,
                            place.getLatLng() != null ? place.getLatLng().longitude : null);
                    lastSelection = resolved;
                    if (listener != null) listener.onClinicPicked(resolved);
                })
                .addOnFailureListener(e -> {
                    lastSelection = picked;
                    if (listener != null) listener.onClinicPicked(picked);
                });
    }

    private void initPlaces() {
        try {
            if (!Places.isInitialized()) {
                ApplicationInfo appInfo = fragment.requireContext().getPackageManager()
                        .getApplicationInfo(fragment.requireContext().getPackageName(),
                                PackageManager.GET_META_DATA);
                String apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY");
                if (apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("${")) {
                    Places.initialize(fragment.requireContext().getApplicationContext(), apiKey);
                } else {
                    return;
                }
            }
            placesClient = Places.createClient(fragment.requireContext());
        } catch (Exception ignored) {
        }
    }

    private void suggestOnFocus() {
        if (!fragment.isAdded()) return;
        if (hasLocationPermission()) {
            resolveLocation(() -> {
                if (!fragment.isAdded()) return;
                if (isInRegion()) {
                    fetchSuggestions("clinic");
                } else {
                    showPopular();
                }
            });
        } else if (!locationRequested) {
            locationRequested = true;
            fragment.requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, locationRequestCode);
        } else {
            showPopular();
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(fragment.requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void resolveLocation(@NonNull Runnable onDone) {
        if (lastLocation != null) {
            onDone.run();
            return;
        }
        if (!hasLocationPermission()) {
            onDone.run();
            return;
        }
        FusedLocationProviderClient client = LocationServices
                .getFusedLocationProviderClient(fragment.requireContext());
        client.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        lastLocation = location;
                    }
                    if (fragment.isAdded()) onDone.run();
                })
                .addOnFailureListener(e -> {
                    if (fragment.isAdded()) onDone.run();
                });
    }

    private boolean isInRegion() {
        if (lastLocation == null) return false;
        return REGION_BOUNDS.contains(new LatLng(
                lastLocation.getLatitude(), lastLocation.getLongitude()));
    }

    private void fetchSuggestions(String query) {
        if (!fragment.isAdded()) return;
        String q = query == null ? "" : query.trim();
        List<ClinicResult> curatedMatches = curatedMatches(q);

        if (placesClient == null) {
            if (q.isEmpty()) {
                showPopular();
            } else if (!curatedMatches.isEmpty()) {
                showItems(curatedMatches);
            } else {
                showPopular();
            }
            return;
        }
        if (q.isEmpty()) {
            if (isInRegion()) {
                fetchSuggestions("clinic");
            } else {
                showPopular();
            }
            return;
        }

        FindAutocompletePredictionsRequest.Builder builder = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setQuery(q)
                .setCountries(Arrays.asList("IL", "PS"));
        if (isInRegion() && lastLocation != null) {
            builder.setLocationBias(RectangularBounds.newInstance(
                    new LatLng(lastLocation.getLatitude() - 0.15, lastLocation.getLongitude() - 0.15),
                    new LatLng(lastLocation.getLatitude() + 0.15, lastLocation.getLongitude() + 0.15)));
        }
        placesClient.findAutocompletePredictions(builder.build())
                .addOnSuccessListener(response -> {
                    if (!fragment.isAdded() || popup == null) return;
                    List<ClinicResult> items = new ArrayList<>(curatedMatches);
                    for (AutocompletePrediction p : response.getAutocompletePredictions()) {
                        items.add(new ClinicResult(p.getPrimaryText(null).toString(),
                                null, null, p.getPlaceId()));
                    }
                    if (!items.isEmpty()) {
                        showItems(items);
                    } else {
                        popup.dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!fragment.isAdded() || popup == null) return;
                    if (!curatedMatches.isEmpty()) {
                        showItems(curatedMatches);
                    } else if (q.isEmpty()) {
                        showPopular();
                    } else {
                        popup.dismiss();
                    }
                });
    }

    private List<ClinicResult> curatedMatches(String query) {
        List<ClinicResult> matches = new ArrayList<>();
        if (query == null || query.isEmpty()) return matches;
        String lower = query.toLowerCase();
        for (PopularClinic c : POPULAR_CLINICS) {
            if (c.en.toLowerCase().contains(lower)
                    || c.ar.toLowerCase().contains(lower)
                    || c.he.toLowerCase().contains(lower)) {
                matches.add(new ClinicResult(localizedName(c), c.lat, c.lng));
            }
        }
        return matches;
    }

    private void showItems(List<ClinicResult> items) {
        if (!fragment.isAdded() || popup == null) return;
        itemResults = new ArrayList<>(items);
        adapter.clear();
        for (ClinicResult r : items) {
            adapter.add(r.name);
        }
        popup.show();
    }

    private void showPopular() {
        if (!fragment.isAdded() || popup == null) return;
        List<ClinicResult> items = new ArrayList<>();
        for (PopularClinic c : POPULAR_CLINICS) {
            items.add(new ClinicResult(localizedName(c), c.lat, c.lng));
        }
        showItems(items);
    }

    private String currentLang() {
        String lang = new PreferencesManager(fragment.requireContext()).getLanguage();
        if ("ar".equals(lang)) return "ar";
        if ("he".equals(lang) || "iw".equals(lang)) return "he";
        if ("en".equals(lang)) return "en";
        String sys = fragment.requireContext().getResources()
                .getConfiguration().locale.getLanguage();
        if ("ar".equals(sys)) return "ar";
        if ("he".equals(sys) || "iw".equals(sys)) return "he";
        return "en";
    }

    private String localizedName(PopularClinic c) {
        if ("ar".equals(currentLang())) return c.ar;
        if ("he".equals(currentLang())) return c.he;
        return c.en;
    }
}