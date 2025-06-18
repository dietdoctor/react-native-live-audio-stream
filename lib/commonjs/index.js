"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _reactNative = require("react-native");
// @ts-ignore

const {
  VoiceStream
} = _reactNative.NativeModules;
const EventEmitter = new _reactNative.NativeEventEmitter(VoiceStream);
const eventKey = 'data';
const VoiceStreamer = {
  init: options => {
    return VoiceStream.init(options);
  },
  start: () => {
    return VoiceStream.start();
  },
  stop: () => {
    return VoiceStream.stop();
  },
  listen: (event, callback) => {
    if (event !== eventKey) {
      throw new Error('Invalid event');
    }
    EventEmitter.removeAllListeners(eventKey);
    return EventEmitter.addListener(eventKey, callback);
  }
};
var _default = exports.default = VoiceStreamer;
//# sourceMappingURL=index.js.map