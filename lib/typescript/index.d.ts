import { VoiceStreamerInterface } from './types';
declare const VoiceStream: VoiceStreamerInterface;
export declare const checkMicrophonePermission: () => Promise<boolean>;
export declare const requestMicrophonePermission: () => Promise<boolean>;
export default VoiceStream;
