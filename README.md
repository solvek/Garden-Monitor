
# Garden Monitor
A useful monitor for garden displaying information on dot-matrix
![enter image description here](images/dmd.jpg)

# Hardware
![enter image description here](images/wiring.png)
## [RTC](https://www.makeuseof.com/tag/how-and-why-to-add-a-real-time-clock-to-arduino/)
## P10 Dot-Matrix display
 * [DMD2 Library](https://github.com/freetronics/DMD2)
 * [Instructable article](http://www.instructables.com/id/Display-Text-at-P10-LED-Display-Using-Arduino/)
* [Linking dot matrix to Arduino](https://maker.pro/projects/arduino/arduino-led-matrix-controlled-android-app-greenpaks-i2c)
*  [Font creator (Java based, use GLCDFontCreator2.zip)](https://code.google.com/archive/p/glcd-arduino/downloads)
### Wiring
|Letter|Meaning|Wire|Arduino|ESP8266|D1/R1|
|--|--|--|--|--|--|
|OE|NOE|White|D9|GPIO15|D8|
|A|A|Purple|D6|GPIO16|D2|
|B|B|Orange|D7|GPIO12|D6|
|S|sck|Green|D13|GPIO14|D5|
|L|clk|Yellow|D8|GPIO0|D10|
|R|R|Blue|D11|GPIO13|D7|

[Connecting to ESP8266](http://forum.freetronics.com/viewtopic.php?t=6687)

## Other hardware
 * [Board WeMos D1](https://wiki.wemos.cc/products:d1:d1)
 * [Mapping GPIOs to Ds](https://jardikblog.wordpress.com/2016/11/02/wemos-d1-r1-vs-wemos-d1-r2/)
 * [How to Program ESP8266 with Arduino UNO](https://www.hackster.io/harshmangukiya/how-to-program-esp8266-with-arduino-uno-efb05f)
 * [WiFiManager](https://github.com/tzapu/WiFiManager)
	 * Running FontCreator:  `java -classpath . FontCreator`
 * [The 74HC595 Shift Register](https://learn.adafruit.com/adafruit-arduino-lesson-4-eight-leds/the-74hc595-shift-register)
# Others
 * [Online Markdown editor](https://stackedit.io)
