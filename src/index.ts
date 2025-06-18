// @ts-ignore
import { NativeModules, NativeEventEmitter, EmitterSubscription } from 'react-native';
import { VoiceStreamerInterface, VoiceStreamNativeModule, VoiceStreamOptions } from './types';

const { VoiceStream } = NativeModules as {
  VoiceStream: VoiceStreamNativeModule;
};

const EventEmitter = new NativeEventEmitter(VoiceStream);
const eventKey: 'data' = 'data';

const VoiceStreamer: VoiceStreamerInterface = {
  init: (options: VoiceStreamOptions): Promise<void> => {
    return VoiceStream.init(options);
  },

  start: (): Promise<void> => {
    return VoiceStream.start();
  },

  stop: (): Promise<void> => {
    return VoiceStream.stop();
  },
  
  listen: (event: 'data', callback: (data: string) => void): EmitterSubscription => {
    if (event !== eventKey) {
      throw new Error('Invalid event');
    }
    EventEmitter.removeAllListeners(eventKey);
    return EventEmitter.addListener(eventKey, callback);
  }
};

export default VoiceStreamer;