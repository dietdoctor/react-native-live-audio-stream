#import <AVFoundation/AVFoundation.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTLog.h>

#define kNumberBuffers 3

typedef struct {
    AudioStreamBasicDescription mDataFormat;
    AudioQueueRef mQueue;
    AudioQueueBufferRef mBuffers[kNumberBuffers];
    UInt32 bufferByteSize;
    BOOL mIsRunning;
    __unsafe_unretained id mSelf;
} AQRecordState;

@interface VoiceStream: RCTEventEmitter <RCTBridgeModule> {
    AQRecordState _recordState;
}
@end