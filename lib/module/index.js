// @ts-ignore
import { NativeModules, NativeEventEmitter } from 'react-native';
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
  }
};
export default VoiceStreamer;
//# sourceMappingURL=index.js.map