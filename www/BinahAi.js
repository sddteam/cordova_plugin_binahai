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
    options.weight = options.weight || 0;
    options.age = options.age || 0;
    options.sex = options.sex || 'UNDEFINED';

    exec(onSuccess, onError, PLUGIN_NAME, 'startCamera', [
        options.licenseKey, 
        options.duration,
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

BinahAi.getAllHistory = function (onSuccess, onError){
    exec(onSuccess, onError, PLUGIN_NAME, 'getAllHistory', []);  
};

BinahAi.getHistoryById = function (measurementId, onSuccess, onError){
    exec(onSuccess, onError, PLUGIN_NAME, 'getHistoryById', [measurementId]);  
};

BinahAi.getHistoryByDateTime = function (dateTime, onSuccess, onError){
    exec(onSuccess, onError, PLUGIN_NAME, 'getHistoryByDateTime', [dateTime]);
}; 

BinahAi.deleteHistoryById = function (measurementId, onSuccess, onError){
    exec(onSuccess, onError, PLUGIN_NAME, 'deleteHistoryById', [measurementId]);
};

BinahAi.getVitalDescription = function (onSuccess, onError){
    exec(onSuccess, onError, PLUGIN_NAME, 'getVitalDescription', []);
};

module.exports = BinahAi;
