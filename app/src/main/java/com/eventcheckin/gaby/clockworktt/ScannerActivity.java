package com.eventcheckin.gaby.clockworktt;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.Result;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import static android.Manifest.permission.CAMERA;


public class ScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private static final int REQUEST_CAMERA = 1;
    private ZXingScannerView scannerView;
    private FirebaseAuth mAuth;

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        pd = new ProgressDialog(this);
        pd.setMessage("Checking in...");

        scannerView = new ZXingScannerView(this);
        setContentView(scannerView);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

            if(checkPermission()){

            }else{
                requestPermission();
            }

        }

    }//end onCreate()

    private void requestPermission() {

        ActivityCompat.requestPermissions(this, new String[]{CAMERA}, REQUEST_CAMERA);

    }

    private boolean checkPermission() {

        return ContextCompat.checkSelfPermission(ScannerActivity.this, CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public void onRequestPermissionResult(int requestCode, String permission[], int grantResults[]){

        switch (requestCode){

            case REQUEST_CAMERA :
                if(grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(cameraAccepted){
                    }else {

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                            if(shouldShowRequestPermissionRationale(CAMERA)){
                                displayAlertMessage("You need to allow access for both permissions", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(new String[]{CAMERA}, REQUEST_CAMERA);
                                        }

                                    }
                                });
                                return;
                            }

                        }

                    }
                }
                break;

        }

    }

    public void displayAlertMessage(String message, DialogInterface.OnClickListener listener){
        new AlertDialog.Builder(ScannerActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", listener)
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume(){
        super.onResume();

        if(checkPermission()){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                if(scannerView == null){
                    scannerView = new ZXingScannerView(this);
                    setContentView(scannerView);
                }
                scannerView.setResultHandler(this);
                scannerView.startCamera();

            }
        }else{
            requestPermission();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        scannerView.stopCamera();

    }

    DatabaseReference checkEventDataRef;

    @Override
    public void handleResult(Result result) {

        final String eventID = result.getText();

        if(!eventID.equals("")){

            scannerView.resumeCameraPreview(ScannerActivity.this);
            scannerView.stopCamera();


            checkEventDataRef = FirebaseDatabase.getInstance().getReference().child("Scan");
            checkEventDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.hasChild(eventID)){
                        pd.show();
                        //save user info to event guest list
                        saveUserInfoToScanEvents(eventID);
                    }

                    if(!dataSnapshot.hasChild(eventID)){
                        pd.dismiss();
                        scannerView.resumeCameraPreview(ScannerActivity.this);
                        scannerView.stopCamera();
                        showErrorMessage();
                        startActivity(new Intent(ScannerActivity.this, MainActivity.class));
                        finish();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }

    }

    private void showErrorMessage() {
        pd.dismiss();
        Toast.makeText(this, "Check In Failed...Contact To Event Organizer!", Toast.LENGTH_LONG).show();
        Toast.makeText(this, "Check In Failed...Contact To Event Organizer!", Toast.LENGTH_LONG).show();
    }

    DatabaseReference userInfoDataRef;
    String userId;
    String userFirstName;
    String userLastName;
    String userImage;
    String userFbLink;
    String userGender;

    private void saveUserInfoToScanEvents(final String eventID) {

        if(mAuth.getCurrentUser() != null){
            userId = mAuth.getCurrentUser().getUid();
        }

        userInfoDataRef = FirebaseDatabase.getInstance().getReference().child("check_in_user").child(userId);
        userInfoDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.hasChild("first_name")){
                   userFirstName = dataSnapshot.child("first_name").getValue().toString();
                }

                if(dataSnapshot.hasChild("last_name")){
                   userLastName = dataSnapshot.child("last_name").getValue().toString();
                }

                if(dataSnapshot.hasChild("gender")){
                   userGender = dataSnapshot.child("gender").getValue().toString();
                }

                if(dataSnapshot.hasChild("image")){
                   userImage = dataSnapshot.child("image").getValue().toString();
                }

                if(dataSnapshot.hasChild("fb_link")){
                   userFbLink = dataSnapshot.child("fb_link").getValue().toString();
                }

                Map map = new HashMap();
                map.put("first_name", userFirstName);
                map.put("last_name", userLastName);
                map.put("gender", userGender);
                map.put("image", userImage);
                map.put("fb_link", userFbLink);
                map.put("timestamp", ServerValue.TIMESTAMP);

                //Make Check In To event
                saveDataToEvent(eventID, userId, map);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void saveDataToEvent(String eventID, String userId, Map map) {

        DatabaseReference eventDataRef = FirebaseDatabase.getInstance().getReference().child("Scan").child(eventID).child("guest_list").child(userId);
        eventDataRef.setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if(task.isSuccessful()){

                    pd.dismiss();

                    //Show Successful Message
                    Toast.makeText(ScannerActivity.this, "You've checked in this event!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(ScannerActivity.this, MainActivity.class));
                    finish();
                }else {
                    showErrorMessage();
                }

            }
        });

    }
}
