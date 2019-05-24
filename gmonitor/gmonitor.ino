/*

Copyright (c) 2012-2014 RedBearLab

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

*/

/*
 *    Chat
 *
 *    Simple chat sketch, work with the Chat iOS/Android App.
 *    Type something from the Arduino serial monitor to send
 *    to the Chat App or vice verse.
 *
 */

//"RBL_nRF8001.h/spi.h/boards.h" is needed in every new project
#include <SPI.h>
#include <EEPROM.h>
#include <boards.h>
#include <RBL_nRF8001.h>

#include <DMD2.h>
#include "fonts/GMSolvek.h"

#define panel_width 1
#define panel_heigh 1

#ifdef ESP8266
SPIDMD dmd(panel_width, panel_heigh, D3, D4, D6, D10);
#else
#define PIN_NOE 10 // White
#define PIN_A 7 // Purple
#define PIN_B 6 // Orange
#define PIN_SCK 5 // Green, Latch
#define PIN_CLK 4 // Yellow
#define PIN_R 3 // Blue, MOSI
SoftDMD dmd(panel_width,panel_heigh, PIN_NOE, PIN_A, PIN_B, PIN_SCK, PIN_CLK, PIN_R);
#endif

// Number of P10 panels used X, Y
DMD_TextBox box(dmd, 2, 1, 32, 16); 
//// Set Box (dmd, x, y, Height, Width)

#include <DHT.h>
#include <Wire.h>

#define DHTPIN 2     // what pin we're connected to
#define DHTTYPE DHT22   // DHT 22  (AM2302)
DHT dht(DHTPIN, DHTTYPE); //// Initialize DHT sensor for normal 16mhz Arduino

#include "DS3231.h"
RTClib RTC;
DS3231 Clock;

#include "Config.h"

#define STATE_TIME 1
#define STATE_TICK_ON 2
#define STATE_TICK_OFF 3
#define STATE_TICK_DATE 4
#define STATE_TICK_TEMP 5

byte state = 0;
long state_time;
long m, recentM = -100000;
byte tick;

void setup()
{  
  // Enable serial debug
  Serial.begin(57600);
  
  Serial.println(F("Started device"));
  dmd.setBrightness(20); // Set brightness 0 - 255 
  dmd.selectFont(GMSolvek); // Font used
  dmd.begin();     // Start DMD 
  Serial.println(F("Display initialized"));  
  
  // Default pins set to 9 and 8 for REQN and RDYN
  // Set your REQN and RDYN here before ble_begin() if you need
  // ble_set_pins(3, 2);
  
  // Set your BLE Shield name here, max. length 10
  ble_set_name("SolvekGM");
  
  // Init. and start BLE library.
  ble_begin();

  // DHT init
  dht.begin();
  Wire.begin();
}

unsigned char buf[16] = {0};
unsigned char len = 0;
int c;

void loop()
{
  m = millis();
  if (m-recentM>state_time) nextState();
  
  if ( ble_available() )
  {
    while ( ble_available() )
      Serial.write(ble_read());
      
    Serial.println();
  }
  
  if ( Serial.available() )
  {
    delay(5);
    
    while ( Serial.available() ){
      c = Serial.read();
      if (c==35) ble_begin();
//      Serial.println(c);
      ble_write(c);
    }
  }
  
  ble_do_events();
}

void nextState(){
  recentM = m;
  transitState();
  state_time = 500;
  switch(state){
    case STATE_TIME:
      showTime();
      tick = 0;
      break;
    case STATE_TICK_ON:
      showTickOn();
      break;            
    case STATE_TICK_OFF:
      showTickOff();
      break;      
    case STATE_TICK_DATE:
      state_time = DELAY_DATE*1000;
      showDate();
      break;      
    case STATE_TICK_TEMP:
      state_time = DELAY_TEMP*1000;
      showTemp();
      break;      
  }
}

void transitState(){
  if (state == STATE_TICK_OFF && tick<DELAY_TIME){
    state = STATE_TICK_ON;
    tick++;
  }
  else {
    state++;
  }
  if (state > STATE_TICK_TEMP) state= STATE_TIME;
}

DateTime now;
inline void showTime(){
  now = RTC.now();
  dmd.clearScreen();
  dmd.drawString(1,3,padZero(now.hour()));
  dmd.drawString(18,3,padZero(now.minute()));
}

inline void showTickOn(){
  dmd.drawBox(15,5,16,6);
  dmd.drawBox(15,9,16,10);
}

inline void showTickOff(){
  dmd.drawBox(15,5,16,10, GRAPHICS_OFF);
}

int dow;
inline void showDate(){
  dmd.clearScreen();
  dmd.drawString(1,3,padZero(now.day())+F(".")+padZero(now.month()));    
  dow = Clock.getDoW();

//    Serial.print(F("Day of week from RTC: "));
//    Serial.println(dow);

    dow = 33*(dow-1)/7;

    dmd.drawLine(dow,0,dow+3,0);
    dmd.drawLine(dow,15,dow+3,15);    
//    Serial.println(now.year());
}

int hum, temp, tmp;
float st;
String t;
inline void showTemp(){
  hum = dht.readHumidity()*63/100;
  st = dht.readTemperature();

  if (st == st){
    temp= st;
    t = padZero(st*10);
    ble_write('#');
    ble_write('K');
    ble_write(t.charAt(0));
    ble_write(t.charAt(1));
    ble_write(t.charAt(2));
    ble_write('\13');
  }else {
      Serial.println(F("Could not read temperature from sensor. Using previous value"));
  }

//  Serial.print(F("Sensor temperature: "));
//  Serial.println(temp);

  tmp = temp/*+tmpCorrection*/;
//    tmp = 44;
  dmd.clearScreen();
  t = String();
  if (tmp>0) t += String(F("+"));
  t +=String(tmp)+F("&");
  dmd.drawString(((tmp>-10) && (tmp<10)) ? 10 : 4,3,t);
  dmd.drawLine(0,0,hum-1,0);
  dmd.drawLine(0,15,hum-1,15);  
}

String padZero(int val){
  String s = String(val);
  if (val < 10) s = "0"+s;
  return s;
}
