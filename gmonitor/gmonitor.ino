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

#define CONFIG_SECTOR 0x80-4
#define CONFIG_ADDRESS ( (CONFIG_SECTOR) * 4096 )
#define CONFIG_SPECIFIED_MARKER 107

struct {
   byte hasData;
   byte brightness;
} conf;

#ifdef ESP8266
#include <TimeLib.h>
#include <ESP8266WiFi.h>          //ESP8266 Core WiFi Library (you most likely already have this in your sketch)

#include <DNSServer.h>            //Local DNS Server used for redirecting all requests to the configuration portal
#include <ESP8266WebServer.h>     //Local WebServer used to serve the configuration portal
#include <WiFiManager.h>          //https://github.com/tzapu/WiFiManager WiFi Configuration Magic

//#include "fonts/SystemFont5x7.h"

#include <NTPClient.h>
#include <WiFiUdp.h>

//#include "Adafruit_MQTT.h"
//#include "Adafruit_MQTT_Client.h"

WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);

char server[] = "dataservice.accuweather.com";

WiFiClient client;

//Adafruit_MQTT_Client mqtt(&client, IO_SERVER, IO_SERVERPORT, IO_USERNAME, IO_KEY);
//
//Adafruit_MQTT_Subscribe brightness = Adafruit_MQTT_Subscribe(&mqtt, IO_USERNAME "/feeds/garden-monitor.display-brightness");
//Adafruit_MQTT_Subscribe onoffbutton = Adafruit_MQTT_Subscribe(&mqtt, IO_USERNAME "/feeds/garden-monitor.control");

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
int hum;
float temp, tmpCorrection=0;
float g=0.4;

bool network = false;

void setup()
{
  Serial.begin(115200);
  Serial.println(F("Started device"));

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

    timeClient.begin();
  
//    brightness.setCallback(slidercallback);
//    onoffbutton.setCallback(onoffcallback);
//    
//    mqtt.subscribe(&brightness);
//    mqtt.subscribe(&onoffbutton);    
  }
  else {
    Serial.println(F("WiFi failed. Will not use wifi"));
  }
  spi_flash_read(CONFIG_ADDRESS ,(uint32 *)(&conf),sizeof(conf));
  #endif  

  ensureConfig();

  // DMD init
  dmd.setBrightness(conf.brightness);
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

 #ifdef ESP8266    
    if (network){
      netupdate();      
    }
 #endif

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

void ensureConfig(){
  Serial.print(F("Checking config. Brightness value is: "));
  Serial.println(conf.brightness);
  if (conf.hasData == CONFIG_SPECIFIED_MARKER) return;
  conf.hasData = CONFIG_SPECIFIED_MARKER;
  conf.brightness = 255;
}

//void delay2(int ms){
//  #ifdef ESP8266
//  MQTT_connect();
//      
//  mqtt.processPackets(ms);
//  
//  if(!mqtt.ping()) {
//    mqtt.disconnect();
//  }
//  #else
//    delay(ms);
//  #endif
//}

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
  if (r == 84 || r == 116){
    commandSetTime();
  }
#ifdef ESP8266
  else if (r == 82 || r == 114){
    commandRestart();
  }
  else if (r == 87 || r == 119){
    commandResetWifi();
  }
  else if (r == 66 || r == 98){
    commandSetBrightness();
  }
#endif  
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

#ifdef ESP8266
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

void commandResetWifi(){
  Serial.println(F("Resetting wifi"));
  dmd.end();
  WiFiManager wifiManager;
  wifiManager.resetSettings();
//  resetFunc();
}

void commandRestart(){
    Serial.println(F("Resetting device"));
    dmd.end();
    ESP.restart();
}

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
  float awt = fraction + decimal;
  Serial.print(F("Temperature from server:"));
  Serial.println(awt);
  resetParser();

  tmpCorrection *= 1-g;
  tmpCorrection += (awt-temp)*g;
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

void commandSetBrightness(){
  byte b = (byte)readNumeral(3);
  changeBrightness(b);
}

void changeBrightness(byte b){
  Serial.print(F("Setting brightness: "));
  Serial.println(b);
  conf.brightness = b;
  dmd.end();
//  Serial.println(F("Erasing config sector"));
  spi_flash_erase_sector(CONFIG_SECTOR);
//  Serial.println(F("Writing config data"));
  spi_flash_write(CONFIG_ADDRESS ,(uint32 *)(&conf),sizeof(conf));
//  Serial.println(F("Setting brighness to dmd"));
  dmd.begin();
  dmd.setBrightness(b);
//  Serial.println(F("Set brighness completed"));
}

//void slidercallback(double x) {
//  int b = (int)x;
//  changeBrightness((byte)b);
//}
//
//void onoffcallback(char *data, uint16_t len) {
//  Serial.print("Button value: ");
//  Serial.println(data);
//  if (len>=1) {
//    runCommand(int(*data));
//  }
//}
//
//void MQTT_connect() {
//  int8_t ret;
//
//  // Stop if already connected.
//  if (mqtt.connected()) {
//    return;
//  }
//
//  Serial.print(F("Connecting to MQTT... "));
//
//  uint8_t retries = 3;
//  while ((ret = mqtt.connect()) != 0) { // connect will return 0 for connected
//       Serial.println(mqtt.connectErrorString(ret));
//       Serial.println(F("Retrying MQTT connection in 10 seconds..."));
//       mqtt.disconnect();
//       delay(10000);  // wait 10 seconds
//       retries--;
//       if (retries == 0) {
//         // basically die and wait for WDT to reset me
//         while (1);
//       }
//  }
//  Serial.println(F("MQTT Connected!"));
//}
#endif
