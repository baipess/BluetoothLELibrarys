package com.qindachang.bluetoothle;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.UUID;

public class BluetoothLe {

    private static class SingletonHolder {
        private static final BluetoothLe INSTANCE = new BluetoothLe();
    }

    private BluetoothLe() {
    }

    public static BluetoothLe getDefault() {
        return SingletonHolder.INSTANCE;
    }

    private BleManager mBleManager;

    private String filterDeviceName;
    private String filterDeviceAddress;
    private UUID uFilerServiceUUID;
    private int scanPeriod;
    private int reportDelayMillis;

    public void init(Context context) {
        if (mBleManager == null) {
            mBleManager = new BleManager(context.getApplicationContext());
        }
    }

    public boolean isBluetoothOpen() {
        return mBleManager.isBluetoothOpen();
    }

    public void enableBluetooth(Activity activity) {
        mBleManager.enableBluetooth(activity);
    }

    public void disableBluetooth() {
        mBleManager.disableBluetooth();
    }

    public boolean clearDeviceCache() {
        return mBleManager.clearDeviceCache();
    }

    public BluetoothLe setScanWithDeviceName(String deviceName) {
        this.filterDeviceName = deviceName;
        return this;
    }

    public BluetoothLe setScanWithDeviceAddress(String deviceAddress) {
        this.filterDeviceAddress = deviceAddress;
        return this;
    }

    public BluetoothLe setScanWithServiceUUID(String serviceUUID) {
        setScanWithServiceUUID(UUID.fromString(serviceUUID));
        return this;
    }

    public BluetoothLe setScanWithServiceUUID(UUID serviceUUID) {
        this.uFilerServiceUUID = serviceUUID;
        return this;
    }

    public BluetoothLe setScanPeriod(int millisecond) {
        this.scanPeriod = millisecond;
        return this;
    }

    public BluetoothLe setReportDelay(int reportDelayMillis) {
        this.reportDelayMillis = reportDelayMillis;
        return this;
    }

    public void startScan(Activity activity, OnLeScanListener onLeScanListener) {
        mBleManager.setOnLeScanListener(onLeScanListener);
        mBleManager.scan(activity, filterDeviceName, filterDeviceAddress, uFilerServiceUUID, scanPeriod, reportDelayMillis);
        filterDeviceName = null;
        filterDeviceAddress = null;
        uFilerServiceUUID = null;
        scanPeriod = 0;
    }

    public void startScan(@NonNull Object tag, Activity activity, OnLeScanListener onLeScanListener) {
        mBleManager.addScanLeListener(tag, onLeScanListener);
        mBleManager.scan(activity, filterDeviceName, filterDeviceAddress, uFilerServiceUUID, scanPeriod, reportDelayMillis);
        filterDeviceName = null;
        filterDeviceAddress = null;
        uFilerServiceUUID = null;
        scanPeriod = 0;
    }

    public void stopScan() {
        mBleManager.stopScan();
    }

    public boolean getScanning() {
        return mBleManager.scanning();
    }

    public boolean getConnected() {
        return mBleManager.getConnected();
    }

    public BluetoothLe setStopScanAfterConnected(boolean enable) {
        mBleManager.setStopScanAfterConnected(enable);
        return this;
    }

    public BluetoothLe setConnectTimeOut(int millisecond) {
        mBleManager.setConnectTimeoutMillis(millisecond);
        return this;
    }

    public BluetoothLe setServiceDiscoverTimeOut(int millisecond) {
        mBleManager.setServiceTimeoutMillis(millisecond);
        return this;
    }

    public BluetoothLe setRetryConnectEnable(boolean enable) {
        mBleManager.setRetryConnectEnable(enable);
        return this;
    }

    public BluetoothLe setRetryConnectCount(int count) {
        mBleManager.setRetryConnectCount(count);
        return this;
    }

    public void startConnect(BluetoothDevice bluetoothDevice) {
        mBleManager.connect(false, bluetoothDevice);
    }

    public void startConnect(boolean autoConnect, BluetoothDevice bluetoothDevice) {
        mBleManager.connect(autoConnect, bluetoothDevice);
    }

    public void startConnect(BluetoothDevice bluetoothDevice, OnLeConnectListener onLeConnectListener) {
        startConnect(false, bluetoothDevice, onLeConnectListener);
    }

    public void startConnect(boolean autoConnect, BluetoothDevice bluetoothDevice, OnLeConnectListener onLeConnectListener) {
        setOnConnectListener(onLeConnectListener);
        mBleManager.connect(autoConnect, bluetoothDevice);
    }

    public void setOnConnectListener(OnLeConnectListener onLeConnectListener) {
        mBleManager.setConnectListener(onLeConnectListener);
    }

    public void setOnConnectListener(@NonNull Object tag, OnLeConnectListener onLeConnectListener) {
        mBleManager.addConnectListener(tag, onLeConnectListener);
    }

    public void disconnect() {
        mBleManager.disconnect();
    }

    public BluetoothLe enableNotification(boolean enable, String serviceUUID, String characteristicUUID) {
        enableNotification(enable, UUID.fromString(serviceUUID), UUID.fromString(characteristicUUID));
        return this;
    }

    public BluetoothLe enableNotification(boolean enable, UUID serviceUUID, UUID characteristicUUID) {
        enableNotification(enable, serviceUUID, new UUID[]{characteristicUUID});
        return this;
    }

    public BluetoothLe enableNotification(boolean enable, String serviceUUID, String[] characteristicUUIDs) {
        int length = characteristicUUIDs.length;
        UUID[] uuids = new UUID[length];
        for (int i = 0; i < length; i++) {
            uuids[i] = UUID.fromString(characteristicUUIDs[i]);
        }
        enableNotification(enable, UUID.fromString(serviceUUID), uuids);
        return this;
    }

    public BluetoothLe enableNotification(boolean enable, UUID serviceUUID, UUID[] characteristicUUIDs) {
        mBleManager.enableNotificationQueue(enable, serviceUUID, characteristicUUIDs);
        return this;
    }

    public void setOnNotificationListener(OnLeNotificationListener onLeNotificationListener) {
        mBleManager.setOnLeNotificationListener(onLeNotificationListener);
    }

    public void setOnNotificationListener(@NonNull Object tag, OnLeNotificationListener onLeNotificationListener) {
        mBleManager.addNotificationListener(tag, onLeNotificationListener);
    }

    public void readCharacteristic(String serviceUUID, String characteristicUUID) {
        mBleManager.readCharacteristicQueue(UUID.fromString(serviceUUID), UUID.fromString(characteristicUUID));
    }

    public void readCharacteristic(UUID serviceUUID, UUID characteristicUUID) {
        mBleManager.readCharacteristicQueue(serviceUUID, characteristicUUID);
    }

    public void readCharacteristic(String serviceUUID, String characteristicUUID, OnLeReadCharacteristicListener onLeReadCharacteristicListener) {
        readCharacteristic(UUID.fromString(serviceUUID), UUID.fromString(characteristicUUID), onLeReadCharacteristicListener);
    }

    public void readCharacteristic(UUID serviceUUID, UUID characteristicUUID, OnLeReadCharacteristicListener onLeReadCharacteristicListener) {
        mBleManager.readCharacteristicQueue(serviceUUID, characteristicUUID);
        setOnReadCharacteristicListener(onLeReadCharacteristicListener);
    }

    public void setOnReadCharacteristicListener(OnLeReadCharacteristicListener onReadCharacteristicListener) {
        mBleManager.setOnLeReadCharacteristicListener(onReadCharacteristicListener);
    }

    public void setOnReadCharacteristicListener(@NonNull Object tag, OnLeReadCharacteristicListener onReadCharacteristicListener) {
        mBleManager.addReadCharacteristicListener(tag, onReadCharacteristicListener);
    }

    public void writeDataToCharacteristic(byte[] bytes, String serviceUUID, String characteristicUUID) {
        writeDataToCharacteristic(bytes, UUID.fromString(serviceUUID), UUID.fromString(characteristicUUID));
    }

    public void writeDataToCharacteristic(byte[] bytes, UUID serviceUUID, UUID characteristicUUID) {
        mBleManager.writeCharacteristicQueue(bytes, serviceUUID, characteristicUUID);
}

    public void writeDataToCharacteristic(byte[] bytes, String serviceUUID, String characteristicUUID, OnLeWriteCharacteristicListener onLeWriteCharacteristicListener) {
        writeDataToCharacteristic(bytes, UUID.fromString(serviceUUID), UUID.fromString(characteristicUUID), onLeWriteCharacteristicListener);
    }

    public void writeDataToCharacteristic(byte[] bytes, UUID serviceUUID, UUID characteristicUUID, OnLeWriteCharacteristicListener onLeWriteCharacteristicListener) {
        setOnWriteCharacteristicListener(onLeWriteCharacteristicListener);
        mBleManager.writeCharacteristicQueue(bytes, serviceUUID, characteristicUUID);
    }

    public void setOnWriteCharacteristicListener(OnLeWriteCharacteristicListener onLeWriteCharacteristicListener) {
        mBleManager.setWriteCharacteristicListener(onLeWriteCharacteristicListener);
    }

    public void setOnWriteCharacteristicListener(@NonNull Object tag, OnLeWriteCharacteristicListener onLeWriteCharacteristicListener) {
        mBleManager.addWriteCharacteristicListener(tag, onLeWriteCharacteristicListener);
    }

    public void close() {
        mBleManager.close();
    }

    public void destroy() {
        mBleManager.destroy();
    }

    public void destroy(@NonNull Object tag) {
        mBleManager.destroy(tag);
    }

    public void cancelTag(@NonNull Object tag) {
        mBleManager.cancelTag(tag);
    }

    public void cancelAllTag() {
        mBleManager.cancelAllTag();
    }

    public void clearQueue() {
        mBleManager.clearQueue();
    }
}
