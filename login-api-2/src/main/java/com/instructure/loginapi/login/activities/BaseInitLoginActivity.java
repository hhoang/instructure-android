/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.instructure.loginapi.login.activities;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;

import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.loginapi.login.BuildConfig;
import com.instructure.loginapi.login.R;
import com.instructure.loginapi.login.view.CanvasLoadingView;
import com.instructure.pandautils.utils.ViewStyler;

public abstract class BaseInitLoginActivity extends AppCompatActivity {

    static { AppCompatDelegate.setCompatVectorFromResourcesEnabled(true); }

    /**** Login Flow:
    *
    * InitLoginActivity ->
    *   LoginLandingPage
    *   ** OR **
    *   StartApplication
    *
    * LoginLandingPage ->
    *   FindSchoolActivity
    *   ** OR **
    *   StartApplication
    *
    * FindSchoolActivity ->
    *   SignInActivity
    *
    * */

    protected abstract Intent beginLoginFlowIntent();
    protected abstract Intent launchApplicationMainActivityIntent();
    protected abstract @ColorInt int themeColor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init_login);
        applyTheme();
        ApiPrefs.setUserAgent(com.instructure.pandautils.utils.Utils.generateUserAgent(this, "candroid"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLoginState();
    }

    /**
     * This function checks whether or not the current user is signed in.
     */
    private void checkLoginState() {
        final boolean isDebuggable =  (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
        final long duration = isDebuggable ? 0 : 1750;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final String token = ApiPrefs.getToken();
                        if (token.isEmpty()) {
                            //Start Login Flow
                            startActivity(beginLoginFlowIntent());
                        } else {
                            //Start App
                            Intent intent = launchApplicationMainActivityIntent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }

                        finish();
                    }
                });
            }
        }, duration);
    }

    private void applyTheme() {
        CanvasLoadingView loadingView = (CanvasLoadingView) findViewById(R.id.progress_bar);
        loadingView.setOverrideColor(themeColor());

        ViewStyler.setStatusBarLight(this);
    }
}
