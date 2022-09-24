definition(
	name: "TV Lights",
	namespace: "hubitat",
	author: "Will Constable",
	description: "Dim the lights for TV watching, and then restore them to their previous levels",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: ""
)

preferences {
	page(name: "mainPage")
}


Map mainPage() {
	dynamicPage(name: "mainPage", title: "TV Lights", uninstall: true, install: true) {
		section {
            input "theDenon", "capability.switch", title: "The AVR to monitor for on/off", multiple: false
            input "theLights", "capability.switch", title: "Lights to control", multiple: true
			input "armButton", "capability.pushableButton", title: "Arm Button", multiple: false
            input "disarmButton", "capability.pushableButton", title: "Disarm Button", multiple: false
            input "armedSignal", "capability.colorControl", title: "Armed Signal Color LED"
            
            input name:"update", type:"button", title:"Update"

		}
	}
}

/*
TODO

- figure out a way to only react to the TV modes of denon being on, instead of its sw update
- get arm/disarm working from innovelli clicks
- clean up logging
*/


void updated() {
    log.debug "updated()"
    unsubscribe()
	  initialize()

    // for debugging
    //captureLights()
    //restoreLights()

}

void installed() {
	  initialize()
}

void initialize() {
    log.debug "initialize()"


	subscribe(theDenon, "switch.off", handler)
    subscribe(theDenon, "switch.on", handler)
    subscribe(armButton, "pushed", armDisarmHandler)
    subscribe(disarmButton, "pushed", armDisarmHandler)


	  subscribe(switches, "switch.on", onHandler)
	  atomicState.someOn = true
    log.debug "initialize() done"

}

def appButtonHandler(update) {
    updated()
}

void armDisarmHandler(evt) {
    log.debug "armDisarmHandler() called: ${evt.name} ${evt.value}"
    if(evt.value == "4") { // winefridge on
        log.debug "armDisarmHandler arming"
        armedSignal.setColor(hue: 33, saturation:100)
        atomicState.armed = true
    } else {
        log.debug "armDisarmHandler disarming"
        armedSignal.setColor(hue: 0, saturation: 100)
        atomicState.armed = false
    }
}

void handler(evt) {
    log.debug "motionHandler() called: ${evt.name} ${evt.value}"
    if(evt.value == "on") {
        log.debug "Denon turned on!"
        if(atomicState.armed) {
            log.debug "Denon triggered dimLights()"
            captureLights()
            dimLights()
        }
    } else {
        log.debug "Denon turned off!"
        if(atomicState.armed) {
            restoreLights()
        }
    }
}

void captureLights() {
    atomicState.lightState = []
    theLights.eachWithIndex { it, idx ->
        log.debug "Capture: $it - $it.currentSwitch - $it.currentLevel"
        atomicState.lightState += [idx: idx, state: it.currentSwitch, level:it.currentLevel]
    }
    log.debug "Captured $atomicState.lightState.size lights"
}

void dimLights() {
    theLights.each {
        it.off()
    }
}

void restoreLights() {
    log.debug("restoreLights")
    atomicState.lightState.each{
        if(it.state == "on") {
            log.debug "restoring turned on light $it.idx to level $it.level"
            theLights[it.idx].on()
            theLights[it.idx].setLevel(it.level)
        }
    }
}

