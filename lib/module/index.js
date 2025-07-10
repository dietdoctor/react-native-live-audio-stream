// @ts-ignore
import { NativeModules, NativeEventEmitter, Platform, PermissionsAndroid } from 'react-native';
const {
  VoiceStream
} = NativeModules;
const EventEmitter = new NativeEventEmitter(VoiceStream);
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
  },
  checkMicrophonePermission: async () => {
    if (Platform.OS === 'ios') {
      return VoiceStream.checkMicrophonePermission();
    } else if (Platform.OS === 'android') {
      try {
        const granted = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.RECORD_AUDIO);
        return granted;
      } catch (error) {
        return false;
      }
    }
    return false;
  },
  requestMicrophonePermission: async () => {
    if (Platform.OS === 'ios') {
      return VoiceStream.requestMicrophonePermission();
    } else if (Platform.OS === 'android') {
      try {
        const granted = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.RECORD_AUDIO);
        return granted === PermissionsAndroid.RESULTS.GRANTED;
      } catch (error) {
        return false;
      }
    }
    return false;
  }
};
export default VoiceStreamer;
//# sourceMappingURL=index.js.map