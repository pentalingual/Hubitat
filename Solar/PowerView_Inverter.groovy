/*
 * PowerView Inverter
 *
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                              URL
 *      ----          ------        -------     ----                                              ---
 *      2023-09-24    pentalingual  0.1.0       Starting version
 *      2024-01-02    pentalingual  0.2.0       Added Token Refresh
 */

static String version() { return '0.1.0' }

metadata {
    definition(
            name: "PowerView Inverter",
            namespace: "pentalingual",
            author: "Andrew Nunes",
            description: "Leverages the PowerView API connection to update your SolArk or SunSynk inverter status",
            category: "Environmental",
            importUrl: "https://raw.githubusercontent.com/pentalingual/Hubitat/main/Solar/PowerView_Inverter.groovy"
    )  {
        capability "Refresh"
        capability "PowerMeter"
        capability "Battery"

        attribute "lastresponsetime", "string"
        attribute "PVPower", "number"
        attribute "LoadPowerDraw", "number"
        attribute "GridPowerDraw", "number"
        attribute "BatterySOC", "number"
    }

    preferences {
          input name: "Blank0",  title: "<center><strong>This driver will maintain an API connection with the PowerView portal to update Hubitat with your latest solar/battery inverter details.</strong></center>", type: "hidden"
    input name: "Instructions", title: "<center>**********<br><i>To make it work, you'll need to figure out your plant ID, and provide this driver your PowerView username and password</i></center>", type: "hidden"
  input name: "Blank1",  title: "<center>**********<br>The Plant ID is at the end of the URL when you navigate to the <a href='https://pv.inteless.com/plants/' target='_blank'>Plant Overview</a> page and click into your desired power plant.</center>",  type: "hidden"
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "refreshSched", type: "int", title: "Refresh every how many minutes?", defaultValue: 15  
        input name: "plantID", type: "string", title: "PowerView Plant ID", description: "<i><small>The Plant ID is at the end of the URL when you login and navigate to the desired plant https://pv.inteless.com/plants/overview/</i></small><strong>?????</strong>", defaultValue: null
        input name: "Username", type: "string", title: "PowerView Username", defaultValue: null
        input name: "Password", type: "password", title: "PowerView Password", hidden: true, defaultValue: null
    }
}


def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated... refreshing every ${refreshSched} minutes"
    schedule("0 0/${refreshSched} * * * ?", refresh)
    log.warn "debug logging is: ${logEnable}"
}

def getToken() {
    body1 = ['username':Username,'password':Password,'grant_type':'password','client_id':'csp-web','source':'elinter']
    def URIa = "https://openapi.inteless.com/v1/oauth/token"
    def URIb = "https://pv.inteless.com/api/v1/oauth/token"
    def URIc = "https://pv.inteless.com/oauth/token"
    def paramsTOK = [
        uri: URIc,
        headers: [
            'Origin': 'https://pv.inteless.com',
            'Referer': 'https://pv.inteless.com/login'
        ],
        body: body1
    ]
    httpPostJson(paramsTOK, { resp -> 
        state.TokenKey = resp.getData().data.access_token
        attemptsNo = 1
        queryData() 
    })
}


def refresh() {
    attemptsNo = 0
    queryData()
}


def queryData()  {
    def key = "Bearer ${state.TokenKey}"
    def paramsEnergy = [  
        uri: "https://pv.inteless.com/api/v1/plant/energy/${plantID}/flow",
        headers: [ 'Authorization' : key ],
        requestContentType: "application/x-www-form-urlencoded"
    ]
    try {
        httpGet(paramsEnergy, { resp ->
            sendEvent(name: 'PVPower', value: resp.getData().data.pvPower)
            sendEvent(name: 'LoadPowerDraw', value: resp.getData().data.loadOrEpsPower)
            sendEvent(name: 'GridPowerDraw', value: resp.getData().data.gridOrMeterPower)
            sendEvent(name: 'BatterySOC', value: resp.getData().data.soc)
        })
    } catch (exception) {
        log.error exception
        log.error "token may have expired, trying to get a new one; number of attempts is ${attemptsNo} and token is ${key} "
        if (attemptsNo == 0) { 
            getToken() 
        }
        return null
    }
}