/*
 * Door Chime
 *            This is a fork of Bryan Copeland's 'Simple Door Chime'. I took over after the Alpha release stopped working, adding Modes/HSM Functionality.
 *
 *  Change History:
 *
 *      Date          Source        Version     What
 *      ----          ------        -------     ----
 *      2020-01-09    djdizzyd      0.1.0       Alpha Release
 *      2022-01-30    pentalingual  0.2.0       Fixed Errors, Added Chimes, HSM Status
 *      2024-02-15    pentalingual  0.3.0       Added UI improvements and Mode Rules
 */


	public static String version()      {  return "0.3.0"  }


definition(
	name: "Door Chime",
	namespace: "pentalingual",
	author: "Bryan Copeland",
	description: "Door Chime App",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: "",
	importUrl: "https://raw.githubusercontent.com/pentalingual/Hubitat/main/Chime/Door_Chime.groovy"
)


preferences {
	page(name: "mainPage")
}


def display() {
      	if(thisName) app.updateLabel("$thisName") else app.updateSetting("thisName", "Door Chime")  
    section (getFormat("title", "${thisName}")) {
		paragraph getFormat("line")
	}
}


def getFormat(type, myText=""){			// Modified from @Stephack Code   
	if(type == "header-green") return "<div style='color:#002855;font-weight: bold;background-color:#B3A369;border: 1px solid;box-shadow: 2px 3px #002855'>${myText}</div>"
    if(type == "line") return "<hr style='background-color:#002855; height: 1px; border: 0;'>"
    if(type == "title") return "<h2 style='color:#002855;font-weight: bold'>${myText}</h2>"
}


List<String> getModeOptions() {
    List<String> options = new ArrayList<>()
    for (Object mode : location.getModes())
        options.add(mode.toString())
    
    return options
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
        display()
		section {
			input "thisName", "text", title: "Name this door chime", submitOnChange: true
			if(thisName) app.updateLabel("$thisName") else app.updateSetting("thisName", "Door Chime")
			input "contactDev", "capability.contactSensor", title: "Select Contact Sensor", submitOnChange: true, required: true, multiple: true
			input "debounce", "bool", title: "Enable Debounce", submitOnChange: true, required: true, defaultValue: false
			if (debounce) {
				input "delayTime", "number", title: "Enter number of milliseconds to delay for debounce", submitOnChange: true, defaultValue: 1000
			}
			input "chimeType", "enum", title: "Type of chime device", options: ["chime": "chime", "speechSynthesis": "TTS", "tone": "Tone/Beep"], submitOnChange: true
			if (chimeType){
				input "chimeDev", "capability.$chimeType", title: "Select Chime Device", submitOnChange:true, required: true, hideWhenEmpty: "chimeType"
			}
			if (chimeDev) {
				switch (chimeType) {
					case "chime":
						if (chimeDev.hasAttribute("soundEffects")) {
                            def soundEffectsList = chimeDev.currentState("soundEffects").value
                            paragraph "<strong>Sound effects list for ${chimeDev}: <br>${soundEffectsList}</strong>"
							input "soundNum", "number", title: "Sound number to play", submitOnChange: true, required: true
						}
						break;
					case "speechSynthesis":
						input "speakText", "text", title: "Text to speak", submitOnChange: true, required: true
						break;
				}
			}
            input "testSound", "button", title: "Test your sound!"
            input "useModes", "enum", title: "Block during certain Modes?", options: getModeOptions(), submitOnChange: true, multiple: true
            input "useHSM", "enum", title: "Use HSM Status for Chime?", options: ["0": "No", "disArmed": "Only when Disarmed"], submitOnChange: true, defaultValue: "No"
            input "enableLog", "bool", title: "Enable Logging", submitOnChange: false, required: true, defaultValue: true
            
		}
	}
}

def appButtonHandler(btn) {
	switch (btn) {
		case "testSound":
			chimeAction()
	}
}


def installed() {
	initialize()
}


def updated() {
	unsubscribe()
	unschedule()
	initialize()
}


def initialize() {
    if(enableLog) log.debug "initializing"
    for (dev in contactDev) {
        if(enableLog) log.debug "subscribing to " + dev.getDisplayName()
	    subscribe(dev, "contact.open", handler)
        }
    if (useHSM=="disArmed") { 
            if(enableLog) log.debug "subscribing to HSM Status"
            subscribe(location, "hsmStatus", statusHandler)
    }
    if (useModes) { 
            if(enableLog) log.debug "subscribing to Hub Modes to prevent chimes: ${useModes}"   
            }
}


def handler(evt) { 
    if(useHSM) {
        if(location.hsmStatus=="allDisarmed") {
        if (useModes) {
            String currentMode = location.getMode()
            if (useModes.contains(currentMode)) {
                if(enableLog) log.info "not chiming due to mode ${currentMode}"
            } else {
            if (debounce) {
	    	    runInMillis(delayTime, debounced, [data: [o: evt.value, d: evt.device.getDisplayName()]])
    		    if(enableLog) log.info "Contact $evt.device $evt.value, start delay of $delayTime milliseconds"
            } else {
	    	    debounced([o: evt.value, d: evt.device.getDisplayName()])
            }
            }
        } else {
            
            if (debounce) {
	    	    runInMillis(delayTime, debounced, [data: [o: evt.value, d: evt.device.getDisplayName()]])
    	    	if(enableLog) log.info "Contact $evt.device $evt.value, start delay of $delayTime milliseconds"
            } else {
	    	    debounced([o: evt.value, d: evt.device.getDisplayName()])
            }
        }
        } else {
            if(location.hsmStatus=="disarmed") {
        if (useModes) {
            String currentMode = location.getMode()
            if (useModes.contains(currentMode)) {
                if(enableLog) log.info "not chiming due to mode ${currentMode}"
            } else {
            if (debounce) {
	    	    runInMillis(delayTime, debounced, [data: [o: evt.value, d: evt.device.getDisplayName()]])
    		    if(enableLog) log.info "Contact $evt.device $evt.value, start delay of $delayTime milliseconds"
            } else {
	    	    debounced([o: evt.value, d: evt.device.getDisplayName()])
            }
            }
        } else {
            
            if (debounce) {
	    	    runInMillis(delayTime, debounced, [data: [o: evt.value, d: evt.device.getDisplayName()]])
    	    	if(enableLog) log.info "Contact $evt.device $evt.value, start delay of $delayTime milliseconds"
            } else {
	    	    debounced([o: evt.value, d: evt.device.getDisplayName()])
            }
        }
            }
        }
    } else {
        if (useModes) {
            String currentMode = location.getMode()
            if (useModes.contains(currentMode)) {
                if(enableLog) log.info "not chiming due to mode ${currentMode}"
            } else {
            if (debounce) {
	    	    runInMillis(delayTime, debounced, [data: [o: evt.value, d: evt.device.getDisplayName()]])
    		    if(enableLog) log.info "Contact $evt.device $evt.value, start delay of $delayTime milliseconds"
            } else {
	    	    debounced([o: evt.value, d: evt.device.getDisplayName()])
            }
            }
        } else {
            
            if (debounce) {
	    	    runInMillis(delayTime, debounced, [data: [o: evt.value, d: evt.device.getDisplayName()]])
    	    	if(enableLog) log.info "Contact $evt.device $evt.value, start delay of $delayTime milliseconds"
            } else {
	    	    debounced([o: evt.value, d: evt.device.getDisplayName()])
            }
        }
    }
}

def debounced(data) {
	if(data.o == "open") {
		if(enableLog) log.info "Contact $data.d debounced chiming" +  chimeDev.getDisplayName() + " sound number $soundNum"
		chimeAction()
	} 
}
                        

def chimeAction() {
	log.info "Chime Action"
	switch(chimeType) {
		case("chime"): 
			//chime Type
			if(enableLog) log.debug "playing sound: $soundNum on " + chimeDev.getDisplayName()
			chimeDev.playSound(soundNum.toInteger())
			break;
		case("speechSynthesis"):
			// speech Type
			if(enableLog) log.debug "Speaking '$speakText' on " + chimeDev.getDisplayName()
			chimeDev.speak(speakText)
			break;
		case("tone"):
			// tone Type
			if(enableLog) log.debug "Sending beep to " + chimeDev.getDisplayName()
			chimeDev.beep()
			break;
	}
}
