/*
 * SolArk Inverter
 *
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                              URL
 *      ----          ------        -------     ----                                              ---
 *      2023-09-24    pentalingual  0.1.0       Starting version
 *      2024-01-02    pentalingual  0.2.0       Added Token Refresh
 *      2024-02-25    pentalingual  0.3.0       Added inverter Details & logging
 *      2024-06-11    pentalingual  0.4.0       Switched to MySolArk
 *      2024-07-04    pentalingual  0.4.2       Updated API Report Handling
 */

static String version() { return '0.4.2' }

metadata {
    definition(
            name: "SolArk Inverter",
            namespace: "pentalingual",
            author: "Andrew Nunes",
            description: "Leverages the MySolArk API to update Hubitat with your SolArk inverter status",
            category: "Integrations",
            importUrl: "https://raw.githubusercontent.com/pentalingual/Hubitat/main/Solar/SolArk_Inverter.groovy"
    )  {
        capability "Initialize"
        capability "Refresh"
        capability "PowerMeter"
        capability "PowerSource"
        capability "CurrentMeter"
        capability "Battery" 

        attribute "lastresponsetime", "string"
        attribute "PVPower", "number"
        attribute "GridPowerDraw", "number"
        attribute "BatteryDraw", "number"
        attribute "GeneratorDraw", "number"
        attribute "BatteryStatus", "string"
    }

    preferences {
        input name: "Blank0",  title: "<center><strong>This driver will maintain an API connection with the MySolArk portal to update Hubitat with your latest solar/battery inverter details.</strong></center>", type: "hidden"
        input name: "Instructions", title: "<center>**********<br><i>To make it work, you'll need to figure out your plant ID to associate with this driver, and provide the API your PowerView username and password</i></center>", type: "hidden"
        input name: "Blank1",  title: "<center>**********<br>The Plant ID is at the end of the URL when you navigate to the <a href='https://www.mysolark.com/plants/' target='_blank'>Plant Overview</a> page and click into your desired power plant.</center>",  type: "hidden"
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "refreshSched", type: "int", title: "Refresh every how many minutes?", defaultValue: 15  
        input name: "plantID", type: "string", title: "MySolArk Plant ID", description: "<i><small>The Plant ID is at the end of the URL when you login and navigate to the desired plant https://www.mysolark.com/plants/overview/</i></small><strong>?????</strong>", defaultValue: null
        input name: "Username", type: "string", title: "MySolArk Username", defaultValue: null
        input name: "Password", type: "password", title: "MySolArk Password", hidden: true, defaultValue: null
    }
}


def initialize() {
     log.info "Initializing the MySolArk service..."
     state.clear()
     state.Amperage = "the AC output being inverted from DC Power Sources (grid/gen current is not inverted)"
     state.Power = "the total number of Watts being drawn by the load/home"
     getToken()
     runIn(5,getPlantDetails)
}
                  
                  
def updated() {
    log.info "Updated... refreshing every ${refreshSched} minutes. Debug logging is: ${logEnable}."
    initialize()
    schedule("0 0/${refreshSched} * * * ?", refresh)
}


void getToken() {
    body1 = ['username':Username,'password':Password,'grant_type':'password','client_id':'csp-web','source':'elinter']
    def URIa = "https://openapi.inteless.com/v1/oauth/token"
    def URIb = "https://pv.inteless.com/api/v1/oauth/token"
    def URIc = "https://www.solarkcloud.com/oauth/token"
    def paramsTOK = [
        uri: URIc,
        headers: [
            'Origin': 'https://www.solarkcloud.com/',
            'Referer': 'https://www.solarkcloud.com/login'
        ],
        body: body1
    ]
    //* Catch error and define. 443. 401, read timed out
    try {
        httpPostJson(paramsTOK, { resp -> 
            if (logEnable) log.debug(resp.getData().data)
            
            state.xTokenKeyx = resp.getData().data.access_token
            attemptsNo = 1
            runIn(10,queryData)
        })
    } catch (Exception) {
     log.error "unable to login. This could be due to an invalid username/password, or MySolArk may be down."
    }
}


void getPlantDetails() {
    def key = "Bearer ${state.xTokenKeyx}"
    body5 = ['status': 1, 'type': -1, 'limit': 1, 'page': 1]
    def paramsInitial = [  
        uri: "https://www.solarkcloud.com/api/v1/plant/${plantID}/inverters",
        headers: [ 'Authorization' : key], 
        query: body5
        ]
        
        httpGet(paramsInitial,  { resp ->
            if (logEnable) log.debug(resp.getData().data)
            
            def invLimit = resp.getData().data.infos.ratePower[0]
            state.SystemSize =  invLimit.toInteger()
            state.inverterSN = resp.getData().data.infos.sn[0]
        })
}


def refresh() {
    attemptsNo = 0
    queryData()
}


void queryData()  {
    def key = "Bearer ${state.xTokenKeyx}"
    def paramsEnergy = [  
        uri: "https://www.solarkcloud.com/api/v1/plant/energy/${plantID}/flow",
        headers: [ 'Authorization' : key ],
        requestContentType: "application/x-www-form-urlencoded"
    ]
    try {
        httpGet(paramsEnergy, { resp ->
            if (logEnable) log.debug(resp.getData().data)
            
            boolean grid = resp.getData().data.gridOrMeterPower > 120
            boolean solar = resp.getData().data.pvPower > 120
            boolean battPower = resp.getData().data.battPower> 120
            float curr = ((resp.getData().data.loadOrEpsPower - resp.getData().data.gridOrMeterPower)/ 120 )
            float amperesEst = curr.round(2)
            int battCharge = resp.getData().data.battPower
            if (resp.getData().data.toBat) battCharge = battCharge* -1 
            int homePower = resp.getData().data.loadOrEpsPower
            int gridPower = resp.getData().data.gridOrMeterPower
            if (resp.getData().data.toGrid) gridPower = -gridPower
            int battSOC = resp.getData().data.soc
            def textSource = ""
            def newSource = ""
            
            if(grid) {
                newSource = "mains"
                textSource = "grid"
            } else {
                if(solar) {
                    newSource = "dc"
                    textSource = "solar"
                } else {
                if(battPower) {
                   newSource =  "battery"  
                    textSource = "battery"  
                    } else { 
                       newSource = "unknown"
                    textSource = "unknown"
                    }
                }
            }
            
            String batStat = ""
            if (amperesEst <-1 ) {
                batStat = "Charging Battery from Grid"
            } else {
                if ( battCharge < 0 ) { 
                     batStat = "Charging Battery from Solar"
                }
                if ( battCharge == 0 ) { 
                    batStat = "Battery not in use"
                }
                if ( battCharge > 0 ) {
                    if( gridPower <0 ) {
                    batStat = "Selling Battery to Grid"
                    } else {
                    batStat = "Discharging Battery"  
                    }
                }
            }
            if ( homePower == 0 )   {          
            } else {
            sendEvent(name: "power", value: homePower, unit: "W")
            sendEvent(name: "battery", value:  battSOC, unit: "%")         
            
            sendEvent(name: "powerSource", value: newSource)
            
            sendEvent(name: "PVPower", value: resp.getData().data.pvPower, unit: "W")
            sendEvent(name: "GridPowerDraw", value: gridPower, unit: "W")
            sendEvent(name: "BatteryDraw", value: battCharge, unit: "W")
            sendEvent(name: "GeneratorDraw", value: resp.getData().data.genPower, unit: "W")
            sendEvent(name: "BatteryStatus", value: batStat)
            }
            
            if (txtEnable) {
                if (batStat == "Battery not in use") {
                    if(newSource && battCharge && gridPower) {
                        log.info "Power Source is ${textSource}, Load is drawing ${gridPower} watts. Battery is at a ${battSOC}% charge."
                    } else {
                        log.error "MySolArk API is offline. We will try again in ${refreshSched} minutes."
                    }
                   } else { 
                    int AbsBatt = Math.abs(battCharge)
                    if ( AbsBatt < 250) {
                        log.info "Power Source is ${textSource}, Load is drawing ${homePower} watts. Battery is at a ${battSOC}% charge, operating at ${AbsBatt} watts."
                    } else {
                        log.info "${batStat} at a rate of ${AbsBatt} watts. Battery is at a ${battSOC}% charge."
                    }
                }
                
            }
            
        })
        getAmperage()
    } catch (exception) {
        log.error exception
        if (logEnable) log.debug("token may have expired, trying to get a new one; number of attempts is ${attemptsNo} and token is ${key} ")
        if (attemptsNo == 0) { 
            getToken() 
        }
        return null
    }
}


void getAmperage() {
     def key = "Bearer ${state.xTokenKeyx}"
     def paramsAmps = [  
        uri: "https://www.solarkcloud.com/api/v1/inverter/${state.inverterSN}/realtime/output",
        headers: [ 'Authorization' : key ],
        requestContentType: "application/x-www-form-urlencoded"
    ]
    
        httpGet(paramsAmps, { resp ->
            if (logEnable) log.debug(resp.getData().data)
            try {
            float valVac1 = 0
            def vac1 = resp.getData().data.vip[0]
            if( vac1 ) {
                vac1 = vac1.current
                valVac1 = vac1.toFloat()
                            } else {
                 valVac1 = 0
                }

            float valVac2 = 0
            def vac2 = resp.getData().data.vip[1]
            if( vac2 ) {
                vac2 = vac2.current
                valVac2 = vac2.toFloat()
                            } else {
                 valVac2 = 0
                }
            
            float valVac3 = 0
            def vac3 = resp.getData().data.vip[2]
            if( vac3 ) {
                vac3 = vac3.current
                valVac3 = vac3.toFloat()
            } else {
                 valVac3 = 0
                }
            
            float amperes = (valVac1 + valVac3 + valVac2).round(2)
            // invLimit warning = 66.66% power at 120 volts is SystemSize/180
            int invLimit = state.SystemSize/180
            int invOutput = (amperes*66.66)/invLimit
            if ( amperes>invLimit ) log.warn("Inverter pushing ${amperes} amps, ${invOutput}% of the inverter limit.")
            sendEvent(name: "amperage", value: amperes, unit: "A")         
            } catch(exception) { }}) 
}

