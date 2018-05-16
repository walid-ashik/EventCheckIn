package com.eventcheckin.gaby.clockworktt;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "FACELOG";
    private CallbackManager mCallbackManager;

    private FirebaseAuth mAuth;
    private Button mFacebookBtn;
    private EditText mEmail;
    private EditText mPassword;
    private Button mSignUp;
    private Button mLogin;
    private TextView mForgotPassword;
    private ProgressDialog pd;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // Initialize Facebook Login button
        mCallbackManager = CallbackManager.Factory.create();
        mFacebookBtn = findViewById(R.id.login_button);
        mEmail = findViewById(R.id.emailEditText);
        mPassword = findViewById(R.id.passwordEditText);
        mSignUp = findViewById(R.id.sign_up_button);
        mLogin = findViewById(R.id.login_button_password);
        mForgotPassword = findViewById(R.id.forgot_password);

        pd = new ProgressDialog(this);
        pd.setMessage("Logging...");
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");


        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = mEmail.getText().toString();
                String password = mPassword.getText().toString();

                if(!email.equals("") && !password.equals("")){

                    loginUser(email, password);

                }
                else {
                    checkEmailPasswordToast();
                }

            }
        });

        mSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = mEmail.getText().toString();
                String password = mPassword.getText().toString();

                if(!email.equals("") && !password.equals("")){

                    signUpUser(email, password);

                }else {
                    checkEmailPasswordToast();
                }

            }
        });


        mFacebookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mFacebookBtn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary
                ));
                mFacebookBtn.setText("Logging...");
                mFacebookBtn.setEnabled(false);

                LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this, Arrays.asList("email", "public_profile"));
                LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        mFacebookBtn.setEnabled(true);
                        mFacebookBtn.setText("Login with Facebook");

                        pd.show();

                        Log.d(TAG, "facebook:onSuccess:" + loginResult);
                        handleFacebookAccessToken(loginResult.getAccessToken(), loginResult);

                    }

                    @Override
                    public void onCancel() {
                        mFacebookBtn.setEnabled(true);
                        mFacebookBtn.setText("Login with Facebook");
                        pd.dismiss();
                        Log.d(TAG, "facebook:onCancel");
                        // ...
                    }

                    @Override
                    public void onError(FacebookException error) {
                        mFacebookBtn.setEnabled(true);
                        pd.dismiss();
                        mFacebookBtn.setText("Login with Facebook");
                        Log.d(TAG, "facebook:onError", error);
                        // ...
                    }
                });

            }
        });

        mForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //TODO add alerDialog and get user to input email and submit to change password

                AlertDialog.Builder mBuilder = new AlertDialog.Builder(LoginActivity.this);
                View forgot_password_view = getLayoutInflater().inflate(R.layout.forgot_password_email_input_layout, null);
                final EditText mForgotPasswordEditText = forgot_password_view.findViewById(R.id.forgot_password_edit_text);
                Button mResetPasswordButton = forgot_password_view.findViewById(R.id.forgot_password_reset_button);

                mForgotPasswordEditText.setText(mEmail.getText().toString());

                mBuilder.setView(forgot_password_view);
                final AlertDialog dialog = mBuilder.create();
                dialog.show();

                mResetPasswordButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String resetEmail = mForgotPasswordEditText.getText().toString();

                        if(!TextUtils.isEmpty(resetEmail)){

                            sendResetPassword(resetEmail, dialog);

                        }else {
                            Toast.makeText(LoginActivity.this, "Provide Your Email Address", Toast.LENGTH_SHORT).show();
                        }

                    }
                });



            }
        });

    }

    private void checkEmailPasswordToast() {

        Toast.makeText(this, "Check your Email and Password",Toast.LENGTH_SHORT).show();

    }

    private void sendResetPassword(String resetEmail, final AlertDialog dialog) {

        mAuth.sendPasswordResetEmail(resetEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    dialog.dismiss();
                    Toast.makeText(LoginActivity.this, "We have sent you instructions to reset your password!",Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Failed to send reset email!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void signUpUser(String email, String password) {

        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if(task.isSuccessful()) {
                    Log.d(TAG, "onComplete: " + "user created");
                    FirebaseAuth.getInstance().getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {

                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            Log.d(TAG, "onComplete: verification email sent");

                            if(task.isSuccessful()){
                                progressDialog.dismiss();
                                FirebaseAuth.getInstance().signOut();
                                Log.d(TAG, "onComplete: sign out user");
                                Toast.makeText(LoginActivity.this, "Please check your email and Verify that", Toast.LENGTH_SHORT).show();
                                moveTaskToBack(true);
                                finish();
                            }else{

                                Log.d(TAG, "onComplete: failed to sent verification email");
                                Toast.makeText(LoginActivity.this, "Check your email! Failed to send verification link", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

                }else {
                    Log.d(TAG, "onComplete: failed to create user");
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "error in creating account!", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    private void loginUser(final String email, String password) {

        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                Log.d(TAG, "onComplete: logging user");

                if(task.isSuccessful()){

                    Log.d(TAG, "onComplete: logged in successfully");

                    if(FirebaseAuth.getInstance().getCurrentUser().isEmailVerified()){

                        Log.d(TAG, "onComplete: user verified");

                        if(FirebaseAuth.getInstance().getCurrentUser() != null){

                            final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                            DatabaseReference userDataRef = FirebaseDatabase.getInstance().getReference().child("check_in_user").child(userId);
                            userDataRef.child("email").setValue(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if(task.isSuccessful()){

                                        progressDialog.dismiss();

                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    }else{

                                        progressDialog.dismiss();
                                        Toast.makeText(LoginActivity.this, "Login Failed! Please check your connection!", Toast.LENGTH_SHORT).show();

                                    }

                                }
                            });

                        }

                    }else {
                        progressDialog.dismiss();
                        Log.d(TAG, "onComplete: user unverified");
                        FirebaseAuth.getInstance().signOut();
                        Toast.makeText(LoginActivity.this, "Verify Email address first!", Toast.LENGTH_SHORT).show();
                        moveTaskToBack(true);
                        finish();
                    }


                } else {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Error in signing in!", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result back to the Facebook SDK
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){

            updateUI();

        }

    }

    private void updateUI() {

        Toast.makeText(this, "You're logged in", Toast.LENGTH_SHORT).show();
        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();

    }

    private void handleFacebookAccessToken(AccessToken token, final LoginResult loginResult) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            setFacebookData(loginResult, user);
//                            updateUI();
                        } else {
                            pd.dismiss();
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                        }

                        // ...
                    }
                });
    }


    String profilePic;

    private void setFacebookData(final LoginResult loginResult, final FirebaseUser user)
    {
        GraphRequest request = GraphRequest.newMeRequest(
                loginResult.getAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        // Application code
                        try {
                            Log.i("Response",response.toString());

                            String email = response.getJSONObject().getString("email");
                            String firstName = response.getJSONObject().getString("first_name");
                            String lastName = response.getJSONObject().getString("last_name");
                            String gender = response.getJSONObject().getString("gender");



                            Profile profile = Profile.getCurrentProfile();
                            String id = profile.getId();
                            String link = profile.getLinkUri().toString();
                            Log.i("Link",link);

                            if (Profile.getCurrentProfile()!=null)
                            {
                                profilePic = Profile.getCurrentProfile().getProfilePictureUri(200, 200).toString();
                            }else {
                                profilePic = "default";
                            }

                            //Save To Database
                            saveToDatabase(email, firstName, lastName, gender, link, profilePic, user);

                            Log.i("Login" + "Email", email);
                            Log.i("Login"+ "FirstName", firstName);
                            Log.i("Login" + "LastName", lastName);
                            Log.i("Login" + "Gender", gender);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,email,first_name,last_name,gender");
        request.setParameters(parameters);
        request.executeAsync();
    }

    String userId;

    private void saveToDatabase(String email, String firstName, String lastName, String gender, String link, String profilePic, FirebaseUser user) {

        userId = user.getUid();

        DatabaseReference mFacebookUserDataRef = FirebaseDatabase.getInstance().getReference().child("check_in_user");

        HashMap map = new HashMap();
        map.put("first_name", firstName);
        map.put("last_name", lastName);
        map.put("email", email);
        map.put("gender", gender);
        map.put("fb_link", link);
        map.put("image", profilePic);

        mFacebookUserDataRef.child(userId).setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if(task.isSuccessful()){

                    pd.dismiss();
                    updateUI();
                    Log.d(TAG, "onComplete: info saved to database");
                }else
                    pd.dismiss();
                    Log.d(TAG, "onComplete: nasty error occurred while saving info to database");

            }
        });

    }

}
