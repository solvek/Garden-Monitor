// How long to show time on display (seconds)
#define DELAY_TIME 12
// How long to show date on display (seconds)
#define DELAY_DATE 4
// How long to show temperature on display (seconds)
#define DELAY_TEMP 4

// Shift in from UTC (minutes)
#define SHIFT_TIMEZONE 120
// Additional shift for DST (minutes)
#define SHIFT_DST 60

// Period for updating time (milliseconds)
#define PERIOD_UPDATE_TIME 24*60*60*1000

// Period for requesting weather conditions (milliseconds)
#define PERIOD_REQUEST_WEATHER 30*60*1000

// Accuweather APIKey
#define API_KEY "PUT YOU VALUE!"

// Accuweather location key. 324505 - Kyiv
#define AW_LOCATION_KEY 324505

// IO Adafruit
#define IO_SERVER "io.adafruit.com"
#define IO_SERVERPORT 1883 // use 8883 for SSL
#define IO_USERNAME    "PUT YOU VALUE!"
#define IO_KEY         "PUT YOU VALUE!"
