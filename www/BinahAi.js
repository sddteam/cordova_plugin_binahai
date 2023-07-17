var exec = require('cordova/exec');

var PLUGIN_NAME = "BinahAi";

var BinahAi = function() {} 

BinahAi.startCamera = function (licenseKey, sex, age, weight, onSuccess, onError) {
    exec(onSuccess, onError, PLUGIN_NAME, 'startCamera', [licenseKey, sex, age, weight]);
};

BinahAi.startScan = function (onSuccess, onError) {
    exec(onSuccess, onError, PLUGIN_NAME, 'startScan', []);
};

BinahAi.stopScan = function (onSuccess, onError) {
    exec(onSuccess, onError, PLUGIN_NAME, 'stopScan', []);
};

BinahAi.imageValidation = function (onSuccess, onError) {
    exec(onSuccess, onError, PLUGIN_NAME, 'imageValidation', []);
};

module.exports = BinahAi;
