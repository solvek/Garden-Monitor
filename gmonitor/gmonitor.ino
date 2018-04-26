/* 
 *  To set datetime execute command "#TYYMMDDHHmmSS"
 *  For example "#T1804261107"
*/

//Libraries
#include <DHT.h>;
#include <Wire.h>
#include "DS3231.h"

//Constants
#define DELAY_TIME 8*1000
#define DELAY_DATE 3*1000
#define DELAY_TEMP 3*1000


#define DHTPIN 2     // what pin we're connected to
#define DHTTYPE DHT22   // DHT 22  (AM2302)
DHT dht(DHTPIN, DHTTYPE); //// Initialize DHT sensor for normal 16mhz Arduino
RTClib RTC;
DS3231 Clock;

//Variables
int chk;
int hum;  //Stores humidity value
int temp; //Stores temperature value

void setup()
{
  Serial.begin(9600);
  dht.begin();
  Wire.begin();
}

void loop()
{
    DateTime now = RTC.now();
    
    Serial.print(padZero(now.hour()));
    Serial.print(":");
    Serial.println(padZero(now.minute()));
    delay(DELAY_TIME);

    readCommand();

    Serial.print(padZero(now.day()));
    Serial.print(".");
    Serial.println(padZero(now.month()));
    delay(DELAY_DATE);

    readCommand();
  
    //Read data and store it to variables hum and temp
    hum = dht.readHumidity();
    temp= dht.readTemperature();
    //Print temp and humidity values to serial monitor
    Serial.print(temp);
    Serial.print("º ");
    Serial.print(hum);
    Serial.println("%");
    delay(DELAY_TEMP);

    Serial.println();

    readCommand();
}

void readCommand(){
//  Serial.println("Checking command");
  int r;
  do{   
    r = Serial.read();
    if (r == -1) return;
//    Serial.print("Read symbol: ");
//    Serial.println(r);
  }while(r != 35); // # char

//  Serial.println("Expecting command");
  r = inputSymbol();
  
  if (r == 84 || r == 116){
    commandSetTime();
  }
  else {
    Serial.print("Unknown command: ");
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

