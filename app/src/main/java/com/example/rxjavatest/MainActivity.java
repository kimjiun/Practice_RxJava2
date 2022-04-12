package com.example.rxjavatest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.rxjavatest.ch1.Ch1_MainActivity;
import com.example.rxjavatest.ch2.Ch2_MainActivity;
import com.example.rxjavatest.ch3.Ch3_MainActivity;
import com.example.rxjavatest.ch5.Ch5_MainActivity;
import com.example.rxjavatest.ch6.Ch6_MainActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startActivity(new Intent(this, Ch6_MainActivity.class));
    }
}