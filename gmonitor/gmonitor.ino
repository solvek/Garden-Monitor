/* 
 *  To set datetime execute command "#TYYMMDDDwHHmmSS"
 *  For example "#T180511051453"
*/

//Libraries
#include <DHT.h>;
#include <Wire.h>
#include "DS3231.h"

#include <SPI.h>
#include <DMD2.h>
#include "fonts/GMSolvek.h"

//Constants
#define DELAY_TIME 12
#define DELAY_DATE 3
#define DELAY_TEMP 4


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

    dmd.clearScreen();
    dmd.drawString(1,3,padZero(now.hour()));
    dmd.drawString(18,3,padZero(now.minute()));
    for(int i=0;i<DELAY_TIME;i++){
      dmd.drawBox(15,5,16,6);
      dmd.drawBox(15,9,16,10);
      delay(500);
      dmd.drawBox(15,5,16,10, GRAPHICS_OFF);
      delay(500);
    }    

    readCommand();

    dmd.clearScreen();
    dmd.drawString(1,3,padZero(now.day())+F(".")+padZero(now.month()));
    int dow = Clock.getDoW();
    Serial.println(dow);
    Serial.println(now.year());
    delay(DELAY_DATE*1000);

    readCommand();
  
    //Read data and store it to variables hum and temp
    hum = dht.readHumidity()*63/100;
    temp= dht.readTemperature();
//    temp = 44;
    dmd.clearScreen();
    String t = String();
    if (temp>0) t += String(F("+"));
    t +=String(temp)+F("&");
    dmd.drawString(((temp>-10) && (temp<10)) ? 10 : 4,3,t);
    dmd.drawLine(0,0,hum,0);
    dmd.drawLine(0,15,hum,15);
    
    delay(DELAY_TEMP*1000);

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

