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
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.glaciersecurity.glaciermessenger.R;

import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {
    private EditText newPassword;
    private TextInputLayout newPasswordLayout;
    private EditText reNewPassword;
    private TextInputLayout reNewPasswordLayout;

    private EditText newUsername;
    private TextInputLayout newUsernameLayout;

    private EditText email;
    private TextInputLayout emailLayout;

    private Button continueSignIn;
    private AlertDialog userDialog;
    private ProgressDialog waitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_account);

        newUsername = (EditText) findViewById(R.id.signup_username);
        newPassword = (EditText) findViewById(R.id.signup_password);
        reNewPassword = (EditText) findViewById(R.id.resignup_password);
        email = (EditText) findViewById(R.id.signup_email);
        newUsernameLayout = (TextInputLayout) findViewById(R.id.signup_username_layout);
        newPasswordLayout = (TextInputLayout) findViewById(R.id.signup_password_layout);
        reNewPasswordLayout = (TextInputLayout) findViewById(R.id.resignup_password_layout);
        emailLayout = (TextInputLayout) findViewById(R.id.signup_email_layout);
        clearLayoutErrors();

        continueSignIn = (Button) findViewById(R.id.continueSignIn);
        continueSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeAllErrors();

                final String emailInput = email.getText().toString();
                if (emailInput != null) {
                    if (emailInput.isEmpty()) {
                        showDialogMessage(getResources().getString(R.string.sign_up_email), getResources().getString(R.string.recovery_email_message_full));
                    }
                }
                if (isValidPasswords( ) && isValidUsername()) {


                } else {
                    //showDialogMessage("Error", "Enter all required attributed", false);
                }
            }
        });

        checkEnabledSignUpButton();

        newUsername.addTextChangedListener(signupTextWatcher);
        newPassword.addTextChangedListener(signupTextWatcher);
        reNewPassword.addTextChangedListener(signupTextWatcher);
        email.addTextChangedListener(signupTextWatcher);


    }


    private void clearLayoutErrors() {
        this.newUsernameLayout.setError(null);
        this.newPasswordLayout.setError(null);
        this.reNewPasswordLayout.setError(null);
        this.emailLayout.setError(null);

    }


    private TextWatcher signupTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkEnabledSignUpButton();

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    public void checkEnabledSignUpButton(){
        String usernameInput = newUsername.getText().toString().trim();
        String passwordInput = newPassword.getText().toString().trim();
        String repasswordInput = reNewPassword.getText().toString().trim();

        continueSignIn.setEnabled(!usernameInput.isEmpty() && !passwordInput.isEmpty()&& !repasswordInput.isEmpty());
        if (!continueSignIn.isEnabled()){
            continueSignIn.setBackground(getResources().getDrawable(R.drawable.btn_rounded_gray_300));
        } else {
            continueSignIn.setBackground(getResources().getDrawable(R.drawable.btn_rounded_accent_300));
        }
    }
    private boolean isValidPasswords(){
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
                return true;
            }
        }
        return false;
    }

    private boolean isValidUsername(){
        final String userNameInput = newUsername.getText().toString();

        if (userNameInput != null) {
                newUsername.requestFocus();
                newUsername.setError(getString(R.string.invalid_username));
//            } else if (!isValid(newUserPassword)) {
//                newPassword.requestFocus();
//                newPassword.setError(getString(R.string.password_not_valid));
            } else {
                return true;
            }

        return false;
    }
//
//    private void setPasswordForFirsTimeLogin(String newUserPassword){
//        AppHelper.setPasswordForFirstTimeLogin(newUserPassword);
//        if (checkAttributes()) {
//            newPasswordLayout.setError(null);
//            removeAllErrors();
//            exit(true);
//        } else {
//            newPasswordLayout.setError("Error");
//            showDialogMessage("Error", "Enter all required attributed", false);
//        }
//    }
//
//    private boolean checkAttributes() {
//        // Check if all required attributes have values
//        return true;
//    }
//
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
//
//
    private void showDialogMessage(String title, String body) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(body).setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //TODO continue signup process
            }
        });
        builder.setTitle(title).setMessage(body).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        userDialog = builder.create();
        userDialog.show();
    }
//
//    private void exit(Boolean continueWithSignIn) {
//        Intent intent = new Intent();
//        intent.putExtra("continueSignIn", continueWithSignIn);
//        setResult(RESULT_OK, intent);
//        finish();
//    }
//
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
    }

}
