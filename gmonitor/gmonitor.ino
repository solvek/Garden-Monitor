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
#include "Config.h"

#define DHTPIN 2     // what pin we're connected to
#define DHTTYPE DHT22   // DHT 22  (AM2302)
DHT dht(DHTPIN, DHTTYPE); //// Initialize DHT sensor for normal 16mhz Arduino
RTClib RTC;
DS3231 Clock;

#define panel_width 1
#define panel_heigh 1

#ifdef ESP8266
#include <TimeLib.h>
#include <ESP8266WiFi.h>          //ESP8266 Core WiFi Library (you most likely already have this in your sketch)

#include <DNSServer.h>            //Local DNS Server used for redirecting all requests to the configuration portal
#include <ESP8266WebServer.h>     //Local WebServer used to serve the configuration portal
#include <WiFiManager.h>          //https://github.com/tzapu/WiFiManager WiFi Configuration Magic

//#include "fonts/SystemFont5x7.h"

#include <NTPClient.h>
#include <WiFiUdp.h>
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);

char server[] = "dataservice.accuweather.com";

WiFiClient client;

//#define pin_A 16
//#define pin_B 12
//#define pin_sclk 0
//#define pin_noe 15
SPIDMD dmd(panel_width, panel_heigh/*, pin_noe, pin_A, pin_B, pin_sclk*/);  // DMD controls the entire display

#else
SoftDMD dmd(panel_width,panel_heigh);
#endif

//Variables
int chk;
int hum;  //Stores humidity value
int temp; //Stores temperature value

bool network = false;

void setup()
{
  Serial.begin(115200);

  #ifdef ESP8266
//  dmd.selectFont(SystemFont5x7);
//  Serial.println("Writing hello world");
//  dmd.drawString(5, 3, F("Conn"));
//  Serial.println("Printed");
  
  String apName = String("GM")+ESP.getChipId();  
//  Serial.print("AP Name:");
//  Serial.println(apName);
//  Serial.print("Chip id:");
//  Serial.println(ESP.getChipId());

//  dmd.end();
  WiFiManager wifiManager;
  int len = apName.length();
  char chars[len];
  apName.toCharArray(chars, len);
  wifiManager.setConfigPortalTimeout(600);
  Serial.println(F("WiFi init"));
  network = wifiManager.autoConnect(chars);

  if (network){
    Serial.println(F("WiFi initialized fine"));
  }
  else {
    Serial.println(F("WiFi failed. Will not use wifi"));
  }

  timeClient.begin();

//  dmd.begin();
  #endif  

  // DMD init
  dmd.setBrightness(255);
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
      dmd.drawBox(15,5,16,10, GRAPHICS_OFF);
      delay(500);
    }    

    readCommand();

    dmd.clearScreen();
    dmd.drawString(1,3,padZero(now.day())+F(".")+padZero(now.month()));    
    int dow = Clock.getDoW();

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
    temp= dht.readTemperature();
//    temp = 44;
    dmd.clearScreen();
    String t = String();
    if (temp>0) t += String(F("+"));
    t +=String(temp)+F("&");
    dmd.drawString(((temp>-10) && (temp<10)) ? 10 : 4,3,t);
    dmd.drawLine(0,0,hum-1,0);
    dmd.drawLine(0,15,hum-1,15);
    
    delay(DELAY_TEMP*1000);

//    Serial.println();

    readCommand();

    if (network) netupdate();
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

long nowt, lastntp=-1, lastweather=-1;
bool needsStop = false;

char temp_marker[]="\"Metric\":{\"Value\":";
#define STATE_MARKER 1
#define STATE_DECIMAL 2
#define STATE_FRACTION 3

int state;
int markerPos;
int decimal;
float fraction, multiplier;

void netupdate(){
  nowt = millis();

//  Serial.println("Checking for timely updates");

  if (exceeded(lastntp, PERIOD_UPDATE_TIME)){
   updateTime();
  }

  if (exceeded(lastweather, PERIOD_REQUEST_WEATHER)){
    requestWeather();
  }

  while (client.available()) {
    handleResponse();
  }

  if (needsStop && !client.connected()){
    client.stop();
    needsStop = false;
  }
}

void requestWeather(){
  resetParser();
  if (!client.connect(server, 80)){
    Serial.println(F("Failed to request weather"));
    return;
  }
  Serial.println(F("Connected to server"));
  // Make a HTTP request:
  String req = F("GET /currentconditions/v1/");
  req += AW_LOCATION_KEY;
  req += F("?apikey=");
  req += API_KEY;
  req += F(" HTTP/1.1");
//  Serial.print(F("Req:"));
//  Serial.println(req);
  client.println(req);
  client.print(F("Host: "));
  client.println(server);
  client.println(F("Connection: close"));
  client.println();
  
  needsStop = true;
  lastweather = nowt;
}

void handleResponse(){
  char c = client.read();
//  Serial.print(c);
  int d = c - '0';
  switch(state){
    case STATE_MARKER:
      if (c==temp_marker[markerPos]){
        markerPos++;
//        Serial.print(F("Probably marker collected: "));
//        Serial.print(markerPos);
//        Serial.print(F(", symbol: "));
//        Serial.println(c);
                
        if (markerPos >= sizeof(temp_marker)-1){
          state = STATE_DECIMAL;
        }        
        break;
      }
      resetParser();
      break;
    case STATE_DECIMAL:
//      Serial.print(F("Decimal char: "));
//      Serial.println(c);
      if (c=='.'){
        state = STATE_FRACTION;
        break;
      }
      if (d<0 || d>9){
        onTemperatureLoaded();
      }
      else {
        decimal *= 10;
        decimal += d;
      }
      break;
     case STATE_FRACTION:
//      Serial.print(F("Fraction char: "));
//      Serial.println(c);     
      if (d<0 || d>9){
        onTemperatureLoaded();
      }
      else {
        multiplier /= 10.0;
        fraction += multiplier*d;
      }
  }
}

void onTemperatureLoaded(){
  float tempearature = fraction + decimal;
  Serial.print(F("Temperature from server:"));
  Serial.println(tempearature);
  resetParser();
}

void resetParser(){
  state = STATE_MARKER;
  markerPos = 0;
  decimal = 0;
  fraction = 0;
  multiplier = 1.0;
}

void updateTime(){
   Serial.println(F("Updating time..."));
    if (!timeClient.update()){
      Serial.println(F("Failed to update time"));
      return;
    }
    Serial.println(F("Received ntp"));

//      long epoch = 1537678727; // 2018-09-23 7:58:47, Sunday, Kyiv, DST
//      long epoch = 1518717827; // 2018-02-15 20:03:47, Thursday, Kyiv, NO DST
//      long epoch = 1553950800; // 2019-03-30 15:00:00, Saturnday, Kyiv, NO DST
//      long epoch = 1554004800; // 2019-03-31 07:00:00, Sunday, Kyiv, DST

    long epoch = timeClient.getEpochTime();
    epoch = toLocal(epoch, SHIFT_TIMEZONE, SHIFT_DST);

//    Serial.print(F("Time: "));
//    Serial.println(timeClient.getFormattedTime());
    Clock.setYear(year(epoch));
    Clock.setMonth(month(epoch));
    Clock.setDate(day(epoch));
    int dow = (weekday(epoch)+5)%7+1;
    Serial.print(F("Setting day of week: "));
    Serial.println(dow);
    Clock.setDoW(dow);
    Clock.setHour(hour(epoch));
    Clock.setMinute(minute(epoch));
    Clock.setSecond(second(epoch));    
    lastntp = nowt;
}

bool exceeded(long last, long period){
//  Serial.print("nowt=");
//  Serial.println(nowt);
//  Serial.print("period=");
//  Serial.println(period);
//  Serial.print("last=");
//  Serial.println(last);
  if (last < 0) return true;
  
  if (nowt > last){
    return nowt - last > period;
  }

  return 2147483647-last > period-nowt;
}

// utc in seconds, shift and shiftDst in minutes, result in seconds
long toLocal(long utc, long shift, long shiftDst){
  long r = utc + shift*60;
  int m = month(r);

  if (((m>3)&&(m<10))
  ||((m==3)&&!hasSundayAfter(r))
  ||((m==10)&&hasSundayAfter(r))) {
    r += shiftDst*60;
  }

  return r;
}

// Whethere there is a Sunday in current month after unix date t
bool hasSundayAfter(long t){  
  int m = month(t);
  do{
    t+=24*60*60;
    if (weekday(t)==1) return true;
  }
  while(month(t)==m);
  return false;
}

