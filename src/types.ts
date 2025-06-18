// @ts-ignore
import {EmitterSubscription } from 'react-native';

export interface VoiceStreamOptions {
    sampleRate?: number;
    bufferSize?: number;
    channels?: number;
    encoding?: string;
}
  
export interface VoiceStreamNativeModule {
    init(options: VoiceStreamOptions): Promise<void>;
    start(): Promise<void>;
    stop(): Promise<void>;
}
  
export interface VoiceStreamerInterface {
    init(options: VoiceStreamOptions): Promise<void>;
    start(): Promise<void>;
    stop(): Promise<void>;
    listen(event: 'data', callback: (data: string) => void): EmitterSubscription;
}
  