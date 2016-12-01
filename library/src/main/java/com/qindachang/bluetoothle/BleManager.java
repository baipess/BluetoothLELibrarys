package com.qindachang.bluetoothle;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;


class BleManager {

    private static final String TAG = BleManager.class.getSimpleName();

    private int REQUEST_PERMISSION_REQ_CODE = 888;

    private boolean isStopScanAfterConnected;
    private boolean isScanning;
    private boolean mConnected;
    private boolean mServiceDiscovered;
    private boolean mRetryConnectEnable;
    private int mRetryConnectCount = 1;
    private int connectTimeoutMillis;
    private int serviceTimeoutMillis;

    private boolean mAutoConnect;
    private BluetoothDevice mBluetoothDevice;

    private Context mContext;

    private BluetoothGatt mBluetoothGatt;

    private OnLeScanListener mOnLeScanListener;
    private OnLeConnectListener mOnLeConnectListener;
    private OnLeNotificationListener mOnLeNotificationListener;
    private OnLeWriteCharacteristicListener mOnLeWriteCharacteristicListener;
    private OnLeReadCharacteristicListener mOnLeReadCharacteristicListener;

    private RequestQueue mRequestQueue = new RequestQueue();
    private List<Map<Object, OnLeScanListener>> scanListenerList = new ArrayList<>();
    private List<Map<Object, OnLeConnectListener>> connectListenerList = new ArrayList<>();
    private List<Map<Object, OnLeWriteCharacteristicListener>> writeCharacteristicListenerList = new ArrayList<>();
    private List<Map<Object, OnLeReadCharacteristicListener>> readCharacteristicListenerList = new ArrayList<>();
    private List<Map<Object, OnLeNotificationListener>> notificationListenerList = new ArrayList<>();

    private Handler mHandler = new Handler(Looper.getMainLooper());

    BleManager(Context context) {
        mContext = context;
    }

    boolean isBluetoothOpen() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter.isEnabled();
    }

    boolean enableBluetooth(Activity activity) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "false. your device does not support bluetooth. ");
            return false;
        }
        if (bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "false. your device has been turn on bluetooth.");
            return false;
        }
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivity(intent);
        return true;
    }

    boolean disableBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
            return true;
        } else {
            Log.d(TAG, "false. your device has been turn off Bluetooth.");
            return false;
        }
    }

    boolean clearDeviceCache() {
        if (mBluetoothGatt == null) {
            Log.e(TAG, "please connected bluetooth then clear cache.");
            return false;
        }
        try {
            Method e = BluetoothGatt.class.getMethod("refresh", new Class[0]);
            if (e != null) {
                boolean success = ((Boolean) e.invoke(mBluetoothGatt, new Object[0])).booleanValue();
                Log.i(TAG, "refresh Device Cache: " + success);
                return success;
            }
        } catch (Exception exception) {
            Log.e(TAG, "An exception occured while refreshing device", exception);
        }

        return false;
    }

    void addScanLeListener(Object tag, OnLeScanListener onLeScanListener) {
        if (scanListenerList.size() > 0) {
            boolean canAdd = true;
            for (Map<Object, OnLeScanListener> map : scanListenerList) {
                if (map.containsKey(tag)) {
                    canAdd = false;
                }
            }
            if (canAdd) {
                Map<Object, OnLeScanListener> map2 = new HashMap<>();
                map2.put(tag, onLeScanListener);
                scanListenerList.add(map2);
            }
        } else {
            Map<Object, OnLeScanListener> map3 = new HashMap<>();
            map3.put(tag, onLeScanListener);
            scanListenerList.add(map3);
        }
    }

    void setOnLeScanListener(OnLeScanListener onLeScanListener) {
        mOnLeScanListener = onLeScanListener;
    }

    void scan(Activity activity, String filterDeviceName, String filterDeviceAddress, UUID uFilerServiceUUID,
              int scanPeriod, int reportDelayMillis) {
        Log.d(TAG, "bluetooth le scanning...");
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                return;
            }
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSION_REQ_CODE);
            return;
        }
        BluetoothLeScannerCompat scannerCompat = BluetoothLeScannerCompat.getScanner();
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(reportDelayMillis)
                .setUseHardwareBatchingIfSupported(false)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        if (filterDeviceName != null) {
            ScanFilter builder = new ScanFilter.Builder().setDeviceName(filterDeviceName).build();
            filters.add(builder);
        }
        if (filterDeviceAddress != null) {
            ScanFilter builder = new ScanFilter.Builder().setDeviceAddress(filterDeviceAddress).build();
            filters.add(builder);
        }
        if (uFilerServiceUUID != null) {
            ScanFilter builder = new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString(uFilerServiceUUID.toString())).build();
            filters.add(builder);
        }
        scannerCompat.startScan(filters, scanSettings, scanCallback);

        int SCAN_DURATION = scanPeriod;
        if (SCAN_DURATION == 0) {
            SCAN_DURATION = 10000;
        }
        isScanning = true;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isScanning) {
                    stopScan();
                }
            }
        }, SCAN_DURATION);
    }

    void stopScan() {
        if (isScanning) {
            final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(scanCallback);
            isScanning = false;
            for (Map<Object, OnLeScanListener> map : scanListenerList) {
                for (Map.Entry<Object, OnLeScanListener> entry : map.entrySet()) {
                    entry.getValue().onScanCompleted();
                }
            }
            if (mOnLeScanListener != null) {
                mOnLeScanListener.onScanCompleted();
            }
            Log.d(TAG, "bluetooth le scan has stop.");
        }
    }

    boolean scanning() {
        return isScanning;
    }

    void setStopScanAfterConnected(boolean set) {
        isStopScanAfterConnected = set;
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            for (Map<Object, OnLeScanListener> map : scanListenerList) {
                for (Map.Entry<Object, OnLeScanListener> entry : map.entrySet()) {
                    entry.getValue().onScanResult(result.getDevice(), result.getRssi(), result.getScanRecord());
                }
            }
            if (mOnLeScanListener != null) {
                mOnLeScanListener.onScanResult(result.getDevice(), result.getRssi(), result.getScanRecord());
            }
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            for (Map<Object, OnLeScanListener> map : scanListenerList) {
                for (Map.Entry<Object, OnLeScanListener> entry : map.entrySet()) {
                    entry.getValue().onBatchScanResults(results);
                }
            }
            if (mOnLeScanListener != null) {
                mOnLeScanListener.onBatchScanResults(results);
            }
        }

        @Override
        public void onScanFailed(final int errorCode) {
            for (Map<Object, OnLeScanListener> map : scanListenerList) {
                for (Map.Entry<Object, OnLeScanListener> entry : map.entrySet()) {
                    entry.getValue().onScanFailed(errorCode);
                }
            }
            if (mOnLeScanListener != null) {
                mOnLeScanListener.onScanFailed(errorCode);
            }
        }
    };

    void setRetryConnectEnable(boolean retryConnectEnable) {
        mRetryConnectEnable = retryConnectEnable;
    }

    void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    void setServiceTimeoutMillis(int serviceTimeoutMillis) {
        this.serviceTimeoutMillis = serviceTimeoutMillis;
    }

    void setRetryConnectCount(int retryConnectCount) {
        mRetryConnectCount = retryConnectCount;
    }

    boolean connect(boolean autoConnect, final BluetoothDevice device) {
        mAutoConnect = autoConnect;
        mBluetoothDevice = device;
        if (mConnected) {
            Log.d(TAG, "Bluetooth has been connected. connect false.");
            for (Map<Object, OnLeConnectListener> map : connectListenerList) {
                for (Map.Entry<Object, OnLeConnectListener> entry : map.entrySet()) {
                    entry.getValue().onDeviceConnectFail();
                }
            }
            if (mOnLeConnectListener != null) {
                mOnLeConnectListener.onDeviceConnectFail();
            }
            return false;
        }
        if (mBluetoothGatt != null) {
            Log.d(TAG, "The BluetoothGatt already exist, set it close() and null.");
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            mConnected = false;
        }
        Log.d(TAG, "create new device connection for BluetoothGatt. ");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(mContext, autoConnect, mGattCallback, TRANSPORT_LE);
        } else {
            mBluetoothGatt = device.connectGatt(mContext, autoConnect, mGattCallback);
        }
        for (Map<Object, OnLeConnectListener> map : connectListenerList) {
            for (Map.Entry<Object, OnLeConnectListener> entry : map.entrySet()) {
                entry.getValue().onDeviceConnecting();
            }
        }
        if (mOnLeConnectListener != null) {
            mOnLeConnectListener.onDeviceConnecting();
        }

        checkConnected();

        return true;
    }

    private void checkConnected() {
        if (mRetryConnectEnable && mRetryConnectCount > 0 && connectTimeoutMillis > 0) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    boolean connected = getConnected();
                    if (!connected) {
                        connect(mAutoConnect, mBluetoothDevice);
                        mRetryConnectCount = mRetryConnectCount - 1;
                    }
                }
            }, connectTimeoutMillis);
        }
    }

    boolean getConnected() {
        return mConnected;
    }

    void setConnectListener(OnLeConnectListener onLeConnectListener) {
        mOnLeConnectListener = onLeConnectListener;
    }

    void addConnectListener(Object tag, OnLeConnectListener onLeConnectListener) {
        Map<Object, OnLeConnectListener> map = new HashMap<>();
        map.put(tag, onLeConnectListener);
        connectListenerList.add(map);
    }

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private boolean enableNotification(boolean enable, BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0)
            return false;
        gatt.setCharacteristicNotification(characteristic, enable);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }

    void enableNotificationQueue(boolean enable, UUID serviceUUID, UUID[] characteristicUUIDs) {
        BluetoothGattService service = mBluetoothGatt.getService(serviceUUID);
        for (UUID characteristicUUID : characteristicUUIDs) {
            mRequestQueue.addRequest(Request.newEnableNotificationsRequest(enable, service.getCharacteristic(characteristicUUID)));
        }
    }

    void setOnLeNotificationListener(OnLeNotificationListener onLeNotificationListener) {
        this.mOnLeNotificationListener = onLeNotificationListener;
    }

    void addNotificationListener(Object tag, OnLeNotificationListener onLeNotificationListener) {
        Map<Object, OnLeNotificationListener> map = new HashMap<>();
        map.put(tag, onLeNotificationListener);
        notificationListenerList.add(map);
    }

    void writeCharacteristicQueue(byte[] bytes, UUID serviceUUID, UUID characteristicUUID) {
        if (mBluetoothGatt == null || serviceUUID == null || characteristicUUID == null) {
            Log.d(TAG, "the bluetooth gatt or serviceUUID or characteristicUUID is null. ");
            return;
        }
        BluetoothGattService service = mBluetoothGatt.getService(serviceUUID);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
        characteristic.setValue(bytes);
        mRequestQueue.addRequest(Request.newWriteRequest(characteristic));


    }

    private boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;
        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0)
            return false;
        return gatt.writeCharacteristic(characteristic);
    }

    void setWriteCharacteristicListener(OnLeWriteCharacteristicListener onLeWriteCharacteristicListener) {
        mOnLeWriteCharacteristicListener = onLeWriteCharacteristicListener;
    }

    void addWriteCharacteristicListener(Object tag, OnLeWriteCharacteristicListener onLeWriteCharacteristicListener) {
        Map<Object, OnLeWriteCharacteristicListener> map = new HashMap<>();
        map.put(tag, onLeWriteCharacteristicListener);
        writeCharacteristicListenerList.add(map);
    }

    void readCharacteristicQueue(UUID serviceUUID, UUID characteristicUUID) {
        BluetoothGattService service = mBluetoothGatt.getService(serviceUUID);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
        mRequestQueue.addRequest(Request.newReadRequest(characteristic));
    }

    private boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;
        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) == 0)
            return false;
        return gatt.readCharacteristic(characteristic);
    }

    void setOnLeReadCharacteristicListener(OnLeReadCharacteristicListener onLeReadCharacteristicListener) {
        mOnLeReadCharacteristicListener = onLeReadCharacteristicListener;
    }

    void addReadCharacteristicListener(Object tag, OnLeReadCharacteristicListener onLeReadCharacteristicListener) {
        Map<Object, OnLeReadCharacteristicListener> map = new HashMap<>();
        map.put(tag, onLeReadCharacteristicListener);
        readCharacteristicListenerList.add(map);
    }

    void disconnect() {
        if (mConnected && mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mConnected = false;
            mServiceDiscovered = false;
        }
    }

    void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            mConnected = false;
            mServiceDiscovered = false;
        }
    }

    private void checkServiceDiscover() {
        if (mRetryConnectEnable && mRetryConnectCount > 0 && serviceTimeoutMillis > 0) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!mServiceDiscovered) {
                        connect(mAutoConnect, mBluetoothDevice);
                        mRetryConnectCount -= 1;
                    }
                }
            }, serviceTimeoutMillis);
        }
    }

    private BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "device connect success!");
                mConnected = true;
                if (isStopScanAfterConnected) {
                    stopScan();
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Map<Object, OnLeConnectListener> map : connectListenerList) {
                            for (Map.Entry<Object, OnLeConnectListener> entry : map.entrySet()) {
                                entry.getValue().onDeviceConnected();
                            }
                        }
                        if (mOnLeConnectListener != null) {
                            mOnLeConnectListener.onDeviceConnected();
                        }
                    }
                });

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (gatt.getDevice().getBondState() != BluetoothDevice.BOND_BONDING) {
                            mBluetoothGatt.discoverServices();
                            checkServiceDiscover();
                        }
                    }
                }, 600);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "device disconnect.");
                mConnected = false;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Map<Object, OnLeConnectListener> map : connectListenerList) {
                            for (Map.Entry<Object, OnLeConnectListener> entry : map.entrySet()) {
                                entry.getValue().onDeviceDisconnected();
                            }
                        }
                        if (mOnLeConnectListener != null) {
                            mOnLeConnectListener.onDeviceDisconnected();
                        }
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "success with find services discovered .");
                mServiceDiscovered = true;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Map<Object, OnLeConnectListener> map : connectListenerList) {
                            for (Map.Entry<Object, OnLeConnectListener> entry : map.entrySet()) {
                                entry.getValue().onServicesDiscovered(gatt);
                            }
                        }
                        if (mOnLeConnectListener != null) {
                            mOnLeConnectListener.onServicesDiscovered(gatt);
                        }
                    }
                });

            } else if (status == BluetoothGatt.GATT_FAILURE) {
                Log.d(TAG, "failure find services discovered.");
                mServiceDiscovered = false;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            //read
            if (status == BluetoothGatt.GATT_SUCCESS) {

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        for (Map<Object, OnLeReadCharacteristicListener> map : readCharacteristicListenerList) {
                            for (Map.Entry<Object, OnLeReadCharacteristicListener> entry : map.entrySet()) {
                                entry.getValue().onSuccess(characteristic);
                            }
                        }
                        if (mOnLeReadCharacteristicListener != null) {
                            mOnLeReadCharacteristicListener.onSuccess(characteristic);
                        }
                    }
                });

            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Map<Object, OnLeReadCharacteristicListener> map : readCharacteristicListenerList) {
                            for (Map.Entry<Object, OnLeReadCharacteristicListener> entry : map.entrySet()) {
                                entry.getValue().onFailure("Phone has lost bonding information", status);
                            }
                        }
                        if (mOnLeReadCharacteristicListener != null) {
                            mOnLeReadCharacteristicListener.onFailure("Phone has lost bonding information", status);
                        }
                    }
                });

            } else {

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Map<Object, OnLeReadCharacteristicListener> map : readCharacteristicListenerList) {
                            for (Map.Entry<Object, OnLeReadCharacteristicListener> entry : map.entrySet()) {
                                entry.getValue().onFailure("Error on reading characteristic", status);
                            }
                        }
                        if (mOnLeReadCharacteristicListener != null) {
                            mOnLeReadCharacteristicListener.onFailure("Error on reading characteristic", status);
                        }
                    }
                });

            }
            mRequestQueue.next();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            final BluetoothGattCharacteristic c = characteristic;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Map<Object, OnLeWriteCharacteristicListener> map : writeCharacteristicListenerList) {
                            for (Map.Entry<Object, OnLeWriteCharacteristicListener> entry : map.entrySet()) {
                                entry.getValue().onSuccess(c);
                            }
                        }
                        if (mOnLeWriteCharacteristicListener != null) {
                            mOnLeWriteCharacteristicListener.onSuccess(c);
                        }
                    }
                });

            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Map<Object, OnLeWriteCharacteristicListener> map : writeCharacteristicListenerList) {
                            for (Map.Entry<Object, OnLeWriteCharacteristicListener> entry : map.entrySet()) {
                                entry.getValue().onFailed("Phone has lost of bonding information. ", status);
                            }
                        }
                        if (mOnLeWriteCharacteristicListener != null) {
                            mOnLeWriteCharacteristicListener.onFailed("Phone has lost of bonding information. ", status);
                        }
                    }
                });

            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Map<Object, OnLeWriteCharacteristicListener> map : writeCharacteristicListenerList) {
                            for (Map.Entry<Object, OnLeWriteCharacteristicListener> entry : map.entrySet()) {
                                entry.getValue().onFailed("Error on reading characteristic", status);
                            }
                        }
                        if (mOnLeWriteCharacteristicListener != null) {
                            mOnLeWriteCharacteristicListener.onFailed("Error on reading characteristic", status);
                        }
                    }
                });

            }
            mRequestQueue.next();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            mHandler.post(new Runnable() {
                @Override
                public void run() {




                    for (Map<Object, OnLeNotificationListener> map : notificationListenerList) {
                        for (Map.Entry<Object, OnLeNotificationListener> entry : map.entrySet()) {
                            entry.getValue().onSuccess(characteristic);
                        }
                    }

                    if (mOnLeNotificationListener != null) {
                        mOnLeNotificationListener.onSuccess(characteristic);
                    }
                }
            });

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            mRequestQueue.next();
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    void destroy() {
        mOnLeScanListener = null;
        mOnLeConnectListener = null;
        mOnLeNotificationListener = null;
        mOnLeWriteCharacteristicListener = null;
        mOnLeReadCharacteristicListener = null;
    }

    void destroy(Object tag) {
        cancelTag(tag);
    }

    void cancelTag(Object tag) {
        for (Map<Object, OnLeScanListener> map : scanListenerList) {
            if (map.containsKey(tag)) {
                scanListenerList.remove(map);
            }
        }
        for (Map<Object, OnLeConnectListener> map : connectListenerList) {
            if (map.containsKey(tag)) {
                connectListenerList.remove(map);
            }
        }
        for (Map<Object, OnLeNotificationListener> map : notificationListenerList) {
            if (map.containsKey(tag)) {
                notificationListenerList.remove(map);
            }
        }
        for (Map<Object, OnLeWriteCharacteristicListener> map : writeCharacteristicListenerList) {
            if (map.containsKey(tag)) {
                writeCharacteristicListenerList.remove(map);
            }
        }
        for (Map<Object, OnLeReadCharacteristicListener> map : readCharacteristicListenerList) {
            if (map.containsKey(tag)) {
                readCharacteristicListenerList.remove(map);
            }
        }
    }

    void cancelAllTag() {
        scanListenerList.clear();
        connectListenerList.clear();
        notificationListenerList.clear();
        writeCharacteristicListenerList.clear();
        readCharacteristicListenerList.clear();

    }

    void clearQueue() {
        mRequestQueue.cancelAll();
    }


    private class RequestQueue {

        private Queue<Request> mRequestBlockingQueue = new LinkedList<>();

        void addRequest(Request request) {
            int oldSize = mRequestBlockingQueue.size();

            mRequestBlockingQueue.add(request);
            if (mRequestBlockingQueue.size() == 1 ){
                startExecutor();

            }

        }

        private void startExecutor() {
            Request request = mRequestBlockingQueue.peek(); //此方法检索，但是不移除此列表的头（第一个元素）。
            switch (request.type) {
                case WRITE:
                    writeCharacteristic(request.getCharacteristic());
                    break;
                case READ:
                    readCharacteristic(request.getCharacteristic());
                    break;
                case ENABLE_NOTIFICATIONS:
                    enableNotification(request.isEnable(), request.getCharacteristic());
                    break;
                case ENABLE_INDICATIONS:
                    break;
            }
        }

        void next() {
            mRequestBlockingQueue.poll();  //此方法检索并移除此列表的头
            if (mRequestBlockingQueue != null && mRequestBlockingQueue.size() > 0) {
                startExecutor();
            }
        }

        void cancelAll() {
            mRequestBlockingQueue.clear();
        }

    }


}
