import React, { useCallback, useRef, useState } from 'react';
import {
  Button,
  EventSubscription,
  PermissionsAndroid,
  Platform,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import VoiceStreamer from 'react-native-voice-stream';

const VoiceStreamerConfig = {
  sampleRate: 12000,
  bufferSize: 6400,
};

function App() {
  const [isInitialized, setIsInitialized] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const audioListener = useRef<EventSubscription | null>(null);
  const [audioChunks, setAudioChunks] = useState<string>('');


   const requestAudioPermission = async () => {
    if (Platform.OS === 'android') {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
      )
      return granted === PermissionsAndroid.RESULTS.GRANTED
    }
    return true
  }

  const initVoiceStreamer = async () => {
    try {
      await VoiceStreamer.init(VoiceStreamerConfig);
      setIsInitialized(true);

      audioListener.current = VoiceStreamer.listen(
        'data',
        (base64Payload: string) => {
          setAudioChunks(audioChunks + '\n' +base64Payload );
        }
      );
    } catch (error) {
      console.error(error);
    }
  };

  const startRecording = async () => {
    try {
      if (audioListener.current) {
        const granted = await requestAudioPermission();
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
  }

  const stopRecording = useCallback(async () => {
      console.log('stopRecording');
      await VoiceStreamer.stop()
      audioListener.current?.remove()
      setIsRecording(false);
      setIsInitialized(false);
  }, [])

  return (
    <View style={styles.container}>
      <SafeAreaView />
      <StatusBar barStyle="dark-content" />
      <Text style={styles.title}>RN Voice stream</Text>
      <View style={styles.statusContainer}>
      {isInitialized ? (
        <Text style={styles.statusText}>🟢 Initialized successfully</Text>
      ) : (
          <Button
            title="Initialize"
            color="black"
            disabled={isInitialized}
            onPress={initVoiceStreamer}
          />
      )}
      {isInitialized ?
      (isRecording ? (
        <View>
        <Text style={styles.statusText}>🟢 Recording...</Text>
          <Button
            title="Stop recording"
            color="black"
            onPress={stopRecording}
          />
        </View>
      ) : (
          <Button
            title="Start recording"
            color="black"
            onPress={startRecording}
          />
      )): null        
      }
      </View>
      <ScrollView contentContainerStyle={styles.scrollView}>
        <Text>Audio chunks: {audioChunks.length}</Text>
        <Text>{audioChunks}</Text>
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
  },
  title: {
    marginVertical: 20,
    fontSize: 24,
  },
  buttonContainer: {
    marginTop: 20,
    backgroundColor: 'black',
    paddingVertical: 8,
    paddingHorizontal: 20,
    borderRadius: 10,
  },
  statusContainer: {
    flex: 1,
  },
  statusText: {
    marginVertical: 4,
  },
  scrollView: {
    height: '50%',
    width: 300,
  },
});

export default App;
