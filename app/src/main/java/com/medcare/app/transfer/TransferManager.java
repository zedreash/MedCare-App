package com.medcare.app.transfer;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.medcare.app.R;
import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.utils.DataTransfer;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class TransferManager {
    public static final String SERVICE_ID = "com.medcare.app.transfer";
    private static final int REQUEST_CODE = 700;
    public static final int REQUEST_ENABLE_BT = 701;
    private static final int PENDING_NONE = 0;
    private static final int PENDING_SEND = 1;
    private static final int PENDING_RECEIVE = 2;

    private static int pendingOperation = PENDING_NONE;
    private static Listener pendingListener;

    public interface Listener {
        void onStatus(String message);
        void onTransferDone(DataTransfer.ImportResult result);
    }

    private static ConnectionsClient client;
    private static boolean advertising = false;
    private static boolean discovering = false;
    private static String connectedEndpoint;

    private TransferManager() {}

    public static void startSending(Fragment fragment, Listener listener) {
        if (!ensurePermissions(fragment)) return;
        if (!isLocationEnabled(fragment.requireContext())) {
            notifyStatus(fragment, listener, R.string.transfer_location_required);
            return;
        }
        if (!ensureBluetooth(fragment, listener, PENDING_SEND)) return;
        client = Nearby.getConnectionsClient(fragment.requireContext());
        advertising = true;
        client.startAdvertising(
                "MedCare-Transfer", SERVICE_ID,
                senderLifecycle(fragment, listener),
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build())
                .addOnFailureListener(e -> {
                    advertising = false;
                    android.util.Log.w("TransferManager", "startAdvertising failed", e);
                    notifyStatus(fragment, listener, R.string.transfer_failed);
                });
        notifyStatus(fragment, listener, R.string.transfer_advertising);
    }

    public static void startReceiving(Fragment fragment, Listener listener) {
        if (!ensurePermissions(fragment)) return;
        if (!isLocationEnabled(fragment.requireContext())) {
            notifyStatus(fragment, listener, R.string.transfer_location_required);
            return;
        }
        if (!ensureBluetooth(fragment, listener, PENDING_RECEIVE)) return;
        client = Nearby.getConnectionsClient(fragment.requireContext());
        discovering = true;
        client.startDiscovery(
                SERVICE_ID,
                receiverDiscovery(fragment, listener),
                new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build())
                .addOnFailureListener(e -> {
                    discovering = false;
                    android.util.Log.w("TransferManager", "startDiscovery failed", e);
                    notifyStatus(fragment, listener, R.string.transfer_failed);
                });
        notifyStatus(fragment, listener, R.string.transfer_discovering);
    }

    private static boolean ensureBluetooth(Fragment fragment, Listener listener, int op) {
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        if (ba != null && ba.isEnabled()) return true;
        try {
            pendingOperation = op;
            pendingListener = listener;
            fragment.startActivityForResult(
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        } catch (Exception e) {
            pendingOperation = PENDING_NONE;
            pendingListener = null;
            notifyStatus(fragment, listener, R.string.transfer_bluetooth_required);
        }
        return false;
    }

    public static void onBluetoothEnableResult(Fragment fragment, boolean enabled) {
        int op = pendingOperation;
        Listener listener = pendingListener;
        pendingOperation = PENDING_NONE;
        pendingListener = null;
        if (!enabled) {
            notifyStatus(fragment, listener, R.string.transfer_bluetooth_required);
            return;
        }
        if (op == PENDING_SEND) {
            startSending(fragment, listener);
        } else if (op == PENDING_RECEIVE) {
            startReceiving(fragment, listener);
        }
    }

    private static boolean isLocationEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        if (Build.VERSION.SDK_INT >= 28) {
            return lm.isLocationEnabled();
        }
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public static void stop() {
        if (client == null) return;
        try {
            if (advertising) { client.stopAdvertising(); advertising = false; }
            if (discovering) { client.stopDiscovery(); discovering = false; }
            if (connectedEndpoint != null) {
                client.disconnectFromEndpoint(connectedEndpoint);
                connectedEndpoint = null;
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean isActive() {
        return advertising || discovering;
    }

    private static boolean ensurePermissions(Fragment fragment) {
        List<String> missing = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(fragment.requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= 31) {
            if (ContextCompat.checkSelfPermission(fragment.requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                missing.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(fragment.requireContext(),
                    Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                missing.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
            if (ContextCompat.checkSelfPermission(fragment.requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                missing.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(fragment.requireContext(),
                Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }
        if (!missing.isEmpty()) {
            fragment.requestPermissions(missing.toArray(new String[0]), REQUEST_CODE);
            return false;
        }
        return true;
    }

    private static void notifyStatus(Fragment fragment, Listener listener, int res) {
        if (fragment.isAdded()) {
            listener.onStatus(fragment.getString(res));
        }
    }

    private static void showConfirmDialog(Fragment fragment, String token,
                                          Runnable accept, Runnable reject) {
        String message = fragment.getString(R.string.transfer_token_message, token)
                + "\n\n" + fragment.getString(R.string.transfer_scope_note);
        new AlertDialog.Builder(fragment.requireContext())
                .setTitle(R.string.transfer_confirm)
                .setMessage(message)
                .setPositiveButton(R.string.confirm, (d, w) -> accept.run())
                .setNegativeButton(R.string.cancel, (d, w) -> reject.run())
                .show();
    }

    private static ConnectionLifecycleCallback senderLifecycle(Fragment fragment, Listener listener) {
        return new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo info) {
                connectedEndpoint = endpointId;
                showConfirmDialog(fragment, info.getAuthenticationToken(),
                        () -> {
                            if (client != null) client.acceptConnection(endpointId, senderPayload());
                        },
                        () -> {
                            if (client != null) client.rejectConnection(endpointId);
                            connectedEndpoint = null;
                        });
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution resolution) {
                if (resolution.getStatus().isSuccess()) {
                    notifyStatus(fragment, listener, R.string.transfer_connected);
                    sendSnapshot(fragment, endpointId, listener);
                } else {
                    notifyStatus(fragment, listener, R.string.transfer_connection_failed);
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                connectedEndpoint = null;
            }
        };
    }

    private static File lastSentFile;

    private static PayloadCallback senderPayload() {
        return new PayloadCallback() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS && lastSentFile != null) {
                    lastSentFile.delete();
                    lastSentFile = null;
                }
            }
        };
    }

    private static void sendSnapshot(Fragment fragment, String endpointId, Listener listener) {
        AppDatabase.getExecutor().execute(() -> {
            try {
                JSONObject snapshot = DataTransfer.buildSnapshot(fragment.requireContext());
                byte[] bytes = snapshot.toString().getBytes("UTF-8");
                File temp = new File(fragment.requireContext().getCacheDir(),
                        "transfer_snapshot_" + System.currentTimeMillis() + ".json");
                try (FileOutputStream fos = new FileOutputStream(temp)) {
                    fos.write(bytes);
                }
                final Payload payload = Payload.fromFile(temp);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (client != null) {
                        lastSentFile = temp;
                        client.sendPayload(endpointId, payload);
                        notifyStatus(fragment, listener, R.string.transfer_sent);
                    }
                });
            } catch (Exception e) {
                android.util.Log.w("TransferManager", "sendSnapshot failed", e);
                new Handler(Looper.getMainLooper()).post(() ->
                        notifyStatus(fragment, listener, R.string.transfer_failed));
            }
        });
    }

    private static EndpointDiscoveryCallback receiverDiscovery(Fragment fragment, Listener listener) {
        return new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                if (client != null) {
                    client.requestConnection("MedCare-Transfer", endpointId,
                            receiverLifecycle(fragment, listener));
                }
            }

            @Override
            public void onEndpointLost(String endpointId) {
            }
        };
    }

    private static ConnectionLifecycleCallback receiverLifecycle(Fragment fragment, Listener listener) {
        return new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo info) {
                connectedEndpoint = endpointId;
                showConfirmDialog(fragment, info.getAuthenticationToken(),
                        () -> {
                            if (client != null) client.acceptConnection(endpointId,
                                    receiverPayload(fragment, listener));
                        },
                        () -> {
                            if (client != null) client.rejectConnection(endpointId);
                            connectedEndpoint = null;
                        });
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution resolution) {
                if (resolution.getStatus().isSuccess()) {
                    notifyStatus(fragment, listener, R.string.transfer_connected);
                } else {
                    notifyStatus(fragment, listener, R.string.transfer_connection_failed);
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                connectedEndpoint = null;
            }
        };
    }

    private static PayloadCallback receiverPayload(Fragment fragment, Listener listener) {
        return new PayloadCallback() {
            private Payload.File pendingFile;

            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                if (payload.getType() == Payload.Type.BYTES) {
                    processSnapshot(fragment, listener, payload.asBytes());
                } else if (payload.getType() == Payload.Type.FILE) {
                    pendingFile = payload.asFile();
                }
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS && pendingFile != null) {
                    final Payload.File filePayload = pendingFile;
                    pendingFile = null;
                    AppDatabase.getExecutor().execute(() -> {
                        try {
                            byte[] bytes = readUri(fragment.requireContext(), filePayload.asUri());
                            processSnapshot(fragment, listener, bytes);
                        } catch (Exception e) {
                            android.util.Log.w("TransferManager", "read file payload failed", e);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                stop();
                                listener.onTransferDone(null);
                            });
                        }
                    });
                }
            }
        };
    }

    private static void processSnapshot(Fragment fragment, Listener listener, byte[] bytes) {
        AppDatabase.getExecutor().execute(() -> {
            DataTransfer.ImportResult result = null;
            try {
                JSONObject root = new JSONObject(new String(bytes, "UTF-8"));
                result = DataTransfer.restoreSnapshot(fragment.requireContext(), root);
            } catch (Exception ignored) {
            }
            final DataTransfer.ImportResult finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> {
                stop();
                listener.onTransferDone(finalResult);
            });
        });
    }

    private static byte[] readUri(Context context, Uri uri) throws Exception {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalArgumentException("cannot open");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }
            return bos.toByteArray();
        }
    }
}