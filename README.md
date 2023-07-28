---
title: BinahAi
description: Integrate BinahAi SDK to cordova.
sdk version: 5.0.5
---
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

    npm install @awesome-cordova-plugins-core
    npm install https://github.com/marhano/awesome_cordova_plugins_binahai.git

Install the cordova plugin BinahAi

    ionic cordova plugin add https://github.com/marhano/cordova_plugin_binahai
    

## Properties

- startCamera
- startScan
- stopScan
- imageValidation

# Methods

## startCamera(options, [successCallback, errorCallback])

Options: All options stated are optional except licenseKey.

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

BinahAi.startCamera(options);
```

## startScan([successCallback, errorCallback])

Start the measurement.

```ts 
BinahAi.startScan().subscribe({
  next: (result) => {
    this.startSimulation();
    if(Object.keys(result).length > 3){
      this.finalResults = result as FinalResults;
      console.log("FINAL RESULTS: " + JSON.stringify(this.finalResults, null, 4));
    }else{
      const liveMeasurements = result as LiveMeasurements;
      this.measurements = liveMeasurements;
    }

    this.changeDetectorRef.detectChanges();
  },
  error: (err) => console.error(err),
  complete: () => console.info('complete')
});
```

Returns n `{Obervable<any>}`, wrap the results with the provided interface `[LiveMeasurements, FinalResult]` which is the two type of result for start scan. During measurement the `LiveMeasurement` are being returned upon manually stopping the measurement or stopped after measurement duration the `FinalResult` will be returned.

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

# Sample App
<a href="https://github.com/marhano/binah_ionic_prototype">binah_ionic_prototype</a> for a complete working Cordova example for Android platforms.

# Credits
Maintained by Bastion Software Development Team - <a href="https://github.com/sddteam">@sddteam</a>

