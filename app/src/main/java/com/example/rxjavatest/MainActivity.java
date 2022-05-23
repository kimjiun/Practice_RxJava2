package com.example.rxjavatest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.example.rxjavatest.ch1.Ch1_MainActivity;
import com.example.rxjavatest.ch11.Ch11_MainActivity;
import com.example.rxjavatest.ch2.Ch2_MainActivity;
import com.example.rxjavatest.ch3.Ch3_MainActivity;
import com.example.rxjavatest.ch5.Ch5_MainActivity;
import com.example.rxjavatest.ch6.Ch6_MainActivity;
import com.example.rxjavatest.ch7.Ch7_MainActivity;
import com.example.rxjavatest.ch8.Ch8_MainActivity;
import com.example.rxjavatest.ch9.Ch9_MainActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.goBtn)
    Button goBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);

        ButterKnife.bind(this);

        goBtn.setOnClickListener(v -> {startActivity(new Intent(this, Ch8_MainActivity.class));});

        startActivity(new Intent(this, Ch11_MainActivity.class));
    }
}