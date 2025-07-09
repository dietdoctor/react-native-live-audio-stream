package com.dietdoctor.voicestream;

import com.facebook.react.TurboReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import com.facebook.react.module.model.ReactModuleInfo;

import java.util.HashMap;

public class VoiceStreamPackage extends TurboReactPackage {
    
    @Override
    public NativeModule getModule(String name, ReactApplicationContext reactContext) {
        if (name.equals(VoiceStreamModule.NAME)) {
            return new VoiceStreamModule(reactContext);
        }
        return null;
    }

    @Override
    public ReactModuleInfoProvider getReactModuleInfoProvider() {
        return () -> {
            final HashMap<String, ReactModuleInfo> moduleInfos = new HashMap<>();
            moduleInfos.put(
                VoiceStreamModule.NAME,
                new ReactModuleInfo(
                    VoiceStreamModule.NAME,
                    VoiceStreamModule.NAME,
                    false, // canOverrideExistingModule
                    false, // needsEagerInit
                    true,  // hasConstants
                    false, // isCxxModule
                    false  // isTurboModule (set to true if using new architecture)
                )
            );
            return moduleInfos;
        };
    }
} 