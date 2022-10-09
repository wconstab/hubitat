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
            input "theLightsOff", "capability.switch", title: "Lights to turn fully off during TV", multiple: true
            input "theLightsDim", "capability.switch", title: "Lights to dim during TV", multiple: true
			input "nanoMote", "capability.pushableButton", title: "NanoMote used for arm/disarm and trigger", multiple: false
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
- consider a delay for turning off lights, during delay period user can cancel via remote click
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
	subscribe(theDenon, "switch.off", denonHandler)
    subscribe(theDenon, "switch.on", denonHandler)
    subscribe(nanoMote, "pushed", nanoMotePushedHandler)
    subscribe(nanoMote, "held", nanoMoteHeldHandler)
	subscribe(switches, "switch.on", onHandler)
	atomicState.someOn = true

}

def appButtonHandler(update) {
    updated()
}

/*  NanoMote Guide
1 - moon
2 - people
3 - center
4 - power
*/

void nanoMotePushedHandler(evt) {
    log.debug "nanoMotePushedHandler() called: ${evt.name} ${evt.value}"
    if(evt.value == "1") {
        if(atomicState.armed) {
            log.debug "nanoMotePushedHandler triggering TV lights"
            captureLights()
            dimLights()
        }
    }
}

void nanoMoteHeldHandler(evt) {
    log.debug "nanoMoteHeldHandler() called: ${evt.name} ${evt.value}"
    if(evt.value == "1") {
        if(atomicState.armed) {
            log.debug "nanoMote disarming TV lights"
            armedSignal.setColor(hue: 0, saturation: 100)
            atomicState.armed = false
        } else {
            log.debug "nanoMote arming TV lights"
            armedSignal.setColor(hue: 33, saturation:100)
            atomicState.armed = true            
        }
    }
}

void denonHandler(evt) {
    log.debug "handler() called: ${evt.name} ${evt.value}"
    if(evt.value == "on") {
        log.debug "Denon turned on!"
        //if(atomicState.armed) {
        //    log.debug "Denon triggered dimLights()"
        //    captureLights()
        //    dimLights()
        //}
    } else {
        log.debug "Denon turned off!"
        if(atomicState.armed) {
            restoreLights()
        }
    }
}

void captureLights() {
    atomicState.lightsOffState = []
    theLightsOff.eachWithIndex { it, idx ->
        log.debug "Capture: $it - $it.currentSwitch - $it.currentLevel"
        atomicState.lightsOffState += [idx: idx, state: it.currentSwitch, level:it.currentLevel]
    }
    atomicState.lightsDimState = []
    theLightsDim.eachWithIndex { it, idx ->
        log.debug "Capture: $it - $it.currentSwitch - $it.currentLevel"
        atomicState.lightsDimState += [idx: idx, state: it.currentSwitch, level:it.currentLevel]
    }
    log.debug "Captured $atomicState.lightState.size lights"
}

void dimLights() {
    theLightsOff.each {
        it.off()
    }
    dimLevel = 5
    theLightsDim.each {
        it.on()
        it.setLevel(dimLevel)
    }
}

void restoreLights() {
    log.debug("restoreLights")
    atomicState.lightsOffState.each{
        if(it.state == "on") {
            log.debug "restoring turned on light $it.idx to level $it.level"
            theLightsOff[it.idx].on()
            theLightsOff[it.idx].setLevel(it.level)
        }
    }
    atomicState.lightsDimState.each{
        if(it.state == "on") {
            log.debug "restoring turned on light $it.idx to level $it.level"
            theLightsDim[it.idx].on()
            theLightsDim[it.idx].setLevel(it.level)
        }
    }    
}
