package com.eventcheckin.gaby.clockworktt;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.crashlytics.android.Crashlytics;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.Result;

import io.fabric.sdk.android.Fabric;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity  {

    private FirebaseAuth mAuth;

    private Button mLogoutButton;
    private Button mCheckInButton;
    private String userId;
    private DatabaseReference CheckInUserRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        mCheckInButton = findViewById(R.id.check_in_button);
        mCheckInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivity(new Intent(MainActivity.this, ScannerActivity.class));

            }
        });
        mLogoutButton = findViewById(R.id.logout_button);

        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logOutUser();
            }
        });

        if(mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid().toString();

            CheckInUserRef = FirebaseDatabase.getInstance().getReference().child("check_in_user").child(userId);
            CheckInUserRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if(!dataSnapshot.hasChild("first_name")){

                        Intent updateInfoActivity = new Intent(MainActivity.this, UserInfoUpdateActivity.class);
                        startActivity(updateInfoActivity);
                        finish();

                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
    }



    public boolean userLoggedIn(){

        if(mAuth.getCurrentUser()!=null){
            return true;
        }else {
            return true;
        }

    }

    private void logOutUser() {

        if(mAuth.getCurrentUser() != null){

            mAuth.signOut();
            LoginManager.getInstance().logOut();

            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser == null){
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
        }

    }

}
