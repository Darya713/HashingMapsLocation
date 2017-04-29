package com.example.butterfly.maps;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

import static com.example.butterfly.maps.MapsActivity.directory;

class ServerParams{
    Context context;
    String device_id;
    int timer;

    ServerParams(Context context, String device_id, int timer) {
        this.context = context;
        this.device_id = device_id;
        this.timer = timer;
    }
}

public class Server extends AsyncTask<ServerParams, Void, String> {

    double latitude, longitude;
    String coords, hash;
    Context context;

    public Server(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(ServerParams... params) {

        GPSTracker gps = new GPSTracker(context);
        while (params[0].timer != 0) {
            if (gps.canGetLocation()) {
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();
                coords = latitude + "," + longitude;

                try {
                    OutputStream os = new FileOutputStream(directory.getPath() + "/" + "hash.txt");
                    byte[] data = coords.getBytes();
                    os.write(data);
                    os.close();
                } catch (IOException e){
                    Log.d("ERROR", e.getMessage());
                }

                try{
                    hash = new MyCryptography().getHash(directory.getPath() + "/" + "hash.txt");
                }
                catch (Throwable t){
                    t.printStackTrace();
                }
            } else {
                gps.showSettingsAlert();
            }

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("device_id", params[0].device_id);
                jsonObject.put("coords", coords);
                jsonObject.put("is_hash", "true");
                jsonObject.put("hashing_algorithm", "stb-31");
                jsonObject.put("hash", hash);
            }
            catch (Throwable t){
                t.printStackTrace();
            }

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://server.soikin.ru:3000/coords");

            try {
                StringEntity stringEntity = new StringEntity(jsonObject.toString());
                httpPost.setEntity(stringEntity);
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");
                HttpResponse httpResponse = httpClient.execute(httpPost);
                Handler handler = new Handler(params[0].context.getMainLooper());
                handler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(params[0].context, coords, Toast.LENGTH_SHORT).show();
                    }
                });
                TimeUnit.SECONDS.sleep(params[0].timer);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return null;
    }
}
