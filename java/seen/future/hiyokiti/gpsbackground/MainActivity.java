package seen.future.hiyokiti.gpsbackground;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_MULTI_PERMISSIONS = 101;//リクエストコード(0以上なんでもいいです)

    private TextView textView; //ログ表示用テキストビュー

    private StorageReadWrite fileReadWrite; //ファイル読み書き用クラス


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();
        fileReadWrite = new StorageReadWrite(context);

        //設定読み込み
        fileReadWrite.loadConfigu(this);

        //Spinner(ドロップダウンリスト)を作成する
        createSpinner();

        // Android 6, API 23以上でユーザー確認が必要です。
        if(Build.VERSION.SDK_INT >= 23){
            checkPermissions();//パーミッションの確認
        }
        else{
            locationServiceStartSetting();//位置情報サービス開始設定
        }
    }


    @Override
    protected void onPause(){
        super.onPause();
        //設定保存
        fileReadWrite.saveConfigu(this);
    }


    // 位置情報と外部ストレージのパーミッションの確認
    private  void checkPermissions(){

        //リクエストパーミッションを保管する配列
        ArrayList reqPermissions = new ArrayList<>();

        // GPS位置情報の パーミッション が許可されているか確認
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // 未許可だったら、要求するパーミッションを配列に追加
            reqPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

        }

        // ネットワーク位置情報の パーミッション が許可されているか確認
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // 未許可だったら、要求するパーミッションを配列に追加
            reqPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        }

        // 外部ストレージ書き込みが許可されているか確認
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // 未許可だったら、要求するパーミッションを配列に追加
            reqPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        }

        if (!reqPermissions.isEmpty()) {

            // 未許可があるようだったらユーザーに許可するようリクエストする
            ActivityCompat.requestPermissions(this,
                    (String[]) reqPermissions.toArray(new String[0]),
                    REQUEST_MULTI_PERMISSIONS);
        }
        else{
            // 許可済み
            locationServiceStartSetting();
        }
    }


    // requestPermissionsのコールバック
    /**
     * @param requestCode  //リクエストコード
     * @param permissions  //要求されたパーミッション(GPSの許可)
     * @param grantResults //許可がある場合、PERMISSION_GRANTEDが返されます。許可がない場合、PERMISSION_DENIEDが返されます。
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_MULTI_PERMISSIONS) {
            //リクエストコードが一致

            if (grantResults.length > 0) {
                //ユーザー応答あり

                for (int i = 0; i < permissions.length; i++) {
                    //GPS位置情報
                    if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        //要求されたパーミッションが位置情報

                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // 許可された

                        } else {
                            // それでも拒否された時の対応
                            Toast.makeText(this,"GPS位置情報の許可がないので計測できません",Toast.LENGTH_LONG).show();
                        }
                    }
                    //ネットワーク位置情報
                    else if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        //要求されたパーミッションが外部ストレージ

                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // 許可された

                        } else {
                            // それでも拒否された時の対応
                            Toast.makeText(this,"ネットワーク位置情報の許可がないので書き込みできません",Toast.LENGTH_LONG).show();
                        }
                    }
                    // 外部ストレージ
                    else if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        //要求されたパーミッションが外部ストレージ

                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // 許可された

                        } else {
                            // それでも拒否された時の対応
                            Toast.makeText(this,"外部書込の許可がないので書き込みできません",Toast.LENGTH_LONG).show();
                        }
                    }
                }

                locationServiceStartSetting();//位置情報サービス開始設定

            }
        }
    }


    //位置情報サービス開始設定
    private void locationServiceStartSetting() {

        textView = (TextView) findViewById(R.id.log_text);

        //スタートボタンの設定
        Button buttonStart = (Button) findViewById(R.id.button_start);
        buttonStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication(), LocationService.class);

                //サービスと位置情報取得を開始
                if(Build.VERSION.SDK_INT >= 26){
                    // API 26 以降
                    startForegroundService(intent);

                }else {
                    //API 26以下
                    startService(intent);

                }

                // Activityを終了させる(アプリがバッググラウンドに行きます)
                //finish();

            }
        });

        //リセットボタンの設定
        Button buttonReset = (Button) findViewById(R.id.button_reset);
        buttonReset.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // サービスと位置情報取得の停止
                Intent intent = new Intent(getApplication(), LocationService.class);
                stopService(intent);

                Toast.makeText(MainActivity.this, "GPS取得を停止します", Toast.LENGTH_SHORT).show();
            }
        });

        //ログボタンの設定
        Button buttonLog = (Button) findViewById(R.id.button_log);
        buttonLog.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 内部または外部ストレージからファイルを取得
                //テキストビューに書き込む
                String text = fileReadWrite.readFile();

                if(text.isEmpty()){
                    Toast.makeText(MainActivity.this, "ログはありません", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainActivity.this, "保存ログを読み込み、表示します", Toast.LENGTH_SHORT).show();
                    textView.setText(text);
                }
            }
        });

        //ログリセットボタンの設定
        Button buttonLogReset = (Button) findViewById(R.id.button_log_reset);
        buttonLogReset.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 内部または外部ストレージのファイルを空にする
                fileReadWrite.clearFile();
                //テキストビューを空にする
                textView.setText("");

                Toast.makeText(MainActivity.this, "ログを消します", Toast.LENGTH_SHORT).show();
            }
        });

        //設定画面移動ボタンの設定
        Button buttonMoveSetting = (Button) findViewById(R.id.button_setting);
        buttonMoveSetting.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(settingsIntent);
            }
        });
    }

    //Spinnerを作成する(Spinnerは、ドロップダウンリストを作成するためのもの)
    private void createSpinner(){

        // 分,秒のAdapter、ビューのコレクションの作成
        ArrayAdapter<Integer> adapterTime = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item);
        adapterTime.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // 最小距離感覚のAdapter、ビューのコレクションの作成
        ArrayAdapter<Integer> adapterDistance = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item);
        adapterDistance.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // モードのAdapter、ビューのコレクションの作成
        ArrayAdapter<String> adapterMode = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapterMode.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        // コレクションにアイテムを追加
        for(int i = 0;i <= 60;i++) adapterTime.add(i); //0〜60の整数代入
        for(int i = 0;i <= 100;i++) adapterDistance.add(i); //0〜100の整数代入
        adapterMode.add("高精度");
        adapterMode.add("GPS");
        adapterMode.add("NET");

        //各Spinnerビューを参照する
        Spinner spinner_min = (Spinner) findViewById(R.id.spinner_min);
        Spinner spinner_sec = (Spinner) findViewById(R.id.spinner_sec);
        Spinner spinner_distance = (Spinner) findViewById(R.id.spinner_distance);
        Spinner spinner_mode = (Spinner) findViewById(R.id.spinner_mode);

        //SpinnerにAdapterをセットする
        spinner_min.setAdapter(adapterTime);
        spinner_sec.setAdapter(adapterTime);
        spinner_distance.setAdapter(adapterDistance);
        spinner_mode.setAdapter(adapterMode);

        //初期値を入れる
        spinner_min.setSelection(LocationService.Min / 60000);
        spinner_sec.setSelection(LocationService.Sec / 1000);
        spinner_distance.setSelection(LocationService.Distance);
        spinner_mode.setSelection(LocationService.CURRENT_MODE);

        //spinner(分)をタッチしたときの処理
        spinner_min.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner = (Spinner) parent;
                // 選択したアイテムを取得
                int item = (int) spinner.getSelectedItem();
                //1分は60000ミリ秒
                LocationService.Min = item * 60000;

            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {// アイテムを選択しなかったとき
            }
        });

        //spinner(秒)をタッチしたときの処理
        spinner_sec.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner = (Spinner) parent;
                // 選択したアイテムを取得
                int item = (int) spinner.getSelectedItem();
                //1秒は1000ミリ秒
                LocationService.Sec = item * 1000;

            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {// アイテムを選択しなかったとき
            }
        });

        //spinner(メートル)をタッチしたときの処理
        spinner_distance.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner = (Spinner) parent;
                // 選択したアイテムを取得
                int item = (int) spinner.getSelectedItem();

                LocationService.Distance = item;

            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {// アイテムを選択しなかったとき

            }
        });

        //spinner(取得モード)をタッチしたときの処理
        spinner_mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner = (Spinner) parent;

                // 選択したアイテムを取得
                String item = (String) spinner.getSelectedItem();

                int itemNo;

                if(item == "高精度"){
                    itemNo = LocationService.HIGH_MODE;
                }
                else if(item == "GPS"){
                    itemNo = LocationService.GPS_MODE;
                }
                else {
                    itemNo = LocationService.NET_MODE;
                }

                LocationService.NEXT_MODE = itemNo;

            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {// アイテムを選択しなかったとき

            }
        });

    }

}