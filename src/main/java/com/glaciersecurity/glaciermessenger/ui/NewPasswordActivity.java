/*
 * Copyright 2013-2017 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the
 * License. A copy of the License is located at
 *
 *      http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License
 * for the specific language governing permissions and
 * limitations under the License.
 */

package com.glaciersecurity.glaciermessenger.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.cognito.AppHelper;

import java.util.regex.Pattern;

public class NewPasswordActivity extends AppCompatActivity {
    private String TAG = "NewPassword";
    private EditText newPassword;
    private TextInputLayout newPasswordLayout;
    private EditText reNewPassword;
    private TextInputLayout reNewPasswordLayout;

    private Button continueSignIn;
    private AlertDialog userDialog;
    private ProgressDialog waitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(false);
            ab.setDisplayHomeAsUpEnabled(false);
        }

        init();
    }

    @Override
    public void onBackPressed() {
        exit(false);
    }

    private void init() {
        newPassword = (EditText) findViewById(R.id.editTextNewPassPass);

        this.newPasswordLayout = (TextInputLayout) findViewById(R.id.new_password_layout);
        this.newPasswordLayout.setError(null);

        this.reNewPassword = (EditText) findViewById(R.id.reEditTextNewPassPass);

        this.reNewPasswordLayout = (TextInputLayout) findViewById(R.id.re_new_password_layout);
        this.reNewPasswordLayout.setError(null);

        continueSignIn = (Button) findViewById(R.id.buttonNewPass);
        continueSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeAllErrors();
                final String newUserPassword = newPassword.getText().toString();
                final String reNewUserPassword = reNewPassword.getText().toString();
                if (newUserPassword != null) {
                    if (newUserPassword.isEmpty()) {
                        newPassword.requestFocus();
                        newPassword.setError(getString(R.string.password_should_not_be_empty));
                    } else if (!isValid(newUserPassword)) {
                        newPassword.requestFocus();
                        newPassword.setError(getString(R.string.password_not_valid));
                    } else if (!newUserPassword.equals(reNewUserPassword)) {
                        reNewPassword.requestFocus();
                        reNewPassword.setError(getString(R.string.passwords_do_not_match));
                    } else {
                        setPasswordForFirsTimeLogin(newUserPassword);
                    }
                } else {
                    showDialogMessage("Error", "Enter all required attributed", false);
                }
            }
        });
    }

    private void setPasswordForFirsTimeLogin(String newUserPassword){
        AppHelper.setPasswordForFirstTimeLogin(newUserPassword);
        if (checkAttributes()) {
            newPasswordLayout.setError(null);
            removeAllErrors();
            exit(true);
        } else {
            newPasswordLayout.setError("Error");
            showDialogMessage("Error", "Enter all required attributed", false);
        }
    }

    private boolean checkAttributes() {
        // Check if all required attributes have values
        return true;
    }

    public static boolean isValid(String passwordhere) {
        Pattern specailCharPatten = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Pattern UpperCasePatten = Pattern.compile("[A-Z ]");
        Pattern lowerCasePatten = Pattern.compile("[a-z ]");
        Pattern digitCasePatten = Pattern.compile("[0-9 ]");
        //errorList.clear();

        boolean flag = true;
        if (passwordhere.length() < 7) {
            flag = false;
        }
        if (!UpperCasePatten.matcher(passwordhere).find()) {
            //errorList.add("Password must have at least one uppercase character !!");
            flag = false;
        }
        if (!lowerCasePatten.matcher(passwordhere).find()) {
            //errorList.add("Password must have at least one lowercase character !!");
            flag = false;
        }
        if (!digitCasePatten.matcher(passwordhere).find()) {
            //errorList.add("Password must have at least one digit character !!");
            flag = false;
        }
        return flag;
    }


    private void showDialogMessage(String title, String body, final boolean exit) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(body).setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    userDialog.dismiss();
                    if (exit) {
                        exit(false);
                    }
                } catch (Exception e) {
                    exit(false);
                }
            }
        });
        userDialog = builder.create();
        userDialog.show();
    }

    private void exit(Boolean continueWithSignIn) {
        Intent intent = new Intent();
        intent.putExtra("continueSignIn", continueWithSignIn);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void removeErrorsOnAllBut(TextInputLayout exception) {
        if (this.newPasswordLayout != exception) {
            this.newPasswordLayout.setErrorEnabled(false);
            this.newPasswordLayout.setError(null);
        }

        if (this.reNewPasswordLayout != exception) {
            this.reNewPasswordLayout.setErrorEnabled(false);
            this.reNewPasswordLayout.setError(null);
        }
    }
    private void removeAllErrors() {
            this.newPasswordLayout.setErrorEnabled(false);
            this.newPasswordLayout.setError(null);
            this.reNewPasswordLayout.setErrorEnabled(false);
            this.reNewPasswordLayout.setError(null);
            this.newPassword.setError(null);
            this.reNewPassword.setError(null);
    }

}
