/*
 * Reolink IP Camera
 *
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                              URL
 *      ----          ------        -------     ----                                              ---
 *      2023-02-19    pentalingual  0.1.0       Starting version
 *      
 */

static String version() { return '0.1.0' }

metadata {
    definition(
       name: "Reolink IP Camera",
       namespace: "pentalingual",
       author: "Andrew Nunes",
       description: "Turns on or off your Reolink IP Camera motion detection notifications",
	   category: "Integrations",
       importUrl: "https://raw.githubusercontent.com/pentalingual/Hubitat/main/Camera/Reolink_IP_Camera.groovy",
	   iconUrl: "",
	   iconX2Url: ""
    )  
    {
       capability "Refresh"
       capability "Switch"
       capability "MotionSensor"
       capability "TestCapability"

       attribute "switch", "enum", ["on", "off"]
       attribute "lastresponsetime", "Date"
       attribute "videoURL", "string"
    }

    preferences {
            input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
            input name: "emailEnable", type: "bool", title: "Enable Email notifications when On?", defaultValue: false
            input name: "pushEnable", type: "bool", title: "Enable Reolink app Push notifications when On?", defaultValue: false
            input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
            input name: "ipAddress", type: "string", title: "Camera IP Address", defaultValue: null
            input name: "Username", type: "string", title: "Camera Username", defaultValue: null
            input name: "Password", type: "password", title: "Camera Password", hidden: true, defaultValue: null
    }
}


def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}


def refresh() {
    body1 = '[{"cmd":"Login", "param":{ "User":{ "Version": "0", "userName":"' 
    body2 = '", "password":"'
    body3 = '"}}}]'
    uri = "http://${ipAddress}/api.cgi?cmd=Login"
    def paramsTOK = [  
         uri: uri,
         contentType: "application/json",
         body: "${body1}${Username}${body2}${Password}${body3}"
     ]
    httpPost(paramsTOK) {resp ->
        tokenResp = resp.getData()
        tokenKey = tokenResp.value.Token.name[0]
    }
    sendEvent(name: "videoURL", value: "<html><a href='http://${ipAddress}?token=${tokenKey}' target='_blank' and rel='noopener noreferrer'>Click to view Feed</a></html>")
    if (logEnable) log.debug("Checking current status")
    uri = "http://${ipAddress}/api.cgi?cmd=GetPush&token=${tokenKey}"
    def paramsPushREF = [
        uri: uri,
        contentType: "application/json",
    ]
    httpGet(paramsPushREF) {resp ->
        pushSched = resp.getData().value.Push.schedule
        if(logEnable) log.debug("Push notifications schedule: ${pushSched[0]}")
        if (pushSched.enable[0] ==1) {
            if(pushEnable) sendEvent(name: "switch", value: "on")
            if(txtEnable) log.info("Reolink camera push notifications are on")
        } else {
            if (pushSched.enable[0] ==0) {
                if(pushEnable) sendEvent(name: "switch", value: "off") 
                if(txtEnable) log.info("Reolink camera push notifications are off")
            } else { if(txtEnable) log.info("There was an error accessing the device, check your credentials and ensure you haven't reached the maximum number of calls in a 30 min session.")
                 if(logEnable) log.debug(tokenResp)
            }
        }
    }
    uri =  "http://${ipAddress}/api.cgi?cmd=GetEmail&token=${tokenKey}"
    def paramsEmailREF = [
        uri: uri,
        contentType: "application/json",
    ]
    httpGet(paramsEmailREF) {resp->
        emailSched = resp.getData().value.Email.schedule
        emailAddress = 
        if(logEnable) log.debug("Email notifications schedule: ${emailSched[0]}")
        if (emailSched.enable[0] ==1) {
            if(emailEnable) sendEvent(name: "switch", value: "on")
            if(txtEnable) log.info("Reolink camera Email notifications are on")
        } else { 
            if (emailSched.enable[0] ==0) {
                if(emailEnable) sendEvent(name: "switch", value: "off") 
                if(txtEnable) log.info("Reolink camera Email notifications are off")
            } else { if(txtEnable) log.info("There was an error accessing the device, check your credentials and ensure you haven't reached the maximum number of calls in a 30 min session.")
                 if(logEnable) log.debug(tokenResp)
            }
        }
    }
}


def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable}"
}


def on() {
    body1 = '[{"cmd":"Login", "param":{ "User":{ "Version": "0", "userName":"' 
    body2 = '", "password":"'
    body3 = '"}}}]'
    uri = "http://${ipAddress}/api.cgi?cmd=Login"
    def paramsTOK = [  
         uri: uri,
         contentType: "application/json",
         body: "${body1}${Username}${body2}${Password}${body3}"
    ]
    httpPost(paramsTOK) {resp ->
        tokenKey = resp.getData().value.Token.name[0]
    }
    if ( pushEnable ) {
        if (logEnable) log.debug("Turning on Push Notifications")
        uri = "http://${ipAddress}/api.cgi?cmd=SetPush&token=${tokenKey}"
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
        if (logEnable) log.debug("Turning on Email Notifications")
        emailURI = "http://${ipAddress}/api.cgi?cmd=SetEmail&token=${tokenKey}"
        def paramsEmail = [
            uri: emailURI,
            contentType: "application/json",
            body: '[{ "cmd":"SetEmail", "param":{ "Email":{"schedule":{"enable":1,"table":"111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111"}}}}]'
        ]
        httpPost(paramsEmail) {resp ->
            if (logEnable) log.debug(resp.getData().value )
        }
    }    
    runIn(2, refresh)
}


def off() {
    body1 = '[{"cmd":"Login", "param":{ "User":{ "Version": "0", "userName":"' 
    body2 = '", "password":"'
    body3 = '"}}}]'
    uri = "http://${ipAddress}/api.cgi?cmd=Login"
    def paramsTOK = [  
     uri: uri,
     contentType: "application/json",
        body: "${body1}${Username}${body2}${Password}${body3}"
    ]
    httpPost(paramsTOK) {resp ->
        tokenKey = resp.getData().value.Token.name[0]
    }
    if ( pushEnable ) {
        if (logEnable) log.debug("Turning off Notifications")
        uri = "http://${ipAddress}/api.cgi?cmd=SetPush&token=${tokenKey}"
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
        emailURI = "http://${ipAddress}/api.cgi?cmd=SetEmail&token=${tokenKey}"
        def paramsEmailoff = [
            uri: emailURI,
            contentType: "application/json",
            body: '[{ "cmd":"SetEmail", "param":{ "Email":{"schedule":{"enable":0,"table":"000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"}}}}]'
        ]
        httpPost(paramsEmailoff) {resp ->
            if (logEnable) log.debug(resp.getData().value )}
        }   
    runIn(2, refresh)
}
