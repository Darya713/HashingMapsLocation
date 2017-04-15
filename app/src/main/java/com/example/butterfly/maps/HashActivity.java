package com.example.butterfly.maps;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class HashActivity extends AppCompatActivity {

    TextView textView_hash;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hash);

        textView_hash = (TextView) findViewById(R.id.textView_hash);
        textView_hash.setText(getIntent().getStringExtra("UID") +
                "\n" + getIntent().getStringExtra("hash") +
                "\nTime: " + (Long.parseLong(getIntent().getStringExtra("stop")) -
                Long.parseLong(getIntent().getStringExtra("start")))/1000 +
                "\n\n" + getIntent().getStringExtra("location"));

    }
}
