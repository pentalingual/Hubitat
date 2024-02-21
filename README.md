# Hubitat

I've built these functional applications/device drivers and use them heavily, because I want my Hubitat automations to be integrated intelligently into my smart home. Since these apps and drivers haven't been ported to Hubitat previously, I decided to build them for myself. I share with the community to save anyone else the trouble. 

<strong>1. Solar: PowerView Inverter</strong>
SunSynk and SolArk Solar inverters use PowerView app for reporting and monitoring. This driver allows you to set a refresh schedule to pull the PowerView API. It maintains the current Inverter states as a device in Hubitat. These are based on Power production and energy flow, grid usage, and Battery charge. They can be used to trigger other automations.
   
<strong>2. Irrigation: Degree Days Timer</strong>
At UC Davis, A.J. Winkler developed a method to measure Heat Summation, and use this for standardizing agriculture decisions. This App applies that approach to irrigation and creates a dynamic watering schedule based on daily temperatures and even rain in your area. The built in calculator allows you to model what your watering schedule will look like. You can install multiple instances for different irrigation area rules.

<strong>3. Chime: Door Chime</strong> (fork of the original Brian Copeland build)
This App is a fork of the original Brian Copeland build, that simply makes a chime or beep, or Speech request when a door on the list opens. I fixed issues added functionality to interact and test your chime devices, and to block chimes during selected Hub modes, or HSM Status. You can install multiple instances for different chime rules.

<strong>4. Windows: Laptop Battery Driver</strong>
After setting up a Task Schedule on your battery powered Windows computer, this driver will read the output on a network shared location and keep Hubitat updated with your computer's battery state of charge. The Hubitat device can then be used to trigger other automations in Hubitat like charging based on your device's State of Charge.

<strong>5. Camera: Reolink IP Camera Driver</strong>
This device driver turns on or off notifications your Reolink IP Camera motion detection notifications. This is useful driver to turn on with HSM is activated so you get Reolink motion notifications once HSM is Armed. 
Requested: Add MotionSensor capability to get Active motion events in Hubitat from your Reolink IP Camera

Coming Soon:

<strong>6. HVAC: Smarter HVAC Manager (for two story homes)</strong>
Presently this is written into 5 different complicated interacting rules but want to simplify the interface and port it onto a coherent app. It monitors current and average house temperatures in a 2-story home and uses classic HVAC methods to integrate upstairs and downstairs thermostats into a single smart, interactive home-HVAC controller.
