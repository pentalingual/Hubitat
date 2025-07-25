/*
 * Reolink IP Camera
 *
 *
 *  Change History:
 *
 *      Date          Source        Version     What
 *      ----          ------        -------     ----
 *      2023-02-19    pentalingual  0.1.0       Starting version
 *      2024-02-20    pentalingual  0.2.0       Added improved connection management
 *	2024-09-07    pentalingual  0.3.1       Fixed glitch when token expired and turning on/off
 *      2025-07-25    pentalingual  0.3.2       Improved token and error handling
 */

static String version() { return '0.3.2' }

metadata {
    definition(
       name: "Reolink IP Camera",
       namespace: "pentalingual",
       author: "Andrew Nunes",
       description: "Turns on or off your Reolink IP Camera's motion detection push/email notifications",
	   category: "Integrations",
       importUrl: "https://raw.githubusercontent.com/pentalingual/Hubitat/main/Camera/Reolink_IP_Camera.groovy",
	   iconUrl: "",
	   iconX2Url: ""
    )  
    {
       capability "Refresh"
       capability "Switch"
       // capability "MotionSensor" Not yet encorporated

       attribute "switch", "enum", ["on", "off"]
       attribute "lastresponsetime", "Date"
       attribute "loginURL", "string"
       attribute "CurrentStatus", "string"
    }

    preferences {
            input name: "Blank0",  title: "<center><strong>This driver will use the API connection with your Reolink IP Camera to turn its notifications on and off.</strong></center>", type: "hidden"
            input name: "Instructions", title: "<center><i>To make it work, you'll need to know your Camera's IP Address and provide the Reolink API your camera's local username and password.</i></center>", type: "hidden"
            input name: "Blank1",  title: "<center>For email notifications, you will need to go to the camera's setting page, click into Surveillance, and provide the email settings from your email provider.</center>",  type: "hidden"    
            input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
            input name: "pushEnable", type: "bool", title: "Control Reolink app Push notifications On/Off?", defaultValue: false
            input name: "emailEnable", type: "bool", title: "Control Email notifications On/Off?", defaultValue: false
            input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
            input name: "ipAddress", type: "string", title: "Camera IP Address", defaultValue: null
            input name: "Username", type: "string", title: "Camera Username", defaultValue: null
            input name: "Password", type: "password", title: "Camera Password", hidden: true, defaultValue: null
    }
}

def getToken() {
    if (logEnable) log.info("Getting a new login token") 
    body1 = '[{"cmd":"Login", "param":{ "User":{ "Version": "0", "userName":"' 
    body2 = '", "password":"'
    body3 = '"}}}]'
    uriA = "http://${ipAddress}/api.cgi?cmd=Login"
    uriB = "http://${ipAddress}/cgi-bin/api.cgi?cmd=Login"
    def paramsTOK = [  
         uri: uriB,
         contentType: "application/json",
         body: "${body1}${Username}${body2}${Password}${body3}"
     ]
    try {
    httpPost(paramsTOK) {resp ->
        tokenResp = resp.getData()
        state.tokenKey = tokenResp.value.Token.name[0]
        state.attemptsNo = 1
    }
    if(state.statusIs  == "Turning On Notifications") turningOn()
    if(state.statusIs  == "Turning Off Notifications") turningOff()
    if(state.statusIs  == "Refreshing...") getCurrentStatus()
    sendEvent(name: "loginURL", value: "<html><a href='http://${ipAddress}/?token=${state.tokenKey}' target='_blank' and rel='noopener noreferrer'>Click to view Feed</a></html>")
    state.Connection = "Valid"
}  catch (exception) {
        log.error exception
        sendEvent(name: "CurrentStatus" , value: "Unable to access Host")
        state.statusIs = "Idle"
        log.error("There was an error accessing the device, check to make sure the IP address is valid, the device is online and on the Hubitat's network.")
    } 
}




def refresh() {
    sendEvent(name: "CurrentStatus" , value: "Refreshing...")
    state.statusIs = "Refreshing..."
    state.attemptsNo = 0  
    if(state.Connection == "Valid") {
    getCurrentStatus()
    } else {
        getToken()
    }
}
    
def getCurrentStatus() {   
    if (logEnable) log.info("Checking current status")
    uri = "http://${ipAddress}/api.cgi?cmd=GetPush&token=${state.tokenKey}"
    def paramsPushREF = [
        uri: uri,
        contentType: "application/json",
    ]
    try {
    httpGet(paramsPushREF) {resp ->
        pushSched = resp.getData().value.Push.schedule
        if(logEnable) log.debug("Push notifications schedule: ${pushSched[0]}")
        if (pushSched.enable[0] ==1) {
            if(pushEnable) sendEvent(name: "switch", value: "on")
            if(txtEnable || logEnable) log.info("Reolink camera push notifications are on")
            sendEvent(name: "CurrentStatus" , value: "Idle")
            state.statusIs = "Idle"
        } else {
            if (pushSched.enable[0] ==0) {
                if(pushEnable) sendEvent(name: "switch", value: "off") 
                if(txtEnable || logEnable) log.info("Reolink camera push notifications are off")
                sendEvent(name: "CurrentStatus" , value:"Idle")
                state.statusIs = "Idle"
            } else {         
                if (logEnable) log.error "token may have expired, trying to get a new one; number of attempts is ${state.attemptsNo} and token is ${state.tokenKey} "
                if (state.attemptsNo == 0) { 
                    getToken() 
                } else {
                    sendEvent(name: "CurrentStatus" , value: "Unable to login")
                    state.statusIs = "Idle"
                    log.error("There was an error accessing the device, check your credentials and ensure you haven't reached the maximum number of calls in a 30 min session.")
                }
            }
        }
    }
    uri =  "http://${ipAddress}/api.cgi?cmd=GetEmail&token=${state.tokenKey}"
    def paramsEmailREF = [
        uri: uri,
        contentType: "application/json",
    ]
    httpGet(paramsEmailREF) {resp->
        emailSched = resp.getData().value.Email.schedule
        if(logEnable) log.debug("Email notifications schedule: ${emailSched[0]}")
        if (emailSched.enable[0] ==1) {
            if(emailEnable) sendEvent(name: "switch", value: "on")
            if(txtEnable || logEnable) log.info("Reolink camera Email notifications are on")
            sendEvent(name: "CurrentStatus" , value: "Idle")
            state.statusIs = "Idle"
        } else { 
            if (emailSched.enable[0] ==0) {
                if(emailEnable) sendEvent(name: "switch", value: "off") 
                if(txtEnable || logEnable) log.info("Reolink camera Email notifications are off")
                sendEvent(name: "CurrentStatus" , value:"Idle")
                state.statusIs = "Idle"
            } else {
                if(emailEnable) sendEvent(name: "switch", value: "off") 
                if(logEnable) log.info("Reolink camera Email server is not set up")
                sendEvent(name: "CurrentStatus" , value:"Idle")
                state.statusIs = "Idle"
                    
                }
                }}

}  catch (exception) {
        log.error exception
        state.Connection = "Error"
                if (state.attemptsNo == 0) { 
            getToken() 
            } else {
        sendEvent(name: "CurrentStatus" , value: "Unable to access Host")
        state.statusIs = "Idle"
        log.error("There was an error accessing the device, check to make sure the IP address is valid, the device is online and on the Hubitat's network.")
    } }
}


def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable}"
    state.attemptsNo = 0
}


def on() {

    sendEvent(name: "CurrentStatus" , value: "Turning On Notifications")
    state.statusIs = "Turning On Notifications"
    state.attemptsNo = 0 
    turningOn()
    }

    
def turningOn() {
    
try {      
    if ( pushEnable ) {
        if (logEnable) log.info("Turning on Push Notifications")
        uri = "http://${ipAddress}/api.cgi?cmd=SetPush&token=${state.tokenKey}"
        def paramsON = [
            uri: uri,
            contentType: "application/json",
            body: '[{ "cmd":"SetPush", "param":{ "Push":{"schedule":{"enable":1,"table":"111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111"}}}}]'
        ]
        httpPost(paramsON) {resp ->
            if (logEnable) log.debug(resp.getData().value )
        }
    }
    if ( emailEnable ) {
        if (logEnable) log.info("Turning on Email Notifications")
        emailURI = "http://${ipAddress}/api.cgi?cmd=SetEmail&token=${state.tokenKey}"
        def paramsEmail = [
            uri: emailURI,
            contentType: "application/json",
            body: '[{ "cmd":"SetEmail", "param":{ "Email":{"schedule":{"enable":1,"table":"111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111"}}}}]'
        ]
        httpPost(paramsEmail) {resp ->
            if (logEnable) log.debug(resp.getData().value )
        }
    }    
    runIn(2, getCurrentStatus)
    }   catch (exception) {
        log.error exception
        state.Connection = "Error"
                if (state.attemptsNo == 0) { 
            getToken() 
            } else {
        sendEvent(name: "CurrentStatus" , value: "Unable to access Host")
        state.statusIs = "Idle"
        log.error("There was an error accessing the device, check to make sure the IP address is valid, the device is online and on the Hubitat's network.")
    } }
}



def off() {
    sendEvent(name: "CurrentStatus" , value: "Turning Off Notifications")
    state.statusIs = "Turning Off Notifications"
    state.attemptsNo = 0  
turningOff()
    }
        

def turningOff() {
          try {      
    if ( pushEnable ) {
        if (logEnable) log.info("Turning off Push Notifications")
        uri = "http://${ipAddress}/api.cgi?cmd=SetPush&token=${state.tokenKey}"
        def paramsOFF = [
            uri: uri,
            contentType: "application/json",
            body: '[{ "cmd":"SetPush", "param":{ "Push":{"schedule":{"enable":0,"table":"000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"}}}}]'
        ]
        httpPost(paramsOFF) {resp ->
            if (logEnable) log.debug(resp.getData().value )
        } 
    }
    if ( emailEnable ) {
        if (logEnable) log.info("Turning off Email Notifications")
        emailURI = "http://${ipAddress}/api.cgi?cmd=SetEmail&token=${state.tokenKey}"
        def paramsEmailoff = [
            uri: emailURI,
            contentType: "application/json",
            body: '[{ "cmd":"SetEmail", "param":{ "Email":{"schedule":{"enable":0,"table":"000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"}}}}]'
        ]
        httpPost(paramsEmailoff) {resp ->
            if (logEnable) log.debug(resp.getData().value )}
        }   
    runIn(2, getCurrentStatus)
    }        catch (exception) {
        log.error exception
        state.Connection = "Error"
                if (state.attemptsNo == 0) { 
            getToken() 
            } else {
        sendEvent(name: "CurrentStatus" , value: "Unable to access Host")
        state.statusIs = "Idle"
        log.error("There was an error accessing the device, check to make sure the IP address is valid, the device is online and on the Hubitat's network.")
    } }
}
