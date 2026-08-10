package com.example.admin.mybledemo.ui;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.Utils;
import com.example.admin.mybledemo.adapter.ControlLogAdapter;
import com.example.admin.mybledemo.adapter.DeviceInfoAdapter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotifyCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.utils.ByteUtils;

public class DeviceInfoActivity extends AppCompatActivity {

    private static final String TAG = "DeviceInfoActivity";
    public static final String EXTRA_TAG = "device";
    private BleDevice bleDevice;
    private Ble<BleDevice> ble;
    private ActionBar actionBar;
    private RecyclerView recyclerView;
    private DeviceInfoAdapter adapter;
    private List<BluetoothGattService> gattServices;
    private TextView tvServices;
    private TextView tvControl;
    private FrameLayout controlContainer;
    private RecyclerView controlRecyclerView;
    private ControlLogAdapter controlLogAdapter;
    private List<String> controlLogs;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deviceinfo);
        initView();
        initData();
    }

    private void initData() {
        ble = Ble.getInstance();
        bleDevice = getIntent().getParcelableExtra(EXTRA_TAG);
        if (bleDevice == null) return;
        ble.connect(bleDevice, connectCallback);
    }

    private void initView() {
        actionBar = getSupportActionBar();
        actionBar.setTitle("详情信息");
        actionBar.setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyclerView);
        gattServices = new ArrayList<>();
        adapter = new DeviceInfoAdapter(this, gattServices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        recyclerView.getItemAnimator().setChangeDuration(300);
        recyclerView.getItemAnimator().setMoveDuration(300);
        recyclerView.setAdapter(adapter);

        tvServices = findViewById(R.id.tv_services);
        tvControl = findViewById(R.id.tv_control);
        controlContainer = findViewById(R.id.control_container);

        controlRecyclerView = findViewById(R.id.controlRecyclerView);
        controlLogs = new ArrayList<>();
        controlLogAdapter = new ControlLogAdapter(this, controlLogs);
        controlRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        controlRecyclerView.setAdapter(controlLogAdapter);

        tvServices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPage(true);
            }
        });
        tvControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPage(false);
            }
        });
        // 默认选中 Services 页面
        switchPage(true);
    }

    /**
     * 切换 Services / Control 页面，并高亮当前选中的文本
     *
     * @param showServices true 显示 Services 列表，false 显示 Control 空白页
     */
    private void switchPage(boolean showServices) {
        if (showServices) {
            recyclerView.setVisibility(View.VISIBLE);
            controlContainer.setVisibility(View.GONE);
            tvServices.setTextColor(ContextCompat.getColor(this, R.color.tab_selected));
            tvControl.setTextColor(ContextCompat.getColor(this, R.color.tab_unselected));
        } else {
            controlContainer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            tvControl.setTextColor(ContextCompat.getColor(this, R.color.tab_selected));
            tvServices.setTextColor(ContextCompat.getColor(this, R.color.tab_unselected));
        }
    }

    /**
     * 追加一条 Control 页面的数据记录，并自动滚动到最新一条
     */
    private void addControlLog(String log) {
        controlLogs.add(log);
        controlLogAdapter.notifyDataSetChanged();
        controlRecyclerView.scrollToPosition(controlLogs.size() - 1);
    }

    private BleConnectCallback<BleDevice> connectCallback = new BleConnectCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(BleDevice device) {
            Log.e(TAG, "onConnectionChanged: " + device.getConnectionState()+Thread.currentThread().getName());
            if (device.isConnected()) {
                actionBar.setSubtitle("已连接");
            }else if (device.isConnecting()){
                actionBar.setSubtitle("连接中...");
            }
            else if (device.isDisconnected()){
                actionBar.setSubtitle("未连接");
            }
        }

        @Override
        public void onConnectFailed(BleDevice device, int errorCode) {
            super.onConnectFailed(device, errorCode);
            Utils.showToast("连接异常，异常状态码:" + errorCode);
        }

        @Override
        public void onConnectCancel(BleDevice device) {
            super.onConnectCancel(device);
            Log.e(TAG, "onConnectCancel: " + device.getBleName());
        }

        @Override
        public void onServicesDiscovered(BleDevice device, BluetoothGatt gatt) {
            super.onServicesDiscovered(device, gatt);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gattServices.addAll(gatt.getServices());
                    adapter.notifyDataSetChanged();
                }
            });

        }

        @Override
        public void onReady(BleDevice device) {
            super.onReady(device);
            //连接成功后，设置通知
            ble.enableNotify(device, true, new BleNotifyCallback<BleDevice>() {
                @Override
                public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
                    // 只记录当前页面设备的原始数据
                    if (bleDevice != null && !device.getBleAddress().equals(bleDevice.getBleAddress())) return;
                    UUID uuid = characteristic.getUuid();
                    BleLog.e(TAG, "onChanged==uuid:" + uuid.toString());
                    BleLog.e(TAG, "onChanged==data:" + ByteUtils.toHexString(characteristic.getValue()));
                    final String log = dateFormat.format(new Date()) + " -> " + ByteUtils.toHexString(characteristic.getValue());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.showToast(String.format("收到设备通知数据: %s", ByteUtils.toHexString(characteristic.getValue())));
                            addControlLog(log);
                        }
                    });
                }

                @Override
                public void onNotifySuccess(BleDevice device) {
                    super.onNotifySuccess(device);
                    BleLog.e(TAG, "onNotifySuccess: "+device.getBleName());
                }
            });
            ble.read(device, new BleReadCallback<BleDevice>() {
                @Override
                public void onReadSuccess(BleDevice bleDevice, BluetoothGattCharacteristic characteristic) {
                    // 只记录当前页面设备的原始数据
                    if (bleDevice != null && !device.getBleAddress().equals(bleDevice.getBleAddress())) return;
                    final String log = dateFormat.format(new Date()) + " -> " + ByteUtils.toHexString(characteristic.getValue());
                    addControlLog(log);
                    byte[] data = characteristic.getValue();
                    BleLog.w(TAG, "onReadSuccess: " + Arrays.toString(data));
                }
            });
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:// 点击返回图标事件
                this.finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleDevice != null){
            if (bleDevice.isConnecting()){
                ble.cancelConnecting(bleDevice);
            }else if (bleDevice.isConnected()){
                ble.disconnect(bleDevice);
            }
        }
        ble.cancelCallback(connectCallback);
    }
}
