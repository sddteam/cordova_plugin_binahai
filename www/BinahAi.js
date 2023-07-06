var exec = require('cordova/exec');

var PLUGIN_NAME = "BinahAi";

var BinahAi = function() {} 

BinahAi.coolMethod = function (arg0, success, error) {
    exec(success, error, PLUGIN_NAME, 'coolMethod', [arg0]);
};

module.exports = BinahAi;
