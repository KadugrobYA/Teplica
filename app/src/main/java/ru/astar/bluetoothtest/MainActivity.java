package ru.astar.bluetoothtest;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_ENABLE_BLUETOOTH = 1001;
    public final String TAG = getClass().getSimpleName();

    private Button btLedOne;
    private Button btLedTwo;

    private boolean isEnabledLedOne = false;
    private boolean isEnabledLedTwo = false;

    private BluetoothAdapter mBluetoothAdapter;
    private ProgressDialog mProgressDialog;
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();
    private DeviceListAdapter mDeviceListAdapter;

    private BluetoothSocket mBluetoothSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private ListView listDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btLedOne = findViewById(R.id.btLedOne);
        btLedTwo = findViewById(R.id.btLedTwo);
        btLedOne.setOnClickListener(clickListener);
        btLedTwo.setOnClickListener(clickListener);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "onCreate: Ваше устройство не поддерживает bluetooth");
            finish();
        }

        mDeviceListAdapter = new DeviceListAdapter(this, R.layout.device_item, mDevices);

        // включаем bluetooth
        enableBluetooth();

        if(mInputStream !=null){
            byte[] buffer = new byte[256];
            int bytes = 0;
            String readMessage="";
            try {
                bytes = mInputStream.read(buffer);
                readMessage = new String(buffer, 0, bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(readMessage!=null){
                showToastMessage(readMessage);
            }

        }


    }

protected void onDestroy(){
        super.onDestroy();
        if(mBluetoothSocket !=null){
            try {
                mBluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(mOutputStream !=null){
            try {
                mOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    if(mInputStream !=null){
        try {
            mInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_search:
                searchDevices();
                break;

            case R.id.item_exit:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * запускает поиск bluetooth устройств
     */
    private void searchDevices() {
        Log.d(TAG, "searchDevices()");
        enableBluetooth();

        checkPermissionLocation();

        if (!mBluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "searchDevices: начинаем поиск устройств.");
            mBluetoothAdapter.startDiscovery();
        }

        if (mBluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "searchDevices: поиск уже был запущен... перезапускаем его еще раз.");
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothAdapter.startDiscovery();
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mRecevier, filter);
    }

    /**
     * Показывает диалоговое окно со списком найденых устройств
     */
    private void showListDevices() {
        Log.d(TAG, "showListDevices()");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Найденые устройства");

        View view = getLayoutInflater().inflate(R.layout.list_devices_view, null);
        listDevices = view.findViewById(R.id.list_devices);
        listDevices.setAdapter(mDeviceListAdapter);

        listDevices.setOnItemClickListener(itemOnClickListener);

        builder.setView(view);
        builder.setNegativeButton("OK", null);
        builder.create();
        builder.show();
    }


    /**
     * Проверяет разрешения на доступ к данным местоположения
     */
    private void checkPermissionLocation() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int check = checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            check += checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");

            if (check != 0) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1002);
            }
        }
    }

    /**
     * Включаем bluetooth
     */
    private void enableBluetooth() {
        Log.d(TAG, "enableBluetooth()");
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableBluetooth: Bluetooth выключен, пытаемся включить");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQ_ENABLE_BLUETOOTH);
        }
    }
    private void setMessage(int command){
        byte buffer = (byte)command;
        Log.d(TAG, "setMessage buffer:  " + buffer);
        if(mOutputStream !=null){
            try {
                mOutputStream.write(buffer);
                mOutputStream.flush();
            }catch (Exception e){
                showToastMessage("Ошибка mOutputStream Команда не передана");
                e.printStackTrace();
            }

        }
    }
    private void startConnection(BluetoothDevice device) {
        if(device !=null){
            try {
                Method method = device.getClass().getMethod("createRfcommSocket",new Class[]{int.class});
                mBluetoothSocket = (BluetoothSocket) method.invoke(device,1);
                mBluetoothSocket.connect();
                mOutputStream = mBluetoothSocket.getOutputStream(); // Присваеваем выходной поток
                mInputStream = mBluetoothSocket.getInputStream();
                showToastMessage("Подключено BL");
            }catch (Exception e){
                showToastMessage("Ошибка Подключения BL");
                e.printStackTrace();
            }

        }
    }
    /**
     * Показывает всплывающее текстовое сообщение
     * @param message - текст сообщения
     */
    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // принудительно пытаемся включить bluetooth
        if (requestCode == REQ_ENABLE_BLUETOOTH) {
            if (!mBluetoothAdapter.isEnabled()) {
                Log.d(TAG, "onActivityResult: Повторно пытаемся отправить запрос на включение bluetooth");
                enableBluetooth();
            }
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // нажата кнопка для управления первым светодиодом
            int command=0;
            if (view.equals(btLedOne)) {
                isEnabledLedOne = !isEnabledLedOne;
                if(isEnabledLedOne){
                    command=1;
                }else{
                    command=2;
                }
                Log.d(TAG, "onClick: isEnabledLedOne = " + isEnabledLedOne);
            }

            // нажата кнопка для управления вторым светодиодом
            if (view.equals(btLedTwo)) {
                isEnabledLedTwo = !isEnabledLedTwo;
                if(isEnabledLedTwo){
                    command=3;
                }else{
                    command=4;
                }
                Log.d(TAG, "onClick: isEnabledLedTwo = " + isEnabledLedTwo);
            }
            setMessage(command);
        }
    };

    private AdapterView.OnItemClickListener itemOnClickListener=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            BluetoothDevice device = mDevices.get(position);
            startConnection(device);
        }
    };



    /**
     * Отслеживаем состояния bluetooth
     * Вкл/Выкл, поиск новых устройств
     */
    private BroadcastReceiver mRecevier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // начат поиск устройств
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                Log.d(TAG, "onReceive: ACTION_DISCOVERY_STARTED");

                showToastMessage("Начат поиск устройств.");

                mProgressDialog = ProgressDialog.show(MainActivity.this, "Поиск устройств", " Пожалуйста подождите...");
            }

            // поиск устройств завершен
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Log.d(TAG, "onReceive: ACTION_DISCOVERY_FINISHED");
                showToastMessage("Поиск устройств завершен.");

                mProgressDialog.dismiss();

                showListDevices();
            }

            // если найдено новое устройство
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                Log.d(TAG, "onReceive: ACTION_FOUND");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (!mDevices.contains(device))
                        mDeviceListAdapter.add(device);
                }
            }
        }
    };
}
