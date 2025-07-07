import React, { useState } from 'react';
import { Button, SafeAreaView, StatusBar, StyleSheet, Text, View, NativeModules } from 'react-native';


const voiceStreamConfig = {
  sampleRate: 24000,
  bufferSize: 2400,
}

const { VoiceStream } = NativeModules;


function App() {
  const [isInitialized, setIsInitialized] = useState(false);

const initVoiceStream = async () => {
  try {
    await VoiceStream.init(voiceStreamConfig);
    setIsInitialized(true);
  } catch (error) {
    console.error(error);
  }
}

  return (
    <View style={styles.container}>
      <SafeAreaView/>
      <StatusBar barStyle={ 'dark-content'} />
      <Text style={styles.text}>RN Voice stream</Text>
      {isInitialized ? <Text>Initialized successfully 🟢</Text> : <></>}
      <View style={styles.buttonContainer}>
        <Button title="Initialize" color="white" onPress={initVoiceStream} />
      </View>
      <View style={styles.bottomSpace}>
        <></>
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-between',
    alignItems: 'center',
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
