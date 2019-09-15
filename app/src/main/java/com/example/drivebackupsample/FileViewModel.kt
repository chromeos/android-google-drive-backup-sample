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

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException


class FileViewModel(application: Application) : AndroidViewModel(application) {

    private val _numberOfFilesOwned: MutableLiveData<Int> = MutableLiveData()

    val numberOfFilesOwned: LiveData<Int>
        get() = _numberOfFilesOwned

    private lateinit var driveService: Drive
    private lateinit var appFolderId: String

    fun initializeDriveService(googleDriveServiceSession: Drive) {
        viewModelScope.launch {
            driveService = googleDriveServiceSession
            appFolderId = googleDriveServiceSession
                    .fetchOrCreateAppFolder(
                        getApplication<MyApplication>().getString(R.string.application_folder)
                    )
            getApplication<MyApplication>().initializeWorkManager(googleDriveServiceSession)
        }
    }

    fun getNumberOfFilesOwnedByApp() {
        viewModelScope.launch {
            val fileList = driveService.getAllFiles().files
            _numberOfFilesOwned.value = fileList.size
        }
    }

    private fun createFile(name: String = "Untitled File", contents: String = "default text") {
        viewModelScope.launch {
            val fileId = driveService.createFile(appFolderId, name)
            driveService.saveFile(fileId, name, contents)
        }
    }

    private fun createFileInBackground(
        name: String,
        contents: String = "default text"
    ) {
        val data = workDataOf(
            DriveUploadWorker.KEY_NAME_ARG to name,
            DriveUploadWorker.KEY_CONTENTS_ARG to contents,
            DriveUploadWorker.KEY_CONTENTS_FOLDER_ID to appFolderId
        )

        val driveWorker = OneTimeWorkRequestBuilder<DriveUploadWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(getApplication()).enqueue(driveWorker)
    }

    fun uploadFileFromAccessFramework(
        contentResolver: ContentResolver,
        uri: Uri,
        background: Boolean
    ) {
        viewModelScope.launch {
            val fileInformation = withContext(Dispatchers.IO) {
                openFileUsingStorageAccessFramework(contentResolver, uri)
            }
            if (!background) {
                createFile(fileInformation.first, fileInformation.second)
            } else {
                createFileInBackground(fileInformation.first, fileInformation.second)
            }
        }
    }

    companion object {
        /**
         * Opens the file at the [uri] returned by a Storage Access Framework [Intent]
         * using the given [contentResolver].
         */
        fun openFileUsingStorageAccessFramework(
            contentResolver: ContentResolver,
            uri: Uri
        ): Pair<String, String> {
            // Retrieve the document's display name from its metadata.
            var name = ""
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    name = cursor.getString(nameIndex)
                } else {
                    throw IOException("Empty cursor returned for file.")
                }
            }

            // Read the document's contents as a String.
            val content = contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: ""

            return name to content
        }
    }

}