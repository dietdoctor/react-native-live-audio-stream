import React, { useRef, useState } from 'react';
import {
  Button,
  EventSubscription,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import VoiceStreamer from 'react-native-voice-stream';

const VoiceStreamerConfig = {
  sampleRate: 24000,
  bufferSize: 2400,
};

//const { VoiceStreamer } = NativeModules;

function App() {
  const [isInitialized, setIsInitialized] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const audioListener = useRef<EventSubscription | null>(null);
  const [audioChunks, setAudioChunks] = useState<string[]>([]);

  const initVoiceStreamer = async () => {
    try {
      await VoiceStreamer.init(VoiceStreamerConfig);
      setIsInitialized(true);

      // audioListener.current = VoiceStreamer.listen(
      //   'data',
      //   (base64Payload: string) => {
      //     console.log(base64Payload);
      //     //setAudioChunks([...audioChunks, base64Payload.slice(0, 10)]);
      //   }
      // );
    } catch (error) {
      console.error(error);
    }
  };

  const startRecording = async () => {
    try {
      //if (audioListener.current) {
        await VoiceStreamer.start();
        setIsRecording(true);
      //}
    } catch (error) {
      console.error(error);
    }
  }

  return (
    <View style={styles.container}>
      <SafeAreaView />
      <StatusBar barStyle="dark-content" />
      <Text style={styles.text}>RN Voice stream</Text>
      <View>
      {isInitialized ? (
        <Text>🟢 Initialized successfully</Text>
      ) : (
        <View style={styles.buttonContainer}>
          <Button
            title="Initialize"
            color="white"
            disabled={isInitialized}
            onPress={initVoiceStreamer}
          />
        </View>
      )}
      {isInitialized ?
      (isRecording ? (
        <Text>🟢 Recording...</Text>
      ) : (
        <View style={styles.buttonContainer}>
          <Button
            title="Start recording"
            color="white"
            onPress={startRecording}
          />
        </View>
      )): null}
      </View>
      <View>
        <Text>Audio chunks: {audioChunks.length}</Text>
        {audioChunks.map((chunk, index) => (
          <Text key={index}>{chunk}</Text>
        ))}
      </View>
      <View style={styles.bottomSpace} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: 'white',
  },
  text: {
    fontSize: 24,
  },
  buttonContainer: {
    marginTop: 20,
    backgroundColor: 'black',
    paddingVertical: 8,
    paddingHorizontal: 20,
    borderRadius: 10,
  },
  bottomSpace: {
    height: 10,
  },
});

export default App;
