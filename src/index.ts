// @ts-ignore
import { NativeModules, NativeEventEmitter, EmitterSubscription, Platform, PermissionsAndroid } from 'react-native';
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
  },

  checkMicrophonePermission: async (): Promise<boolean> => {
    if (Platform.OS === 'ios') {
      return VoiceStream.checkMicrophonePermission();
    } else if (Platform.OS === 'android') {
      try {
        const granted = await PermissionsAndroid.check(
          PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
        );
        return granted;
      } catch (error) {
        return false;
      }
    }
    return false;
  },

  requestMicrophonePermission: async (): Promise<boolean> => {
    if (Platform.OS === 'ios') {
      return VoiceStream.requestMicrophonePermission();
    } else if (Platform.OS === 'android') {
      try {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
        );
        return granted === PermissionsAndroid.RESULTS.GRANTED;
      } catch (error) {
        return false;
      }
    }
    return false;
  }
};

export default VoiceStreamer;