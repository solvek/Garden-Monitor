/* 
 *  To set datetime execute command "#TYYMMDDHHmmSS"
 *  For example "#T1804261107"
*/

//Libraries
#include <DHT.h>;
#include <Wire.h>
#include "DS3231.h"

#include <SPI.h>
#include <DMD2.h>
#include "fonts/GMSolvek.h"

//Constants
#define DELAY_TIME 10*1000
#define DELAY_DATE 3*1000
#define DELAY_TEMP 3*1000


#define DHTPIN 2     // what pin we're connected to
#define DHTTYPE DHT22   // DHT 22  (AM2302)
DHT dht(DHTPIN, DHTTYPE); //// Initialize DHT sensor for normal 16mhz Arduino
RTClib RTC;
DS3231 Clock;

SoftDMD dmd(1,1);

//Variables
int chk;
int hum;  //Stores humidity value
int temp; //Stores temperature value

void setup()
{
  Serial.begin(9600);
  
  // DHT init
  dht.begin();
  Wire.begin();

  // DMD init
  dmd.setBrightness(255);
  dmd.selectFont(GMSolvek);
  dmd.begin();
}

void loop()
{
    DateTime now = RTC.now();

    String t = padZero(now.hour())+F(":")+padZero(now.minute());
//    String t = "28:88";
//    Serial.println(t);
    dmd.clearScreen();
    dmd.drawString(1,3,t);
    delay(DELAY_TIME);

    readCommand();

    t = padZero(now.day())+F(".")+padZero(now.month());
//    Serial.println(t);
    dmd.clearScreen();
    dmd.drawString(1,3,t);
    delay(DELAY_DATE);

    readCommand();
  
    //Read data and store it to variables hum and temp
//    hum = dht.readHumidity();
    temp= dht.readTemperature();
    //Print temp and humidity values to serial monitor
//    t = String(temp)+F("&")+hum+F("%");
    t = String(temp)+F("&");
//    Serial.println(t);
    dmd.clearScreen();
    dmd.drawString(6,3,t);    
    delay(DELAY_TEMP);

//    Serial.println();

    readCommand();
}

void readCommand(){
//  Serial.println(F("Checking command"));
  int r;
  do{   
    r = Serial.read();
    if (r == -1) return;
//    Serial.print(F("Read symbol: "));
//    Serial.println(r);
  }while(r != 35); // # char

//  Serial.println(F("Expecting command"));
  r = inputSymbol();
  
  if (r == 84 || r == 116){
    commandSetTime();
  }
  else {
    Serial.print(F("Unknown command: "));
    Serial.println(r);
  }
}

// Datetime in format YYMMDDHHmmSS
void commandSetTime(){
//  Serial.println("Setting time");
  Clock.setYear(readNumeral(2));
  Clock.setMonth(readNumeral(2));
  Clock.setDate(readNumeral(2));
  Clock.setHour(readNumeral(2));
  Clock.setMinute(readNumeral(2));
  Clock.setSecond(readNumeral(2));
}

int readNumeral(int count){
  int r = 0;
  for(int i=0;i<count;i++){
    r *= 10;
    r += inputSymbol()-48;
  }
  return r;
}

int inputSymbol(){
  int s;
  while((s=Serial.read()) == -1);
  return s;
}

String padZero(int val){
  String s = String(val);
  if (val < 10) s = "0"+s;
  return s;
}

