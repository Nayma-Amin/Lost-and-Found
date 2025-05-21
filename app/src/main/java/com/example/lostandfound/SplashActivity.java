package com.example.lostandfound;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        ImageView appLogo = findViewById(R.id.evLogo);

        ScaleAnimation scale = new ScaleAnimation(0.5f, 1.0f, 0.5f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(1500);

        RotateAnimation rotate = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(1500);

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(scale);
        animationSet.addAnimation(rotate);
        appLogo.startAnimation(animationSet);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, HomePage.class));
            finish();
        }, 3000);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10000);
            }
        }
        new Handler().postDelayed(() -> {
            Intent intent;

            String postId = getIntent().getStringExtra("postId");
            String eventId = getIntent().getStringExtra("eventId");

            if (postId != null) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.putExtra("postId", postId);
            } else {
                intent = new Intent(SplashActivity.this, HomePage.class);
            }

            startActivity(intent);
            finish();
        }, 3000);

    }
}

