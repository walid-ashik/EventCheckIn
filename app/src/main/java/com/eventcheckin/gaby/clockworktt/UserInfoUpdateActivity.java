package com.eventcheckin.gaby.clockworktt;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class UserInfoUpdateActivity extends AppCompatActivity {

    private static final int GALLERY_PICK = 1;
    private CircleImageView mUserImage;
    private Spinner mUserGender;
    private EditText mUserFirstName;
    private EditText mUserLastName;
    private EditText mUserFbUsername;
    private Button mUpdateButton;
    private FirebaseAuth mAuth;
    private String userId,gender;
    private DatabaseReference mUserInfoDataRef;
    private ProgressDialog progressDialog;
    private StorageReference mStorageUserImageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info_update);

        getSupportActionBar().hide();

        mUserImage = findViewById(R.id.user_image);
        mUserGender = findViewById(R.id.user_gender);
        mUserFirstName = findViewById(R.id.userFirstName);
        mUserLastName = findViewById(R.id.userLastName);
        mUserFbUsername = findViewById(R.id.userFbLink);
        mUpdateButton = findViewById(R.id.update_button);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading...");
        progressDialog.setCanceledOnTouchOutside(false);

        mAuth = FirebaseAuth.getInstance();

        if(mAuth.getCurrentUser() == null){
            startActivity(new Intent(UserInfoUpdateActivity.this,LoginActivity.class));
            finish();
        }

        userId = mAuth.getCurrentUser().getUid().toString();

        mUserInfoDataRef = FirebaseDatabase.getInstance().getReference().child("check_in_user").child(userId);
        mStorageUserImageRef = FirebaseStorage.getInstance().getReference().child("check_in_user_image");
        mUserImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent, "Choose Your Image"), GALLERY_PICK);

            }
        });

        mUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String first_name = mUserFirstName.getEditableText().toString();
                String last_name = mUserLastName.getEditableText().toString();
                String fb_link = mUserFbUsername.getEditableText().toString();

                mUserGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                        gender = adapterView.getSelectedItem().toString();

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });


                if(!first_name.isEmpty() && !last_name.isEmpty() && !fb_link.isEmpty())
                {
                    progressDialog.show();
                    Map map = new HashMap();
                    map.put("first_name", first_name);
                    map.put("last_name", last_name);
                    map.put("gender", gender);
                    map.put("fb_username", fb_link);
                    map.put("fb_link", "null");

                    mUserInfoDataRef.updateChildren(map).addOnCompleteListener(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {

                            if(task.isSuccessful()){
                                progressDialog.dismiss();
                                Toast.makeText(UserInfoUpdateActivity.this, "Updated Successfully", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(UserInfoUpdateActivity.this, MainActivity.class));
                                finish();

                            }else{
                                progressDialog.dismiss();
                                Toast.makeText(UserInfoUpdateActivity.this, "Please Try Again!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }else{
                    Toast.makeText(UserInfoUpdateActivity.this, "Please Input All Field", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_PICK && resultCode == RESULT_OK){

            Uri imageUri = data.getData();

            CropImage.activity(imageUri)
                    .setAspectRatio(1,1)
                    .start(this);

        }

        Bitmap thumb_icon;
        byte[] thumb_icon_data = new byte[0];

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            progressDialog.show();
            progressDialog.setCanceledOnTouchOutside(false);

            if(resultCode == RESULT_OK){
                Uri resultUri = result.getUri();

                File thumb_icon_file = new File(resultUri.getPath());


                try {
                    thumb_icon = new Compressor(this)
                            .setMaxHeight(200)
                            .setMaxWidth(200)
                            .setQuality(75)
                            .compressToBitmap(thumb_icon_file);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thumb_icon.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    thumb_icon_data = baos.toByteArray();


                } catch (IOException e) {
                    e.printStackTrace();
                }

                //TODO replace puskh key event
                StorageReference imagePath = mStorageUserImageRef.child(userId + ".jpg");
                final StorageReference thumbIconPath = mStorageUserImageRef.child("user_thumb_images").child(userId + ".jpg");

                final UploadTask uploadTask = thumbIconPath.putBytes(thumb_icon_data);

                imagePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                        final String imageDownloadUri = task.getResult().getDownloadUrl().toString();

                        if(task.isSuccessful()){

                            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                    if(task.isSuccessful()){

                                        String thumbImageDownloadUri = task.getResult().getDownloadUrl().toString();

                                        Map map = new HashMap();
                                        map.put("image", thumbImageDownloadUri);

                                        mUserInfoDataRef.updateChildren(map).addOnCompleteListener(new OnCompleteListener() {
                                            @Override
                                            public void onComplete(@NonNull Task task) {

                                                if(task.isSuccessful()){
                                                    progressDialog.dismiss();
                                                    //Icon Uploaded to FirebaseStorage
                                                    Toast.makeText(UserInfoUpdateActivity.this, "Image Successfully Uploaded!", Toast.LENGTH_SHORT).show();
                                                }else {
                                                    progressDialog.dismiss();
                                                    Toast.makeText(UserInfoUpdateActivity.this, "Uploading Fail...please try again!", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });

                                    }
                                }
                            });

                        }else {

                            progressDialog.dismiss();
                            Toast.makeText(UserInfoUpdateActivity.this, "Uploading Fail...please try again!", Toast.LENGTH_SHORT).show();
                        }

                    }
                });

            }

        }else if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
            Toast.makeText(this, "CROP IMAGE ACTIVITY GOT AN ERROR!", Toast.LENGTH_SHORT).show();
        }

    }


}
