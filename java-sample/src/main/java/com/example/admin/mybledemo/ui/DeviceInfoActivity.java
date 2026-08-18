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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.io.UnsupportedEncodingException;

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
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.utils.ByteUtils;

public class DeviceInfoActivity extends AppCompatActivity {

    private static final String TAG = "DeviceInfoActivity";
    public static final String EXTRA_TAG = "device";
    /** 优先发送目标：fff2 Write Without Response 特征 */
    private static final UUID FFF2_UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
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
    private Button btnAaa;
    private Button btn123;
    private CheckBox cbHex;
    private TextView tvHexLabel;
    private EditText etSend;
    private Button btnSend;
    private UUID writeServiceUuid;
    private UUID writeCharacteristicUuid;


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

        btnAaa = findViewById(R.id.btn_aaa);
        btn123 = findViewById(R.id.btn_123);
        cbHex = findViewById(R.id.cb_hex);
        tvHexLabel = findViewById(R.id.tv_hex_label);
        etSend = findViewById(R.id.et_send);
        btnSend = findViewById(R.id.btn_send);

        btnAaa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("AAA");
            }
        });
        btn123.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("123");
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData(etSend.getText().toString());
            }
        });
        tvHexLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cbHex.toggle();
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
            tvServices.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            tvControl.setTextColor(ContextCompat.getColor(this, R.color.tab_unselected));
        } else {
            controlContainer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            tvControl.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            tvServices.setTextColor(ContextCompat.getColor(this, R.color.tab_unselected));
        }
    }

    /**
     * 追加一条 Control 页面的数据记录，并自动滚动到最新一条
     */
    private void addControlLog(String log) {
        controlLogs.add(log);
        controlLogAdapter.notifyItemInserted(controlLogs.size() - 1);
        controlRecyclerView.scrollToPosition(controlLogs.size() - 1);
    }

    /**
     * 发送数据到当前连接的蓝牙设备：
     * 勾选 16 进制时按 hex 解析内容，否则按 UTF-8 编码发送
     */
    private void sendData(String content) {
        if (bleDevice == null || !bleDevice.isConnected()) {
            Utils.showToast("设备未连接");
            return;
        }
        if (writeCharacteristicUuid == null) {
            Utils.showToast("设备无可写特征，无法发送");
            return;
        }
        byte[] bytes;
//        if (cbHex.isChecked()) {
//            // 清理 0x/空格/冒号/逗号分隔符
//            String hex = content.replaceAll("(?i)0x", "").replaceAll("[\\s:，,]", "");
//            if (hex.isEmpty()) {
//                Utils.showToast("请输入要发送的数据");
//                return;
//            }
//            if ((hex.length() & 1) != 0) {
//                hex = "0" + hex;// 奇数长度前补 0
//            }
//            try {
//                bytes = ByteUtils.hexStr2Bytes(hex);
//            } catch (Exception e) {
//                Utils.showToast("无效的16进制数据");
//                return;
//            }
//        } else {
            try {
                bytes = content.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                bytes = content.getBytes();
            }
//        }
        boolean result = ble.writeByUuid(bleDevice, bytes, writeServiceUuid, writeCharacteristicUuid, writeCallback);
        if (!result) {
            Utils.showToast("发送失败：特征不可写或设备异常");
        }
    }

    private BleWriteCallback<BleDevice> writeCallback = new BleWriteCallback<BleDevice>() {
        @Override
        public void onWriteSuccess(BleDevice device, BluetoothGattCharacteristic characteristic) {
            BleLog.e(TAG, "onWriteSuccess: " + device.getBleName());
            Utils.showToast("发送成功");
        }

        @Override
        public void onWriteFailed(BleDevice device, int failedCode) {
            super.onWriteFailed(device, failedCode);
            Utils.showToast("发送失败，错误码:" + failedCode);
        }
    };

    private BleNotifyCallback<BleDevice> notifyCallback = new BleNotifyCallback<BleDevice>() {
        @Override
        public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
            // 只记录当前页面设备的原始数据
            if (bleDevice != null && !device.getBleAddress().equals(bleDevice.getBleAddress())) return;
            UUID uuid = characteristic.getUuid();
            BleLog.e(TAG, "onChanged==uuid:" + uuid.toString());
            BleLog.e(TAG, "onChanged==data:" + ByteUtils.toHexString(characteristic.getValue()));
            final byte[] value = characteristic.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 勾选 16 进制显示原始 hex 内容，否则显示 utf-8 字符串
                    String data = cbHex.isChecked()
                            ? ByteUtils.toHexString(value)
                            : ByteUtils.toString(value, "UTF-8");
                    final String log = dateFormat.format(new Date()) + " -> " + data;
                    addControlLog(log);
                }
            });
        }

        @Override
        public void onNotifySuccess(BleDevice device) {
            super.onNotifySuccess(device);
            BleLog.e(TAG, "onNotifySuccess: " + device.getBleName());
        }
    };

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

            // 遍历设备所有 service/characteristic，逐个使能 notify/indicate 特征的通知，
            // 以接收任意蓝牙设备上报的全部原始数据（不依赖 MyApplication 中配置的 service uuid）
            for (BluetoothGattService service : gatt.getServices()) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    int properties = characteristic.getProperties();
                    if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                            || (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                        ble.enableNotifyByUuid(device, true, service.getUuid(), characteristic.getUuid(), notifyCallback);
                    }
                }
            }

            // 优先查找 fff2 Write Without Response 特征作为发送目标
            if (writeCharacteristicUuid == null) {
                for (BluetoothGattService service : gatt.getServices()) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        if (FFF2_UUID.equals(characteristic.getUuid())) {
                            int properties = characteristic.getProperties();
                            if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                                    || (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                                writeServiceUuid = service.getUuid();
                                writeCharacteristicUuid = characteristic.getUuid();
                                break;
                            }
                        }
                    }
                    if (writeCharacteristicUuid != null) break;
                }
            }
            // 回退：记录第一个可写特征，供发送功能使用（不依赖 MyApplication 中配置的写特征 uuid）
            if (writeCharacteristicUuid == null) {
                for (BluetoothGattService service : gatt.getServices()) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        int properties = characteristic.getProperties();
                        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
                                || (properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                            writeServiceUuid = service.getUuid();
                            writeCharacteristicUuid = characteristic.getUuid();
                            break;
                        }
                    }
                    if (writeCharacteristicUuid != null) break;
                }
            }
        }

        @Override
        public void onReady(BleDevice device) {
            super.onReady(device);
            // 通知使能已移至 onServicesDiscovered 中按 UUID 逐个开启
            ble.read(device, new BleReadCallback<BleDevice>() {
                @Override
                public void onReadSuccess(BleDevice bleDevice, BluetoothGattCharacteristic characteristic) {
                    // 只记录当前页面设备的原始数据
                    if (bleDevice != null && !device.getBleAddress().equals(bleDevice.getBleAddress())) return;
                    final String log = dateFormat.format(new Date()) + " -> " + ByteUtils.toHexString(characteristic.getValue());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addControlLog(log);
                        }
                    });
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
