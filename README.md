---
title: BinahAi
description: Integrate BinahAi SDK to cordova.
sdk version: 5.0.5
---

# UPDATED README!!! - 09/21/2023
<!--
# license: Licensed to the Apache Software Foundation (ASF) under one
#         or more contributor license agreements.  See the NOTICE file
#         distributed with this work for additional information
#         regarding copyright ownership.  The ASF licenses this file
#         to you under the Apache License, Version 2.0 (the
#         "License"); you may not use this file except in compliance
#         with the License.  You may obtain a copy of the License at
#
#           http://www.apache.org/licenses/LICENSE-2.0
#
#         Unless required by applicable law or agreed to in writing,
#         software distributed under the License is distributed on an
#         "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#         KIND, either express or implied.  See the License for the
#         specific language governing permissions and limitations
#         under the License. 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
-->

# cordova-plugin-binahai


## How to use

Import `BinahAi` in your component file: 

```ts
import { BinahAi } from '@awesome-cordova-plugins/binah-ai/ngx';
```

Add `BinahAi` to the constructor:
```ts
constructor(private platform: Platform, private binahAi: BinahAi)
```
Ensure that the platform is ready before using the plugin:
```ts
this.platform.ready().then(() => {
  this.binahAi.startCamera().then((res: any) => {
    console.log(res);
  }).catch((error: any) => {
    console.error(error);
  });
});
```

## Installation
Install `@awesome-cordova-plugins/core` and `@awesome-cordova-plugins/binah-ai` from github repository `https://github.com/marhano/awesome_cordova_plugins_binahai.git` this will act as wrapper for the actual plugin

    npm install @awesome-cordova-plugins/core
    npm install https://github.com/marhano/awesome_cordova_plugins_binahai.git

Install the cordova plugin BinahAi (add "#branchName" at the end to install a specific branch)

    ionic cordova plugin add https://github.com/marhano/cordova_plugin_binahai.git

## Ionic setup
Open `config.xml` and add the snippet below.

    <preference name="android-minSdkVersion" value="27" />
    <preference name="AndroidGradlePluginVersion" value="7.3.1" />

The camera fragment is default to be at the back of the webview so you need to make sure the ion content background is set to transparent.

    ion-content { --background: transparent !important; }

Also remove the dark mode theme from the `variables.scss` file.

## Properties

- startCamera
- stopCamera
- startScan
- stopScan
- imageValidation
- getSessionState
- userFaceValidation
- getAllMeasurement
- getMeasurementById
- getMeasurementByDateTime

# Methods

## startCamera(options, [successCallback, errorCallback])

Options: All options stated are optional except licenseKey [ondev].

- `licenseKey` - binah ai provided license for android
- `sex` - (as classified at birth) [UNSPECIFIED / MALE / FEMALE]
- `age` - [years]
- `weight` - [Kilograms]

In order to provide highly accurate results, the SDK requires demographic information regarding the user who is taking the measurement. This information is not mandatory, but it improves the accuracy of the vital signs that are calculated by the system. The demographic information consists of three fields:

Example:

```ts
let options = {
  licenseKey: [XXXXXX-XXXXXX-XXXXXX-XXXXXX-XXXXXX-XXXXXX],
  sex: "MALE",
  age: 23,
  weight: 62,
};

BinahAi.startCamera(options).then((result) => {
  console.log(result);
}).catch((error) => {
  console.log(error);
});
```

## stopCamera([successCallback, errorCallback])

Stop the camera preview. It is recommended to call this method whenever the preview is not visible.

Example:

```ts
BinahAi.stopCamera();
```

## startScan([successCallback, errorCallback])

Start the measurement.

```ts 
BinahAi.startScan().subscribe({
  next: (result) => {
    if(typeOf result === 'number'){
      const measurementId = result;
    }else{
      console.log("LIVE RESULTS: " + JSON.stringify(result, null, 4));
    }
  },
  error: (err) => console.error(err),
  complete: () => console.info('complete')
});
```

Returns an `{Obervable<any>}`. During measurement the `LiveMeasurement` are being returned upon manually stopping the measurement or stopped after specified duration the `FinalResult` will be returned which is the `measurement_id` that is saved locally to the user device.

The `measurement_id` can now be used to get the vital signs data using the `getMeasurementById` method.

## stopScan([successCallback, errorCallback])

Stop the measurement.

```ts 
BinahAi.stopScan();
```

## imageValidation([successCallback, errorCallback])

During the measurement, the SDK assists the user in following these guidelines. It validates each camera frame and updates the imageValidity with any detected deviations from the guidelines.

```ts 
this.binahAi.imageValidation().subscribe({
  next: (result) => {
    console.log(result);
  },
  error: (err) => {
    console.error(err);
  }
});
```

The following are conditions that will invalidate the image for processing by the SDK:

- `Device Orientation` - The device orientation is unsupported for the session.
- `Undetected Face/Finger` - The SDK cannot detect the user's face or finger.
- `Tilted Head` - The user's face is not facing directly towards the camera.
- `Face Too Far` - The user's face is positioned too far from the camera. Instruct the user to hold the camera closer to their face.
- `Uneven Light` - The light on the user's face is not evenly distributed.

Returns a `{Observable<any>}` with the image validation code.

## getSessionState([successCallback, errorCallback])

Retrieve the current state of the session.

| State Name  | State Definition |
| ------------- | ------------- |
| INITIALIZING  | The session is in its initial state, performing initialization actions. Please wait until you receive the message indicating that the session is in the READY state before starting to measure vital signs or before calling any session APIs.  |
| READY  | The session is now ready to be started. The application can display the camera preview. Refer to the Creating a Preview page for detailed instructions.  |
| STARTING | The session is currently preparing to measure vital signs. |
| PROCESSING | The session is processing the images and calculating vital signs. For information on the handling of instantaneous vital signs, please refer to the Vital Signs page. |
| STOPPING | The session has been stopped, and the measurement results are being calculated. For information on the handling the final results, please see the Vital Signs page. |
| TERMINATING | The session is currently being terminated. Please refrain from calling any session APIs. |
| TERMINATED | The session has been gracefully terminated, and a new session can now be initiated. |

```ts
this.binahAi.getSessionState().subscribe({
  next: (result) => {
    console.log(result);
  },
  error: (error) => {
    console.error(error);
  }
});
```
## getAllMeasurement([successCallback, errorCallback])

Get all the measurement that is saved to the user device.

```ts
const result = await this.binahAi.getAllMeasurement();
```

Returns a `Promise<any>` with the following data as JSONArray.

- `measurement_id` - unique measurement identifier.
- `user_id` - id of the current user the measurement belongs to.
- `date_time` - date and time the measurement is taken.
- `vital_signs_data` - all vital signs data including all the neccessary variables in a `JSONObject` format, ex. vital_signs_data.pulseRate

| vitalSign  |  description  |
| ------------- | ------------- |
| name | vital sign formatted name |
| summary_description | a short description of the vital sign |
| full_description | full description text |
| unit_m | unit of measurement |
| value | vital sign value |
    

## getMeasurementById(measurementId, [successCallback, errorCallback])

Returns a single `Promise<any>` of scan result.

```ts
const result = await this.binahAi.getMeasurementById(measurementId);
```

## getMeasurementByDateTime(dateTime, [successCallback, errorCallback])

Returns an array `Promise<any>` of scan result.

```ts
const result = await this.binahAi.getMeasurementByDateTime(dateTime);
```
The dateTime is a string in this format `YYYY-MM-DD`.

# Sample App
<a href="https://github.com/marhano/binah_ionic_prototype">binah_ionic_prototype</a> for a complete working Cordova example for Android platforms.

# Credits
Maintained by Bastion Software Development Team - <a href="https://github.com/sddteam">@sddteam</a>

