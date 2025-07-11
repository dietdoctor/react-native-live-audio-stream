import React from 'react';
import {
  Text,
  TouchableWithoutFeedbackProps,
  StyleSheet,
  TouchableWithoutFeedback,
} from 'react-native';

export const Button = (props: TouchableWithoutFeedbackProps & { title: string }) => {
  return (
    <TouchableWithoutFeedback {...props} style={defaultStyles.button}>
      <Text style={defaultStyles.text}>{props.title}</Text>
    </TouchableWithoutFeedback>
  );
};

const defaultStyles = StyleSheet.create({
  button: {
    backgroundColor: 'red',
  },
  text: {
    color: 'white',
    fontSize: 16,
    textAlign: 'center',
    paddingHorizontal: 32,
    paddingVertical: 8,
  },
});
