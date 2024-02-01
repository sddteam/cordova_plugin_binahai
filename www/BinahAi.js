var exec = require('cordova/exec');

var PLUGIN_NAME = "BinahAi";

var BinahAi = function() {} 

function isFunction(obj) {
    return !!(obj && obj.constructor && obj.call && obj.apply);
  };

BinahAi.startCamera = function (options, onSuccess, onError) {
    if(!options){
        options = {};
    }else if(isFunction(options)){
        onSuccess = options;
        options = {};
    }

    options.licenseKey = options.licenseKey;
    options.duration = options.duration;
    options.userId = options.userId;
    options.weight = options.weight || 0;
    options.age = options.age || 0;
    options.sex = options.sex || 'UNDEFINED';

    exec(onSuccess, onError, PLUGIN_NAME, 'startCamera', [
        options.licenseKey, 
        options.duration,
        options.userId,
        options.sex, 
        options.age, 
        options.weight]);
};

BinahAi.stopCamera = function(onSuccess, onError){
    exec(onSuccess, onError, PLUGIN_NAME, 'stopCamera', []);
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

BinahAi.getSessionState = function (onSuccess, onError) {
    exec(onSuccess, onError, PLUGIN_NAME, 'getSessionState', []);
};

BinahAi.userFaceValidation = function (onSuccess, onError) {
    exec(onSuccess, onError, PLUGIN_NAME, 'userFaceValidation', []);
};

BinahAi.getAllMeasurement = function (userId, onSuccess, onError){
    exec(onSuccess, onError, PLUGIN_NAME, 'getAllMeasurement', [userId]);  
};

BinahAi.getMeasurementById = function (userId, measurementId, onSuccess, onError){
    exec(onSuccess, onError, PLUGIN_NAME, 'getMeasurementById', [userId, measurementId]);  
};

BinahAi.getMeasurementByDateTime = function (userId, dateTime, onSuccess, onError){
    exec(onSuccess, onError, PLUGIN_NAME, 'getMeasurementByDateTime', [userId, dateTime]);
}; 

BinahAi.deleteMeasurementById = function (userId, measurementId, onSuccess, onError){
    exec(onSuccess, onError, PLUGIN_NAME, 'deleteMeasurementById', [userId, measurementId]);
};

BinahAi.shareResult = function (result, onSuccess, onError){
    exec(onSuccess, onError, PLUGIN_NAME, 'shareResult', [result]);
};

BinahAi.extractHealthData = function(userId, onSuccess, onError){
    exec(onSuccess, onError, PLUGIN_NAME, 'extractHealthData', [userId]);
};



module.exports = BinahAi;
