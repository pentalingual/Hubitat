/*
 * Degree Days Irrigation Timer
 *
 *
 *  Change History:
 *
 *      Date          Source        Version     What
 *      ----          ------        -------     ----
 *      2024-02-09    pentalingual  0.1.0       Initialized Community Release
 *
 */

	public static String version()      {  return "0.1.0"  }

definition(
	name: "Degree Days Irrigation Timer",
	namespace: "pentalingual",
	author: "Andrew Nunes",
	description: "Irrigation scheduler based on cumulative degree days since last watering or precipication",
	category: "Utility",
        importUrl: "https://raw.githubusercontent.com/pentalingual/Hubitat/master/Irrigation/Degree_Days_Irrigation_Timer.groovy",
	iconUrl: "",
	iconX2Url: ""
)


preferences {
	page(name: "mainPage")
    page(name: "calcPage", nextPage	: "mainPage")
}


def display() {
      	if(thisName) app.updateLabel("$thisName") else app.updateSetting("thisName", "Irrigation System")  
    section (getFormat("title", "${thisName} - Degree Days Timer")) {
		paragraph getFormat("line")
	}
}


def getFormat(type, myText=""){			// Modified from @Stephack Code   
	if(type == "header-green") return "<div style='color:#002855;font-weight: bold;background-color:#B3A369;border: 1px solid;box-shadow: 2px 3px #002855'>${myText}</div>"
    if(type == "line") return "<hr style='background-color:#002855; height: 1px; border: 0;'>"
    if(type == "title") return "<h2 style='color:#002855;font-weight: bold'>${myText}</h2>"
}


def installed() {
	            if(atomicState.CDD == null) {
                atomicState.CDD =  0 
                } 
    initialize()
}
    

def updated() {
	if(enableLog) log.debug "Updated with settings: ${settings}"
	unschedule()
	initialize()
}


def initialize() {
    if(enableLog) log.debug "initializing the ${thisName}"
    	unschedule()
        for (dev in irrSwitch) {
            if(enableLog) log.debug "setting ${thisName} switch to " + dev.getDisplayName()
           atomicState.timerMin = irrStart.substring(14,16) as int
           atomicState.timerHour = irrStart.substring(11,13) as int      
            log.info "setting the ${irrSwitch} to run every day at ${atomicState.timerHour}:${atomicState.timerMin} for ${irrTimer} minutes when ${degreeDays} cumulative degree days are met. Presently CDD are ${atomicState.CDD}"
    scheduler()
        }
}


def scheduler() {
    schedule("0 ${atomicState.timerMin} ${atomicState.timerHour} * * ?", refresh)
}


def refresh() {
   if(enableLog) log.debug "checking the weather. . ."
    openMapsAPI.refresh()
   runIn(45, irrPrerun)
}


def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
        if(!state.Installed) {   
            calcPage()
        } else {
        display()
		section 
        {
			input "thisName", "text", title: "Name this Irrigation Circuit:", submitOnChange: true
            paragraph "<small>* This app is based on a UC Davis Viticulture system measuring total warm degrees accumulated over time. It works by triggering your irrigation timer once the cumulative degree days since your last watering event have been passed.</small>"
        }
              
        section("Weather API Settings", hideable: true, hidden: !enableWatering){
            input "openMapsAPI", "capability.sensor", title: "Select your OpenMaps Api Weather widget device:", submitOnChange: true, required: enableWatering
            paragraph "<small>* Use your weather maps device to measure each day's high temperature and inches of rain per day. </small>"
            paragraph "<strong>OpenWeatherMap-Alerts Weather Driver</strong>	Download from Hubitat Packet Manager or use the Import URL!  "      
            paragraph "@Matthew (Scottma61) Copyright 2023 Import URL: https://raw.githubusercontent.com/HubitatCommunity/OpenWeatherMap-Alerts-Weather-Driver/master/OpenWeatherMap-Alerts%2520Weather%2520Driver.groovy </strong></small>"
            }
            
        section{
			 href(name: "gotocalcPage", title: "Degree Days Calculator", required: false, page: "calcPage", description: "Model your degree day Settings!")
               }
            
        section("<strong>Water Timer Settings</strong>"){
            input "enableWatering", "bool", title: "<strong>Enable Water Timer</strong>", submitOnChange: true, defaultValue: false
            input "irrSwitch", "capability.switch", title: "Select switch to turn on the irrigation system:", submitOnChange: true, required: enableWatering, multiple: true
            paragraph ""
            input "degreeDays", "number", title:"Target Cumulative Degree Days (CDD) between waterings:", submitOnChange: false, required: enableWatering, defaultValue: sugDegDayTarg
            paragraph "<small>* If a CDD target is set at 80, and the zero bound is 60 degrees, then on day 3 of 90 degree heat, irrigation will trigger with 90 CDD and subtract 80, carrying the balance of 10 to the next day.</small>"
            
            input "irrStart", "time", title:"Time each day to check the day's highs, and the amount has rained, and then run the irrigation system*:", submitOnChange: false, required: enableWatering
            paragraph "<i><small>*A time before midnight is recommended for most accuracy on the inches it has rained that day.</small></i>"
            input "irrTimer", "number", title:"Minutes of watering once irrigation is triggered:", submitOnChange: false, required: enableWatering
			
            }
        section ("Additional Degree Day Settings", hideable: true, hidden: false) {            
                
                input "degreeZero", "number", title:"The Zero Degree Bound: High Fahrenheit temperature at which plant growth is minimal and without vigor, this baseline low temperature is subtracted from each day's high to establish that day's degree days", submitOnChange: false, required: enableWatering, defaultValue: sampleLow
            
                input "degreeOver", "number", title:"Minimum Degree Days (MDD) to add at zero:", submitOnChange: false, defaultValue: sampleMDD
                paragraph "<small>* MDD overrides the Zero Degree Bound subtractor and guarantees a minimum watering frequency each (CDD/MDD) days when the highs are near or below the Zero Degree Bound in cool seasons, absent any precipitation.</small>"
                }    
            section {
                 input "enableLog", "bool", title: "Enable Debug Logging", submitOnChange: false, defaultValue: true
            }
	}
    }
}


def calcPage() {
    return dynamicPage(name: "calcPage", title: " ", install: false, uninstall: false) {
        display()
        section("Read More", hideable: true, hidden: state.freqHidden ) {
            paragraph "Plants need to be watered more often when temperatures are high than when temperatures are cooler. The plants may need to be watered as often as once a day when hot. But in the winter, you may only need them watered once every 2 or 4 weeks if it doesn’t rain. This app keeps a running tally of the total degrees each day that are above your low temperature - or ‘zero degree bound'. Each irrigation or precipitation event means your cumulative degree days are reset downwards. Every day, this app works by first checking the weather, and if there were greater than 0 inches of rain reported, it reduces your accumulated degree days proportionally with 1 inch as 100% of your irrigation target." 
                paragraph "In order to calculate your benchmarks, estimate how often you want the system triggered given a high, medium, and low temperature. The lower bound is 50 by default and needs to be your approximate low temperature when plant vigor stops, but this should be set higher for plants that go dormant at a higher temperature; perhaps lower for plants that grow despite even cooler temperatures."
             paragraph "Use the Minimum Degree Days to increase your frequency of watering when it is cooler."
 }
section("Enter your estimated watering frequencies", hideable: true, hidden: state.freqHidden) {
                highDeg = 100
                medDeg = 80
                lowDeg = 65
                coldDeg = 50
      
        input name: "hiFreq",  type: "number", title: "At highs of <font color='red'>${highDeg}</font> degrees water every how many days?", submitOnChange: false, defaultValue: 1
        input name: "medFreq",  type: "number", title: "At highs of <font color='orange'>${medDeg}</font> degrees water every how many days?", submitOnChange: false, defaultValue: 4
        input name: "lowFreq",  type: "number", title: "At highs of <font color='green'>${lowDeg}</font> degrees water every how many days?", submitOnChange: false, defaultValue: 8
 input name: "freqHidden", type: "button", title: "See Results", state: "freqHidden"
}
        
        
    section("<strong><br>Fine tine your watering calculations</strong>", hideable: false, hidden: false) {
            input name: "sampleLow",  type: "number", title: "Low temperature where plant vigor is minimal:", submitOnChange: true, defaultValue: 50
            
                highDegreeDays = 0
                medDegreeDays = 0
                lowDegreeDays = 0
                coldDegreeDays = 0
        
            try  { target0 = Math.max(0, sampleMDD) 
                 } catch(e) {
                   target0 = 0
                 }
        
            try  { sampleLow0 = Math.max(0, sampleLow)
                 } catch(e) {
                   sampleLow0 = 0
                 }
        
                highDegreeDays = Math.max( highDeg - sampleLow0, target0)
                medDegreeDays = Math.max( medDeg - sampleLow0, target0)
                lowDegreeDays = Math.max( lowDeg - sampleLow0, target0)
                coldDegreeDays = Math.max( coldDeg - sampleLow0, target0)
        
        try  { highdegEstTarg = hiFreq * highDegreeDays
             } catch(e) { highdegEstTarg = 0}
             try  { meddegEstTarg = medFreq * medDegreeDays
                  } catch(e) {meddegEstTarg =0}
                   try  { lowdegEstTarg = lowFreq * lowDegreeDays
                        } catch(e) {lowdegEstTarg = 0}
                       
        recDegDayTarg = Math.round((highdegEstTarg + meddegEstTarg + lowdegEstTarg)/3)
        
        maxRecMDD = Math.max(65 - sampleLow0 -1, 0)

        paragraph "<table><thead><tr><td><strong>Daily High</strong></td><td></td><td><strong>Water N Days</strong></td><td></t><td><strong>Day Degrees > Low</strong></td><td></td><td><strong>Accumulated Degrees to water</strong></td></tr></thead> <tr><td><font color='red'>${highDeg}</font></td><td></td><td>${hiFreq}</td><td>x</td><td>${highDegreeDays}</td><td>=</td><td>${highdegEstTarg}</td></tr>  <tr><td><font color='orange'>${medDeg}</font></td><td></td><td>${medFreq} </td><td>x</td><td>${medDegreeDays}</td><td>=</td><td>${meddegEstTarg}</td></tr><tr><td><font color='green'>${lowDeg}</font></td><td></td><td>${lowFreq} </td><td>x</td><td>${lowDegreeDays}</td><td>=</td><td>${lowdegEstTarg}</td></table>"      
         paragraph "<strong>Suggested degree day target: ${recDegDayTarg}</strong>"

        input name: "sugDegDayTarg", type: "number", title: "Target Degree days between waterings:", submitOnChange: true, defaultValue: recDegDayTarg
     input name: "sampleMDD", type: "number", title: "Minimum Degrees to add per day*:", submitOnChange: true, defaultValue: 0    
        
        if( maxRecMDD ==0) {       
        paragraph "<i><small>* Your low temperature is set at or above 65 degrees</small></i>"   
                    } else {
             paragraph "<i><small>* Using a number larger than ${maxRecMDD} will nullify the degree watering schedule when the weather is above 65 degrees</small></i>"
        }
    } 
   section {
         try  { float highDayCalc = sugDegDayTarg/highDegreeDays
               highRound = Math.round(highDayCalc)
               if(highRound == highDayCalc) {
                   sugHighTargDays   = "Every ${highRound} Days" 
               } else {
                   highRound = highDayCalc.round(1)
                   sugHighTargDays   = "Every ${highRound} Days" 
               }
              } catch(e) {
             sugHighTargDays   =      "Never"      
               }
       
         try  {  float medDayCalc =    sugDegDayTarg/medDegreeDays
               medRound = Math.round(medDayCalc)
               if(medRound==medDayCalc) {
                           sugMedTargDays    =  "Every ${medRound} Days"
                            } else {
               medRound = medDayCalc.round(1)
               sugMedTargDays    =  "Every ${medRound} Days"
         }
              } catch(e) {
                    sugMedTargDays    =   "Never"             
               }
       
             try  { float lowDayCalc =  sugDegDayTarg/lowDegreeDays
                    lowRound = Math.round(lowDayCalc)
                   if(lowDayCalc==lowRound) {
                                      sugLowTargDays    =  "Every ${lowRound} Days"
                                } else {
                 lowRound = lowDayCalc.round(1)
                   sugLowTargDays    =  "Every ${lowRound} Days"
             }
              } catch(e) {
                   sugLowTargDays    = "Never"
               }
                     try  { float coldDayCalc =  sugDegDayTarg/coldDegreeDays
                       coldRound = Math.round(coldDayCalc)   
                           if(coldDayCalc == coldRound) {
                                sugColdTargDays    = "Every ${coldRound} Days"
                                 } else {
                         coldRound = coldDayCalc.round(1)    
                    sugColdTargDays    = "Every ${coldRound} Days"
                           }
              } catch(e) {
                   sugColdTargDays    = "Never"
               }
       
       
         
        paragraph "<table><thead><tr><td>Daily High</td><td></td><td>Day Degrees</td><td></td><td>Accumulated Degrees to water</td><td></td><td><strong>Target Results for ${sugDegDayTarg} CDD</strong></td></thead> <tr><td><font color='red'>${highDeg}</font></td><td></td><td>${highDegreeDays}</td><td>=</td><td>${highdegEstTarg}</td><td></td><td><i> ${sugHighTargDays}</i></td></tr>  <tr><td><font color='orange'>${medDeg}</font></td><td></td><td>${medDegreeDays}</td><td>=</td><td>${meddegEstTarg}</td><td></td><td><i> ${sugMedTargDays}</i></td></tr>  <tr><td><font color='green'>${lowDeg}</font></td><td></td><td>${lowDegreeDays}</td><td>=</td><td>${lowdegEstTarg}</td><td></td><td><i> ${sugLowTargDays}</i></td></tr>  <tr><td><font color='blue'>${coldDeg}</font></td><td></td><td>${coldDegreeDays}</td><td>=</td><td>N</td><td></td><td><i> ${sugColdTargDays}</i></td></tr></table>"   
       if(state.freqHidden) {         
               state.Installed = true

    }
    }
    }
}


def irrPrerun() {
    todaysHigh = openMapsAPI.currentValue("forecastHigh")
   todaysRain = openMapsAPI.currentValue("rainToday")
   degreesToday = todaysHigh - degreeZero
    if(enableLog) log.debug "Report: we expect ${todaysRain} inches of rain today and a high of ${todaysHigh}. Cumulative degree days on record were ${atomicState.CDD}"
    if(degreesToday > degreeOver) {
        ddToday = degreesToday
    } else {
        ddToday = degreeOver
    }
        tempCDD = atomicState.CDD + ddToday
    atomicState.CDD = tempCDD
    if(todaysRain >0) {
        subRain = Math.round(degreeDays * todaysRain)
        log.info "Adding ${ddToday} degree days, but with ${todaysRain} inches of rain today and a cumulative ${tempCDD} degree days, we are also subtracting ${subRain} from that CDD total."
        atomicState.CDD = tempCDD - subRain
    }   else {   
    if(atomicState.CDD > degreeDays) {
        atomicState.CDD = tempCDD - degreeDays
            log.info "By adding ${ddToday} degree days for a cumulative ${tempCDD} degree days, we have surpassed our CDD target of ${degreeDays}. Resetting CDD to ${atomicState.CDD} and triggering irrigation."
        irrRun()
    } else {
            log.info "Adding ${ddToday} degree days for a cumulative ${atomicState.CDD} degree days"
    }
        runIn(30, scheduler)
}
}


def irrRun() {
    if(enableWatering) { irrSwitch.on()
    checkMin = atomicState.timerMin + irrTimer
    checkHour = atomicState.timerHour + 1
    if(checkMin >60) {
        setMin = checkMin -60
        if(checkHour >24) {
            setHour = 0
        } else {
            setHour = checkHour
        }
    } else {
        setMin = checkMin
        setHour = atomicState.timerHour
    }        
    if(enableLog) log.debug "Running the irrigation system for ${irrTimer} minutes"
    schedule("45 ${setMin} ${setHour} * * ?", irrOff)
}
}


def irrOff() {
    irrSwitch.off()
}
