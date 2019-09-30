# Android on Chrome OS & Google Drive Android API Sample

This application serves as an example of how to use the Google Drive API to
pull down your app's files as well as update them in the background using
[Work Manager API's.](https://developer.android.com/topic/libraries/architecture/workmanager)

This serves as a starting point to allow you to easily store data in Drive
while being able to recover the files if the profile on the Chrome OS Device is
deleted or removed due to lack of memory.

## What does it do?

*   Creates text files in the user's My Drive folder
*   Edits file contents and metadata and saves them to Drive
*   Queries the REST API for all files visible to the app using the [Drive
Client Library.](https://developers.google.com/drive/api/v3/about-sdk)
*   Updates/Creates text files in the background using Work Manager

## Getting started with the sample
*   `Main Activity` handles the authorization of the user and the creation of the
Google Drive service with this authorization.
*   `FileViewModel` handles most of the conversation between the buttons in the
activity and making the calls to the Drive API. This is also the launch of
the coroutines where we are making these calls.
*   `DriveExtensions.kt` enhances the Drive API to handle network calls.
*   The remaining class enable background processing with WorkManager. We are
using a `DelegatingWorkFactory` to be ableto create our workers with the
API client that we need.

## Things to keep in mind?
*   Using WorkManager to save things in the background is great, but there are
many ways that the user could stop this from occurring. On Chrome OS, logging out
of the device or closing the lid will turn off the Android container and in turn
will stop anything running in the background.

As a disclaimer: this sample handles this by pushing to the background if you
choose to use that option, and it will be stopped if the Android container is
stopped
*   One other thing to note is that we are not setting any constraints
on our Work Manager jobs, due to this being a Chrome OS based sample, the same
network and battery constraints don't necessarily apply the same due to Chrome
OS devices having larger batteries and not LTE enabled. Take note of this if you
are also building apps for both Chrome OS devices and phones.

## Set Up (Mostly Drive API setup)

1.  Install the [Android SDK](https://developer.android.com/sdk/index.html).
2.  Download and configure the
    [Google Play services SDK](https://developer.android.com/google/play-services/setup.html).
3.  Create a
    [Google API Console](https://console.developers.google.com/projectselector/apis/dashboard)
    project and enable the Drive API library.
4.  Register an OAuth 2.0 client for the package
    `com.google.android.gms.drive.sample.driveapimigration` with your own
    [debug keys](https://developers.google.com/drive/android/auth).
5.  Add the `../auth/drive.file` scope to the OAuth consent screen in the API
    Console.

See full instructions in the
[Getting Started guide](https://developers.google.com/drive/android/get-started).

Find [API Documentation and references here](https://developers.google.com/drive/api/v3/about-sdk)

Support
-------

If you've found an error in this sample, please
[file an issue](https://github.com/chromeos/android-google-drive-backup-sample/issues)

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub. Please see
[CONTRIBUTING.md](CONTRIBUTING.md) for more details.
