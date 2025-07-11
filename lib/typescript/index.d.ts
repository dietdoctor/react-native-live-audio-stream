import { VoiceStreamerInterface } from "./types";
declare const VoiceStream: VoiceStreamerInterface;
/**
 * Verifies whether microphone access is currently granted for this app
 * @returns True if permission is granted, false otherwise
 */
export declare const checkMicrophonePermission: () => Promise<boolean>;
/**
 * Prompts the user to grant microphone access if not already authorized (Android only)
 * @returns True if permission granted or already exists, false if denied
 */
export declare const requestMicrophonePermission: () => Promise<boolean>;
export default VoiceStream;
