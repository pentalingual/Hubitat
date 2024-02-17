/*
 * Laptop Battery
 *
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                              URL
 *      ----          ------        -------     ----                                              ---
 *      2022-01-22    pentalingual  0.1.0       Starting version
 *      2024-02-17    pentalingual  0.1.5       Simplified logging
 */

import java.text.SimpleDateFormat

static String version() { return '0.1.0' }

metadata {
    definition(
            name: "Laptop Battery",
            namespace: "pentalingual",
            author: "Andrew Nunes",
    )  {
        capability "Actuator"
        capability "Refresh"
        capability "Battery"

        attribute "battery", "number"
        attribute "lastCheckin", "Date"
        attribute "lastRefresh", "Date"
    }
}

String bSl = $/\/$
String PowerShellLoc = "C:${bSl}Windows${bSl}System32${bSl}WindowsPowerShell${bSl}v1.0${bSl}powershell.exe"

preferences {
  input name: "Blank0",  title: "<center><strong>This driver will read a text file from a shared network location that is updated by your computer with the battery percentage. </strong></center>", type: "hidden"
    input name: "Instructions", title: "<center>**********<br><i>To make it work, you'll need to create a <strong>task</strong> in the Windows<strong> Task Scheduler </strong>that periodically maintains a text file with the device's current battery percent on a network shared folder (one that Hubitat can access, like a Network attached server).</i></center>", type: "hidden"
  input name: "Blank1",  title: "<center>**********<br>Here are the windows scripts that need to be embeded in a task's actions to keep your device battery percentage updated:<br>1. <strong>Program/script: <br></strong><i>${PowerShellLoc}</i><br><strong>argument: </strong><br><i>Get-Date | Out-File -FilePath .${bSl}batteryreport.txt</i></center>",  type: "hidden"
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
  input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
  input name: "decodeEnable", type: "bool", title: "Enable decoding/Clean non-ANSI characters", defaultValue: true
  input name: "fileLocation", type: "string", title: "Link to the BatteryReport", defaultValue: null
  input("autoUpdatehour", "number", title: "Enable automatic update every n hours", defaultValue: 0, required: true, displayDuringSetup: true)
  input("autoUpdatemin", "number", title: "Enable automatic update every n minutes", defaultValue: 0, required: true, displayDuringSetup: true)
}


def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def installed() {
    logging("installed()", 100)
    unschedule()
    refresh()
}

def updated() {
    log.info "updated... debug logging is: ${logEnable}. "
    if (logEnable) {
        if (autoUpdatemin+autoUpdatehour ==0)log.warn("Update: Automatic Update DISABLED")
    }
    if (logEnable) {
        if (autoUpdatemin+autoUpdatehour>0)log.debug("Update: Automatic Update enabled")
    }
    if (logEnable) runIn(1800, logsOff)
    unschedule()
    refresh()
    schedUpdate()

}

def refresh() {
       runCmd()
}

def schedUpdate() {
    unschedule()
    if (autoUpdatemin+autoUpdatehour>0) { 
        if (autoUpdatemin>0) { 
            if (autoUpdatehour>0) { 
                schedule("0 0/${autoUpdatemin} 0/${autoUpdatehour} * * ?", refresh)
                if (logEnable) log.debug("autoupdate: refresh schedule set for every ${autoUpdatehour} hour(s) and ${autoUpdatemin} minute(s)")
            } else {
                schedule("0 0/${autoUpdatemin} * * * ?", refresh)
                if (logEnable) log.debug("autoupdate: refresh schedule set for every ${autoUpdatemin} minute(s)")
            } 
        } else {
            schedule("0 0 0/${autoUpdatehour} * * ?", refresh)
            if (logEnable) log.debug("autoupdate: refresh schedule set for every ${autoUpdatehour} hour(s)")
        }
    }
}

                                        


def runCmd() {
    now = new Date()

    sendEvent(name: "lastRefresh", value: now)
    
   checkBattery()
}

def checkBattery() {
    if (device.currentValue("lastRefresh") == (null)) {
        runCmd()
    } else {
        batteryPreviouslySet = true
        existingBatteryreport = "${battery}"
    }
    readFile()
}

def readFile(){
    if (logEnable) log.debug("Downloading the newest report at ${fileLocation}")
    uri = fileLocation
    def queryResults = ""
    def params = [
        uri: uri,
        contentType: "text/html; charset=ANSI"
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
                printLine = resp.getData() as String
                printLines = printLine.readLines()
                String BL = printLines[12]
                String LCI = printLines[2]
                if(decodeEnable) {
                String Remover = BL.substring(0,1)
                String BLC = BL.replaceAll(Remover, '')
                int BLI = BLC.toInteger()
                String LCIFinal = LCI.replaceAll(Remover, '')
                Date LCIDate = Date.parse('EEEEE, MMMMM d, yyyy h:mm:ss aa', LCIFinal)
                sendEvent(name: "lastCheckin", value:  LCIDate.format('EEE MMM d HH:mm:ss z yyyy'), descriptionText: descriptionText)
                sendEvent(name: "battery", value: BLI)
                if (txtEnable) log.info("Battery reported as ${BLI}% on ${LCIDate}")
                } else {
                    int BLI = BL.toInteger()
                 Date LCIDate = Date.parse('EEEEE, MMMMM d, yyyy h:mm:ss aa', LCI)
                sendEvent(name: "lastCheckin", value:  LCIDate.format('EEE MMM d HH:mm:ss z yyyy'), descriptionText: descriptionText)
                sendEvent(name: "battery", value: BLI)
                }
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Check that the Link provided to the BatteryReport has the battery report. Debugging will log what was received. Error: ${exception.message}. "
        if (logEnable)  log.debug( printLine )
        return null;
    }
}
