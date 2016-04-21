package fz.demoapps.drivelikecrazy;

import android.content.Context;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveApi.DriveIdResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Created by fz on 25.03.16.
 */
public class FileAppenderAndroidApi implements GoogleApiClient.ConnectionCallbacks {
    final String TAG = "FileAppenderAndroidApi";

    public interface FileAppenderListener {
        void OnContentChanged();
        void DataReady(String data);
    }

    final String FILE_TEXT = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    final String FILE_APPEND_TEXT = "&абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ0123456789!";
    final String FILE_NAME = "wow_such_content_many_data.txt";
    final int FILE_SIZE = 1024*1024*20;

    final Context mCtx;
    final GoogleApiClient.OnConnectionFailedListener mConnFailCallback;
    final FileAppenderListener mFileAppenderListener;
    GoogleApiClient mGoogleApiClient;
    String textFileID = new String();

    public FileAppenderAndroidApi(Context c, GoogleApiClient.OnConnectionFailedListener connectionFailedCb, FileAppenderListener fal) {
        mCtx = c;
        mConnFailCallback = connectionFailedCb;
        mFileAppenderListener = fal;
    }

    public void start() {
        if (mGoogleApiClient == null) {
            // Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection
            // failures.
            // Since no account name is passed, the user is prompted to choose.
            mGoogleApiClient = new GoogleApiClient.Builder(mCtx)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(mConnFailCallback)
                    .build();
        }
        // Connect the client. Once connected, the camera is launched.
        mGoogleApiClient.connect();
    }

    public void stop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    public void createBigFile() {
        Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(mCreateNewContentResult);
    }

    ResultCallback<DriveContentsResult> mCreateNewContentResult = new ResultCallback<DriveContentsResult>() {

        @Override
        public void onResult(DriveContentsResult driveContentsResult) {
            if (!driveContentsResult.getStatus().isSuccess()) {
                Log.e(TAG, "Error while trying to create new file contents");
                return;
            }
            final DriveContents driveContents = driveContentsResult.getDriveContents();
            ParcelFileDescriptor outputFile = driveContents.getParcelFileDescriptor();
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile.getFileDescriptor());

            try {
                Writer writer = new OutputStreamWriter(fileOutputStream);
                for(int i = 0; i < FILE_SIZE / FILE_TEXT.length(); i ++) {
                    writer.write("#" + i + ":" + FILE_TEXT);
                }
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(FILE_NAME)
                    .setMimeType("text/plain")
                    .setStarred(true).build();

            DriveFolder df = Drive.DriveApi.getRootFolder(mGoogleApiClient);
            df.createFile(mGoogleApiClient, changeSet, driveContents).setResultCallback(mFileCreationCompleteResult);
        }
    };

    ResultCallback<DriveFileResult> mFileCreationCompleteResult = new ResultCallback<DriveFileResult>() {
        @Override
        public void onResult(DriveFileResult driveFileResult) {
            textFileID = driveFileResult.getDriveFile().getDriveId().toString();
            listRootFolderContents();
        }
    };

    public boolean isTestFileExists() {
        return !textFileID.isEmpty();
    }

    public void appendSomeData() {
        DriveId driveId = DriveId.decodeFromString(textFileID);
        DriveFile file = driveId.asDriveFile();
        new AppendDataAsyncTask().execute(file);
    }

    public class AppendDataAsyncTask extends AsyncTask<DriveFile, Void, Boolean> {
        @Override
        protected Boolean doInBackground(DriveFile... params) {
            DriveFile file = params[0];
            try {
                DriveContentsResult driveContentsResult = file.open(
                        mGoogleApiClient, DriveFile.MODE_READ_WRITE, null).await();
                if (!driveContentsResult.getStatus().isSuccess()) {
                    return false;
                }
                final DriveContents driveContents = driveContentsResult.getDriveContents();
                ParcelFileDescriptor outputFile = driveContents.getParcelFileDescriptor();
                FileInputStream fis = new FileInputStream(outputFile.getFileDescriptor());

                fis.read(new byte[fis.available()]);

                FileOutputStream fileOutputStream = new FileOutputStream(outputFile.getFileDescriptor());

                fileOutputStream.write(FILE_APPEND_TEXT.getBytes());


                com.google.android.gms.common.api.Status status =
                        driveContents.commit(mGoogleApiClient, null).await();

                Log.i(TAG, "Commited!");
                return status.getStatus().isSuccess();
            } catch (IOException e) {
                Log.e(TAG, "IOException while appending to the output stream", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean) {
                listRootFolderContents();
            }
        }
    }

    public void listRootFolderContents() {
        DriveFolder df = Drive.DriveApi.getRootFolder(mGoogleApiClient);
        df.listChildren(mGoogleApiClient).setResultCallback(mListRootFolderCallback);
    }

    ResultCallback<MetadataBufferResult> mListRootFolderCallback = new
        ResultCallback<MetadataBufferResult>() {
            @Override
            public void onResult(MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.e(TAG, "Problem while retrieving files");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                StringBuilder finalString = new StringBuilder();

                Log.e(TAG, "Got data! root contains " + mb.getCount() + " files");
                for (int i = 0; i < mb.getCount(); i++) {
                    Metadata data = mb.get(i);
                    finalString.append(data.getOriginalFilename())
                            .append(" ")
                            .append(data.getFileSize())
                            .append("\n");
                }

                mb.release();

                mFileAppenderListener.DataReady(finalString.toString());
            }
        };


    @Override
    public void onConnected(Bundle bundle) {
        listRootFolderContents();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }


}
