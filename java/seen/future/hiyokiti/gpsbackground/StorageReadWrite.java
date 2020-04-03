package seen.future.hiyokiti.gpsbackground;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Androidで保存する領域は決まった場所に保存されます
 * getExternalFilesDirは外部（ＳＤカード）がなければ内部に保存されます
 * アプリをアンインストールするとファイルは消えます
 *
 * ログ保存先(外部):/storage/sdcard0/Android/data/パッケージ名/files/Documents
 * ログ保存先(内部):/Android/data/パッケージ名/files/Documents
 * 設定の保存はデータが少ないのでSharedPreferences(少ないデータ向き)でアプリ内に保存しています。
 */

class StorageReadWrite {

    private File file;
    private StringBuffer stringBuffer;

    StorageReadWrite(Context context) {
        File path;
        //外部または内部ストレージのDocumentsのパスを持ってくる,なければ作ります。
        if (Build.VERSION.SDK_INT >= 19) {
            //Android 4.4 API19以降
            path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        }else{

            path = context.getExternalFilesDir("/Documents");
        }

        file = new File(path, "log.txt");
    }


    // ファイルをクリア
    void clearFile(){
        writeFile("", false);

        // StringBuffer clear
        if(stringBuffer != null)stringBuffer.setLength(0);
    }


    // ファイルを保存
    void writeFile(String gpsLog, boolean mode) {
        if(isExternalStorageWritable()){
            try{
                //指定された名前のファイルに書き込むためのファイル出力ストリームを作成します。
                // 第一引数はファイル名のみの指定です。第二引数のモードはtrueの場合、バイトはファイルの先頭ではなく最後に書き込まれます。
                FileOutputStream fileOutputStream = new FileOutputStream(file, mode);

                //文字ストリームからバイト・ストリームへ
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);

                //バッファリングする
                BufferedWriter bw = new BufferedWriter(outputStreamWriter);

                //書き込む
                bw.write(gpsLog);

                //ストリームをフラッシュします。
                bw.flush();

                bw.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    // ファイルを読み出し
    String readFile() {
        stringBuffer = new StringBuffer();

        // 現在ストレージが読出しできるかチェック
        if(isExternalStorageReadable()){

            try {
                FileInputStream fileInputStream = new FileInputStream(file);

                //バイト・ストリームから文字ストリームへ
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

                BufferedReader reader= new BufferedReader(inputStreamReader);

                String lineBuffer;

                //1行ずつ読み込む
                while( (lineBuffer = reader.readLine()) != null ) {
                    stringBuffer.append(lineBuffer);
                    stringBuffer.append(System.getProperty("line.separator"));//改行コード
                }

            } catch (Exception e) {
                stringBuffer.append("error: FileInputStream");
                e.printStackTrace();
            }
        }

        return stringBuffer.toString();
    }


    /* 外部ストレージが読み取りおよび書き込みに使用可能かどうかを確認します */
    boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state));
    }


    /* 外部ストレージが少なくとも読み取り可能かどうかを確認します */
    boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }


    //設定の保存
    void saveConfigu(Context context){
        SharedPreferences data = PreferenceManager.getDefaultSharedPreferences(context);
        data.edit().clear().commit();
        data.edit().putInt("Min",LocationService.Min).commit();
        data.edit().putInt("Sec",LocationService.Sec).commit();
        data.edit().putInt("Distance",LocationService.Distance).commit();
        data.edit().putInt("Mode",LocationService.CURRENT_MODE).commit();
    }


    //設定の読み込み
    void loadConfigu(Context context){
        SharedPreferences data = PreferenceManager.getDefaultSharedPreferences(context);
        LocationService.Min = data.getInt("Min",0);
        LocationService.Sec = data.getInt("Sec",0);
        LocationService.Distance = data.getInt("Distance",1);
        LocationService.CURRENT_MODE = data.getInt("Mode",LocationService.HIGH_MODE);
    }

}
