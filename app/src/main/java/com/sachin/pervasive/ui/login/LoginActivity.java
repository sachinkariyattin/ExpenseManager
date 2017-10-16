package com.sachin.pervasive.ui.login;

import android.content.Intent;
import android.os.Bundle;

import com.sachin.pervasive.R;
import com.sachin.pervasive.ui.BaseActivity;
import com.sachin.pervasive.ui.MainActivity;

public class LoginActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (true) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish();
        }
    }

}
