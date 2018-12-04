NeoLights: PC ambient lights manager for NeoJava
------------------------------------------------
[![NeoLights demo](http://img.youtube.com/vi/HDTILDFrtZg/0.jpg)](http://www.youtube.com/watch?v=HDTILDFrtZg "GNU/Linux computer ambiant light ")

## 1 - Build
Use maven for build:

    mvn package -DskipTests

## 2 - Arduino part

Here's the code you must load in the Arduino part of the UDOO

```Arduino
#include <Arduino.h>

#define TOP_STRIP 0x30
#define BOTTOM_STRIP 0x31

int led = 13;

int topRedPin = 10; //Red: PWM pin 10
int topGreenPin = 11; //Green: PWM pin 11
int topBluePin = 9; //Blue: PWM pin 9 
int bottomRedPin = 6; //Red: PWM pin 6
int bottomGreenPin = 7; //Green: PWM pin 7
int bottomBluePin = 5; //Blue: PWM pin 5 

void setup() { 

  // initialize serial communication at 115200 bits per second: 
  Serial.begin(115200); 
  
  // configure led's pin mode to output 
  pinMode(led, OUTPUT); 
  digitalWrite(led, HIGH); 
  setRGB(TOP_STRIP, 255, 0, 0);
  delay(100);
  setRGB(TOP_STRIP, 0, 255, 0);
  delay(100);
  setRGB(TOP_STRIP, 0, 0, 255);
  delay(100);
  setRGB(BOTTOM_STRIP, 0, 0, 255);
  delay(100);
  setRGB(BOTTOM_STRIP, 0, 255, 0);
  delay(100);
  setRGB(BOTTOM_STRIP, 255, 0, 0);
  delay(100);
  setRGB(TOP_STRIP, 255, 255, 255);
  setRGB(BOTTOM_STRIP, 255, 255, 255);
 
} 

void loop() { 
  
 if(Serial.available() >= 5){ 
   digitalWrite(led, HIGH); 
   int byteRead = Serial.read();
   if(byteRead == 0xff){
    int pos = Serial.read(); 
    int red = Serial.read(); 
    int green= Serial.read(); 
    int blue = Serial.read(); 
    setRGB(pos, red, green, blue);
   }
 }
 delay(10); 
 digitalWrite(led, LOW); 
}

void setRGB(int pos, int red, int green, int blue){
    if(pos == 0x30){ 
      analogWrite (topRedPin, red); 
      analogWrite (topGreenPin, green); 
      analogWrite (topBluePin, blue);
    } 
    else if(pos == 0x31){ 
      analogWrite (bottomRedPin, red); 
      analogWrite (bottomGreenPin, green); 
      analogWrite (bottomBluePin, blue); 
    } 
}

```

## 3 - Run

    java -jar NeoLights.jar {HOST} {PORT}
    
HOST and PORT are those of the UDOO Neo which is running NeoJava ( https://github.com/BOSSoNe0013/NeoJava )
You can use screen software to keep app running in background

## License

Copyright 2016 Cyril Bosselut

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.