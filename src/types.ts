import {EmitterSubscription, NativeModule } from 'react-native';

export interface VoiceStreamOptions {
    sampleRate?: number;
    bufferSize?: number;
    channels?: number;
    encoding?: string;
}
  
export interface VoiceStreamNativeModule extends NativeModule {
    init(options: VoiceStreamOptions): Promise<void>;
    start(): Promise<void>;
    stop(): Promise<void>;
    checkMicrophonePermission(): Promise<boolean>; //only for iOS
    requestMicrophonePermission(): Promise<boolean>; //only for iOS
}
  
export interface VoiceStreamerInterface {
    init(options: VoiceStreamOptions): Promise<void>;
    start(): Promise<void>;
    stop(): Promise<void>;
    listen(event: 'data', callback: (data: string) => void): EmitterSubscription;
}