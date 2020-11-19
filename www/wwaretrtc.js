var exec = require('cordova/exec');

exports.testing = function (arg0, success, error) {
    exec(success, error, 'wwaretrtc', 'testing', [arg0]);
};
exports.enterroom = function (arg0, success, error) {
    exec(success, error, 'wwaretrtc', 'enterroom', [arg0]);
};

exports.exitroom = function (arg0, success, error) {
    exec(success, error, 'wwaretrtc', 'exitroom', [arg0]);
};

exports.checkPermission = function (arg0, success, error) {
    exec(success, error, 'wwaretrtc', 'checkPermission', [arg0]);
};
