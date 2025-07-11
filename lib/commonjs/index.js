"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.requestMicrophonePermission = exports.default = exports.checkMicrophonePermission = void 0;
var _reactNative = require("react-native");
// @ts-ignore

const {
  VoiceStream: NativeVoiceStream
} = _reactNative.NativeModules;
const EventEmitter = new _reactNative.NativeEventEmitter(NativeVoiceStream);
const eventKey = 'data';
const VoiceStream = {
  init: options => {
    return NativeVoiceStream.init(options);
  },
  start: () => {
    return NativeVoiceStream.start();
  },
  stop: () => {
    return NativeVoiceStream.stop();
  },
  listen: (event, callback) => {
    if (event !== eventKey) {
      throw new Error('Invalid event');
    }
    EventEmitter.removeAllListeners(eventKey);
    return EventEmitter.addListener(eventKey, callback);
  }
};
const checkMicrophonePermission = async () => {
  if (_reactNative.Platform.OS === 'ios') {
    return NativeVoiceStream.checkMicrophonePermission();
  } else if (_reactNative.Platform.OS === 'android') {
    try {
      const granted = await _reactNative.PermissionsAndroid.check(_reactNative.PermissionsAndroid.PERMISSIONS.RECORD_AUDIO);
      return granted;
    } catch (error) {
      return false;
    }
  }
  return false;
};
exports.checkMicrophonePermission = checkMicrophonePermission;
const requestMicrophonePermission = async () => {
  if (_reactNative.Platform.OS === 'ios') {
    return NativeVoiceStream.requestMicrophonePermission();
  } else if (_reactNative.Platform.OS === 'android') {
    try {
      const granted = await _reactNative.PermissionsAndroid.request(_reactNative.PermissionsAndroid.PERMISSIONS.RECORD_AUDIO);
      return granted === _reactNative.PermissionsAndroid.RESULTS.GRANTED;
    } catch (error) {
      return false;
    }
  }
  return false;
};
exports.requestMicrophonePermission = requestMicrophonePermission;
var _default = exports.default = VoiceStream;
//# sourceMappingURL=index.js.map