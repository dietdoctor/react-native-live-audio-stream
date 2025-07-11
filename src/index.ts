// @ts-ignore
import { NativeModules, NativeEventEmitter, EmitterSubscription, Platform, PermissionsAndroid } from 'react-native';
import { VoiceStreamerInterface, VoiceStreamNativeModule, VoiceStreamOptions } from './types';

const { VoiceStream: NativeVoiceStream } = NativeModules as {
  VoiceStream: VoiceStreamNativeModule;
};

const EventEmitter = new NativeEventEmitter(NativeVoiceStream);
const eventKey: 'data' = 'data';

const VoiceStream: VoiceStreamerInterface = {
  init: (options: VoiceStreamOptions): Promise<void> => {
    return NativeVoiceStream.init(options);
  },

  start: (): Promise<void> => {
    return NativeVoiceStream.start();
  },

  stop: (): Promise<void> => {
    return NativeVoiceStream.stop();
  },
  
  listen: (event: 'data', callback: (data: string) => void): EmitterSubscription => {
    if (event !== eventKey) {
      throw new Error('Invalid event');
    }
    EventEmitter.removeAllListeners(eventKey);
    return EventEmitter.addListener(eventKey, callback);
  },
};

export const checkMicrophonePermission = async (): Promise<boolean> => {
  if (Platform.OS === 'ios') {
    return NativeVoiceStream.checkMicrophonePermission();
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
};

export const requestMicrophonePermission = async (): Promise<boolean> => {
  if (Platform.OS === 'ios') {
    return NativeVoiceStream.requestMicrophonePermission();
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

export default VoiceStream;