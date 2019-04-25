#include <DHT.h>;
#include <Wire.h>
#include "DS3231.h"

#include <SPI.h>
#include <DMD2.h>
#include "fonts/GMSolvek.h"
#include "Config.h"

#define DHTPIN 2     // what pin we're connected to
#define DHTTYPE DHT22   // DHT 22  (AM2302)
DHT dht(DHTPIN, DHTTYPE); //// Initialize DHT sensor for normal 16mhz Arduino
RTClib RTC;
DS3231 Clock;

#define panel_width 1
#define panel_heigh 1

SoftDMD dmd(panel_width,panel_heigh);

//Variables
int chk;
int hum;
float temp, tmpCorrection=0;
float g=0.4;

void setup()
{
  Serial.begin(9600);
  Serial.println(F("Started device"));

  // DMD init
  dmd.setBrightness(128);
  dmd.selectFont(GMSolvek);
  dmd.begin();
  
  // DHT init
  dht.begin();
  Wire.begin();
}

void loop()
{
    DateTime now = RTC.now();

    dmd.clearScreen();
    dmd.drawString(1,3,padZero(now.hour()));
    dmd.drawString(18,3,padZero(now.minute()));
    for(int i=0;i<DELAY_TIME;i++){
      dmd.drawBox(15,5,16,6);
      dmd.drawBox(15,9,16,10);
      delay(500);
      dmd.drawBox(15,4,16,11, GRAPHICS_OFF);
      delay(500);
    }    

    readCommand();

    dmd.clearScreen();
    dmd.drawString(1,3,padZero(now.day())+F(".")+padZero(now.month()));    
    int dow = Clock.getDoW();

//    Serial.print(F("Day: "));
//    Serial.println(now.day());
//    Serial.print(F("Month: "));
//    Serial.println(now.month());
//    Serial.print(F("Day of week from RTC: "));
//    Serial.println(dow);

    dow = 33*(dow-1)/7;

    dmd.drawLine(dow,0,dow+3,0);
    dmd.drawLine(dow,15,dow+3,15);    
//    Serial.println(now.year());
    delay(DELAY_DATE*1000);

    readCommand();
  
    //Read data and store it to variables hum and temp
    hum = dht.readHumidity()*63/100;
    float st = dht.readTemperature();

    if (st == st){
      temp= st;
    }else {
      Serial.println(F("Could not read temperature from sensor. Using previous value"));
    }

    Serial.print(F("Sensor temperature: "));
    Serial.println(temp);

    int tmp = temp+tmpCorrection;
//    tmp = 44;
    dmd.clearScreen();
    String t = String();
    if (tmp>0) t += String(F("+"));
    t +=String(tmp)+F("&");
    dmd.drawString(((tmp>-10) && (tmp<10)) ? 10 : 4,3,t);
    dmd.drawLine(0,0,hum-1,0);
    dmd.drawLine(0,15,hum-1,15);
    
    delay(DELAY_TEMP*1000);

//    Serial.println();

    readCommand();
}

void(* resetFunc) (void) = 0; //declare reset function @ address 0

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
  runCommand(r);  
}

void runCommand(int r){
  if (r == 84 || r == 116){ // T
    commandSetTime();
  }
  else {
    Serial.print(F("Unknown command: "));
    Serial.println(r);
  }
}

// Datetime in format YYMMDDDWHHmmSS
void commandSetTime(){
//  Serial.println("Setting time");
//  int val;
//  val = readNumeral(2);
//  Serial.print(F("Year: "));
//  Serial.println(val);  
//  Clock.setYear(val);
//
//  val = readNumeral(2);
//  Serial.print(F("Month: "));
//  Serial.println(val);  
//  Clock.setMonth(val);
//
//  val = readNumeral(2);
//  Serial.print(F("Day: "));
//  Serial.println(val);  
//  Clock.setDate(val);
//
//  val = readNumeral(2);
//  Serial.print(F("DoW: "));
//  Serial.println(val);  
//  Clock.setDoW(val);
  
  Clock.setYear(readNumeral(2));
  Clock.setMonth(readNumeral(2));
  Clock.setDate(readNumeral(2));
  Clock.setDoW(readNumeral(2));
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