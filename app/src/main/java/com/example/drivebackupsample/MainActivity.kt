/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.drivebackupsample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import android.app.Activity
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.services.drive.Drive
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import org.apache.http.entity.ContentType

class MainActivity : AppCompatActivity() {

    private lateinit var uploadFileButton: Button
    private lateinit var uploadFileBackgroundButton: Button
    private lateinit var queryFilesButton: Button
    private lateinit var fileViewModel: FileViewModel
    private lateinit var numberOfFilesTextView: TextView

    /**
     * Creates an [Intent] for opening the Storage Access Framework file picker.
     */
    private val filePickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = ContentType.TEXT_PLAIN.mimeType
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileViewModel = ViewModelProvider(this)[FileViewModel::class.java]
        requestSignIn()

        setContentView(R.layout.activity_main)

        initializeUIElements()
    }

    public override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        resultData: Intent?
    ) {
        if (resultData == null || resultCode != Activity.RESULT_OK)
            return super.onActivityResult(requestCode, resultCode, resultData)

        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> handleSignInResult(resultData)
            REQUEST_CODE_OPEN_FILE -> resultData.data?.let {
                fileViewModel.uploadFileFromAccessFramework(contentResolver, it, false)
            }
            REQUEST_CODE_OPEN_FILE_BACKGROUND -> resultData.data?.let {
                fileViewModel.uploadFileFromAccessFramework(contentResolver, it, true)
            }
        }

        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun requestSignIn() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        val client = GoogleSignIn.getClient(this, signInOptions)

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private fun handleSignInResult(result: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
            .addOnSuccessListener { googleAccount ->

                // Use the authenticated account to sign in to the Drive service.
                val credential = GoogleAccountCredential.usingOAuth2(
                    this,
                    listOf(
                        DriveScopes.DRIVE_FILE,
                        DriveScopes.DRIVE_APPDATA
                    )
                )
                credential.selectedAccount = googleAccount.account
                val googleDriveService = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory(),
                    credential
                ).setApplicationName("Drive Backup Sample")
                    .build()

                //Sets the drive api service on the view model and creates our apps folder
                fileViewModel.initializeDriveService(googleDriveService)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Unable to sign in.", exception)
            }
    }


    private fun initializeUIElements() {
        uploadFileButton = findViewById(R.id.upload_files_button)
        uploadFileBackgroundButton = findViewById(R.id.upload_files_background_button)
        queryFilesButton = findViewById(R.id.query_files_button)
        numberOfFilesTextView = findViewById(R.id.number_of_files_text_view)

        uploadFileButton.setOnClickListener {
            startActivityForResult(filePickerIntent, REQUEST_CODE_OPEN_FILE)
        }

        uploadFileBackgroundButton.setOnClickListener {
            startActivityForResult(filePickerIntent, REQUEST_CODE_OPEN_FILE_BACKGROUND)
        }
        queryFilesButton.setOnClickListener {
            fileViewModel.getNumberOfFilesOwnedByApp()
        }

        fileViewModel.numberOfFilesOwned.observe(this, Observer { numOfFiles ->
            val formattedString = getString(R.string.number_of_files, numOfFiles)
            numberOfFilesTextView.text = formattedString
        })
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val REQUEST_CODE_SIGN_IN = 1
        private const val REQUEST_CODE_OPEN_FILE = 2
        private const val REQUEST_CODE_OPEN_FILE_BACKGROUND = 3

    }
}
