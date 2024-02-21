# Hubitat

<br>I've built these functional applications/device drivers and use them heavily, because I want my Hubitat automations to be integrated intelligently into my smart home. Since these apps and drivers haven't been ported to Hubitat previously, I decided to build them for myself. I share with the community to save anyone else the trouble. 
<br>
<br>
<br><strong>1.<big>Solar:</big> PowerView Inverter</strong><br>
SunSynk and SolArk Solar inverters use PowerView app for reporting and monitoring. This driver allows you to set a refresh schedule to pull the PowerView API. It maintains the current Inverter states as a device in Hubitat. These are based on Power production and energy flow, grid usage, and Battery charge. They can be used to trigger other automations.
<br>
<br><strong>2. <big>Irrigation:</big> Degree Days Timer</strong><br>
At UC Davis, A.J. Winkler developed a method to measure Heat Summation, and use this for standardizing agriculture decisions. This App applies that approach to irrigation and creates a dynamic watering schedule based on daily temperatures and even rain in your area. The built in calculator allows you to model what your watering schedule will look like. You can install multiple instances for different irrigation area rules.
<br>
<br><strong>3. <big>Chime:</big> Door Chime</strong> (fork of the original Brian Copeland build)<br>
This App is a fork of the original Brian Copeland build, that simply makes a chime or beep, or Speech request when a door on the list opens. I fixed issues added functionality to interact and test your chime devices, and to block chimes during selected Hub modes, or HSM Status. You can install multiple instances for different chime rules.
<br>
<br><strong>4. <big>Windows:</big> Laptop Battery Driver</strong><br>
After setting up a Task Schedule on your battery powered Windows computer, this driver will read the output on a network shared location and keep Hubitat updated with your computer's battery state of charge. The Hubitat device can then be used to trigger other automations in Hubitat like charging based on your device's State of Charge.
<br>
<br><strong>5. <big>Camera:</big> Reolink IP Camera Driver</strong><br>
This device driver turns on or off notifications your Reolink IP Camera motion detection notifications. This is useful driver to turn on with HSM is activated so you get Reolink motion notifications once HSM is Armed. 
<br>Requested: Add MotionSensor capability to get Active motion events in Hubitat from your Reolink IP Camera
<br>
<br><i>Coming Soon:</i>
<br>
<br><strong>6. <big>HVAC:</big> Smarter HVAC Manager (for two story homes)</strong><br>
<i>Presently this is written into 5 different complicated interacting rules but want to simplify the interface and port it onto a coherent app. It monitors current and average house temperatures in a 2-story home and uses classic HVAC methods to integrate upstairs and downstairs thermostats into a single smart, interactive home-HVAC controller.</i>
