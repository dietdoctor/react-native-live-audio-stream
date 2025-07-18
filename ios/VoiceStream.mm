#import "VoiceStream.h"
#import <React/RCTLog.h>
#import <AVFoundation/AVFoundation.h>

@implementation VoiceStream

RCT_EXPORT_MODULE(NativeVoiceStream);

RCT_EXPORT_METHOD(init:(NSDictionary *) options) {
    RCTLogInfo(@"[VoiceStream] init");
    _recordState.mDataFormat.mSampleRate        = options[@"sampleRate"] == nil ? 48000 : [options[@"sampleRate"] doubleValue];
    _recordState.mDataFormat.mBitsPerChannel    = options[@"bitsPerSample"] == nil ? 16 : [options[@"bitsPerSample"] unsignedIntValue];
    _recordState.mDataFormat.mChannelsPerFrame  = options[@"channels"] == nil ? 1 : [options[@"channels"] unsignedIntValue];
    _recordState.mDataFormat.mBytesPerPacket    = (_recordState.mDataFormat.mBitsPerChannel / 8) * _recordState.mDataFormat.mChannelsPerFrame;
    _recordState.mDataFormat.mBytesPerFrame     = _recordState.mDataFormat.mBytesPerPacket;
    _recordState.mDataFormat.mFramesPerPacket   = 1;
    _recordState.mDataFormat.mReserved          = 0;
    _recordState.mDataFormat.mFormatID          = kAudioFormatLinearPCM;
    _recordState.mDataFormat.mFormatFlags       = _recordState.mDataFormat.mBitsPerChannel == 8 ? kLinearPCMFormatFlagIsPacked : (kLinearPCMFormatFlagIsSignedInteger | kLinearPCMFormatFlagIsPacked);
    _recordState.bufferByteSize                 = options[@"bufferSize"] == nil ? 2400 : [options[@"bufferSize"] unsignedIntValue];
    _recordState.mSelf = self;
}

RCT_EXPORT_METHOD(start) {
    RCTLogInfo(@"[VoiceStream] start");
    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    NSError *error = nil;
    BOOL success;

    // Apple recommended:
    // Instead of setting your category and mode properties independently, set them at the same time
    if (@available(iOS 10.0, *)) {
        success = [audioSession setCategory: AVAudioSessionCategoryPlayAndRecord
                                       mode: AVAudioSessionModeVoiceChat
                                    options: AVAudioSessionCategoryOptionDuckOthers |
                                             AVAudioSessionCategoryOptionAllowBluetooth |
                                             AVAudioSessionCategoryOptionAllowAirPlay
                                      error: &error];
    } else {
        success = [audioSession setCategory: AVAudioSessionCategoryPlayAndRecord withOptions: AVAudioSessionCategoryOptionDuckOthers error: &error];
        success = [audioSession setMode: AVAudioSessionModeVoiceChat error: &error] && success;
    }
    if (!success || error != nil) {
        RCTLog(@"[VoiceStream] Problem setting up AVAudioSession category and mode. Error: %@", error);
        return;
    }

    _recordState.mIsRunning = true;

    OSStatus status = AudioQueueNewInput(&_recordState.mDataFormat, HandleInputBuffer, &_recordState, NULL, NULL, 0, &_recordState.mQueue);
    if (status != 0) {
        RCTLog(@"[VoiceStream] Record Failed. Cannot initialize AudioQueueNewInput. status: %i", (int) status);
        return;
    }

    for (int i = 0; i < kNumberBuffers; i++) {
        AudioQueueAllocateBuffer(_recordState.mQueue, _recordState.bufferByteSize, &_recordState.mBuffers[i]);
        AudioQueueEnqueueBuffer(_recordState.mQueue, _recordState.mBuffers[i], 0, NULL);
    }
    AudioQueueStart(_recordState.mQueue, NULL);
}

RCT_EXPORT_METHOD(stop) {
    RCTLogInfo(@"[VoiceStream] stop");
    if (_recordState.mIsRunning) {
        _recordState.mIsRunning = false;
        AudioQueueStop(_recordState.mQueue, true);
        for (int i = 0; i < kNumberBuffers; i++) {
            AudioQueueFreeBuffer(_recordState.mQueue, _recordState.mBuffers[i]);
        }
        AudioQueueDispose(_recordState.mQueue, true);
    }
}

RCT_EXPORT_METHOD(checkMicrophonePermission:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    AVAudioSessionRecordPermission permission = [[AVAudioSession sharedInstance] recordPermission];
    BOOL granted = (permission == AVAudioSessionRecordPermissionGranted);
    resolve(@(granted));
}

RCT_EXPORT_METHOD(requestMicrophonePermission:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    [[AVAudioSession sharedInstance] requestRecordPermission:^(BOOL granted) {
        resolve(@(granted));
    }];
}

void HandleInputBuffer(void *inUserData,
                       AudioQueueRef inAQ,
                       AudioQueueBufferRef inBuffer,
                       const AudioTimeStamp *inStartTime,
                       UInt32 inNumPackets,
                       const AudioStreamPacketDescription *inPacketDesc) {
    AQRecordState* pRecordState = (AQRecordState *)inUserData;

    if (!pRecordState->mIsRunning) {
        return;
    }

    short *samples = (short *) inBuffer->mAudioData;
    long nsamples = inBuffer->mAudioDataByteSize;
    NSData *data = [NSData dataWithBytes:samples length:nsamples];
    NSString *str = [data base64EncodedStringWithOptions:0];
    RCTLogInfo(@"[VoiceStream] data: %@", str);
    [pRecordState->mSelf sendEventWithName:@"data" body:str];

    AudioQueueEnqueueBuffer(pRecordState->mQueue, inBuffer, 0, NULL);
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"data"];
}

- (void)dealloc {
    RCTLogInfo(@"[VoiceStream] dealloc");
    AudioQueueDispose(_recordState.mQueue, true);
}

@end