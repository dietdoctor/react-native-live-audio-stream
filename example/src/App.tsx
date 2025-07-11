import React, { useCallback, useRef, useState } from 'react';
import {
  EventSubscription,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import VoiceStreamer from 'react-native-voice-stream';
import { Button } from './components';

const VoiceStreamerConfig = {
  sampleRate: 12000,
  bufferSize: 6400,
};

function App() {
  const [isInitialized, setIsInitialized] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const audioListener = useRef<EventSubscription | null>(null);
  const [audioChunks, setAudioChunks] = useState<string>('');
  const [audioChunksCount, setAudioChunksCount] = useState<number>(0);

  const initVoiceStreamer = async () => {
    try {
      await VoiceStreamer.init(VoiceStreamerConfig);
      setIsInitialized(true);

      audioListener.current = VoiceStreamer.listen(
        'data',
        (base64Payload: string) => {
          setAudioChunks(prev => `${prev}${base64Payload.slice(0, 30)} ...\n`);
          setAudioChunksCount(prev => prev + 1);
        },
      );
    } catch (error) {
      console.error(error);
    }
  };

  const startRecording = async () => {
    try {
      if (audioListener.current) {
        const granted = await VoiceStreamer.checkMicrophonePermission();
        if (!granted) {
          console.log('Audio permission not granted');
          return;
        }
        await VoiceStreamer.start();
        setIsRecording(true);
      }
    } catch (error) {
      console.error(error);
    }
  };

  const stopRecording = useCallback(async () => {
    console.log('stopRecording');
    await VoiceStreamer.stop();
    audioListener.current?.remove();
    setIsRecording(false);
    setIsInitialized(false);
  }, []);

  const clearData = () => {
    setAudioChunks('');
    setAudioChunksCount(0);
  };

  return (
    <View style={styles.container}>
      <StatusBar
        translucent={true}
        backgroundColor="transparent"
        barStyle="dark-content"
      />
      <Text style={styles.title}>Voice Stream Demo</Text>

      <View
        style={{
          flexDirection: 'row',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        {/* Status */}
        <View style={styles.statusContainer}>
          {isInitialized && !isRecording ? (
            <Text style={styles.statusText}>🟢 Initialized successfully</Text>
          ) : isRecording ? (
            <Text style={styles.statusText}>🟢 Recording...</Text>
          ) : null}
        </View>
        <View>
          <Text style={{ marginBottom: 8, alignSelf: 'flex-end' }}>Config</Text>
          {Object.entries(VoiceStreamerConfig).map(([key, value]) => (
            <Text key={key} style={{ alignSelf: 'flex-end' }}>
              {key}: {value}
            </Text>
          ))}
        </View>
      </View>

      {/* Buttons */}
      <View style={styles.buttonContainer}>
        {!isInitialized ? (
          <Button
            title="Initialize"
            disabled={isInitialized}
            onPress={initVoiceStreamer}
          />
        ) : isRecording ? (
          <Button title="Stop recording" onPress={stopRecording} />
        ) : (
          <Button title="Start recording" onPress={startRecording} />
        )}
      </View>
      <View
        style={{
          flexDirection: 'row',
          justifyContent: 'space-between',
          marginVertical: 16,
          width: '100%',
        }}
      >
        <Text style={{ fontSize: 16, alignSelf: 'center' }}>
          Streamed chunks: {audioChunksCount}
        </Text>
        {!isRecording && audioChunks ? (
          <Text
            onPress={clearData}
            style={{ textDecorationLine: 'underline' }}
          >
            Clear
          </Text>
        ) : null}
      </View>
      <ScrollView style={{ flex: 1 }}>
        <Text style={{ fontSize: 14 }}>{audioChunks}</Text>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'column',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: 'white',
    paddingVertical: 48,
    paddingHorizontal: 32,
  },
  title: {
    marginVertical: 20,
    fontSize: 24,
  },
  buttonContainer: {
    marginVertical: 16,
    backgroundColor: 'black',
    paddingVertical: 8,
    paddingHorizontal: 20,
    borderRadius: 10,
  },
  statusContainer: {
    flex: 1,
    minHeight: 100,
  },
  statusText: {
    marginVertical: 4,
  },
});

export default App;
