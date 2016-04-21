package fz.demoapps.drivelikecrazy;

//import android.support.v7.app.AppCompatActivity;
//import android.os.Bundle;
//
//public class MainActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//    }
//}


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.drive.DriveScopes;

import com.google.api.services.drive.model.*;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity implements GoogleApiClient.OnConnectionFailedListener, FileAppenderAndroidApi.FileAppenderListener {

    final String TAG = "MainActivity";
    final int REQUEST_CODE_RESOLUTION = 12;

    private TextView mOutputText;
    private FileAppenderAndroidApi mAppender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOutputText = (TextView) findViewById(R.id.outText);

        mAppender = new FileAppenderAndroidApi(this, this, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
                if (resultCode == RESULT_OK) {
                    mAppender.start();
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAppender.start();
    }

    @Override
    protected void onPause() {
        mAppender.stop();
        super.onPause();
    }

    @Override
    public void OnContentChanged() {

    }

    @Override
    public void DataReady(String data) {
        mOutputText.setText(data);
    }

    public void listDataClicked(View view) {
        mAppender.listRootFolderContents();
    }

    public void updateFileClicked(View view) {
        if(mAppender.isTestFileExists()) {
            mAppender.appendSomeData();
        }
        else {
            mAppender.createBigFile();
        }
    }

    //    GoogleAccountCredential mCredential;
//    ProgressDialog mProgress;
//
//    static final int REQUEST_ACCOUNT_PICKER = 1000;
//    static final int REQUEST_AUTHORIZATION = 1001;
//    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
//    private static final String PREF_ACCOUNT_NAME = "accountName";
//    private static final String[] SCOPES = { DriveScopes.DRIVE };
//
//    /**
//     * Create the main activity.
//     * @param savedInstanceState previously saved instance data.
//     */
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        mOutputText = (TextView)findViewById(R.id.outText);
//
//        mProgress = new ProgressDialog(this);
//        mProgress.setMessage("Calling Drive API ...");
//
//        // Initialize credentials and service object.
//        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
//        final String ac = settings.getString(PREF_ACCOUNT_NAME, null);
//
//        mCredential = GoogleAccountCredential.usingOAuth2(
//                getApplicationContext(), Arrays.asList(SCOPES))
//                .setBackOff(new ExponentialBackOff());
//
//        mCredential.setSelectedAccountName(ac);
//
//        mOutputText.setText("selected Acc: " + mCredential.getSelectedAccountName());
//
//    }
//
//
//    /**
//     * Called whenever this activity is pushed to the foreground, such as after
//     * a call to onCreate().
//     */
//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (isGooglePlayServicesAvailable()) {
//            refreshResults();
//        } else {
//            mOutputText.setText("Google Play Services required: " +
//                    "after installing, close and relaunch this app.");
//        }
//    }
//
//    /**
//     * Called when an activity launched here (specifically, AccountPicker
//     * and authorization) exits, giving you the requestCode you started it with,
//     * the resultCode it returned, and any additional data from it.
//     * @param requestCode code indicating which activity result is incoming.
//     * @param resultCode code indicating the result of the incoming
//     *     activity result.
//     * @param data Intent (containing result data) returned by incoming
//     *     activity result.
//     */
//    @Override
//    protected void onActivityResult(
//            int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        switch(requestCode) {
//            case REQUEST_GOOGLE_PLAY_SERVICES:
//                if (resultCode != RESULT_OK) {
//                    isGooglePlayServicesAvailable();
//                }
//                break;
//            case REQUEST_ACCOUNT_PICKER:
//                if (resultCode == RESULT_OK && data != null &&
//                        data.getExtras() != null) {
//                    String accountName =
//                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
//                    if (accountName != null) {
//                        mCredential.setSelectedAccountName(accountName);
//                        SharedPreferences settings =
//                                getPreferences(Context.MODE_PRIVATE);
//                        SharedPreferences.Editor editor = settings.edit();
//                        editor.putString(PREF_ACCOUNT_NAME, accountName);
//                        editor.apply();
//
//                        mOutputText.setText("Account set to: " + accountName);
//                    }
//                } else if (resultCode == RESULT_CANCELED) {
//                    mOutputText.setText("Account unspecified.");
//                }
//                break;
//            case REQUEST_AUTHORIZATION:
//                if (resultCode != RESULT_OK) {
//                    chooseAccount();
//                }
//                break;
//        }
//
//        super.onActivityResult(requestCode, resultCode, data);
//    }
//
//    /**
//     * Attempt to get a set of data from the Drive API to display. If the
//     * email address isn't known yet, then call chooseAccount() method so the
//     * user can pick an account.
//     */
//    private void refreshResults() {
//        mOutputText.setText("selected Acc 2: " + mCredential.getSelectedAccountName());
//
//        if (mCredential.getSelectedAccountName() == null) {
//            chooseAccount();
//        } else {
//            if (isDeviceOnline()) {
//                new MakeRequestTask(mCredential).execute();
//            } else {
//                mOutputText.setText("No network connection available.");
//            }
//        }
//    }
//
//    /**
//     * Starts an activity in Google Play Services so the user can pick an
//     * account.
//     */
//    private void chooseAccount() {
//        startActivityForResult(
//                mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
//    }
//
//    /**
//     * Checks whether the device currently has a network connection.
//     * @return true if the device has a network connection, false otherwise.
//     */
//    private boolean isDeviceOnline() {
//        ConnectivityManager connMgr =
//                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
//        return (networkInfo != null && networkInfo.isConnected());
//    }
//
//    /**
//     * Check that Google Play services APK is installed and up to date. Will
//     * launch an error dialog for the user to update Google Play Services if
//     * possible.
//     * @return true if Google Play Services is available and up to
//     *     date on this device; false otherwise.
//     */
//    private boolean isGooglePlayServicesAvailable() {
//        final int connectionStatusCode =
//                GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
//        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
//            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
//            return false;
//        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
//            return false;
//        }
//        return true;
//    }
//
//    /**
//     * Display an error dialog showing that Google Play Services is missing
//     * or out of date.
//     * @param connectionStatusCode code describing the presence (or lack of)
//     *     Google Play Services on this device.
//     */
//    void showGooglePlayServicesAvailabilityErrorDialog(
//            final int connectionStatusCode) {
//        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
//                connectionStatusCode,
//                MainActivity.this,
//                REQUEST_GOOGLE_PLAY_SERVICES);
//        dialog.show();
//    }
//
//    /**
//     * An asynchronous task that handles the Drive API call.
//     * Placing the API calls in their own task ensures the UI stays responsive.
//     */
//    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
//        private com.google.api.services.drive.Drive mService = null;
//        private Exception mLastError = null;
//
//        public MakeRequestTask(GoogleAccountCredential credential) {
//            HttpTransport transport = AndroidHttp.newCompatibleTransport();
//            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
//            mService = new com.google.api.services.drive.Drive.Builder(
//                    transport, jsonFactory, credential)
//                    .setApplicationName("Drive Like Crazy")
//                    .build();
//        }
//
//        /**
//         * Background task to call Drive API.
//         * @param params no parameters needed for this task.
//         */
//        @Override
//        protected List<String> doInBackground(Void... params) {
//            try {
//                return getDataFromApi();
//            } catch (Exception e) {
//                mLastError = e;
//                cancel(true);
//                return null;
//            }
//        }
//
//        /**
//         * Fetch a list of up to 10 file names and IDs.
//         * @return List of Strings describing files, or an empty list if no files
//         *         found.
//         * @throws IOException
//         */
//        private List<String> getDataFromApi() throws IOException {
//            // Get a list of up to 10 files.
//            List<String> fileInfo = new ArrayList<String>();
//
//            mService.files().patch()
//            FileList result = mService.files().list()
//                    .setMaxResults(10)
//                    .execute();
//            List<File> files = result.getItems();
//            if (files != null) {
//                for (File file : files) {
//                    fileInfo.add(String.format("%s (%s)\n",
//                            file.getTitle(), file.getId()));
//                }
//            }
//            return fileInfo;
//        }
//
//
//        @Override
//        protected void onPreExecute() {
//            mOutputText.setText("");
//            mProgress.show();
//        }
//
//        @Override
//        protected void onPostExecute(List<String> output) {
//            mProgress.hide();
//            if (output == null || output.size() == 0) {
//                mOutputText.setText("No results returned.");
//            } else {
//                output.add(0, "Data retrieved using the Drive API:");
//                mOutputText.setText(TextUtils.join("\n", output));
//            }
//        }
//
//        @Override
//        protected void onCancelled() {
//            mProgress.hide();
//            if (mLastError != null) {
//                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
//                    showGooglePlayServicesAvailabilityErrorDialog(
//                            ((GooglePlayServicesAvailabilityIOException) mLastError)
//                                    .getConnectionStatusCode());
//                } else if (mLastError instanceof UserRecoverableAuthIOException) {
//                    startActivityForResult(
//                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
//                            MainActivity.REQUEST_AUTHORIZATION);
//                } else {
//                    mOutputText.setText("The following error occurred:\n"
//                            + mLastError.getMessage());
//                }
//            } else {
//                mOutputText.setText("Request cancelled.");
//            }
//        }
//    }
}