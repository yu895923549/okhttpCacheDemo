package com.example.destiny.cachetest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

import com.example.destiny.cachetest.model.TimeBean;
import com.example.destiny.cachetest.utils.OkHttp3Utils;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Button mButton;
    private TextView mText1;
    private TextView mText2;
    private String URL = "http://api.k780.com:88/?app=life.time&appkey=10003&sign=b59bc3ef6191eb9f747dd4e83c99f2a4&format=json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mButton = (Button) findViewById(R.id.button);
        mText1 = (TextView) findViewById(R.id.text1);
        mText2 = (TextView) findViewById(R.id.text2);
        mButton.setOnClickListener(view -> {
            Request request = new Request.Builder()
                    .get()
                    .url(URL)
                    .build();
            OkHttp3Utils.getOKhttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call1, IOException e) {

                }

                @Override
                public void onResponse(Call call1, Response response) throws IOException {
                    String s = response.body().string().toString();
                    TimeBean timeBean = new Gson().fromJson(s, TimeBean.class);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    mText1.post(() -> mText1.setText(simpleDateFormat.format(new Date())));

                    if (OkHttp3Utils.isNetworkReachable(MainActivity.this)) {
                        mText2.post(() -> mText2.setText(timeBean.getResult().getDatetime_1()));
                    } else {
                        mText2.post(() -> mText2.setText(timeBean.getResult().getDatetime_1() + "（缓存数据）"));
                    }

                }
            });

        });
    }

}