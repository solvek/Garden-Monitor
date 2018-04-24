/* How to use the DHT-22 sensor with Arduino uno
   Temperature and humidity sensor
   More info: http://www.ardumotive.com/how-to-use-dht-22-sensor-en.html
   Dev: Michalis Vasilakis // Date: 1/7/2015 // www.ardumotive.com */

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

    Serial.print(padZero(now.day()));
    Serial.print(".");
    Serial.println(padZero(now.month()));
    delay(DELAY_DATE);
  
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
}

String padZero(int val){
  String s = String(val);
  if (val < 10) s = "0"+s;
  return s;
}

