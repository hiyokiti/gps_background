package seen.future.hiyokiti.gpsbackground;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.core.app.ActivityCompat;

/**
 * 高精度モード：GPSとネットワークを使用（電力消費大）
 * ＧＰＳモード：GPSを使用（電力消費中）
 * ＮＥＴモード：ネットワークを使用(電力消費小)
 */

public class LocationService extends Service implements LocationListener{

    public static final int HIGH_MODE = 0;//高精度モードの定数
    public static final int GPS_MODE = 1;//GPSモードの定数
    public static final int NET_MODE = 2;//ネットワークモードの定数
    public static int CURRENT_MODE = 0;//現在のモード
    public static int NEXT_MODE = 0;//次のモード

    public static int Min = 0; //通知の最小時間間隔 (ミリ秒)
    public static int Sec = 0;
    public static int Distance = 1;//通知の最小距離感覚 (メートル)

    private LocationManager locationManager;//システムロケーションサービスへのアクセス
    private Context context;
    private StorageReadWrite fileReadWrite;//ファイル読み書き用クラス

    private int counter; //受信した回数をカウント


    //サービスが最初に作成された時のよばれます
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        // 外部または内部ストレージにログを保存
        fileReadWrite = new StorageReadWrite(context);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

    }


    //startServiceでサービスが開始要求を受けたときのよばれます。
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //API26以降はバッグラウンドでGPSの取得制限がかかっているため
        //ステータスバーにアイコンを表示することでフォアグラウンド扱いにします。
        if(Build.VERSION.SDK_INT >= 26){
            int requestCode = 0;
            String channelId = "default";//チャネルの文字列ID
            String title = "タイトルです";//ステータスバーに表示される名前

            // MainActivityのインテントを作成します。
            Intent resultIntent = new Intent(this, MainActivity.class);

            //通知がクリックされた時にアクティビティを起動する
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(context, requestCode, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);


            // ForegroundにするためNotificationが必要、Contextを設定
            NotificationManager notificationManager =
                    (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

            // Notification　Channel 設定
            NotificationChannel channel = new NotificationChannel(
                    channelId, title , NotificationManager.IMPORTANCE_DEFAULT);

            //このチャネルのユーザーに表示される説明を設定します。
            channel.setDescription("Silent Notification");

            // 通知音を消さないと毎回通知音が出てしまう
            channel.setSound(null,null);

            // 通知ランプを消す
            channel.enableLights(false);

            //通知ライトの色を設定 します。
            channel.setLightColor(Color.BLUE);

            // 通知バイブレーション無し
            channel.enableVibration(false);


            if(notificationManager != null) {
                //通知を投稿できる通知チャネルを作成します。
                notificationManager.createNotificationChannel(channel);
                Notification notification = new Notification.Builder(context, channelId)
                        // ステータスバーのアイコン設定
                        .setSmallIcon(android.R.drawable.btn_star)
                        //プラットフォーム通知のテキストの最初の行を設定します。
                        .setContentTitle(title)
                        //プラットフォーム通知のテキストの2行目を設定します。
                        .setContentText("内容です。")
                        //この通知は、ユーザーが触れると自動的に却下されます。
                        .setAutoCancel(true)
                        //通知をクリックしたときに送信されます。
                        .setContentIntent(pendingIntent)
                        //通知に関連するタイムスタンプ（通常はイベントが発生した時間）を追加します。
                        .setWhen(System.currentTimeMillis())
                        //設定されているすべてのオプションを組み合わせて、新しいNotification オブジェクトを返します。
                        .build();


                //サービスをフォアグラウンド状態(ステータスバーにアイコンを表示)にする
                startForeground(1, notification);
            }
        }

        //高精度以外の現在と違うモード選択時は
        //前モードを消すためにstopGpsを呼びます
        if(NEXT_MODE != CURRENT_MODE &&
                NEXT_MODE != HIGH_MODE){
            stopGPS();
        }

        CURRENT_MODE = NEXT_MODE;

        //位置情報取得開始
        if(CURRENT_MODE == HIGH_MODE){
            //高精度モード
            startGpsAndNetwork();
        }
        else if(CURRENT_MODE == GPS_MODE){
            //Gpsモード
            startGps();
        }
        else if(CURRENT_MODE == NET_MODE){
            //ネットワークモード
            startNetwork();
        }

        return START_NOT_STICKY;
    }

    //GPSとネットワークの両方で取得開始
    private void startGpsAndNetwork() {

        //GPS設定がオンになっているかしらべる
        final boolean gpsEnabled = locationManager.
                isProviderEnabled(LocationManager.GPS_PROVIDER);
        //ネットワーク設定がオンになっているか調べる
        final boolean netEnabled = locationManager.
                isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled || !netEnabled) {
            // GPSまたはネットワークの設定がオフだったら、位置情報を許可する画面に移動する
            enableLocationSettings();
        }

        if (locationManager != null) {
            try {
                //GPSとネットワークのパーミッションチェック
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)!=
                        PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_COARSE_LOCATION)!=
                                PackageManager.PERMISSION_GRANTED)
                {
                    // GPSとネットワークを許可してなければ、取得しない
                    return;
                }

                //Gps取得開始
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        Min + Sec, Distance, this);
                //ネットワーク取得開始
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        Min + Sec, Distance, this);
                Toast.makeText(LocationService.this,
                        "GPSとネットワークで取得を開始します", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //GPSで取得開始
    private void startGps(){
        //GPS設定がオンになっているかしらべる
        final boolean gpsEnabled = locationManager.
                isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled) {
            // GPSの設定がオフだったら、位置情報を許可する画面に移動する
            enableLocationSettings();
        }

        if (locationManager != null) {
            try {
                //requestLocationUpdatesを呼ぶ前にパーミッションチェックが必要
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)!=
                        PackageManager.PERMISSION_GRANTED)
                {
                    // 位置情報取得を許可してなければ、Gps取得しない
                    return;
                }
                //Gps取得開始
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        Min + Sec, Distance, this);

                Toast.makeText(LocationService.this,
                        "GPS取得を開始します", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //ネットワークで取得開始
    private void startNetwork(){
        //ネットワーク設定がオンになっているかしらべる
        final boolean netEnabled = locationManager.
                isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!netEnabled) {
            // ネットワークの設定がオフだったら、位置情報を許可する画面に移動する
            enableLocationSettings();
        }

        if (locationManager != null) {
            try {
                //requestLocationUpdatesを呼ぶ前にパーミッションチェックが必要
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)!=
                        PackageManager.PERMISSION_GRANTED)
                {
                    // 位置情報取得を許可してなければ、ネットワーク取得しない
                    return;
                }
                //ネットワーク取得開始
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        Min + Sec, Distance, this);
                Toast.makeText(LocationService.this,
                        "ネットワーク取得を開始します", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    //位置情報の取得・更新　(同じ場所にいると呼ばれにくいです)
    @Override
    public void onLocationChanged(Location location) {

        //位置情報が更新されるたびにトーストで表示するようにしました
        counter++;
        //受信した回数
        String text = Integer.toString(counter);
        //緯度
        String latitude = Double.toString(location.getLatitude());
        //経度
        String longitude = Double.toString(location.getLongitude());
        //トースト
        Toast.makeText(context,"緯度　：　" + latitude + "\n"
                + "経度　：　" + longitude + "\n"
                + "受信カウント　＝　" + text, Toast.LENGTH_SHORT).show();


        /**********************ログを保存***********************************/
        StringBuilder strBuf = new StringBuilder();

        strBuf.append("----------\n");

        // 現在の時刻を取得
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy'年'MM'月'dd'日'　kk'時'mm'分'ss'秒'" + "\n");
        strBuf.append(sdf.format(date));

        //緯度を取得します。
        String str = "緯度 = " + String.valueOf(location.getLatitude()) + "\n";
        strBuf.append(str);

        //経度を取得します。
        str = "経度 = " + String.valueOf(location.getLongitude()) + "\n";
        strBuf.append(str);

        //この位置の推定水平精度を半径でメートル単位で取得します。
        str = "精度 = " + String.valueOf(location.getAccuracy()) + "m" + "\n";
        strBuf.append(str);

        //可能な場合は、WGS 84基準楕円体より上のメートルで高度を取得します。
        str = "高度 = " + String.valueOf(location.getAltitude()) + "m" + "\n";
        strBuf.append(str);

        //速度が利用可能な場合は、地上のメートル/秒で速度を取得します。
        str = "速度 = " + String.valueOf(location.getSpeed()) + "m/s"+ "\n";
        strBuf.append(str);

        //度で方位を取得します。
        str = "方位 = " + String.valueOf(location.getBearing()) + "\n";
        strBuf.append(str);

        strBuf.append("----------\n");

        //log.txtの一番最後に保存
        fileReadWrite.writeFile(strBuf.toString(), true);
        /**********************ログを保存***********************************/

    }


    //プロバイダーがユーザーによって無効にされたときに呼び出されます。
    @Override
    public void onProviderDisabled(String provider) {
    }


    //ユーザーがプロバイダーを有効にすると呼び出されます。
    @Override
    public void onProviderEnabled(String provider) {
    }


    //利用可能なロケーションプロバイダの利用状況が変わると呼ばれます(このメソッドはAPIレベル29で廃止されました)
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }


    //位置情報を許可する画面に移動する
    private void enableLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(settingsIntent);
    }


    //位置情報取得を止める
    private void stopGPS(){
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED)
            {
                // 位置情報取得を許可してなければ、なにもしない
                return;
            }

            //位置情報取得を止める
            locationManager.removeUpdates(this);

        }
    }


    //サービスが破棄される時によばれます。
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopGPS();
    }


    //bindServiceサービスがバインドされた時よばれます
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
