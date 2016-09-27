NeoLights: PC ambiant lights manager for NeoJava
------------------------------------------------

## 1 - Build
Use maven for build:

    mvn package -DskipTests

## 2 - Arduino part

Here's the code you must load in the Arduino part of the UDOO

```c_cpp
int led = 2;

int topRed, topGreen, topBlue; 
int bottomRed, bottomGreen, bottomBlue; 
int topRedPin = 10; //Red: PWM pin 10
int topGreenPin = 11; //Green: PWM pin 11
int topBluePin = 9; //Blue: PWM pin 9 
int bottomRedPin = 6; //Red: PWM pin 6
int bottomGreenPin = 7; //Green: PWM pin 7
int bottomBluePin = 5; //Blue: PWM pin 5

void setup() {

// initialize serial communication at 115200 bits per second:

Serial.begin(115200);

// configure led’s pin mode to output

pinMode(led, OUTPUT); 
int topRed = 255; 
int topBlue = 255; 
int topGreen = 255; 
}

void loop() {

if(Serial.available() >= 5){ 
  digitalWrite(led, HIGH); 
  int byteRead = Serial.read();
  if(byteRead == 0x1b){
int pos = Serial.read(); 
  if(pos == 0x00){ 
     topRed = Serial.read(); 
     topGreen= Serial.read(); 
     topBlue = Serial.read(); 
     analogWrite (topRedPin, topRed); 
     analogWrite (topGreenPin, topGreen); 
     analogWrite (topBluePin, topBlue); 
  } 
  else if(pos == 0x01){ 
     bottomRed = Serial.read(); 
     bottomGreen= Serial.read(); 
     bottomBlue = Serial.read(); 
     analogWrite (bottomRedPin, bottomRed); 
     analogWrite (bottomGreenPin, bottomGreen); 
     analogWrite (bottomBluePin, bottomBlue); 
  } 
  } 
} 
delay(100); 
digitalWrite(led, LOW); 
}
```

## 3 - Run

    java -jar NeoLights.jar {HOST} {PORT}
    
HOST and PORT are those of the UDOO Neo which is running NeoJava ( https://github.com/BOSSoNe0013/NeoJava )
You can use screen software to keep app running in background

## License

Copyright 2014 Cyril Bosselut

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.